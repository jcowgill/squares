package uk.org.cowgill.james.squares;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
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
public class PanelGame extends JPanel
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
	 * @param rootPane root pane to set default button of (or null)
	 */
	public PanelGame(JRootPane rootPane, GameController ctrl)
	{
		setLayout(new BorderLayout(0, 0));
		
		//Top panel
		JPanel panel1 = new JPanel();
		panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		lblPlayer[0] = new JLabel("Player 1");
		lblPlayer[0].setHorizontalAlignment(SwingConstants.RIGHT);
		panel1.add(lblPlayer[0]);
		
		lblScore[0] = new JLabel("0");
		lblScore[0].setHorizontalAlignment(SwingConstants.CENTER);
		lblScore[0].setPreferredSize(new Dimension(32, 15));
		panel1.add(lblScore[0]);
		
		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setPreferredSize(new Dimension(2, 15));
		panel1.add(separator);
		
		lblScore[1] = new JLabel("0");
		lblScore[1].setHorizontalAlignment(SwingConstants.CENTER);
		lblScore[1].setPreferredSize(new Dimension(32, 15));
		panel1.add(lblScore[1]);

		lblPlayer[1] = new JLabel("Player 2");
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
		if(rootPane != null)
		{
			rootPane.setDefaultButton(sendButton);
		}
		
		//Controller
		this.ctrl = ctrl;
	}

	/**
	 * Appends a line of text to the chat area
	 * 
	 * @param str text to append
	 */
	public void appendTextArea(String str)
	{
		chatOut.append("\n");
		chatOut.append(str);
	}
	
	/**
	 * Refreshes the game content to match the information in the game state object
	 */
	public void updateGameState()
	{
		//TODO Update names
		//TODO Update scores
		//TODO Repaint game area
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
		private GameState state = new GameState(8, true);
		
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
					//Ensure hover is in correct position
					mouseMoved(e);
					
					//TODO make move
					repaint();
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
		 * Sets the game state of this canvas
		 * 
		 * @param state the new state object or null to display nothing
		 */
		public void setGameState(GameState state)
		{
			this.state = state;
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
