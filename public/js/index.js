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
gameState.isAlive = true;

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

var targ;
function onRuleClickChange(e){
	e = e.target;
	targ = e;
	var value = e.checked;
	var name = $("#roleDescriptionLabel").text();
	var info = J.descriptions[name];
	var i = e.parentElement.id.substring(1);
	i = parseInt(i);
	var rule = info.rules[i];
	gameState.rules[rule].val = value;

	sendRules();
}

function onRuleValueChange(e){
	e = e.target;
	var value = parseInt(e.value);
	var name = $("#roleDescriptionLabel").text();
	var color = $("#roleDescriptionLabel").css('color');
	color = convertColor(color);
	var member = gameState.factions[name+color];
	var i = e.parentElement.id.substring(1);
	i = parseInt(i);
	var ruleName = member.rules[i];
	gameState.rules[ruleName].val = value;

	sendRules();
}

function setRegularRules(){
	$(".general_rules").unbind();
	$(".general_rules").prop('disabled', !gameState.isHost);
	$("#dayStartRule").prop('checked', gameState.rules["Day_Start"].val);
	$("#nightLengthRule").val(gameState.rules["Night_Length"].val);
	$("#dayLengthRule").val(gameState.rules["Day_Length"].val);

	if(!gameState.isHost)
		return;

	$("#dayStartRule").click(function(e){
		gameState.rules["Day_Start"].val = e.target.checked;
		sendRules();
	});
	$("#nightLengthRule").bind('keyup input', function(e){
		gameState.rules["Night_Length"].val = parseInt(e.target.value);
		sendRules();
	});
	$("#dayLengthRule").bind('keyup input', function(e){
    	gameState.rules["Day_Length"].val = parseInt(e.target.value);
		sendRules();
	});
}

J.MAX_RULES = 4;
function setRules(obj){
	var name = obj.name;
	var color = obj.color;
	setRegularRules();
	$("#rules_pane").show();
	var info = obj
	if(info === undefined)
		return;
	var header = $('#roleDescriptionLabel');
	header.text(name);
	header.css('color', color);
	$('#roleDescriptionText').html(info.description);

	var rule, element, input, id, type, val, i;
	if(info.rules === undefined)
		i = 0;
	else{
		for (i = 0; i < info.rules.length; i++){
			id = info.rules[i];
			rule = gameState.rules[id];
			element = $('#r' + i);
			val = rule.val;
			if(Number(rule.val) === rule.val && rule.val % 1 === 0)
				type = "number";
			else
				type = "checkbox";
			element.html(rule.name + " <input class='numberInput' type=" + type + ">");
			element.show();
			input = $('#r' + i + " input");
			input.unbind();
			if(type === 'checkbox'){
				input.prop('checked', rule.val);
				if(gameState.isHost)
					input.click(onRuleClickChange);
			}else{
				input.val(rule.val);
				if(gameState.isHost)
					input.bind('keyup input', onRuleValueChange);
			}
				
			
			input.prop('disabled', !gameState.isHost);
		}
	}
	for (; i < J.MAX_RULES; i++){
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
	var name = e.innerHTML;
	var color = convertColor(e.style.color);
	return gameState.factions[name + color];
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
	if(cata === undefined)
		return;
	$("#rules_pane").hide();
	var pane = $("#role_catalogue_clickable");
	pane.empty();

	var role_li;
	for (var i = 0; i < cata.members.length; i++){
		role_li = $('<li>'+cata.members[i].name+'</li>');
		role_li[0].style.color = cata.members[i].color;
		role_li.appendTo(pane);
		if(!gameState.started){
			role_li.addClass("pregame_li");
		}
	}
	if(!gameState.started){
		pane.unbind();
		pane.on("click", "li", function (e){
			var obj = getMemberFromClick(e.target);
			setRules(obj);
		});
		pane.on("dblclick", "li", function(e){
			var memb = getMemberFromClick(e.target);
			
			setRules(memb);
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
				setRules(memb);
				removeRole(memb.name, memb.color);
			});
			rolesList.on("click", ".pregame_li", function(e){
				var memb = getMemberFromClick(e.target);
				setRules(memb);
			});
		}
	}
}
var my_ele;

function setGraveYard(graveYard_o){
	gameState.graveyard = graveYard_o;
	var graveYard = $("#graveYard");
	graveYard.empty();
	var color;
	var role_li;
	for (var i = 0; i < graveYard_o.length; i++){
		if(graveYard_o[i].name === user.displayName)
			gameState.isAlive = false;
		role_li = $('<li>'+graveYard_o[i].roleName+'</li>');
		role_li[0].style.color = graveYard_o[i].color;
		role_li.appendTo(graveYard);
	}
}

function setFrame(){
	var fSpinner = $("#frameChoiceSpinner");
	if(gameState.playerLists[J.type][gameState.commandsIndex] === 'Frame' && gameState.isAlive){
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

function setCommandsLabel(label){
	if(label === 'Gun')
		label = 'Give Gun';
	if(label === 'Armor')
		label = 'Give Armor';
	if(label === 'swap1')
		label = 'Pickup 1'
	if(label === 'swap2')
		label = 'Pickup 2'
	$('#commandLabel').html(label);
}

function refreshPlayers(){
	var playerLists = gameState.playerLists;
	var playerListType = playerLists[J.type][gameState.commandsIndex % playerLists[J.type].length];
	if(gameState.isAlive)
		gameState.visiblePlayers = playerLists[playerListType];
	else
		gameState.visiblePlayers = [];
	if(gameState.isDay && gameState.isAlive)
		gameState.visiblePlayers.push({playerName:"Skip Day", playerActive: true, playerVote: gameState.skipVote, playerSelected: gameState.isSkipping});
	else
		setFrame();

	setCommandsLabel(playerListType);
	
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
	$('.radios').prop('disabled', gameState.endedNight && !gameState.isDay);

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
	if(!target && command === "Vote"){//untarget
		message = "unvote";
	}else{
		if(command === "Pickup 1")
			command = 'swap1';
		else if(command === "Pickup 2")
			command = 'swap2';
		else if(command === "Give Gun")
			command = 'gun';
		else if(command === "Give Armor")
			command = 'armor';
		message = command + " " + name;
		if(command === "Vote" && name == "Skip Day")
			message = "skip day";
		if(command === "Frame"){
			message += (" " + $("#frameChoiceSpinner").val());
		}
	}
	web_send({message: message});
}

function filterRadio(event){
	return onRadioClick(event.target);
}
function onRadioClick(e){
	var checked = e.checked;
	
	var name = e.parentElement.getAttribute('name');
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
		trimObjects.css('border-color', color);
	}
	trimObjects = $('.info-list-container');
	for(var i = 0; i < trimObjects.length; i++){
		trimObjects.css('border-color', color);
	}
}
setTrim("#008080");

function showButton(bool){
	var button = $("#theButton");
	if(bool){
		button.show();
		if(gameState.isOver){
			button.text('Exit Game');
		}
		else if(gameState.isDay){
			if(gameState.role.roleName === 'Mayor'){
				button.text('Reveal');
			}else{
				button.text('Burn');
			}
		}else{
			$('.radios').prop('disabled', gameState.endedNight || gameState.isDay);
			if(gameState.endedNight){
				button.text('Cancel End Night');
			}else
				button.text('End Night');
		}

	}
	else
		button.hide();
}

function setFactions(){
	var factionList = $("#team_catalogue_pane");
	factionList.empty();
	var factionItem, f, name;
	var factions = gameState.factions;
	for(var i = 0; i < factions.factionNames.length; i++){
		name = factions.factionNames[i];
		f = factions[name];
		factionItem = $('<li><span style="color: ' + f.color + ';">' + f.name + '</span></li>');
		factionList.append(factionItem);
	}
}

function setRoleInfo(roleInfo){
	gameState.role = roleInfo;
	$("#roleCardHeader").html(user.displayName + " (" + roleInfo.roleName + ")");
	var descriptionText = roleInfo.roleDescription;
	if (roleInfo.roleKnowsTeam){
		descriptionText += "<br><br><span class='trim'>Allies</span><br>";
		var ally;
		for (var i = 0; i < roleInfo.roleTeam.length; i++){
			ally = roleInfo.roleTeam[i];
			descriptionText += "<span style='color: " + ally.teamAllyColor + ";'>" + ally.teamAllyRole + "</span> : " + ally.teamAllyName + "<br>";
		}
	}
	$(".roleCard p").html(descriptionText);
	setTrim(roleInfo.roleColor);
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
		$("#pregame_messages").empty();
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
		if(object.isFinished !== undefined){
			gameState.isOver = object.isFinished;
		}
		if(object[J.host] !== undefined){
			gameState.host = object[J.host];
		}
		if(object[J.endedNight] !== undefined)
			gameState.endedNight = object[J.endedNight];
		if(object.factions !== undefined){
			gameState.factions = object.factions;
			setFactions();
		}
		if(object[J.isHost] !== undefined)
			setHost(object[J.isHost]);

		if(hasType(J.roleInfo, object.type))
			setRoleInfo(object.roleInfo);

		if(hasType(J.graveYard, object.type))
			setGraveYard(object.graveYard);

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

		if(hasType(J.dayLabel, object.type))
			setDayLabel(object.dayLabel);
		if(object[J.timer] !== undefined)
			gameState.timer = object[J.timer];
		
		if(hasType(J.rules, object.type)){
			gameState.rules = object.rules;
			var ele = $('#roleDescriptionLabel');
			setRules(ele.text());
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

function greetServer(){
	var o = {}
	o.message = "greeting";
	o.server = true;
	web_send(o);
	setTimer();
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

			var o = {}
			o.action = false;
			o.message = message;
			web_send(o);

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
			greetServer();
		});
		ws.onclose = (function(){
			$('#messages').append($('<li>').text("Connection with server terminated"));
			socket = null;
		});

		var button = $("#theButton");
		button.on("click", function(){
			if(gameState.isOver){
				location.reload();
			}else if(gameState.isDay){
				if(gameState.role.roleName === 'Mayor'){
					web_send({message: 'Reveal'});
				}else{
					web_send({message: 'Burn'});
				}
			}else{
				web_send({message: 'end night'});
				gameState.endedNight = !gameState.endedNight;
				if(gameState.endedNight){
					button.text('Cancel End Night');
				}else
					button.text('End Night');
				$('.radios').prop('disabled', gameState.endedNight || gameState.isDay);
			}


		});
		

	}else if (user_o === null){
		login_page.hidden = false;
		lobby_page.hidden = true;
		setup_page.hidden = true;
		main.hidden = true;
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
	firebase.auth().signInWithEmailAndPassword(username + "@sc2mafia.com", password)
	.then(function(user){
		login_page.hidden = true;
		greetServer();
	},
	function error(error){
		my_err = error;
		var errorBox = $("#errorCode");
		if(error.code === "auth/wrong-password"){
			errorBox.text('Incorrect password');
		}else if(error.code === "auth/user-not-found" || error.code === "auth/invalid-email"){
			errorBox.text('Username not found!');
		}else{
			console.log(error);
		}
	});
}

function setDisplayName(user_o){
	return user_o.updateProfile({displayName: user_o.email});
}

var my_err;
function signup(){
	username = $("#username_field")[0].value.replace(/ /g, '');
	if(username.length=== 0)
		return;
	password = $("#password_field")[0].value;
	firebase.auth().createUserWithEmailAndPassword(username + "@sc2mafia.com", password)
	.then(function(user_o){
		return user_o.updateProfile({displayName: username});
	})
	.then(function(user){
		login_page.hidden = true;
		greetServer();
	},
	function error(error){
		var errorBox = $("#errorCode");
		if(error.code === "auth/invalid-email"){
			errorBox.text('Badly formatted username');
		}else if(error.code === "auth/weak-password"){
			errorBox.text('Password is too weak.');
		}else if(error.code === "auth/email-already-in-use"){
			errorBox.text('That username is taken!');
		}else{
			console.log(error);
		}
	});
}

$(document).ready(function(){
	$("#logoutButton")[0].onclick = logout;
	$("#login_button")[0].onclick = login;
	$("#signup_button")[0].onclick = signup;
	$("#team_catalogue_pane").on("click", "li", function(e){
		var catalog = gameState.factions[e.target.innerHTML];
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