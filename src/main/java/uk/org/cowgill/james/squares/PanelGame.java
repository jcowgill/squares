package uk.org.cowgill.james.squares;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.SocketChannel;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;

/**
 * The JPanel which displays the visual elements of the game
 * 
 * @author james
 */
public class PanelGame extends JPanel implements GameOutput
{
	private static final long serialVersionUID = 1L;

	//Controller
	private final GameController ctrl;
	
	//Form controls
	private final JLabel lblPlayer[] = new JLabel[2];
	private final JLabel lblScore[] = new JLabel[2];
	private final JButton sendButton = new JButton("Send");
	private final JTextArea chatOut = new JTextArea();
	private final JTextField textField;
	private final GameCanvas gameCanvas = new GameCanvas();
	
	/**
	 * Creates a panel for the game screen
	 * 
	 * @param window the main squares window
	 * @param channel connection to other player
	 * @param myName my player name
	 * @param isMaster true if this computer is the host
	 */
	public PanelGame(JFrame window, SocketChannel channel, String myName, boolean isMaster) throws IOException
	{
		setLayout(new BorderLayout(0, 0));
		
		//Top panel
		JPanel panel1 = new JPanel();
		panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		lblPlayer[0] = new JLabel("Player 1");
		lblPlayer[0].setForeground(Color.BLUE);
		lblPlayer[0].setHorizontalAlignment(SwingConstants.RIGHT);
		panel1.add(lblPlayer[0]);
		
		lblScore[0] = new JLabel("0");
		lblScore[0].setForeground(Color.BLUE);
		lblScore[0].setHorizontalAlignment(SwingConstants.CENTER);
		lblScore[0].setPreferredSize(new Dimension(32, 15));
		panel1.add(lblScore[0]);
		
		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setPreferredSize(new Dimension(2, 15));
		panel1.add(separator);
		
		lblScore[1] = new JLabel("0");
		lblScore[1].setForeground(Color.RED);
		lblScore[1].setHorizontalAlignment(SwingConstants.CENTER);
		lblScore[1].setPreferredSize(new Dimension(32, 15));
		panel1.add(lblScore[1]);

		lblPlayer[1] = new JLabel("Player 2");
		lblPlayer[1].setForeground(Color.RED);
		panel1.add(lblPlayer[1]);
		
		add(panel1, BorderLayout.NORTH);
		
		//Bottom panel
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane(chatOut);
		scrollPane.setPreferredSize(new Dimension(3, 100));
		panel2.add(scrollPane, BorderLayout.NORTH);
		
		textField = new JTextField();
		panel2.add(textField, BorderLayout.CENTER);
		textField.setColumns(10);
		
		panel2.add(sendButton, BorderLayout.EAST);
		
		add(panel2, BorderLayout.SOUTH);
		
		//Game canvas
		add(gameCanvas, BorderLayout.CENTER);
		
		//Default button
		window.getRootPane().setDefaultButton(sendButton);
		
		//Create controller
		this.ctrl = new GameController(channel, this, myName, isMaster);
		
		//Send button handler
		this.sendButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				//Button pressed
				String text = textField.getText().trim();
				textField.setText("");
				
				//Builtin command?
				if(text.startsWith("/win"))
				{
					//Send win command
					if(!ctrl.win())
					{
						chatOut.append("\nYou cannot win yet.");
					}
				}
				else if(text.startsWith("/surrender"))
				{
					//Surrender
					ctrl.surrender();
				}
				else if(text.startsWith("/play"))
				{
					//Start new game
					if(!ctrl.isPlaying())
					{
						ctrl.startGame();
					}
				}
				else
				{
					//Send chat text
					ctrl.chat(text);
				}
			}
		});
		
		//Disable controls for initialization
		sendButton.setEnabled(false);
		textField.setEnabled(false);
		chatOut.setEditable(false);
		
		//Print init message
		chatOut.append("Connecting...");
	}
	
	@Override
	public void gameStartup()
	{
		//Connected
		lblPlayer[0].setText(ctrl.getPlayerName(1));
		lblPlayer[1].setText(ctrl.getPlayerName(2));
		chatOut.append("\nConnected.\nType /play to start a new game.");
		sendButton.setEnabled(true);
		textField.setEnabled(true);
	}

	@Override
	public void gameClosed()
	{
		//Connection Closed
		chatOut.append("\nConnection Closed.");
		sendButton.setEnabled(false);
		textField.setEnabled(false);
	}

	@Override
	public void gameError(GameControllerException e)
	{
		//Connection error
		// Get exception text
		StringWriter writer = new StringWriter();
		PrintWriter pWriter = new PrintWriter(writer);
		e.printStackTrace(pWriter);
		pWriter.close();
		
		//Print message
		chatOut.append("\nException thrown by game controller:\n" + writer.toString());
		sendButton.setEnabled(false);
		textField.setEnabled(false);
	}

	@Override
	public void gameChat(String str)
	{
		chatOut.append("\n> ");
		chatOut.append(str);
	}

	@Override
	public void gameStart(GameState state, boolean yourMove)
	{
		//New game has started
		chatOut.append("\nGame Started" + 
						"\n Click the lines on the screen to make your move" +
						"\n /win allows you to win now if your opponent cannot possibly win" + 
						"\n /surrender allows you to surrender\n");
		this.gameCanvas.setGameState(state, yourMove);
	}

	@Override
	public void gameMove(GameState state, boolean yourMove)
	{
		//Update game canvas
		gameCanvas.moveComplete(yourMove);
	}

	@Override
	public void gameEnd(boolean youWon, boolean premature, int player1Score,
			int player2Score)
	{
		//Game status
		if(youWon)
		{
			chatOut.append("\nYou Won!");
		}
		else
		{
			chatOut.append("\nYou Lost.");
		}
		
		//Show score
		chatOut.append("\n The score is now " + player1Score + "-" + player2Score);
		chatOut.append("\n Type /play to start again");
	}
	
	/**
	 * The canvas used to draw the main game content
	 * 
	 * @author james
	 */
	private class GameCanvas extends JPanel
	{
		private static final long serialVersionUID = 1L;
		
		//Position and Size Constants
		private static final int PADDING = 16;
		private static final int DOT_SIZE = 10;
		private static final int SQUARE_SIZE = 48;
		private static final int SQUARES = 8;

		//Game area size without padding
		private static final int CONTENT_SIZE = SQUARE_SIZE * SQUARES + DOT_SIZE;
		
		//Stroke used to draw lines with
		private final Stroke lineStroke = new BasicStroke(4);
		
		//Square colours
		private final Color[] squareColour = new Color[]{
				Color.LIGHT_GRAY, Color.BLUE, Color.RED
		};
		
		//Game State
		private GameState state;
		private boolean myMove;
		
		//Coordinates for line to draw mouse hovering on
		private int lastMouseX;
		private int lastMouseY;
		private boolean lastMouseIsLeft;
		private boolean lastMouseEnabled = false;
		
		/**
		 * Creates a new game canvas
		 */
		public GameCanvas()
		{
			super(true);
			
			//Setup panel
			setPreferredSize(new Dimension(PADDING * 2 + CONTENT_SIZE, PADDING * 2 + CONTENT_SIZE));
			setBorder(new LineBorder(Color.BLACK));
			setBackground(squareColour[0]);
			
			//Add line hovering handler
			MouseInputAdapter mouseListener = new MouseInputAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					//My turn?
					if(myMove)
					{
						//Ensure hover is in correct position
						mouseMoved(e);
						
						//Make the given move
						PanelGame.this.ctrl.move(
								GameCanvas.this.lastMouseX,
								GameCanvas.this.lastMouseY,
								GameCanvas.this.lastMouseIsLeft);
						
						//Refresh
						repaint();
					}
				}
				
				@Override
				public void mouseExited(MouseEvent e)
				{
					//Disable hover
					GameCanvas.this.lastMouseEnabled = false;
					repaint();
				}
				
				@Override
				public void mouseMoved(MouseEvent e)
				{
					//Ignore if it's not my move
					if(!myMove)
					{
						return;
					}
					
					//Calulate the square and where inside the square we are
					int x 		= (e.getX() - PADDING - DOT_SIZE / 2) / SQUARE_SIZE;
					int xInside	= (e.getX() - PADDING - DOT_SIZE / 2) % SQUARE_SIZE;
					int y 		= (e.getY() - PADDING - DOT_SIZE / 2) / SQUARE_SIZE;
					int yInside	= (e.getY() - PADDING - DOT_SIZE / 2) % SQUARE_SIZE;
					boolean isLeft;
					
					//Check for padding positions
					if(x == 8)
					{
						//Bottom right corner?
						if(y == 8)
						{
							//Which way to move up?
							if(xInside < yInside)
							{
								x--;
								isLeft = false;
							}
							else
							{
								y--;
								isLeft = true;
							}
						}
						else
						{
							//Force left
							isLeft = true;
						}
					}
					else
					{
						//Bottom side
						if(y == 8)
						{
							isLeft = false; 
						}
						else
						{
							//MAIN TESTER
							//Which of the 4 surrounding lines is the nearest?
							if(SQUARE_SIZE - xInside < yInside)
							{
								//Bottom right
								if(xInside < yInside)
								{
									//Bottom
									y++;
									isLeft = false;
								}
								else
								{
									//Right
									x++;
									isLeft = true;
								}
							}
							else
							{
								//On current square - to the left?
								isLeft = xInside < yInside;
							}							
						}
					}
					
					//Has it changed?
					if(!GameCanvas.this.lastMouseEnabled ||
							x != GameCanvas.this.lastMouseX ||
							y != GameCanvas.this.lastMouseY ||
							isLeft != GameCanvas.this.lastMouseIsLeft)
					{
						//Store last mouse location
						GameCanvas.this.lastMouseX = x;
						GameCanvas.this.lastMouseY = y;
						GameCanvas.this.lastMouseIsLeft = isLeft;
						GameCanvas.this.lastMouseEnabled = true;
						
						//Repaint
						repaint();
					}
				}
			};
			
			addMouseListener(mouseListener);
			addMouseMotionListener(mouseListener);
		}
		
		/**
		 * Should be called after a move is completed
		 * 
		 * @param myMove true if it is my move now
		 */
		public void moveComplete(boolean myMove)
		{
			this.myMove = myMove;
			repaint();
		}
		
		/**
		 * Sets the game state of this canvas
		 * 
		 * @param state the new state object or null to display nothing
		 * @param myMove true if it is my move now
		 */
		public void setGameState(GameState state, boolean myMove)
		{
			this.state = state;
			this.myMove = myMove;
			if(!myMove)
			{
				//Disable hover
				this.lastMouseEnabled = false;
			}
			
			repaint();
		}
		
		@Override
		protected void paintComponent(Graphics gOld)
		{
			//Paint base of object
			super.paintComponent(gOld);
			
			//Repaint game window
			if(state != null)
			{
				Graphics2D g = (Graphics2D) gOld.create(PADDING, PADDING, CONTENT_SIZE, CONTENT_SIZE);

				//Fill squares
				for(int x = 0; x < 8; x++)
				{
					for(int y = 0; y < 8; y++)
					{
						g.setColor(squareColour[state.getSquareColour(x, y)]);
						g.fillRect(x * SQUARE_SIZE + DOT_SIZE / 2,
									y * SQUARE_SIZE + DOT_SIZE / 2,
									SQUARE_SIZE, SQUARE_SIZE);
					}
				}
				
				//Paint game lines
				g.setStroke(lineStroke);
				g.setColor(Color.DARK_GRAY);
				
				// Hover lines
				if(lastMouseEnabled)
				{
					int baseX = lastMouseX * SQUARE_SIZE + DOT_SIZE / 2;
					int baseY = lastMouseY * SQUARE_SIZE + DOT_SIZE / 2;
					
					if(lastMouseIsLeft)
					{
						//Draw left line
						g.drawLine(baseX, baseY, baseX, baseY + SQUARE_SIZE);
					}
					else
					{
						//Draw top line
						g.drawLine(baseX, baseY, baseX + SQUARE_SIZE, baseY);
					}
				}
				
				// Top lines
				g.setColor(Color.BLACK);
				for(int x = 0; x < 8; x++)
				{
					for(int y = 0; y <= 8; y++)
					{
						if(state.getTopLine(x, y))
						{
							g.drawLine(x * SQUARE_SIZE + DOT_SIZE / 2,
										y * SQUARE_SIZE + DOT_SIZE / 2,
										(x + 1) * SQUARE_SIZE + DOT_SIZE / 2,
										y * SQUARE_SIZE + DOT_SIZE / 2);
						}
					}
				}
				
				// Left lines
				for(int x = 0; x <= 8; x++)
				{
					for(int y = 0; y < 8; y++)
					{
						if(state.getLeftLine(x, y))
						{
							g.drawLine(x * SQUARE_SIZE + DOT_SIZE / 2,
										y * SQUARE_SIZE + DOT_SIZE / 2,
										x * SQUARE_SIZE + DOT_SIZE / 2,
										(y + 1) * SQUARE_SIZE + DOT_SIZE / 2);
						}
					}
				}
				
				//Paint dots
				for(int x = 0; x <= 8; x++)
				{
					for(int y = 0; y <= 8; y++)
					{
						g.fillOval(SQUARE_SIZE * x,
									SQUARE_SIZE * y,
									DOT_SIZE, DOT_SIZE);
					}
				}
			}
		}
	}
}
