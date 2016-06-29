package nnode;

import json.JSONException;
import json.JSONObject;
import shared.event.EventList;
import shared.event.Message;
import shared.logic.support.Communicator;
import shared.logic.support.CommunicatorHandler;
import shared.packaging.Packager;



public class NodeCommunicator extends Communicator{
    	
	NodeSwitch nc;
	NodePlayer np;
	public NodeCommunicator(NodeSwitch nc, NodePlayer np){
		this.nc = nc;
		this.np = np;
	}
	
	public static JSONObject getJObject(String completedMessage) throws JSONException{
		JSONObject j = new JSONObject();
		j.put("message", completedMessage);
		j.put("server", false);
		return j;
	}
	
	public void sendToNC(String completedMessage){
		if(nc != null){
			try{
				JSONObject j = getJObject(completedMessage);
				nc.write(np, j);
			}catch(JSONException e){}
		}
	}
	public void sendMessage(Message f) {
		sendToNC(f.access(getPlayer(), true));
		
	}
	public void sendMessage(EventList eList){
		StringBuilder sb = new StringBuilder();
		for(Message e: eList){
			sb.append(e.access(getPlayer().getName(), true) + "\n");
		}
		sendToNC(sb.toString());
	}
	public void writeToParcel(Packager p, CommunicatorHandler ch) {

	}

	public void getFromParcel(Packager p) {
		
	}

	public Communicator copy() {
		return null;
	}
}
