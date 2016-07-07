package nnode;

import java.util.ArrayList;
import java.util.HashMap;

import shared.logic.Member;
import shared.logic.Narrator;
import shared.logic.Team;
import shared.logic.support.Constants;
import shared.logic.support.RoleTemplate;
import shared.roles.RandomRole;

public class FactionManager {

	
	public FactionManager(Narrator n){
		factions = new ArrayList<>();
		factionMap = new HashMap<>();
		
		
		addFaction(new Faction(n.getTeam(Constants.A_TOWN), RandomRole.TownRandom())).isEditable = false;
		addFaction(new Faction(n.getTeam(Constants.A_MAFIA), RandomRole.MafiaRandom())).isEditable = false;
		addFaction(new Faction(n.getTeam(Constants.A_YAKUZA), RandomRole.MafiaRandom())).isEditable = false;
		
		ArrayList<RoleTemplate> list = new ArrayList<RoleTemplate>();
		Faction neutFaction = new Faction("Neutrals", Constants.A_NEUTRAL);
		list.clear();
		for(Member m: RandomRole.NeutralRandom().list.values())
			list.add(m);
		neutFaction.add(list);
		neutFaction.setDescription("The neutrals of the town.  Some want to see everyone die, some don't care who win, and some just want to see the town lose.");
		addFaction(neutFaction);
		
		Faction randFaction = new Faction("Randoms", Constants.A_RANDOM);
		list.clear();
		list.add(RandomRole.TownRandom());
		list.add(RandomRole.TownInvestigative());
		list.add(RandomRole.TownProtective());
		list.add(RandomRole.TownKilling());
		list.add(RandomRole.MafiaRandom());
		list.add(RandomRole.YakuzaRandom());
		list.add(RandomRole.NeutralRandom());
		list.add(RandomRole.NeutralBenignRandom());
		list.add(RandomRole.NeutralEvilRandom());
		randFaction.add(list);
		randFaction.setDescription("These roles are randomly generated from the given teams");
		addFaction(randFaction);
		
		for(Faction f: factions)
			f.isEditable = false;
	}
	
	public void removeTeam(String color){
		Faction toRemove = null;
		for(Faction f: factions){
			if(f.getColor().equals(color)){
				toRemove = f;
				break;
			}
		}
		factions.remove(toRemove);
	}
	
	ArrayList<Faction> factions;
	HashMap<String, Faction> factionMap;
	private Faction addFaction(Faction f){
		factions.add(f);
		factionMap.put(f.getColor(), f);
		return f;
	}
	
	public void addFaction(Team t){
		addFaction(new Faction(t));
	}

	public Faction getFaction(String color) {
		return factionMap.get(color);
	}
}
