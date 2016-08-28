var exec = require('child_process').exec;
var fs = require('fs');
const NARRATOR = "Narrator";
const READ_JAVA_OUTPUT = true;
const LOG_CLIENT_OUTPUT = false;

/*firebase.initializeApp({
  serviceAccount: "Narrator-1c6d40c23a30.json",
  databaseURL: "https://narrator-119be.firebaseio.com"
});*/

var gcm = require('node-gcm');
var androidNotifier = new gcm.Sender(require('./keys').androidNotifToken);

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
        'title':'The title',         //this is the Title of the notification
        'message':'My custom message' //This is the subtext of the notification
      }
  });
     
  androidNotifier.send(message, { registrationTokens: recipients }, function (err, response) {
      if(err) 
        console.error(err);
      else if(response.failure !=== 0)
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

console.log("http server listening on %d", port);

var wss = new WebSocketServer({server: server});
console.log("websocket server created");


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
      }
      if (!(o.name in connections_mapping)){
        //o.message = 'greeting';  message should already be greeting
        pipe_write(message);
        connections_mapping[o.name] = ws;
        return;
      }
      if(connections_mapping[o.name] != ws){
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
        console.log("websocket connection with " + i + " close");
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
  console.log('running java prog');
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
