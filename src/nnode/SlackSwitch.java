package nnode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import android.texting.TextHandler;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import shared.event.EventList;
import shared.event.Message;
import shared.event.OGIMessage;
import shared.logic.Narrator;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.support.Communicator;
import shared.logic.support.CommunicatorHandler;
import shared.logic.support.rules.Rules;
import shared.logic.templates.BasicRoles;
import shared.packaging.Packager;

public class SlackSwitch {

	public static void main( String[] args ) throws IOException, JSONException {
    	new SlackSwitch().start();
    }
	
	
	void start() throws IOException{
		System.out.print("java starting!!");
    	setupSocket();
    	
    	Scanner scan = new Scanner( input );
    	
    	startTimer();

    	while(scan.hasNextLine()){
    		String message = scan.nextLine().replace("\n", "");
    		try{
        		
        		//addToLog(message);
                //System.out.println("(heroku -> java): " + message);
        		handleMessage(message);
        	//i++;
        	}catch(JSONException e){
        		System.err.println("[ " + message + " ] was invalid");
        		e.printStackTrace();
        	}catch(NullPointerException f){
        		f.printStackTrace();
        	}catch(Throwable t){
        		t.printStackTrace();
        		break;
        	}
    	}
    	
    	System.out.println("closing");
    	
        scan.close();
        if(writer != null){
        	writer.close();
        	socket.close();
        	server.close();
        }
	}
	
	Narrator n;
	ServerSocket server;
	Socket socket;
	PrintWriter writer;
	InputStream input;
	
	void setupSocket() throws IOException{
		if(server != null)
			return;
    	server = new ServerSocket(1337);
    	socket = server.accept();
    	input = socket.getInputStream();
    	writer = new PrintWriter(socket.getOutputStream(),false);
    }

	private void setupNarrator(){
		n = Narrator.Default();
		
		n.addRole(BasicRoles.Citizen());
		
		n.addRole(BasicRoles.Sheriff());
		n.addRole(BasicRoles.Mafioso());
		
		n.getRules().setBool(Rules.DAY_START, Narrator.NIGHT_START);
	}
	
	private void handleMessage(String s) throws JSONException{
		System.out.println(s);
		JSONObject jo = new JSONObject(s);
		
		String message = jo.getString("message");
		String from = jo.getString("from");
		if(from.equals("narrator") && message.equals("start")){
			startGame(jo);
			return;
		}
		Player owner = n.getPlayerByName(from);
		if(owner == null)
			return;

		try{
			th.text(owner, message, false);
		}catch (Throwable t){
			t.printStackTrace();
			new OGIMessage(owner, "Server : " + t.getMessage());
    	}
		
	}
	
	private SwitchHandler th;
	void startGame(JSONObject jo) throws JSONException{
		if(n == null || (n.isStarted() && !n.isInProgress()))
			setupNarrator();
		JSONArray jArray = jo.getJSONArray("players");
		String playerName;
		for(int i = 0; i < jArray.length(); i++){
			playerName = jArray.getString(i);
			if(!playerName.contains("narrator"))
				n.addPlayer(playerName, new SlackCommunicator(this));
		}
		
		th = new SwitchHandler(n, n.getAllPlayers());
		n.startGame();
		th.broadcast("I will accept messages in this channel if they start with a \"-\"");
	}
	
	private class SwitchHandler extends TextHandler{

		public SwitchHandler(Narrator n, PlayerList texters) {
			super(n, texters);
		}
		
		
		public void broadcast(String message){

			Player p = null;
			if(p != SlackCommunicator.lastPlayer){
				sendToSlack();
				SlackCommunicator.lastPlayer = p;
			}
			SlackCommunicator.message += (message + "\n");
		}
		
	}
	
	public synchronized void sendToSlack() {
		if(SlackCommunicator.message == "")
			return;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		JSONObject jo = new JSONObject();
		try{
			jo.put("message", SlackCommunicator.message);
			Player p = SlackCommunicator.lastPlayer;
			if(p == null)
				jo.put("name", "narrator");
			else
				jo.put("name", p.getName());
		}catch(JSONException e){
			
		}

		writer.println(jo.toString() + "$$");
		writer.flush();
		SlackCommunicator.message = "";
	}
	
	private static class SlackCommunicator extends Communicator{

		static SlackSwitch ss;
		public SlackCommunicator(SlackSwitch ss){
			SlackCommunicator.ss = ss;
		}
		
		private static Player lastPlayer;
		private static String message = "";
		public void sendMessage(Message e) {
			
			Player p = getPlayer();
			String message = e.access(p, false);
			
			if(p != lastPlayer){
				sendToSlack();
				lastPlayer = p;
			}
			SlackCommunicator.message += (message + "\n");
		}
		
		private static void sendToSlack(){
			ss.sendToSlack();
		}

		
		public void sendMessage(EventList message) {
			for(Message e: message){
				sendMessage(e);
			}
			
		}

		public void writeToParcel(Packager p, CommunicatorHandler ch) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void getFromParcel(Packager p) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Communicator copy() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	
	
	public void startTimer(){
		new Thread(new Runnable(){
			public void run() {
				while(true){
					try {
						Thread.sleep(1500);
					} catch (InterruptedException e) {
					}
					sendToSlack();
				}
			}
			
		}).start();;
	}
}
