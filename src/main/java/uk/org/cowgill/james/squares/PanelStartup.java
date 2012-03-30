package uk.org.cowgill.james.squares;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

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

	private final JTextField playerName = new JTextField("Player", 20);
	private final JTextField connectHost = new JTextField(20);
	private final JTextField connectPort = new JTextField("1503", 10);
	private final JTextField hostPort = new JTextField("1503", 10);
	
	private final JLabel lblPlayerName = new JLabel("Player Name: ", JLabel.RIGHT);
	private final JLabel lblConnectHost = new JLabel("Hostname: ", JLabel.RIGHT);
	private final JLabel lblConnectPort = new JLabel("Port: ", JLabel.RIGHT);
	private final JLabel lblHostPort = new JLabel("Port: ", JLabel.RIGHT);

	private final JPanel connectOptions;
	
	private final JFrame window;

	/**
	 * Creates a new startup panel
	 */
	public PanelStartup(JFrame window)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		//Panel to select player name
		JPanel namePane = new JPanel();
		namePane.add(lblPlayerName);
		namePane.add(playerName);
		
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
		connectOptions = new JPanel(new CardLayout());
		JPanel connectPane = new JPanel();

		JPanel labelPane = new JPanel(new GridLayout(2, 1, 0, 4));
		labelPane.add(lblConnectHost);
		labelPane.add(lblConnectPort);

		JPanel textPane = new JPanel(new GridLayout(2, 1));
		textPane.add(connectHost);
		textPane.add(connectPort);

		connectPane.add(labelPane);
		connectPane.add(textPane);

		connectOptions.add(connectPane, "Connect");

		//Host panel
		JPanel hostPane = new JPanel();
		hostPane.add(lblHostPort);
		hostPane.add(hostPort);

		connectOptions.add(hostPane, "Host");

		//Finish button
		JPanel finish = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		finishBtn = new JButton("Start");
		finishBtn.addActionListener(this);
		finish.add(finishBtn);

		//Add to main panel
		add(namePane);
		add(top);
		add(connectOptions);
		add(finish);
		
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
			((CardLayout) connectOptions.getLayout())
					.show(connectOptions, ((JRadioButton) e.getSource()).getText());
		}
		else
		{
			Thread transitionThread;
			
			String portText;
			final int port;
			
			//Get port text
			if(connectRadio.isSelected())
			{
				portText = connectPort.getText();
			}
			else
			{
				portText = hostPort.getText();
			}
			
			//Convert
			try
			{
				port = Integer.parseInt(portText);
			}
			catch(NumberFormatException except)
			{
				new ErrorMsgBox("Invalid port number", except).run();
				return;
			}
			
			//Create transition thread
			if(connectRadio.isSelected())
			{
				//Get arguments
				final String hostName = connectHost.getText();
			
				//Create transition thread
				transitionThread = new Thread(new TransitionRunnable("Error connecting:")
				{
					@Override
					public SocketChannel connect() throws Exception
					{
						return SocketChannel.open(new InetSocketAddress(hostName, port));
					}
				});
			}
			else
			{
				//Create transition thread
				transitionThread = new Thread(new TransitionRunnable("Error setting up hoster:")
				{
					@Override
					public SocketChannel connect() throws Exception
					{
						//Open listener
						ServerSocketChannel listener = ServerSocketChannel.open();
						listener.socket().bind(new InetSocketAddress(port));
						
						//Get connection
						SocketChannel channel = listener.accept();
						
						//Close listener and return
						listener.close();
						return channel;
					}
				});
			}
			
			//Disable input
			setInputEnabled(false);
			
			//Start thread
			transitionThread.setName("PanelStartup Network Transition");
			transitionThread.start();
		}
	}
	
	/**
	 * Enables or disables input buttons and text boxes
	 * 
	 * @param value true if input is to be enabled
	 */
	private void setInputEnabled(boolean value)
	{
		finishBtn.setEnabled(value);
		connectRadio.setEnabled(value);
		hostRadio.setEnabled(value);
		playerName.setEnabled(value);
		connectHost.setEnabled(value);
		connectPort.setEnabled(value);
		hostPort.setEnabled(value);
		lblPlayerName.setEnabled(value);
		lblConnectHost.setEnabled(value);
		lblConnectPort.setEnabled(value);
		lblHostPort.setEnabled(value);
	}
	
	/**
	 * Provides a routine to manage the transition from PanelStartup to PanelGame
	 * 
	 * @author James
	 */
	private abstract class TransitionRunnable implements Runnable
	{
		private String errMsg;
		
		public TransitionRunnable(String errMsg)
		{
			this.errMsg = errMsg;
		}
		
		@Override
		public void run()
		{
			SocketChannel retChannel = null;
			
			//Attempt to connect
			try
			{
				retChannel = connect();
			}
			catch(Exception e)
			{
				//Submit error message
				new ErrorMsgBox(errMsg, e).queue();
			}
			
			//Return to swing thread
			final SocketChannel channel = retChannel;
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					//Successful
					if(channel != null)
					{
						//Create new game window
						PanelGame gamePane = null;
						
						try
						{
							gamePane = new PanelGame(window, channel,
									playerName.getText(), !connectRadio.isSelected());
						}
						catch(Exception e)
						{
							//Show message box immediately
							new ErrorMsgBox("Error initializing game window:", e).run();
						}
						
						//Do transition
						if(gamePane != null)
						{
							window.setContentPane(gamePane);
							window.pack();
							return;
						}
					}
					
					//If we're here, an error occured sor re-enable input
					setInputEnabled(true);
				}
			});
		}
		
		/**
		 * Called to do the actual connection of the SocketChannel
		 */
		public abstract SocketChannel connect() throws Exception;
	}
	
	/**
	 * An error message box which shows an exception
	 * 
	 * Can be run asynchronously
	 * 
	 * @author James
	 */
	private class ErrorMsgBox implements Runnable
	{
		private final String msg;
		
		public ErrorMsgBox(String msg, Exception e)
		{
			//Append exception and store
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			
			this.msg = msg + "\n\n" + writer.toString();
		}
		
		public void queue()
		{
			SwingUtilities.invokeLater(this);
		}

		@Override
		public void run()
		{
			//Show message box
			JOptionPane.showMessageDialog(PanelStartup.this, msg,
					"Squares", JOptionPane.ERROR_MESSAGE);
		}
	}
}
