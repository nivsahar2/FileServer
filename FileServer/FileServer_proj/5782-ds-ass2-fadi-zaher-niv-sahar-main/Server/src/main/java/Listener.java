
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @authors: Fadi Zaher 205792526 Niv Sahar 205808272
 */
public class Listener extends Thread {

    ServerSocket listeningSocket;

    public Listener(ServerSocket serverSocket) {
        // save the socket we've been provided
        listeningSocket = serverSocket;
    }

    @Override
    public void run() {
        // start to listen on the socket
        try {
            while (true) {
                Socket clientSession = listeningSocket.accept();
                System.err.println(clientSession.getRemoteSocketAddress());

                // see if we were interrupted - then stop
                if (this.isInterrupted()) {
                    Constants.logger.info("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + " Stopped listening since we were interrupted.");
                    return;
                }
                // create a new handling thread for the client
                ClientHandler clientThread = new ClientHandler(clientSession);
                Constants.my_workers.add(clientThread);
                clientThread.start();

            }
        } catch (IOException e) {
            // problem with this connection, show the output and quit
            Constants.logger.severe("[" + new SimpleDateFormat("dd-M-YYYY HH:mm:ss").format(new Date(System.currentTimeMillis())) + "] " + "ERROR: listening for connections: ");
            return;
        }
    }

}
