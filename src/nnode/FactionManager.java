package nnode;

import java.util.ArrayList;

import shared.logic.Member;
import shared.logic.support.Constants;
import shared.logic.support.RoleTemplate;
import shared.roles.RandomRole;

public class FactionManager {

	ArrayList<Faction> factions;
	public FactionManager(){
		factions = new ArrayList<>();
		
		Faction townFaction = new Faction("Town");
		townFaction.setColor(Constants.A_TOWN);
		ArrayList<RoleTemplate> list = new ArrayList<>();
		for(Member m: RandomRole.TownRandom().list.values())
			list.add(m);
		townFaction.add(list);
		factions.add(townFaction);
		
		
		Faction mafFaction = new Faction("Mafia");
		mafFaction.setColor(Constants.A_MAFIA);
		list.clear();
		for(Member m: RandomRole.MafiaRandom().list.values())
			list.add(m);
		mafFaction.add(list);
		factions.add(mafFaction);
		
		
		Faction yakFaction = new Faction("Yakuza");
		yakFaction.setColor(Constants.A_YAKUZA);
		list.clear();
		for(Member m: RandomRole.MafiaRandom().list.values())
			list.add(m.setColor(Constants.A_YAKUZA));
		yakFaction.add(list);
		factions.add(yakFaction);
		
		Faction neutFaction = new Faction("Neutrals");
		neutFaction.setColor(Constants.A_NEUTRAL);
		list.clear();
		for(Member m: RandomRole.NeutralRandom().list.values())
			list.add(m);
		neutFaction.add(list);
		factions.add(neutFaction);
		
		Faction randFaction = new Faction("Randoms");
		randFaction.setColor(Constants.A_RANDOM);
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
		factions.add(randFaction);
		
	}
}
