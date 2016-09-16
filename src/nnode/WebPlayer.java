package nnode;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import shared.logic.Player;
import shared.logic.support.Communicator;
import shared.logic.support.CommunicatorNull;

public class WebPlayer extends NodePlayer{


	private boolean active = false;
	
	public WebPlayer(String name, NodeSwitch nc){
		super(name, nc);
	}
	
	public int hashcode(){
		return name.hashCode();
	}

	public boolean isInLobby() {
		return inst == null;
	}
	
	public boolean equals(Object o){
		if(o == null)
			return false;
		if(o.getClass() != getClass())
			return false;
		return name.equals(((WebPlayer) o).name);
		
	}

	public void write(JSONObject jo) throws JSONException {
		jo.put("name", name);
    	//System.out.println("(java -> heroku): " + s + "\n");
    	nc.nodePush(jo);
	}

	public void setActive() throws JSONException {
		active = true;
		if(isInGame())
			inst.onPlayerListStatusChange();
	}
	
	public Player joinGame(Instance inst, Communicator c) throws JSONException{
		super.joinGame(inst, c);
		inst.webUsers.add(this.player);
		inst.sendGameState(this.player);
		return this.player;
	}
	
	public boolean isActive(){
		return active;
	}

	public void setInactive() throws JSONException {
		active = false;
		if(isInGame()){
			inst.onPlayerListStatusChange();
			if(inst.n.isStarted() && !inst.n.isInProgress()){
				inst = null;
				player.setCommunicator(new CommunicatorNull());
				player = null;
			}
		}
	}

	public boolean isInGame() {
		return inst != null;
	}

	

	public void sendLobbyMessage(String string) throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("lobbyUpdate", true);
		JSONArray jArray = new JSONArray();
		jArray.put(string);
		jo.put("message", jArray);
		write(jo);
	}

	public void leaveGame() throws JSONException {
		inst.removePlayer(this);
		inst = null;
		this.player = null;
		nc.joinLobby(this);
		nc.onLobbyListChange();
	}

	private static final long MINUTE = 60000; 
	private long lastPinged = -1;
	public void ping(NodePlayer requester) throws JSONException {
		long currentTime = System.currentTimeMillis();
		JSONObject jo;
		if(currentTime - lastPinged < 2 * MINUTE){
			requester.sendMessage(name + " cannot be pinged again so soon.");
			
		}else{
			jo = Instance.GetGUIObject();
			jo.put("ping", true);
			write(jo);
			lastPinged = currentTime;
		}
	}

	public void sendMessage(String s){
		JSONObject jo = new JSONObject();
		try{
			jo.put("message", s);
			write(jo);
		}catch(JSONException e){
			
		}
		
	}

	public boolean notificationCapable() {
		return !isActive();
	}
}
