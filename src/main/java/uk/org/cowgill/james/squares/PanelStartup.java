package uk.org.cowgill.james.squares;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * The JPanel which displays the options selection screen at startup
 * 
 * @author James
 */
public class PanelStartup extends JPanel implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private final JButton finishBtn;

	private final JRadioButton connectRadio;
	private final JRadioButton hostRadio;

	private final JTextField connectHost;
	private final JTextField connectPort;
	private final JTextField hostPort;

	private final JPanel bottomPane;
	
	private final JFrame window;

	/**
	 * Creates a new startup panel
	 * 
	 * @author james
	 */
	public PanelStartup(JFrame window)
	{
		super("Test");

		//Top panel with radio buttons
		JPanel top = new JPanel();
		connectRadio = new JRadioButton("Connect", true);
		connectRadio.addActionListener(this);
		top.add(connectRadio);

		hostRadio = new JRadioButton("Host");
		hostRadio.addActionListener(this);
		top.add(hostRadio);

		ButtonGroup radioButtons = new ButtonGroup();
		radioButtons.add(connectRadio);
		radioButtons.add(hostRadio);

		//Connect panel
		bottomPane = new JPanel(new CardLayout());
		JPanel connectPane = new JPanel();

		JPanel labelPane = new JPanel(new GridLayout(2, 1));
		labelPane.add(new JLabel("Hostname: ", JLabel.RIGHT));
		labelPane.add(new JLabel("Port: ", JLabel.RIGHT));

		JPanel textPane = new JPanel(new GridLayout(2, 1));
		connectHost = new JTextField(20);
		textPane.add(connectHost);
		connectPort = new JTextField("1503", 10);
		textPane.add(connectPort);

		connectPane.add(labelPane);
		connectPane.add(textPane);

		bottomPane.add(connectPane, "Connect");

		//Host panel
		JPanel hostPane = new JPanel();
		hostPane.add(new JLabel("Port: ", JLabel.RIGHT));
		hostPort = new JTextField("1503", 10);
		hostPane.add(hostPort);

		bottomPane.add(hostPane, "Host");

		//Finish button
		JPanel finish = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		finishBtn = new JButton("Start");
		finishBtn.addActionListener(this);
		finish.add(finishBtn);

		//Add to main panel
		add(top, BorderLayout.PAGE_START);
		add(bottomPane, BorderLayout.CENTER);
		add(finish, BorderLayout.PAGE_END);
		
		//Set window
		this.window = window;
		
		//Set default button
		window.getRootPane().setDefaultButton(finishBtn);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		//From a button?
		if(e.getSource() instanceof JRadioButton)
		{
			//Switch card
			((CardLayout) bottomPane.getLayout())
					.show(bottomPane, ((JRadioButton) e.getSource()).getText());
		}
		else
		{
			//TODO Start game
			throw new RuntimeException();
		}
	}
}
