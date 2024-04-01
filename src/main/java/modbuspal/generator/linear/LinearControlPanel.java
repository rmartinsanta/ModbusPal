/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * LinearControlPanel.java
 *
 * Created on 22 déc. 2008, 22:01:06
 */

package modbuspal.generator.linear;

/**
 * the control panel for the linear generator
 * @author nnovic
 */
public class LinearControlPanel
extends javax.swing.JPanel
{
    private final LinearGenerator generator;
    
    /** Creates new form LinearControlPanel
     * @param gen the linear generator whose parameters are being displayed by this component
     */
    public LinearControlPanel(LinearGenerator gen)
    {
        generator = gen;
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        startTextField = new javax.swing.JTextField();
        endTextField = new javax.swing.JTextField();
        startRelativeCheckBox = new javax.swing.JCheckBox();
        endRelativeCheckBox = new javax.swing.JCheckBox();

        setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Start value:");
        add(jLabel1, new java.awt.GridBagConstraints());

        jLabel2.setText("End value:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        add(jLabel2, gridBagConstraints);

        startTextField.setText(String.valueOf(generator.startValue));
        startTextField.setPreferredSize(new java.awt.Dimension(60, 20));
        startTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                startTextFieldFocusLost(evt);
            }
        });
        add(startTextField, new java.awt.GridBagConstraints());

        endTextField.setText(String.valueOf(generator.endValue));
        endTextField.setPreferredSize(new java.awt.Dimension(60, 20));
        endTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                endTextFieldFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        add(endTextField, gridBagConstraints);

        startRelativeCheckBox.setSelected(generator.relativeStart);
        startRelativeCheckBox.setText("relative");
        startRelativeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startRelativeCheckBoxActionPerformed(evt);
            }
        });
        add(startRelativeCheckBox, new java.awt.GridBagConstraints());

        endRelativeCheckBox.setSelected(generator.relativeEnd);
        endRelativeCheckBox.setText("relative");
        endRelativeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endRelativeCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        add(endRelativeCheckBox, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void endTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_endTextFieldFocusLost
        generator.endValue = Double.parseDouble( endTextField.getText() );
    }//GEN-LAST:event_endTextFieldFocusLost

    private void startTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_startTextFieldFocusLost
        generator.startValue = Double.parseDouble( startTextField.getText() );
    }//GEN-LAST:event_startTextFieldFocusLost

    private void startRelativeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startRelativeCheckBoxActionPerformed
        generator.relativeStart = startRelativeCheckBox.isSelected();
    }//GEN-LAST:event_startRelativeCheckBoxActionPerformed

    private void endRelativeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_endRelativeCheckBoxActionPerformed
        generator.relativeEnd = endRelativeCheckBox.isSelected();
    }//GEN-LAST:event_endRelativeCheckBoxActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JCheckBox endRelativeCheckBox;
    javax.swing.JTextField endTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    javax.swing.JCheckBox startRelativeCheckBox;
    javax.swing.JTextField startTextField;
    // End of variables declaration//GEN-END:variables

}
