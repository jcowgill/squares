package uk.org.cowgill.james.squares;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import javax.swing.SwingUtilities;

/**
 * A wrapper for MessageConnection which forwards events to the swing Event Thread
 *
 * @author James
 */
public abstract class SwingMessageConnection extends MessageConnection
{
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
	protected abstract void eventSwingClosed();
	
	@Override
	protected abstract void eventError(Exception e)
	{
		//Forward
		SwingUtilities.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				eventSwingError(e);
			}
		});
	}
	
	@Override
	protected abstract void eventRead(ByteBuffer buffer) throws Exception
	{
		try
		{
			//Forward
			SwingUtilities.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					eventRead(buffer);
				}
			});
		}
		catch(InvocationTargetException e)
		{
			//Rethrow exception
			throw e.getTargetException();
		}
	}
	
	@Override
	protected abstract void eventClosed()
	{
		//Forward
		SwingUtilities.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				eventClosed();
			}
		});
	}
}
