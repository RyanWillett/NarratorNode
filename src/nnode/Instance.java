package nnode;

import java.util.HashMap;

import android.texting.TextHandler;
import json.JSONArray;
import json.JSONConstants;
import json.JSONException;
import json.JSONObject;
import shared.event.Event;
import shared.logic.Narrator;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.Rules;
import shared.logic.exceptions.PlayerTargetingException;
import shared.logic.listeners.NarratorListener;
import shared.logic.support.Constants;
import shared.logic.support.RoleTemplate;
import shared.roles.RandomRole;
import shared.roles.Role;

public class Instance implements NarratorListener{

	protected Narrator n;
	protected TextHandler th;
	private HashMap<String, Player> phoneBook;
	private NodeController nc;
	
	public Instance(NodeController nc){
		this.nc = nc;
		phoneBook = new HashMap<String, Player>();
		n = Narrator.Default();
		
		Rules r = n.getRules();
        r.DAY_START = Narrator.DAY_START;
        r.DAY_LENGTH = 20;
        r.NIGHT_LENGTH = 20;
        
        th = new TextHandler(n, nc, new PlayerList());
	}
	
	public Player addPlayer(String name) throws JSONException{
    	Player p = n.addPlayer(name, new NodeCommunicator(nc));
		
		phoneBook.put(name.toLowerCase(), p);
		
		
		
		
		return p;
	}
	private void playerJWrite(Player p, JSONObject j) throws JSONException{
		String email = nc.rPhoneBook.get(p.getName());
		nc.write(email,  j);
	}
	private JSONObject getGUIObject() throws JSONException{
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
		n.startGame();
	}
	
	private long timerStart;
	private long getEndTime(){
		int length = n.isDay() ? n.getRules().DAY_LENGTH : n.getRules().NIGHT_LENGTH; 
		return (length * 1000) - (System.currentTimeMillis() - timerStart);
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
					int length = n.isDay() ? n.getRules().DAY_LENGTH : n.getRules().NIGHT_LENGTH;
					for(int i = 0 ; i < length; i++){
						Thread.sleep(1000);
					}
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
	private void addJPlayerLists(JSONObject state, Player p) throws JSONException{
		JSONObject playerLists = new JSONObject();
		playerLists.put(JSONConstants.type, new JSONArray());
		
		if(n.isStarted()){
			if(n.isDay()){
				PlayerList votes = n.getLivePlayers().remove(p);
				if(p.isDead())
					votes.clear();
				JSONArray names = new JSONArray(votes.getNamesToStringArray());
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

					JSONArray names = new JSONArray(acceptableTargets.getNamesToStringArray());
					playerLists.put(s_ability, names);
					playerLists.getJSONArray(JSONConstants.type).put(s_ability);
				}
				if(playerLists.getJSONArray(JSONConstants.type).length() == 0){
					JSONArray names = new JSONArray();
					playerLists.put("You have no acceptable night actions tonight!", names);
					playerLists.getJSONArray(JSONConstants.type).put("You have no acceptable night actions tonight!");
				}
			}
		}else{
			JSONArray names = new JSONArray(n.getAllPlayers().getNamesToStringArray());
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
	
	private void addJRoleInfo(Player p, JSONObject state) throws JSONException{
		JSONObject roleInfo = new JSONObject();
		roleInfo.put(JSONConstants.roleColor, p.getTeam().getColor());
		roleInfo.put(JSONConstants.roleName, p.getRoleName());
		roleInfo.put(JSONConstants.roleDescription, p.getRoleInfo());
		roleInfo.put(JSONConstants.roleTeam, p.getTeam().getName());

		state.getJSONArray(JSONConstants.type).put(JSONConstants.roleInfo);
		state.put(JSONConstants.roleInfo, roleInfo);
	}
	
	private void addJGraveYard(JSONObject state) throws JSONException{
		JSONArray graveYard = new JSONArray();
		
		JSONObject graveMarker;
		String color;
		for(Player p: n.getDeadPlayers()){
			graveMarker = new JSONObject();
			if(p.isCleaned())
				color = "#FFFFFF";
			else
				color = p.getTeam().getColor();
			graveMarker.put(JSONConstants.color, color);
			graveMarker.put(JSONConstants.roleName, p.getDescription());
			graveYard.put(graveMarker);
		}
		
		state.getJSONArray(JSONConstants.type).put(JSONConstants.graveYard);
		state.put(JSONConstants.graveYard, graveYard);
	}
	
	private void addJRules(JSONObject state) throws JSONException{
		JSONObject rules = new JSONObject();
		Rules r = n.getRules();
		rules.put("dayLength", r.DAY_LENGTH);
		rules.put("dayStart", r.DAY_START);
		rules.put("nightLength", r.NIGHT_LENGTH);
		rules.put("doctorNotification", r.doctorKnowsIfTargetIsAttacked);
		rules.put("vigShots", r.vigilanteShots);
		rules.put("vetShots", r.vetAlerts);
		rules.put("mayorVote", r.mayorVoteCount);
		rules.put("blockImmune", r.blockersRoleBlockImmune);
		rules.put("execImmune", r.exeuctionerImmune);
		rules.put("execWinImmune", r.exeuctionerWinImmune);
		rules.put("witchFeedback", r.witchLeavesFeedback);
		rules.put("skInvulnerability", r.serialKillerIsInvulnerable);
		rules.put("arsonInvulnerability", r.arsonInvlunerable);
		rules.put("arsonDayIgnite", r.arsonDayIgnite);
		rules.put("mmInvulnerability", r.mmInvulnerable);
		rules.put("mmDelay", r.mmSpreeDelay);
		rules.put("cultKeepRole", r.cultKeepsRoles);
		rules.put("cultPRCooldown", r.cultPowerRoleCooldown);
		rules.put("cultConversionCD", r.cultConversionCooldown);
		rules.put("cultImplodes", r.cultImplodesOnLeaderDeath);
		rules.put("gfInvulnerability", r.gfInvulnerable);
		rules.put("gfUndetectable", r.gfUndetectable);
		
		state.getJSONArray(JSONConstants.type).put(JSONConstants.rules);
		state.put(JSONConstants.rules, rules);
	}
	
	private void sendGameState(Player p) throws JSONException{
		JSONObject state = getGUIObject();
		addJRolesList(state);
		addJPlayerLists(state, p);
		addJDayLabel(state);
		addJGraveYard(state);
		state.put(JSONConstants.gameStart, n.isStarted());
		state.put(JSONConstants.showButton, n.isNight() || p.hasDayAction());
		state.put(JSONConstants.endedNight, n.isNight() && p.endedNight());
		
		
		if(n.isStarted()){
			addJRoleInfo(p, state);
			state.put(JSONConstants.isDay, n.isDay());
			if(n.isInProgress())
				state.put(JSONConstants.timer, getEndTime());
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
	
	public synchronized void handlePlayerMessage(JSONObject jo) throws JSONException {
		String name = jo.getString("name");
    	String message = jo.getString("message");
    	if(message.length() == 0)
    		return;
    	Player p = phoneBook.get(name.toLowerCase());
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
    			String role_name = jo.getString(JSONConstants.roleName);
    			
    			if(Role.isRole(role_name)){
    				n.addRole(role_name, color);
    			}else{
    				RandomRole rr = null;
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
    			String role_name = jo.getString(JSONConstants.roleName);
    			RoleTemplate r = n.getAllRoles().get(role_name, color);
    			if(r != null){
    				n.removeRole(r);
    				sendGameState();
    			}
    			return;
    		}
    		if(message.equals(JSONConstants.ruleChange)){
    			Rules r = n.getRules();
    			jo = jo.getJSONObject(JSONConstants.ruleChange);
    			r.DAY_LENGTH = jo.getInt("dayLength");
    			r.NIGHT_LENGTH = jo.getInt("nightLength");
    			r.DAY_START = jo.getBoolean("dayStart");
    			r.doctorKnowsIfTargetIsAttacked = jo.getBoolean("doctorNotification");
    			r.vigilanteShots = jo.getInt("vigShots");
    			r.vetAlerts = jo.getInt("vetShots");
    			r.mayorVoteCount = jo.getInt("mayorVote");
    			r.blockersRoleBlockImmune = jo.getBoolean("blockImmune");
    			r.exeuctionerImmune = jo.getBoolean("execImmune");
    			r.exeuctionerWinImmune = jo.getBoolean("execWinImmune");
    			r.witchLeavesFeedback = jo.getBoolean("witchFeedback");
    			r.serialKillerIsInvulnerable = jo.getBoolean("skInvulnerability");
    			r.arsonInvlunerable = jo.getBoolean("arsonInvulnerability");
    			r.arsonDayIgnite = jo.getBoolean("arsonDayIgnite");
    			r.mmInvulnerable = jo.getBoolean("mmInvulnerability");
    			r.mmSpreeDelay = jo.getInt("mmDelay");
    			r.cultKeepsRoles = jo.getBoolean("cultKeepRole");
    			r.cultPowerRoleCooldown = jo.getInt("cultPRCooldown");
    			r.cultConversionCooldown = jo.getInt("cultConversionCD");
    			r.cultImplodesOnLeaderDeath = jo.getBoolean("cultImplodes");
    			r.gfInvulnerable = jo.getBoolean("gfInvulnerability");
    			r.gfUndetectable = jo.getBoolean("gfUndetectable");
    			
    			JSONObject state = getGUIObject();
    			addJRules(state);
    			for(Player pi: n.getAllPlayers().remove(host)){
    				playerJWrite(pi, state);
    			}
    			return;
    		}
    		if(message.equals(JSONConstants.startGame)){
    			try{
    				startGame();
    			}catch(Throwable t){
    				p.sendMessage(t.getMessage());
    			}
    			return;
    		}
		}

    	
    	
		try{
			th.text(p, message, false);
		}catch (Throwable t){
			t.printStackTrace();
			p.sendMessage(new Event(-1).add("Server : " + t.getMessage()));
    	}
    	
    	
		
	}

	public void onGameStart() {
		startTimer();
		sendGameState();
	}

	public void onNightStart(PlayerList lynched) {
		startTimer();
		sendGameState();
	}

	public void onDayStart(PlayerList newDead) {
		startTimer();
		sendGameState();
	}
	
	public void onEndGame() {
		//game ended
		timer.interrupt();
    	if(n.isStarted() && !n.isInProgress()){
        	Event e = new Event(-1).add("Server : " + "Press refresh to join another game!");
        	n.getAllPlayers().sendMessage(e);
        	
        	/*ArrayList<String> toDelete = new ArrayList<>();
        	if(nc != null)
	        	for (String email: nc.phoneBook.keySet()){
	        		if(nc.phoneBook.get(email).n == n){
	        			toDelete.add(email);
	        		}
	        	}
        	
        	
        	String[] toDeleteArray = new String[toDelete.size()];
        	toDeleteArray = toDelete.toArray(toDeleteArray);
        	try{
        		JSONObject jo = new JSONObject();
        		jo.put("server", true);
        		jo.put("message", "closeConnection");
        		jo.put("players", toDeleteArray);
        		nc.write(null, jo);
        	}catch(JSONException f){}
        	
        	for (String email: toDelete){
        		nc.phoneBook.remove(email);
        	}
        	*/
        }
		
	}

	@Override
	public void onMayorReveal(Player mayor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onArsonDayBurn(Player arson, PlayerList burned) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onVote(Player voter, Player target, int voteCount, Event e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnvote(Player voter, Player prev, int voteCountToLynch, Event e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onChangeVote(Player voter, Player target, Player prevTarget, int toLynch, Event e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNightTarget(Player owner, Player target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNightTargetRemove(Player owner, Player prev) {
		// TODO Auto-generated method stub
		
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
	public void onMessageReceive(Player receiver, Event e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onModKill(Player bad) {
		// TODO Auto-generated method stub
		
	}

	
}