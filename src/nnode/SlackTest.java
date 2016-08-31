package nnode;

import java.io.IOException;

import android.texting.TextInput;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import shared.ai.Brain;
import shared.event.Message;
import shared.logic.Narrator;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.listeners.NarratorListener;
import shared.logic.support.Random;
import shared.logic.templates.BasicRoles;
import shared.logic.templates.TestController;

public class SlackTest implements NarratorListener, TextInput {

	SlackSwitch ss;
	Brain b;
	public SlackTest(String[] args) throws IOException, JSONException {
		
		
		this.ss = new SlackSwitch();
    	ss.setupSocket();
    	ss.n = Narrator.Default();
    	Narrator n = ss.n;
    	
    	n.addRole(BasicRoles.Citizen());
    	n.addRole(BasicRoles.Citizen());
    	n.addRole(BasicRoles.Citizen());
    	n.addRole(BasicRoles.Mafioso());
    	n.addRole(BasicRoles.Mafioso());
    	n.addRole(BasicRoles.Sheriff());
    	n.addRole(BasicRoles.Doctor());
    	
    	
    	
    	JSONArray jarry = new JSONArray();
    	for(int i = 1; i < args.length; i++)
    		jarry.put(args[i]);
    	JSONObject jo = new JSONObject();
    	jo.put("players", jarry);
    	ss.startGame(jo);
    	
    	b = new Brain(new PlayerList(), new Random());
    	for(Player p: n.getAllPlayers()){
    		b.addSlave(p, new TestController(n));
    	}

    	ss.startTimer();
    	n.addListener(this);
    	onGameStart();
		
		
	}

	public static void main( String[] args ) throws IOException, JSONException {
    	new SlackTest(args);
    }

	public void onGameStart() {
		if(ss.n.isDay())
			onDayStart(null);
		else
			onNightStart(null);
	}

	public void onNightStart(PlayerList lynched) {
		this.voterCount = 0;
		b.nightAction();
	}

	int voterCount = 0;
	public void onDayStart(PlayerList newDead) {
		PlayerList voters = b.dayAction();
		this.voterCount = voters.size();
	}

	@Override
	public void onEndGame() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMayorReveal(Player mayor, Message e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAssassination(Player assassin, Player target, Message e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onArsonDayBurn(Player arson, PlayerList burned, Message e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onVote(Player voter, Player target, int voteCount, Message e) {
		onVote();
		
	}

	@Override
	public void onUnvote(Player voter, Player prev, int voteCountToLynch, Message e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onChangeVote(Player voter, Player target, Player prevTarget, int toLynch, Message e) {
		onVote();
		
	}
	
	public synchronized void onVote(){
		voterCount--;
		if(voterCount <= 0)
			onDayStart(null);
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
		System.out.println(p.toString() + " ended night.");
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
		// TODO Auto-generated method stub
		
	}

	public void text(Player p, String message, boolean sync) {
		
		
	}
}
