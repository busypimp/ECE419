import java.net.*;
import java.io.*;
import java.util.*;

public class OnlineBrokerHandlerThread extends Thread {
	private Socket socket = null;

	public OnlineBrokerHandlerThread(Socket socket) {
		super("EchoServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {

	boolean gotByePacket = false;

	Hashtable stocks = new Hashtable();
		
	//Read names from nasdaq
	try {
		FileReader fr = new FileReader ("nasdaq");
		BufferedReader br = new BufferedReader(fr);
		String strLine;
		String delims = "[ ]+";
				
		//Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			// Print the content on the console
			String[] elem = strLine.split(delims);
			Long l = Long.parseLong(elem[1]);
			stocks.put(elem[0], l);
		}
				 
		//Close the input stream
		fr.close();
	} catch (Exception e) {
		System.err.println("Error: " + e.getMessage());
	}		
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			BrokerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			

			while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				BrokerPacket packetToClient = new BrokerPacket();
				String clientRequest;
				//packetToClient.type = BrokerPacket.ECHO_REPLY;
				
				/* process message */
				/* just reply the results.  */
				if(packetFromClient.type == BrokerPacket.BROKER_QUOTE) {
					clientRequest = packetFromClient.symbol;
					System.out.println("From Client: " + clientRequest);

					//Get value from the Hashtable
					//String tmpQuote = (String)stocks.get(clientRequest);
					Long tmpQuote = (Long)stocks.get(clientRequest);
					if(tmpQuote == null) {
						tmpQuote = (long) 0;
					}
					packetToClient.quote = tmpQuote;
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					
					/* wait for next packet */
					continue;
				}
				
				/* Sending an ECHO_NULL || ECHO_BYE means quit */
				if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					packetToClient = new BrokerPacket();
					packetToClient.type = BrokerPacket.BROKER_BYE;
					//packetToClient.message = "Bye!";
					toClient.writeObject(packetToClient);
					break;
				}
				
				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown ECHO_* packet!!");
				System.exit(-1);
			}
			
			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}
}
