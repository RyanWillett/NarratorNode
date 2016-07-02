package nnode;

import json.JSONException;
import json.JSONObject;

public class RoughTester {
	public static void main(String[] args) throws JSONException{
		
		RoughTester rt = new RoughTester();
		
		rt.addPlayer("Bob");
		rt.joinLobby("Bob");
		rt.say("Bob", "a");
		
		System.out.println(rt.ns.instances.get(0).n.getHappenings());
		
	}
	
	
	
	private void say(String name, String message) throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("server", false);
		jo.put("name", name);
		jo.put("message", "say null hi");
		
		
		
		String m = jo.toString();
		ns.handleMessage(m);
	}



	private void joinLobby(String name) throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("server", false);
		jo.put("name", name);
		jo.put("action", true);
		jo.put("message", "joinPublic");

		String message = jo.toString();
		ns.handleMessage(message);
	}



	private void addPlayer(String name) throws JSONException {
		JSONObject jo = new JSONObject();
		jo.put("server", true);
		jo.put("name", name);
		jo.put("message", "greeting");
		
		String message = jo.toString();
		ns.handleMessage(message);
		
	}


	NodeSwitch ns;
	public RoughTester(){
		ns = new NodeSwitch();
		
	}
}
