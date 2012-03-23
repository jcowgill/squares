package uk.org.cowgill.james.squares;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

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

/**
 * The JPanel which displays the visual elements of the game
 * 
 * @author james
 */
public class PanelGame extends JPanel
{
	private static final long serialVersionUID = 1L;

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
	public PanelGame(JRootPane rootPane)
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
	 * Sets the game state object controlling the game
	 * 
	 * @param state game state
	 */
	public void setGameState(GameState state)
	{
		gameCanvas.state = state;
		updateGameState();
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
	private static class GameCanvas extends JPanel
	{
		private static final long serialVersionUID = 1L;
		
		//State of game
		public GameState state; 

		public GameCanvas()
		{
			super(true);
			setPreferredSize(new Dimension(300, 300));
			setBorder(new LineBorder(Color.BLACK));
			setBackground(Color.BLUE);
		}
	}
}
