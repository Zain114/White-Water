package vServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.gstreamer.Format;
import org.gstreamer.SeekFlags;
import org.gstreamer.SeekType;
import org.gstreamer.State;
import org.gstreamer.StateChangeReturn;

import vClient.ClientData;

public class TCPServer implements Runnable{
	
	public void run() 
	{
		String clientString;
		String serverString;
		ServerSocket socket = null;
		
		try {
			System.out.println("SERVER: Initializing server socket");
			socket = new ServerSocket(5000);
			System.out.printf("SERVER: Server socket initialized %s\n", socket.toString());
			while(true)
			{
				System.out.println("SERVER: Connecting to socket port 5000");
				Socket connectionSocket = socket.accept();
				System.out.printf("SERVER: Connected to socket %s\n", connectionSocket.toString());
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				System.out.println("SERVER: Reading from socket");
				clientString = inFromClient.readLine() + "\n";
				ServerData.clientCommand = clientString;
				if(ServerData.state.equals(ServerData.State.NEGOTIATING))
				{
					serverString = negotiate();
					ServerData.state = ServerData.State.STREAMING;
					outToClient.writeBytes(serverString + "\n");
					ServerData.mainThread.interrupt();
				}
				else if(ServerData.state.equals(ServerData.State.STREAMING))
				{
					adaptPipeline();
					serverString = "State change successful";
					outToClient.writeBytes(serverString + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public static String negotiate()
	{
		String resourcesFR = "";
		String resourcesWidth = "";
		String resourcesHeight = "";
		try {
			resourcesFR = ServerData.resourcesReader.readLine();
			resourcesWidth = ServerData.resourcesReader.readLine();
			resourcesHeight = ServerData.resourcesReader.readLine();
			//ServerData.resourcesReader.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String resources[] = ServerData.clientCommand.split(" ");
		System.out.println(resources[0] + resources[1] + resources[2]);
		
		if(Integer.parseInt(resourcesFR) < Integer.parseInt(resources[0]))
			resources[0] = resourcesFR;
		if(Integer.parseInt(resourcesWidth) < Integer.parseInt(resources[1]))
			resources[1] = resourcesWidth;
		if(Integer.parseInt(resourcesHeight) < Integer.parseInt(resources[2].replace("\n", "")))
			resources[2] = resourcesHeight;
		
		String negotiatedResources = resources[0] + " " + resources[1] + " " + resources[2];
		return negotiatedResources;
	}
	
	public static void adaptPipeline()
	{
		if(ServerData.clientCommand.contains("play"))
		{
			StateChangeReturn ret = ServerData.pipe.setState(State.PLAYING);
			ServerData.setRate(ServerData.pipe, 1); 
			System.out.println(ret.toString());
		}
		else if(ServerData.clientCommand.contains("pause"))
		{
			StateChangeReturn ret = ServerData.pipe.setState(State.PAUSED);
			System.out.println(ret.toString());
		}
		else if(ServerData.clientCommand.contains("fastforward"))
		{ 
			if(ServerData.Rate > 0) {
				ServerData.setRate(ServerData.pipe, 2 * ServerData.Rate);
			}
			else
			{
				ServerData.setRate(ServerData.pipe, 1);
			}
		}
		else if(ServerData.clientCommand.contains("rewind"))
		{

			if(ServerData.Rate < 0)
				ServerData.setRate(ServerData.pipe, 2 * ServerData.Rate);
			else if ( ServerData.Rate == 1)
				ServerData.setRate(ServerData.pipe, -2);
			else if ( ServerData.Rate > 1)
				ServerData.setRate(ServerData.pipe, 1);

		}
	}
}
