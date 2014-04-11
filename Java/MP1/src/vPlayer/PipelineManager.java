package vPlayer;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.gstreamer.*;
import org.gstreamer.elements.AppSink;
import org.gstreamer.event.SeekEvent;

public class PipelineManager{

	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void modify_pipeline()
	{
		switch(PlayerData.mode)
		{
		case PLAYER:
			System.out.println("Initializing player");
			discard_pipeline();
			player_pipeline();
			connect_to_signals();
			PlayerData.pipe.setState(State.READY);
			PlayerData.pipe.setState(State.PAUSED);
			break;
		case VIDEO_RECORDER:
			System.out.println("Initializing video recorder");
			discard_pipeline();
			videorecorder_pipeline();
			connect_to_signals();
			PlayerData.pipe.setState(State.READY);
			PlayerData.pipe.setState(State.PAUSED);
			break;
		case AUDIO_RECORDER:
			System.out.println("Initializing audio recorder");
			discard_pipeline();
			audiorecorder_pipeline();
			connect_to_signals();
			PlayerData.pipe.setState(State.READY);
			PlayerData.pipe.setState(State.PAUSED);			
			break;
		default:
			System.out.println("Unrecognized pipeline");
			PlayerData.pipe.setState(State.READY);
			break;
		}
	}
	
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void discard_pipeline()
	{
		if(PlayerData.pipe != null)
		{
			//need to explicitly remove windowSink
			PlayerData.pipe.setState(State.READY);
			PlayerData.pipe.remove(PlayerData.windowSink);
			PlayerData.pipe.remove(PlayerData.appSink);
			PlayerData.appSink = null;
			PlayerData.pipe.setState(State.NULL);
		}
		for(int i=0; i < PlayerData.elems.size(); i++)
		{
			PlayerData.elems.get(0).dispose();
			PlayerData.elems.remove(0);
		}
	}
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void server_pipeline()
	{
		PlayerData.pipe = new Pipeline("server-pipeline");
		
		/*
		___________	   ______________    ______________    _____________    ___________
		| v4l2src |	-> | ffenc_h263 | -> | rtph263pay | -> | gstrtpbin | -> | udpsink | 
		___________    ______________    ______________    _____________    ___________
		*/
		
		Element source = ElementFactory.make("v4l2src", "webcam");
		Element encoder = ElementFactory.make("ffenc_h263", "encoder");
		Element pay = ElementFactory.make("rtph263pay", "pay");
		Element rtpBin = ElementFactory.make("gstrtpbin", "rtp-bin"); 
		Element udpSink = ElementFactory.make("udpsink", "udpsink");
		
		PlayerData.pipe.addMany(source, encoder, pay, rtpBin, udpSink);
		
		Element.linkMany(source, encoder, pay);
		
		udpSink.set("host", "127.0.0.1");
		udpSink.set("port", "5001");
		
		Pad send_rtp_sink_0 = rtpBin.getRequestPad("send_rtp_sink_0");
		Pad paySrcPad = pay.getStaticPad("src%d");
		paySrcPad.link(send_rtp_sink_0);
		
		Pad send_rtp_src_0 = rtpBin.getRequestPad("send_rtp_src_0");
		Pad udpSinkPad = udpSink.getStaticPad("sink");
		send_rtp_src_0.link(udpSinkPad);
		
	}
	
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void player_pipeline()
	{
		PlayerData.pipe = new Pipeline("player-pipeline");
		
		Element source = ElementFactory.make("filesrc", "source");
		Element tee = ElementFactory.make("tee", "tee");
		Element playerQueue = ElementFactory.make("queue", "player-queue");
		Element monitorQueue = ElementFactory.make("queue", "monitor-queue");
		Element decoder = ElementFactory.make("decodebin2", "decoder");
		PlayerData.appSink = (AppSink) ElementFactory.make("appsink", "monitor");
		//Element sink = ElementFactory.make("xvimagesink", "xsink");
		
		source.set("location", PlayerData.file);
		tee.set("silent", "false");
		PlayerData.appSink.set("emit-signals", true);
		PlayerData.appSink.connect(new AppSink.NEW_BUFFER() {
			public void newBuffer(AppSink appSink) {
				//take timestamp between new buffers, and record that as the time to decompress
				long time = System.nanoTime();
				String decompressionTime = "Decompression time(ns): " + (time - PlayerData.timeStamp) + "\n";
				PlayerData.monitor.append(decompressionTime);
				PlayerData.timeStamp = time;
				Buffer buffer = appSink.pullBuffer();
				System.out.printf("Size: %s Offset: %s\n", buffer.getSize(), buffer.getOffset());
			}
		});
		
		decoder.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				System.out.printf("New pad %s added to %s\n", newPad.getName(), source.getName());
				Pad sink_pad = PlayerData.windowSink.getStaticPad("sink");
				//Pad sink_pad = PlayerData.pipe.getElementByName("xsink").getStaticPad("sink");
				if(sink_pad.isLinked())
					System.out.println("Pad already linked");
				else
				{
					PadLinkReturn ret = newPad.link(sink_pad);
					if(ret == null)
						System.out.println("Pad link failed");
				}
			}
		});
		
		PlayerData.elems.add(source);
		PlayerData.elems.add(tee);
		PlayerData.elems.add(playerQueue);
		PlayerData.elems.add(monitorQueue);
		PlayerData.elems.add(decoder);
		
		////////////////////////////////////////seperate bins code//////////////////////////////////////////////
		/*
		PlayerData.playerBin = new Bin("player-bin");
		PlayerData.appSinkBin = new Bin("appsink-bin");
		
		PlayerData.appSinkBin.addMany(source, tee, monitorQueue, appSink);
		Element.linkMany(source, tee);
		Element.linkMany(monitorQueue, appSink);
		
		Pad teeMonitorSrc = tee.getRequestPad("src%d");
		Pad queueMonitorSink = monitorQueue.getStaticPad("sink");
		teeMonitorSrc.link(queueMonitorSink);
		
		PlayerData.playerBin.addMany(playerQueue, decoder, sink);
		
		
		PlayerData.pipe.add(PlayerData.appSinkBin);
		*/
		///////////////////////////////////////////////////////////////////////////////////////////////////////
		
		PlayerData.pipe.addMany(source, tee, monitorQueue, playerQueue, decoder, PlayerData.appSink, PlayerData.windowSink);
		
		if(!Element.linkMany(source, tee))
			System.out.println("Failed: source -> tee");
		if(!Element.linkMany(playerQueue, decoder))
			System.out.println("Failed: player-queue -> decoder");
		if(!Element.linkMany(monitorQueue, PlayerData.appSink))
			System.out.println("Failed: monitor-queue -> appsink");
		
		Pad teePlayerSrc = tee.getRequestPad("src%d");
		Pad teeMonitorSrc = tee.getRequestPad("src%d");
		Pad queuePlayerSink = playerQueue.getStaticPad("sink");
		Pad queueMonitorSink = monitorQueue.getStaticPad("sink");
		
		PadLinkReturn ret = teePlayerSrc.link(queuePlayerSink);
		if(!ret.equals(PadLinkReturn.OK))
			System.out.println("Failed: tee -> player-queue");
		ret = teeMonitorSrc.link(queueMonitorSink);
		if(!ret.equals(PadLinkReturn.OK))
			System.out.println("Failed: tee -> monitor-queue");
		
		//gray out undesired buttons
		if(PlayerData.controlButtons.size() != 0)
		{
			PlayerData.controlButtons.get(0).setEnabled(true);
			PlayerData.controlButtons.get(1).setEnabled(true);
			PlayerData.controlButtons.get(2).setEnabled(true);
			PlayerData.controlButtons.get(3).setEnabled(false);
			PlayerData.controlButtons.get(4).setEnabled(true);
		}

		//SeekEvent seekEvent = new SeekEvent(2, Format.TIME, 0, SeekType.CUR, 0, SeekType.CUR, 100);
		//System.out.println(seekEvent.getRate());
		
		//try to subscribe to the have data signal of the pad
		/*
		newPad.connect(new Pad.HAVE_DATA(){
			public void haveData(Pad pad, MiniObject data) {
				
				Buffer buffer = (Buffer) data;
				ByteBuffer bBuffer = buffer.getByteBuffer();
				//if(bBuffer != null)
					System.out.printf("Decoder buffer of size: %s \n", buffer.getSize());
			}
		});
				
		//try to attach a probe to the pad
		
		newPad.addEventProbe(new Pad.EVENT_PROBE(){
			public boolean eventReceived(Pad pad, Event event) {
				Structure eventStruct = event.getStructure();
				String eventName = eventStruct.getName();
				//System.out.println(eventName);
				if(eventName.equals("GstEventNewsegment"))
				{
					System.out.printf("Event: %s from Pad: %s \n", eventStruct, pad.getName());
					//System.out.printf("Current position: %s\n", eventStruct.getValue("position"));
				}
				return false;
			}
		});
		*/
	}
	
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void videorecorder_pipeline()
	{
		PlayerData.pipe = new Pipeline("video-recorder");
		
		Element source = ElementFactory.make("v4l2src", "source");
		Element tee = ElementFactory.make("tee", "tee");
		Element encTee = ElementFactory.make("tee", "enc-tee");
		Element streamQueue = ElementFactory.make("queue", "stream-queue");
		Element recorderQueue = ElementFactory.make("queue", "recorder-queue");
		Element recorderQueue2 = ElementFactory.make("queue", "recorder-queue-2");
		Element monitorQueue = ElementFactory.make("queue", "monitor-queue");
		PlayerData.appSink = (AppSink) ElementFactory.make("appsink", "app-sink");
		
		PlayerData.appSink.set("emit-signals", true);
		PlayerData.appSink.connect(new AppSink.NEW_BUFFER() {
			public void newBuffer(AppSink appSink) {
				//take timestamp between new buffers, and record that as the time to decompress
				long time = System.nanoTime();
				String compressionTime = "Compression time(ns): " + (time - PlayerData.timeStamp) + "\n";
				PlayerData.monitor.append(compressionTime);
				PlayerData.timeStamp = time;
				Buffer buffer = appSink.pullBuffer();
				System.out.printf("Size: %s Offset: %s\n", buffer.getSize(), buffer.getOffset());
			}
		});
		
		Caps encCaps = new Caps();
		encCaps = Caps.fromString("video/x-raw-yuv" + PlayerData.resolution );//+ PlayerData.frameRate width=640, height=480, framerate=20/1");
		Element colorspace = ElementFactory.make("ffmpegcolorspace", "colorspace");
		Element encoder = ElementFactory.make("jpegenc", "encoder");;
		switch(PlayerData.vidEnc)
		{
		case MJPEG:
			encoder = ElementFactory.make("jpegenc", "encoder");
			break;
		case MPEG4:
			encoder = ElementFactory.make("ffenc_mpeg4", "encoder");
			break;
		default:
			break;
		}
		Element mux = ElementFactory.make("avimux", "mux");
		Element sink = ElementFactory.make("filesink", "sink");
		
		tee.set("silent", "false");
		sink.set("location", "Recording1.mpg");
		
		PlayerData.elems.add(source);
		PlayerData.elems.add(tee);
		PlayerData.elems.add(encTee);
		PlayerData.elems.add(streamQueue);
		PlayerData.elems.add(recorderQueue);
		PlayerData.elems.add(monitorQueue);
		PlayerData.elems.add(colorspace);
		PlayerData.elems.add(encoder);
		PlayerData.elems.add(mux);
		PlayerData.elems.add(sink);
		
		PlayerData.pipe.addMany(source, tee, streamQueue, PlayerData.windowSink, recorderQueue, colorspace, encoder, encTee, recorderQueue2, mux, sink, monitorQueue, PlayerData.appSink);
		/*
		if(!Element.linkMany(source, tee))
			System.out.println("Failed: webcam -> tee");
		if(!Element.linkPadsFiltered(recorderQueue, "src", colorspace, "sink", encCaps))
			System.out.println("Failed: recorder queue -> colorspace");
		if(!Element.linkMany(colorspace, encoder, mux, sink))
			System.out.println("Failed: colorspace -> encoder -> file sink");
		if(!Element.linkMany(streamQueue, PlayerData.windowSink))
			System.out.println("Failed: stream queue -> widget");
		*/
		if(!Element.linkMany(source, tee))
			System.out.println("Failed: webcam -> tee");
		if(!Element.linkMany(streamQueue, PlayerData.windowSink))
			System.out.println("Failed: stream queue -> video sink");
		if(!Element.linkPadsFiltered(recorderQueue, "src", colorspace, "sink", encCaps))
			System.out.println("Failed: recorder queue -> colorspace");
		if(!Element.linkMany(colorspace, encoder, encTee))
			System.out.println("Failed: colorspace -> encoder -> encTee");
		if(!Element.linkMany(recorderQueue2, mux, sink))
			System.out.println("Failed: recorder queue 2 -> mux -> sink");
		if(!Element.linkMany(monitorQueue, PlayerData.appSink))
			System.out.println("Failed: monitor queue -> app sink");
		
		
		//create two new src pads with and link them with queue pads
		Pad teeStreamSrc = tee.getRequestPad("src%d");
		Pad teeRecorderSrc = tee.getRequestPad("src%d");
		Pad queueStreamSink = streamQueue.getStaticPad("sink");
		Pad queueRecorderSink = recorderQueue.getStaticPad("sink");
		
		teeStreamSrc.link(queueStreamSink);
		teeRecorderSrc.link(queueRecorderSink);
		
		Pad encTeeRecorderSrc = encTee.getRequestPad("src%d");
		Pad encTeeMonitorSrc = encTee.getRequestPad("src%d");
		Pad queueRecorder2Sink = recorderQueue2.getStaticPad("sink");
		Pad queueMonitorSink = monitorQueue.getStaticPad("sink");
		
		encTeeRecorderSrc.link(queueRecorder2Sink);
		encTeeMonitorSrc.link(queueMonitorSink);
		
		if(PlayerData.controlButtons.size() != 0 )
		{
			PlayerData.controlButtons.get(0).setEnabled(false);
			PlayerData.controlButtons.get(1).setEnabled(true);
			PlayerData.controlButtons.get(2).setEnabled(true);
			PlayerData.controlButtons.get(3).setEnabled(true);
			PlayerData.controlButtons.get(4).setEnabled(false);
		}
	}
	
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void audiorecorder_pipeline()
	{
		PlayerData.pipe = new Pipeline("audio-recorder");
		
		Element source = ElementFactory.make("alsasrc", "source");
		Element converter = ElementFactory.make("audioconvert", "converter");
		Element encoder = ElementFactory.make("vorbisenc", "encoder");
		switch(PlayerData.audEnc)
		{
		case ALAW:
			encoder = ElementFactory.make("alawenc", "encoder");
			break;
		case MULAW:
			encoder = ElementFactory.make("mulawenc", "encoder");
			break;
		case MKV:
			encoder = ElementFactory.make("vorbisenc", "encoder");
			break;
		default:
			break;
		}
		Element mux = ElementFactory.make("webmmux", "mux");
		Element sink = ElementFactory.make("filesink", "sink");
		
		source.set("device", "hw:2");
		sink.set("location", "AudioRec1.mkv");
		
		PlayerData.elems.add(source);
		PlayerData.elems.add(converter);
		PlayerData.elems.add(encoder);
		PlayerData.elems.add(mux);
		PlayerData.elems.add(sink);
		
		PlayerData.pipe.addMany(source, converter, encoder, mux, sink);
		
		Element.linkMany(source, converter, encoder, mux, sink);
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void connect_to_signals()
	{
		/*
		//connect to signal MESSAGE
		PlayerData.pipe.getBus().connect(new Bus.MESSAGE() {
			public void busMessage(Bus bus, Message msg) {
				String messageName = msg.getStructure().getName();
				if(messageName.equals("Pause STAte"))
				{
					System.out.println("Thread: " + Thread.currentThread().getId());
					System.out.println("Main thread pausing");
					PlayerData.pipe.pause();
				}
				if(messageName.equals("Play STAte"))
				{
					System.out.println("Thread: " + Thread.currentThread().getId());
					System.out.println("Main thread playing");
					PlayerData.pipe.play();
				}
			}
		});
		*/
		
		//connect to signal TAG
		PlayerData.pipe.getBus().connect(new Bus.TAG() {
			public void tagsFound(GstObject source, TagList tagList) {
				for(String tagName : tagList.getTagNames())
				{
					for(Object tagData : tagList.getValues(tagName))
					{
						String data = "[" + tagName + "] = " + tagData + " \n";
						PlayerData.monitor.append(data);
					}
				}
			}
		});
		
		//connect to signal EOS
		PlayerData.pipe.getBus().connect(new Bus.EOS() {
			public void endOfStream(GstObject source) {
				//exit gracefully
				System.out.printf("[%s} reached EOS.\n", source);
				Gst.quit();
			}
		});
		
		//connect to signal ERROR
		PlayerData.pipe.getBus().connect(new Bus.ERROR() {
			@Override
			public void errorMessage(GstObject source, int code, String message) {
				//print error from message
				System.out.printf("[%s] encountered error code %d: %s\n",source, code, message);
				Gst.quit();
			}
		});
		
		//connect to change of state
		PlayerData.pipe.getBus().connect(new Bus.STATE_CHANGED() {
			public void stateChanged(GstObject source, State oldstate, State newstate, State pending) {
				if(source.equals(PlayerData.pipe))
				{
					System.out.printf("[%s] changed state from %s to %s\n", source.getName(), oldstate.toString(), newstate.toString());
				}
				else if(source.equals(PlayerData.appSink))
				{
					if(newstate.equals(State.PLAYING))
						PlayerData.pipe.setState(State.PLAYING);
					else if(newstate.equals(State.PAUSED))
						PlayerData.pipe.setState(State.PAUSED);
				}
			}
		});
		 
		//connect to buffering signal for monitor data
		PlayerData.pipe.getBus().connect(new Bus.BUFFERING() {
			public void bufferingData(GstObject source, int percentage) {
				System.out.println("BUFFERING DATA");
				System.out.println("Source " + source.getName() + ": " + percentage);
			}
		});
	}
	
	/**
	 * Author: Zain
	 * Purpose: We will attach data probes on elements such as decodebin2 to inform us when data is available to the pad. We will do this to
	 * 			time how long the data takes to pass through the element for monitoring purposes
	 * Parameters:
	 * Return:
	 */
	//protected static PadProbeReturn detectedStream(Pad pad, PadProbeInfo info)
	//{
		
	//}
}
