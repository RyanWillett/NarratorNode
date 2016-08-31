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
gameState.isObserver = true;

gameState.editingAllies = false;
gameState.editingRoles = false;

var J = {};
J.guiUpdate = 'guiUpdate';

J.dayLabel = 'dayLabel';
J.gameID = 'gameID';
J.isObserver = 'isObserver';

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

function isOnLobby(){
	return $("#lobby_page").is(":visible");
}

function addToChat(message){
	if (message.length === 0)
		return;
	var toAppend = message.replace('\n', '');
	var element;
	var texts;
	if(isOnLobby()){
		element = $('#lobby_messages');
	}else if(gameState.started){
		element = $('#messages');
	}else{
		element = $('#pregame_messages');
	}
	element.append($('<li>').html(toAppend));

	if(isOnLobby()){
		texts = $('#lobby_messages li').length - 1;
	}else if(gameState.started){
		texts = $('#messages li').length - 1;
	}else{
		texts = $('#pregame_messages li').length - 1;
	}
	if(isOnLobby()){
		$('#lobby_messages li')[texts].scrollIntoView();
	}else if(gameState.started)
		$('#messages li')[texts].scrollIntoView();
	else
		$('#pregame_messages li')[texts].scrollIntoView();
}


var socket = null;
function web_send(o){
	if(user.displayName === undefined || user.displayName === null)
		return;
	o.name  = user.displayName;
	o.slack = false;	
	if(socket === null)
		connect();
	else
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
	var color = $("#roleDescriptionLabel").css('color');
	color = convertColor(color);
	var member = gameState.factions[name+color];
	var i = e.parentElement.id.substring(1);
	i = parseInt(i);
	var ruleName = e.name;
	gameState.rules[ruleName].val = value;

	sendRules();
}

function onRuleValueChange(e){
	e = e.target;
	var value = parseInt(e.value);
	var ruleName = e.name;
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

J.MAX_RULES = 5;
gameState.activeRule = null;

function setActiveRule(newRule){
	gameState.activeRule = newRule;
}

function clearRules(){
	$('#roleDescriptionLabel').text("");
	var element;
	for (var i = 0; i < J.MAX_RULES; i++){
		element = $('#r' + i);
		element.hide();
	}
}

function setRules(){
	var rulePane = gameState.activeRule;
	if(rulePane === null){
		clearRules();
		return;
	}
	var name = rulePane.name;
	var color = rulePane.color;
	$("#rules_pane").show();

	var header = $('#roleDescriptionLabel');
	var teamRule = rulePane.members !== undefined;
	if(teamRule)
		header.attr('name', rulePane.color);
	else{
		header.attr('name', rulePane.name + rulePane.color);
		$(".editTeamTrio").hide();
	}
	header.text(name);
	header.css('color', color);

	if(rulePane.description === undefined)
		$('#roleDescriptionText').html("");
	else	
		$('#roleDescriptionText').html(rulePane.description.replace(new RegExp("\n", 'g'), "<br>"));

	var rule, element, input, id, type, val, i;
	if(rulePane.rules === undefined)
		i = 0;
	else{
		for (i = 0; i < rulePane.rules.length; i++){
			id = rulePane.rules[i];
			rule = gameState.rules[id];
			element = $('#r' + i);

			if(rule.isNum)
				type = "number";
			else
				type = "checkbox";

			element.html(rule.name + " <input class='numberInput' type=" + type + ">");
			element.show();

			input = $('#r' + i + " input");
			input.attr('name', rule.id);
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

function getMember(e){
	var o = {};
	o.name = e.innerHTML;
	o.color = convertColor(e.style.color);
	o.rule = gameState.factions[o.name + o.color];

	o.simpleName = e.getAttribute("name");
	return o;
}

function addRole(memb){
	var role_object = {};
	role_object.message = J.addRole;
	role_object.roleColor = memb.color;
	role_object.roleName = memb.name;
	
	if(gameState.isHost)
		web_send(role_object);
}

function removeRole(member){
	var role_object = {};
	role_object.message = J.removeRole;
	role_object.roleColor = member.color;
	role_object.roleName = member.name;
	if(gameState.isHost)
		web_send(role_object);
	setActiveRule(null);
	setRules();
}

gameState.activeFaction = null;

function setCatalogue(){
	var aFaction = gameState.activeFaction;
	if(aFaction === null)
		return;
	$("#newTeamColor, #newTeamName, #newTeamSubmitButton").hide();
	if(aFaction.isEditable && gameState.isHost)
		$("#editAlliesButton, #editRolesButton, #deleteTeamButton").show();
	else
		$("#editAlliesButton, #editRolesButton, #deleteTeamButton").hide();
	var pane = $("#role_catalogue_clickable");
	pane.empty();

	var leftList;
	if(gameState.editingAllies)
		leftList = aFaction.allies;
	else
		leftList = aFaction.members;

	var list_item;
	for (var i = 0; i < leftList.length; i++){
		list_item = $('<li>'+leftList[i].name+'</li>');
		list_item[0].style.color = leftList[i].color;
		list_item.appendTo(pane);
		if(!gameState.started){
			list_item.addClass("pregame_li");
		}
	}


	pane.unbind();
	pane.on("click", "li", function (e){
		if(gameState.editingRoles){

		}else if(gameState.editingAllies){

		}else{
			var obj = getMember(e.target);
			$("#editAlliesButton, #editRolesButton, #deleteTeamButton").hide();
			$(".addTeamTrio").hide();
			setActiveRule(obj.rule);
			setRules();
		}
	});
	pane.on("dblclick", "li", function(e){
		var memb = getMember(e.target);
		$("#editAlliesButton, #editRolesButton, #deleteTeamButton").hide();
		$(".addTeamTrio").hide();
		if(gameState.editingRoles){
			var o = {};
			o.roleName = memb.name;
			o.message = "removeTeamRole";
			o.color = gameState.activeFaction.color;
			web_send(o);
		}else if(gameState.editingAllies){
			var o = {};
			o.message = "removeTeamAlly";
			o.color = gameState.activeFaction.color;
			o.ally = memb.color;
			web_send(o);
		}else{
			addRole(memb);
		}
	});
	
}

function hex(num){
	num = parseInt(num);
	num = num.toString(16).toUpperCase();
	if(num.length === 1)
		num = "0" + num;
	return num;
}

function setRolesList(rolesList_o, func){
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
		role_li.attr('name', rolesList_o[i].simpleName);
		role_li.appendTo(rolesList);
		if(!gameState.started){
			role_li.addClass("pregame_li");
		}
	}
	if(!gameState.started){
		if(func === removeRole)
			$('#ingameRolesPane span').text("Roles List (" + rolesList_o.length + ")");
		if(gameState.isHost){
			rolesList.unbind();
			rolesList.on("dblclick", ".pregame_li", function(e){
				var memb = getMember(e.target);
				func(memb);
			});
			rolesList.on("click", ".pregame_li", function(e){
				if(!gameState.editingAllies && !gameState.editingRoles){
					var memb = getMember(e.target);
					setActiveRule(memb.rule);
					setRules();
				}else{
					//setActiveRule(null);
				}
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
	var disabledRadios = gameState.isObserver || gameState.endedNight && !gameState.isDay;
	$('.radios').prop('disabled', disabledRadios);

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

function addAvailableRole(e){
	var o = {};
	o.message = "addTeamRole";
	o.color = gameState.activeFaction.color;
	o.roleName = e.name;
	o.simpleName = e.simpleName;
	web_send(o);
}

function removeEnemy(e){
	var o = {};
	o.message = "removeTeamEnemy";
	o.color = gameState.activeFaction.color;
	o.enemy = e.color;
	web_send(o);
}

function displayTeamEditRoles(){
	if(gameState.activeFaction === null)
		return;
	var color = convertColor($("#roleDescriptionLabel").css('color'));
	var unAvailHeader = $("#availableRolesHeader");
	unAvailHeader.text("Available Roles");
	var availHeader = $("#inGameRolesHeader");
	availHeader.text("Blacklisted Roles");

	var faction = gameState.activeFaction;
	var blacklisted = [];
	var o;
	for(var i = 0; i < faction.blacklisted.length; i++){
		o = {}
		o.roleType = faction.blacklisted[i].name;
		o.color = faction.color;
		o.simpleName = faction.blacklisted[i].simpleName;
		blacklisted.push(o);
	}
	setRolesList(blacklisted, addAvailableRole);
	setCatalogue();
}

function displayTeamEditAllies(){
	if(gameState.activeFaction === null)
		return;
	var color = convertColor($("#roleDescriptionLabel").css('color'));
	var unAvailHeader = $("#availableRolesHeader");
	unAvailHeader.text("Allies");
	var availHeader = $("#inGameRolesHeader");
	availHeader.text("Enemies");

	var faction = gameState.activeFaction;
	var enemies = [];
	var o;
	for(var i = 0; i < faction.enemies.length; i++){
		o = {}
		o.roleType = faction.enemies[i].name;
		o.color = faction.enemies[i].color;
		o.simpleName = o.color;
		enemies.push(o);
	}
	setRolesList(enemies, removeEnemy);
	setCatalogue();
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

	if(gameState.isHost){
		$("#newTeamButton").show();
	}else{
		$("#newTeamButton, #deleteTeamButton").hide();
	}

	
	$("#rules_pane").hide();

	$("#deleteTeamButton").unbind();
	$("#deleteTeamButton").click(function(){
		if(gameState.editingRoles){
			$("#availableRolesHeader").text("Available Roles");
			setMode(false, false);
			setCatalogue();
			setRolesList(gameState.rolesList, removeRole);
			$("#deleteTeamButton").text("Delete Team");
		}else if(gameState.editingAllies){
			setMode(false, false);
			$("#availableRolesHeader").text("Available Roles");
			setCatalogue();
			setRolesList(gameState.rolesList, removeRole);
			$("#deleteTeamButton").text("Delete Team");
		}else{
			setActiveRule(null);
			var o = {message:"deleteTeam"};
			o.color = convertColor($("#roleDescriptionLabel").css('color'));
			web_send(o);
		}
	});

	$("#editRolesButton").unbind();
	$("#editRolesButton").click(function(){
		$("#deleteTeamButton").text("Save Changes");
		gameState.editingRoles = true;
		gameState.editingAllies = false;
		displayTeamEditRoles();
	});

	$("#editAlliesButton").unbind();
	$("#editAlliesButton").click(function(){
		$("#deleteTeamButton").text("Save Changes");
		gameState.editingRoles = false;
		gameState.editingAllies = true;
		displayTeamEditAllies();
	});

	if(gameState.editingRoles)
		displayTeamEditRoles();
	if(gameState.editingAllies)
		displayTeamEditAllies();
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
		$("#newTeamButton").show()
	}
	else{
		leaveButton = $("#setupButtonB");
		startButton = $("#setupButtonA");
		$("#newTeamButton").hide();
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
		if(gameState.isObserver){
			location.reload();
			return;
		}
		$("#pregame_messages").empty();
		web_send({message: "leaveGame"});
	});
}

function setGameID(){
	if(gameState.started){
		
	}else{
		$("#setup_top_div h2").text("New Game #" + gameState.gameID);
	}
}

function setGlobalPlayerList(players){
	var pList = $("#global_players_list");
	pList.empty();
	for(var i = 0; i < players.length; i++){
		pList.append("<li>" + players[i]+"</li>");
	}
}

var gameObject = null;
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
		if(object[J.isObserver] !== undefined)
			gameState.isObserver = object[J.isObserver];
		if(object[J.gameID] !== undefined){
			gameState.gameID = object[J.gameID];
			console.log(gameState.gameID);
			setGameID();
		}

		if(hasType(J.rules, object.type)){
			if(gameState.factions !== undefined && gameState.factions.factionNames.length !== object.factions.factionNames.length)
				$("#newTeamColor #newTeamName").val("");
			gameState.rules = object.rules;
			gameState.factions = object.factions;
			if(gameState.activeRule !== null && gameState.activeRule !== undefined){
				if(gameState.factions.factionNames.indexOf(gameState.activeRule.name) === -1 && gameState.activeRule.members !== undefined){
					setActiveRule(null);
				}else{
					var activeFactionColor = gameState.activeFaction.color;
					var newFaction = gameState.factions[activeFactionColor];
					if(gameState.activeRule !== null){
						if(gameState.activeRule.enemies === undefined){
							var tag = gameState.activeRule.name + gameState.activeRule.color;
							setActiveRule(gameState.factions[tag]);
						}
						else
							setActiveRule(gameState.factions[gameState.activeRule.name]);
					}
					gameState.activeFaction = newFaction;
					var header = $("#roleDescriptionLabel");

				}
			}
			setCatalogue();
			setFactions(); //has to be after the new faction is being set
			setRules();
			setRegularRules();
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

		if(hasType(J.roles, object.type)){
			gameState.rolesList = object.roles;
			if(!gameState.editingAllies && !gameState.editingRoles)
				setRolesList(gameState.rolesList, removeRole);
		}

		if(hasType(J.dayLabel, object.type))
			setDayLabel(object.dayLabel);
		if(object[J.timer] !== undefined)
			gameState.timer = object[J.timer];
		
		
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

function setSubmitFunction(){
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
		
		if(m.val().length === 0 || gameState.isObserver){
			m.val('');
			return false;
		}

		var o = {}
		o.action = false;
		o.message = message;
		web_send(o);

		m.val('');
		return false;
	});
}

function connect(){
	var host = location.origin.replace(/^http/, 'ws');
	if(!host.endsWith(":3000"))
		host = host + ":3000";
	var ws = new WebSocket(host);

	setSubmitFunction();

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
}

firebase.auth().onAuthStateChanged(function(user_o){
	if(user_o && user === null){//to ensure that we're not initiating a websocket a million times
		user = user_o;
		connect();
		

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

gameState.loggingIn = false;
function login(){
	gameState.loggingIn = true;
	username = $("#username_field")[0].value;
	password = $("#password_field")[0].value;
	firebase.auth().signInWithEmailAndPassword(username + "@sc2mafia.com", password)
	.then(function(user){
		login_page.hidden = true;
		gameState.loggingIn = false;
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
		gameState.loggingIn = false;
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

function isHex(h){
	var a = parseInt(h,16);
	a = a.toString(16).toLowerCase();
	while(a.length < 6)
		a = "0" + a;
	return (a === h.toLowerCase())
}

function setMode(eAllies, eRoles){
	gameState.editingAllies = eAllies;
	gameState.editingRoles = eRoles;
}

$(document).ready(function(){
	$("#logoutButton")[0].onclick = logout;
	$("#login_button")[0].onclick = login;
	$("#signup_button")[0].onclick = signup;
	$("#team_catalogue_pane").on("click", "li", function(e){
		var catalog = gameState.factions[e.target.innerHTML];
		if(gameState.activeFaction !== catalog && catalog !== undefined){
			setMode(false, false);
			gameState.activeFaction = catalog;
			$("#availableRolesHeader").text("Available Roles");
			setRolesList(gameState.rolesList, removeRole);
			setActiveRule(gameState.activeFaction);
			setRules();
			setCatalogue(); 
		}
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
	$("#newTeamSubmitButton").unbind();
	$("#newTeamSubmitButton").click(function(){
		var newColor = $("#newTeamColor");
		var newTeam = $("#newTeamName");
		var submit = $("#newTeamSubmitButton");
		

		
		var color = newColor.val();
		var team = newTeam.val();
		if(color.length === 0 || team.length === 0)
			return;
		if(color[0] !== "#"){
			color = "#" + color;
		}
		if(color.length !== 7 && color.length != 4){
			addToChat("RGB codes are typically 6 characters long.");
			return;
		}
		if(color.length === 4){
			color = color[0] + color[1] + color[1] + color[2] + color[2] + color[3] + color[3];
		}
		if(!isHex(color.substring(1))){
			addToChat("This isn't in RGB format.");
			return;
		}
		var o = {};
		o.message = "addTeam";
		o.color = color.toUpperCase();
		o.teamName = team;
		web_send(o);
	});
	$("#newTeamButton").click(function(){
		$(".editTeamTrio").hide();
		setActiveRule(null);
		setRules();

		var newColor = $("#newTeamColor");
		var newTeam = $("#newTeamName");
		$("#rules_pane p").text("");
		$("#roleDescriptionLabel").text("New Team Name");
		$("#roleDescriptionLabel").show();
		$("#rules_pane").show();
		$("#newTeamSubmitButton").show();
		newColor.show();
		newTeam.show();

		newTeam.unbind();
		newTeam.bind('keyup input', function(e){
			$("#roleDescriptionLabel").text(newTeam.val());
		});
		newColor.unbind();
		newColor.bind('keyup input', function(e){
			$("#roleDescriptionLabel").css('color', newColor.val());
		});
		$("#")
	});
});