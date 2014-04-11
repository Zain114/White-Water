package vServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import org.gstreamer.Format;
import org.gstreamer.Pipeline;
import org.gstreamer.SeekFlags;
import org.gstreamer.elements.good.RTPBin;

public class ServerData {

	public enum Mode
	{
		SERVER
	}
	
	public enum State {
		NEGOTIATING, STREAMING
	}
	
	protected static Thread mainThread;
	protected static State state;
	protected static Pipeline pipe;
	protected static int Rate; 
	protected static RTPBin rtpBin;
	protected static String clientCommand;
	protected static Mode mode;
	protected static BufferedReader resourcesReader;
	protected static BufferedWriter resourcesWriter;
	protected static int width;
	protected static int height;
	protected static int framerate;
	
	protected static void setRate(Pipeline pipe, int rate)
	{
		Format format = org.gstreamer.Format.TIME;
		
		int flags = SeekFlags.ACCURATE | SeekFlags.FLUSH;
		
		ServerData.Rate = rate;
		System.out.println("Rate is now: " + rate);
		pipe.seek(rate, format, flags, org.gstreamer.SeekType.NONE, 0, org.gstreamer.SeekType.NONE, 0);
	}



}
