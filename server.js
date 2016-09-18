var exec  = require('child_process').exec;
var fs    = require('fs');
var gcm   = require('node-gcm');
var mysql = require('mysql');
var keys  = require('./keys');
var slackAPI = require('slackbotapi');
const NARRATOR = "Narrator";
const MAFIA_CHANNEL_NAME = 'narrator_mafia';
const READ_JAVA_OUTPUT = true;
const LOG_CLIENT_OUTPUT = false;

var STARTERS = ['c1mckay', 'michaelcmkay', 'euromkay', 'charles', 'Voss'];

/*firebase.initializeApp({
  serviceAccount: "Narrator-1c6d40c23a30.json",
  databaseURL: "https://narrator-119be.firebaseio.com"
});*/

var androidNotifier = new gcm.Sender(keys.androidNotifToken);

var db_connection = mysql.createConnection({
  host     : 'localhost',
  user     : keys.databaseUser,
});


db_connection.connect(function(err) {
  if(err === null){
    console.log('Database connection success!\n\tUsing \'Users\' table');
    db_connection.query('use narrator');
  }else
    if(keys.runningLocally){
      if(err.code !== 'ER_ACCESS_DENIED_ERROR' && err.code !== 'ECONNREFUSED'){
        console.log(err);
      }else{
        console.log("No connection to database, because this is a local session.");
        db_connection = null;
      }
    }else{
      console.log(err);
    }
});



function query(query, fini){
  if(db_connection === null)
    return;
  var func = function(err, results){
    if(err === null){
      if(fini !== undefined)
        fini(results);
    }else{
      log(err);
    }
  }
  db_connection.query(query, func);
}

String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

function classExists(name){
  name = name.replace(".java", ".class");
  try{
    fs.statSync(name);
    return true;
  }catch(err){
    try{
      fs.statSync('src/' + name);
      return true;
    }catch(err){}
    return false;
  }
}

function compile_file(files, i, final_func){
  var comp_name;
  if (i >= files.length){
    final_func();
    return;
  }

  comp_name = files[i];
  console.log('compiling ' + comp_name);
  var command = "javac  -sourcepath src src/";
  exec(command + comp_name, {}, function(error, stdout, stderr){
    if (error) {
      console.log(error);
      return;
    }
    compile_file(files, i + 1, final_func);
  });
}

function sendNotification(title, message, recipients){
  var message = new gcm.Message({
      data: { 
        'title': title,         //this is the Title of the notification
        'message': message //This is the subtext of the notification
      }
  });
     
  androidNotifier.send(message, { registrationTokens: recipients }, function (err, response) {
      if(err) 
        console.error(err);
      else if(response.failure !== 0)
        console.log(response);
  });
}


var WebSocketServer = require("ws").Server;
var http = require("http");
var express = require("express");
var app = express();
var port = process.env.PORT || 3000;

app.use(express.static(__dirname + "/public"));

var server = http.createServer(app);
server.listen(port);

var wss = new WebSocketServer({server: server});
console.log("Websocket server created...");


/*function web_send_all(message){
  console.log('sending global web message ' + message);
  var key;
  for (key in connections_mapping){
    connections_mapping[key].send(message);
  }
}*/


function web_send(name, message){
  if(LOG_CLIENT_OUTPUT)
    console.log('sending web message to ' + name + " : " + message);
  var c = connections_mapping[name];
  //console.log(connections_mapping.length);
  if (c!== undefined && c !== null)
    c.send(message, function ack(error){
      if(error === undefined || error === null)
        return;
      console.log(error);
    });
}

var pipe = null;

function pipe_Jwrite(o){
  return pipe_write(JSON.stringify(o));
}

function toJava(o){
  if(pipe === null || pipe === undefined)
    return;

  //console.log(o);
  pipe.write(JSON.stringify(o) + "\n");
}

function pipe_write(m){
  if(pipe !== null){
    pipe.write(m + '\n');
    //console.log('(heroku -> java) "' + m + '"');
    return true;
  }
  console.log('pipe is null');
  return false;

}

function toJString(name, message){
  var o = {"message": message};
  o.name = name;
  return JSON.stringify(o);
}

connections_mapping = {};

wss.on("connection", function(ws) {
  var o = toJString(NARRATOR, "You are now connected!");
  ws.send(o);

  ws.on('message', function(message){
    //console.log('web -> heroku : ' + message);
    try{
      o = JSON.parse(message);

      if(o.name === null || o.name === undefined){
        o.name = generateName();
        message = JSON.stringify(o);
        o = JSON.parse(message);
      }
      if(o.token !== undefined && o.token !== null){
        console.log('saving notification id');
        saveAndroidToken(o.name, o.token);
      }
      if(connections_mapping[o.name] != ws && connections_mapping[o.name] !== undefined){
        connections_mapping[o.name].close();
      }
      connections_mapping[o.name] = ws;

      if(o.message.length !== 0){
        pipe_write(message);
      }
    }catch(myErrorMessage){
      console.log(myErrorMessage);
    }
  });

  ws.on('error', function(err){
    console.log(err);
  });

  ws.on("close", function() {
    var i;
    for (i in connections_mapping){
    	if (connections_mapping[i] === ws){
        o = {};
        o.server = true;
        o.message = 'disconnect';
        o.name = i;
        pipe_Jwrite(o);
        delete connections_mapping[i];
        break;
      }
    }
  });
})

function saveAndroidToken(name, tokenID){
  var queryString = "insert into users (username, android_id) values('" + name;
  queryString += "', '" + tokenID + "') on duplicate key update android_id = '";
  queryString += tokenID + "'";
  query(queryString);
}

var observerID = 1;
function generateName(){
  observerID++;
  return "oberver" + observerID;
}

function handle_java_event(jo){
  if (jo.message === "closeConnection"){
    var name;
    for (var i = 0; i < jo.players.length; i++){
      name = jo.players[i];
      connections_mapping[name].close();
      delete connections_mapping[name];
    }
  }
  else if (jo.message === "sendNotification"){
    var title = jo.title;
    var subtitle = jo.subtitle;
    var users = jo.recipients;

    var fini = function(results){
      var tokens = [];
      var id;
      for(var i = 0; i < results.length; i++){
        id = results[i]['android_id'];
        tokens.push(id);
      }
      sendNotification(title, subtitle, tokens);
    }

    var queryString = "SELECT android_id FROM users WHERE username in (";
    for(var i = 0; i < users.length; i++){
      queryString += ("'" + users[i]);
      if(i !== users.length - 1){
        queryString += "', "
      }
    }
    queryString += "')";
    query(queryString, fini);
  }else if(jo.message === 'slackAdd'){
    if(STARTERS.indexOf(jo.host) === -1)
      return;

    var apiToken;
    if(jo.slackField === 'sdsc'){
      apiToken = keys.sdscToken;
    }else if(jo.slackField === 'pepband'){
      apiToken = keys.pepbandToken;
    }else{
      apiToken = keys.myToken;
    }

    //apiToken = keys.myToken;
    initializeSlackBot(apiToken, jo.gameID);

  }else if(jo.message === 'slackMessage'){
    queueSlackMessage(jo);
  }else if(jo.message === 'slackKill'){
    var slackInstance = slackBots[jo.gameID];
    if(!slackInstance)
      return;
    slackBots[jo.gameID] = null;
    slackInstance.bot.autoReconnect = false;
    slackInstance.bot.ws.close();
    slackInstance.active = false;
  }
}
var slackBots = {};

function initializeSlackBot(apiToken, instanceID){
  var slackBot = new slackAPI({
    'token': apiToken,
    'logging': false,
    'autoReconnect': true
  });

  var slackInstance = slackBots[instanceID] = {}
  slackInstance.instanceID = instanceID;
  slackInstance.active = true;
  slackInstance.messages = [];
  slackBots[instanceID].bot = slackBot;

  function sendSlackMessage(){
    var remove = true;
    if(slackInstance.messages.length != 0){
      var message = slackInstance.messages[0];
      try{
        if(message.recipient !== "narrator"){
          slackBot.sendPM(message.recipient, message.message);
        }else{
          if(!slackInstance.channelID){
            findChannel(slackInstance);
            remove = false;
          }else{
            slackBot.sendMsg(slackInstance.channelID, message.message);  
          }
          
        }   
        if(remove)
          slackInstance.messages.splice(0, 1); //removes the first element     
      }catch(err){
        console.log(err);
      }
    }else{
      
    }

    if(slackInstance.active)
      setTimeout(sendSlackMessage, 1000);
  }
  sendSlackMessage();

  collectUsers(apiToken, instanceID);
  findChannel(slackInstance);

  slackBot.on('message', function (data) {
    if (typeof data.text === 'undefined')
      return;

    var message = data.text;
    var channel = data.channel;

    if(data.subtype){ //someone just left or got added
      if(data.subtype === 'group_join'){
        addSlackUser();
      }else if(data.subtype){
        removeSlackUser();
      }
      return;
    }

    if(data.user === null)
      return;
    var user = slackBot.getUser(data.user);
    if(user.is_bot)
      return;
    if(!slackInstance.channelID)
      findChannel(slackInstance);
    if(slackInstance.channelID === data.channel){
      if(message.length === 0)
        return;
      if(message[0] !== '-')
        return;
      message = message.substring(1);
    }
    var o = {};
    o.slack = true;
    o.message = 'slackUserInput';
    o.slackMessage = message;
    o.from = slackInstance.tables.userToFirstName[user.name];
    o.instanceID = instanceID;
    toJava(o);
  });
}

function findChannel(slackInstance){
  var slackBot = slackInstance.bot;
  var groups = slackBot.slackData.groups;
  if(groups === undefined){
    "groups is still undefined";
    return;
  }
  for(var i = 0; i < groups.length; i++){
    if(groups[i].name === MAFIA_CHANNEL_NAME){
      slackInstance.channelID = groups[i].id;
      return;
    }
  }
  console.log("didn't find the channel");
}



function collectUsers(apiToken, instanceID){
  var slackBotObject = slackBots[instanceID];
  var slackTable = slackBotObject.tables = {}
  slackTable.firstNameToUser = {};
  slackTable.userToFirstName = {};
  
  var slackRequire = require('slack-node')
  slack = new slackRequire(apiToken);

  slack.api('groups.list', {
  }, function(err, response){
    for(var i = 0; i < response.groups.length; i++){
      if(response.groups[i].name === MAFIA_CHANNEL_NAME)
        getUsers(response.groups[i].id);
    }
  });

  var num = 0;

  function getUsers(id){
    slack.api('groups.info', {
      channel: id
    },function(err, response){
      var members = response.group.members;
      num = members.length;
      for(var i = 0; i < members.length; i++){
        getUserName(members[i]);
      }
    });
  }

  users = [];

  function getUserName(id){
    slack.api('users.info', {
      user: id
    },function(err, response){
      var user = response.user;
      if(user.is_bot)
        return;
      var name;
      if(user.profile['first_name'])
        name = user.profile['first_name'];
      else
        name = user.name;
      users.push(name);

      slackTable.firstNameToUser[name] = user.name;
      slackTable.userToFirstName[user.name] = name;

      addSlackUser(name, instanceID);

    });
  }
}

function addSlackUser(name, instanceID){
  var o = {};
  o.slack = true;
  o.instanceID = instanceID;
  o.message = 'addPlayer';
  o.slackName = name;
  toJava(o);
}

function queueSlackMessage(jo){
  var slackBotObject = slackBots[jo.gameID];
  if(!slackBotObject)
    return;
  var slackBot = slackBotObject.bot;

  var message = jo.slackMessage;
  var name;
  if(jo.name !== 'narrator'){
    name = slackBotObject.tables.firstNameToUser[jo.name]; 
  }else{
    name = 'narrator';
  }

  var o = {};
  o.recipient = name;
  o.message = message;
  slackBotObject.messages.push(o);
  console.log(o);
}

var junk = "";

function receiver(data) {
  data = data.toString();
  data = data.replace('\r', '');
  junk = junk + data;
  var index, completed, message;

  while(-1 != (index = junk.indexOf("$$"))){
    completed = junk.substring(0, index);
    junk = junk.substring(index + 2, junk.length);
    
    //console.log('java -> heroku : ' + completed);

    var jo = JSON.parse(completed);
    if(jo.server)
      handle_java_event(jo);
    else
      web_send(jo.name, completed);
    
  }
}


function connectClient(){
  var net = require('net');
  var client_o = new net.Socket();
  client_o.connect(1337, '127.0.0.1', function() {
    console.log('Connected');
    pipe = client_o;
  });

  client_o.on('data', function(data) {
    receiver(data);
  });

  client_o.on('close', function() {
    console.log('Connection closed');
  });
}

function runJava(){
  console.log('Starting java process...');
  setTimeout(connectClient, 3000);
  var spawn = require('child_process').spawn
  java = spawn("java", ["nnode/NodeSwitch"], {cwd: 'src'});
  java.stdout.on('data', function(data){
    //console.log('(java) : ' + data);
    if(READ_JAVA_OUTPUT)
      console.log(data.toString());
  });
  java.stderr.on('data', function(data){
    console.log('(javaErr) : ' + data);
  });
  java.on('exit', function (code) {
    console.log('child process exited with code ' + code);
});
}



compile_file(['nnode/NodeSwitch.java'], 0, runJava);