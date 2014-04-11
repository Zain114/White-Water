package vPlayer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;

import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.Pipeline;
import org.gstreamer.elements.AppSink;
import org.gstreamer.swing.VideoComponent;

public class PlayerData {

	public enum Mode
	{
		PLAYER, VIDEO_RECORDER, AUDIO_RECORDER, SERVER, CLIENT
	}
	
	public enum VideoEncoding
	{
		MJPEG, MPEG4
	}
	
	public enum AudioEncoding
	{
		ALAW, MULAW, MKV
	}
	
	protected static Pipeline pipe;
	protected static Bin playerBin;
	protected static Bin appSinkBin;
	protected static AppSink appSink;
	protected static List<Element> elems = new ArrayList<Element>();
	protected static Element windowSink;
	protected static JFrame frame;
	protected static Mode mode;
	protected static VideoEncoding vidEnc;
	protected static AudioEncoding audEnc;
	protected static VideoComponent vid_comp;
	protected static String frameRate;
	protected static String resolution;
	protected static List<JButton> controlButtons = new ArrayList<JButton>(); //{0:Play, 1:Pause, 2:Stop, 3:Record, 4:Open, 5:Player, 6:Recorder 
	protected static String file;
	protected static JPanel controls;
	protected static JTextArea monitor;
	protected static JSlider slider;
	protected static boolean seek;
	protected static int rate;
	protected static long duration;
	protected static long position;
	//protected static ProbeHandler probeHandler;
	protected static long timeStamp;
	protected static long encDecTime;
	
}
