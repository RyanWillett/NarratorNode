package nnode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.texting.StateObject;
import android.texting.TextHandler;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import shared.event.ChatMessage;
import shared.event.EventList;
import shared.event.Message;
import shared.event.OGIMessage;
import shared.logic.Member;
import shared.logic.Narrator;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.Team;
import shared.logic.exceptions.NarratorException;
import shared.logic.exceptions.PlayerTargetingException;
import shared.logic.listeners.NarratorListener;
import shared.logic.support.CommandHandler;
import shared.logic.support.Communicator;
import shared.logic.support.Constants;
import shared.logic.support.Faction;
import shared.logic.support.FactionManager;
import shared.logic.support.Random;
import shared.logic.support.RoleTemplate;
import shared.logic.support.StringChoice;
import shared.logic.support.rules.Rules;
import shared.logic.templates.BasicRoles;
import shared.roles.Blocker;
import shared.roles.CultLeader;
import shared.roles.Driver;
import shared.roles.MassMurderer;
import shared.roles.RandomRole;
import shared.roles.Role;
import shared.roles.SerialKiller;

public class Instance implements NarratorListener{

	public Narrator n;
	protected CommandHandler ch;
	HashMap<Player, NodePlayer> phoneBook;
	HashMap<String, SlackPlayer> slackMap;
	protected PlayerList webUsers, slackPlayers;
	private NodeSwitch nc;
	String gameID;
	
	public Instance(NodeSwitch nc){
		this.nc = nc;
		phoneBook = new HashMap<>();
		n = Narrator.Default();
    	n.addListener(this);
		
		Rules r = n.getRules();
		r.setBool(Rules.DAY_START, Narrator.DAY_START);
		r.setInt(Rules.DAY_LENGTH, 5);
		r.setInt(Rules.NIGHT_LENGTH, 2);
        
        fManager = new FactionManager(n);
        ch = new CommandHandler(n);
        
        gameID = nc.getID(this);
        observers = new ArrayList<>();
        webUsers = new PlayerList();
        slackPlayers = new PlayerList();
        slackMap = new HashMap<>();
	}
	
	public void removePlayer(NodePlayer leaver) throws JSONException {
		n.removePlayer(leaver.player);
		
		if(leaver.player == host){
			if(webUsers.isEmpty()){
				nc.instances.remove(this);
			}else{
				repickers = new PlayerList();
				host = webUsers.get(0);
				sendGameState(host);
			}
		}
		
		phoneBook.remove(leaver.player);
		leaver.player = null;
		leaver.inst = null;
		
		broadcastToSlackUsers(leaver.name + " has left the lobby.");
		new OGIMessage(n.getAllPlayers().remove(slackPlayers), leaver.name + " has left the lobby.");
		

		
		new OGIMessage(n._players, leaver.name + " has left the lobby.");
		
		onPlayerListStatusChange();
	}
	
	public Player addPlayer(NodePlayer np, Communicator c) throws JSONException{
    	Player p = n.addPlayer(np.name, c);
		np.player = p;
		np.inst = this;
		phoneBook.put(p, np);
		
		if(n.getAllPlayers().size() == 1)
			host = n.getAllPlayers().get(0);
		
		sendGameState();
		
		broadcastToSlackUsers(np.name + " has joined the lobby.");
		new OGIMessage(n.getAllPlayers().remove(p).remove(slackPlayers), np.name + " has joined the lobby.");
		
		
		if(n.getPlayerCount() == n.getAllRoles().size())
			sendNotification(host, "Game is now ready to start!");
		
		return p;
	}
	
	private ArrayList<WebPlayer> observers;
	public void addObserver(WebPlayer np) {
		observers.add(np);
		try {
			JSONObject jo = sendGameState((Player) null);
			np.write(jo);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}
		
	public void playerJWrite(Player p, JSONObject j) throws JSONException{
		NodePlayer np = phoneBook.get(p);
		np.write(j);
	}
	static JSONObject GetGUIObject() throws JSONException{
		JSONObject jo = new JSONObject();
		jo.put(StateObject.guiUpdate, true);
		jo.put("server", false);
		jo.put(StateObject.type, new JSONArray());
		return jo;
	}
	
	SwitchHandler th;
	private void startGame(){
		if(nc != null)
			nc.instances.remove(this);
    	n._players.sortByName();
    	
    	//th is initialized first because if the game is started, a message will be sent out
    	//instance will not quite be set yet, and throw null pointers
		th = new SwitchHandler(n, slackPlayers, this);
		n.startGame();
		

		th.broadcast("I will accept messages in this channel if they start with a \"!\"");
	}
	
	class SwitchHandler extends TextHandler{

		private Instance inst;
		public SwitchHandler(Narrator n, PlayerList texters, Instance i) {
			super(n, texters);
			this.inst = i;
		}
		
		public void broadcast(String message){
			inst.broadcastToSlackUsers(message);
		}
		
	}
	
	private long timerStart;
	private long getEndTime(){
		int length = n.isDay() ? n.getRules().getInt(Rules.DAY_LENGTH) : n.getRules().getInt(Rules.NIGHT_LENGTH); 
		return (length * 60000) - (System.currentTimeMillis() - timerStart);
	}
	
	private Thread timer = new Thread();
	public Player host;
	private void startTimer(){
		final boolean isDay = n.isDay();
		synchronized(this){
			timer.interrupt();
		}
		timer = new Thread(new Runnable(){
			public void run() {
				try {
					int length = n.isDay() ? n.getRules().getInt(Rules.DAY_LENGTH) : n.getRules().getInt(Rules.NIGHT_LENGTH);
					Thread.sleep((60000 * length) - 30000);
					broadcastToSlackUsers( "30 seconds left!");
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					return;
				}
				synchronized(this){
					if(n.isDay() == isDay && n.isInProgress())
						if(isDay){
							endDay();
						}else{
							endNight();
						}
				}
			}
		});
		timerStart = System.currentTimeMillis();
		timer.start();;
	}
	
	private void endDay(){
		int minLynch = n.getMinLynchVote();
		while(n.isDay()){
			minLynch --;
			n.checkVote(minLynch);
		}
	}
	private void endNight(){	
		for(Player waiting: n.getLivePlayers().remove(n.getEndedNightPeople()).shuffle(n.getRandom())){
			waiting.endNight();
		}
	}
	
	
	
	
	
	
	
	
	
	FactionManager fManager;
	
	void sendGameState(PlayerList players) throws JSONException{
		for(Player p: players)
			sendGameState(p);
	}
	
	JSONObject sendGameState(Player p) throws JSONException{
		StateObject iObject = getInstObject();
		iObject.addState(StateObject.ROLESLIST);
		iObject.addState(StateObject.PLAYERLISTS);
		iObject.addState(StateObject.DAYLABEL);
		iObject.addState(StateObject.GRAVEYARD);
		
		iObject.addKey(StateObject.gameStart, n.isStarted());
		iObject.addKey(StateObject.showButton, p != null && (!n.isInProgress() || p.isAlive() && (n.isNight() || p.hasDayAction())));
		iObject.addKey(StateObject.endedNight, p != null && n.isNight() && p.endedNight());
		iObject.addKey(StateObject.gameID, gameID);
		iObject.addKey(StateObject.isObserver, p == null);
		
		if(n.isStarted()){
			iObject.addState(StateObject.ROLEINFO);
			iObject.addState(StateObject.ACTIVETEAMS);
			iObject.addKey(StateObject.isDay, n.isDay());
			if(n.isInProgress()){
				iObject.addKey(StateObject.timer, getEndTime());
			}else{
				iObject.addKey(StateObject.isFinished, true);
			}
			if(n.isDay()){
				iObject.addKey(StateObject.skipVote, n.Skipper.getVoters().size());
				iObject.addKey(StateObject.isSkipping, p != null && n.Skipper == p.getVoteTarget());
			}
		}else{
			iObject.addKey(StateObject.isHost, p == host);
			iObject.addKey(StateObject.host, host.getName());
			iObject.addState(StateObject.RULES);
		}
		
		return iObject.send(p);
	}
	
	void sendGameState(){
		try{
			for(Player p: webUsers)
				sendGameState(p);
			JSONObject jo;
			for(WebPlayer observer: observers){
				jo = sendGameState((Player) null);
				observer.write(jo);
			}
		}catch(JSONException e){
			e.printStackTrace();
		}
	}
	
	private void deleteTeam(JSONObject jo) throws JSONException{
		String color = jo.getString("color");
		Team t = n.getTeam(color);
		fManager.removeTeam(color);
		n.removeTeam(t);
		
		sendRules();
	}
	
	private void sendRules() throws JSONException{
		StateObject so = getInstObject().addState(StateObject.RULES);
		so.send(n._players);
		JSONObject jo = so.send((Player) null);
		for(WebPlayer observer: observers){
			observer.write(jo);
		}
	}
	
	private void addTeam(JSONObject jo) throws JSONException{
		String name = jo.getString("teamName").replaceAll(" ", "");
		for(Team t: n.getAllTeams()){
			if(t.getName().equalsIgnoreCase(name)){
				new OGIMessage(host, "That team name is taken!");
				return;
			}
		}

		String color = jo.getString("color");
		if(n.getTeam(color.toUpperCase()) != null || color.toUpperCase().equals(Constants.A_RANDOM) || color.toUpperCase().equals(Constants.A_NEUTRAL)){
			new OGIMessage(host, "That team color is already taken!");
			return;
		}
		
		
		Team newTeam = n.addTeam(color);
		newTeam.setName(name);
		newTeam.setDescription("Custom team implemented by " + host.getName());
		fManager.addFaction(newTeam);
		
		sendRules();
	}
	private void addTeamRole(JSONObject jo) throws JSONException{
		String color = jo.getString("color");
		String simpleName = jo.getString("simpleName");
		
		Faction f = fManager.getFaction(color);
		
		Member newMember = f.makeAvailable(simpleName, fManager);
		
		if(f.members.size() == 2){
			Faction randFac = fManager.getFaction(Constants.A_RANDOM);
			ArrayList<RoleTemplate> list = new ArrayList<>();
			
			RandomRole r = new RandomRole(f.getName() + " Random", f.getColor());
			for(RoleTemplate m: f.members){
				if(!m.isRandom()){
					r.addMember((Member) m);
				}
			}
			list.add(r);
			
			randFac.add(list);
		}else if(f.members.size() > 2){
			Faction randFac = fManager.getFaction(Constants.A_RANDOM);
			for(RoleTemplate rt: randFac.members){
				if(rt.isRandom()){
					RandomRole rr = (RandomRole) rt;
					if(rr.getName().equals(f.getName() + " Random")){
						rr.addMember(newMember);
						break;
					}
				}
			}
		}
		
		sendRules();
	}
	
	private void removeTeamRole(JSONObject jo) throws JSONException{
		String color = jo.getString("color");
		String name = jo.getString("roleName");
		
		Faction f = fManager.getFaction(color);
		
		Member m = f.makeUnavailable(name, fManager);
		
		StateObject s = getInstObject().addState(StateObject.RULES);
		if(n.getAllRoles().contains(m)){
			n.getAllRoles().purge(m);
			s.addState(StateObject.ROLESLIST);
		}
		
		sendRules();
	}
	
	private void removeTeamAlly(JSONObject jo)throws JSONException{
		String teamColor = jo.getString("color");
		String allyColor = jo.getString("ally");
		
		Team team = n.getTeam(teamColor);
		Team ally = n.getTeam(allyColor);
		
		team.setEnemies(ally);

		sendRules();
	}
	
	private void removeTeamEnemy(JSONObject jo)throws JSONException{
		String teamColor = jo.getString("color");
		String enemyColor = jo.getString("enemy");
		
		Team team = n.getTeam(teamColor);
		Team enemy = n.getTeam(enemyColor);
		
		team.setAllies(enemy);

		sendRules();
	}
	
	private StateObject getInstObject(){
		return new InstObject(this);
	}
	
	private static class InstObject extends StateObject{
		
		Instance i;
		JSONObject jo;
		public InstObject(Instance i) {
				super(i.n, i.fManager);
				this.i = i;
				jo = new JSONObject();
				try {
					jo.put(StateObject.type, new JSONArray());
					jo.put(StateObject.guiUpdate, true);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
	
		public boolean isActive(Player p) {
			NodePlayer np = i.phoneBook.get(p);
			if(np == null)
				return false;
			return np.isActive();
		}
	
		public JSONObject getObject() throws JSONException {
			return jo;
		}
	
		public void write(Player p, JSONObject jo){
			NodePlayer np = i.phoneBook.get(p);
			if(np == null)
				return;
			try{
				np.write(jo);
			}catch(JSONException e){
				e.printStackTrace();
			}
		}
	}
	
	private PlayerList repickers = new PlayerList();
	private void repickHost(String message){
		Player potential = webUsers.getPlayerByName(message);
		if(potential != null && phoneBook.get(potential).isActive() && message.length() != 0){
			host = potential;
		}else{
			PlayerList options = webUsers.remove(host).shuffle(new Random());
			Player oldHost = host;
			for(Player p: options){
				if(phoneBook.get(p).isActive()){
					host = p;
					break;
				}
			}
			if(oldHost == host && !options.isEmpty())
				host = options.getRandom(new Random());
		}
		sendGameState();
		repickers.clear();
	}
	
	private void repickRequest(WebPlayer repicker, String message){
		if(repicker.player == host){
			repickHost(message);
		}else{
			if(!repickers.contains(repicker.player)){
				repickers.add(repicker.player);
			}
			StringChoice sc = new StringChoice(repicker.player);
			sc.add(repicker.player, "You");
			StringChoice sc2 = new StringChoice(host);
			sc2.add(host, "you");
			StringChoice have = new StringChoice("has");
			have.add(repicker.player, "have");
			
			new OGIMessage(webUsers, sc, " ", have, " voted to repick ", sc2, ".");
			if(repickers.size() >= webUsers.size()/2 + 1){
				repickHost("");
			}
		}
	}
	boolean slackAdded = false;
	private void addSlack(String message) throws JSONException{
		if(slackAdded)
			return;
		message = message.toLowerCase();
		if(!message.equalsIgnoreCase("sdsc") && !message.equalsIgnoreCase("pepband") && !message.equalsIgnoreCase("thenarrator"))
			return;
		slackAdded = true;
		
		JSONObject jo = new JSONObject();
		jo.put(StateObject.host, host.getName());
		jo.put(StateObject.gameID, gameID);
		jo.put(StateObject.message, "slackAdd");
		jo.put("slackField", message);
		nc.serverWrite(jo);
	}
	
	private String translateName(String input){
		switch(input){
		case BasicRoles.BUS_DRIVER:
			return Driver.ROLE_NAME;
		case BasicRoles.CHAUFFEUR:
			return Driver.ROLE_NAME;
		case BasicRoles.ESCORT:
			return Blocker.ROLE_NAME;
		case BasicRoles.CONSORT:
			return Blocker.ROLE_NAME;
		case MassMurderer.ROLE_NAME:
			return MassMurderer.class.getSimpleName();
		case SerialKiller.ROLE_NAME:
			return SerialKiller.class.getSimpleName();
		case CultLeader.ROLE_NAME:
			return CultLeader.class.getSimpleName();
		default:
			return input;
		}
	}
	
	public synchronized void handlePlayerMessage(WebPlayer np, JSONObject jo) throws JSONException {
		String name = jo.getString("name");
    	String message = jo.getString("message");
    	if(message.length() == 0)
    		return;
    	Player p = np.player;
    	if (p == null)
    		throw new PlayerTargetingException(name + " wasn't found.");
    	if(message.equals(StateObject.requestGameState)){
    		sendGameState(p);
    		return;
    	}
    	
    	if(message.equals(StateObject.requestChat)){
    		p.sendMessage(p.getEvents());
    		return;
		}
    	
    	if(host == p && !n.isInProgress()){
    		if(message.equals(StateObject.addRole)){
    			String color = jo.getString(StateObject.roleColor);
    			String displayed_name = jo.getString(StateObject.roleName);
    			String role_name = translateName(displayed_name);
    			
    			if(Role.isRole(role_name)){
    				n.addRole(role_name, color).setRoleName(displayed_name);
    			}else{
    				RoleTemplate rr = null;
    				switch(role_name){
    				case Constants.TOWN_RANDOM_ROLE_NAME:
    					rr = RandomRole.TownRandom();
    					break;
    				case Constants.TOWN_PROTECTIVE_ROLE_NAME:
    					rr = RandomRole.TownProtective();
    					break;
    				case Constants.TOWN_INVESTIGATIVE_ROLE_NAME:
    					rr = RandomRole.TownInvestigative();
    					break;
    				case Constants.TOWN_KILLING_ROLE_NAME:
    					rr = RandomRole.TownKilling();
    					break;
    				case Constants.YAKUZA_RANDOM_ROLE_NAME:
    					rr = RandomRole.YakuzaRandom();
    					break;
    				case Constants.MAFIA_RANDOM_ROLE_NAME:
    					rr = RandomRole.MafiaRandom();
    					break;
    				case Constants.NEUTRAL_RANDOM_ROLE_NAME:
    					rr = RandomRole.NeutralRandom();
    					break;
    				case Constants.ANY_RANDOM_ROLE_NAME:
    					rr = RandomRole.AnyRandom();
    					break;
    				}
    				if(rr != null)
        				n.addRole(rr);
        			else
        				return;
    			}
    			sendGameState();
    			return;
    		}
    		if(message.equals(StateObject.removeRole)){
    			String color = jo.getString(StateObject.roleColor);
    			String displayed_name = jo.getString(StateObject.roleName);
    			//String role_name = this.translateName(displayed_name);
    			RoleTemplate r = n.getAllRoles().get(displayed_name, color);
    			if(r != null){
    				n.removeRole(r);
    				sendGameState();
    			}
    			return;
    		}
    		if(message.equals(StateObject.ruleChange)){
    			Rules r = n.getRules();
    			jo = jo.getJSONObject(StateObject.ruleChange);
    			JSONObject inputJRule;
    			for(String id: r.rules.keySet()){
    				inputJRule = jo.getJSONObject(id);
    				try{
    					int val = inputJRule.getInt("val");
    					r.setInt(id, val);
    				}catch(JSONException e){
    					boolean val = inputJRule.getBoolean("val");
    					r.setBool(id, val);
    				}
    			}
    			boolean valb;
    			int vali;
    			for(Team t: n.getAllTeams()){
    				if(t.getColor().equals(Constants.A_SKIP))
    					continue;
    				
    				inputJRule = jo.getJSONObject(t.getColor() + "kill");
    				valb = inputJRule.getBoolean("val");
    				t.setKill(valb);
    				
    				inputJRule = jo.getJSONObject(t.getColor() + "identity");
    				valb = inputJRule.getBoolean("val");
    				t.setKnowsTeam(valb);
    				
    				inputJRule = jo.getJSONObject(t.getColor() + "liveToWin");
    				valb = inputJRule.getBoolean("val");
    				t.setMustBeAliveToWin(valb);
    				
    				inputJRule = jo.getJSONObject(t.getColor() + "priority");
    				vali = inputJRule.getInt("val");
    				t.setPriority(vali);
    			}
    			sendRules();
    			return;
    		}
    		
    		if(message.equals("addTeam")){
    			addTeam(jo);
    			return;
    		}
    		else if(message.equals(StateObject.deleteTeam)){
    			deleteTeam(jo);
    			return;
    		}
    		else if(message.equals("addTeamRole")){
    			addTeamRole(jo);
    			return;
    		}
    		else if(message.equals("removeTeamRole")){
    			removeTeamRole(jo);
    			return;
    		}
    		else if(message.equals("removeTeamAlly")){
    			removeTeamAlly(jo);
    			return;
    		}
    		else if(message.equals("removeTeamEnemy")){
    			removeTeamEnemy(jo);
    			return;
    		}
    		
    		if(message.equals(StateObject.startGame)){
    			try{
    				startGame();
    			}catch(NarratorException e){
    				new OGIMessage(p, e.getMessage());
    				
    			}catch(Throwable t){
    				t.printStackTrace();
    			}
    			return;
    		}
    		
    		if(message.startsWith("say null -slack ")){
    			addSlack(message.replace("say null -slack ", ""));
    			return;
    		}
		}

    	if(message.equals(StateObject.leaveGame)){
    		np.leaveGame();
    		return;
    	}
    	if(message.startsWith("say null -ping ")){
    		String m = message.replace("say null -ping ", "");
    		for(NodePlayer pinged: this.phoneBook.values()){
    			if(pinged.name.equalsIgnoreCase(m)){
    				pinged.ping(np);
    				return;
    			}
    		}
    	}
    	if(message.startsWith("say null -repick") && !n.isStarted()){
    		repickRequest(np, message.replace("say null -repick", "").replaceAll(" ", ""));
    		return;
    	}
    	
    	
		try{
			ch.command(p, message, name);
		}catch (Throwable t){
			t.printStackTrace();
			new OGIMessage(p, "Server : " + t.getMessage());
    	}
    	
    	
		
	}
	public void onGameStart() {
		startTimer();
		sendGameState();
		resetChat();
		
		sendNotification("Game has started!");
	}

	protected void resetChat(Player p){
		StringBuilder sb = new StringBuilder();
		for(Message e: p.getEvents()){
			sb.append(e.access(p.getName(), true) + "\n");
		}
		try {
			JSONObject jo = WebCommunicator.getJObject(sb.toString());
			jo.put("chatReset", true);
			phoneBook.get(p).write(jo);
			
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
	}
	
	private void resetChat(){
		for(Player p: webUsers){
			resetChat(p);
		}
		for(WebPlayer observer: observers){
			StringBuilder sb = new StringBuilder();
			for(Message e: n.getEventManager().getEvents(Message.PUBLIC)){
				sb.append(e.access(Message.PUBLIC, true) + "\n");
			}
			try {
				JSONObject jo = WebCommunicator.getJObject(sb.toString());
				jo.put("chatReset", true);
				observer.write(jo);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}
	public void onNightStart(PlayerList lynched, PlayerList poisoned, EventList e) {
		startTimer();
		sendGameState();
		resetChat();
		
		sendNotification("The day has ended! Submit your night actions.");
	}

	public void onDayStart(PlayerList newDead) {
		startTimer();
		sendGameState();
		
		resetChat();
		sendNotification("A new day has started...");
	}
	
	private void saveCommands(){
    	DateFormat dateFormat = new SimpleDateFormat("MM-dd-HH-mm");
		File file = new File(dateFormat.format(new Date())+"[" + host.getName() + "].txt");
		try {
			file.createNewFile();
			FileWriter in = new FileWriter(file);
			JSONObject jo = new JSONObject();
			JSONArray playerInfoArray = new JSONArray();
			int day = 0;
			for(Player p: n.getAllPlayers()){
				JSONObject playerInfo = new JSONObject();
				playerInfo.put("name", p.getName());
				playerInfo.put("color", p.getTeam(day));
				playerInfo.put("role", p.getRole(day).getClass().getSimpleName());
				playerInfoArray.put(playerInfo);
			}
			jo.put("playerInfo", playerInfoArray);
			JSONArray commands = new JSONArray();
			for(String s: n.getCommands()){
				commands.put(s + "\n");
			}
			jo.put("commands", commands);
			in.write(jo.toString());
			in.close();
		} catch (IOException | JSONException | NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	public void onEndGame() {
		//game ended
		saveCommands();
		timer.interrupt();
		sendGameState();
		resetChat();
		broadcast(n.getWinMessage(), true);
		sendNotification(n.getWinMessage().access(Message.PUBLIC, false));
    	new OGIMessage(webUsers, "Server : " + "Press refresh to join another game!");
    	nc.removeInstance(gameID);
	}
	
	public void onMayorReveal(Player mayor, Message e) {
		sendVotes(null);
		broadcast(e, false);
		sendNotification(mayor.getDescription() + " has revealed as the mayor!");
	}

	public void onArsonDayBurn(Player arson, PlayerList burned, Message e) {
		resetChat();
		sendGameState();
		sendNotification("There was a fiery explosion!");
	}
	
	public void onAssassination(Player assassin, Player target, Message e){
		resetChat();
		sendGameState();
		sendNotification(target.getDescription() + " was assassinated!");
	}

	private void sendVotes(Message event){
		try{
			for(Player p: webUsers){
				StateObject io = getInstObject();
				io.addState(StateObject.PLAYERLISTS);
				io.addKey(StateObject.skipVote, n.Skipper.getVoters().size());
				io.addKey(StateObject.isSkipping, n.Skipper == p.getVoteTarget());
				io.send(Player.list(p));
			}
			StateObject io = getInstObject();
			io.addState(StateObject.PLAYERLISTS);
			io.addKey(StateObject.skipVote, n.Skipper.getVoters().size());
			io.addKey(StateObject.isSkipping, false);
			for(WebPlayer observer: observers){
				JSONObject jo = io.send((Player) null);
				observer.write(jo);
			}
		}catch(JSONException e){}

		if(event != null){
			broadcast(event, false);
			sendNotification(event);
		}
	}
	
	public void onVote(Player voter, Player target, int voteCount, Message e) {
		sendVotes(e);
		
		
	}

	public void onUnvote(Player voter, Player prev, int voteCountToLynch, Message e) {
		sendVotes(e);
	}

	public void onChangeVote(Player voter, Player target, Player prevTarget, int toLynch, Message e) {
		sendVotes(e);
	}

	public void onNightTarget(Player owner, Player target) {
		//feedback already being sent and stored
		getInstObject().addState(StateObject.PLAYERLISTS).send(owner);
	}

	public void onNightTargetRemove(Player owner, Player prev) {
		getInstObject().addState(StateObject.PLAYERLISTS).send(owner);
	}

	public void onEndNight(Player p) {
		getInstObject().addKey(StateObject.endedNight, n.isNight()).send(p);
	}

	
	public void onCancelEndNight(Player p) {
		getInstObject().addKey(StateObject.endedNight, false).send(p);
	}

	public void sendNotification(String s){
		nc.sendNotification(getWebPlayerList(n.getAllPlayers()), "Narrator", s);
	}
	
	private ArrayList<NodePlayer> getWebPlayerList(PlayerList pl){
		ArrayList<NodePlayer> nList = new ArrayList<>();
		
		for(Player p: pl){
			nList.add(phoneBook.get(p));
		}
		return nList;
	}
	
	public void sendNotification(Message e){
		String text;
		for(Player p: n.getAllPlayers()){
			text = e.access(p, false);
			if(text.length() != 0){
				sendNotification(p, text);
			}
		}
	}
	
	public void onMessageReceive(Player receiver, Message e) {
		if(e instanceof ChatMessage){
			ChatMessage cm = (ChatMessage) e;
			sendNotification(receiver, cm.sender.getName(), cm.message);
		}else
			sendNotification(receiver, e.access(receiver, false));
	}

	public void sendNotification(Player player, String subtitle){
		sendNotification(player, "Narrator", subtitle);
	}
	public void sendNotification(Player player, String title, String subtitle){
		NodePlayer np = phoneBook.get(player);
		if(!np.notificationCapable())
			return;
		ArrayList<NodePlayer> nList = new ArrayList<>();
		nList.add(np);
		nc.sendNotification(nList, title, subtitle);
	}
	
	public void onModKill(Player bad) {
		resetChat();
		
	}
	
	public void broadcast(Message m, boolean toSlack){
		for(Player p: webUsers){
			p.sendMessage(m);
		}
		
		if(toSlack)
			broadcastToSlackUsers(m.access(Message.PUBLIC, false));
	}
	
	public void broadcastToSlackUsers(String s){
		JSONObject jo = new JSONObject();
		try {
			jo.put("server", true);
			jo.put(StateObject.message, "slackMessage");
			jo.put(StateObject.gameID, gameID);
			jo.put("name", "narrator");
			jo.put("slackMessage", s);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		nc.nodePush(jo);
	}

	public void onPlayerListStatusChange() throws JSONException {
		StateObject so = getInstObject().addState(StateObject.PLAYERLISTS);
		so.send(webUsers);
		JSONObject jo = so.send((Player) null);
		for(WebPlayer np: observers){
			np.write(jo);
		}
		
	}

	public boolean isFull() {
		if(n.isStarted())
			return false;
		return n.getAllPlayers().size() < n.getAllRoles().size();
	}

	

	

	
}
