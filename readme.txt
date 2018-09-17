 TeamCity server-side plugin

 This is CallFire team city gerrit plugin that posts stuff into gerrit

 1. Implement
 Put your implementing classes to "<artifactId>-server" module. Do not forget to update spring context file in 'main/resources/META-INF'. See TeamCity documentation for details.

 2. Build
 Issue 'mvn package' command from the root project to build your plugin. Resulting package <artifactId>.zip will be placed in 'target' directory. 
 
 3. Install
 To install the plugin, put zip archive to 'plugins' dir under TeamCity data directory and restart the server.
