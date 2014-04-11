package vClient;

import java.io.*;
import java.util.*;

import javax.swing.*;

import org.gstreamer.*;
import org.gstreamer.elements.AppSink;
import org.gstreamer.elements.good.RTPBin;
import org.gstreamer.swing.VideoComponent;

public class ClientData {

	public enum Mode
	{
		CLIENT
	}
	
	public enum State {
		NEGOTIATING, STREAMING
	}
	
	protected static Thread mainThread;
	protected static State state;
	protected static String serverResponse;
	protected static Pipeline pipe;
	protected static AppSink RTCPSink;
	protected static RTPBin rtpBin;
	protected static Element windowSink;
	protected static JFrame frame;
	protected static Mode mode;
	protected static VideoComponent vid_comp;
	protected static String frameRate;
	protected static String resolution;
	protected static List<JButton> controlButtons = new ArrayList<JButton>(); 
	protected static JPanel controls;
	protected static JTextArea monitor;
	protected static JSlider slider;
	protected static boolean seek;
	protected static int rate;
	protected static long duration;
	protected static long position;
	protected static long timeStamp;
	protected static long encDecTime;
	protected static BufferedReader resourcesReader;
	protected static BufferedWriter resourcesWriter;
}
