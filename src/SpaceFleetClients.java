import java.io.*;
import java.net.*;
import java.util.*;

public class SpaceFleetClients implements Serializable{
	private static int numShuttles = 7;
	private static int numRecharge = 3;
	public static final long oneDay = 24*60*60*1000, halfAnHour = 30*60*1000; // ms 
	public static final int cruising = 0,landingArea = 1;
	public static ArrayList<Shuttle> shuttleList = new ArrayList<>();

	// For I/O
	private ObjectInputStream sInput; // To read from the socket
	private ObjectOutputStream sOutput;	// To write on the socket
	private Socket socket;

	// The server, the port and the username
	private String server, username;
	private int port;

	public SpaceFleetClients(String server, int port, String username,String []args) {
		this.server = server;
		this.port = port;
		this.username = username;

		if(args.length!=0){
			// So have args
			try {
				// Update the # of shuttles
				this.numShuttles = Integer.parseInt(args[0]);
			} catch (NumberFormatException nfe) {} // Catch the exception 
		}
	}

	public ObjectInputStream getsInput() {
		return sInput;
	}

	public ObjectOutputStream getsOutput() {
		return sOutput;
	}

	public static int getNumShuttles() {
		return numShuttles;
	}

	public static int getNumRecharge() {
		return numRecharge;
	}

	public void create(){
		// Create a controller and start it
		Controller controller = new Controller(0);
		controller.setSfc(this);
		controller.start();

		for (int i = 0; i < numShuttles; i++) {
			// Creating a shuttle
			Shuttle shuttle = new Shuttle(i);
			
			// Parsing the controller
			shuttle.setController(controller);
			
			shuttle.setSFC(this);
			
			// Add the created shuttle to the list
			shuttleList.add(shuttle);
			shuttle.start();
		}
		
		// Create and start the supervisor
		Supervisor sup = null;
		sup = new Supervisor(0);
		sup.setSfc(this);
		sup.start();

		// Set the supervisor to all shuttles
		for (Shuttle shuttle : shuttleList) {
			// Parse the supervisor
			shuttle.setSupervisor(sup);
		}
	}

	public synchronized static ArrayList<Shuttle> getShuttleList() {
		return shuttleList;
	}

	private void disconnect() {
		try {
			if(sInput != null) {
				sInput.close();
			}
		}
		catch(Exception e) {} // not much else I can do
		try {
			if(sOutput != null) {
				sOutput.close();
			}
		}
		catch(Exception e) {} // not much else I can do
		try {
			if(socket != null) {
				socket.close();
			}
		}
		catch(Exception e) {} // not much else I can do
	}

	/*
	 * To send a message to the server
	 */
	public synchronized void sendMessage(Message m) {
		try {
			sOutput.writeObject(m);
		}
		catch(IOException e) {
			// display("Exception writing to server: " + e);
		}
	}

	public Message receiveMessage() {
		Message m = null;
		try {
			m = (Message) sInput.readObject();
		} catch (IOException e) {
			display("Exception reading to server: " + e);
		} catch (ClassNotFoundException ex) {}

		return m;
	}

	/*
	 * To start the communication
	 */
	public boolean start() {
		// Try to connect to the server
		try {
			socket = new Socket(server, port);
		}
		
		// If it failed not much I can so
		catch(Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}

		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);

		/* Creating both Data Stream */
		try {
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// Creates the Thread to listen from the server
		// new Client.ListenFromServer().start();
		create(); // Create the all clients and start to listen to the server

		try {
			sOutput.writeObject(username);
		}
		catch (IOException eIO) {
			disconnect();
			return false;
		}
		// Success, we inform the caller that it worked
		return true;
	}

	private void display(String msg) {
		System.out.println(msg);  // Println in console mode
	}

	public static void main(String[] args) {
		// Default values
		int portNumber = 1500;
		String serverAddress = "localhost";
		String userName = "shuttle, controller and supervisor";

		// Create the Client object
		SpaceFleetClients clients = new SpaceFleetClients(serverAddress, portNumber, userName, args);
		
		// Test if we can start the connection to the Server
		// If it failed, then there's nothing we can do
		if(!clients.start()) {
			return;
		}		
	}

	class ListenFromServer extends Thread {
		@Override
		public void run() {
			while(true) {
				Message rm = receiveMessage();
				if(rm.getToWhome() == Message.shuttle){
					Shuttle sh = (Shuttle) rm.getData();
					switch(rm.getType()){
					case Message.stationOpened:
						sh.setReceivedMsg(Message.stationOpened);
						break;
					case Message.fillingUpResevoir:
						sh.setReceivedMsg(Message.fillingUpResevoir);
						break;
					case Message.takeOffTime:
						sh.setReceivedMsg(Message.takeOffTime);
						break;
					}   
				}
			}
		}
	}
}