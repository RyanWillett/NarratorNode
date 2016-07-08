package nnode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import android.texting.TextHandler;
import android.texting.TextInput;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import shared.logic.Player;

public class NodeSwitch implements TextInput{
	
	ServerSocket server;
	Socket socket;
	PrintWriter writer;
	TextHandler th;
	InputStream input;
	//protected HashMap<String, Instance> phoneBook;
	protected ArrayList<Instance> instances;
	protected HashMap<String, NodePlayer> phoneBook;
	

    private static final boolean TEST_MODE = false; 
	
    
    
    public static void main( String[] args ) throws IOException, JSONException {
    	new NodeSwitch().start();
    }
    
    private void setupSocket() throws IOException{
    	server = new ServerSocket(1337);
    	socket = server.accept();
    	input = socket.getInputStream();
    	writer = new PrintWriter(socket.getOutputStream(),false);
    	
    }
    private void setupLogger() throws IOException{
    	if(TEST_MODE){
    		String path = "C:\\Users\\Michael\\Desktop\\HerokuTest\\node-ws-test\\src\\";
    		file = new File("C:\\Users\\Michael\\Desktop\\HerokuTest\\node-ws-test\\src\\" + "ab.txt");
    		
    	}else{
        	DateFormat dateFormat = new SimpleDateFormat("MM-dd-HH-mm");
    		file = new File(dateFormat.format(new Date())+".txt");
    	}
    	//file.createNewFile();
    	//if(!TEST_MODE)
    		//in = new FileWriter(file);
    }
  
    
    File file;
    FileWriter in;
    public void addToLog(String message) throws IOException{
    	if(in != null)
    		in.write(message + "\n");
    }
    
	public NodeSwitch() {
		lobbyList = new ArrayList<>();
    	lobbyMessages = new ArrayList<>();
    	phoneBook = new HashMap<>();
    	instances = new ArrayList<>();
	}
    
    @SuppressWarnings("unused")
	public void start()throws IOException, JSONException{
    	System.out.print("java starting");
    	if(!TEST_MODE)
    		setupSocket();
    	setupLogger();
    	
    	
    	
    	Scanner scan;
    	if(TEST_MODE){
    		scan = new Scanner(file);
    	}else{
            scan = new Scanner( input );
    	}
        
        
  
        while ((!TEST_MODE || scan.hasNextLine()) ) {
        	
        	String message = scan.nextLine().replace("\n", "");
        	try{
        		
        		//addToLog(message);
                //System.out.println("(heroku -> java): " + message);
        		handleMessage(message);
        		
        	//i++;
        	}catch(JSONException e){
        		System.err.println("[ " + message + " ] was invalid");
        		e.printStackTrace();
        	}catch(Throwable t){
        		t.printStackTrace();
        		break;
        	}

        }
        System.out.println("java ending");
        if(in != null)
        	in.close();
        scan.close();
        if(writer != null){
        	writer.close();
        	socket.close();
        	server.close();
        }
    }
    
    public void handleMessage(String message) throws JSONException{
    	JSONObject jo = new JSONObject(message);
		if(jo.has("server") && jo.getBoolean("server")){
			handleServerMessage(jo);
		}else{
			handlePlayerMessage(jo);
		}
    }
    
    public Instance getInstance(String name){
    	name = name.toLowerCase();
    	for(Instance inst: instances){
    		if(inst.host.getName().toLowerCase().equals(name) && !inst.isFull()){
    			return inst;
    		}
    	}
    	return null;
    }
    
    public Instance getInstance(){
    	if(instances.isEmpty()){
    		instances.add(new Instance(this));
    		return instances.get(0);
    	}
    	Instance inst = instances.get(0);
    	for(Instance i: instances){
    		if(i.n.getPlayerCount() > inst.n.getPlayerCount()){
    			inst = i;
    		}
    	}
    	return inst;
    }
    
    //ensured that name isn't in the books yet
    
    
    void onLobbyListChange() throws JSONException{
    	JSONObject jo = new JSONObject();
		jo.put("lobbyUpdate", true);
		
		JSONArray gLobbyPlayers = new JSONArray();
		for(NodePlayer np: lobbyList){
			gLobbyPlayers.put(np.name);
		}
		jo.put("playerList", gLobbyPlayers);
		for(NodePlayer np: lobbyList){
			np.write(jo);
		}
    }
    
    private NodePlayer addNodePlayer(String name) throws JSONException{
    	NodePlayer np = new NodePlayer(name, this); 
		phoneBook.put(name, np);
    	
    	return np;
    }
    
    public void joinLobby(NodePlayer np) throws JSONException {
		lobbyList.add(np);
		onLobbyListChange();

		JSONObject jo = new JSONObject();
		jo.put("lobbyUpdate", true);
		JSONArray jMessages = new JSONArray();
		for (LobbyMessage message: lobbyMessages){
			jMessages.put(message.access(np));
		}
		jo.put("message", jMessages);
		jo.remove("server");
		jo.put("reset", true);
		jo.put("sever", false);
		write(np, jo);
	}
    
    public void handleServerMessage(JSONObject jo) throws JSONException{
		String name = jo.getString("name");
		NodePlayer np = phoneBook.get(name);
		if(np == null){
			np = addNodePlayer(name);
		}
    	switch(jo.getString("message")){
    	case "greeting":
    		if(np.isActive())
    			return;
			np.setActive();
    		if(np.isInLobby()){
        		joinLobby(np);
        		
    			return;
    		}else{
    			np.inst.sendGameState(np.player);
    			np.inst.resetChat(np.player);
    		}
    		break;
    	
    	case "disconnect":
    		if(np.isInLobby()){
    			phoneBook.remove(np.name);
    			removePlayerFromLobby(np);
    		}else{
    			if(!np.inst.n.isInProgress() && np.inst.n.isStarted())
        			phoneBook.remove(np.name);
    		}
    		np.setInactive();
    		break;
    	default:
    		
    	}
    }
    
    private void removePlayerFromLobby(NodePlayer np) throws JSONException{
		lobbyList.remove(np);
		onLobbyListChange();
    }
    
    private ArrayList<LobbyMessage> lobbyMessages;
    protected ArrayList<NodePlayer> lobbyList;
    private void addMessage(NodePlayer nPlayer, String message) throws JSONException{
    	
    	LobbyMessage messageToPush = new LobbyMessage(nPlayer, message);
    	lobbyMessages.add(messageToPush);
    	if(lobbyMessages.size() > 100)
    		lobbyMessages.remove(0);
    	JSONObject jo;
    	for(NodePlayer np: lobbyList){
    		jo = new JSONObject();
    		JSONArray jArray = new JSONArray();
    		jArray.put(messageToPush.access(np));
    		jo.put("message", jArray);
    		jo.put("lobbyUpdate", true);
    		write(np, jo);
    	}
    }
    public void handlePlayerMessage(JSONObject jo) throws JSONException{
    	String name = jo.getString("name");
    	NodePlayer np = phoneBook.get(name);
    	Instance i = np.inst;
    	if(i == null){
    		String message = jo.getString("message");
    		if(jo.getBoolean("action")){
    			if(message.equals("joinPublic")){
    				removePlayerFromLobby(np);
    				i = getInstance();
    				np.joinGame(i);
    			}else if(message.equals("hostPublic")){
    				removePlayerFromLobby(np);
    				i = new Instance(this);
    				instances.add(i);
    				np.joinGame(i);
    			}else if(message.equals("joinPrivate")){
    				i = getInstance(jo.getString("hostName"));
    				if(i != null){
        				removePlayerFromLobby(np);
    					np.joinGame(i);
    				}else{
    					np.sendLobbyMessage("Couldn't find that user!");
    				}
    					
    			}
    		}else
    			addMessage(np, message);
    	}else
    		i.handlePlayerMessage(np, jo);
    }
    
    public void write(String name, JSONObject jo) throws JSONException{
    	write(phoneBook.get(name), jo);
    }
    public void write(NodePlayer np, JSONObject jo) throws JSONException{
    	jo.put("name", np.name);
    	String s = jo.toString();
    	//System.out.println("(java -> heroku): " + s + "\n");
    	if(writer != null){
    		writer.println(s + "$$");
    		writer.flush();
    	}
    }
    
    

	public void text(Player p, String message, boolean sync) {
		
	}

	
}