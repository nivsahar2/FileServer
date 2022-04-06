// IMPORTS

import javax.swing.*;
import java.nio.file.Files;
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @authors: NivSahar 205808272 Fadi Zaher 205792526
 */
// Client class contains all the client operations/variables
class Client {

    // Array list contains all the servers in the configuration file.
    public static ArrayList<String> Servers = new ArrayList<>();

    // Map contains all the socket session to the servers - key=host_name | value=socket object
    public static Map<String, Socket> Servers_sockets;

    // variable that will hold the current file, chosen in the GUI
    public static File current_file;

    // variable to hold the file name
    public static String file_name = "";

    // GUI object to make changes on the GUI
    //###############################
    public static JTextArea txtArea;
    public static JList serversList;
    public static JList filesList;
    public static JTextField chosenFile;
    private static JFileChooser jfc = null;
    private static String whoami;
    //####################################

    // input/output variables
    public static PrintWriter out = null;
    private static BufferedReader in = null;

    // variable will hold the client socket
    public static Socket clientSocket = null;

    // Array for hold the tokenizer parts of the command from the GUI.
    private static ArrayList<String> command_list = null;

    // General function that execute every time there is an event(click) from the GUI  
    public static void ExecuteFromGUI(String command, File file) throws IOException {

        // holds the current file
        current_file = file;

        // this block checks which command the user choose in the GUI
        // built the command that will be send to the server
        // and execute the right function
        // ############################################################
        if (command.contains("UPLOAD")) {
            command += " " + file_name;
            uploadFileAllServers(command.replace("ALL", ""));

        } else if (command.contains("LOCK") && !command.contains("UNLOCK")) {
            lockFileAllServers(command.replace("ALL", ""));

        } else if (command.contains("UNLOCK")) {
            unlockFileAllServers(command.replace("ALL", ""));

        } else if (command.contains("GETVERSION")) {
            if (command.contains("ALL")) {
                getVersionAllServers(command.replace("ALL", ""));
            } else {
                getVersion(command);
            }
        } else if (command.contains("GETLIST")) {
            if (command.contains("ALL")) {
                getFileListAllServers(command.replace("ALL", ""));
            } else {
                getFileList(command);
            }
        } else if (command.contains("DOWNLOAD")) {
            downloadFile(command);
        }
        // ############################################################
    }


    /**
     * this function handles getting version of the picked file from the list,
     * from specific server
     *
     * @param command
     */
    public static void getVersion(String command) {


        // variables
        String msg = null, result = null;
        String file_name;
        // check if there is chosen file fromm the list and if there is chosen server from the severs list
        if (checkFileFromList() && checkChosenServer()) {
            try {
                // get file name
                file_name = String.valueOf(filesList.getSelectedValue());
                // build the command that send to the server
                command += " " + file_name;
                // get the socket of the current server
                clientSocket = Servers_sockets.get(serversList.getSelectedValue());
                // Input/Output
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // send command to server
                out.println(command + " " + whoami);


                // server respond
                msg = in.readLine();

                // check if the server respond VERSION
                // build the message for the user in accordance
                if (msg.toUpperCase().contains("VERSION")) {

                    String[] res = msg.split(" ");
                    //build the message to display
                    if (res.length == 4) {
                        result = res[0] + " : " + res[1] + " " + res[2] + " " + res[3] + " from " + serversList.getSelectedValue() + "\n";
                    } else {
                        result = res[0] + " : " + res[1] + " " + res[2] + " from " + serversList.getSelectedValue() + "\n";
                    }

                } else {
                    result = msg + " : Failed to get version of " + file_name + " from server: " + serversList.getSelectedValue() + "\n";
                }
                // display message
                txtArea.append(result);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Cannot getversion of file", "InfoBox: " + "IO error", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * this function handles getting files list from specific server.
     *
     * @param command
     */

    public static void getFileList(String command) {
        // check id the user choose server
        if (checkChosenServer()) {
            try {
                // get the socket of the current server
                clientSocket = Servers_sockets.get(serversList.getSelectedValue());
                //Input/Output - with server
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // send the command to server
                String result;

                out.println(command + " " + whoami);


                // server respond
                String msg = in.readLine();

                // variables
                String temp;
                DefaultListModel tempList = new DefaultListModel();
                // check the server respond and build the result in accordance
                if (msg.toUpperCase().equals("OK")) {
                    int list_length = Integer.parseInt(in.readLine());

                    try {
                        //iterates and adds the files to the list
                        for (int i = 0; i < list_length; i++) {
                            temp = in.readLine();
                            tempList.addElement(temp);
                            filesList.setModel(tempList);
                        }
                        result = msg + " : File list was reaturned from server: " + serversList.getSelectedValue() + " (look at the list next to servers list)\n";
                        // display the message
                        txtArea.append(result);
                    } catch (Exception e) {

                    }
                } else {
                    // if there is no files in server
                    result = msg + " : There are no files in the server yet\n";
                    txtArea.append(result);
                }

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Cannot filelist from server", "InfoBox: " + "IO error", JOptionPane.INFORMATION_MESSAGE);
            }
        }


    }

    /**
     * this function handles uploading single file the first server it succeeded to talk with
     *
     * @param command
     */
    public static void uploadFileAllServers(String command) {

        // variables I/O
        PrintWriter out = null;
        BufferedReader in = null;
        String file_name;
        String result = null;
        OutputStream outputStream;
        FileInputStream fileInputStream;

        if (checkChosenFile()) {

        file_name = current_file.getName();
        // build the command that will send to the server
        command += " " + file_name;

        // iterate all the server from the configuration file
        for (Map.Entry<String, Socket> entry : Servers_sockets.entrySet()) {
            clientSocket = entry.getValue();
            if (Objects.isNull(clientSocket)) {
                txtArea.append("ERROR : Not connected to: " + entry.getKey() + "\n");
                continue;
            }
            try {

                // initialize I/O
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // send command to server and sender id
                out.println(command + " " + whoami);
                // send the file to server with Base64 operation
                String originalInput = encodeFileToBase64(current_file);
                // send file size
                out.println(originalInput);


                // server respond
                String msg = in.readLine();

                if (msg.equals("OK")) {
                    result = msg + " : The file " + file_name + " was uploaded successfully to " + entry.getKey() + "\n";

                } else {
                    result = msg + " : The file " + file_name + " was not uploaded successfully to " + entry.getKey() + "\n";
                }
                // display message
                txtArea.append(result);
                break;
            } catch (IOException e) {
                //    JOptionPane.showMessageDialog(null, "Cannot upload file", "InfoBox: " + "IO error", JOptionPane.INFORMATION_MESSAGE);
                txtArea.append("Cannot upload file with server: " + entry.getKey() + "\n");
            }
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
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot find file", "InfoBox: " + "IO error", JOptionPane.INFORMATION_MESSAGE);
            return "";
        }
    }

    /**
     * this function handle the lock on file in a multiple servers
     *
     * @param command - the command that the user choose
     */
    public static void lockFileAllServers(String command) {
        String msg = null, result = null;
        String file_name;


        if (checkFileFromList()) {
            file_name = String.valueOf(filesList.getSelectedValue());
            command += " " + file_name;

            // iterate over all the server in the conf file
            for (Map.Entry<String, Socket> entry : Servers_sockets.entrySet()) {

                try {

                    clientSocket = entry.getValue();
                    // Initialize I/O variables
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    // send command to server and sender id
                    out.println(command + " " + whoami);


                    // server respond
                    msg = in.readLine();


                    // create message related to the response
                    if (msg.toUpperCase().equals("OK")) {
                        result = msg + " : Succeeded to lock the file " + file_name + " on server: " + entry.getKey() + "\n";
                    } else {
                        result = msg + " : Failed to lock the file " + file_name + " on server: " + entry.getKey() + "\n";
                    }
                    txtArea.append(result);
                    break;

                } catch (Exception ex) {
                  //  JOptionPane.showMessageDialog(null, "Cannot lock file on: " + entry.getKey(), "InfoBox: " + "IO error", JOptionPane.INFORMATION_MESSAGE);
                    txtArea.append("Cannot lock file on: " + entry.getKey() + "\n");
                }
            }
        }
    }

    /**
     * this function handle unlock file on the first server it succeeded to talk with
     *
     * @param command
     */
    public static void unlockFileAllServers(String command) {

        // variables
        String msg = null, result = null;
        String file_name;

        // check the if frile chosen and if server chosen
        if (checkFileFromList()) {

            // get name of file
            file_name = String.valueOf(filesList.getSelectedValue());
            // build command
            command += " " + file_name;
            // iterate over all the server in the configurstion file, untill find the first that is connected
            for (Map.Entry<String, Socket> entry : Servers_sockets.entrySet()) {

                try {
                    // get the socket of the current chosen server
                    clientSocket = entry.getValue();
                    // initialize the I/O
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    // send commnad to server and the sender ID
                    out.println(command + " " + whoami);

                    // get respond from server
                    msg = in.readLine();

                    // build the specific msg to display to the user.
                    if (msg.toUpperCase().equals("OK")) {
                        result = msg + " : Succeeded to unlock the file " + file_name + " on server: " + entry.getKey() + "\n";
                    } else {
                        result = msg + " : Failed to unlock the file " + file_name + " on server: " + entry.getKey() + "\n";
                    }

                    // display message to user
                    txtArea.append(result);
                    break;

                } catch (Exception ex) {
                  //  JOptionPane.showMessageDialog(null, "Cannot unlock file on: " + entry.getKey(), "InfoBox: " + "IO error", JOptionPane.INFORMATION_MESSAGE);
                    txtArea.append("Cannot unlock file on: " + entry.getKey() + "\n");
                }
            }
        }
    }

    /**
     * this function gets the versions of the picked file from all servers
     *
     * @param command
     */
    public static void getVersionAllServers(String command) {

        // variables
        String msg = null, result = null;
        String file_name;
        //check if the user chose file and chose server
        if (checkFileFromList() && checkChosenServer()) {
            // get file name
            file_name = String.valueOf(filesList.getSelectedValue());
            // build the comand to the server
            command += " " + file_name;
            //run over all the sockets of the server
            for (Map.Entry<String, Socket> entry : Servers_sockets.entrySet()) {
                clientSocket = entry.getValue();


                // check if its null
                if (Objects.isNull(clientSocket)) {
                    txtArea.append("ERROR : Not connected to: " + entry.getKey() + "\n");
                    continue;
                }
                try {
                    //input/output - with server
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    //sending the command to the server and sender ID
                    out.println(command + " " + whoami);


                    //receiving the response of the server
                    msg = in.readLine();

                    // build the message to display
                    if (msg.toUpperCase().contains("VERSION")) {
                        result = msg + " in " + entry.getKey() + "\n";
                    } else {
                        result = msg + " : Failed to get version of " + file_name + " from server : " + entry.getKey()
                                + "\n";
                    }
                    //display to the user
                    txtArea.append(result);

                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Cannot getversion of file from all servers - server: " + entry.getKey() + " no root anymore", "InfoBox: " + "IO error", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    /**
     * this function handle getting list of files of all servers
     *
     * @param command
     */
    public static void getFileListAllServers(String command) {


        //variables
        String msg = null, result = null;
        txtArea.append("All servers list: \n");
        // run over all the sockets server
        for (Map.Entry<String, Socket> entry : Servers_sockets.entrySet()) {
            clientSocket = entry.getValue();
            // ERROR if its null

            if (Objects.isNull(clientSocket)) {
                txtArea.append("ERROR: Not connected to: " + entry.getKey() + "\n");
                continue;
            }

            try {
                // initialize input/output
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                //sending the command to the server and sender ID
                out.println(command + " " + whoami);


                //receiving the response of the server
                msg = in.readLine();

                // if server recieves OK
                if (msg.toUpperCase().equals("OK")) {
                    String temp;

                    String list_length = in.readLine();

                    try {
                        // display to the user the list of files in the server
                        String server_list = entry.getKey() + ": ";
                        //iterating and build the string that holds the full list of files in the specific server
                        for (int i = 0; i < Integer.parseInt(list_length); i++) {
                            temp = in.readLine();

                            server_list += temp + ":";
                        }
                        server_list += "\n";
                        txtArea.append(server_list);
                    } catch (Exception e) {

                    }
                } else {
                    result = msg + " : in " + entry.getKey() + " there are no files\n";
                }
                // display the result
                txtArea.append(result);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Cannot filelist from all servers - server: " + entry.getKey() + " no root anymore ", "InfoBox: " + "IO error", JOptionPane.INFORMATION_MESSAGE);
            }

        }

    }

    /**
     * this function handle the download of file from server
     *
     * @param command
     */
    public static void downloadFile(String command) throws IOException {

        // variables
        PrintWriter out = null;
        BufferedReader in = null;
        String file_name;
        String directory_name = "";
        String msg = null, result = null;

        // check if the user choose server and file from the Files list
        if (checkChosenServer() && checkFileFromList()) {
            try {

                //file dialog for directory choose only
                jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jfc.setAcceptAllFileFilterUsed(false);

                if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    // get the directory to download the file to.
                    directory_name = jfc.getSelectedFile().getAbsolutePath();
                    // get file name to download
                    file_name = String.valueOf(filesList.getSelectedValue());

                    // build the command to send to the server
                    command += " " + file_name;
                    // get the current socket
                    clientSocket = Servers_sockets.get(serversList.getSelectedValue());
                    // input/output to server
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    // send command and file name to server and sender ID
                    out.println(command + " " + whoami);

                    FileOutputStream fileOutputStream = new FileOutputStream(directory_name + "\\" + file_name, false);
                    // use Base64 operation for download file
                    byte[] decodedBytes = Base64.getDecoder().decode(in.readLine().getBytes());

                    fileOutputStream.write(decodedBytes, 0, decodedBytes.length);
                    fileOutputStream.close();

                    // server respond
                    msg = in.readLine();
                    // build the result string to display to the user
                    if (msg.toUpperCase().equals("OK")) {
                        result = msg + " : Succeeded in downloading the file " + file_name + " from server: " + serversList.getSelectedValue() + "\n";

                    } else {
                        result = msg + " : Failed to download the file " + " from server: " + serversList.getSelectedValue() + "\n";

                    }
                    // display to the user
                    txtArea.append(result);
                } else {
                    JOptionPane.showMessageDialog(null, "Bad path was given", "IO error", JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Cannot download file, error occurs", "IO error", JOptionPane.INFORMATION_MESSAGE);
            }
        }

    }

    /**
     * function that returns the host ip
     *
     * @param server
     * @return HOST_IP
     */
    public static String getHost_IP(String server) {
        String HOST_IP = "";
        for (String serv : Servers) {
            if (serv.contains(server)) {
                HOST_IP = serv.split("\\s")[1].split("\\:")[0];
            }
        }
        return HOST_IP;
    }

    /**
     * function that returns the host port
     *
     * @param server
     * @return HOST_PORT
     */
    public static int getHost_PORT(String server) {
        int HOST_PORT = 0;
        for (String serv : Servers) {
            if (serv.contains(server)) {
                HOST_PORT = Integer.parseInt(serv.split("\\s")[1].split("\\:")[1]);
            }
        }

        return HOST_PORT;
    }

    /**
     * function that returns the host name
     *
     * @param server
     * @return HOST_NAME
     */
    public static String getHost_NAME(String server) {
        String HOST_NAME = "";

        for (String serv : Servers) {
            if (serv.contains(server)) {
                HOST_NAME = serv.split("\\s")[0];
            }
        }

        return HOST_NAME;
    }

    /**
     * function that initializes the servers array
     *
     * @param AL
     */
    public static void InitializeServers(ArrayList<String> AL) {
        for (String server : AL) {
            Servers.add(server);
        }
    }

    /**
     * checks if the client choose file from the list
     *
     * @return
     */
    public static boolean checkFileFromList() {

        try {
            if (!filesList.getSelectedValue().equals("")) {
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "Please choose a file from the list!", "InfoBox: " + "Empty text box", JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please choose a file from the list!", "InfoBox: " + "Empty text box", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
    }

    /**
     * checks if the client picked file to upload
     *
     * @return
     */
    public static boolean checkChosenFile() {

        if (!chosenFile.getText().equals("")) {
            return true;
        } else {
            JOptionPane.showMessageDialog(null, "Please choose a file!", "InfoBox: " + "Empty text box", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
    }

    /**
     * checks if the client picked server from the list
     *
     * @return
     */
    public static boolean checkChosenServer() {

        String chosenServer;

        try {
            // check if user choose a file form the list
            if (!Objects.isNull(serversList.getSelectedValue())) {
                chosenServer = String.valueOf(serversList.getSelectedValue());
                clientSocket = Servers_sockets.get(chosenServer);
                // check if there is a socket connected
                if (clientSocket != null) {
                    return true;
                } else {
                    JOptionPane.showMessageDialog(null, "Not connected to server: " + chosenServer, "InfoBox: " + "Connection ERROR", JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }
            } else {
                JOptionPane.showMessageDialog(null, "Please choose a Server!", "InfoBox: " + "Empty text box", JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        } catch (NullPointerException e) {
            JOptionPane.showMessageDialog(null, "Please choose a Server!", "InfoBox: " + "Empty text box", JOptionPane.INFORMATION_MESSAGE);
        }
        return false;

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

        Socket clientSocket = null;

        try {
            // if the client not connected to server, create a connection
          //  if (Objects.isNull(Servers_sockets.get(chosenServer)) || Servers_sockets.get(chosenServer).isConnected()) {
                clientSocket = new Socket(HOST_IP, HOST_PORT);
         //   } else {
         //       clientSocket = Servers_sockets.get(chosenServer);
        //    }

            txtArea.append("Connected to server: " + chosenServer + "\n");

        } catch (Exception e) {
            txtArea.append("Failed connect to Server: " + chosenServer + "\n");

        }
        return clientSocket;
    }

    /**
     * function that initialize the servers sockets array
     *
     * @return
     */
    public static void Communication() {

        Servers_sockets = new TreeMap<String, Socket>();

        for (int i = 0; i < Servers.size(); i++) {
            Servers_sockets.put(getHost_NAME(Servers.get(i)), connectToServer(getHost_IP(Servers.get(i)), getHost_PORT(Servers.get(i)), String.valueOf(getHost_NAME(Servers.get(i)))));
        }
    }

    /**
     * quit function - this function close the Client and send a quit message to the server
     * for close all the handlers that the server created for this client
     */

    public static void Quit() {

        PrintWriter out = null;

        for (int i = 0; i < Servers.size(); i++) {
            try {
                Socket temp_Socket = Servers_sockets.get(getHost_NAME(Servers.get(i)));
                if (!Objects.isNull(temp_Socket)) {
                    out = new PrintWriter(temp_Socket.getOutputStream(), true);
                    // tell the server that the client quit
                    out.println("Quit");
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Cannot close connection", "InfoBox: " + "Empty text box", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        System.exit(0);
    }

    /**
     * handles initialization of the - txtArea, serversList, choosenFile,
     * fileList
     *
     * @param txtA
     * @param sList
     * @param cFile
     * @param fList
     * @param jfc
     */
    public static void InitializeObjects(JTextArea txtA, JList sList, JTextField cFile, JList fList, JFileChooser jfc) {
        txtArea = txtA;
        serversList = sList;
        chosenFile = cFile;
        filesList = fList;

        try {
            whoami = Inet4Address.getLocalHost().getHostAddress() + ":" + new Random().nextInt(8000);
        } catch (UnknownHostException ex) {
        }
    }

}
