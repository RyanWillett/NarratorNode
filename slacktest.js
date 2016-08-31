var exec = require('child_process').exec;
var slackAPI = require('slackbotapi');

testKeys = ['xoxb-70808420675-ZWcsAD63IDt582BxbNodyxeT',
'xoxb-70856180806-VEmaRdKvv60gmyTOmud3WSKL',
'xoxb-70873689746-zTIjEjWBhyurGFF2do14pxYC',
'xoxb-70865938311-Au81lXjp3oEya4gGYn1EAPUF',
'xoxb-70853971748-brwyoOGBkgjt8Re6L6aNueHa',
'xoxb-70867972081-VRmXg9kdvgekEFQXFmHptQE4',
'xoxb-70856588790-egOLsO6nwh0KOayRQNJxjFBn',
'xoxb-70864135045-1WWmwJ8Wyyz2M9ByXB1YkvnU']
var apiToken = "xoxb-66457395024-tij2OKtM6rzPWrUuMGr83SB6"; //test one
const READ_JAVA_OUTPUT = true;
const LOG_CLIENT_OUTPUT = false;

bots = {};


var slackbot;
function initializeBots(){
	var key;
	for(var i = 0; i < testKeys.length; i++){
		key = testKeys[i];
		slackbot = new slackAPI({
	    'token': key,
	    'logging': false,
	    'autoReconnect': true
		});
		bots['test-bot' + i.toString()] = slackbot;
	}
	slackbot = new slackAPI({
	    'token': apiToken,
	    'logging': true,
	    'autoReconnect': true
	});
}

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

var channelID = null;

function findChannel(){
	var groups = slackbot.slackData.groups;
	for(var i = 0; i < groups.length; i++){
		if(groups[i].name === 'narrator_mafia'){
			channelID = groups[i].id;
			break;
		}
	}
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
    var name = jo.name;
    var message = jo.message;

	if (channelID === null)
		findChannel();
    slackbot.sendMsg(channelID, message);    	
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


var junk = "";

function runJava(){
  console.log('running java prog');
  setTimeout(connectClient, 3000);
  var spawn = require('child_process').spawn
  var args = ["nnode/SlackTest"];
  for (var i = 0; i < testKeys.length; i++){
  	args.push(('test-bot' + i));
  }
  java = spawn("java", args, {cwd: 'src'});
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

function startNarrator(){
	compile_file(['nnode/SlackTest.java'], 0, runJava);
}

initializeBots();
startNarrator();