package uk.org.cowgill.james.squares;

/**
 * Exception raised inside the game controller
 * 
 * @author James
 */
public class GameControllerException extends Exception
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new GameControllerException
	 */
	public GameControllerException()
	{
	}

	/**
	 * Creates a new GameControllerException
	 * 
	 * @param message exception message
	 */
	public GameControllerException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new GameControllerException
	 * 
	 * @param cause exception cause
	 */
	public GameControllerException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * Creates a new GameControllerException
	 * 
	 * @param message exception message
	 * @param cause exception cause
	 */
	public GameControllerException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
