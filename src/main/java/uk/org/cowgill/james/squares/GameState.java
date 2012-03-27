package uk.org.cowgill.james.squares;

/**
 * Contains information about a single game of squares
 * 
 * @author James
 */
public class GameState
{
	private boolean player1Turn;
	private int size;
	private int[] score = new int[2];
	
	private byte[][] squareValue;
	private boolean[][] topLine;
	private boolean[][] leftLine;
	
	/**
	 * The result of a move operation
	 */
	public enum MoveResult
	{
		/**
		 * Move was illegal - no change in game state
		 */
		Illegal,
		
		/**
		 * Move was OK, other player's turn
		 */
		Ok,
		
		/**
		 * Move was OK, you're turn again
		 */
		OkAgain,
	}
	
	/**
	 * Creates and initializes a new game state
	 * 
	 * @param size size of game (width and height)
	 * @param player1Starts true if player 1 starts first (rather than player 2)
	 */
	public GameState(int size, boolean player1Starts)
	{
		//Initialize game
		this.size = size;
		player1Turn = player1Starts;
		squareValue = new byte[size][size];
		topLine 	= new boolean[size + 1][size + 1];
		leftLine 	= new boolean[size + 1][size + 1];
	}
	
	/**
	 * Returns true if it is player 1's turn
	 * @return
	 */
	public boolean isPlayer1Turn()
	{
		return player1Turn;
	}
	
	/**
	 * Makes a move on behalf of the given player
	 * 
	 * This method is safe from bogus (unchecked) inputs.
	 * 
	 * @param player player making the move
	 * @param x x coordinate (starts at 0)
	 * @param y y coordinate (starts at 0)
	 * @param isLeft true if the move is on the left line of the given coordinate
	 * @return the result of the move
	 */
	public MoveResult move(int player, int x, int y, boolean isLeft)
	{
		MoveResult res = MoveResult.Ok;
		
		//Initial parameter validation
		if((player != 1 && player != 2) || x < 0 || y < 0 || x > size || y > size)
		{
			return MoveResult.Illegal;
		}
		
		//Must be correct player's move
		if((player1Turn && player == 2) || (!player1Turn && player == 1))
		{
			return MoveResult.Illegal;
		}
		
		//Do move
		if(isLeft)
		{
			//Move already made (or invalid)?
			if(y == size || leftLine[x][y])
			{
				return MoveResult.Illegal;
			}
			
			//Set line
			leftLine[x][y] = true;
			
			//Check square to the left
			if(x != 0 && (leftLine[x - 1][y] && topLine[x - 1][y] && topLine[x - 1][y + 1]))
			{
				//Square to the left is coloured
				squareValue[x - 1][y] = (byte) player;
				score[player - 1]++;
				res = MoveResult.OkAgain;
			}
			
			//Check square to the right
			if(x != size && (leftLine[x + 1][y] && topLine[x][y] && topLine[x][y + 1]))
			{
				//Square to the right is coloured
				squareValue[x][y] = (byte) player;
				score[player - 1]++;
				res = MoveResult.OkAgain;
			}
		}
		else
		{
			//Move already made (or invalid)?
			if(x == size || topLine[x][y])
			{
				return MoveResult.Illegal;
			}
			
			//Set line
			topLine[x][y] = true;
			
			//Check square to the top
			if(y != 0 && (topLine[x][y - 1] && leftLine[x][y - 1] && leftLine[x + 1][y - 1]))
			{
				//Square to the top is coloured
				squareValue[x][y - 1] = (byte) player;
				score[player - 1]++;
				res = MoveResult.OkAgain;
			}
			
			//Check square to the bottom
			if(y != size && (topLine[x][y + 1] && leftLine[x][y] && leftLine[x + 1][y]))
			{
				//Square to the bottom is coloured
				squareValue[x][y] = (byte) player;
				score[player - 1]++;
				res = MoveResult.OkAgain;
			}
		}
		
		//If we're here, the move was good
		player1Turn = !player1Turn;
		return res;
	}
	
	/**
	 * Returns true if the given player can win immediately
	 * 
	 * @param player player to check
	 * @return true if the player can win NOW
	 */
	public boolean canWinNow(int player)
	{
		//Must be their turn and have enough squares
		if(player1Turn == (player == 1))
		{
			return 2 * score[player - 1] > size * size;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Returns the game dimensions
	 */
	public int getSize()
	{
		return size;
	}
	
	/**
	 * Returns true if the game has completed (no more moves can be made)
	 */
	public boolean isComplete()
	{
		return score[0] + score[1] == size * size;
	}
	
	/**
	 * Gets a player's score (number of filled squares)
	 * 
	 * @param player the player to get teh score from
	 * @return their score
	 */
	public int getScore(int player)
	{
		return score[player - 1];
	}
	
	/**
	 * Gets the colour of the square at the given coordinate
	 * 
	 * @param x x coordinate (starts at 0)
	 * @param y y coordinate (starts at 0)
	 * @return 0 if blank, 1 for player 1, and 2 for player 2
	 */
	public int getSquareColour(int x, int y)
	{
		return squareValue[x][y];
	}
	
	/**
	 * Gets whether the top line has been filled in
	 * 
	 * @param x x coordinate (starts at 0)
	 * @param y y coordinate (starts at 0)
	 */
	public boolean getTopLine(int x, int y)
	{
		return topLine[x][y];
	}

	/**
	 * Gets whether the left line has been filled in
	 * 
	 * @param x x coordinate (starts at 0)
	 * @param y y coordinate (starts at 0)
	 */
	public boolean getLeftLine(int x, int y)
	{
		return leftLine[x][y];
	}
}
