package vClient;

import java.io.*;
import java.net.*;

import org.gstreamer.State;

public class TCPClient implements Runnable{

	private String message;
	

	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	public static void sendServerMessage(String message)
	{
		TCPClient client = new TCPClient(message);
		Thread clientThread = new Thread(client);
		clientThread.start();
	}
	
	TCPClient(String message)
	{
		this.message = message;
	}
	
	public void run()
	{
		try
		{
			System.out.println("CLIENT: Initializing socket port 5000");
			Socket socket = new Socket("localhost", 5000);
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
			System.out.println("CLIENT: Writing to socket");
			outToServer.writeBytes(message + "\n");
			System.out.println("CLIENT: Reading from socket");
			String serverString = inFromServer.readLine();
			ClientData.serverResponse = serverString;
			System.out.printf("Server sent: %s\n", serverString);
			socket.close();
			if(ClientData.state.equals(ClientData.State.NEGOTIATING))
			{
				ClientData.state = ClientData.State.STREAMING;
				ClientData.mainThread.interrupt();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	public static String adjustProperties()
	{
		
		String resourcesFR = "";
		String resourcesWidth = "";
		String resourcesHeight = "";

		try {
			resourcesFR = ClientData.resourcesReader.readLine();
			resourcesWidth = ClientData.resourcesReader.readLine();
			resourcesHeight = ClientData.resourcesReader.readLine();
			//ClientData.resourcesReader.reset();
		} catch (IOException e) {
			System.err.println("Could not read from resources file");
		}
		if(Integer.parseInt(resourcesFR) < Integer.parseInt(ClientData.frameRate))
			ClientData.frameRate = resourcesFR;
		String resolution[] = ClientData.resolution.split("x");
		if(Integer.parseInt(resourcesWidth) < Integer.parseInt(resolution[0]))
			resolution[0] = resourcesWidth;
		if(Integer.parseInt(resourcesFR) < Integer.parseInt(resolution[1]))
			resolution[1] = resourcesHeight;
		
		ClientData.resolution = resolution[0] + "x" + resolution[1];
		
		String properties = ClientData.frameRate + " " + resolution[0] + " " + resolution[1];
		return properties;
	}
	
	
	/**
	 * Author:
	 * Purpose:
	 * Parameters:
	 * Return:
	 */
	public static String negotiateProperties(String properties)
	{		
		TCPClient.sendServerMessage(properties);
		while(!ClientData.mainThread.interrupted());
		
		if(!ClientData.serverResponse.equals(properties))
		{
			System.out.println("Negotiation Failed: Server cannot facilitate request. Modify properties to " + ClientData.serverResponse);
		}
		else
		{
			System.out.println("Negotiation Successful: Setting properties to " + ClientData.serverResponse);
		}
		return ClientData.serverResponse;
	}
	
}
