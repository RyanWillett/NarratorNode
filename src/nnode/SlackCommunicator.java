package nnode;

import shared.event.EventList;
import shared.event.Message;
import shared.logic.support.Communicator;
import shared.logic.support.CommunicatorHandler;
import shared.packaging.Packager;

public class SlackCommunicator extends Communicator{

	public SlackPlayer sp;
	public SlackCommunicator(SlackPlayer sp){
		this.sp = sp;
	}
	
	public void sendMessage(Message e) {
		String message = e.access(getPlayer(), false);
		sp.sendMessage(message);
	}
	
	public void sendMessage(EventList message) {
		StringBuilder sb = new StringBuilder();
		for(Message m: message){
			sb.append(m.access(getPlayer(), false));
		}
		sp.sendMessage(sb.toString());
	}

	public void writeToParcel(Packager p, CommunicatorHandler ch) {
		
	}

	public void getFromParcel(Packager p) {
		
	}

	public Communicator copy() {
		return null;
	}
		

	
}
