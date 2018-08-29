import java.io.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Shuttle extends Thread implements Serializable{

	public static long time = System.currentTimeMillis();
	private int currentState = SpaceFleetClients.cruising;
	private int takeOffChance = 3;// 3 is maximum
	private Controller controller;
	private Supervisor supervisor;
	private SpaceFleetClients sfc;
	private int receivedMsg = -1;
	private int id;
	private boolean landing = false;
	private long landedTime;

	public Shuttle(int id) {
		setName("ShuttleThread-"+id);
		this.id = id;
	}

	public void setSFC(SpaceFleetClients sfc) {
		this.sfc = sfc;
	}

	public void setController(Controller controller) {
		this.controller = controller;
	}

	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
	}

	public long getLandedTime() {
		return landedTime;
	}

	public void setLandedTime(long landedTime) {
		this.landedTime = landedTime;
	}

	public boolean toLand(){
		if(new Random().nextInt(5)==SpaceFleetClients.landingArea) {
			return true;
		}
		return false;
	}
	
	// Print messages
	public void msg(String m) {
		System.out.println("["+(System.currentTimeMillis()-time)+"] "+getName()+": "+m);
	}
	
	// Random number generation between 50 and 100 to represent the % of needed fuel
	public int neededFuel(){
		int fuel = 0;
		int min = 50, max = 100;
		Random rand = new Random();
		fuel = rand.nextInt(max - min) + min;

		return fuel;
	}

	public int getTakeOffChance() {
		return takeOffChance;
	}

	public void setTakeOffChance(int takeOffChance) {
		this.takeOffChance = takeOffChance;
	}

	public int getCurrentState() {
		return currentState;
	}

	public void setCurrentState(int currentState) {
		this.currentState = currentState;
	}

	public int get_Id() {
		return id;
	}

	public synchronized void setReceivedMsg(int receivedMsg) {
		this.receivedMsg = receivedMsg;
	}

	public int getReceivedMsg() {
		return receivedMsg;
	}

	@Override
	public void run(){
		msg("Started...");
		
		while (System.currentTimeMillis()-time < SpaceFleetClients.oneDay) {
			int m = getReceivedMsg();
			switch(m){
			case Message.stationOpened:
				try {
					sleep(1000);// 1 second
				} catch (InterruptedException ex) {
					Logger.getLogger(Shuttle.class.getName()).log(Level.SEVERE, null, ex);
				}
				msg("Accepted to fill up the fuel tank");
				receivedMsg = -1;
				break;
			case Message.fillingUpResevoir:
				msg("Recieved message: WAITING.. TO FILLUP THE RESERVOIR");
				receivedMsg = -1;
				break;
			case Message.takeOffTime:
				msg("Recieved message from " + supervisor.getName() + " : TIME TO TAKEOFF ");
				msg("Again started cruising..");
				setCurrentState(SpaceFleetClients.cruising);
				receivedMsg = -1;
				break;
			}
		}
		msg("Stopped");
	}
}