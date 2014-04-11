package vPlayer;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gstreamer.Bus;
import org.gstreamer.Format;
import org.gstreamer.Message;
import org.gstreamer.MessageType;
import org.gstreamer.Pad;
import org.gstreamer.SeekType;
import org.gstreamer.State;
import org.gstreamer.StateChangeReturn;
import org.gstreamer.Structure;
import org.gstreamer.event.FlushStartEvent;
import org.gstreamer.event.FlushStopEvent;
import org.gstreamer.lowlevel.*;

import vPlayer.PlayerData.Mode;

public class GUIManager {
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static JPanel createControlPanel()
	{
		//Player button				
		JButton player_button = new JButton("Player");
		player_button.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				System.out.println("Switching to player");
				PlayerData.mode = PlayerData.Mode.PLAYER;
				PipelineManager.modify_pipeline();
			}
		});
		//Recorder button
		JButton recorder_button = new JButton("Recorder");
		recorder_button.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				System.out.println("Switching to recorder");
				PlayerData.mode = PlayerData.Mode.VIDEO_RECORDER;
				PipelineManager.modify_pipeline();
			}
		});
		//play button
		JButton playButton;
		URL playURL;
		ImageIcon play;
		File check = new File("resources/123play.bmp");
		if(check.exists())
		{
			//playURL = getClass().getResource("resources/play.gif");
			//play = new ImageIcon(playURL);
			//playButton = new JButton(play);
			play = new ImageIcon("resources/play.gif","haha");
			playButton = new JButton(play);
		}
		else
			playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				//GstBusAPI.GSTBUS_API.gst_bus_post(PlayerData.pipe.getBus(), GstMessageAPI.GSTMESSAGE_API.gst_message_new_custom(MessageType.APPLICATION, PlayerData.pipe, GstStructureAPI.GSTSTRUCTURE_API.gst_structure_empty_new("Play STAte")));
				PlayerData.timeStamp = System.nanoTime();
				System.out.println("Setting state to playing");
				PlayerData.appSink.setState(State.PLAYING);
				PlayerData.rate = 1;
				//PlayerData.pipe.seek(PlayerData.rate, Format.TIME, 0, SeekType.NONE, PlayerData.position/1000000000, SeekType.NONE, PlayerData.duration/1000000000);
			}					
		});
		//pause button
		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				//GstBusAPI.GSTBUS_API.gst_bus_post(PlayerData.pipe.getBus(), GstMessageAPI.GSTMESSAGE_API.gst_message_new_custom(MessageType.APPLICATION, PlayerData.pipe, GstStructureAPI.GSTSTRUCTURE_API.gst_structure_empty_new("Pause STAte")));
				System.out.println("Setting state to paused");
				if(PlayerData.mode == Mode.VIDEO_RECORDER)
					PlayerData.pipe.setState(State.PAUSED);
				else
					PlayerData.appSink.setState(State.PAUSED);
			}					
		});
		//stop button
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Setting state to ready");
				//PlayerData.appSink.setState(State.READY);
				if(PlayerData.mode == Mode.VIDEO_RECORDER)
					PlayerData.pipe.setState(State.READY);
				else
				{
					PlayerData.pipe.setState(State.READY);
					PlayerData.pipe.setState(State.PAUSED);
				}
			}					
		});
		//record button
		JButton recordButton = new JButton("Record");
		recordButton.setEnabled(false);
		recordButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				PlayerData.timeStamp = System.nanoTime();
				System.out.println("Setting state to playing");
				PlayerData.pipe.setState(State.PLAYING);
				//PlayerData.appSink.setState(State.PLAYING);
			}					
		});
		//file open button
		JButton fileOpenButton = new JButton("Open");
		fileOpenButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("Opening file picker");
				PlayerData.pipe.setState(State.READY);
				JFileChooser fileChooser = new JFileChooser();
				int ret = fileChooser.showOpenDialog(null);
				if (ret == JFileChooser.APPROVE_OPTION) {
		            File f = fileChooser.getSelectedFile();
		            String fWPath = f.getAbsolutePath();
		            PlayerData.file = fWPath;
		            System.out.println("Opening: " + fWPath);
		            PipelineManager.modify_pipeline();
		        } else {
		        	System.out.println("Open command cancelled by user");
		        }
			}	
		});
		
		//fast forward
		JButton fastForwardButton = new JButton("Fastforward");
		fastForwardButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(PlayerData.rate < 0)
					PlayerData.rate = 0;
				PlayerData.rate += 2;
				PlayerData.pipe.seek(PlayerData.rate, Format.TIME, 0, SeekType.NONE, PlayerData.position/1000000000, SeekType.NONE, PlayerData.duration/1000000000);
			}
		});
		
		//rewind
		JButton rewindButton = new JButton("Rewind");
		rewindButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(PlayerData.rate > 0)
					PlayerData.rate = 0;
				PlayerData.rate -= 2;
				PlayerData.pipe.seek(PlayerData.rate, Format.TIME, 0, SeekType.NONE, PlayerData.position/1000000000, SeekType.NONE, PlayerData.duration/1000000000);
			}
		});
		
		
		//we can only read duration and current position after we have set state to playing
		//PlayerData.duration = PlayerData.pipe.queryDuration(Format.TIME);
		PlayerData.duration = 51;
		System.out.println((int)(PlayerData.duration/1000000000));
		PlayerData.slider = new JSlider(0,(int)(PlayerData.duration/1000000000),0);
		PlayerData.slider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				//when state changes, get the new position
				if(PlayerData.seek)
				{
					//move stream to that position
					System.out.println(PlayerData.slider.getValue());
					PlayerData.pipe.seek(PlayerData.slider.getValue(), TimeUnit.SECONDS);
					PlayerData.seek = false;
				}
			}
		});
		PlayerData.slider.addMouseListener(new MouseListener()
				{
					public void mouseClicked(MouseEvent e) {
						PlayerData.seek = true;
					}

					public void mousePressed(MouseEvent e) {

					}

					public void mouseReleased(MouseEvent e) {
						PlayerData.seek = true;
					}

					public void mouseEntered(MouseEvent e) {

					}

					public void mouseExited(MouseEvent e) {
					}
					
				});
		
		JPanel controls = new JPanel();
		controls.add(player_button);
		controls.add(recorder_button);
		controls.add(playButton);
		controls.add(pauseButton);
		controls.add(stopButton);
		controls.add(recordButton);
		controls.add(fastForwardButton);
		controls.add(rewindButton);
		controls.add(PlayerData.slider);
		
		//add buttons to list
		PlayerData.controlButtons.add(playButton);
		PlayerData.controlButtons.add(pauseButton);
		PlayerData.controlButtons.add(stopButton);
		PlayerData.controlButtons.add(recordButton);
		PlayerData.controlButtons.add(fileOpenButton);
		PlayerData.controlButtons.add(recorder_button);
		PlayerData.controlButtons.add(player_button);
		PlayerData.controlButtons.add(fastForwardButton);
		PlayerData.controlButtons.add(rewindButton);
		
		//define layout
		GroupLayout layout = new GroupLayout(controls);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		layout.setHorizontalGroup(
				layout.createParallelGroup()
					.addGroup(layout.createSequentialGroup()
						.addComponent(rewindButton)
						.addComponent(playButton)
						.addComponent(pauseButton)
						.addComponent(fastForwardButton)
						.addComponent(stopButton)
						.addComponent(recordButton)
						.addComponent(fileOpenButton)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
				         GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(recorder_button)
						.addComponent(player_button))
					.addComponent(PlayerData.slider)
		);				
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup()
						.addComponent(rewindButton)
					    .addComponent(playButton)
					    .addComponent(pauseButton)
					    .addComponent(fastForwardButton)
					    .addComponent(stopButton)
					    .addComponent(recordButton)
					    .addComponent(fileOpenButton)
					    .addComponent(recorder_button)
					    .addComponent(player_button))
				    .addComponent(PlayerData.slider)
		);
		controls.setLayout(layout);
		return controls;
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static JPanel createUserOptionsPanel()	
	{
		//resolution options
		String[] resList = {"320x240", "640x480", "960x720", "1280x1080"};
		//framerate options
		String[] frList = {"10", "15", "20", "30"};
		
		//frame rate list picker
		JComboBox resCB = new JComboBox(resList);
		resCB.setPreferredSize(new Dimension(100,50));
		resCB.setSelectedIndex(0);
		resCB.setPreferredSize(new Dimension(70,30));
		resCB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JComboBox source = (JComboBox)e.getSource();
				String selected = (String)source.getSelectedItem();
				if(selected.equals("320x240"))
					PlayerData.resolution = ",width=320, height=240 ";
				else if(selected.equals("640x480"))
					PlayerData.resolution = ",width=640, height=480 ";
				else if(selected.equals("960x720"))
					PlayerData.resolution = ",width=960, height=720 ";
				else if(selected.equals("1280x1080"))
					PlayerData.resolution = ",width=1280, height=1080 ";
			}
		});
		//resolution list picker
		JComboBox frCB = new JComboBox(frList);
		frCB.setPreferredSize(new Dimension(10,10));
		frCB.setSelectedIndex(0);
		frCB.setPreferredSize(new Dimension(70,30));
		frCB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JComboBox source = (JComboBox)e.getSource();
				String selected = (String)source.getSelectedItem();
				if(selected.equals("10"))
					PlayerData.frameRate = ",framerate=10/1 ";
				else if(selected.equals("15"))
					PlayerData.frameRate = ",framerate=15/1 ";
				else if(selected.equals("20"))
					PlayerData.frameRate = ",framerate=20/1 ";
				else if(selected.equals("30"))
					PlayerData.frameRate = ",framerate=30/1 ";
			}
		});
		
		JPanel userOptions = new JPanel();
		userOptions.setPreferredSize(new Dimension(100,150));
		userOptions.add(resCB);
		userOptions.add(frCB);
		
		GroupLayout userOptionsLayout = new GroupLayout(userOptions);
		userOptionsLayout.setAutoCreateGaps(true);
		userOptionsLayout.setAutoCreateContainerGaps(true);
		
		userOptionsLayout.setHorizontalGroup(
			userOptionsLayout.createParallelGroup()
				.addComponent(resCB)
				.addComponent(frCB)
		);
		userOptionsLayout.setVerticalGroup(
			userOptionsLayout.createSequentialGroup()
				.addComponent(resCB)
				.addComponent(frCB)
		);
		userOptions.setLayout(userOptionsLayout);
		
		return userOptions;
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static JPanel createEncodingOptionsPanel()
	{
		//create user options panel
		JPanel userOptions = createUserOptionsPanel();	
		
		//mjpeg radio button
		JRadioButton mjpegButton = new JRadioButton("mjpeg");
		mjpegButton.setSelected(true);
		mjpegButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("MJPEG encoding");
				PlayerData.vidEnc = PlayerData.VideoEncoding.MJPEG;
				PipelineManager.modify_pipeline();
			}					
		});
		
		//mpeg4 radio button
		JRadioButton mpeg4Button = new JRadioButton("mpeg4");
		mpeg4Button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("MPEG4 encoding");
				PlayerData.vidEnc = PlayerData.VideoEncoding.MPEG4;
				PipelineManager.modify_pipeline();
			}					
		});
		
		//alaw radio button
		JRadioButton alawButton = new JRadioButton("alaw");
		alawButton.setSelected(true);
		alawButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("ALAW encoding");
				PlayerData.audEnc = PlayerData.AudioEncoding.ALAW;
				PipelineManager.modify_pipeline();
			}					
		});
		
		//mulaw radio button
		JRadioButton mulawButton = new JRadioButton("mulaw");
		mulawButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("MULAW encoding");
				PlayerData.audEnc = PlayerData.AudioEncoding.MULAW;
				PipelineManager.modify_pipeline();
			}					
		});
		
		ButtonGroup vidGroup = new ButtonGroup();
		vidGroup.add(mjpegButton);
		vidGroup.add(mpeg4Button);
		
		ButtonGroup audGroup = new ButtonGroup();
		audGroup.add(alawButton);
		audGroup.add(mulawButton);
		
		//Monitor data
		PlayerData.monitor = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(PlayerData.monitor);
		PlayerData.monitor.setEditable(false);
		
		JPanel encOptions = new JPanel();

		encOptions.add(userOptions);
		encOptions.add(mjpegButton);
		encOptions.add(mpeg4Button);
		encOptions.add(alawButton);
		encOptions.add(mulawButton);
		
		//define layout
		GroupLayout encLayout = new GroupLayout(encOptions);
		encLayout.setAutoCreateGaps(true);
		encLayout.setAutoCreateContainerGaps(true);
		
		encLayout.setHorizontalGroup(
				encLayout.createParallelGroup()
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(scrollPane)
					)
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(userOptions)
					)
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(mjpegButton)
					.addComponent(mpeg4Button)
					)
					.addGroup(encLayout.createSequentialGroup()						
					.addComponent(alawButton)
					.addComponent(mulawButton)
					)
		);
		encLayout.setVerticalGroup(
				encLayout.createSequentialGroup()		
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(scrollPane)
					)
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(userOptions)
					)
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(mjpegButton)
					.addComponent(mpeg4Button)
					)
					.addGroup(encLayout.createParallelGroup()						
					.addComponent(alawButton)
					.addComponent(mulawButton)
					)
		);
		encOptions.setLayout(encLayout);
		return encOptions;
	}
}
