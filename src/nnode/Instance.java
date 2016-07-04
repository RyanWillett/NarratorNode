package nnode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.texting.TextHandler;
import json.JSONArray;
import json.JSONConstants;
import json.JSONException;
import json.JSONObject;
import shared.event.Message;
import shared.event.OGIMessage;
import shared.logic.Narrator;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.Team;
import shared.logic.exceptions.NarratorException;
import shared.logic.exceptions.PlayerTargetingException;
import shared.logic.listeners.NarratorListener;
import shared.logic.support.Constants;
import shared.logic.support.Random;
import shared.logic.support.RoleTemplate;
import shared.logic.support.StringChoice;
import shared.logic.support.rules.Rule;
import shared.logic.support.rules.RuleBool;
import shared.logic.support.rules.RuleInt;
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

	protected Narrator n;
	protected TextHandler th;
	private HashMap<Player, NodePlayer> phoneBook;
	private NodeSwitch nc;
	
	public Instance(NodeSwitch nc){
		this.nc = nc;
		phoneBook = new HashMap<>();
		n = Narrator.Default();
		
		Rules r = n.getRules();
		r.setBool(Rules.DAY_START, Narrator.DAY_START);
		r.setInt(Rules.DAY_LENGTH, 5);
		r.setInt(Rules.NIGHT_LENGTH, 2);
        
        th = new TextHandler(n, nc, new PlayerList());
	}
	
	public void removePlayer(NodePlayer leaver) throws JSONException {
		n.removePlayer(leaver.player);
		
		if(leaver.player == host){
			if(n.getAllPlayers().isEmpty()){
				nc.instances.remove(this);
			}else{
				repickers = new PlayerList();
				host = n.getAllPlayers().get(0);
				sendGameState(host);
			}
		}
		
		phoneBook.remove(leaver.player);
		leaver.player = null;
		leaver.inst = null;
		
		
		JSONObject j1 = new JSONObject();
		j1.put("message", leaver.name + " has left the lobby.");
		j1.put("server", false);
		j1.put("from", "Server");
		announce(j1, null);
		onPlayerListStatusChange();
	}
	
	public Player addPlayer(NodePlayer np) throws JSONException{
    	Player p = n.addPlayer(np.name, new NodeCommunicator(nc, np));
		np.player = p;
		np.inst = this;
		phoneBook.put(p, np);
		
		if(n.getAllPlayers().size() == 1)
			host = n.getAllPlayers().get(0);
		
		sendGameState();
		
		JSONObject j1 = new JSONObject();
		j1.put("message", np.name + " has joined the lobby.");
		j1.put("server", false);
		j1.put("from", "Server");
		announce(j1, p);
		
		return p;
	}
	private void playerJWrite(Player p, JSONObject j) throws JSONException{
		NodePlayer np = phoneBook.get(p);
		nc.write(np,  j);
	}
	static JSONObject GetGUIObject() throws JSONException{
		JSONObject jo = new JSONObject();
		jo.put(JSONConstants.guiUpdate, true);
		jo.put("server", false);
		jo.put(JSONConstants.type, new JSONArray());
		return jo;
	}
	
	private void startGame(){
		th.setTexters(n.getAllPlayers());
		if(nc != null)
			nc.instances.remove(this);
    	n.addListener(this);
    	n._players.sortByName();
		n.startGame();
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
					Thread.sleep(60000 * length);
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
	
	private void addJRolesList(JSONObject state) throws JSONException{
		JSONArray roles = new JSONArray();
		JSONObject role;
		for(RoleTemplate r: n.getAllRoles()){
			role = new JSONObject();
			role.put(JSONConstants.roleType, r.getName());
			
			role.put(JSONConstants.color, r.getColor());
			roles.put(role);
		}
		state.getJSONArray(JSONConstants.type).put(JSONConstants.roles);
		state.put(JSONConstants.roles, roles);
	}
	private JSONArray getJPlayerArray(PlayerList input) throws JSONException{
		return getJPlayerArray(input, new PlayerList());
	}
	private JSONArray getJPlayerArray(PlayerList input, Player p) throws JSONException{
		PlayerList list = new PlayerList();
		if(p != null)
			list.add(p);
		return getJPlayerArray(input, list);
	}
	private JSONArray getJPlayerArray(PlayerList input, PlayerList selected) throws JSONException{
		JSONArray arr = new JSONArray();
		if(input.isEmpty())
			return arr;
		PlayerList allPlayers = n.getAllPlayers();
		
		JSONObject jo;
		for(Player pi: input){
			jo = new JSONObject();
			jo.put(JSONConstants.playerName, pi.getName());
			jo.put(JSONConstants.playerIndex, allPlayers.indexOf(pi) + 1);
			jo.put(JSONConstants.playerSelected, selected.contains(pi));
			jo.put(JSONConstants.playerActive, phoneBook.get(pi).isActive());
			if(n.isStarted() && pi.getVoters() != null){
				jo.put(JSONConstants.playerVote, pi.getVoters().size());
			}
			arr.put(jo);
		}
			
		
		
		return arr;
	}
	private void addJPlayerLists(JSONObject state, Player p) throws JSONException{
		JSONObject playerLists = new JSONObject();
		playerLists.put(JSONConstants.type, new JSONArray());
		
		if(n.isStarted()){
			if(n.isDay){
				PlayerList votes;
				if(n.isInProgress())
					votes = n.getLivePlayers().remove(p);
				else
					votes = n.getAllPlayers().remove(p);
				if(p.isDead())
					votes.clear();
				JSONArray names = getJPlayerArray(votes, p.getVoteTarget());
				playerLists.put("Vote", names);
				playerLists.getJSONArray(JSONConstants.type).put("Vote");
			}else{
				String[] abilities = p.getAbilities();
				for(String s_ability: abilities){
					int ability = p.parseAbility(s_ability);
					PlayerList acceptableTargets = new PlayerList();
					for(Player potentialTarget: n.getAllPlayers()){
						if(p.isAcceptableTarget(potentialTarget, ability)){
							acceptableTargets.add(potentialTarget);
						}
					}
					if(acceptableTargets.isEmpty())
						continue;

					JSONArray names = getJPlayerArray(acceptableTargets, p.getTargets(ability));
					playerLists.put(s_ability, names);
					playerLists.getJSONArray(JSONConstants.type).put(s_ability);
				}
				if(playerLists.getJSONArray(JSONConstants.type).length() == 0){
					JSONArray names = getJPlayerArray(new PlayerList());
					playerLists.put("You have no acceptable night actions tonight!", names);
					playerLists.getJSONArray(JSONConstants.type).put("You have no acceptable night actions tonight!");
				}
			}
		}else{
			JSONArray names = getJPlayerArray(n.getAllPlayers());
			playerLists.put("Lobby", names);
			playerLists.getJSONArray(JSONConstants.type).put("Lobby");
		}
		
		
		state.getJSONArray(JSONConstants.type).put(JSONConstants.playerLists);
		state.put(JSONConstants.playerLists, playerLists);
	}
	private void addJDayLabel(JSONObject state) throws JSONException{
		String dayLabel;
		if (!n.isStarted()){
			dayLabel = "Night 0";
		}else if(n.isDay()){
			dayLabel = "Day " + n.getDayNumber();
		}else{
			dayLabel = "Night " + n.getDayNumber();
		}
		state.getJSONArray(JSONConstants.type).put(JSONConstants.dayLabel);
		state.put(JSONConstants.dayLabel, dayLabel);
	}
	private ArrayList<Team> shouldShowTeam(Player p){
		ArrayList<Team> teams = new ArrayList<>();
		for(Team t: p.getTeams()){
			if(!t.knowsTeam())
				continue;
			if(t.getMembers().remove(p).getLivePlayers().isEmpty())
				continue;
			teams.add(t);
		}
		return teams;			
	}
	
	private void addJRoleInfo(Player p, JSONObject state) throws JSONException{
		JSONObject roleInfo = new JSONObject();
		roleInfo.put(JSONConstants.roleColor, p.getTeam().getColor());
		roleInfo.put(JSONConstants.roleName, p.getRoleName());
		roleInfo.put(JSONConstants.roleDescription, p.getRoleInfo());
		
		ArrayList<Team> knownTeams = shouldShowTeam(p);
		boolean displayTeam = !knownTeams.isEmpty();
		roleInfo.put(JSONConstants.roleKnowsTeam, displayTeam);
		if(displayTeam){
			JSONArray allyList = new JSONArray();
			JSONObject allyObject;
			for(Team group: knownTeams){
				for(Player ally: group.getMembers().remove(p).getLivePlayers()){
					allyObject = new JSONObject();
					allyObject.put(JSONConstants.teamAllyName, ally.getName());
					allyObject.put(JSONConstants.teamAllyRole, ally.getRoleName());
					allyObject.put(JSONConstants.teamAllyColor, group.getColor());
					allyList.put(allyObject);
				}
				
			}
			roleInfo.put(JSONConstants.roleTeam, allyList);
		}

		state.getJSONArray(JSONConstants.type).put(JSONConstants.roleInfo);
		state.put(JSONConstants.roleInfo, roleInfo);
	}
	
	private void addJGraveYard(JSONObject state) throws JSONException{
		JSONArray graveYard = new JSONArray();
		
		JSONObject graveMarker;
		String color;
		for(Player p: n.getDeadPlayers().sortByDeath()){
			graveMarker = new JSONObject();
			if(p.isCleaned())
				color = "#FFFFFF";
			else
				color = p.getTeam().getColor();
			graveMarker.put(JSONConstants.color, color);
			graveMarker.put(JSONConstants.roleName, p.getDescription());
			graveMarker.put("name", p.getName());
			graveYard.put(graveMarker);
		}
		
		state.getJSONArray(JSONConstants.type).put(JSONConstants.graveYard);
		state.put(JSONConstants.graveYard, graveYard);
	}
	
	private void addJRules(JSONObject state) throws JSONException{
		JSONObject jRules = new JSONObject();
		Rules rules = n.getRules();
		Rule r;
		JSONObject ruleObject;
		for(String key: rules.rules.keySet()){
			ruleObject = new JSONObject();
			r = rules.getRule(key);
			ruleObject.put("id", r.id);
			ruleObject.put("name", r.name);
			if(r.getClass() == RuleInt.class)
				ruleObject.put("val", ((RuleInt) r).val);
			else
				ruleObject.put("val", ((RuleBool) r).val);
			jRules.put(r.id, ruleObject);
		}
		
		
		state.getJSONArray(JSONConstants.type).put(JSONConstants.rules);
		state.put(JSONConstants.rules, jRules);
	}
	
	void sendGameState(Player p) throws JSONException{
		JSONObject state = GetGUIObject();
		addJRolesList(state);
		addJPlayerLists(state, p);
		addJDayLabel(state);
		addJGraveYard(state);
		state.put(JSONConstants.gameStart, n.isStarted());
		state.put(JSONConstants.showButton, !n.isInProgress() || p.isAlive() && (n.isNight() || p.hasDayAction()));
		state.put(JSONConstants.endedNight, n.isNight() && p.endedNight());
		
		
		if(n.isStarted()){
			addJRoleInfo(p, state);
			state.put(JSONConstants.isDay, n.isDay());
			if(n.isInProgress()){
				state.put(JSONConstants.timer, getEndTime());
			}else{
				state.put(JSONConstants.isFinished, true);
			}
			if(n.isDay()){
				state.put(JSONConstants.skipVote, p.getSkipper().getVoters().size());
				state.put(JSONConstants.isSkipping, p.getSkipper() == p.getVoteTarget());
			}
		}else{
			state.put(JSONConstants.isHost, p == host);
			state.put(JSONConstants.host, host.getName());
			addJRules(state);
		}
			
		playerJWrite(p, state);
	}
	
	void sendGameState(){
		try{
			for(Player p: n.getAllPlayers())
				sendGameState(p);
		}catch(JSONException e){
			e.printStackTrace();
		}
	}
	
	private PlayerList repickers = new PlayerList();
	private void repickHost(String message){
		Player potential = n.getAllPlayers().getPlayerByName(message);
		if(potential != null && phoneBook.get(potential).isActive() && message.length() != 0){
			host = potential;
		}else{
			PlayerList options = n.getAllPlayers().remove(host).shuffle(new Random());
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
	
	private void repickRequest(NodePlayer repicker, String message){
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
			
			new OGIMessage(n.getAllPlayers(), sc, " ", have, " voted to repick ", sc2, ".");
			if(repickers.size() >= n.getAllPlayers().size()/2 + 1){
				repickHost("");
			}
		}
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
	
	public synchronized void handlePlayerMessage(NodePlayer np, JSONObject jo) throws JSONException {
		String name = jo.getString("name");
    	String message = jo.getString("message");
    	if(message.length() == 0)
    		return;
    	Player p = np.player;
    	if (p == null)
    		throw new PlayerTargetingException(name + " wasn't found.");
    	if(message.equals(JSONConstants.requestGameState)){
    		sendGameState(p);
    		return;
    	}
    	
    	if(message.equals(JSONConstants.requestChat)){
    		p.sendMessage(p.getEvents());
    		return;
		}
    	
    	if(host == p){
    		if(message.equals(JSONConstants.addRole)){
    			String color = jo.getString(JSONConstants.roleColor);
    			String displayed_name = jo.getString(JSONConstants.roleName);
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
    		if(message.equals(JSONConstants.removeRole)){
    			String color = jo.getString(JSONConstants.roleColor);
    			String displayed_name = jo.getString(JSONConstants.roleName);
    			//String role_name = this.translateName(displayed_name);
    			RoleTemplate r = n.getAllRoles().get(displayed_name, color);
    			if(r != null){
    				n.removeRole(r);
    				sendGameState();
    			}
    			return;
    		}
    		if(message.equals(JSONConstants.ruleChange)){
    			Rules r = n.getRules();
    			jo = jo.getJSONObject(JSONConstants.ruleChange);
    			JSONObject inputJRule;
    			for(String id: r.rules.keySet()){
    				inputJRule = jo.getJSONObject(id);
    				try{
    					int val = inputJRule.getInt("val");
    					r.setInt(id, val);
    				}catch(JSONException e){
    					System.out.println(id);
    					boolean val = inputJRule.getBoolean("val");
    					r.setBool(id, val);
    				}
    			}
    			
    			JSONObject state = GetGUIObject();
    			addJRules(state);
    			for(Player pi: n.getAllPlayers().remove(host)){
    				playerJWrite(pi, state);
    			}
    			return;
    		}
    		if(message.equals(JSONConstants.startGame)){
    			try{
    				startGame();
    			}catch(NarratorException e){
    				new OGIMessage(p, e.getMessage());
    				
    			}catch(Throwable t){
    				t.printStackTrace();
    			}
    			return;
    		}
		}

    	if(message.equals("leaveGame")){
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
			th.text(p, message, false);
		}catch (Throwable t){
			t.printStackTrace();
			new OGIMessage(p, "Server : " + t.getMessage());
    	}
    	
    	
		
	}
	public void onGameStart() {
		startTimer();
		sendGameState();
		resetChat();
	}

	protected void resetChat(Player p){
		System.out.println("resetting chat");
		StringBuilder sb = new StringBuilder();
		for(Message e: p.getEvents()){
			sb.append(e.access(p.getName(), true) + "\n");
		}
		try {
			JSONObject jo = NodeCommunicator.getJObject(sb.toString());
			jo.put("chatReset", true);
			nc.write(phoneBook.get(p), jo);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
	}
	
	private void resetChat(){
		for(Player p: n.getAllPlayers()){
			resetChat(p);
		}
			
	}
	public void onNightStart(PlayerList lynched) {
		startTimer();
		sendGameState();
		resetChat();
		
	}

	public void onDayStart(PlayerList newDead) {
		startTimer();
		sendGameState();
		
		resetChat();
		
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
			}
			jo.put("playerInfo", playerInfoArray);
			jo.put("commands", new JSONArray(n.getCommands()));
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
    	new OGIMessage(n.getAllPlayers(), "Server : " + "Press refresh to join another game!");
	}
	
	public void onMayorReveal(Player mayor) {
		
	}

	public void onArsonDayBurn(Player arson, PlayerList burned) {
		resetChat();
	}

	private void sendVotes(){
		try{
			for(Player p: n.getAllPlayers()){
				JSONObject state = GetGUIObject();
				addJPlayerLists(state, p);
				state.put(JSONConstants.skipVote, p.getSkipper().getVoters().size());
				state.put(JSONConstants.isSkipping, p.getSkipper() == p.getVoteTarget());
				playerJWrite(p, state);
			}
		}catch(JSONException e){}
	}
	
	public void onVote(Player voter, Player target, int voteCount, Message e) {
		sendVotes();
		
	}

	public void onUnvote(Player voter, Player prev, int voteCountToLynch, Message e) {
		sendVotes();
	}

	public void onChangeVote(Player voter, Player target, Player prevTarget, int toLynch, Message e) {
		sendVotes();
		
	}

	public void onNightTarget(Player owner, Player target) {
		try {
			JSONObject state = GetGUIObject();
			addJPlayerLists(state, owner);
			playerJWrite(owner, state);
		} catch (JSONException e) {}
		
	}

	public void onNightTargetRemove(Player owner, Player prev) {
		try {
			JSONObject state = GetGUIObject();
			addJPlayerLists(state, owner);
			playerJWrite(owner, state);
		} catch (JSONException e) {}
	}

	@Override
	public void onEndNight(Player p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCancelEndNight(Player p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageReceive(Player receiver, Message e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onModKill(Player bad) {
		resetChat();
		
	}

	
	public void announce(JSONObject j1, Player excluded) throws JSONException {
		for (Player p: n.getAllPlayers()){
			if(p != excluded)
				playerJWrite(p, j1);
		}
		
	}

	public void onPlayerListStatusChange() throws JSONException {
		JSONObject state;
		for(Player p: n.getAllPlayers()){
			state = GetGUIObject();
			addJPlayerLists(state, p);
			this.playerJWrite(p, state);
		}
		
	}

	public boolean isFull() {
		if(n.isStarted())
			return false;
		return n.getAllPlayers().size() < n.getAllRoles().size();
	}

	

	
}
