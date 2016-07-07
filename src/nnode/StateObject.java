package nnode;

import java.util.ArrayList;
import java.util.HashMap;

import json.JSONArray;
import json.JSONConstants;
import json.JSONException;
import json.JSONObject;
import shared.logic.Member;
import shared.logic.Narrator;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.Team;
import shared.logic.support.Constants;
import shared.logic.support.RoleTemplate;
import shared.logic.support.rules.Rule;
import shared.logic.support.rules.RuleBool;
import shared.logic.support.rules.RuleInt;
import shared.logic.support.rules.Rules;

public class StateObject {

	public static final String RULES = "Rules";
	public static final String ROLESLIST = "RolesList";
	public static final String PLAYERLISTS = "PlayerLists";
	public static final String DAYLABEL = "DayLabel";
	public static final String GRAVEYARD = "Graveyard";
	public static final String ROLEINFO = "RoleInfo";
	
	private Instance inst;
	private Narrator n;
	private FactionManager fManager; 
	private HashMap<Player, NodePlayer> phoneBook;
	private HashMap<String, Object> extraKeys;
	public StateObject(Instance i){
		states = new ArrayList<String>();
		this.inst = i;
		n = inst.n;
		fManager = inst.fManager;
		phoneBook = i.phoneBook;
		extraKeys = new HashMap<>();
	}
	
	ArrayList<String> states;
	public StateObject addState(String state){
		states.add(state);
		return this;
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
		addJFactions(state);
		JSONObject jRules = new JSONObject();
		Rules rules = n.getRules();
		Rule r;
		JSONObject ruleObject;
		for(String key: rules.rules.keySet()){
			ruleObject = new JSONObject();
			r = rules.getRule(key);
			ruleObject.put("id", r.id);
			ruleObject.put("name", r.name);
			if(r.getClass() == RuleInt.class){
				ruleObject.put("val", ((RuleInt) r).val);
				ruleObject.put("isNum", true);
			}else{
				ruleObject.put("val", ((RuleBool) r).val);
				ruleObject.put("isNum", false);
			}
			jRules.put(r.id, ruleObject);
		}
		String id;
		for(Team t: n.getAllTeams()){
			if(t.getColor().equals(Constants.A_SKIP))
				continue;
			
			id = t.getColor() + "kill";
			ruleObject = new JSONObject();
			ruleObject.put("name", "Has Faction kill");
			ruleObject.put("id", id);
			ruleObject.put("isNum", false);
			ruleObject.put("val", t.canKill());
			jRules.put(id, ruleObject);
			
			id = t.getColor() + "identity";
			ruleObject = new JSONObject();
			ruleObject.put("name", "Knows who allies are");
			ruleObject.put("id", id);
			ruleObject.put("isNum", false);
			ruleObject.put("val", t.knowsTeam());
			jRules.put(id, ruleObject);
			
			id = t.getColor() + "liveToWin";
			ruleObject = new JSONObject();
			ruleObject.put("name", "Must be alive to win");
			ruleObject.put("id", id);
			ruleObject.put("isNum", false);
			ruleObject.put("val", t.getAliveToWin());
			jRules.put(id, ruleObject);
			
			id = t.getColor() + "priority";
			ruleObject = new JSONObject();
			ruleObject.put("name", "Win priority");
			ruleObject.put("id", id);
			ruleObject.put("val", t.getPriority());
			ruleObject.put("isNum", true);
			jRules.put(id, ruleObject);
		}
		
		
		state.getJSONArray(JSONConstants.type).put(JSONConstants.rules);
		state.put(JSONConstants.rules, jRules);
	}
	
	private void addJFactions(JSONObject state) throws JSONException{
		JSONArray fMembers, blacklisted, allies, enemies, factionNames = new JSONArray();
		JSONObject jFaction, jRT, allyInfo, jFactions = new JSONObject();
		for(Faction f: fManager.factions){
			jFaction = new JSONObject();
			fMembers = new JSONArray();
			blacklisted = new JSONArray();
			
			jFaction.put("name", f.getName());
			factionNames.put(f.getName());
			jFaction.put("color", f.getColor());
			jFaction.put("description", f.getDescription());
			jFaction.put("isEditable", f.isEditable);
			
			for(RoleTemplate rt: f.members){
				jRT = new JSONObject();
				jRT.put("name", rt.getName());
				jRT.put("description", rt.getDescription());
				jRT.put("color", rt.getColor());
				jRT.put("rules", new JSONArray(rt.getRules()));
				jFactions.put(rt.getName() + rt.getColor(), jRT);
				fMembers.put(jRT);
			}
			jFaction.put("members", fMembers);
			for(Member rt: f.unavailableRoles){
				jRT = new JSONObject();
				jRT.put("name", rt.getName());
				jRT.put("simpleName", rt.getSimpleName());
				blacklisted.put(jRT);
			}
			jFaction.put("blacklisted", blacklisted);
			
			
			
			
			if(f.isEditable)
				jFaction.put("rules", f.getRules());
			jFactions.put(f.getName(), jFaction);
			jFactions.put(f.getColor(), jFaction);
			
			Team fTeam = f.getTeam();
			if(fTeam == null)
				continue;

			allies = new JSONArray();
			enemies = new JSONArray();
			for(Team t: n.getAllTeams()){
				if(t.getName().equals(Constants.A_SKIP))
					continue;
				if(t == fTeam)
					continue;
				allyInfo = new JSONObject();
				allyInfo.put("color", t.getColor());
				allyInfo.put("name", t.getName());
				if(t.isEnemy(fTeam))
					enemies.put(allyInfo);
				else
					allies.put(allyInfo);
			}
			jFaction.put("allies", allies);
			jFaction.put("enemies", enemies);
			
		}
		jFactions.put("factionNames", factionNames);
		
		state.getJSONArray(JSONConstants.type).put(JSONConstants.factions);
		state.put(JSONConstants.factions, jFactions);
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
	private JSONArray getJPlayerArray(PlayerList input) throws JSONException{
		return getJPlayerArray(input, new PlayerList());
	}
	private JSONArray getJPlayerArray(PlayerList input, Player p) throws JSONException{
		PlayerList list = new PlayerList();
		if(p != null)
			list.add(p);
		return getJPlayerArray(input, list);
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

	public void send(Player p) throws JSONException{
		JSONObject obj = Instance.GetGUIObject();
		for(String state: states){
			if(state.equals(RULES))
				addJRules(obj);
			else if(state.equals(ROLESLIST))
				addJRolesList(obj);
			else if(state.equals(PLAYERLISTS))
				addJPlayerLists(obj, p);
			else if(state.equals(DAYLABEL))
				addJDayLabel(obj);
			else if(state.equals(GRAVEYARD))
				addJGraveYard(obj);
			else if(state.equals(ROLEINFO))
				addJRoleInfo(p, obj);
			
		}
		for(String key: extraKeys.keySet()){
			obj.put(key, extraKeys.get(key));
		}
		inst.playerJWrite(p, obj);
	}
	
	public void send(PlayerList players) throws JSONException {
		for(Player p: players){
			send(p);
		}
	}

	public void addKey(String key, Object val) {
		extraKeys.put(key, val);
		
	}
}
