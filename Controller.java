import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller extends Thread implements Serializable {
	public static long time = System.currentTimeMillis();
	private long stationOpenedAt = time;
	private int nextShuttleIndexToRecharge = 0;
	private int reservoirStock = 200; 
	private int recievedMsg = -1, recievedfrom = -1;
	private SpaceFleetClients sfc;

	public Controller(int id) {
		setName("ContollerThread-"+id); 
	}

	public void setSfc(SpaceFleetClients sfc) {
		this.sfc = sfc;
	}

	// Print messages
	public void msg(String m) { 
		System.out.println("[" + (System.currentTimeMillis() - time) + "] " + getName() + " : " + m); 
	}

	// Sending signals to shuttles
	private void sendMsg(Shuttle sh,int msg){
		String msgStr = null;
		switch(msg){
		case Message.stationOpened:
			msgStr="STATION OPENED";
			break;
		case Message.fillingUpResevoir:
			msgStr="WAITING TO FILLUP THE RESERVOIR";
			break;
		}
		msg("sent message: "+msgStr+" to "+sh.getName());
		sfc.sendMessage(new Message(msg, msgStr, Message.shuttle,sh));
	}

	private Shuttle getNextShuttle(){
		Shuttle sh = null;
		// From the list of shuttles, get the index to recharge
		sh = SpaceFleetClients.getShuttleList().get(nextShuttleIndexToRecharge);
		if(nextShuttleIndexToRecharge == SpaceFleetClients.getNumShuttles() - 1) {
			nextShuttleIndexToRecharge = 0;
		} else {
			nextShuttleIndexToRecharge++; 
		}
		return sh;
	}

	public int getreservoirStock() {
		return reservoirStock;
	}

	public void reducereservoirStock(int used) {
		this.reservoirStock -= used;
	}

	public void refillreservoirStock() {
		this.reservoirStock = 200;
	}

	public int getReceivedMsg() {
		return recievedMsg;
	}

	public void setReceivedMsg(int receivedMsg,int from) {
		this.recievedMsg = receivedMsg;
		this.recievedfrom = from;
	}

	public int getCruisingShuttlesCount(){
		int count = 0;
		ArrayList<Shuttle> shuttleList = SpaceFleetClients.getShuttleList();
		for (Shuttle shuttle : shuttleList) {
			if(shuttle.getCurrentState() == SpaceFleetClients.cruising) {
				count++;
			}
		}
		return count;
	}

	@Override
	public void run() {
		msg("Started...");
		while (System.currentTimeMillis() - time < SpaceFleetClients.oneDay) {
			// Checking to open the station
			long currentTimeMillis = System.currentTimeMillis();

			if(currentTimeMillis - stationOpenedAt > SpaceFleetClients.halfAnHour){
				ArrayList<Integer> shuttleGroup = new ArrayList<>();

				// Opened the station
				msg("Opened the station");
				refillreservoirStock();
				stationOpenedAt = currentTimeMillis;

				// Send message to shuttles
				int shuttleCount = SpaceFleetClients.getNumRecharge();

				if(SpaceFleetClients.getNumShuttles() < SpaceFleetClients.getNumRecharge()) {
					shuttleCount = SpaceFleetClients.getNumShuttles();
				}

				if(getCruisingShuttlesCount() < SpaceFleetClients.getNumRecharge()) {
					shuttleCount = getCruisingShuttlesCount();
				}

				// Iterating through the shuttleCount 
				for(int i = 0; i < shuttleCount; i++) {
					Shuttle nextShuttle = null;
					while(true) {
						nextShuttle = getNextShuttle(); // Grab next shuttle
						// If the next shuttle is cruising
						if(nextShuttle.getCurrentState() == SpaceFleetClients.cruising) {
							break;
						}
					}
					shuttleGroup.add(nextShuttle.get_Id());
					sendMsg(nextShuttle, Message.stationOpened);
				}

				try {
					// Set delay
					sleep(2000);
				} catch (InterruptedException ex) {
					Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
				}

				/* Client code*/
				ArrayList<Shuttle> shuttleList = SpaceFleetClients.getShuttleList();
				// Filling up the tanks of shuttles
				for (Integer id : shuttleGroup) {
					int neededFuel = shuttleList.get(id).neededFuel();
					if(getreservoirStock() >= neededFuel){
						// Fill up the tank of shuttle
						msg(shuttleList.get(id).getName()+" is filled up by "+neededFuel+"% needed fuel");
						// Reduce the stock of the station
						reducereservoirStock(neededFuel);
					} else {
						msg("There is not enough fuel to fill a shuttle’s tank, waiting to refill the station");
						// Send the message to next shuttles
						sendMsg(shuttleList.get(id), Message.fillingUpResevoir);
						// Refill the station
						refillreservoirStock();
						msg(shuttleList.get(id).getName() + " is filled up by " + neededFuel + "% needed fuel");
						// Reduce the stock of the station
						reducereservoirStock(neededFuel);
					}
				}

				// Move to landing/takeoff area
				for (Integer id : shuttleGroup) {
					Shuttle shuttleGet = shuttleList.get(id);
					boolean toLand = shuttleGet.toLand();
					if(toLand) {
						// Land the shuttle
						int takeOffChance = shuttleGet.getTakeOffChance();
						shuttleGet.setTakeOffChance(takeOffChance - 1);
						shuttleGet.setCurrentState(SpaceFleetClients.landingArea);
						shuttleGet.setLandedTime(System.currentTimeMillis());
						msg(shuttleGet.getName() + " was moved to the landing/takeoff area");
					}  
				}
			} 
		}
		msg("Stopped!");   
	}
}