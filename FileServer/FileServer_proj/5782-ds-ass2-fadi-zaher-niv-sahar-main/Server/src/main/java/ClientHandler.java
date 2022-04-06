
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @authors: Fadi Zaher 205792526 Niv Sahar 205808272
 */
public class ClientHandler extends Thread {

    // variables
    boolean connection = true;
    ArrayList<FileRep> my_locked_files = new ArrayList<>();
    BufferedReader in = null;
    PrintWriter out = null;
    InetSocketAddress socketAddress = null;
    Socket clientSocket;
    String client_id;

    
    // constructor
    ClientHandler(Socket clientSession) {

        this.clientSocket = clientSession;
        //client_id = getNewID();
        // write to the log file

        socketAddress = (InetSocketAddress) clientSocket.getRemoteSocketAddress();

        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Client : " + client_id + ",  CONNECTED");

    }

    @Override
    public void run() {
        try {
            //prevent log output on system output screen
            Constants.logger.setUseParentHandlers(false);

            //enter the loop only while there is stable connection between client-server
            while (connection) {
                try {
                    //printwriter and buffered reader to receive and send data
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    System.err.println("Waiting for inpit - client handler");
                    //receivnig the command from the client
                    String command = in.readLine();
                    System.err.println("command is : " + command);

                    //check that the command is not null
                    if (!Objects.isNull(command)) {

                        //tokenizing the full command and inserting it to an array
                        ArrayList<String> client_request = tokenize(command);
                        // write to the log file

                        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "The Client: " + client_id + ", sent '" + command + "' command");

                        String receivedCommand, receivedFileName = null;

                        //the command - LOCK,UNLOCK etc..
                        receivedCommand = client_request.get(0);
                        System.err.println(receivedCommand);
                        System.err.println("\n request: " + client_request);

                        //if it is on-file command (like lock filename.txt)
                        if (client_request.size() > 2) {
                            //file name
                            client_id = client_request.get(2);
                            receivedFileName = client_request.get(1);
                            System.err.println("file :" + receivedFileName);
                        } else {
                            client_id = client_request.get(1);
                        }
                        System.err.println("sender is : " + client_id);

                        //switch cases to check what command was sent by the client
                        switch (receivedCommand.toUpperCase()) {
                            case "LOCK":
                                lock(receivedCommand, receivedFileName);
                                break;
                            case "UNLOCK":
                                unlock(receivedCommand, receivedFileName);
                                break;
                            case "DOWNLOAD":
                                download(receivedFileName);
                                break;
                            case "GETVERSION":
                                getVersion(receivedFileName);
                                break;
                            case "UPLOAD":
                                upload(receivedFileName);
                                break;
                            case "GETLIST":
                                getList();
                                break;
                            case "Quit":
                                quit();
                                break;
                            //....
                            default:
                                // write to the log file
                                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " unknown commad was received" + receivedCommand);
                                break;
                        }
                    } else {
                        // write to the log file
                        Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Empty command was received");
                        break;
                    }

                } catch (SocketException se) {
                    //setting the connection to false so the while loop stop
                    Constants.logger.severe("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Socket ERROR has occured, Client " + client_id + " disconnected");
                    connection = false;
                }
            }
        } catch (IOException e) {
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "IOException ERROR has occured ");
        }
    }

    /**
     *
     * function the occurs when client closed.
     * all the client handlers that created for this client will be deleted.
     *
     */

    private void quit() {

        synchronized (my_locked_files) {
            //unlocking the files this client locked and forgot to unlock before exiting
            unlock_my_files();
            //interrupting this thread
            this.interrupt();
        }
        // write to the log file
        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Quit command received by client : " + client_id);
        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Connection stopped with client : " + client_id);
    }

    /**
     * function that handle the lock operaton that came from the client
     *
     */
    private void lock(String command, String receivedFileName) {
        // syncronized block
        synchronized (Constants.indexing_map) {

            System.err.println("get command: " + command);
            ArrayList<String> servers_Respond = new ArrayList<String>();

            // true -> if the hash contains the file
            if (Constants.indexing_map.containsKey(receivedFileName)) {
                System.err.println("lock Contains file");
                // object that represents the file from the indexing map
                FileRep fr = (FileRep) Constants.indexing_map.get(receivedFileName);
                System.err.println(fr.getLocked_by());
                System.err.println(fr.isLock_status());
                System.err.println(fr.getFile_name());
                // send lock for all neigbors, and wait for responds
                servers_Respond = LockServers(command, receivedFileName);
                System.err.println("server responses: " + servers_Respond);
                // check if the file locked or one of the neighbors repeated with ERROR - send ERROR to client
                // can't lock locked file - so error will be sent to client
                if (fr.isLock_status() || servers_Respond.contains("ERROR")) {
                    out.println("ERROR");
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: Client : " + client_id + ", failed in locking the file -> " + receivedFileName);
                } else {
                    // if can lock the file
                    System.err.println("can lock");
                    //lock the file
                    fr.setLock_status(true);
                    fr.setLocked_by(client_id);
                    fr.setFile_version(fr.getFile_version());
                    // send OK to client
                    out.println("OK");
                    //adding this file to this client locked files list
                    my_locked_files.add(fr);
                    System.err.println("added to my lockes");
                    // write to the log file
                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Client : " + client_id + ", locked the file -> " + receivedFileName);
                }
            } else {
                // if not exist in file indexer
                out.println("ERROR");
                // write to the log file
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: Client : " + client_id + ", tried to lock un-found file");
            }
        }
    }

    /**
     * function that handle the unlock operaton that came from the client
     *
     */

    private void unlock(String command, String receivedFileName) {

        // syncrinized block
        synchronized (Constants.indexing_map) {
            System.err.println("UNLOCK occurs");
            ArrayList<String> servers_Respond = new ArrayList<String>();

            //true -> if the hash contains the file
            if (Constants.indexing_map.containsKey(receivedFileName)) {
                System.err.println("Contains file in unlock");
                //object that represents the file from the indexing map
                FileRep fr = (FileRep) Constants.indexing_map.get(receivedFileName);

                //send unlock to neighbors and wait for responses
                unLockServers(command, receivedFileName);
                //true -> if the client is the one who locked the file
                if (fr.getLocked_by().equals(client_id)) {

                    //setting lock status to false
                    fr.setLock_status(false);
                    fr.setLocked_by("");
                    fr.setFile_version(fr.getFile_version());

                    System.err.println("locked by: " + fr.getLocked_by());
                    System.err.println("lock status: " + fr.isLock_status());
                    System.err.println("file name: " + fr.getFile_name());

                    // removing this file from this client locked files list
                    my_locked_files.remove(fr);

                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Client : " + client_id + ", lock File ----> " + receivedFileName);
                    // send OK to client.
                    out.println("OK");
                } else if (!my_locked_files.contains(fr)) {
                    // send ERROR
                    out.println("ERROR");
                    // write to the log file
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: Client : " + client_id + ", failed to unlock File ----> " + receivedFileName);
                }
            } else {
                // send ERROR
                out.println("ERROR");
                // write to the log file
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: Client : " + client_id + ", tried to unlock un-found file");
            }
        }
    }

    /**
     * function that handle the download operaton that came from the client
     *
     */

    private void download(String receivedFileName) {

        // syncronized block
        synchronized (Constants.indexing_map) {
            // check if the file exist in file indexer
            if (Constants.indexing_map.containsKey(receivedFileName)) {

                try {

                    File current_file = new File(Constants.ROOT_DIR_PATH + receivedFileName);
                    // use Base64 operation
                    String originalInput = encodeFileToBase64(current_file);
                    // send the string file to the client
                    out.println(originalInput);
                    //sending OK message that command was received
                    out.println("OK");

                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Client: " + client_id + ", File -> " + receivedFileName + " downloaded");

                } catch (Exception e) {
                    out.println("ERROR");
                    // write to the log file
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: Client " + client_id + ", Failed download the file " + receivedFileName);
                }

            } else {
                // send OK to client
                out.println("ERROR");
                // write to the log file
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: " + receivedFileName + "Not exists storage, Downloading has failed");
            }
        }
    }

    /**
     * function that handle the lock operaton that came from the client
     *
     */

    private void upload(String receivedFileName) throws IOException {

        synchronized (Constants.indexing_map) {

            if (ProcessServer.checkFileName(receivedFileName)) {
                //true --> if the file exist
                if (Constants.indexing_map.containsKey(receivedFileName)) {

                    //true --> if the file is locked by me
                    if (my_locked_files.contains(Constants.indexing_map.get(receivedFileName))) {
                        byte[] decodedBytes = null;
                        try {
                            
                            // download file to server with Base64 operation the file will be at temporary directory
                            FileOutputStream fileOutputStream = new FileOutputStream(Constants.TEMP_DIR_PATH + receivedFileName, false);

                            decodedBytes = Base64.getDecoder().decode(in.readLine().getBytes());

                            fileOutputStream.write(decodedBytes, 0, decodedBytes.length);
                            fileOutputStream.close();
                            
                            // write to log
                            Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Client: " + client_id + " uploaded file to temp directory: " + receivedFileName);

                            System.err.println("finished upload");
                            
                            
                            FileRep temp_fr = new FileRep(Constants.TEMP_DIR_PATH, receivedFileName);
                            // server call offer function to offer file to neighnors
                            ArrayList<String> response = offerFileToNeighbors(temp_fr, receivedFileName);

                            // if one or more of the neighnors not send ERROR -> get in if
                            if (!response.contains("ERROR")) {

                                System.err.println("---------------was here----------------");
                                System.err.println("UPLOAD client handler 3.1");
                                File src = new File(Constants.TEMP_DIR_PATH + receivedFileName);
                                System.err.println("UPLOAD client handler 3.2");
                                File dest = new File(Constants.ROOT_DIR_PATH + receivedFileName);
                                System.err.println("UPLOAD client handler 3.3");
                                // copy the file from temp dir to the root dir
                                FileUtils.copyFile(src, dest, false);
                                System.err.println("UPLOAD client handler 3.5");
                                // update the current timestamp and version
                                FileRep fr = (FileRep) Constants.indexing_map.get(receivedFileName);
                                //setting timestamp to the file -> time stamp from the temp_fr
                                fr.setLast_change_time(temp_fr.getLast_change_time());
                                fr.setVersion();
                                // send OK to the client
                                out.println("OK");
                                // write to the log file
                                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Server reponse with OK message after success to upload file: " + receivedFileName);
                                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Client: " + client_id + " uploaded file  : " + receivedFileName);

                            } else {
                                // write ERROR to client
                                out.print("ERROR");
                            }
                            // write to the log file
                            Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Client: " + client_id + ", Uploaded, File -> " + receivedFileName + " overwriten");

                        // delete the files are exist in the temporary directory    
                        try {
                            FileUtils.cleanDirectory(new File(Constants.TEMP_DIR_PATH.substring(0, Constants.TEMP_DIR_PATH.length() - 1)));
                        } catch (Exception e) {
                            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: File -> " + receivedFileName + " Cannot be deleted from temp directory ");
                        }
                        
                        
                        } catch (IOException e) {
                            if (decodedBytes.length == 0)
                                in.readLine();
                                // added can be checked
                                out.println("ERROR");
                            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: Client: " + client_id + ", File -> " + receivedFileName + " upload failed ");
                        }

                    } //the is not locked by me
                    else {
                        // if error occurs catch the file that should be read but dont do anything
                        in.readLine();
                        // return ERROR to client
                        out.println("ERROR");
                        // write to log
                        Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: File -> " + receivedFileName + " upload failed, not locked by client: " + client_id);

                    }
                } //the file doesn't exist in the server
                else { // if the file not exist in file indexer
                    try {
                        
                        // download the file to server with Base64 operation and save it to temp dir
                        FileOutputStream fileOutputStream = new FileOutputStream(Constants.TEMP_DIR_PATH + receivedFileName, false);

                        byte[] decodedBytes = Base64.getDecoder().decode(in.readLine().getBytes());
                        String decodedString = new String(decodedBytes);

                        fileOutputStream.write(decodedBytes, 0, decodedBytes.length);
                        fileOutputStream.close();
                        
                        // write to log
                        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Client: " + client_id + " uploaded file to temp directory: " + receivedFileName);
                        
                        // offer the file that in the temp dir to the neighbors servers
                        FileRep temp_fr = new FileRep(Constants.TEMP_DIR_PATH, receivedFileName);

                        ArrayList<String> response = offerFileToNeighbors(temp_fr, receivedFileName);
                        // if one or more of the neighbors respond with ERROR -> dont get in if
                        if (!response.contains("ERROR")) {
                            System.err.println("---------------was here----------------");
                            // copy the file from the temp dir to the root dir
                            FileUtils.copyFile(new File(Constants.TEMP_DIR_PATH + receivedFileName), new File(Constants.ROOT_DIR_PATH + receivedFileName), false);
                            Constants.indexing_map.put(receivedFileName, new FileRep(Constants.TEMP_DIR_PATH, receivedFileName));
                            FileRep fr = (FileRep) Constants.indexing_map.get(receivedFileName);
                            //setting new timestamp to the file
                            fr.setLast_change_time(temp_fr.getLast_change_time());

                            // write OK to client
                            out.println("OK");
                            // write to the log file
                            Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Server reponse with OK message after success to upload file: " + receivedFileName);

                            Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Client: " + client_id + " uploaded file to root directory: " + receivedFileName);
                        } else {
                            out.println("ERROR");
                        }

                        try {
                            FileUtils.cleanDirectory(new File(Constants.TEMP_DIR_PATH.substring(0, Constants.TEMP_DIR_PATH.length() - 1)));
                            System.err.println("UPLOAD client handler 4");
                        } catch (Exception e) {
                            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: File -> " + receivedFileName + " Cannot be deleted from temp directory ");
                        }

                    } catch (IOException e) {
                        // if error occurs catch the file that should be read but dont do anything
                        in.readLine();
                        // return ERROR to client
                        out.println("ERROR");
                        // write to the log file
                        Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: Client: " + client_id + ", File -> " + receivedFileName + " upload failed ");
                    }
                }
            }
        }
    }
    
    /**
    * function that handle the OFFER operaton to all the neighbors server.
    *
    */

    public ArrayList<String> offerFileToNeighbors(FileRep fr, String received_file) {

        System.err.println("was in offerFile");

        //variable that will be use from input/ouput throught sockets
        PrintWriter out = null;
        BufferedReader in = null;
        String file_name;
        DataOutputStream dos;
        FileInputStream fis = null;
        Socket server_socket;
        String command;

        // result from the server
        String result = null;
        ArrayList<String> responses = new ArrayList<>();
        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Server started to offer " + received_file + " to servers");

        // get current file name
        file_name = received_file;
        // build the command that will send to the server like has been asked in the assignment
        command = "OFFER " + fr.getFile_version() + " " + file_name;
        
        // iterate over all the neighbors sockets
        for (Map.Entry<String, Socket> entry : Constants.neighbor_sockets.entrySet()) {
            try {
                // if coonection doesnt exist - try to connect
                if (Objects.isNull(entry.getValue())) {
                    for (String neigbor : Constants.neighbors) {
                        if (neigbor.contains(entry.getKey())) {
                            entry.setValue(new Socket(ProcessServer.getHost_IP(neigbor), ProcessServer.getHost_PORT(neigbor)));
                        }
                    }
                }
                
                // offer the file that stored in the temp dir

                File file = new File(Constants.TEMP_DIR_PATH + file_name);

                // select the current socket the transfer files on.
                server_socket = entry.getValue();
                // initialize the input/output variable
                out = new PrintWriter(server_socket.getOutputStream(), true);
                fis = new FileInputStream(Constants.TEMP_DIR_PATH + file_name);
                in = new BufferedReader(new InputStreamReader(server_socket.getInputStream()));
                // send the command to the server - EXAMPLE:  UPLOAD file1.txt
                System.err.println(server_socket);
                out.println(command);
                out.flush();
                System.err.println(command);
                System.err.println("1");
                // send who is the client sender
                out.println(client_id);
                out.flush();
                System.err.println("2");
                // wait for the server msg (OK/ERROR)
                String msg = in.readLine();
                System.err.println(msg + "-------------------");

                System.err.println("3");
                // if everything good and server respone OK
                if (msg.toUpperCase().equals("DOWNLOAD")) {
                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Server: " + entry.getKey() + " respone with " + msg + " message");
                    // send file to neighbor with Base64 operation
                    String originalInput = encodeFileToBase64(file);
                    // send file size
                    out.println(originalInput);
                    System.err.println("after while");
                    //ok message
                    msg = in.readLine();
                    System.err.println("Serverhandler offer response " + msg);
                    // add to the responses array
                    responses.add(msg);
                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " server: " + entry.getKey() + " downloaded the file: " + received_file + " successfully ");
                } else if (msg.equalsIgnoreCase("ERROR")) {
                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Server: " + entry.getKey() + " respone with " + msg + " message");
                    System.err.println("OFFER received error !!!!!!!");
                    //error message
                    // add the error to the array responses
                    responses.add(msg);
                    // break after we have at least one ERROR - enough to stop the procedure
                    break;

                }

                try {
                    // close file stream
                    fis.close();
                } catch (IOException e) {
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Cannot close FileInptStream of file: " + file_name);
                }
            } catch (SocketException e) {
                // if the server not connect
                responses.add("NotConnected");
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " server : " + entry.getKey() + " is not connected");
                System.err.println("NotConnected");
                // message box error while upload a file
            } catch (Exception e) {
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: OFFER to servers has failed");
            }
            try {
                if (!Objects.isNull(fis)) {
                    fis.close();
                }
            } catch (Exception ex) {

            }
        }
        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " the responses are " + responses);
        // return the responses array
        return responses;
    }

    /**
    * function that handle the GETVERSION operaton from server of specific file
    *
    */   
    
    private void getVersion(String receivedFileName) {
        
        // syncronized block
        synchronized (Constants.indexing_map) {
            

            String file_time_stamp, file_version_code;
            
            // check if the file exist in the file indexer
            if (Constants.indexing_map.containsKey(receivedFileName)) {
                // take the file
                FileRep fr = (FileRep) Constants.indexing_map.get(receivedFileName);

                // return the client a VERSION message like has been asked in the assignment
                String res = "VERSION " + fr.getFile_version() + "|" + fr.getLocked_by();
                //sending the message to the client of VERSION keyword
                out.println(res);
                // write to the log file
                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Version of -> " + receivedFileName + " was returned to client " + client_id + " the " + res);
            } else {
                // return ERROR if file dont exist
                out.println("ERROR");
                // write to the log file
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: at returning the version of -> " + receivedFileName + "  to Client: " + client_id);
            }
        }

    }
    /**
    * function that handle the GETLIST operatons from a server.
    *
    */

    private void getList() {
        // syncronized block
        synchronized (Constants.indexing_map) {
            
            // if the server dont contains any file
            if (!Constants.indexing_map.isEmpty()) {
                //object that holds the root file
                File file = new File(Constants.ROOT_DIR_PATH);

                //listing the file content into list
                ArrayList<String> filesList = new ArrayList<>();                
                //iterating over the files on the server and adding them to the fileList
                for (String item : file.list()) {
                    if (Constants.indexing_map.containsKey(item)) {
                        filesList.add(item);
                    }
                }
                // send OK the client
                out.println("OK");
                // send the client the size of the files list
                out.println(filesList.size());

                //iterating over the files in the array and outputting them to the client side
                for (String item : filesList) {
                    out.println(item);
                }

                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " List of files was returned to the Client: " + client_id + " the list :" + filesList);

            } else {
                // send ERROR if the file indexer is empty
                out.println("ERROR");
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR in returning list of file to the Client " + client_id);
            }
        }
    }


    /*
     * release all locked files by the clienthandler that operated this function
     */
    public void unlock_my_files() {

        //release all locked files
        for (FileRep file : my_locked_files) {
            file.setLock_status(false);
            file.setLocked_by("");
        }
        my_locked_files.removeAll(my_locked_files);
    }

    /*
     * tokenizer to split the full command into command and filename
     */
    public static ArrayList<String> tokenize(String fullCommand) {
        ArrayList<String> command_list = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(fullCommand, " ");
        String filename = "";

        while (st.hasMoreTokens()) {
            command_list.add(st.nextToken());
        }
        command_list.add(filename);
        
        return command_list;
    }
    
    /**
    * function that handle the LOCK operation on all the neighbors server.
    *
    */

    public ArrayList<String> LockServers(String command, String receivedFileName) {

        // variables
        PrintWriter out = null;
        BufferedReader in = null;
        Socket serverSocket = null;
        String msg;
        // command to send
        command += " " + receivedFileName;
        ArrayList<String> temp_responses = new ArrayList<String>();
        System.err.println("LOCK all server occurs");

            // if server not connect try to connect again.
            for (Map.Entry<String, Socket> entry : Constants.neighbor_sockets.entrySet()) {

                try {
                if (Objects.isNull(entry.getValue())) {
                    for (String neigbor : Constants.neighbors) {
                        if (neigbor.contains(entry.getKey())) {
                            try {
                                entry.setValue(new Socket(ProcessServer.getHost_IP(neigbor),
                                        ProcessServer.getHost_PORT(neigbor)
                                ));
                            } catch (Exception ex) {
                                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: Cannot connect to server IP: " + ProcessServer.getHost_IP(neigbor) + " PORT: " + ProcessServer.getHost_PORT(neigbor));

                            }
                            System.err.println(entry);
                        }
                    }
                }
                // get current server socket
                serverSocket = entry.getValue();
                System.out.println(serverSocket);
                
                // initialize I/O
                out = new PrintWriter(serverSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Trying to lock at server: " + entry.getKey());
                // send command to neighbor
                out.println(command);
                System.err.println("SEND lock to server " + entry.getValue());
                // send the sender ID to the neighbor
                out.println(client_id);
                System.err.println("SEND ID to server ");
                // wait for the neighbor respond
                msg = in.readLine();
                System.err.println("GET " + msg + " from server: " + entry.getValue());
                // add to the responses array
                temp_responses.add(msg);

            } catch (Exception e) {
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: Server Cannot " + command);
                }
            }

        return temp_responses;
    }
    
    /**
     * 
    * function that handle the UNLOCK operation on all the neighbors server.
     *
     */

    public void unLockServers(String command, String receivedFileName) {

        // variables
        PrintWriter out = null;
        BufferedReader in = null;
        Socket serverSocket = null;
        String msg;
        System.err.println("UNLOCK all server occurs");
        // commadn to send
        command += " " + receivedFileName;

            // iterate over all the neighbors sockets
            for (Map.Entry<String, Socket> entry : Constants.neighbor_sockets.entrySet()) {
                try {

                // reconnect to a server if its not connected allready
                if (Objects.isNull(entry.getValue())) {
                    for (String neigbor : Constants.neighbors) {
                        if (neigbor.contains(entry.getKey())) {
                            entry.setValue(new Socket(ProcessServer.getHost_IP(neigbor), ProcessServer.getHost_PORT(neigbor)));
                        }
                    }
                }
                // get the current neighbor socket
                serverSocket = entry.getValue();
                // initialize I/O
                out = new PrintWriter(serverSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Trying to unlock at server: " + entry.getKey());
                // send command to neighbor
                out.println(command);                
                System.err.println("SEND unlock to server: " + entry.getValue());
                // send the sender ID to the neighbor
                out.println(client_id);
                // get response from server
                msg = in.readLine();
                System.err.println("GET " + msg + " from server: " + entry.getValue());
                // no need to check if neighbor suceeded to unlock or not. (requested in the assignment)
                } catch (Exception e) {
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: Server Cannot " + command);
                }
            }

    }
    
     /**
     * this function encoding the file to base64
     *
     * @param file - the command that the user choose
     */

    private static String encodeFileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "SUCCESS : " + file.getName() + " was encoded");
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR : " + file.getName() + " was not encoded successfully");
            return "";
        }
    }
//</editor-fold>
}
