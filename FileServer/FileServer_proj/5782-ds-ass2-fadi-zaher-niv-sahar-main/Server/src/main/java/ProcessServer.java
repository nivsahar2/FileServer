
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @authors: Fadi Zaher 205792526 Niv Sahar 205808272
 */
public class ProcessServer {

    Socket clientSocket;

    public static void main(String[] args) {

        //creating the server's indexing map/table
        Constants.indexing_map = new ConcurrentHashMap<String, FileRep>();

        //creating the server's clients array
        Constants.my_workers = new ArrayList<ClientHandler>();

        //logger
        Constants.logger = Logger.getLogger("MyLog");

        //properties obj for the config file read
        Properties prop = new Properties();

        //server config file name
        String fileName = "server.config";

        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " Server Started \n");

        try (FileInputStream fis = new FileInputStream(fileName)) {

            //creating file handler for our log file and setting append to false - cause we need new log file for each server run - can be changed
            Constants.fh = new FileHandler("server_logs.log", false);

            //adding handler for the logger obj
            Constants.logger.addHandler(Constants.fh);

            //setting output to console false
            Constants.logger.setUseParentHandlers(false);

            //choosing the simple format of the file log file
            SimpleFormatter formatter = new SimpleFormatter();
            Constants.fh.setFormatter(formatter);

            //loading the config file properties and getting them
            prop.load(fis);

            //initializing the file root and server port from the properties in the config file
            Constants.ROOT_DIR_PATH = prop.getProperty("root_dir");
            Constants.TEMP_DIR_PATH = prop.getProperty("temp_dir");
            Constants.CLIENT_PORT = prop.getProperty("client2server_port");
            Constants.SERVER_PORT = prop.getProperty("server2server_port");
            // get neighbor property from conf file
            String[] temp = prop.getProperty("neighbors").split(" ");

            // initialize the neighbors array
            for (int i = 0; i < temp.length; i++) {

                Constants.neighbors.add(temp[i]);
            }
            // call the Communication func, to try connect all the neighbors that ON.
            Communication();

            //creating the storage directory of server
            File root_dir = new File(Constants.ROOT_DIR_PATH);
            root_dir.mkdir();
            File temp_dir = new File(Constants.TEMP_DIR_PATH);
            temp_dir.mkdir();

            //if there is nothing in the storage or the directory yet created. the program won't crash
            try {
                //deleting all files in the directory
                deleteAllFilesInStorage(Constants.ROOT_DIR_PATH);
            } catch (Exception ex) {
                // write to the log file
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: coudln't read the configuration file\n");
            }

        } catch (FileNotFoundException e) {
            // write to the log file

            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: couldn't find the File");

        } catch (IOException e) {
            // write to the log file

            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: IO exception ");

        }

        // make a list of addresses to choose from
        // add in the usual ones
        //P.S - token from Dr. Micheal solution Class exercise 2  - and edited by us
        Vector<Inet4Address> adds = new Vector<Inet4Address>();
        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            adds.add((Inet4Address) Inet4Address.getByAddress(new byte[]{0, 0, 0, 0}));
            adds.addElement((Inet4Address) Inet4Address.getLoopbackAddress());
        } catch (UnknownHostException ex) {
            // something is really weird - this should never fail
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: couldn't find the IP address 0.0.0.0 ");
            return;

        }
        try {
            // get the local IP addresses from the network interface listing
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                // see if it has an IPv4 address
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    // go over the addresses and add them

                    InetAddress add = addresses.nextElement();
                    if (!add.isLoopbackAddress() && add instanceof Inet4Address) {
                        adds.addElement((Inet4Address) add);
                    }
                }
            }
        } catch (SocketException ex) {
            // can't get local addresses, something's wrong
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: Can't get network");

            return;
        }

        // call the StartSync func, for syncronized with all the server that is ON.
        StartSync();

        System.out.println("Choose an IP address to listen on :");

        int count = 0;
        for (int i = 0; i < adds.size(); i++) {
            // check if the ip is valid and show it in the list 
            if (check_IP(adds.elementAt(i).getHostAddress())) {
                System.out.println(i + ": " + adds.elementAt(i).getHostAddress());
                count++;
            }
        }
        // if there isnt atleast one ip valid, close the server.
        if (count < 1) {
            System.err.println("There is no available IP address\n");
            System.exit(0);
        }

        BufferedReader brIn = new BufferedReader(new InputStreamReader(System.in));
        int choice = -1;

        while (choice < 0 || choice
                >= adds.size()) {
            System.out.print(": ");

            try {
                String line = brIn.readLine();
                if (line.equals("")) {
                    System.err.println("Please choose number from the list!\n");
                    continue;
                }

                choice = Integer.parseInt(line.trim());
                // write to the log file

            } catch (IOException | NumberFormatException ex) {
                // write to the log file

                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: couldn't parse the choice ");
                continue;
            }
        }
        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " User choose " + adds.elementAt(choice).getHostAddress().toString());

        // the listen/stop loop
        String lineIn = "";
        Boolean quit = false;
        String serverPort = Constants.SERVER_PORT, clientPort = Constants.CLIENT_PORT;

        do {
            ServerSocket listener = null;
            ServerSocket s_listener = null;

            // start to listen on the one that the user chose and client port - for clients
            try {
                listener = new ServerSocket(Integer.parseInt(clientPort), 0, adds.elementAt(choice));
                Listener listening = new Listener(listener);
                listening.start();
                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " Server start listening on IP: " + adds.elementAt(choice).getHostAddress() + " PORT: " + Constants.CLIENT_PORT + " for clients");
            } catch (IOException e) {
                // fatal error, just quit
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: Can't listen on " + adds.elementAt(choice) + ":" + Constants.CLIENT_PORT);
                return;
            }

            // start to listen on the one that the user chose and server port -  for servers

            try {
                s_listener = new ServerSocket(Integer.parseInt(serverPort), 0, adds.elementAt(choice));
                ServerListener listen = new ServerListener(s_listener);
                listen.start();
                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " Server start listening on IP: " + adds.elementAt(choice).getHostAddress() + " PORT: " + Constants.SERVER_PORT + " for servers");

            } catch (IOException e) {
                // fatal error, just quit
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: Can't listen on " + adds.elementAt(choice) + ":" + Constants.SERVER_PORT);
                return;
            }

            // listen for the command to stop listening
            do {
                // we now have a working server socket, we'll use it later
                System.out.println(adds.elementAt(choice) + "\n");
                System.out.println("Listening on " + listener.getLocalSocketAddress().toString() + " for Clients ");
                System.out.println("Listening on " + s_listener.getLocalSocketAddress().toString() + " for Servers ");

                System.out.println("Enter 'STOP' to stop listening");
                try {
                    lineIn = brIn.readLine();
                } catch (IOException ex) {
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: reading from console");
                }

            } while (!lineIn.trim().toLowerCase().equals("stop"));

            // stop listening
            try {
                listener.close();
                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " Server Stop listening " + adds.elementAt(choice).getHostAddress().toString() + "for clients");

            } catch (IOException e) {
                // error while stopping to listen?  weird
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR:  stopping listening to clients");

            }

            // now we can resume listening if we want
            System.out.println("Listen again? [y/n]");
            do {
                System.out.print(": ");
                try {
                    lineIn = brIn.readLine();
                } catch (IOException ex) {
                    // write to the log file

                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: reading from console");
                }

            } while (!lineIn.trim().toLowerCase().equals("y") && !lineIn.trim().toLowerCase().equals("n"));

            // see whether we have an n or a y
            if (lineIn.trim().toLowerCase().equals("y")) {
                try {
                    System.err.println("Continue listenning for clients on port: ");
                    clientPort = brIn.readLine();

                } catch (IOException ex) {
                    // write to the log file
                    Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " ERROR: reading new port from console ");
                }

                quit = false;
                // write to the log file

                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " Resuming listening " + adds.elementAt(choice).getHostAddress().toString());

                System.out.println("Resuming listening");
            } else {
                quit = true;
                // quitting
                int index = 0;
                //interrupts every alive worker when the server closes
                while (Constants.my_workers.size() - 1 > 0) {
                    Constants.my_workers.get(index).interrupt();
                    //Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " stop listening to client: " + Constants.my_workers.get(index).client_id + " IP: " + listener.getInetAddress() + " PORT: " + listener.getLocalPort());
                    Constants.my_workers.remove(Constants.my_workers.get(index));
                    index++;
                }
                System.out.println("Bye!");
                // write to the log file

                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " stop listening and exit server ");

                System.gc();
                System.exit(0);
            }
        } while (!quit);

    }

    /**
     * deleting all the files at the storage directory on program start status:
     * working
     *
     * @param storage_root
     */
    static public void deleteAllFilesInStorage(String storage_root) {
        File file = new File(storage_root);
        String[] filesList = file.list();

        for (String item : filesList) {
            new File(storage_root + "\\" + item).delete();
        }
        // write to the log file

        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " All files was deleted at :" + storage_root);
    }

    /**
     * function that checks if the file name is legal
     *
     * @param fileName
     * @return true --> if the file name is legal || false --> if the file name
     * is illegal
     * @throws IOException
     */
    public static boolean checkFileName(String fileName) throws IOException {

        char[] str = fileName.toCharArray();
        // check if the file name has a char from the ASCII that dont acceptable
        for (char x : str) {
            if ((int) x >= 0 && (int) x <= 31) {
                // write to the log file

                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " The file name: " + fileName + " is unvalid");

                return false;
            }
        }

        // check if the file name has one of the illegals characters
        if (fileName.contains("<") || fileName.contains(">") || fileName.contains(":")
                || fileName.contains("\"") || fileName.contains("/") || fileName.contains("\\")
                || fileName.contains("|") || fileName.contains("?") || fileName.contains("*")) {
            // write to the log file

            Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " The file name: " + fileName + " is unvalid");

            return false;
        }

        return true;

    }

    /**
     * function that checks if the IP address is legal - checks that it's format
     * is like : xxx.xxx.xxx.xxx
     *
     * @param ip
     * @return true --> if the IP address is
     */
    public static boolean check_IP(String ip) {
        try {
            if (ip.startsWith(".") || ip.endsWith(".")) {
                return false;
            }
            if (ip == null || ip.equals("")) {
                return false;
            }
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            } else {
                for (String part : parts) {
                    if ((Integer.parseInt(part) < 0) || (Integer.parseInt(part) > 255)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * function that returns the host ip
     *
     * @param server
     * @return HOST_IP
     */
    public static String getHost_IP(String server) {
        return server.split("\\:")[1];
    }

    /**
     * function that returns the host port
     *
     * @param server
     * @return HOST_PORT
     */
    public static int getHost_PORT(String server) {
        return Integer.parseInt(server.split("\\:")[2]);
    }

    /**
     * function that returns the host name
     *
     * @param server
     * @return HOST_NAME
     */
    public static String getHost_NAME(String server) {
        return server.split("\\:")[0];
    }

    /**
     * Syncing function - this function will syncronized with all the server that is ON.
     * getting there files, and meta data.
     */
    public static void StartSync() {

        // variables and I/O vars
        BufferedReader in = null;
        PrintWriter out = null;
        ObjectInputStream ois;
        String command = "STARTSYNC";
        Socket serverSocket = null;
        ConcurrentHashMap<String, FileRep> received_indexer;

        Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " Start syncronize with servers");
        // key - server name
        // value - server indexer - from the key server
        Constants.SyncIndexersMap = new ConcurrentHashMap<>();

        // iterate over all the socket neighbors
        for (Map.Entry<String, Socket> entry : Constants.neighbor_sockets.entrySet()) {
            try {
                System.err.println("host " + entry.getKey());
                // get the socket of the current chosen server
                System.err.println(entry.getKey());
                serverSocket = entry.getValue();

                // initialize the out
                out = new PrintWriter(serverSocket.getOutputStream(), true);

                System.err.println(command);
                // send command to server
                out.println(command);
                // send to server that there is not sender.
                out.println("");
                // initialize the object
                ois = new ObjectInputStream(serverSocket.getInputStream());
                // get file indexer from neighbor server

                received_indexer = (ConcurrentHashMap<String, FileRep>) ois.readObject();
                System.err.println("received indexer");
                // if the file indexer not exist in SyncIndexerMap yet, add it
                if (!Constants.SyncIndexersMap.containsKey(entry.getKey())) {
                    //adding the received indexer to the synced indexers map
                    Constants.SyncIndexersMap.put(entry.getKey(), received_indexer);
                    System.err.println("indexer of " + entry.getKey());
                }

                // run over all file indexers
                for (ConcurrentHashMap<String, FileRep> map : Constants.SyncIndexersMap.values()) {
                    //if my current index_map is empty, i will insert the first map content into mine
                    if (Constants.indexing_map.size() == 0) {
                        System.err.println(map.size());
                        for (FileRep fileRep : map.values()) {
                            System.err.println("----------------");
                            System.err.println(fileRep + "\n");
                            System.err.println(Constants.neighbor_sockets.get(entry.getKey()) + "\n");
                            System.err.println(fileRep.getFile_name() + "\n");
                            System.err.println("----------------");
                            Constants.indexing_map.put(fileRep.getFile_name(), fileRep);
                            requestFile("DOWNLOAD", fileRep.getFile_name(), Constants.neighbor_sockets.get(entry.getKey()));
                        }
                    } //if it is not empty (that means that i entered the if statement already and now im coming here again), i will check the versions (by dates) and compare
                    else {
                        System.err.println("PROCESS : was in else ");
                        // run over all the ConcurrentMap values
                        for (FileRep fileRep : Constants.indexing_map.values()) {
                            System.err.println("Process: " + fileRep.getFile_name());
                            //returns the dates from each file
                            Date time_stamp_current_file = new SimpleDateFormat("dd-M-YYYY|HH:mm:ss").parse(fileRep.getLast_change_time());
                            Date time_stamp_other_file = new SimpleDateFormat("dd-M-YYYY|HH:mm:ss").parse(map.get(fileRep.getFile_name()).getLast_change_time());

                            //then current file is newer
                            if (time_stamp_current_file.after(time_stamp_other_file)) {
                                continue;
                            } else { // the current file is older, replace it.
                                Constants.indexing_map.replace(fileRep.getFile_name(), map.get(fileRep.getFile_name()));
                                // request to download the file from the neighbor server.
                                requestFile("DOWNLOAD", fileRep.getFile_name(), Constants.neighbor_sockets.get(entry.getKey()));
                            }
                        }
                    }
                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " SUCCESS : synchronized with " + entry.getKey());
                }
            } catch (Exception e) {
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " Connection refused to server: " + entry.getKey());
            }

        }
        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " finished synchronizing step");
    }

    /**
     * function that starts the communication with the server side.
     *
     * @param HOST_IP
     * @param HOST_PORT
     * @param chosenServer
     * @return
     */

    public static Socket connectToServer(String HOST_IP, int HOST_PORT, String chosenServer) {

        Socket serverSocket = null;
        System.err.println(HOST_IP + " - " + HOST_PORT);
        try {
            // if the client not connected to server, create a connection
            if (Objects.isNull(Constants.neighbor_sockets.get(chosenServer))) {
                serverSocket = new Socket(HOST_IP, HOST_PORT);
                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " SUCCESS : new socket was created for " + serverSocket.getRemoteSocketAddress());

            } else {
                serverSocket = Constants.neighbor_sockets.get(chosenServer);
            }

        } catch (Exception e) {
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " Connection refused to server: " + chosenServer);
        }

        return serverSocket;
    }

    /**
     * function that initialize the servers sockets array
     *
     * @return
     */

    public static void Communication() {

        Constants.neighbor_sockets = new TreeMap<String, Socket>();
        for (int i = 0; i < Constants.neighbors.size(); i++) {

            Constants.neighbor_sockets.put(getHost_NAME(Constants.neighbors.get(i)), connectToServer(getHost_IP(Constants.neighbors.get(i)), getHost_PORT(Constants.neighbors.get(i)), String.valueOf(getHost_NAME(Constants.neighbors.get(i)))));
        }
    }

    /**
     * function download the file from the neighbor server - to the root dir.
     *
     * @return
     *
     */

    private static void requestFile(String command, String file_name, Socket other_server) {
        // variables
        PrintWriter out = null;
        BufferedReader in = null;
        FileOutputStream fos = null;
        String msg = null;

        Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " File " + file_name + " was requested from server " + other_server.getRemoteSocketAddress());
        try {
            // build the command to send to the server
            command += " " + file_name;
            System.err.println("requestFile : " + command);
            // input/output to server
            out = new PrintWriter(other_server.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(other_server.getInputStream()));
            // send command and file name to server
            out.println(command);
            // send "" for no client sender
            out.println("");

            // if the server reponse OK
            FileOutputStream fileOutputStream = new FileOutputStream(Constants.ROOT_DIR_PATH + file_name, false);
            // download file with Base64 operations
            byte[] decodedBytes = Base64.getDecoder().decode(in.readLine().getBytes());
            fileOutputStream.write(decodedBytes, 0, decodedBytes.length);
            fileOutputStream.close();

            // server response
            msg = in.readLine();

            if (msg.toUpperCase().equals("OK")) {
                System.err.println("finished request file");
                Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " SUCCESS : File " + file_name + " received from server " + other_server.getRemoteSocketAddress());
            } else {
                Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + "ERROR :  File " + file_name + " was not received from server " + other_server.getRemoteSocketAddress());
                System.err.println("received error: " + msg);
            }
            // werite to log file
            Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " SUCCEEDED request file: " + file_name + " from server " + other_server.getRemoteSocketAddress());

        } catch (FileNotFoundException fileNotFoundException) {
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + " File " + file_name + " was not found");
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + "ERROR :  File " + file_name + " was not received from server " + other_server.getRemoteSocketAddress());
        } catch (IOException ioException) {
            Constants.logger.warning("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "]" + "ERROR :  File " + file_name + " was not received from server " + other_server.getRemoteSocketAddress());
        }
    }

}
