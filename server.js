var exec = require('child_process').exec;
var fs = require('fs');
const NARRATOR = "Narrator";

String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

function walk(dir) {
  var results = [];
  var list = fs.readdirSync(dir);
  list.forEach(function(file) {
    file = require("path").join(dir,file);
    var stat = fs.statSync(file);
    if (stat && stat.isDirectory())
      results = results.concat(walk(file));
    else{
      file = file.toString();
      if (file.endsWith(".java")){
        file = file.replace('src/', '');
        results.push(file);
      }
    }
  });
  return results;
}

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
  /*if (classExists(comp_name)){
    console.log('already compiled: ' + comp_name);
    compile_file(files, i + 1, final_func);
    return;
  }*/
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

var toCompileWithClass = walk('src');



var WebSocketServer = require("ws").Server;
var http = require("http");
var express = require("express");
var app = express();
var port = process.env.PORT || 5000;

app.use(express.static(__dirname + "/public"))

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
  //console.log('sending web message to ' + name + " : " + message);
  var c = connections_mapping[name];
  //console.log(connections_mapping.length);
  if (c!== undefined)
    c.send(message);
  //console.log('finished sending to ' + name);
}

var pipe = null;

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
  return JSON.stringify(o);
}

connections_mapping = {};

wss.on("connection", function(ws) {
  var o = toJString(NARRATOR, "You are now connected!");
  ws.send(o);

  console.log("websocket connection open");

  ws.on('message', function(message){
    //console.log('web -> heroku : ' + message);
    try{
      o = JSON.parse(message);
      var prevMessage = o.message;

      
      if (!(o.email in connections_mapping)){
        o.message = 'addplayer';
        o.server = true;     
        pipe_write(JSON.stringify(o));
      }
      else if(connections_mapping[o.email] != ws){
        connections_mapping[o.email].close();
      }
      connections_mapping[o.email] = ws;

      if(prevMessage.length !== 0){
        var submitted = pipe_write(message);
        if (!submitted){
          web_send(o.email, message);
        //  web_send_all(message);
        }
      }
    }catch(f){
      console.log("message that caused error : " + message);
      console.log(f);
    }
    
  });

  ws.on("close", function() {
    var i;
    for (i in connections_mapping){
      //todo set inactive
    	if (connections_mapping[i] === ws){
        console.log("websocket connection with " + i + " close");
        delete connections_mapping[i];
        break;
      }
    }
  });
})


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
      web_send(jo.email, completed);
    
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
  setTimeout(connectClient, 4000);
  var spawn = require('child_process').spawn
  java = spawn("java", ["nnode/NodeController"], {cwd: 'src'});
  java.stdout.on('data', function(data){
    //console.log('(java) : ' + data);
    console.log(data.toString());
  });
  java.stderr.on('data', function(data){
    console.log('(javaErr) : ' + data);
  });
  java.on('exit', function (code) {
    console.log('child process exited with code ' + code);
});
}



compile_file(['nnode/NodeController.java'], 0, runJava);
