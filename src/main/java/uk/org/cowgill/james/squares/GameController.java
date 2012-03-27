package uk.org.cowgill.james.squares;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Random;

/**
 * This class controls the communications with
 * another GameController over the network and the global game state.
 * 
 * This class is not thread safe. All calls to this class MUST be
 * made from the swing event dispatch thread
 * 
 * @author James
 */
public class GameController
{
	/**
	 * The version of the protocol used by the controller
	 */
	public static final int PROTOCOL_VERSION = 1;
	
	//Master status constants
	private static final int NOT_MASTER = 0;
	private static final int MASTER_ME_FIRST = 1;
	private static final int MASTER_YOU_FIRST = 2;
	
	//Protocol message constants
	private static final byte CMD_INIT = 0;
	private static final byte CMD_ERROR = 1;
	private static final byte CMD_PLAY = 2;
	private static final byte CMD_MOVE = 3;
	private static final byte CMD_WIN = 4;
	private static final byte CMD_SURRENDER = 5;
	private static final byte CMD_CHAT = 6;
	
	/**
	 * The master status of this connection - one of the MASTER constants
	 */
	private final int masterStatus;
	
	/**
	 * Network connection
	 */
	private final SwingMessageConnection conn;
	
	/**
	 * Game output link
	 */
	private final GameOutput output;
	
	/**
	 * The player number of this player (1 or 2)
	 */
	private final int playerNum;
	
	/**
	 * Player scores (games won)
	 */
	private final int[] score = new int[2];
	
	/**
	 * Names of players
	 */
	private String[] playerNames = new String[2];
	
	/**
	 * The controller state
	 */
	private ControllerState controlState = ControllerState.InitWaiting;
	
	/**
	 * True if player 1 started first this game (or for the next game if state == Ready)
	 */
	private boolean player1First;
	
	/**
	 * Contains the state of the current game of squares (null unless controlState == Playing)
	 */
	private GameState gameState;
	
	/**
	 * The controller state
	 * 
	 * @author James
	 */
	private enum ControllerState
	{
		/**
		 * Controller is waiting for an INIT message from the other player
		 */
		InitWaiting,
		
		/**
		 * Controller is ready to start a new game
		 * 
		 * If gameState != null, we have send a start game request.
		 */
		Ready,
		
		/**
		 * Controller has received and verified a start game request from the other controller
		 */
		ReadyPlayReceived,
		
		/**
		 * A game is in progress
		 */
		Playing,
	}
	
	/**
	 * Creates and initializes a new game controller.
	 * 
	 * One of the controllers should be the master.
	 * This is used during startup to decide who goes first.
	 * Generally, you should make the "hosting" computer the master.
	 * 
	 * @param channel channel to communicate with
	 * @param output system to output game information to
	 * @param myName this controller's player name
	 * @param isMaster true if this controller is the master
	 */
	public GameController(SocketChannel channel, final GameOutput output,
			String myName, boolean isMaster) throws IOException
	{
		//validate parameters
		if(output == null)
		{
			throw new IllegalArgumentException("output is null");
		}
		else if(myName == null)
		{
			throw new IllegalArgumentException("myName is null");
		}
		
		//Encode name
		ByteBuffer nameBuf = encodeString(myName, 246);
		if(nameBuf == null)
		{
			throw new IllegalArgumentException("player name is too long or malformed");
		}
		
		//Calculate master status
		if(isMaster)
		{
			masterStatus = (new Random().nextBoolean()) ? MASTER_ME_FIRST : MASTER_YOU_FIRST;
		}
		else
		{
			masterStatus = NOT_MASTER;
		}
		
		//Store output and name
		this.output = output;
		playerNames[isMaster ? 0 : 1] = myName;
		playerNum = isMaster ? 1 : 2;
		
		//Create message controller
		conn = new SwingMessageConnection(channel)
		{
			@Override
			protected void eventSwingError(Exception e)
			{
				raiseGameError(e);
			}

			@Override
			protected void eventSwingRead(ByteBuffer buffer) throws Exception
			{
				//Ignore empty messages
				if(buffer.remaining() == 0)
				{
					return;
				}
				
				//Accept INIT when in correct state only
				if(controlState == ControllerState.InitWaiting)
				{
					if(buffer.get() == CMD_INIT)
					{
						//Get version
						if(buffer.getInt() != PROTOCOL_VERSION)
						{
							throw new GameControllerException("Both players must be using the same Squares version");
						}
						
						//Get master status
						int otherMasterStatus = buffer.getInt();
						
						//Validate statuses
						if((masterStatus == NOT_MASTER && otherMasterStatus == NOT_MASTER) ||
							(masterStatus != NOT_MASTER && otherMasterStatus != NOT_MASTER))
						{
							throw new GameControllerException("Failed to select master computer");
						}
						
						//Get player name
						String otherName = decodeString(buffer);
						
						//Is it my turn first?
						if(masterStatus == NOT_MASTER)
						{
							player1First = (otherMasterStatus == MASTER_YOU_FIRST);
						}
						else
						{
							player1First = (masterStatus == MASTER_ME_FIRST);
						}
						
						//Store player names
						if(masterStatus == NOT_MASTER)
						{
							playerNames[1] = otherName;
						}
						else
						{
							playerNames[0] = otherName;
						}
						
						//Ready to start
						controlState = ControllerState.Ready;
						
						//TODO raise ready event
					}
					else
					{
						throw new GameControllerException("Unexpected INIT message received");
					}
					
					return;
				}
				
				//What command?
				switch(buffer.get())
				{
					case CMD_PLAY:
						//Other player is ready to start
						if(controlState != ControllerState.Ready)
						{
							//Illegal request
							throw new GameControllerException("Unexpected PLAY message received");
						}
						
						//Validate message details
						boolean otherPlayer1First = (buffer.get() != 0);
						int otherScore0 = buffer.getInt();
						int otherScore1 = buffer.getInt();
						
						if(player1First != otherPlayer1First || otherScore0 != score[0] || otherScore1 != score[1])
						{
							//Inconsistancy
							throw new GameControllerException("Data Inconsistency (hacking attempt?)");
						}
						
						//Continue
						if(gameState == null)
						{
							//Mark received
							controlState = ControllerState.ReadyPlayReceived;
						}
						else
						{
							//Game has started
							controlState = ControllerState.Playing;
							
							//TODO Notify output
						}
						
						break;
						
					case CMD_WIN:
						//Other player has claimed the win
						if(controlState != ControllerState.Playing)
						{
							//Illegal request
							throw new GameControllerException("Unexpected WIN message received");
						}
						
						//Can they win?
						// The ^3 here swaps 1 with 2
						if(gameState.canWinNow(playerNum ^ 3))
						{
							//Game ended
							gameEnded(true, true);
						}
						else
						{
							throw new GameControllerException("Data Inconsistency (hacking attempt?)");
						}
						
						break;
						
					case CMD_SURRENDER:
						//Other player has surrended
						if(controlState != ControllerState.Playing)
						{
							//Ignore
							break;
						}

						//Game ended
						gameEnded(false, true);
						break;
				
					case CMD_CHAT:
						//Output chat message
						output.gameChat(decodeString(buffer));
						break;
				}
			}

			@Override
			protected void eventSwingClosed() throws Exception
			{
				// TODO Auto-generated method stub
				
			}
		};
		
		//Send INIT message
		ByteBuffer buf = ByteBuffer.allocate(9 + nameBuf.limit());
		buf.put(CMD_INIT);
		buf.putInt(PROTOCOL_VERSION);
		buf.putInt(masterStatus);
		buf.put(nameBuf);
		buf.flip();
		
		conn.sendMsg(buf);
	}
	
	/**
	 * Begins a new game of squares
	 */
	public void startGame()
	{
		//Must be ready first
		if(controlState != ControllerState.Ready ||
				controlState != ControllerState.ReadyPlayReceived || gameState != null)
		{
			throw new IllegalStateException("controller is not ready to start a new game");
		}
		
		//Create new game state
		gameState = new GameState(8, player1First);
		
		//Send PLAY request
		ByteBuffer buf = ByteBuffer.allocate(10);
		buf.put(CMD_PLAY);
		buf.put((byte) (player1First ? 1 : 0));
		buf.putInt(score[0]);
		buf.putInt(score[1]);
		buf.flip();
		
		sendMsgSecure(buf);
		
		//Ready to play now?
		if(controlState == ControllerState.ReadyPlayReceived)
		{
			//Game has started
			controlState = ControllerState.Playing;
			
			//TODO Notify output
		}
	}
	
	/**
	 * Attempts to win the current game
	 *
	 * If you're opponent cannot possibly get enough squares, this allows you to win immediately
	 *
	 * @return false if you cannot win at this time
	 */
	public boolean win()
	{
		//In a game?
		if(controlState != ControllerState.Playing)
		{
			throw new IllegalStateException("controller is not currently playing a game");
		}
		
		//Can win?
		if(gameState.canWinNow(playerNum))
		{
			//Send WIN command
			sendMsgSecure(ByteBuffer.wrap(new byte[] { CMD_WIN }));

			//Game ended
			gameState = null;
			controlState = ControllerState.Ready;

			//Game ended
			gameEnded(true, true);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Surrenders the current game
	 * 
	 * If a game is in progress, you can always do this
	 */
	public void surrender()
	{
		//In a game?
		if(controlState != ControllerState.Playing)
		{
			throw new IllegalStateException("controller is not currently playing a game");
		}
		
		//Send surrender
		sendMsgSecure(ByteBuffer.wrap(new byte[] { CMD_SURRENDER }));
		
		//Game ended
		gameEnded(false, true);
	}
	
	/**
	 * Closes the connection with the other game controller
	 * 
	 * If playing a game, surrenders first
	 */
	public void close()
	{
		//Ignore if closed
		if(conn.isConnected())
		{
			//Surrender first if playing
			if(controlState == ControllerState.Playing)
			{
				try
				{
					conn.sendMsg(ByteBuffer.wrap(new byte[] { CMD_SURRENDER }));
				}
				catch(IOException e)
				{
				}
			}
			
			try
			{	
				//Close connection
				conn.close();
			}
			catch(IOException e)
			{
			}
		}
	}
	
	public boolean move()
	{
		//
	}
	
	/**
	 * Sends some chat text to the opponent
	 *
	 * The opponent must be connected before text is sent
	 *
	 * @param text text to send
	 * @return false if the text was too long or malformed
	 */
	public boolean chat(String text)
	{
		//Encode message
		ByteBuffer chatBuf = encodeString(text, 254);
		if(chatBuf == null)
		{
			return false;
		}
		
		//Construct chat message
		ByteBuffer buf = ByteBuffer.allocate(1 + chatBuf.limit());
		buf.put(CMD_CHAT);
		buf.put(chatBuf);
		buf.flip();
		
		//Send message
		sendMsgSecure(buf);
		return true;
	}
	
	/**
	 * Called to end the current game
	 * 
	 * @param iWon true if i won
	 * @param premature true if the game ended prematurly
	 */
	private void gameEnded(boolean iWon, boolean premature)
	{
		//Wipe game state
		gameState = null;
		controlState = ControllerState.Ready;
		
		//Update scores
		if(iWon)
		{
			score[playerNum - 1]++;
		}
		else
		{
			score[(playerNum ^ 3) - 1]++;
		}
		
		//Notify output
		output.gameEnd(iWon, premature, score[0], score[1]);
	}
	
	/**
	 * Raises the game error e to the game output
	 * 
	 * This will close everything down (all errors are unrecoverable)
	 * 
	 * @param e exception to report to output
	 */
	private void raiseGameError(Exception e)
	{
		GameControllerException wrapped;
		
		//What sort of error?
		if(e instanceof GameControllerException)
		{
			//Do not wrap
			wrapped = (GameControllerException) e;
		}
		else if(e instanceof IOException)
		{
			//Network error
			wrapped = new GameControllerException("Network Error:\n" + e.getMessage(), e);
		}
		else if(e instanceof BufferUnderflowException)
		{
			//Protocol error - not enough bytes
			wrapped = new GameControllerException("Bad message from other controller", e);
		}
		else if(e instanceof CharacterCodingException)
		{
			//Malformed String
			wrapped = new GameControllerException("Malformed string received", e);
		}
		else
		{
			//Generic wrap
			wrapped = new GameControllerException("Error:\n" + e.getMessage(), e);
		}
		
		//Report error to game output
		this.output.gameError(wrapped);
		
		//Close connection (ignore any errors)
		try
		{
			this.conn.close();
		}
		catch (IOException e1)
		{
		}
	}
	
	/**
	 * Sends a message over the connection while handling any exceptions
	 * 
	 * @param buffer message to send
	 * @return true if no exceptions were thrown
	 */
	private boolean sendMsgSecure(ByteBuffer buffer)
	{
		//Must not be waiting for init
		if(controlState == ControllerState.InitWaiting)
		{
			//Not finished connecting
			raiseGameError(new IllegalStateException("controller has not finished connection process"));
			return false;
		}
		
		try
		{
			//Send message
			this.conn.sendMsg(buffer);
		}
		catch (IOException e)
		{
			//Raise error
			raiseGameError(e);
			return false;
		}
		
		return true;
	}
	
	private static final CharsetEncoder strEncoder = Charset.forName("UTF-8").newEncoder();
	private static final CharsetDecoder strDecoder = Charset.forName("UTF-8").newDecoder();
	
	/**
	 * Encodes the given string using UTF-8 into the given byte buffer
	 * 
	 * This function may return false if:
	 * - The string is too large
	 * - The string is malformed (invalid unicode code point)
	 * 
	 * @param str string to encode
	 * @param maxLen maximum buffer length in bytes
	 * @return true if the encode was sucessful or false if not
	 */
	private static ByteBuffer encodeString(String str, int maxLen)
	{
		//Verify string length first
		if(str.length() > maxLen)
		{
			return null;
		}
		
		try
		{
			//Encode buffer
			ByteBuffer buf = strEncoder.encode(CharBuffer.wrap(str));
			
			//Check length
			if(buf.limit() <= maxLen)
			{
				return buf;
			}
		}
		catch (CharacterCodingException e)
		{
			//Ignore and return null
		}
		
		return null;
	}
	
	/**
	 * Decodes the given buffer (encoded in UTF-8) into a string
	 * 
	 * @param buffer buffer to decode
	 * @return the string
	 */
	private static String decodeString(ByteBuffer buffer) throws CharacterCodingException
	{
		//Decode buffer and return string
		return strDecoder.decode(buffer).toString();
	}
}
