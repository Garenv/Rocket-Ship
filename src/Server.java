import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
	// A unique ID for each connection
	private static int uniqueId;
	// An ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	// The port number to listen for connection
	private int port;
	// The boolean that will be turned of to stop the server
	private boolean keepGoing;

	public Server(int port) {
		// The port
		this.port = port;
		// ArrayList for the Client list
		al = new ArrayList<ClientThread>();
	}

	public void start() {
		keepGoing = true;
		/* create socket server and wait for connection requests */
		try {
			// The socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// Infinite loop to wait for connections
			while(keepGoing){
				// Format message saying we are waiting
				display("Server waiting for Clients on port " + port + "...");

				Socket socket = serverSocket.accept();  	// Accept connection
				// If I was asked to stop
				if(!keepGoing)
					break;
				ClientThread t = new ClientThread(socket);  // Make a thread of it
				al.add(t); // Save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					}
					catch(IOException ioE) {}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		// Something went wrong
		catch (IOException e) {
			String msg =  "Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}

	protected void stop() {
		keepGoing = false;
		// Connect to myself as Client to exit statement
		// Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", port);
		} catch(IOException e) {}
	}

	private void display(String msg) {
		System.out.println(msg);
	}

	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// found it
			if(ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}

	public static void main(String[] args) {
		// Start server on port 1500 
		int portNumber = 1500;
		// Create a server object and start it
		Server server = new Server(portNumber);
		server.start();
	}

	/** One instance of this thread will run for each client */
	// As a client helper thread
	class ClientThread extends Thread {
		// The socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		int id; // My unique id (easier for deconnection)
		String name; // The name of the Client
		Message cm;

		public ClientThread(Socket socket) {
			// A unique id
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			try {
				// Create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// Read the name of the client
				name = (String) sInput.readObject();
				display(name + " client threads just connected.");
			} catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			} catch (ClassNotFoundException e) {}
		}

		// What will run forever
		public void run() {
			boolean keepGoing = true;
			while(keepGoing) {
				try {
					cm = (Message) sInput.readObject();
				}catch (IOException e) {
					// display(name + " Exception reading Streams: " + e);
					break;
				} catch (ClassNotFoundException ex) {}
				writeMsg(cm);
			}
			// Remove myself from the arrayList containing the list of the
			// Connected Clients
			remove(id);
			close();
		}

		// Try to close everything
		private void close() {
			// Try to close the connection
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(Message message) {
			// If Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// Write the message to the stream
			try {
				sOutput.writeObject(message);
			} catch(IOException e) {
				display("Error sending message to " + name);
				display(e.toString());
			}
			return true;
		}
	}
}