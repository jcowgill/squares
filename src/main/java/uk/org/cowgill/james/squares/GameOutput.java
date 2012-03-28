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
	 * Called when the connection to the other controller is established
	 */
	public void gameStartup();
	
	/**
	 * Called when the connection is closed properly
	 */
	public void gameClosed();
	
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
	
	/**
	 * Called when a new game has been started
	 * 
	 * @param state the state of the game (do not modify)
	 */
	public void gameStart(GameState state);
	
	/**
	 * Called after a move has been made
	 * 
	 * @param state the state of the game (do not modify)
	 * @param yourMove it is your move now
	 */
	public void gameMove(GameState state, boolean yourMove);
	
	/**
	 * Called when the game has been completed
	 * 
	 * @param youWon true if you won
	 * @param premature true if the game was ended early (ie: clicking WIN or SURRENDER)
	 * @param player1Score the score for player 1
	 * @param player2Score the score for player 2
	 */
	public void gameEnd(boolean youWon, boolean premature, int player1Score, int player2Score);
}
