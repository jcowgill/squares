package uk.org.cowgill.james.squares;

/**
 * The output for game events which need to be displayed or need attention
 *
 * All the methods will always be called from the EDT so thay can be used with swing.
 * 
 * @author James
 */
public interface GameOutput
{
	/**
	 * Called when an error occurs within the game controller
	 * 
	 * All errors reported here are unrecoverable.
	 * 
	 * @param e the exception which occured
	 */
	public void gameError(GameControllerException e);
	
	/**
	 * Called when chat text is received from the other controller
	 * 
	 * @param str the text that was sent
	 */
	public void gameChat(String str);
}
