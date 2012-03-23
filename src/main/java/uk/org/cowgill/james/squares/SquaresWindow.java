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

	public static void main(String[] args)
	{
		//Test panel using frame
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run()
			{
				JFrame frame = new JFrame("Squares Test Frame");
				frame.setContentPane(new PanelGame(frame.getRootPane()));
				frame.pack();
				frame.setResizable(false);
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				frame.setVisible(true);
			}
		});
	}
}
