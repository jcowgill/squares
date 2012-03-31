package uk.org.cowgill.james.squares;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * The top level window for the squares program
 * 
 * @author james
 */
public class SquaresWindow extends JFrame
{
	private static final long serialVersionUID = 1L;

	public SquaresWindow()
	{
		super("Squares");
		
		setContentPane(new PanelStartup(this));
		pack();
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationByPlatform(true);
	}
	
	public static void main(String[] args)
	{
		//Start main window
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run()
			{
				new SquaresWindow().setVisible(true);
			}
		});
	}
}
