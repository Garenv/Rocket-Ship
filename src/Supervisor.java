import java.io.*;
import java.util.*;

public class Supervisor extends Thread implements Serializable{
	public static long time = System.currentTimeMillis();
	public static final long landingTime = 5*60*1000; // 5 minutes
	private SpaceFleetClients sfc;

	public Supervisor(int id) {
		setName("SupervisorThread-"+id);
	}

	public void setSfc(SpaceFleetClients sfc) {
		this.sfc = sfc;
	}

	// Printing the message
	public void msg(String m) { 
		System.out.println("[" + (System.currentTimeMillis() - time) + "] " + getName() + ": " + m); 
	}

	// Sending signals to shuttles
	private void sendMsg(Shuttle sh, int msg){
		String msgStr = null;
		switch(msg) {
		case Message.takeOffTime:
			msgStr = "TIME TO TAKEOFF";
			break;  
		}
		msg("sent message: " + msgStr + " to " + sh.getName());
		sh.setReceivedMsg(msg);
	}

	@Override
	public void run(){
		while (System.currentTimeMillis() - time < SpaceFleetClients.oneDay) {
			ArrayList<Shuttle> shuttleList = SpaceFleetClients.getShuttleList();
			for (Shuttle shuttle : shuttleList) {
				if(shuttle.getCurrentState( )== SpaceFleetClients.landingArea){
					if(System.currentTimeMillis() - shuttle.getLandedTime() > landingTime){
						// It's time to take off!!!!
						if(shuttle.getTakeOffChance()>0)
							sendMsg(shuttle, Message.takeOffTime);
						else
							msg(shuttle.getName()+" has used at least 3 cruises.");
					}
				}
			}
		}
	}
}