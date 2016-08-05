# Node.js Narrator Server

Node backbone for running the Narrator on the web.  How it works:  Node creates a java process from 'nnode.NodeSwitch' and then sets up a socket connection to pass info back and forth.  Node then just sits on top, receiving requests from the browser, and passes it to the Java process and passes back anything that the Java process comes up, back to the browser.

The 'nnode.NodeSwitch' is in charge of converting Narrator info, into JSONObjects that the browser can interpret.
```
