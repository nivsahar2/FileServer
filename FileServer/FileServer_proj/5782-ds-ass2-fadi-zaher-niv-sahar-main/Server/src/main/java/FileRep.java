import java.io.FileInputStream;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;


/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 * @authors: Fadi Zaher 205792526 Niv Sahar 205808272
 */
public class FileRep implements Serializable {

    //will hold the file name
    private String file_name;

    //will gold the file lock status
    private boolean lock_status;

    //will hold the time stamp of the last modification time
    private String last_change_time;

    // holds the format that should returned when GETVERSION occurs
    private String file_version;

    // hold who locked the file
    private String locked_by = "";

    // holds the current file version
    private String version = "";

    // constructor
    public FileRep(String path, String file_name) {
        
        this.file_name = file_name;
        this.lock_status = false;
        this.last_change_time = new SimpleDateFormat("dd-M-YYYY|HH:mm:ss").format(new Date(System.currentTimeMillis()));
        this.version = this.hashFiles(path, file_name);
        this.file_version = this.version + " " + last_change_time;
    }
    
    //getters and setters

    public void setVersion() {

        this.version = this.hashFiles(Constants.TEMP_DIR_PATH, this.file_name);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLocked_by() {
        return locked_by;
    }

    public void setLocked_by(String locked_by) {
        this.locked_by = locked_by;
    }

    public String getFile_version() {
        this.file_version = version + " " + last_change_time;
        return file_version;
    }

    public boolean isLock_status() {
        return lock_status;
    }

    public void setLock_status(boolean lock_status) {
        this.lock_status = lock_status;
    }

    public String getLast_change_time() {
        return last_change_time;
    }

    public void setLast_change_time(String last_change_time) {
        this.last_change_time = last_change_time;
    }

    public void setFile_version(String file_version) {
        this.file_version = file_version;
    }

    /**
     * hashing the file content into ID code status: working
     *
     * @param file_name
     * @return ArrayList<String> of the codes
     */
    public String hashFiles(String path, String file_name) {

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }

        // file hashing with DigestInputStream
        DigestInputStream dis;
        try {
            dis = new DigestInputStream(new FileInputStream(path + file_name), md);
            while (dis.read() != -1) ; //empty loop to clear the data
            md = dis.getMessageDigest();

            // bytes to hex
            StringBuilder result = new StringBuilder();
            for (byte b : md.digest()) {
                result.append(String.format("%02x", b));
            }
            dis.close();
            return result.toString().toUpperCase();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public String getFile_name() {
        return file_name;
    }
}