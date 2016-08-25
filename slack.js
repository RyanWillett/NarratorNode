
//helpful link : https://github.com/xBytez/slackbotapi/blob/master/lib/index.js

var apiToken = require('./keys').myToken;
//var apiToken = require('./keys').pepbandToken;
//var apiToken = require('./keys').sdscToken;

var MAFIA_CHANNEL_NAME = 'narrator_mafia';
var STARTERS = ['c1mckay', 'michaelcmkay', 'euromkay']

const READ_JAVA_OUTPUT = true;
const LOG_CLIENT_OUTPUT = false;

var slackAPI = require('slackbotapi');
var exec = require('child_process').exec;

var slackbot = new slackAPI({
    'token': apiToken,
    'logging': false,
    'autoReconnect': true
});

function compile_file(files, i, final_func){
  var comp_name;
  if (i >= files.length){
    final_func();
    return;
  }

  comp_name = files[i];
  console.log('compiling ' + comp_name);
  var command = "javac  -sourcepath src "; 
  //if (process.env.PORT === undefined) 
    command += "src/";
  exec(command + comp_name, {}, function(error, stdout, stderr){
    if (error) {
      console.log(error);
      return;
    }
    compile_file(files, i + 1, final_func);
  });
}

var pipe = null;
var junk = "";

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
  console.log('running java prog');
  setTimeout(connectClient, 3000);
  var spawn = require('child_process').spawn
  java = spawn("java", ["nnode/SlackSwitch"], {cwd: 'src'});
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
    var name = firstNameToUser[jo.name];
    var message = jo.message;

    if (name !== "narrator")
      slackbot.sendPM(name, message);
    else if(channelID !== null)
      slackbot.sendMsg(channelID, message);
    
  }
}

var channelID = null;

function findChannel(){
  var groups = slackbot.slackData.groups;
  for(var i = 0; i < groups.length; i++){
    if(groups[i].name === MAFIA_CHANNEL_NAME){
      channelID = groups[i].id;
      break;
    }
  }
}

compile_file(['nnode/SlackSwitch.java'], 0, runJava);

slackbot.on('message', function (data) {
    if (typeof data.text === 'undefined')
    	return;

    var message = data.text;
    var channel = data.channel;
    if(data.user === null)
      return;
    var user = slackbot.getUser(data.user);
    if(message.toLowerCase() === 'start' && STARTERS.indexOf(user.name) != -1){
      collectUsers();
      slackbot.sendMsg(channel, 'starting');
      return;
    }
    if(!started)
      return;
    if(channelID === null)
      findChannel();
    if(channelID === data.channel){
      if(message.length === 0)
        return;
      if(message[0] !== '-')
        return;
      message = message.substring(1);
    }
    var o = {};
    o.from = userToFirstName[user.name];
    o.message = message;
    toJava(o);
});

var firstNameToUser = null;
var userToFirstName = null;
function collectUsers(){
  firstNameToUser = {};
  userToFirstName = {};
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
      var name;
      if(user.profile['first_name'])
        name = user.profile['first_name'];
      else
        name = user.name;
      users.push(name);

      firstNameToUser[name] = user.name;
      userToFirstName[user.name] = name;
      if(users.length == num){
        startGame(users);
      }
    });
  }
}

var started = false;
function startGame(users){
  started = true;
  var o = {}
  o.message = 'start';
  o.players = users;
  o.from = 'narrator';
  toJava(o);
}


function toJava(o){
  if(pipe === null || pipe === undefined)
    return;

  pipe.write(JSON.stringify(o) + "\n");
}

