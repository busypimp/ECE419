import java.io.*;
import java.net.*;
import java.util.Hashtable;
/**
 * BrokerExchange
 * ============
 * 
 * Three functions supported
 * 		add
 * 		remove
 * 		update
 * 
 */


public class BrokerExchange {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket echoSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			/* variables for hostname/port */
			String hostname = "localhost";
			int port = 4444;
			
			if(args.length == 2 ) {
				hostname = args[0];
				port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			echoSocket = new Socket(hostname, port);

			out = new ObjectOutputStream(echoSocket.getOutputStream());
			in = new ObjectInputStream(echoSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		System.out.print("Enter command or quit for exit:\n> ");
		while ((userInput = stdIn.readLine()) != null
				&& !userInput.toLowerCase().equals("x")) {
			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			
			//Split up commands 
			String delims = "[ ]+";
			String[] cmds = userInput.toLowerCase().split(delims);
//			int cmdNum = cmds.length;

			packetToServer.symbol = cmds[1];
			if(cmds[0].equals("add")) {
				packetToServer.type = BrokerPacket.EXCHANGE_ADD;
//System.out.println("in th eadding...");				

			} else if(cmds[0].equals("remove")) {
				packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;

				
			} else if(cmds[0].equals("update")) {
				packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
				packetToServer.quote = Long.parseLong(cmds[2]);
				
			}
//			else {
//			}
			
			
//			packetToServer.type = BrokerPacket.BROKER_QUOTE;
//			packetToServer.symbol = userInput.toLowerCase();
			out.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) in.readObject();
//System.out.println("before swtich " + packetToServer.type + "and the add is... " + BrokerPacket.EXCHANGE_ADD);
			switch(packetToServer.type) {
			case BrokerPacket.EXCHANGE_ADD:
//System.out.println("in swtich");
				if(packetFromServer.error_code == BrokerPacket.ERROR_SYMBOL_EXISTS)
					System.out.println(cmds[1].toUpperCase() + " exists.");
				else
					System.out.println(cmds[1].toUpperCase() + " added.");
				break;
			case BrokerPacket.EXCHANGE_REMOVE:
				if(packetFromServer.error_code == BrokerPacket.ERROR_INVALID_SYMBOL)
					System.out.println(cmds[1].toUpperCase() + " invalid.");
				else
					System.out.println(cmds[1].toUpperCase() + " removed.");
				break;
			case BrokerPacket.EXCHANGE_UPDATE:
				if(packetFromServer.error_code == BrokerPacket.ERROR_INVALID_SYMBOL)
					System.out.println(cmds[1].toUpperCase() + " invalid.");
				else if(packetFromServer.error_code == BrokerPacket.ERROR_OUT_OF_RANGE)
					System.out.println(cmds[1].toUpperCase() + " out of range.");
				else
					System.out.println(cmds[1].toUpperCase() + " updated to " + packetToServer.quote + ".");
				break;
			//case BrokerPacket.:
				//break;
			}
			
//			if (packetFromServer.type == BrokerPacket.ECHO_REPLY)
			//System.out.println("Quote from broker: " + packetFromServer.quote);

			/* re-print console prompt */
			System.out.print("> ");
		}

		/* tell server that i'm quitting */
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		//BrokerPacket.message = "Bye!";
		out.writeObject(packetToServer);

		out.close();
		in.close();
		stdIn.close();
		echoSocket.close();
	}
}
