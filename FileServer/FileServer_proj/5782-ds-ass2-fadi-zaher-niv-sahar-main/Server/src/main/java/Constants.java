
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * 
 * @authors: Fadi Zaher 205792526 Niv Sahar 205808272
 *
 */
// this Class hold the variables that should be Common to the use of the Server and the Client Handler
// 
class Constants {

    // variable
    // SERVER_PORT - the server port to listen on.
    public static String CLIENT_PORT = null, ROOT_DIR_PATH = null, SERVER_PORT = null, TEMP_DIR_PATH = null;
    public static FileHandler fh;
    public static Logger logger;
    public static Vector<String> neighbors = new Vector<String>();
    public static Map<String, Socket> neighbor_sockets;
    public static ConcurrentMap<String, ConcurrentHashMap<String, FileRep>> SyncIndexersMap;


    /**
     * concurrent hash map will represent the indexer of the server content files:
     * <Key: file name , Value: meta data on the file (object of FileRep)>
     */
    public static ConcurrentMap<String, FileRep> indexing_map;

    /**
     * my_workers represents the client handlers array
     */
    public static ArrayList<ClientHandler> my_workers;

}
