package nnode;

import json.JSONException;
import json.JSONObject;
import shared.logic.Player;
import shared.logic.support.Communicator;

public abstract class NodePlayer {

	public String name;
	public Player player;
	public Instance inst;
	public NodeSwitch nc;
	public NodePlayer(String name, NodeSwitch nc){
		this.name = name;
		this.nc = nc;
	}
	
	public Player joinGame(Instance inst, Communicator c) throws JSONException {
		this.inst = inst;
		return inst.addPlayer(this, c);
	}

	public boolean isActive() {
		return false;
	}

	public abstract void write(JSONObject jo) throws JSONException;

	public void ping(WebPlayer np) throws JSONException{
		// TODO Auto-generated method stub
		
	}

	public abstract void sendMessage(String string);

	public abstract boolean notificationCapable();
	
}
