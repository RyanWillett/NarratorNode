var apiToken = "xoxb-64623018487-UE5zryjpBgLwFXFtJznvcJYC";
const READ_JAVA_OUTPUT = true;
const LOG_CLIENT_OUTPUT = false;

var slackAPI = require('slackbotapi');
var exec = require('child_process').exec;

var slackbot = new slackAPI({
    'token': apiToken,
    'logging': true,
    'autoReconnect': true
});

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

slackbot.on('message', function (data) {
    // If no text, return.
    if (typeof data.text === 'undefined')
    	return;

    console.log(data);
    var message = data.text;
    // If someone says `cake!!` respond to their message with 'user OOH, CAKE!! :cake:'
    if (data.text === 'cake!!') 
    	slackbot.sendMsg(data.channel, 
    		'@' + slackbot.getUser(data.user).name + ' OOH, CAKE!! :cake:');

    console.log(data);
    console.log(data.channel);

    // If the first character starts with %, you can change this to your own prefix of course.
    if (data.text.charAt(0) === '%') {
        // Split the command and it's arguments into an array
        var command = data.text.substring(1).split(' ');

        // If command[2] is not undefined, use command[1] to have all arguments in command[1]
        if (typeof command[2] !== 'undefined') {
            for (var i = 2; i < command.length; i++) {
                command[1] = command[1] + ' ' + command[i];
            }
        }

        // Switch to check which command has been requested.
        switch (command[0].toLowerCase()) {
            // If hello
            case 'hello':
                // Send message
                slackbot.sendMsg(data.channel, 'Oh, hello @' + slack.getUser(data.user).name + ' !');
                break;

            case 'say':
                var say = data.text.split('%say ');
                slackbot.sendMsg(data.channel, say[1]);
                break;
        }
    }
});



