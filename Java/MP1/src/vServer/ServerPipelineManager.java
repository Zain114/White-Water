package vServer;

import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.Structure;
import org.gstreamer.TagList;
import org.gstreamer.elements.good.RTPBin;
import org.gstreamer.lowlevel.GObjectAPI;
import org.gstreamer.lowlevel.GType;
import org.gstreamer.lowlevel.GstTypes;

import com.sun.jna.Pointer;

import vServer.ServerData;

public class ServerPipelineManager {

	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void modify_pipeline()
	{
		switch(ServerData.mode)
		{
		case SERVER:
			System.out.println("Initializing Server");
			discard_pipeline();
			server_pipeline();
			connect_to_signals();
			ServerData.pipe.setState(State.READY);
			break;
		default:
			System.out.println("Unrecognized pipeline");
			ServerData.pipe.setState(State.READY);
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
		if(ServerData.pipe != null)
		{
			//need to explicitly remove windowSink
			ServerData.pipe.setState(State.READY);
			//ServerData.pipe.remove(ServerData.RTCPSink);
			//ServerData.RTCPSink = null;
			ServerData.pipe.setState(State.NULL);
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
	
		ServerData.pipe = new Pipeline("server-pipeline");
		
		/*
		___________	   _____________    ______________    ______________    ______________    _____________    ____________________    __________________    _______________
		| v4l2src |	-> | videorate | -> | videoscale | -> | ffenc_h263 | -> | rtph263pay | -> | gstrtpbin | -> | .send_rtp_sink_0 | -> |.send_rtp_src_0 | -> | udpsink 5001| 
		___________    _____________    ______________    ______________    ______________    _____________    ____________________    __________________    _______________
																|
																|			____________________    ________________
																|___\		| .send_rtcp_src_0 | -> | udpsink 5002 |
																|	/		____________________    ________________
																|			_______________    _______    _________    _____________________
																|___\		| udpsrc 5003 | -> | tee | -> | queue | -> | .recv_rtcp_sink_0 |
																	/		_______________    _______    _________    _____________________
																	 				              |       _________    ___________
																	 				              |___\   | queue | -> | appsink |
																	 				                  /   _________    ___________
		*/
		
		//Initialize elements
		Element source = ElementFactory.make("videotestsrc", "webcam");
		Element videorate = ElementFactory.make("videorate", "rate");
		Element videoscale = ElementFactory.make("videoscale", "scale");
		Element encoder = ElementFactory.make("ffenc_h263", "encoder");
		Element pay = ElementFactory.make("rtph263pay", "pay");
		ServerData.rtpBin = (RTPBin)ElementFactory.make("gstrtpbin", "rtp-bin"); 
		Element udpRTPSink = ElementFactory.make("udpsink", "udp-rtp-sink");
		Element udpRTCPSrc = ElementFactory.make("udpsrc", "udp-rtcp-src");
		Element udpRTCPSink = ElementFactory.make("udpsink", "udp-rtcp-sink");
		
		//Error check
		if(source == null || videorate == null || videoscale == null || encoder == null || pay == null || ServerData.rtpBin == null || udpRTPSink == null || udpRTCPSrc == null || udpRTCPSink == null)
			System.err.println("Could not create all elements");
		
		ServerData.pipe.addMany(source, videorate, videoscale, encoder, pay, ServerData.rtpBin, udpRTPSink, udpRTCPSrc, udpRTCPSink);
		
		String rateCapsStr = String.format("video/x-raw-yuv,framerate=%s/1", ServerData.framerate);
		System.out.println(rateCapsStr);
		Caps rateCaps = Caps.fromString(rateCapsStr);
		
		String scaleCapsStr = String.format("video/x-raw-yuv,width=%s,height=%s", ServerData.width, ServerData.height);
		System.out.println(scaleCapsStr);
		Caps scaleCaps = Caps.fromString(scaleCapsStr);
		
		//Link link-able elements
		//Element.linkMany(source, videorate);
		//if(!Element.linkPadsFiltered(videorate, "src", videoscale, "sink", rateCaps))
		//	System.err.println("Could not connect videotestsrc -> videorate");
		//if(!Element.linkPadsFiltered(videoscale, "src", encoder, "sink", scaleCaps))
		//	System.err.println("Could not connect videorate -> videoscale");
		//Element.linkMany(encoder, pay);
		Element.linkMany(source, encoder, pay);
		
		//Send RTP packets on 5001
		udpRTPSink.set("host", "127.0.0.1");
		udpRTPSink.set("port", "5001");
		//Receive RTCP packets on 5003
		udpRTCPSrc.set("port", "5003");
		//Send RTCP packets on 5002
		udpRTCPSink.set("host", "127.0.0.1");
		udpRTCPSink.set("port", "5002");
		
		//Link sometimes pads manually
		ServerData.rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element source, Pad newPad) {
				//System.out.printf("New pad %s added to %s\n", newPad.getName(), source.getName());
				if(newPad.getName().contains("send_rtp_src"))
				{
					Pad udpSinkPad = ServerData.pipe.getElementByName("udp-rtp-sink").getStaticPad("sink");
					newPad.link(udpSinkPad);
				}
			}
		});
		
		//Link request pads manually
		Pad send_rtp_sink_0 = ServerData.rtpBin.getRequestPad("send_rtp_sink_0");
		Pad paySrcPad = pay.getStaticPad("src");
		if(send_rtp_sink_0 == null || paySrcPad == null)
			System.err.println("Could not create rtpbin.send_rtp_sink_0 or pay.src pad");
		paySrcPad.link(send_rtp_sink_0);
		
		Pad send_rtcp_src_0 = ServerData.rtpBin.getRequestPad("send_rtcp_src_0");
		Pad udpSinkPadRTCP = udpRTCPSink.getStaticPad("sink");
		if(send_rtcp_src_0 == null || udpSinkPadRTCP == null)
			System.err.println("Could not create rtpbin.send_rtcp_src_0 or udp.src pad");
		send_rtcp_src_0.link(udpSinkPadRTCP);
		
		Pad recv_rtcp_sink_0 = ServerData.rtpBin.getRequestPad("recv_rtcp_sink_0");
		Pad udpSrcPadRTCP = udpRTCPSrc.getStaticPad("src");
		udpSrcPadRTCP.link(recv_rtcp_sink_0);
	}
	
		
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	protected static void connect_to_signals()
	{
		//connect to signal EOS
		ServerData.pipe.getBus().connect(new Bus.EOS() {
			public void endOfStream(GstObject source) {
				System.out.printf("[%s} reached EOS.\n", source);
				Gst.quit();
			}
		});
		
		//connect to signal ERROR
		ServerData.pipe.getBus().connect(new Bus.ERROR() {
			public void errorMessage(GstObject source, int code, String message) {
				System.out.printf("[%s] encountered error code %d: %s\n",source, code, message);
				Gst.quit();
			}
		});
		
		//connect to change of state
		ServerData.pipe.getBus().connect(new Bus.STATE_CHANGED() {
			public void stateChanged(GstObject source, State oldstate, State newstate, State pending) {
				if(source.equals(ServerData.pipe))
				{
					System.out.printf("[%s] changed state from %s to %s\n", source.getName(), oldstate.toString(), newstate.toString());
				}
			}
		});
		
		ServerData.rtpBin.connect(new RTPBin.ON_NEW_SSRC() {
			public void onNewSsrc(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("1 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
			}
		});
		
		ServerData.rtpBin.connect(new RTPBin.ON_SSRC_SDES() {
			public void onSsrcSdes(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("2 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
			}
		});
		
		ServerData.rtpBin.connect(new RTPBin.ON_SSRC_ACTIVE() {
			public void onSsrcActive(RTPBin rtpBin, int sessionid, int ssrc) {
				//System.out.printf("3 : RTCP packet received from ssrc: %s session: %s\n", ssrc, sessionid);
				Element rtpSession = ServerData.rtpBin.getElementByName("rtpsession0");
			}
		});
	}
	
}
