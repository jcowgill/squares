package uk.org.cowgill.james.squares;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.swing.SwingUtilities;

/**
 * A wrapper for MessageConnection which forwards events to the swing Event Thread
 *
 * @author James
 */
public abstract class SwingMessageConnection extends MessageConnection
{
	/**
	 * Creates a new message connection using the given socket and callback interface
	 *
	 * @param socket the socket this connection will control
	 */
	public SwingMessageConnection(SocketChannel socket) throws IOException
	{
		super(socket);
	}

	/**
	 * Event which occurs when an error is thrown on the reader thread
	 *
	 * The connection is automatically closed after this returns.
	 *
	 * @param e the exception raised
	 */
	protected abstract void eventSwingError(Exception e);
	
	/**
	 * Event which occurs when a new message is avaliable
	 *
	 * The message is stored in the buffer. The position and limit are set accordingly.
	 *
	 * Any exceptions thrown are forwarded to eventError.
	 *
	 * @param buffer buffer containing data which was read
	 */
	protected abstract void eventSwingRead(ByteBuffer buffer) throws Exception;
	
	/**
	 * Event which occurs when a graceful close has happened caused by the other connection.
	 */
	protected abstract void eventSwingClosed() throws Exception;
	
	@Override
	protected void eventError(final Exception e)
	{
		//Forward
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					eventSwingError(e);
				}
			});
		}
		catch (InterruptedException e1)
		{
			//Throw invocation exception
			throw new SwingInvocationException(e1);
		}
		catch (InvocationTargetException e1)
		{
			//Throw invocation exception
			throw new SwingInvocationException(e1);
		}
	}
	
	@Override
	protected void eventRead(final ByteBuffer buffer) throws Exception
	{
		try
		{
			//Forward
			SwingUtilities.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						eventSwingRead(buffer);
					}
					catch (Exception e)
					{
						//Wrap exception
						throw new SwingInvocationException(e);
					}
				}
			});
		}
		catch(InvocationTargetException e)
		{
			//If invocation, rethrow
			if(e.getTargetException() instanceof SwingInvocationException)
			{
				throw ((SwingInvocationException) e.getTargetException()).getException();
			}
			else
			{
				//Must be an error
				throw (Error) e.getTargetException();
			}
		}
	}
	
	@Override
	protected void eventClosed() throws Exception
	{
		try
		{
			//Forward
			SwingUtilities.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						eventSwingClosed();
					}
					catch (Exception e)
					{
						//Wrap exception
						throw new SwingInvocationException(e);
					}
				}
			});
		}
		catch(InvocationTargetException e)
		{
			//If invocation, rethrow
			if(e.getTargetException() instanceof SwingInvocationException)
			{
				throw ((SwingInvocationException) e.getTargetException()).getException();
			}
			else
			{
				//Must be an error
				throw (Error) e.getTargetException();
			}
		}
	}
	
	/**
	 * Private class used to wrap exceptions which cannot be throw inside the Runnable interface
	 * 
	 * @author James
	 */
	private static final class SwingInvocationException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public SwingInvocationException(Exception e)
		{
			super(e);
		}
		
		public Exception getException()
		{
			return (Exception) getCause();
		}
	}
}
