/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * HelpViewer.java
 *
 * Created on 18 avr. 2011, 10:36:59
 */

package modbuspal.help;

import modbuspal.toolkit.FileTools;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

/**
 * The JFrame that contains the HelpViewerPane
 * @author nnovic
 */
public class HelpViewer
extends javax.swing.JFrame
implements HyperlinkListener
{

    private final HashMap<String,HelpViewerPane> panes = new HashMap<String,HelpViewerPane>();

    /** Creates new form HelpViewer */
    public HelpViewer() 
    {
        initComponents();
        setIconImage(FileTools.getImage("/img/icon32.png"));

        try
        {
            URL url = getClass().getResource("index.html");
            addTab("Index",url);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void addTab(String title, URL url) throws IOException
    {
        HelpViewerPane pane = panes.get(title);
        if(pane==null)
        {
            pane = new HelpViewerPane(url);
            jTabbedPane1.add(title, pane);
            panes.put(title,pane);
            pane.addHyperlinkListener(this);
        }
        jTabbedPane1.setSelectedComponent(pane);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();

        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    public static HelpViewer open(String file) 
    throws IOException
    {
        HelpViewer helpViewer = new HelpViewer();
        
        URL url = HelpViewer.class.getResource(file);
        helpViewer.openURL(url);
        
        helpViewer.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        helpViewer.setExtendedState(JFrame.MAXIMIZED_BOTH);

        helpViewer.setVisible(true);
        helpViewer.toFront();
        
        return helpViewer;
    }
    
    
    /**
    * @param args the command line arguments
    */
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                HelpViewer hv = new HelpViewer();
                hv.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                hv.setExtendedState(MAXIMIZED_BOTH);
                hv.setVisible(true);
                
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables

    
    private void openURL(URL url) 
    throws IOException
    {
        String file = url.getFile();
        String[] parts = file.split("[/\\\\]");
        addTab(parts[parts.length-1], url);
    }
    
    
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            try
            {
                openURL(e.getURL());
            }
            catch(IOException ioe)
            {
                // Some warning to user
            }
        }
    }
}
