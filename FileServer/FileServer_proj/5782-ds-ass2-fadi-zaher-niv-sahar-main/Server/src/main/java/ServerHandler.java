
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 * @author NivSahar
 */
public class ServerHandler extends Thread {

    // Class variables
    Socket new_Socket = null;
    boolean connection = true;
    ArrayList<FileRep> my_locked_files = new ArrayList<>();
    BufferedReader in = null;
    PrintWriter out = null;
    DataInputStream dis;
    DataOutputStream dos;
    FileInputStream fis;
    FileOutputStream fos;
    InetSocketAddress socketAddress = null;
    Socket serverSocket;
    String full_command, receivedCommand, receivedFileName;
    String command_sender = "";

    ServerHandler(Socket serverSession) {
        this.serverSocket = serverSession;
        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "Server " + serverSocket.getRemoteSocketAddress() + " ,  CONNECTED");
    }

    @Override
    public void run() {

        boolean connection = true;
        try {
            System.err.println("Server Handler");
            //printwriter and buffered reader to receive and send data
            out = new PrintWriter(serverSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            while (connection) {
                try {
                    // get the full command
                    System.err.println("waiting for input");
                    full_command = in.readLine();
                    System.err.println("full command " + full_command);
                    // holds the sender ID - which client
                    command_sender = in.readLine();
                    System.err.println("sender " + command_sender);
                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "command : " + full_command + " was received from " + command_sender);
                    // tokenize the full command
                    ArrayList<String> server_request = tokenize(full_command);
                    System.err.println("TOKENIZER: #########: " + server_request);

                    // seperate to vairables the splitted string
                    if (!Objects.isNull(server_request)) {
                        receivedCommand = server_request.get(0);

                        if (server_request.size() > 1 && !full_command.contains("OFFER")) {
                            receivedFileName = server_request.get(1);
                        } else if (full_command.contains("OFFER")) {
                            receivedFileName = server_request.get(3);
                        }
                    }

                    System.err.println(receivedCommand);
                    System.err.println(receivedFileName);

                    // check which functio will be execute
                    if (receivedCommand.equals("STARTSYNC")) {
                        StartupSync();
                    } else if (receivedCommand.equals("LOCK")) {
                        lock();
                    } else if (receivedCommand.equals("UNLOCK")) {
                        unlock();
                    } else if (receivedCommand.equals("DOWNLOAD")) {
                        DownloadFile(receivedFileName);
                    } else if (receivedCommand.equals("OFFER")) {
                        System.err.println("was here");
                        get_Offer(full_command);
                    }
                } catch (Exception e) {
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Connection ERROR " + command_sender);
                    connection = false;
                }
            }
        } catch (Exception e) {
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Sever handler failed to get commands");
        }
    }

    /**
     *
     * function that handle the DOWNLOAD operation from the server.
     *
     */
    private void DownloadFile(String receivedFileName) {

        synchronized (Constants.indexing_map) {

            try {
                // dos = new DataOutputStream(clientSocket.getOutputStream());
                File current_file = new File(Constants.ROOT_DIR_PATH + receivedFileName);
                //fis = new FileInputStream(Constants.FILE_ROOT_PATH + receivedFileName);

                String originalInput = encodeFileToBase64(current_file);

                // send file size
                out.println(originalInput);

                // write to the log file
                out.println("OK");
                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "SUCCESS : file : " + receivedFileName + " was downloaded successfully");
            } catch (Exception e) {
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR : file : " + receivedFileName + " was not downloaded successfully");
                out.println("ERROR");

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
            Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "SUCCESS : file -> " + file.getName() + " was successfully encoded");
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            //ADD A LOG!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //TODO:
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR : file -> " + file.getName() + " couldn't be encoded");
        }
        return "";
    }

    // this function handle the StartSync that the root server execute - return the current file indexer
    public void StartupSync() {

        // syncronized block
        synchronized (Constants.indexing_map) {

            ObjectOutputStream oos;
            //  ObjectInputStream ois;

            try {
                // send the file indexer object 
                oos = new ObjectOutputStream(serverSocket.getOutputStream());

                oos.writeObject(Constants.indexing_map);

                oos.flush();

                System.err.println("recieved command: " + receivedCommand.toUpperCase());
                // write to log
                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "SUCCESS : indexing map was sent to the neighbor server : " + this.serverSocket.getRemoteSocketAddress());

            } catch (Exception e) {
                // write to log
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR : indexing map was  not sent to the neighbor server : " + this.serverSocket.getRemoteSocketAddress());
            }
        }
    }

    /*
     * tokenizer to split the full command into command and filename
     */
    public static ArrayList<String> tokenize(String fullCommand) {

        ArrayList<String> command_list = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(fullCommand, " ");
        String filename = "";

        if (!fullCommand.contains("OFFER")) {
            if (st.hasMoreTokens()) {
                command_list.add(st.nextToken());
            }
            if (st.hasMoreTokens()) {
                filename += st.nextToken();
            }

            while (st.hasMoreTokens()) {
                filename += " " + st.nextToken();
            }
            command_list.add(filename);
        } else {
            while (st.hasMoreTokens()) {
                command_list.add(st.nextToken());
            }
        }
        return command_list;
    }

    /**
     * function that handle the lock operaton that came from the client handler
     * (root server)
     *
     */
    // CHECK ALL THIS FUNCTION
    public void lock() {

        synchronized (Constants.indexing_map) {

            try {
                //true -> if the hash contains the file
                if (Constants.indexing_map.containsKey(receivedFileName)) {
                    System.err.println("LOCK all server occurs");
                    //object that represents the file from the indexing map
                    FileRep fr = (FileRep) Constants.indexing_map.get(receivedFileName);
                    //can't lock locked file - so error will be sent to client
                    if (fr.isLock_status()) {
                        out.println("ERROR");
                        Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: failed in locking the file -> " + receivedFileName);

                    } else { // if not locked

                        System.err.println("FILE STATUS BEFORE------------------------------");
                        System.err.println("file lock status: " + fr.isLock_status());
                        System.err.println("file name: " + fr.getFile_name());
                        System.err.println("locked: " + fr.getLocked_by());
                        System.err.println("------------------------------------------");
                        //lock the file
                        fr.setLock_status(true);
                        fr.setLocked_by(command_sender);
                        System.err.println("LOCK: " + fr.getLocked_by());
                        // TO CHECK
                        // why i need this??
                        // fr.setFile_version(fr.getFile_version());

                        System.err.println("FILE STATUS AFTER------------------------------");
                        System.err.println("file lock status: " + fr.isLock_status());
                        System.err.println("file name: " + fr.getFile_name());
                        System.err.println("locked: " + fr.getLocked_by());
                        System.err.println("------------------------------------------");
                        //send OK the the root server
                        out.println("OK");
                        System.err.println("OK occurs in lock serverhandler");
                        //adding this file to this client locked files list
                        // why i use this array here?
                        // my_locked_files.add(fr);
                        // write to the log file
                        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "SUCCESS: locked the file -> " + receivedFileName);
                    }
                } else {
                    out.println("ERROR");
                    // write to the log file
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: tried to lock un-found file");
                }
            } catch (Exception e) {
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: tried to lock un-found file");

            }
        }
    }

    /**
     * function that handle the unlock operaton that came from the client
     * handler (root server)
     *
     */
    public void unlock() {

        // syncronized block
        synchronized (Constants.indexing_map) {

            String current_client;
            System.err.println("UNLOCK server occurs - server handler");

            try {
                //true -> if the hash contains the file

                if (Constants.indexing_map.containsKey(receivedFileName)) {

                    //object that represents the file from the indexing map
                    FileRep fr = (FileRep) Constants.indexing_map.get(receivedFileName);
                    //true -> if the client is the one who locked the file
                    System.err.println("who sender: " + command_sender);
                    System.err.println("getLocked_by " + fr.getLocked_by());
                    // check if the sender that requested to unlock is the one that lock it.
                    if (fr.getLocked_by().equals(command_sender)) {

                        //setting lock status to false
                        fr.setLock_status(false);
                        fr.setLocked_by("");
                        System.err.println("FILE STATUS ------------------------------");
                        System.err.println("file lock status: " + fr.isLock_status());
                        System.err.println("file name: " + fr.getFile_name());
                        System.err.println("locked: " + fr.getLocked_by());
                        System.err.println("------------------------------------------");
                        // why i need this!
                        //removing this file from this client locked files list
                        // my_locked_files.remove(fr); // write to the log file

                        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " lock File ----> " + receivedFileName);
                        // send OK to root server
                        out.println("OK");
                        System.err.println("OK occurs in unlock serverhandler");
                    } else {
                        // send ERROR to root server
                        out.println("ERROR");
                        // write to the log file

                        Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: failed to unlock File ----> " + receivedFileName);
                    }

                } else {
                    // send ERROR to root server
                    out.println("ERROR");
                    // write to the log file
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: tried to unlock un-found file");
                }
            } catch (Exception e) {
                //TODO:
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " ERROR: failed to unlock File ----> " + receivedFileName);
            }
        }

    }

    /**
     * function that handle the OFFER operaton that came from the client
     * handler(root server)
     *
     */
    public void get_Offer(String receivedFileName) throws IOException {

        System.err.println("************in GET_OFFER**********");
        // syncronized block
        synchronized (Constants.indexing_map) {

            // variables
            String file_name;
            String file_digest;
            String file_timestap;
            String command;

            // split the full command
            ArrayList<String> splitted_command = tokenize(full_command);

            // initialize the variables with the data from the OFFER command, like requested 
            System.err.println("-------------------");
            command = splitted_command.get(0);
            System.err.println(command);
            file_digest = splitted_command.get(1);
            System.err.println(file_digest);
            file_timestap = splitted_command.get(2);
            System.err.println(file_timestap);
            file_name = splitted_command.get(3);
            System.err.println(file_name);
            System.err.println("-------------------");

            //true --> if the file exist
            if (Constants.indexing_map.containsKey(file_name)) {

                System.err.println("FILE exist - server handler");
                FileRep fr = Constants.indexing_map.get(file_name);
                //true --> if the file is locked by me
                System.err.println("----------FILE DETAILS server handler-------------");
                System.err.println(fr.getFile_name());
                System.err.println(fr.getLocked_by());
                System.err.println(fr.getFile_version());
                System.err.println(fr.getLast_change_time());
                System.err.println("------------------------------------");
                System.err.println("sender " + command_sender);

                // if the file locked by the sender
                if (fr.getLocked_by().equals(command_sender)) {
                    System.err.println("Locked by me - server handler");
                    try {
                        //opening data channel to read the file later

                        FileOutputStream fos = new FileOutputStream(Constants.ROOT_DIR_PATH + file_name, false);
                        // request to download the file - if its exist and locked by sender 
                        out.println("DOWNLOAD");
                        System.err.println("SERVER handler send: DOWNLOAD");
                        //setting byte array that will hold packets of the received file with Base64
                        byte[] decodedBytes = Base64.getDecoder().decode(in.readLine().getBytes());
                        String decodedString = new String(decodedBytes);

                        fos.write(decodedBytes, 0, decodedBytes.length);
                        fos.close();

                        System.err.println("After while server handler");

                        //setting new timestamp to the file that came from OFFER command
                        fr.setLast_change_time(file_timestap);

                        // set file digest that came from OFFER command
                        fr.setVersion(file_digest);

                        fos.close();
                        // sned OK to root server
                        out.println("OK");
                        System.err.println("OK occurs in get_offer serverhandler");
                        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "SUCCESS : the file was uploaded, File -> " + receivedFileName + " was overwritten");

                    } catch (IOException e) {
                        Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR : the file was not uploaded, File -> " + receivedFileName + " was not overwritten");
                    }

                } //the is not locked by me
                else {
                    // write to the log file
                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR : the file was not uploaded, File -> " + receivedFileName + " was not overwritten");
                    out.println("ERROR");
                }
            } //the file doesn't exist in the server
            else {
                try {
                    System.err.println("in ELSE");
                    //oppening data channel to read the file later

                    //creating object that will be used to write the folder content
                    FileOutputStream fos = new FileOutputStream(Constants.ROOT_DIR_PATH + file_name, false);
                    System.err.println("before DOWNLOAD");

                    out.println("DOWNLOAD");
                    //in.readLine();

                    //initializing the file_size from the client side
                    byte[] decodedBytes = Base64.getDecoder().decode(in.readLine().getBytes());
                    String decodedString = new String(decodedBytes);

                    fos.write(decodedBytes, 0, decodedBytes.length);
                    fos.close();

                    FileRep fr = new FileRep(Constants.ROOT_DIR_PATH, file_name);

                    //setting new timestamp to the file that came from the root server
                    fr.setLast_change_time(file_timestap);
                    // set the digest from the OFFER command that came from the OFFER command
                    fr.setVersion(file_digest);
                    System.err.println("before OK");

                    //create new file
                    Constants.indexing_map.put(file_name, fr);
                    // write to the log file
                    out.println("OK");

                    System.err.println("OK occurs in get_offer(else) serverhandler");
                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "SUCCESS : the file was uploaded, File -> " + receivedFileName);
                } catch (IOException e) {
                    out.println("ERROR");
                    // write to the log file
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR : the file was not uploaded, File -> " + receivedFileName);
                }
            }
        }
        System.err.println("get_offer server handler finished");
    }
}
