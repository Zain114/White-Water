package vClient;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.swing.*;
import org.gstreamer.elements.*;
import org.gstreamer.elements.good.RTPBin;

public class vClientManager {
	
	public static void main(String[] args)
	{
		ClientData.state = ClientData.State.NEGOTIATING;
		
		ClientResource res = ClientResource.getInstance();
		res.initWithFile("client-resources.txt");
		
		
		
		
		System.out.println("Resources are " + res.getBandwidth());
		
		
		File resources = new File("client-resources.txt");
		try {
			ClientData.resourcesReader = new BufferedReader(new FileReader(resources));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		/*
		try {
			ClientData.resourcesWriter = new BufferedWriter(new FileWriter(resources));
		} catch (IOException e) {
			e.printStackTrace();
		} 
		*/
		args = Gst.init("Client Pipeline", args);
		
		ClientData.mode = ClientData.Mode.CLIENT;
		
		ClientData.resolution = "640x480";
		ClientData.frameRate = "10";
		ClientData.seek = false;
		
		//initialize static window reference
		ClientData.vid_comp = new VideoComponent();
		ClientData.windowSink = ClientData.vid_comp.getElement();
			
		SwingUtilities.invokeLater(new Runnable() 
		{ 
			public void run() 
			{	    		
				ClientData.mainThread = Thread.currentThread();
				//create the control panel
				ClientData.controls = ClientGUIManager.createControlPanel();
				
				//create encoding options panel
				JPanel encOptions = ClientGUIManager.createEncodingOptionsPanel();
				
				//Actual top level widget
				ClientData.frame = new JFrame("vPlayer"); 
				ClientData.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	            
				ClientData.frame.add(ClientData.controls, BorderLayout.SOUTH);
				ClientData.frame.add(encOptions, BorderLayout.EAST);
				ClientData.frame.getContentPane().add(ClientData.vid_comp, BorderLayout.CENTER);
				ClientData.vid_comp.setPreferredSize(new Dimension(640, 480)); 
				ClientData.frame.setSize(1080, 920);
				ClientData.frame.setVisible(true);
	        } 
	    });
	}
}
