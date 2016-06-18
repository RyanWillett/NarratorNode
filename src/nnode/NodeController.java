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
import json.JSONException;
import json.JSONObject;
import shared.logic.Player;
import shared.logic.support.CommunicatorNull;

public class NodeController implements TextInput{
	
	ServerSocket server;
	Socket socket;
	PrintWriter writer;
	TextHandler th;
	InputStream input;
	protected HashMap<String, Instance> phoneBook;
	protected ArrayList<Instance> instances;
	protected HashMap<String, String> rPhoneBook;

    private static final boolean TEST_MODE = false; 
	
    
    
    public static void main( String[] args ) throws IOException, JSONException {
    	new NodeController();
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
        	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    		file = new File(dateFormat.format(new Date())+".txt");
    	}
    	file.createNewFile();
    	if(!TEST_MODE)
    		in = new FileWriter(file);
    }
  
    
    File file;
    FileWriter in;
    public void addToLog(String message) throws IOException{
    	if(in != null)
    		in.write(message + "\n");
    }
    
    @SuppressWarnings("unused")
	public NodeController() throws IOException, JSONException{
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
        
    	phoneBook = new HashMap<>();
    	rPhoneBook = new HashMap<>();
    	instances = new ArrayList<>();
        
  
        while ((!TEST_MODE || scan.hasNextLine()) ) {
        	
        	String message = scan.nextLine().replace("\n", "");
        	try{
        		
        		addToLog(message);
                //System.out.println("(heroku -> java): " + message);
        		JSONObject jo = new JSONObject(message);
        		if(jo.getBoolean("server")){
        			handleServerMessage(jo);
        		}else{
        			handlePlayerMessage(jo);
        		}
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
    public Instance addPlayer(String email, String name) throws JSONException{
		Instance inst = getInstance();
		phoneBook.put(email, inst);
		rPhoneBook.put(name, email);
		Player newP = inst.addPlayer(name);
		if(inst.n.getAllPlayers().size() == 1)
			inst.host = newP;
		inst.sendGameState();
		
		JSONObject j1 = new JSONObject();
		j1.put("message", name + " has joined.");
		j1.put("server", false);
		j1.put("from", "Server");
		for (Player p: inst.n.getAllPlayers()){
			write(rPhoneBook.get(p.getName()), j1);
		}
		return inst;
		
    }
    
    public void handleServerMessage(JSONObject jo) throws JSONException{
    	switch(jo.getString("message")){
    	case "addplayer":
    		String name = jo.getString("name");
    		String email = jo.getString("email");
    		Instance inst = phoneBook.get(email);
    		if(inst != null){
    			if(inst.n.isInProgress())
    				return;
    			inst.n.getPlayerByName(name).setCommunicator(new CommunicatorNull());
    		}
    		
    		addPlayer(email, name);
    		
    		
    		
    		
    	default:
    		
    	}
    }
    
    public void endTimer(Instance i){
    	
    }
    
    public void handlePlayerMessage(JSONObject jo) throws JSONException{
    	String email = jo.getString("email");
    	Instance i = phoneBook.get(email);
    	if(i == null)
    		i = this.addPlayer(email, jo.getString("name"));
    	i.handlePlayerMessage(jo);
    }
    
    public void write(String receiver, JSONObject jo) throws JSONException{
    	jo.put("email", receiver);
    	String s = jo.toString();
    	if(writer != null){
    		writer.println(s + "$$");
    		writer.flush();
    	}
    	//System.out.println("(java -> heroku): " + s);
    }
    
    

	public void text(Player p, String message, boolean sync) {
		
	}
}