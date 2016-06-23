package nnode;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import shared.logic.Player;
import shared.logic.support.CommunicatorNull;

public class NodePlayer {

	protected Instance inst;
	protected String name;
	protected String email;
	protected Player player;
	private NodeSwitch nc;
	private boolean active;
	
	public NodePlayer(String email, String name, NodeSwitch nc){
		this.email = email;
		this.name  = name;
		this.nc = nc;
	}
	
	public int hashcode(){
		return email.hashCode();
	}

	public boolean isInLobby() {
		return inst == null;
	}
	
	public boolean equals(Object o){
		if(o == null)
			return false;
		if(o.getClass() != getClass())
			return false;
		return email.equals(((NodePlayer) o).email);
		
	}

	public void write(JSONObject jo) throws JSONException {
		nc.write(this, jo);
		
	}

	public void setActive() throws JSONException {
		active = true;
		if(isInGame())
			inst.onPlayerListStatusChange();
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

	public void joinGame(Instance inst) throws JSONException {
		this.inst = inst;
		inst.addPlayer(this);
		
	}

	public void sendLobbyMessage(String string) throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("lobbyUpdate", true);
		JSONArray jArray = new JSONArray();
		jArray.put(string);
		jo.put("message", jArray);
		nc.write(this, jo);
	}

	public void leaveGame() throws JSONException {
		inst.removePlayer(this);
		inst = null;
		nc.joinLobby(this);
		nc.onLobbyListChange();
	}

	private static final long MINUTE = 60000; 
	private long lastPinged = -1;
	public void ping(NodePlayer requester) throws JSONException {
		long currentTime = System.currentTimeMillis();
		JSONObject jo;
		if(currentTime - lastPinged < 2 * MINUTE){
			jo = new JSONObject();
			jo.put("message", name + " cannot be pinged again so soon.");
			requester.write(jo);
		}else{
			jo = Instance.GetGUIObject();
			jo.put("ping", true);
			write(jo);
			lastPinged = currentTime;
		}
	}

}
