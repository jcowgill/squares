package uk.org.cowgill.james.squares;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Provides a wrapper around a SocketChannel which allows sending and
 * receiving messages using another Thread.
 *
 * @author James
 */
public abstract class MessageConnection implements Runnable
{
	private Selector selector;
	private SocketChannel socket;
	private Thread readerThread;

	/**
	 * Creates a new message connection using the given socket and callback interface
	 *
	 * @param socket the socket this connection will control
	 */
	public MessageConnection(SocketChannel socket) throws IOException
	{
		//Store socket
		this.socket = socket;
		
		//Must be open
		if(!this.socket.isConnected())
		{
			throw new NotYetConnectedException();
		}
		
		//Disable nagle
		this.socket.socket().setTcpNoDelay(true);
		
		//Disable blocking (using a selector)
		this.socket.configureBlocking(false);
		
		//Open selector and register channel
		this.selector = Selector.open();
		this.socket.register(this.selector, SelectionKey.OP_READ, null);
		
		//Start reader thread
		readerThread = new Thread(this, "MessageConnection Thread");
		readerThread.setDaemon(true);
		readerThread.start();
	}
	
	/**
	 * Sends the message in the given buffer over the connection
	 *
	 * @param buffer the content of the message to send (up to 255 bytes)
	 */
	public void sendMsg(ByteBuffer buffer) throws IOException
	{
		//Check max length
		if(buffer.remaining() > Byte.MAX_VALUE)
		{
			throw new IOException("MessageConnection can only send messages up to 255 bytes long");
		}
		
		//Send length prefix
		ByteBuffer lenBuf = ByteBuffer.wrap(new byte[] { (byte) buffer.remaining() });
		socket.write(lenBuf);
		
		//Send  data
		socket.write(buffer);
	}
	
	/**
	 * Returns true if this connection is connected
	 */
	public boolean isConnected()
	{
		return this.socket.isConnected();
	}
	
	/**
	 * Closes the socket associated with this connection
	 *
	 * This does not raise the closed event
	 */
	public void close() throws IOException
	{
		//Close the selector which will initiate the close from the thread
		this.selector.close();
	}
	
	/**
	 * Event which occurs when an error is thrown on the <b>reader thread</b>
	 *
	 * Errors which occur when calling sendMsg / close are thrown and not handled by this method.
	 *
	 * The connection is automatically closed after this returns.
	 *
	 * @param e the exception raised
	 */
	protected abstract void eventError(Exception e);
	
	/**
	 * Event which occurs when a new message is avaliable
	 *
	 * The message is stored in the buffer. The position and limit are set accordingly.
	 *
	 * Any exceptions thrown are forwarded to eventError.
	 *
	 * @param buffer buffer containing data which was read
	 */
	protected abstract void eventRead(ByteBuffer buffer) throws Exception;
	
	/**
	 * Event which occurs when a graceful close has happened caused by the other connection.
	 *
	 * Any exceptions thrown are forwarded to eventError.
	 */
	protected abstract void eventClosed() throws Exception;
	
	@Override
	public void run()
	{
		//Create storage buffer
		byte[] rawBuffer = new byte[512];
		ByteBuffer buffer = ByteBuffer.wrap(rawBuffer);
		
		//Start selection loop
		try
		{
			for(;;)
			{
				if(this.selector.select() != 0)
				{
					//Clear selection kets
					this.selector.selectedKeys().clear();
					
					//Process read request
					int bytes = this.socket.read(buffer);
					if(bytes == -1)
					{
						//EOF reached and the current message has not been processed
						// Raise close event and exit
						eventClosed();
						break;
					}
					else if(bytes != 0)
					{
						//Ignore request if bytes = 0 (nothing todo)
						
						//Process messages
						int pos = 0;
						while(pos + rawBuffer[pos] < buffer.position())
						{
							//There is a message at the start of the buffer, so send it on
							eventRead(ByteBuffer.wrap(rawBuffer, pos + 1, rawBuffer[pos]).asReadOnlyBuffer());
							
							//Advance position
							pos += rawBuffer[pos] + 1;
						}
						
						//Copy the rest of the array back to the beginning
						System.arraycopy(rawBuffer, pos, rawBuffer, 0, buffer.position() - pos);
						
						//Update buffer position
						buffer.position(buffer.position() - pos);
					}
				}
			}
		}
		catch(ClosedSelectorException e)
		{
			//Ignore and fallthrough
		}
		catch(Exception e)
		{
			//Notify of the exception
			eventError(e);
		}
		finally
		{
			//Shutdown then close the channel
			try
			{
				this.selector.close();
				this.socket.socket().setSoLinger(true, 10);
				this.socket.socket().shutdownInput();
				this.socket.socket().shutdownOutput();
			}
			catch(IOException e)
			{
				//Ignore this
			}
			finally
			{
				try
				{
					this.socket.close();
				}
				catch(IOException e)
				{
					//Ignore errors
				}
			}
		}
	}
}
