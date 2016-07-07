package nnode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import shared.logic.Member;
import shared.logic.Team;
import shared.logic.support.RoleTemplate;
import shared.logic.templates.BasicRoles;
import shared.roles.RandomRole;

public class Faction {

	public ArrayList<Member> unavailableRoles;
	public ArrayList<RoleTemplate> members;
	private Faction(String color){
		this.color = color;
		members = new ArrayList<>();
		unavailableRoles = new ArrayList<>();
		setUnavailableRoles();
	}
	
	private void setUnavailableRoles(){
		unavailableRoles.add(BasicRoles.Agent());
		unavailableRoles.add(BasicRoles.Amnesiac());
		unavailableRoles.add(BasicRoles.Armorsmith());
		unavailableRoles.add(BasicRoles.Arsonist());
		unavailableRoles.add(BasicRoles.Baker());
		unavailableRoles.add(BasicRoles.Blackmailer());
		unavailableRoles.add(BasicRoles.Bodyguard());
		unavailableRoles.add(BasicRoles.BusDriver());
		unavailableRoles.add(BasicRoles.Citizen());
		unavailableRoles.add(BasicRoles.Cultist());
		unavailableRoles.add(BasicRoles.CultLeader());
		unavailableRoles.add(BasicRoles.Detective());
		unavailableRoles.add(BasicRoles.Doctor());
		unavailableRoles.add(BasicRoles.Escort());
		unavailableRoles.add(BasicRoles.Executioner());
		unavailableRoles.add(BasicRoles.Framer());
		unavailableRoles.add(BasicRoles.Godfather());
		unavailableRoles.add(BasicRoles.Gunsmith());
		unavailableRoles.add(BasicRoles.Janitor());
		unavailableRoles.add(BasicRoles.Jester());
		unavailableRoles.add(BasicRoles.Lookout());
		unavailableRoles.add(BasicRoles.Mafioso());
		unavailableRoles.add(BasicRoles.MassMurderer());
		unavailableRoles.add(BasicRoles.Mayor());
		unavailableRoles.add(BasicRoles.SerialKiller());
		unavailableRoles.add(BasicRoles.Sheriff());
		unavailableRoles.add(BasicRoles.Survivor());
		unavailableRoles.add(BasicRoles.Veteran());
		unavailableRoles.add(BasicRoles.Vigilante());
		unavailableRoles.add(BasicRoles.Witch());
	}
	
	public Faction(String name, String color){
		this(color);
		this.name = name;
	}
	
	public Faction(Team t){
		this(t.getColor());
		setTeam(t);
	}
	
	private Member removeFromAvailable(String simpleName){
		Member toMakeAvailable = null;
		for(Member m: unavailableRoles){
			if(m.getSimpleName().equals(simpleName))
				toMakeAvailable = m;
		}
		if(toMakeAvailable == null)
			return null;
		unavailableRoles.remove(toMakeAvailable);
		return toMakeAvailable;
	}
	
	public void makeAvailable(Member m){
		removeFromAvailable(m.getSimpleName());
		if(team != null)
			m.setColor(getColor());
		members.add(m);
	}
	
	//class name, not given name
	public void makeAvailable(String className){
		Member toMakeAvailable = removeFromAvailable(className);
		if(toMakeAvailable == null)
			return;
		if(team != null)
			toMakeAvailable.setColor(team.getColor());
		members.add(toMakeAvailable);
	}
	
	public Faction(Team team, RandomRole toAdd) {
		this(team);
		
		addCollection(toAdd.list.values());
		
		isEditable = false;
	}
	
	private void addCollection(Collection<Member> members){
		for(Member m: members){
			makeAvailable(m);
		}
	}

	private String color, name;
	
	public String getName(){
		if(team != null)
			return team.getName();
		return name;
	}
	
	public String getColor(){
		if(team != null)
			return team.getColor();
		return color;
	}
	
	public void add(ArrayList<RoleTemplate> list) {
		for(RoleTemplate m: list){
			if(m.isRandom())
				members.add(m);
			else{
				Member memb = (Member) m;
				if(team != null)
					memb.setColor(getColor());
				makeAvailable(memb);
			}
		}
	}
	
	public void setTeam(Team t){
		this.team = t;
	}
	
	private Team team;
	public String getDescription() {
		if(team == null)
			return description;
		return team.getDescription();
	}
	
	private String description;
	public void setDescription(String string) {
		this.description = string;
	}
	public ArrayList<String> getRules() {
		ArrayList<String> list = new ArrayList<>();
		if(team == null)
			return list;
		
		list.add(team.getColor() + "kill");
		list.add(team.getColor() + "identity");
		list.add(team.getColor() + "liveToWin");
		list.add(team.getColor() + "priority");
		
		return list;
	}

	public boolean isEditable = true;
	public void makeUnavailable(String roleName) {
		Member newUnavailable = null;
		for(RoleTemplate rt: members){
			if(rt.isRandom())
				continue;
			Member m = (Member) rt;
			if(m.getName().equals(roleName)){
				newUnavailable = m;
				break;
			}
		}
		if(newUnavailable == null)
			return;
		unavailableRoles.add(newUnavailable);
		members.remove(newUnavailable);

		Collections.sort(unavailableRoles);
	}

	public Team getTeam() {
		return team;
	}

}
