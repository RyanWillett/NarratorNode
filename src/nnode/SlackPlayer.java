package nnode;

import android.texting.StateObject;
import json.JSONException;
import json.JSONObject;
import shared.logic.Player;
import shared.logic.support.Communicator;

public class SlackPlayer extends NodePlayer {

	public SlackPlayer(String name, NodeSwitch nc){
		super(name, nc);
	}

	public void write(JSONObject jo) {}

	
	public void sendMessage(String message) {
		JSONObject jo = new JSONObject();
		try {
			jo.put("server", true);
			jo.put(StateObject.message, "slackMessage");
			jo.put(StateObject.gameID, inst.gameID);
			jo.put("name", name);
			jo.put("slackMessage", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		nc.nodePush(jo);
	}

	
	public boolean notificationCapable() {
		return false;
	}
	
	public Player joinGame(Instance inst, Communicator c) throws JSONException{
		super.joinGame(inst, c);
		inst.slackPlayers.add(this.player);
		inst.slackMap.put(name, this);
		return this.player;
	}
}
