package nnode;

import java.util.ArrayList;

import shared.logic.support.RoleTemplate;

public class Faction {

	public Faction(String name){
		this.name = name;
		members = new ArrayList<>();
	}
	
	public String color, name;
	public void setColor(String color) {
		this.color = color;
	}
	
	public ArrayList<RoleTemplate> members;
	public void add(ArrayList<RoleTemplate> list) {
		for(RoleTemplate m: list){
			this.members.add(m);
		}
	}

}
