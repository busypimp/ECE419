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
				
				//Read names from nasdaq
				Hashtable stocks = new Hashtable();
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
				
				//Check to see if the symbol exists or not
				String clientSym = packetFromClient.symbol;
				Boolean exists = stocks.containsKey(clientSym);

				System.out.println("From Client: " + packetFromClient.type + " " + clientSym + " " + packetFromClient.quote );

				/* process message */
				switch(packetFromClient.type) {
				case BrokerPacket.EXCHANGE_ADD:
					if(exists)
						packetToClient.error_code = BrokerPacket.ERROR_SYMBOL_EXISTS;
					else
						stocks.put(clientSym, (long) 0);
					break;
				case BrokerPacket.EXCHANGE_REMOVE:
					if(!exists)
						packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;			
					else
						stocks.remove(clientSym);
					break;
				case BrokerPacket.EXCHANGE_UPDATE:
					if(!exists)
						packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
					else if(packetFromClient.quote > 300 || packetFromClient.quote < 0)
						packetToClient.error_code = BrokerPacket.ERROR_OUT_OF_RANGE;
					else
						stocks.put(clientSym, packetFromClient.quote);
					break;
				case BrokerPacket.BROKER_QUOTE:
					if(!exists) {
						packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
						packetToClient.symbol = clientSym.toUpperCase();
						break;
					}
					Long tmpQuote = (Long)stocks.get(clientSym);
					if(tmpQuote == null) {
						tmpQuote = (long) 0;
					}
					packetToClient.quote = tmpQuote;					
					break;
				case BrokerPacket.BROKER_NULL:
				case BrokerPacket.BROKER_BYE:
					gotByePacket = true;
					packetToClient = new BrokerPacket();
					packetToClient.type = BrokerPacket.BROKER_BYE;
					//packetToClient.message = "Bye!";
					toClient.writeObject(packetToClient);
					continue;
				default:
					/* if code comes here, there is an error in the packet */
					System.err.println("ERROR: Unknown ECHO_* packet!!");
					System.exit(-1);				
				}
					/* send reply back to client */
				try{
				  // Create file 
					FileWriter fstream = new FileWriter("nasdaq");
					BufferedWriter out = new BufferedWriter(fstream);
				  
					for (Enumeration e = stocks.keys() ; e.hasMoreElements() ;) {
						String tmp = (String) e.nextElement();
						out.write(tmp + " " + stocks.get(tmp) + "\n");
					}
					//Close the output stream
					out.close();
				}catch (Exception e){//Catch exception if any
					System.err.println("Error: " + e.getMessage());
				}
				
				toClient.writeObject(packetToClient);
					
				/* wait for next packet */
				/* Sending an ECHO_NULL || ECHO_BYE means quit */
				//if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
				//	gotByePacket = true;
				//	packetToClient = new BrokerPacket();
				//	packetToClient.type = BrokerPacket.BROKER_BYE;
				//	//packetToClient.message = "Bye!";
				//	toClient.writeObject(packetToClient);
				//	break;
				//}
				
				/* if code comes here, there is an error in the packet */
//				System.err.println("ERROR: Unknown ECHO_* packet!!");
//				System.exit(-1);
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
