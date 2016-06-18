package nnode;

import json.JSONException;
import json.JSONObject;
import shared.event.Event;
import shared.logic.Player;
import shared.logic.support.Communicator;
import shared.logic.support.CommunicatorHandler;
import shared.packaging.Packager;



public class NodeCommunicator extends Communicator{
    	
	NodeController nc;
	public NodeCommunicator(NodeController nc){
		this.nc = nc;
	}
	public void sendMessage(Event f) {
		Player p = getPlayer();
		JSONObject j = new JSONObject();
		try {
			j.put("message", f.access(p, true));
			j.put("server", false);
			String email = nc.rPhoneBook.get(p.getName());
			if(nc != null)
				nc.write(email, j);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	public void writeToParcel(Packager p, CommunicatorHandler ch) {

	}

	public void getFromParcel(Packager p) {
		
	}

	public Communicator copy() {
		return null;
	}
}
