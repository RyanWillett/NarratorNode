/// Initialize Firebase
function initFireBase() {
	var config = {
		apiKey: "AIzaSyBqatywpJsXTXBmYP9p8eR-p3B0OWsmDTk",
		authDomain: "narrator-119be.firebaseapp.com",
		databaseURL: "https://narrator-119be.firebaseio.com",
		storageBucket: "narrator-119be.appspot.com",
	};
	firebase.initializeApp(config);
}
initFireBase();

var main = document.getElementById("main");
var login_page = document.getElementById("login_page");
var setup_page = document.getElementById("setup_page");
var lobby_page = document.getElementById("lobby_page");

var user = null;
var gameState={};
gameState.started = false;
gameState.host = false;
gameState.commandsIndex = 0;
gameState.isOver = false;
gameState.timer = -2;

var J = {};
J.guiUpdate = 'guiUpdate';

J.dayLabel = 'dayLabel';

J.type = 'type';

J.playerList = 'playerLists';

J.requestGameState = 'requestGameState';
J.requestChat = 'requestChat';

J.roles = 'roles';
J.color = 'color';

J.roleInfo = 'roleInfo';
J.roleName = 'roleName';
J.roleDescription = 'roleDescription';
J.roleTeam = 'roleTeam';

J.gameStart = "gameStart";

J.isDay = "isDay";
J.showButton = "showButton";
J.endedNight = "endedNight";

J.graveYard = "graveYard";

J.isHost = "isHost";
J.addRole = "addRole";
J.removeRole = "removeRole";
J.startGame = "startGame";
J.host = "host";
J.timer = "timer";
J.rules = "rules";
J.ruleChange = "ruleChange";

J.skipVote = "skipVote";
J.isSkipping = "isSkipping";

J.TOWN = "#6495ED";
J.MAFIA = "#FF0000";
J.YAKUZA = "#FFD700";

J.catalogue = {
	town:[
		{name: 'Citizen',    color: J.TOWN},
		{name: 'Sheriff',    color: J.TOWN},
		{name: 'Detective',  color: J.TOWN},
		{name: 'Lookout',    color: J.TOWN},
		{name: 'Doctor',     color: J.TOWN},
		{name: 'Bodyguard',  color: J.TOWN},
		{name: 'Escort',     color: J.TOWN},
		{name: 'Bus Driver', color: J.TOWN},
		{name: 'Veteran',    color: J.TOWN},
		{name: 'Vigilante',  color: J.TOWN},
		{name: 'Mayor',      color: J.TOWN}
	],
	mafia:[
		{name: 'Mafioso',     color: J.MAFIA},
		{name: 'Godfather',   color: J.MAFIA},
		{name: 'Consort',     color: J.MAFIA},
		{name: 'Chauffeur',   color: J.MAFIA},
		{name: 'Agent',       color: J.MAFIA},
		{name: 'Blackmailer', color: J.MAFIA},
		{name: 'Framer',      color: J.MAFIA},
		{name: 'Janitor',     color: J.MAFIA},
	],
	yakuza:[
		{name: 'Mafioso',     color: J.YAKUZA},
		{name: 'Godfather',   color: J.YAKUZA},
		{name: 'Consort',     color: J.YAKUZA},
		{name: 'Chauffeur',   color: J.YAKUZA},
		{name: 'Agent',       color: J.YAKUZA},
		{name: 'Blackmailer', color: J.YAKUZA},
		{name: 'Framer',      color: J.YAKUZA},
		{name: 'Janitor',     color: J.YAKUZA},
	],
	neutrals:[
		{name: 'Serial Killer', color: "#8B4513"},
		{name: 'Arsonist',      color: "#FF8C00"},
		{name: 'Mass Murderer', color: "#CD853E"},
		{name: 'Witch',         color: "#666600"},
		{name: 'Cultist',       color: "#D5E68C"},
		{name: 'Cult Leader',   color: "#D5E68C"},
		{name: 'Jester',        color: "#DDA0DD"},
		{name: 'Executioner',   color: "#DDA0DD"}
	],
	randoms:[
		{name: "Any Random",         color: "#FFFFFF"},
		{name: "Town Random",        color: J.TOWN},
		{name: "Town Investigative", color: J.TOWN},
		{name: "Town Protective",    color: J.TOWN},
		{name: "Town Killing",       color: J.TOWN},
		{name: "Mafia Random",       color: J.MAFIA},
		{name: "Yakuza Random",      color: J.YAKUZA},
		{name: "Neutral Random",     color: "#888888"}
	]
};

J.descriptions = {
	Citizen: {
		description: "During the day you are a voice of reason.  You don't do anything at night.",
		rules: []
	},
	Agent: {
		description: "You can stalk someone to find out who they visited and who visited them.",
		rules: []
	},
	Arsonist: {
		description: "You have the ability to douse someone in flammable gasoline, undouse them, or burn everyone you previously doused.",
		rules: [{text: "Invulnerable",
				type: "checkbox",
				name: "arsonInvulnerability"},
				{text: "Day Ignite",
				type: "checkbox",
				name: "arsonDayIgnite"}]
	},
	Blackmailer: {description: "You have the ability to stop people from voting and talking.",
		rules: []
	},
	Escort: {
		description: "You entertain at night. They will not be able to complete their night actions.",
		rules: [{text: "Roleblock Immune",
				type: "checkbox",
				name: "blockImmune"}]
	},
	Consort: {
		description: "You entertain at night. They will not be able to complete their night actions.",
		rules: [{text: "Roleblock Immune",
				type: "checkbox",
				name: "blockImmune"}]
	},
	Bodyguard: {
		description: "You guard people from death.  If they are attacked, you will kill the attacker but also die in the process.",
		rules: []
	},
	Cultist: {
		description: "You are part of the cult.  Collaborate with your leader to convert someone.",
		rules: []
	},
	'Cult Leader': {
		description: "You can recruit anyone else into the cult. Expand until you are one with all.",
		rules: [{text: "Cult keeps roles",
				type: "checkbox",
				name: "cultKeepRole"},
				{text: "Cult PR conversion cooldown",
				type: "number",
				name: "cultPRCooldown"},
				{text: "Cult conversion cooldown",
				type: "number",
				name: "cultConversionCD"},
				{text: "Cult implodes upon death",
				type: "checkbox",
				name: "cultImplodes"}]
	},
	Detective: {
		description: "You have the ability to find out who someone visits.",
		rules: []
	},
	Doctor: {
		description: "You have the ability to save someone from an attack.",
		rules: [{text: "Knows if successful",
				type: "checkbox",
				name: "doctorNotification"}]
	},
	'Bus Driver': {
		description: "You can pick up any two people to drive around.  Any action that affects one will instead affect the other.",
		rules: []
	},
	Chauffeur: {
		description: "You can pick up any two people to drive around.  Any action that affects one will instead affect the other.",
		rules: []
	},
	Executioner: {
		description: "Your sole purpose in life is to get your target killed. Do it.",
		rules: [{text: "Exec Immune",
				type: "checkbox",
				name: "execImmune"},
				{text: "Exec Immune on Win",
				type: "checkbox",
				name: "execWinImmune"}]
	},
	Framer: {
		description: "You have the ability to change how people look to sheriffs for the night.",
		rules: []
	},
	Godfather: {
		description: "You are the leader of your team!  You can override who is sent to kill",
		rules: [{text: "Night Kill Immune",
				type: "checkbox",
				name: "gfInvulnerability"},
				{text: "Detection Immune",
				type: "checkbox",
				name: "gfUndetectable"}]
	},
	Janitor: {
		description: "You have the ability to hide the role of a person from being annouced to the town.",
		rules: []
	},
	Jester: {
		description: "Your goal is to get yourself lynched.  Do it through any means necessary.",
		rules: []
	},
	Lookout: {
		description: "You have the ability to find out all who visit someone.",
		rules: []
	},
	Mafioso: {
		description: "You are a minion of the Mafia.  Collaborate on who to kill during the night, and cause havoc during the day.",
		rules: []
	},
	'Mass Murderer': {
		description: "You have the ability to kill everyone who visits your night target.",
		rules: [{text: "Invulnerable",
				type: "checkbox",
				name: "mmInvulnerability"}, 
				{text: "Spree Delay",
				type: "number",
				name: "mmDelay"}]
	},
	Mayor: {
		description: "You are the leader of the town!  At any point during the day, you can reveal yourself and gain extra votes.",
		rules: [{text: "Vote Power",
				name: "mayorVote",
				type: "number"}]
	},
	'Serial Killer': {
		description: "You are a crazed psychopath trying to kill everyone. Do it.",
		rules: [{text: "Invulnerable",
				type: "checkbox",
				name: "skInvulnerability"}]
	},
	Sheriff: {
		description: "You have the ability to see what team someone is on.",
		rules: []
	},
	Veteran: {
		description: "You have the ability to kill all who visit you when you are on alert.",
		rules: [{text: "Charges",
				type: "number",
				name: "vetShots"}]
	},
	Vigilante: {
		description: "You have the ability to kill people at night.",
		rules: [{text: "Charges",
				type: "number",
				name: "vigShots"}]
	},
	Witch: {
		description: "You have the ability to change someone else's action target.",
		rules: [{text: "Leaves Feedback",
				type: "checkbox",
				name: "witchFeedback"}]
	},
	'Any Random': {
		description: "Spawns any role.",
		rules: []
	},
	'Town Random': {
		description: "Spawns:<br> Citizen<br>Sheriff<br>Doctor<br>Lookout<br>Detective<br>Bus Driver<br>Escort<br>Vigilante<br>Mayor<br>Bodyguard<br>Veteran",
		rules: []
	},
	'Town Investigative': {
		description: "Spawns:<br>Sheriff<br>Lookout<br>Detective",
		rules: []
	},
	'Town Protective': {
		description: "Spawns:<br>Doctor<br>Bus Driver<br>Escort<br>Bodyguard",
		rules: []
	},
	'Town Killing': {
		description: "Spawns:<br>Vigilante<br>Bodyguard<br>Veteran",
		rules: []
	},
	'Yakuza Random': {
		description: "Spawns:<br>Mafioso<br>Godfather<br>Consort<br>Chauffeur<br>Agent<br>Blackmailer<br>Framer<br>Janitor",
		rules: []
	},
	'Mafia Random': {
		description: "Spawns:<br>Mafioso<br>Godfather<br>Consort<br>Chauffeur<br>Agent<br>Blackmailer<br>Framer<br>Janitor",
		rules: []
	},
	'Neutral Random': {
		description: "Spawns<br>Serial Killer<br>Arsonist<br>Mass Murderer<br>Witch<br>Cultist<br>Cult Leader<br>Jester<br>Executioner",
		rules: []
	}
};

function toJString(user, message){
	var o = {
		"email" : user.email.toString(), 
		"name" : user.displayName.toString(), 
		"message": message,
		"server": false
	};
	return JSON.stringify(o);
}

function addToChat(message){
	if (message.length === 0)
		return;
	var toAppend = message.replace('\n', '');
	var element;
	var texts;
	if($("#lobby_page").is(":visible")){
		element = $('#lobby_messages');
	}else if(gameState.started){
		element = $('#messages');
	}else{
		element = $('#pregame_messages');
	}
	element.append($('<li>').html(toAppend));

	if($("#lobby_page").is(":visible")){
		texts = $('#lobby_messages li').length - 1;
	}else if(gameState.started){
		texts = $('#messages li').length - 1;
	}else{
		texts = $('#pregame_messages li').length - 1;
	}
	if($("#lobby_page").is(":visible")){
		$('#lobby_messages li')[texts].scrollIntoView();
	}else if(gameState.started)
		$('#messages li')[texts].scrollIntoView();
	else
		$('#pregame_messages li')[texts].scrollIntoView();
}


var socket = null;
function web_send(o){
	o.email = user.email;
	o.name  = user.displayName;
	socket.send(JSON.stringify(o));
}
$('form').submit(function(event){
	if (socket !== null)
		return false;
	else
		addToChat('NOT CONNECTED', $('#m').val());
	$('#m').val('');
	return false;
});

function sendRules(){
	var toSend = {};
	toSend.message = J.ruleChange;
	toSend[J.ruleChange] = gameState.rules;
	web_send(toSend);
}

function onRuleClickChange(e){
	e = e.target;
	var value = e.checked;
	var name = $("#roleDescriptionLabel").text();
	var info = J.descriptions[name];
	var i = e.parentElement.id.substring(1);
	i = parseInt(i);
	var rule = info.rules[i];
	gameState.rules[rule.name] = value;

	sendRules();
}

function onRuleValueChange(e){
	e = e.target;
	var value = parseInt(e.value);
	var name = $("#roleDescriptionLabel").text();
	var info = J.descriptions[name];
	var i = e.parentElement.id.substring(1);
	i = parseInt(i);
	var rule = info.rules[i];
	gameState.rules[rule.name] = value;

	sendRules();
}

function setRegularRules(){
	$(".general_rules").unbind();
	$(".general_rules").prop('disabled', !gameState.isHost);
	$("#dayStartRule").prop('checked', gameState.rules.dayStart);
	$("#nightLengthRule").val(gameState.rules.nightLength);
	$("#dayLengthRule").val(gameState.rules.dayLength);

	if(!gameState.isHost)
		return;
	$("#dayStartRule").click(function(e){
		gameState.rules.dayStart = e.target.checked;
		sendRules();
	});
	$("#nightLengthRule").bind('keyup input', function(e){
		gameState.rules.nightLength = parseInt(e.target.value);
		sendRules();
	});
	$("#dayLengthRule").bind('keyup input', function(e){
    	gameState.rules.dayLength = parseInt(e.target.value);
		sendRules();
	});
}

J.MAX_RULES = 4;
function setRules(name, color){
	setRegularRules();
	$("#rules_pane").show();
	var info = J.descriptions[name];
	if(info === undefined)
		return;
	var header = $('#roleDescriptionLabel');
	header.text(name);
	header.css('color', color);
	$('#roleDescriptionText').html(info.description);

	var rule, element, input;
	for (var i = 0; i < info.rules.length; i++){
		rule = info.rules[i];
		element = $('#r' + i);
		element.html(rule.text + " <input class='numberInput' type=" + rule.type + ">");
		element.show();
		input = $('#r' + i + " input");
		input.unbind();
		if(rule.type === 'checkbox'){
			input.prop('checked', gameState.rules[rule.name]);
			if(gameState.isHost)
				input.click(onRuleClickChange);
		}else{
			input.val(gameState.rules[rule.name]);
			if(gameState.isHost)
				input.bind('keyup input', onRuleValueChange);
		}
			
		
		input.prop('disabled', !gameState.isHost);
	}
	for (i =info.rules.length; i < J.MAX_RULES; i++){
		element = $('#r' + i);
		element.hide();
	}
}

function convertColor(color){
	color = color.replace(" ", "");
	color = color.replace(" ", "");
	color = color.replace("rgb(", "");
	color = color.replace(")", "");
	color = color.split(',');

	return "#" + hex(color[0]) + hex(color[1]) + hex(color[2]);
}

function getMemberFromClick(e){
	var obj = {}
	obj.name = e.innerHTML;
	obj.color = convertColor(e.style.color);
	return obj;
}

function addRole(name, color){
	var role_object = {};
	role_object.message = J.addRole;
	role_object.roleColor = color;
	role_object.roleName = name;
	
	if(gameState.isHost)
		web_send(role_object);
}

function removeRole(name, color){
	var role_object = {};
	role_object.message = J.removeRole;
	role_object.roleColor = color;
	role_object.roleName = name;
	if(gameState.isHost)
		web_send(role_object);
}

function setCatalogue(cata){
	$("#rules_pane").hide();
	var pane = $("#role_catalogue_clickable");
	pane.empty();

	for (var i = 0; i < cata.length; i++){
		role_li = $('<li>'+cata[i].name+'</li>');
		role_li[0].style.color = cata[i].color;
		role_li.appendTo(pane);
		if(!gameState.started){
			role_li.addClass("pregame_li");
		}
	}
	if(!gameState.started){
		pane.unbind();
		pane.on("click", "li", function (e){
			var obj = getMemberFromClick(e.target);
			setRules(obj.name, obj.color);
		});
		pane.on("dblclick", "li", function(e){
			var memb = getMemberFromClick(e.target);
			
			setRules(memb.name, memb.color);
			addRole(memb.name, memb.color);
		});
	}
}

function hex(num){
	num = parseInt(num);
	num = num.toString(16).toUpperCase();
	if(num.length === 1)
		num = "0" + num;
	return num;
}

function setRolesList(rolesList_o){
	var rolesList;
	if(gameState.started)
		rolesList = $("#rolesList");
	else
		rolesList = $("#setup_roles_list");
	rolesList.empty();
	var color;
	var role_li;
	for (var i = 0; i < rolesList_o.length; i++){
		role_li = $('<li>'+rolesList_o[i].roleType+'</li>');
		role_li[0].style.color = rolesList_o[i].color;
		role_li.appendTo(rolesList);
		if(!gameState.started){
			role_li.addClass("pregame_li");
		}
	}
	if(!gameState.started){
		$('#ingameRolesPane span').text("Roles List (" + rolesList_o.length + ")");
		if(gameState.isHost){
			rolesList.unbind();
			rolesList.on("dblclick", ".pregame_li", function(e){
				var memb = getMemberFromClick(e.target);
				setRules(memb.name, memb.color);
				removeRole(memb.name, memb.color);
			});
			rolesList.on("click", ".pregame_li", function(e){
				var memb = getMemberFromClick(e.target);
				setRules(memb.name, memb.color);
			});
		}
	}
}
var my_ele;

function setGraveYard(graveYard_o){
	var graveYard = $("#graveYard");
	graveYard.empty();
	var color;
	var role_li;
	for (var i = 0; i < graveYard_o.length; i++){
		role_li = $('<li>'+graveYard_o[i].roleName+'</li>');
		role_li[0].style.color = graveYard_o[i].color;
		role_li.appendTo(graveYard);
	}
}

function setFrame(){
	var fSpinner = $("#frameChoiceSpinner");
	if(gameState.playerLists[J.type][gameState.commandsIndex] === 'Frame'){
		fSpinner.show();
		fSpinner.unbind();
		fSpinner.change(function(){
			for(var i = 0; i < gameState.visiblePlayers.length; i++){
				if(gameState.visiblePlayers[i].playerSelected){
					sendAction(true, gameState.visiblePlayers[i].playerName);
				}
			}
		})
	}else{
		fSpinner.hide();
	}
}

function refreshPlayers(){
	var playerLists = gameState.playerLists;
	var playerListType = playerLists[J.type][gameState.commandsIndex % playerLists[J.type].length];
	gameState.visiblePlayers = playerLists[playerListType];
	if(gameState.isDay)
		gameState.visiblePlayers.push({playerName:"Skip Day", playerActive: true, playerVote: gameState.skipVote, playerSelected: gameState.isSkipping});
	else
		setFrame();
	$('#commandLabel').html(playerListType);
	
	var selectables;
	var radioText = "";
	if(gameState.started){
		radioText = '<input class="radios" type="checkbox">';
		selectables = $('#selectables');
	}else{
		selectables = $('#setup_players_list');
	}
	
	var li, text, player;
	selectables.empty();
	for(var i = 0; i < gameState.visiblePlayers.length; i++){
		player = gameState.visiblePlayers[i];
		text = '<li name="' + player.playerName + '">'+radioText;
		if(player.playerActive){
			text += player.playerName;
		}else{
			text += "-" + player.playerName;
		}
		
		if(!gameState.started && player.playerName === gameState.host){
			text += " (Host)";
		}else if(gameState.isDay){
			text += ("<span style='float:right;'> (" + player.playerVote + ")</span>");
		}

		text += '</li>';
		li = $(text);
		if(gameState.started)
			li.find(">:first-child").prop('checked', player.playerSelected);
		li.appendTo(selectables);
	}

	if(gameState.started)
		$('.radios').change(filterRadio);
	else{
		$('#playerListPane span').text("Lobby (" + gameState.visiblePlayers.length + ")");
	}
}

function setMultiCommandButtons(){
	var len = gameState.playerLists[J.type].length;
	if(len < 2){
		$('#lt').hide();
		$('#gt').hide();
	}else{
		var lt = $('#lt');
		var gt = $('#gt');
		lt.show();
		gt.show();
		lt.unbind();
		gt.unbind();
		lt.click(function(){
			gameState.commandsIndex = (gameState.commandsIndex + len - 1) % len;
			refreshPlayers();
		});
		gt.click(function(){
			gameState.commandsIndex = (gameState.commandsIndex + 1) % len;
		
			refreshPlayers();
		});
	}
	
}

function sendAction(target, name){
	var command = $('#commandLabel').text();

	var message;
	if(!target){//untarget
		if(command === "Vote"){
			message = "unvote";
		}else
			message = command + " untarget";
	}else{
		message = command + " " + name;
		if(command === "Vote" && name == "Skip Day")
			message = "skip day";
		if(command === "Frame"){
			message += (" " + $("#frameChoiceSpinner").val());
		}
	}
	console.log(message);
	web_send({message: message});
}

function filterRadio(event){
	return onRadioClick(event.target);
}
function onRadioClick(e){
	var checked = e.checked;
	
	var name = e.parentElement.getAttribute('name');
	console.log(name);
	sendAction(checked, name);
}


function setLivePlayers(playerLists){
	gameState.playerLists = playerLists;

	setMultiCommandButtons();

	refreshPlayers();
}

function setDayLabel(label){
	$('#dayLabel').html(label);
}

function setTimer(){
	if(gameState.timer > 0){
		var min = Math.floor(gameState.timer/60000);
		var seconds = Math.floor((gameState.timer % 60000) / 1000).toString();
		if(seconds.length < 2)
			seconds = "0" + seconds;

		$("#timer").text(min + ":" + seconds);
		gameState.timer -= 1000;
	}
	setTimeout(setTimer, 1000);
}

function setTrim(color){
	trimObjects = $('.trim');
	for(var i = 0; i < trimObjects.length; i++){
		trimObjects[i].style.color=color;
	}
}
setTrim("#008080");

function showButton(bool){
	var button = $("#theButton");
	if(bool){
		button.show();
		if(gameState.isDay){
			if(gameState.role.roleName === 'Mayor'){
				button.text('Reveal');
			}else{
				button.text('Burn');
			}
		}else{
			if(gameState.endedNight){
				button.text('Cancel End Night');
			}else
				button.text('End Night');
		}

	}
	else
		button.hide();
}

function setRoleInfo(roleInfo){
	gameState.role = roleInfo;
	setTrim(roleInfo.roleColor);
	$("#roleCardHeader").html(user.displayName + " (" + roleInfo.roleName + ")");
	$(".roleCard p").html(roleInfo.roleDescription);
}

function setHost(bool){
	gameState.isHost = bool;
	var startButton;
	var leaveButton;
	if(gameState.isHost){
		startButton = $("#setupButtonB");
		leaveButton = $("#setupButtonA");
		startButton.show();
	}
	else{
		leaveButton = $("#setupButtonB");
		startButton = $("#setupButtonA");
		startButton.hide();
	}
	startButton.unbind();
	leaveButton.unbind();
	leaveButton.show();

	leaveButton.text('Leave');
	startButton.text("Start");

	leaveButton.css('color', "#DA340D");
	startButton.css('color', '#159C0B');

	$(".general_rules").prop('disabled', !gameState.isHost);


	startButton.on("click", function(){
		if(gameState.isHost)
			web_send({message: J.startGame});	
	});
	leaveButton.on("click", function(){
		web_send({message: "leaveGame"});
	});
}

function setGlobalPlayerList(players){
	var pList = $("#global_players_list");
	pList.empty();
	for(var i = 0; i < players.length; i++){
		pList.append("<li>" + players[i]+"</li>");
	}
}

function hasType(key, type){
	return (type.indexOf(key) >= 0);
}
function handleObject(object){
	if(object.lobbyUpdate){
		if(object.playerList !== undefined){
			setGlobalPlayerList(object.playerList);
			return;
		}
		main.hidden = true;
		setup_page.hidden = true;
		lobby_page.hidden = false;
		if(object.reset)
			$("#lobby_messages").empty();
		for(var i = 0; i < object.message.length; i++){
			addToChat(object.message[i]);
		}
	}else if(object.guiUpdate){
		gameObject = object;
		if(object[J.gameStart] !== undefined){
			gameState.started = object[J.gameStart];
			if(gameState.started){
				main.hidden = false;
				setup_page.hidden = true;
			}else{
				main.hidden = true;
				setup_page.hidden = false;
			}
			lobby_page.hidden = true;
		}
		if(object[J.host] !== undefined){
			gameState.host = object[J.host];
		}
		if(object[J.endedNight] !== undefined)
			gameState.endedNight = object[J.endedNight];
		if(object[J.isHost] !== undefined)
			setHost(object[J.isHost]);

		if(hasType(J.roleInfo, object.type))
			setRoleInfo(object.roleInfo);

		if(object[J.isDay] !== undefined)
			gameState.isDay = object[J.isDay];
		if(object[J.showButton] !== undefined)
			showButton(object[J.showButton]);

		if(object[J.skipVote] !== undefined){
			gameState.skipVote = object[J.skipVote];
			gameState.isSkipping = object[J.isSkipping];
		}
		if(hasType(J.playerList, object.type)){
			setLivePlayers(object.playerLists);
		}

		if(hasType(J.roles, object.type))
			setRolesList(object.roles);

		if(hasType(J.graveYard, object.type))
			setGraveYard(object.graveYard);

		if(hasType(J.dayLabel, object.type))
			setDayLabel(object.dayLabel);
		if(object[J.timer] !== undefined)
			gameState.timer = object[J.timer];
		
		if(hasType(J.rules, object.type)){
			gameState.rules = object.rules;
			var ele = $('#roleDescriptionLabel');
			setRules(ele.text(), convertColor(ele.css("color")));
		}

		if(object.ping !== undefined){
			$("#ping")[0].play();
		}



		
	}else{	
		if(object.chatReset){

			var element;
			if(gameState.started)
				element = $('#messages');
			else
				element = $('#pregame_messages');
			
			element.empty();
		}
		var splits = object.message.split('\n');
		for (var i = 0; i < splits.length; i++){
			addToChat(splits[i]);
		}
	}
}

function host_submit(){
	var inp = $("#host_name_input");
	var o = {};
	o.action = true;
	o.message = "joinPrivate";
	o.hostName = inp.val();
	web_send(o);
	inp.val('');
	return false;
}

firebase.auth().onAuthStateChanged(function(user_o){
	if(user_o && user === null){//to ensure that we're not initiating a websocket a million times
		user = user_o;
		$("#roleCardHeader").html(user.displayName);
		var host = location.origin.replace(/^http/, 'ws');
		var ws = new WebSocket(host);

		$('form').submit(function(e){
			e = e.target.childNodes[0];
			if(e.id === 'host_name_input')
				return host_submit();

			var team, m, message;
			if($("#lobby_page").is(":visible")){
				m = $("#m_lobby");
				message = m.val();
			}else{
				if(gameState.started){
					team = gameState.role.roleColor;
					m = $('#m');
				}else{
					team = "null";
					m = $('#m_setup');
				}
				message = 'say ' + team + ' ' + m.val();
			}
			
			if(m.val().length === 0)
				return false;

			
			var text = toJString(user_o, message);
			var o = JSON.parse(text);
			o.action = false;
			text = JSON.stringify(o);
			ws.send( text );
			m.val('');
			return false;
		});

		ws.onmessage = (function(msg){
			handleObject(JSON.parse(msg.data));
			if(user.displayName === "Voss")
				$("#newChatMessageAudio")[0].play();
		});

		ws.onopen = (function(){
			console.log('websocket client opened');
			socket = ws;
			ws.send(toJString(user, "greeting"));
			setTimer();
			/*THIS THIS
			
			
			
			ws.send(toJString(user, J.requestChat)); */
		});
		ws.onclose = (function(){
			$('#messages').append($('<li>').text("Connection with server terminated"));
			socket = null;
		});

		var button = $("#theButton");
		button.on("click", function(){
			if(gameState.isOver){

			}else if(gameState.isDay){
				if(gameState.role.roleName === 'Mayor'){
					web_send({message: 'reveal'});
				}else{
					web_send({message: 'burn'});
				}
			}else{
				web_send({message: 'end night'});
				gameState.endedNight = !gameState.endedNight;
				if(gameState.endedNight){
					button.text('Cancel End Night');
				}else
					button.text('End Night');
			}


		});
		

	}else if (user_o === null){
		login_page.hidden = false;
	}
});

function logout(){
	firebase.auth().signOut().then(function(){
		main.hidden = true;
		login_page.hidden = false;
	}, function(){
		console.log('fail');
	});
}

function login(){
	username = $("#username_field")[0].value;
	password = $("#password_field")[0].value;
	firebase.auth().signInWithEmailAndPassword(username, password)
	.then(function(user){
		main.hidden = false;
		login_page.hidden = true;
	},
	function error(error){
		console.log(error);
	});
}

function setDisplayName(user_o){
	var name = $("#display_field")[0].value.replace(/ /g, '');
	return user_o.updateProfile({displayName: name});
}

function signup(){
	display_field = $("#display_field")[0];
	if (display_field.hidden){
		display_field.hidden = false;
	}else{
		username = $("#username_field")[0].value;
		password = $("#password_field")[0].value;
		firebase.auth().createUserWithEmailAndPassword(username, password)
		.then(setDisplayName)
		.then(function(user){
			main.hidden = false;
			login_page.hidden = true;
		},
		function error(error){
			console.log(error);
		});
	}
}

$(document).ready(function(){
	$("#logoutButton")[0].onclick = logout;
	$("#login_button")[0].onclick = login;
	$("#signup_button")[0].onclick = signup;
	$("#team_catalogue_pane").on("click", "li", function(e){
		var catalog;
		switch(e.target.innerHTML){
		case 'Town':
			catalog = J.catalogue.town;
			break;
		case 'Mafia':
			catalog = J.catalogue.mafia;
			break;
		case 'Yakuza':
			catalog = J.catalogue.yakuza;
			break;
		case 'Neutrals':
			catalog = J.catalogue.neutrals;
			break;
		default:
			catalog = J.catalogue.randoms;
		}
		setCatalogue(catalog);
	});
	$(".lobby_buttons").unbind();
	$("#joinButton").click(function(){
		var o = {};
		o.action = true;
		o.message = "joinPublic";
		web_send(o);
	});
	$("#hostPublicButton").click(function(){
		var o = {};
		o.action = true;
		o.message = "hostPublic";
		web_send(o);
	});
});
