package vPlayer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.swing.*;
import org.gstreamer.elements.*;
import org.gstreamer.elements.good.RTPBin;

public class vPlayerManager {
	
	private static Pipeline pipe;
	
	public static void main(String[] args)
	{
		//initialize GStreamer
		args = Gst.init("Simple Pipeline", args);
		
		//set startup mode
		PlayerData.mode = PlayerData.Mode.PLAYER;
		PlayerData.vidEnc = PlayerData.VideoEncoding.MJPEG;
		PlayerData.audEnc = PlayerData.AudioEncoding.ALAW;
		PlayerData.resolution = ",width=640, height=480";
		PlayerData.frameRate = ",framerate=10/1";
		PlayerData.file = "Cranes.mpg";
		PlayerData.seek = false;
		
		//initialize static window reference
		PlayerData.vid_comp = new VideoComponent();
		PlayerData.windowSink = PlayerData.vid_comp.getElement();
		
		PipelineManager.modify_pipeline();
		
		SwingUtilities.invokeLater(new Runnable() 
		{ 
			public void run() 
			{
				PlayerData.pipe.setState(State.READY);
				PlayerData.pipe.setState(State.PLAYING);
	    		System.out.println(PlayerData.pipe.queryDuration(Format.TIME));
	    		PlayerData.pipe.setState(State.PAUSED);
	    		
				//create the control panel
	    		PlayerData.controls = GUIManager.createControlPanel();
				
				//create encoding options panel
				JPanel encOptions = GUIManager.createEncodingOptionsPanel();
				
				//Actual top level widget
				PlayerData.frame = new JFrame("vPlayer"); 
				PlayerData.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	            
				PlayerData.frame.add(PlayerData.controls, BorderLayout.SOUTH);
				PlayerData.frame.add(encOptions, BorderLayout.EAST);
				PlayerData.frame.getContentPane().add(PlayerData.vid_comp, BorderLayout.CENTER);
				PlayerData.vid_comp.setPreferredSize(new Dimension(640, 480)); 
				PlayerData.frame.setSize(1080, 920);
				PlayerData.frame.setVisible(true);
				/*
	            Timer timer = new Timer(1000, new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						if(PlayerData.pipe.isPlaying() && !PlayerData.seek)
						{
							PlayerData.position = PlayerData.pipe.queryPosition(Format.TIME);
							PlayerData.slider.setValue((int)(PlayerData.position/1000000000));
						}
					}
	            });	
	            timer.start();
	            */
		
	        } 
	    });
		
		
		////////////////////////////////////////////////////SERVER//////////////////////////////////////////////////////////
		/*
		pipe = new Pipeline("server-pipeline");
		
		Element source = ElementFactory.make("v4l2src", "webcam");
		Element encoder = ElementFactory.make("ffenc_h263", "encoder");
		Element pay = ElementFactory.make("rtph263pay", "pay");
		RTPBin rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin"); 
		Element udpSink = ElementFactory.make("udpsink", "udpsink");
		
		if(source == null || encoder == null || pay == null || rtpBin == null || udpSink == null)
			System.err.println("Could not create all elements");
		
		pipe.addMany(source, encoder, pay, rtpBin, udpSink);
		
		Element.linkMany(source, encoder, pay);
		
		udpSink.set("host", "127.0.0.1");
		udpSink.set("port", "5001");
		
		rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				System.out.printf("New pad %s added to %s\n", newPad.getName(), source.getName());
				Pad udpSinkPad = pipe.getElementByName("udpsink").getStaticPad("sink");
				System.out.println(udpSinkPad.getName());
				newPad.link(udpSinkPad);
			}
		});
		
		Pad send_rtp_sink_0 = rtpBin.getRequestPad("send_rtp_sink_0");
		Pad paySrcPad = pay.getStaticPad("src");
		if(send_rtp_sink_0 == null || paySrcPad == null)
			System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
		paySrcPad.link(send_rtp_sink_0);
		*/
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		
		
		////////////////////////////////////////////////////CLIENT//////////////////////////////////////////////////////////
		/*
		pipe = new Pipeline("client-pipeline");
		
		Element udpSrc = ElementFactory.make("udpsrc", "udp-src");
		System.out.println("1 " + udpSrc);
		RTPBin rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin");
		System.out.println("2 " + rtpBin);
		Element depay = ElementFactory.make("rtph263depay", "depay");
		System.out.println("3 " + depay);
		Element decoder = ElementFactory.make("ffdec_h263", "decoder");
		System.out.println("4 " + decoder);
		Element sink = ElementFactory.make("xvimagesink", "sink");
		System.out.println("5 " + sink);
		
		Caps udpCaps = Caps.fromString("application/x-rtp,encoding-name=(string)H263,media=(string)video,clock-rate=(int)90000,payload=(int)96");
		System.out.println("6 " + udpCaps.toString());
		udpSrc.setCaps(udpCaps);
		udpSrc.set("port", "5001");
		
		rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				System.out.printf("New pad %s added to %s\n", newPad.toString(), source.toString());
				Pad depaySink = pipe.getElementByName("depay").getStaticPad("sink");
				PadLinkReturn ret = newPad.link(depaySink);
				System.out.println(ret.toString());
			}
		});
		
		pipe.addMany(udpSrc, rtpBin, depay, decoder, sink);
		
		boolean link = Element.linkMany(udpSrc, rtpBin);
		System.out.println("9 " + link);
		link = Element.linkMany(depay, decoder, sink);
		System.out.println("10 " + link);
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		pipe.setState(State.PLAYING);
		Gst.main();
		*/
	}
}
