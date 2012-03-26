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
 * This class controls the game logic and communications with
 * another GameController over the network.
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
	 * Names of players
	 */
	private String[] playerNames = new String[2];
	
	/**
	 * The current state of the game
	 */
	private GameState state = GameState.InitWaiting;
	
	/**
	 * True if I started first this game (or for the next game if state == Ready)
	 */
	private boolean myTurnFirst;
	
	/**
	 * The current state of the game
	 * 
	 * @author James
	 */
	private enum GameState
	{
		/**
		 * Controller is waiting for an INIT message from the other player
		 */
		InitWaiting,
		
		/**
		 * Controller is ready to start a new game
		 */
		Ready,
		
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
		playerNames[0] = myName;
		
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
				if(state == GameState.InitWaiting)
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
							myTurnFirst = (otherMasterStatus == MASTER_YOU_FIRST);
						}
						else
						{
							myTurnFirst = (otherMasterStatus == MASTER_ME_FIRST);
						}
						
						//Store player names
						if(myTurnFirst)
						{
							playerNames[1] = otherName;
						}
						else
						{
							playerNames[1] = playerNames[0];
							playerNames[0] = otherName;
						}
						
						//Ready to start
						state = GameState.Ready;
						
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
	
	public void startGame()
	{
		//
	}
	
	/**
	 * Attempts to win the current game
	 *
	 * If you're opponent cannot possibly get enough squares, this allows you to win immediately
	 *
	 * @param text text to send
	 * @return false if you cannot win the game immediately
	 */
	public boolean win()
	{
		//
	}
	
	public void surrender()
	{
		//
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
			if(state == GameState.Playing)
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
		if(state == GameState.InitWaiting)
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
