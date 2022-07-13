# Authors
- Fadi Zaher - 205792526
- Niv Sahar - 205808272

# Github users:
- nivsahar2
- Fadi-Zaher


# Detailed work list
Client Side:
 - Niv : 10
 - Fadi : 13

Server Side: 
- Fadi : 26
- Niv : 29

Git README and side work:
- Fadi : 3
- Niv : 2

# Intro
Assignment 2 in distributed systems course - 4th year in software engineering.

# Project architecture

We took the project we made from assignment 1 and made some changes to the projects architecture.
Link for Assignment 1: "https://github.com/nivsahar2/5782-ds-ass1-fadi-zaher-niv-sahar"

Server:

    - Each server listen on 2 ports: 5000(xxxx) and 5001(yyyy) for example.
    - xxxx will listen for clients and will create a client handler process that will handle his requirments.
    - yyyy will listen for server and will create a server handler process that will handle servers requirments (offer, sync and etc)
    - The first server that will be UP and clients can connect to, will be the root server.
    - Every time a new server will be started all the servers that is currently UP will create a server handler that will work for this server.
    - Operations like UPLOAD, LOCK, UNLOCK that came from the client will be execute from the client handler of the root server to the server handler of the neighbors servers.
    - When server is started it will try to connect to neighbors server from the conf file, if they UP the server will talk with their server handler 
      and will send them  StartSync request to do the StartSync progress as been asked in the assignment.

Client:

    - Will execute an operation on the first server he succeeded to talk with.

![image](https://user-images.githubusercontent.com/71365529/147513272-66299fa0-be5d-4dc2-8855-dc134fca3c5a.png)

Here is an image that shows what happend when Server 1 is UP (root) and then Server 2 started and client 1 reconnect to server 2. 

**There is no server handler yet in Server 2, but when Client handler 1 will need and try to talk to Server2 it will be created.


# Project Description 
This project is about multi-clients to multi-servers, where multiple clients can access multiple servers and do certan file operations:
- Upload files ( multiple servers)
- Download files (from single server)
- Lock files ( multiple servers)
- Unlock files ( multiple servers)
- Get version of file(from single or multiple servers)
- Get list of files in specific server

*in every operation the client will send the identity of the client that will be his IP that will be unique(we use different cp) and concat of random number

The client does its operations via Client GUI (usage will be explained later)

# Client Configuration File (unchanged)
Client configuration file contains the server name, the servers ipv4(should be changed to the IP that choose when the server start listen) address and the servers ports
 
```text 
HOST_server1 10.0.201.1:5000 
HOST_server2 10.0.201.2:6000 
HOST_server3 10.0.201.3:7000
```

The configuration file will send like this:

```text 
HOST_server1 x.x.x.x:w
HOST_server2 y.y.y.y:d
HOST_server3 z.z.z.z:p
```
You will need to put the ip that the server is listening on

    - if this different ip - you can use the same port.
    - if this is the same ip like 127.0.0.1 - you need to change the port of every server.
     

# Client Graphical User Interface
The client GUI Contains -

**2 ListView:**
1) contains the servers list
2) (optional) contains the files list of picked server

**1 Combo box:**
1) contains the operations that client can do

**2 Text view:**
1) will contain the picked file from local disk
2) will show ouputs to the client

**5 Buttons:**
1) Quit 
2) Execute operation
3) Reconnect to server
4) Choose file (opens file dialog)
5) Clear list

![image](https://user-images.githubusercontent.com/73060580/141838365-8a4ed664-24b4-4d70-878e-022432b59099.png) 
 

# Client GUI usage:
As mentioned before the are bunch of operations that our program support, it will be catigorized now and explained.
(better to run the server first and then start the client gui - but not must)

- When the Client program starts, its tries to conect to all the server that listenning and display output to the user ehich server it connected to.

### **Normal operations:**

1. Quit - to quit the program click on the Quit button

2. Choose file - click on Choose File button and a file dialog will be opened so you can choose the file.

3. Choosing the operation - click on the combo box arrow, the operations options will be show, as you can choose any operation you want to execute by clicking on its name one      
   click.

4. Execute operation - after choosing the operation you want to execute, click on **Execute** button to execute.

5. Reconnect to a server (if you not connected to spisific server)

### **Combo-box operations**

1. Upload - to upload to all servers 

2. Getlist - to get file list that a spisific server contains, click on the server you want its files (on the servers listview) and the execute Getlist command

3. Download - to download file from a single server when you chose the file from the files list (files list will be shown after executing the Getlist operation)

4. Lock - locks single file on all servers, execute this operation if you dont want others to edit the specific file that you are editing

5. Unlock - unlock the file in all servers, that you already locked and you finished editing it (by editing - means uploading new version of it)

9. GetVersion - to get the version of the specific picked file (timestamp and using SHA-2 with algorithm sha-256)

10. GetVersionFromAllServers - to get versions of specific file from all connected servers


 # Server 
 Once the program is started it will lookup for active neighbor servers and if there is any, the new server content will get synchronized with the active one's. In short talk, every new server will get updated from the active servers. 
 
 # Server Configuration File
 Server configuration file contains:
- client2server_port -> the port that the server listen to its comming connections from clients side
- server2server-port -> the port that the server listens to it comming connections from the neighbor servers side
- neighbors -> the neighbors servers
- root_dir -> the main storage directory of the server
- temp_dir -> the temporary storage directory of the server
 

```text
client2server_port=5000
server2server_port=5001

neighbors=HOST_server2:127.0.0.1:6001,HOST_server3:127.0.0.1:7001

root_dir=./files/
temp_dir=./temp/

```

The configuration file will be send like that:

```text
client2server_port=x
server2server_port=x+1

neighbors=xxx:000.000.000.000:y,zzz:000.000.000.000:w

root_dir=./files/
temp_dir=./temp/
```
 
 # Server Terminal Usage
 The server terminal offers to the user to pick the ipv4 he wants the server to listen to.
 ![image](https://user-images.githubusercontent.com/73060580/141839002-d99d7545-d442-4a18-8601-8343d91ebe96.png)

 Also the server asks the user if he wants the server to pause listening by typing "STOP".
 After pauseing the listening, the user can resume by typing "y" it or to end it totaly by typing "n".
 
 ![image](https://user-images.githubusercontent.com/73060580/141839265-9e67f64b-a7d2-4c58-8f32-ec00b2a1ec69.png)
 
 Also there is an option to change the port the server might listen to by the user using the terminal,
 to do so - STOP -> y -> type the port
 
 ![image](https://user-images.githubusercontent.com/73060580/143771395-9236e31b-0749-43ae-ace3-64159574c597.png)

*Server operations to neightbors*
The root server will send a operations to neighbors server, suah as: OFFER, LOCK, UNLOCK.
    
    - OFFER - will offer the file currently uploaded to server in format - OFFER digest datetime filename.
      and wait for server request to download(DOWNLOAD), if any upload failed -> return ERROR to client.
    - LOCK - send lock to all neighbors in format - LOCK filename.
      and wait for OK respond, if any neighbor failed to lock all procedure stop and send ERROR to client.    
    - UNLOCK - send unlock to all neighbors, and dont need to check if it success or failed.
    
 # Log file
  Log file contains all the operations that the server doing, errors occurs, succeeded/failed operations.
  every line will contains the level of the issue(INFO,WARNING,SERVE), datetime and info about the issue.
  the file created in the root project path
  Location = ./ or "Project_Location"/Server/


 # Thread Safety Stragety
 
We have used the ConcurrentHashMap thread safety data structure, in every needed operation in the Client Handler we do it on the ConcurrentMap to increase the throughput of our concurrent code by allowing concurrent read/writes on the table without locking the entire table, its supported by internal locking.

for another operations that is not related to the ConcurrentMap (and not atomic) we wrapped it with the syncronized keyword on the ConcurrentHashMap before the checks on the files indexer, for making the access to the file (override/upload) forbidden for more then one client at a time (mutual exclusion).


# Instructions for compiling the source code for both the client and server programs
  
  Both programs, the Client and the Server were compiled the same way, we chose to do so via the Intellij dev env.
  The steps to do so in Intellij:
  
  1. File -> Project structure -> Project settings -> Artifacts -> Green (+) -> JAR -> From module with dependencies ->
  -> pick the main class and jar location -> OK -> add the dependencies -> OK
 
  2. Build -> Build Artifacts -> wait untill build is done
 
  Those steps were described in the link:   https://stackoverflow.com/questions/1082580/how-to-build-jars-from-intellij-properly
  
  
# Instructions for running the client and server programs
   
  Both can be running as jar executable, there is directort "jar out files" and inside:
     
    - Server: contains the server configuration file, Server jar file, Batch file (run-me.bat) for running the server(java -jar     
      Server.jar).
    - Client: contains servers list file, Client jar file.
   
  Run the programs:
  
    - Server: Click on the batch script (run-me.bat) that will execute the Server.jar file that will open a CLI for choosing an IP, and to manage listenning Start and 
      Stop process.
    - Client: Click on the Jar file to Open the Client GUI, for managed the operations from the user to the client programs that will send to the server.
        
# Additional Information

 Client:
      
      - There is some actions that will get an message box display on the GUI, This used for some alert or errors that not requested in the Assignments.
        Example: if you execute an upload command, but dont choose a file. (You will get a messsage box in accorance)
 Server:
       
      - We added a client id, to every client that connected to the server and making operations on it.
        we do this for our understanding, and we added this to the output in the log file (its additonal information - and does not offend to the Messages you requsted in the           log file) 

 
