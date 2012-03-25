package uk.org.cowgill.james.squares;

import java.io.IOException;
import java.nio.BufferOverflowException;
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
 * This class is not thread safe. ll calls to this class MUST be
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
	public GameController(SocketChannel channel, GameOutput output,
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
				// TODO Auto-generated method stub
				
			}

			@Override
			protected void eventSwingRead(ByteBuffer buffer) throws Exception
			{
				//Ignore empty messages
				if(buffer.remaining() == 0)
				{
					return;
				}
				
				try
				{
					//What command?
					switch(buffer.get())
					{
					case CMD_INIT:
						//
					}
				}
				catch(BufferOverflowException e)
				{
					//
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
	 * @return the string or null if the string is malformed
	 */
	private static String decodeString(ByteBuffer buffer)
	{
		try
		{
			//Decode buffer and return string
			return strDecoder.decode(buffer).toString();
		}
		catch (CharacterCodingException e)
		{
			//Return null (error)
			return null;
		}
	}
}
