/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * AddSlaveDialog.java
 *
 * Created on 20 déc. 2008, 12:07:28
 */

package modbuspal.main;

import modbuspal.help.HelpViewer;
import modbuspal.master.ModbusMasterTarget;
import modbuspal.slave.ModbusSlaveAddress;
import modbuspal.slave.ModbusSlaveAddressParsers;
import modbuspal.toolkit.GUITools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * a dialog in which the user defines new modbus slave to add into the project
 *
 * @author nnovic
 */
public class AddSlaveDialog
        extends javax.swing.JDialog {
    private boolean added = false;

    /**
     * Creates new form AddSlaveDialog. This constructor is usually
     * called when the user add a new slave in the project.
     */
    public AddSlaveDialog() {
        this(null);
        setTitle("New slave");
    }

    /**
     * Creates new form AddSlaveDialog. This constructor is usually
     * called when the user duplicates a modbus slave.
     *
     * @param name suggested name for the modbus slave(s) to add
     */
    public AddSlaveDialog(String name) {
        setModalityType(ModalityType.DOCUMENT_MODAL);

        initComponents();

        setTitle("Duplicate " + name);
        GUITools.underline(rtuSlaveHelpLabel);
        GUITools.underline(tcpSlaveHelpLabel);
        //GUITools.align(parent,this);

        if (name != null) {
            nameTextField.setText(name);
        }
    }

    /**
     * Indicates that the user has validated the selection by clicking
     * on the ok button.
     *
     * @return true if the user has validate the selection by clicking on the
     * ok button
     */
    public boolean isAdded() {
        return added;
    }


    private List<ModbusSlaveAddress> parseSlaveIds() {
        ArrayList<ModbusSlaveAddress> output = new ArrayList<>();
        String rawList = slavesTextArea.getText();
        String[] chunks = rawList.split("[,\r\n]+");

        for (String chunk : chunks) {
            output.addAll(ModbusSlaveAddressParsers.tryAnyParser(chunk));
        }

        return output;
    }


    /**
     * Gets the list of the slaves to add in the project, identified by their
     * slave numbers
     *
     * @return list of the modbus slave numbers to create in the project
     */
    public ModbusSlaveAddress[] getTargetList() {
        List<ModbusSlaveAddress> list = parseSlaveIds();
        ModbusSlaveAddress[] output = new ModbusSlaveAddress[0];
        return list.toArray(output);
        
        /*Object sel[] = slaveIdList.getSelectedValues();
        int ids[] = new int[sel.length];

        for( int i=0; i<sel.length; i++)
        {
            ids[i] = (Integer)sel[i];
        }
        return ids;*/
    }

    /**
     * Gets the slave name that has been typed by the user and shall be
     * used for the modbus slave(s) to create in the project
     *
     * @return slave name typed by the user
     */
    public String getTargetName() {
        return nameTextField.getText();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        addButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        nameTextField = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        slavesTextArea = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        rtuSlaveHelpLabel = new javax.swing.JLabel();
        tcpSlaveHelpLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("New slave");
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        addButton.setText("Add");
        addButton.addActionListener(evt -> addButtonActionPerformed(evt));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 2, 10, 10);
        getContentPane().add(addButton, gridBagConstraints);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(evt -> cancelButtonActionPerformed(evt));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 2, 10, 2);
        getContentPane().add(cancelButton, gridBagConstraints);

        jLabel1.setText("Add slave:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 2, 2);
        getContentPane().add(jLabel1, gridBagConstraints);

        jLabel2.setText("Slave name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 10, 2);
        getContentPane().add(jLabel2, gridBagConstraints);

        nameTextField.setText("unknown slave");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 10, 2);
        getContentPane().add(nameTextField, gridBagConstraints);

        slavesTextArea.setColumns(20);
        slavesTextArea.setRows(5);
        jScrollPane2.setViewportView(slavesTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 2, 2, 2);
        getContentPane().add(jScrollPane2, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridLayout(0, 1, 5, 5));

        rtuSlaveHelpLabel.setForeground(new java.awt.Color(0, 0, 255));
        rtuSlaveHelpLabel.setText("How to define MODBUS RTU slaves...");
        rtuSlaveHelpLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        rtuSlaveHelpLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rtuSlaveHelpLabelMouseClicked(evt);
            }
        });
        jPanel1.add(rtuSlaveHelpLabel);

        tcpSlaveHelpLabel.setForeground(new java.awt.Color(0, 0, 255));
        tcpSlaveHelpLabel.setText("How to define MODBUS TCP slaves...");
        tcpSlaveHelpLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        tcpSlaveHelpLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tcpSlaveHelpLabelMouseClicked(evt);
            }
        });
        jPanel1.add(tcpSlaveHelpLabel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        getContentPane().add(jPanel1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        added = true;
        setVisible(false);
    }//GEN-LAST:event_addButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        added = false;
        setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void rtuSlaveHelpLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rtuSlaveHelpLabelMouseClicked
        try {
            HelpViewer helpViewer = HelpViewer.open("modbus-slaves-add-rtu.html");
        } catch (IOException ex) {
            Logger.getLogger(AddSlaveDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_rtuSlaveHelpLabelMouseClicked

    private void tcpSlaveHelpLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tcpSlaveHelpLabelMouseClicked
        try {
            HelpViewer helpViewer = HelpViewer.open("modbus-slaves-add-tcp.html");
        } catch (IOException ex) {
            Logger.getLogger(AddSlaveDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_tcpSlaveHelpLabelMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField nameTextField;
    private javax.swing.JLabel rtuSlaveHelpLabel;
    private javax.swing.JTextArea slavesTextArea;
    private javax.swing.JLabel tcpSlaveHelpLabel;
    // End of variables declaration//GEN-END:variables

    public void initializeWith(ModbusMasterTarget mmt) {
        slavesTextArea.setText(mmt.getTargetListAsText());
        nameTextField.setText(mmt.getTargetName());
    }

    public String getTargetListAsText() {
        return slavesTextArea.getText();
    }
}
