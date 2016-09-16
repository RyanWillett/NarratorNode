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
import java.util.Random;
import java.util.Scanner;

import android.texting.StateObject;
import android.texting.TextHandler;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import shared.logic.Player;
import shared.logic.support.Communicator;

public class NodeSwitch{
	
	ServerSocket server;
	Socket socket;
	PrintWriter writer;
	TextHandler th;
	InputStream input;
	//protected HashMap<String, Instance> phoneBook;
	public ArrayList<Instance> instances;
	protected HashMap<String, WebPlayer> phoneBook;
	

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
    private static final String[] BAD_WORDS = {
    		"ANUS", "ARSE", "CLIT", "COCK", "COON",
    		"CUNT", "DAGO", "DAMN", "DICK", "DIKE",
    		"DYKE", "FUCK", "GOOK", "HEEB", "HELL",
    		"HOMO", "JIZZ", "KIKE", "KUNT", "KYKE",
    		"MICK", "MUFF", "PAKI", "PISS", "POON",
    		"PUTO", "SHIT", "SHIZ", "SLUT", "SMEG",
    		"SPIC", "SPIC", "TARD", "TITS", "TWAT",
    		"WANK"};
    private ArrayList<String> activeIDs;
	public NodeSwitch() {
		lobbyList = new ArrayList<>();
    	lobbyMessages = new ArrayList<>();
    	phoneBook = new HashMap<>();
    	instances = new ArrayList<>();
    	r = new Random();
    	idToInstance = new HashMap<>();
    	
    	activeIDs = new ArrayList<String>();
    	for(String bad_word: BAD_WORDS){
    		activeIDs.add(bad_word);
    	}
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
        	}catch(NullPointerException f){
        		f.printStackTrace();
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
    	if(jo.has("slack") && jo.getBoolean("slack")){
    		handleSlackMessage(jo);
    	}else if(jo.has("server") && jo.getBoolean("server")){
			handleServerMessage(jo);
		}else{
			handlePlayerMessage(jo);
		}
    }
    
    private void handleSlackMessage(JSONObject jo) throws JSONException{
    	Instance i = idToInstance.get(jo.get("instanceID"));
    	switch (jo.getString(StateObject.message)){
    		case "addPlayer":
    			String name = jo.getString("slackName");
    			SlackPlayer sp = new SlackPlayer(name, this);
    			Communicator c = new SlackCommunicator(sp); 
    	    	sp.joinGame(i, c);
    			break;
    		case "slackUserInput":
    			sp = i.slackMap.get(jo.getString("from"));
    			i.th.text(sp.player, jo.getString("slackMessage"), false);
    			break;
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
    
    private static final int offset = (int) 'A';
    private Random r;
    
    //could be optimized by not selecting 'bad numbers' first
    
    private HashMap<String, Instance> idToInstance;
    public String getID(Instance i){
    	String id = getID();
    	idToInstance.put(id, i);
    	return id;
    }
    public void removeInstance(String id){
    	idToInstance.remove(id);
    }
    public String getID(){
    	int id = r.nextInt(456976) + 1;
    	StringBuilder sb = new StringBuilder();
    	
    	int i;
    	while(sb.length() < 4){
    		i = (id % 26) + offset;
    		sb.append(((char) i));
    		id -= (i - offset);
    		id /= 26;
    	}
    	String word = sb.toString();
    	if(activeIDs.contains(word))
    		return getID();
    	return word;
    }
    
    public Instance getInstance(){
    	if(instances.isEmpty()){
    		instances.add(new Instance(this));
    		return instances.get(0);
    	}
    	Instance inst = instances.get(0);
    	for(Instance i: instances){
    		if(i.n.getPlayerCount() > inst.n.getPlayerCount() && !i.isFull()){
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
		for(WebPlayer np: lobbyList){
			gLobbyPlayers.put(np.name);
		}
		jo.put("playerList", gLobbyPlayers);
		for(WebPlayer np: lobbyList){
			np.write(jo);
		}
    }
    
    private WebPlayer addNodePlayer(String name) throws JSONException{
    	WebPlayer np = new WebPlayer(name, this); 
		phoneBook.put(name, np);
    	
    	return np;
    }
    
    public void joinLobby(WebPlayer np) throws JSONException {
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
		np.write(jo);
	}
    
    public void handleServerMessage(JSONObject jo) throws JSONException{
		String name = jo.getString("name");
		WebPlayer np = phoneBook.get(name);
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
    
    private void removePlayerFromLobby(WebPlayer np) throws JSONException{
		lobbyList.remove(np);
		onLobbyListChange();
    }
    
    private ArrayList<LobbyMessage> lobbyMessages;
    protected ArrayList<WebPlayer> lobbyList;
	public SwitchListener switchListener;
    private void addMessage(WebPlayer nPlayer, String message) throws JSONException{
    	
    	LobbyMessage messageToPush = new LobbyMessage(nPlayer, message);
    	lobbyMessages.add(messageToPush);
    	if(lobbyMessages.size() > 100)
    		lobbyMessages.remove(0);
    	JSONObject jo;
    	for(WebPlayer np: lobbyList){
    		jo = new JSONObject();
    		JSONArray jArray = new JSONArray();
    		jArray.put(messageToPush.access(np));
    		jo.put("message", jArray);
    		jo.put("lobbyUpdate", true);
    		np.write(jo);
    	}
    }
    public void handlePlayerMessage(JSONObject jo) throws JSONException{
    	String name = jo.getString("name");
    	WebPlayer np = phoneBook.get(name);
    	Instance i = np.inst;
    	if(i == null){
        	Communicator c = new WebCommunicator(this, np);
    		String message = jo.getString("message");
    		if(jo.has("action") && jo.getBoolean("action")){
    			if(message.equals("joinPublic")){
    				removePlayerFromLobby(np);
    				i = getInstance();
    				np.joinGame(i, c);
    			}else if(message.equals("hostPublic")){
    				removePlayerFromLobby(np);
    				i = new Instance(this);
    				instances.add(i);
    				np.joinGame(i, c);
    			}else if(message.equals("joinPrivate")){
    				i = getInstance(jo.getString("hostName"));
    				if(i != null){
        				removePlayerFromLobby(np);
    					np.joinGame(i, c);
    				}else{
    					i = idToInstance.get(jo.getString("hostName").toUpperCase());
    					if(i != null){ // observer mode
    						i.addObserver(np);
    					}else{
    						np.sendLobbyMessage("Couldn't find that user!");
    					}
    				}
    					
    			}
    		}else
    			addMessage(np, message);
    	}else
    		i.handlePlayerMessage(np, jo);
    }
    
    public void write(String name, JSONObject jo) throws JSONException{
    	phoneBook.get(name).write(jo);
    }

    void serverWrite(JSONObject jo){
		try {
			jo.put("server", true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		nodePush(jo);
	}
    
    void nodePush(JSONObject jo){
    	String s = jo.toString();
    	if(writer != null){
    		writer.println(s + "$$");
    		writer.flush();
    	}else if(switchListener != null){
    		switchListener.onSwitchMessage(s);
    	}
    }
    
    

	public void text(Player p, String message, boolean sync) {
		
	}

	public interface SwitchListener{
		public void onSwitchMessage(String s);
	}

	public void sendNotification(ArrayList<NodePlayer> arrayList, String title, String subtitle) {
		JSONObject jo = new JSONObject();
		try {
			jo.put(StateObject.message, "sendNotification");
			JSONArray jRecipients = new JSONArray();
			for(NodePlayer np: arrayList){
				if(np.notificationCapable())
					jRecipients.put(np.name);
				//else
					//System.out.println(np.name + " is active so, won't be sending something to him");
			}
			if(jRecipients.length() == 0)
				return;
			jo.put("recipients", jRecipients);
			jo.put("title", title);
			jo.put("subtitle", subtitle);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		serverWrite(jo);
	}
	
	
}