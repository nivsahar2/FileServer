
import java.awt.Component;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
/**
 *
 * @author NivSahar and Fadi Zaher
 */
public class ClientGUI_1 extends javax.swing.JFrame {

    // dialog for chosing a file
    JFileChooser openFile = null;
    // array list for all the servers from the configuration file
    ArrayList<String> Servers = new ArrayList<String>();
    File file;

    /**
     * Creates new form ClientGUI
     */
    public ClientGUI_1() {
        initComponents();
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        openFile = new JFileChooser();
        // combo box holding the all operations we can do
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"UPLOAD", "LOCK", "UNLOCK", "GETVERSION", "GETLIST", "DOWNLOAD",  "GETVERSIONALL", "GETLISTALL"}));
        txtServerList2.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = {""};

            public int getSize() {
                return strings.length;
            }

            public String getElementAt(int i) {
                return strings[i];
            }
        });
        Servers = getServers();
        // send the objects to the Client static class
        Client.InitializeObjects(txtArea, txtServerList, txtChosenFile, txtServerList2, openFile);
        // create the communication between the client and the server
        Client.Communication();

    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jLabel3 = new javax.swing.JLabel();
        btnExecute = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtServerList = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtArea = new javax.swing.JTextArea();
        txtChosenFile = new javax.swing.JTextField();
        btnChooseFile = new javax.swing.JButton();
        btnQuit = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox<>();
        btnReconnect = new javax.swing.JButton();
        lblServerList = new javax.swing.JLabel();
        lblFilesList = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtServerList2 = new javax.swing.JList<>();
        jButton1 = new javax.swing.JButton();

        jLabel3.setText("jLabel3");

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        btnExecute.setText("Execute");
        btnExecute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExecuteAction(evt);
            }
        });

        txtServerList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        txtServerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(txtServerList);

        jLabel2.setFont(new java.awt.Font("Agency FB", 1, 24)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 51, 51));
        jLabel2.setText("Output");
        jLabel2.setBorder(new javax.swing.border.MatteBorder(null));
        jLabel2.setPreferredSize(new java.awt.Dimension(60, 16));

        txtArea.setColumns(20);
        txtArea.setRows(5);
        jScrollPane3.setViewportView(txtArea);

        btnChooseFile.setText("Choose File");
        btnChooseFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ChooseFileAction(evt);
            }
        });

        btnQuit.setText("Quit");
        btnQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                QuitAction(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnReconnect.setText("Reconnect");
        btnReconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReconnectAction(evt);
            }
        });

        lblServerList.setText("Servers List");

        lblFilesList.setText("Files List");

        txtServerList2.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        txtServerList2.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(txtServerList2);
        txtServerList2.getAccessibleContext().setAccessibleName("jScrollPane2");

        jButton1.setActionCommand("ClearList");
        jButton1.setLabel("Clear List");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearListActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtChosenFile, javax.swing.GroupLayout.PREFERRED_SIZE, 249, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnExecute, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnChooseFile, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblFilesList)
                .addGap(104, 104, 104)
                .addComponent(lblServerList)
                .addGap(52, 52, 52))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 472, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(btnQuit, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(67, 67, 67)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnReconnect)
                        .addGap(28, 28, 28)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtChosenFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnChooseFile)
                            .addComponent(lblFilesList)
                            .addComponent(lblServerList))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(btnExecute)
                                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnReconnect)
                            .addComponent(jButton1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnQuit))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        btnExecute.getAccessibleContext().setAccessibleName("downloadFileButton");
        jButton1.getAccessibleContext().setAccessibleName("ClearList");

        pack();
    }// </editor-fold>                        

    
    /*
    * This function will execute triggered by the execute button
    * it will execute function in the client static class with the current operation to do
    * and send the file that chosen
    */
    private void ExecuteAction(java.awt.event.ActionEvent evt) {                               
        try {

            Client.ExecuteFromGUI(jComboBox1.getSelectedItem().toString(), file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Can execute commad", "InfoBox: " + "ERROR", JOptionPane.INFORMATION_MESSAGE);
        }
        txtChosenFile.setText("");

    }                              

    // the trigger for the option to chose a afile
    private void ChooseFileAction(java.awt.event.ActionEvent evt) {                                  
        JFileChooser openFile = new JFileChooser();
        openFile.showOpenDialog(null);
        try {
            txtChosenFile.setText(String.valueOf(openFile.getSelectedFile().getName()));
            file = openFile.getSelectedFile();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please choose a file again!", "InfoBox: " + "Empty text box", JOptionPane.INFORMATION_MESSAGE);
        }

    }                                 

    // exit the client by pressing Quit button
    private void QuitAction(java.awt.event.ActionEvent evt) {                            
        // TODO add your handling code here:
        Client.Quit();
    }                           

    // function that handling the reconnection  to the server
    private void ReconnectAction(java.awt.event.ActionEvent evt) {                                 
        try {
            // get all the details of the server
            String IP = Client.getHost_IP(txtServerList.getSelectedValue());
            String NAME = Client.getHost_NAME(txtServerList.getSelectedValue());
            int PORT = Client.getHost_PORT(txtServerList.getSelectedValue());
            Socket socket = Client.connectToServer(IP, PORT, NAME);
            Client.Servers_sockets.put(NAME, socket);
        } catch (NullPointerException e) {
            JOptionPane.showMessageDialog(null, "Please choose a Server!", "InfoBox: " + "Empty text box", JOptionPane.INFORMATION_MESSAGE);
        }
    }                                
    // clear the file list
    private void ClearListActionPerformed(java.awt.event.ActionEvent evt) {                                          
        // TODO add your handling code here:
        DefaultListModel tempList = new DefaultListModel();
        
        txtServerList2.setModel(tempList);
    }                                         

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ClientGUI_1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ClientGUI_1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ClientGUI_1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ClientGUI_1.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ClientGUI_1().setVisible(true);
            }
        });
    }

    // this function get the servers from the configuration file once th GUI client upload
    public ArrayList<String> getServers() {

        ArrayList<String> Servers = new ArrayList<String>();
        ArrayList<String> Full_Servers = new ArrayList<String>();
        String current;
        try {
            // read the configuration file and add this to the full_server array list
            File myObj = new File(System.getProperty("user.dir") + "\\Servers.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                current = myReader.nextLine();
                Full_Servers.add(current);
                String splitted[] = current.split("\\s+");
                Servers.add(splitted[0]);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        txtServerList.setModel(new javax.swing.AbstractListModel<String>() {
            public int getSize() {
                return Servers.size();
            }

            public String getElementAt(int i) {
                return Servers.get(i);
            }
        });

        Client.InitializeServers(Full_Servers);

        return Servers;
    }


    // Variables declaration - do not modify                     
    private javax.swing.JButton btnChooseFile;
    private javax.swing.JButton btnExecute;
    private javax.swing.JButton btnQuit;
    private javax.swing.JButton btnReconnect;
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lblFilesList;
    private javax.swing.JLabel lblServerList;
    private javax.swing.JTextArea txtArea;
    private javax.swing.JTextField txtChosenFile;
    private javax.swing.JList<String> txtServerList;
    private javax.swing.JList<String> txtServerList2;
    // End of variables declaration                   

}
