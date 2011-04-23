/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ModbusPalGui.java
 *
 * Created on 16 déc. 2008, 08:35:06
 */

package modbuspal.main;

import java.awt.CardLayout;
import javax.xml.parsers.ParserConfigurationException;
import modbuspal.automation.AutomationPanel;
import modbuspal.toolkit.NumericTextField;
import modbuspal.slave.ModbusSlavePanel;
import java.awt.Component;
import java.awt.Dialog.ModalExclusionType;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import modbuspal.automation.Automation;
import modbuspal.help.HelpViewer;
import modbuspal.link.*;
import modbuspal.master.ModbusMaster;
import modbuspal.master.ModbusMasterDialog;
import modbuspal.recorder.ModbusPalRecorder;
import modbuspal.script.ScriptManagerDialog;
import modbuspal.slave.ModbusSlave;
import modbuspal.toolkit.GUITools;
import modbuspal.toolkit.XFileChooser;
import org.xml.sax.SAXException;

/**
 *
 * @author nnovic
 */
public class ModbusPalPane
extends JPanel
implements ModbusPalXML, WindowListener, ModbusPalListener, ModbusLinkListener
{
    public static final String APP_STRING = "ModbusPal 1.6";
    public static final String BASE_REGISTRY_KEY = "modbuspal";

    private ArrayList<ModbusPalProjectListener> listeners = new ArrayList<ModbusPalProjectListener>();
        
    private ModbusMaster modbusMaster = new ModbusMaster();
    private ModbusMasterDialog modbusMasterDialog = null;
    ScriptManagerDialog scriptManagerDialog = null;
    private ModbusLink currentLink = null;
    private AppConsole console = null;
    ModbusPalProject modbusPalProject;
    private HelpViewer helpViewer = null;


    public void addModbusPalProjectListener(ModbusPalProjectListener l)
    {
        if(listeners.contains(l)==false)
        {
            listeners.add(l);
        }
    }

    public void removeModbusPalProjectListener(ModbusPalProjectListener l)
    {
        if(listeners.contains(l)==true)
        {
            listeners.remove(l);
        }
    }



    public void setProject(ModbusPalProject project)
    {
        System.out.printf("Set project %s\r\n", project.getName());

        //- - - - - - - - - - - - - - -
        // Clear existing project
        //- - - - - - - - - - - - - - -

        if( modbusPalProject!=null )
        {
            modbusPalProject.removeModbusPalListener(this);
        }


        //- - - - - - - - - - - - - -
        // Register new project
        //- - - - - - - - - - - - - -

        ModbusPalProject old = modbusPalProject;
        modbusPalProject = project;
        modbusPalProject.addModbusPalListener(this);


        //- - - - - - - - - - - - - -
        // Refresh GUI
        //- - - - - - - - - - - - - -

        // Select the link tab:
        for( int i=0; i<linksTabbedPane.getComponentCount(); i++ ) {
            if( linksTabbedPane.getTitleAt(i).compareTo( project.selectedLink )==0 ) {
                linksTabbedPane.setSelectedIndex(i);
            }
        }

        // Init tcp/ip settings
        portTextField.setText( project.linkTcpipPort );

        // Init serial settings
        comPortComboBox.setSelectedItem( project.linkSerialComId );
        baudRateComboBox.setSelectedItem( project.linkSerialBaudrate );
        parityComboBox.setSelectedIndex( project.linkSerialParity );
        xonxoffCheckBox.setSelected( project.linkSerialXonXoff );
        rtsctsCheckBox.setSelected( project.linkSerialRtsCts );

        // Init record/replay settings
        setReplayFile(project.linkReplayFile);

        
        //- - - - - - - - - - - - - -
        // Refresh list of slaves
        //- - - - - - - - - - - - - -

        slavesListPanel.removeAll();
        for(int i=0; i<ModbusConst.MAX_MODBUS_SLAVE; i++)
        {
            if( project.getModbusSlave(i)!=null )
            {
                modbusSlaveAdded(project.getModbusSlave(i));
            }
        }

        //- - - - - - - - - - - - - -
        // Refresh list of automations
        //- - - - - - - - - - - - - -

        automationsListPanel.removeAll();
        Automation[] automations = project.getAutomations();
        for(int i=0; i<automations.length; i++)
        {
            automationAdded(automations[i], i);
        }

        //- - - - - - - - - - - - -
        // Refresh list of scripts
        //- - - - - - - - - - - - -

        if(scriptManagerDialog!=null)
        {
            scriptManagerDialog.setProject(modbusPalProject);
        }
        
        System.out.printf("[%s] Project set\r\n", modbusPalProject.getName());

        notifyModbusPalProjectChanged(old, modbusPalProject);

        //- - - - - - - - - - -
        // Refresh Display
        //- - - - - - - - - - -

        validate();
        repaint();
    }



    private void notifyModbusPalProjectChanged(ModbusPalProject oldProject, ModbusPalProject newProject)
    {
        for(ModbusPalProjectListener l:listeners)
        {
            l.modbusPalProjectChanged(oldProject, newProject);
        }
    }


    void saveProject() throws FileNotFoundException, IOException
    {
        System.out.printf("[%s] Save project\r\n", modbusPalProject.getName());

        // update selected link tab:
        int index = linksTabbedPane.getSelectedIndex();
        modbusPalProject.selectedLink = linksTabbedPane.getTitleAt(index);

        // update tcp/ip settings
        modbusPalProject.linkTcpipPort = portTextField.getText();

        // update serial settings
        modbusPalProject.linkSerialComId = (String)comPortComboBox.getSelectedItem();
        modbusPalProject.linkSerialBaudrate = (String)baudRateComboBox.getSelectedItem();
        modbusPalProject.linkSerialParity = parityComboBox.getSelectedIndex();
        modbusPalProject.linkSerialXonXoff = xonxoffCheckBox.isSelected();
        modbusPalProject.linkSerialRtsCts = rtsctsCheckBox.isSelected();

        // update record/replay settings
        modbusPalProject.linkReplayFile = (File)chosenRecordFileTextField.getClientProperty("record file");

        modbusPalProject.save();
    }



    private void setReplayFile(File src)
    {
        if( src!=null )
        {
            chosenRecordFileTextField.setText( src.getPath() );
            chosenRecordFileTextField.putClientProperty("record file", src);
        }
        else
        {
            chosenRecordFileTextField.setText(null);
            chosenRecordFileTextField.putClientProperty("record file", null);
        }
    }















    /** Creates new form ModbusPalGui */
    ModbusPalPane(boolean useInternalConsole)
    {
        initComponents();

        if(useInternalConsole)
        {
            installConsole();
            consoleToggleButton.setEnabled(true);
        }
        else
        {
            consoleToggleButton.setToolTipText("Console is disabled");
        }

        installRecorder();
        installCommPorts();
        installScriptEngine();

        setProject( new ModbusPalProject() );

    }


    private void installConsole()
    {
        try {
            console = new AppConsole();
            console.addWindowListener(this);
        } catch (IOException ex) {
            Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private boolean verifyRXTX()
    {
        // try to load the CommPortVerifier class
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try
        {
            Class c = cl.loadClass("gnu.io.CommPortIdentifier");
            return true;
        }
        catch (ClassNotFoundException ex)
        {
            return false;
        }
    }

    private boolean verifyPython()
    {
        // try to load the CommPortVerifier class
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try
        {
            Class c = cl.loadClass("org.python.util.PythonInterpreter");
            return true;
        }
        catch (ClassNotFoundException ex)
        {
            return false;
        }
    }

    private void installScriptEngine()
    {

        if( verifyPython() == true )
        {
            scriptManagerDialog = new ScriptManagerDialog();
            scriptManagerDialog.addWindowListener(this);
        }
        else
        {
            scriptManagerDialog = null;
        }
    }

    private void installRecorder()
    {
        ModbusPalRecorder.touch();
    }

    private void installCommPorts()
    {
        // check if RXTX Comm lib is available
        if( verifyRXTX() == false )
        {
            CardLayout layout = (CardLayout)jPanel1.getLayout();
            layout.show(jPanel1, "disabled");
            return;
        }

        // detect the comm ports
        ModbusSerialLink.install();
        
        // get the list of comm ports (as strings)
        // and put it in the swing list
        comPortComboBox.setModel( ModbusSerialLink.getListOfCommPorts() );
    }
    
   



    @Override
    public void pduProcessed()
    {
        ( (TiltLabel)tiltLabel ).tilt(TiltLabel.GREEN);
    }


    @Override
    public void pduException()
    {
        ( (TiltLabel)tiltLabel ).tilt(TiltLabel.RED);
    }


    @Override
    public void pduNotServiced()
    {
        ( (TiltLabel)tiltLabel ).tilt(TiltLabel.YELLOW);
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

        jPanel6 = new javax.swing.JPanel();
        settingsPanel1 = new javax.swing.JPanel();
        linkPanel1 = new javax.swing.JPanel();
        linksTabbedPane1 = new javax.swing.JTabbedPane();
        tcpIpSettingsPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        portTextField1 = new NumericTextField();
        jPanel7 = new javax.swing.JPanel();
        serialSettingsPanel1 = new javax.swing.JPanel();
        comPortComboBox1 = new javax.swing.JComboBox();
        baudRateComboBox1 = new javax.swing.JComboBox();
        parityComboBox1 = new javax.swing.JComboBox();
        xonxoffCheckBox1 = new javax.swing.JCheckBox();
        rtsctsCheckBox1 = new javax.swing.JCheckBox();
        jPanel8 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        replaySettingsPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        recordFileChooseButton1 = new javax.swing.JButton();
        chosenRecordFileTextField1 = new javax.swing.JTextField();
        runPanel1 = new javax.swing.JPanel();
        runToggleButton1 = new javax.swing.JToggleButton();
        learnToggleButton1 = new javax.swing.JToggleButton();
        tiltLabel1 = new TiltLabel();
        recordToggleButton1 = new javax.swing.JToggleButton();
        asciiToggleButton1 = new javax.swing.JToggleButton();
        projectPanel1 = new javax.swing.JPanel();
        loadButton1 = new javax.swing.JButton();
        saveProjectButton1 = new javax.swing.JButton();
        clearProjectButton1 = new javax.swing.JButton();
        saveProjectAsButton1 = new javax.swing.JButton();
        toolsPanel1 = new javax.swing.JPanel();
        masterToggleButton1 = new javax.swing.JToggleButton();
        scriptsToggleButton1 = new javax.swing.JToggleButton();
        helpButton1 = new javax.swing.JButton();
        consoleToggleButton1 = new javax.swing.JToggleButton();
        jSplitPane2 = new javax.swing.JSplitPane();
        slavesListView1 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        addModbusSlaveButton1 = new javax.swing.JButton();
        enableAllSlavesButton1 = new javax.swing.JButton();
        disableAllSlavesButton1 = new javax.swing.JButton();
        slaveListScrollPane1 = new javax.swing.JScrollPane();
        slavesListPanel1 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        addAutomationButton1 = new javax.swing.JButton();
        startAllAutomationsButton1 = new javax.swing.JButton();
        stopAllAutomationsButton1 = new javax.swing.JButton();
        automationListScrollPane1 = new javax.swing.JScrollPane();
        automationsListPanel1 = new javax.swing.JPanel();
        settingsPanel = new javax.swing.JPanel();
        linkPanel = new javax.swing.JPanel();
        linksTabbedPane = new javax.swing.JTabbedPane();
        tcpIpSettingsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        portTextField = new NumericTextField();
        jPanel1 = new javax.swing.JPanel();
        serialSettingsPanel = new javax.swing.JPanel();
        comPortComboBox = new javax.swing.JComboBox();
        baudRateComboBox = new javax.swing.JComboBox();
        parityComboBox = new javax.swing.JComboBox();
        xonxoffCheckBox = new javax.swing.JCheckBox();
        rtsctsCheckBox = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        replaySettingsPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        recordFileChooseButton = new javax.swing.JButton();
        chosenRecordFileTextField = new javax.swing.JTextField();
        runPanel = new javax.swing.JPanel();
        runToggleButton = new javax.swing.JToggleButton();
        learnToggleButton = new javax.swing.JToggleButton();
        tiltLabel = new TiltLabel();
        recordToggleButton = new javax.swing.JToggleButton();
        asciiToggleButton = new javax.swing.JToggleButton();
        projectPanel = new javax.swing.JPanel();
        loadButton = new javax.swing.JButton();
        saveProjectButton = new javax.swing.JButton();
        clearProjectButton = new javax.swing.JButton();
        saveProjectAsButton = new javax.swing.JButton();
        toolsPanel = new javax.swing.JPanel();
        masterToggleButton = new javax.swing.JToggleButton();
        scriptsToggleButton = new javax.swing.JToggleButton();
        helpButton = new javax.swing.JButton();
        consoleToggleButton = new javax.swing.JToggleButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        slavesListView = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        addModbusSlaveButton = new javax.swing.JButton();
        enableAllSlavesButton = new javax.swing.JButton();
        disableAllSlavesButton = new javax.swing.JButton();
        slaveListScrollPane = new javax.swing.JScrollPane();
        slavesListPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        addAutomationButton = new javax.swing.JButton();
        startAllAutomationsButton = new javax.swing.JButton();
        stopAllAutomationsButton = new javax.swing.JButton();
        automationListScrollPane = new javax.swing.JScrollPane();
        automationsListPanel = new javax.swing.JPanel();

        jPanel6.setLayout(new java.awt.BorderLayout());

        settingsPanel1.setLayout(new java.awt.GridBagLayout());

        linkPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Link settings"));
        linkPanel1.setLayout(new java.awt.GridBagLayout());

        tcpIpSettingsPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel4.setText("TCP Port:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 2);
        tcpIpSettingsPanel1.add(jLabel4, gridBagConstraints);

        portTextField1.setText("502");
        portTextField1.setPreferredSize(new java.awt.Dimension(40, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 5, 5);
        tcpIpSettingsPanel1.add(portTextField1, gridBagConstraints);

        linksTabbedPane1.addTab("TCP/IP", tcpIpSettingsPanel1);

        jPanel7.setLayout(new java.awt.CardLayout());

        serialSettingsPanel1.setLayout(new java.awt.GridBagLayout());

        comPortComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "COM 1" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 2, 2);
        serialSettingsPanel1.add(comPortComboBox1, gridBagConstraints);

        baudRateComboBox1.setEditable(true);
        baudRateComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "115200", "57600", "19200", "9600" }));
        baudRateComboBox1.setSelectedIndex(2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 2, 5);
        serialSettingsPanel1.add(baudRateComboBox1, gridBagConstraints);

        parityComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No parity", "Odd parity", "Even parity" }));
        parityComboBox1.setSelectedIndex(2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 2);
        serialSettingsPanel1.add(parityComboBox1, gridBagConstraints);

        xonxoffCheckBox1.setText("XON/XOFF");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 5, 2);
        serialSettingsPanel1.add(xonxoffCheckBox1, gridBagConstraints);

        rtsctsCheckBox1.setText("RTS/CTS");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 5, 5);
        serialSettingsPanel1.add(rtsctsCheckBox1, gridBagConstraints);

        jPanel7.add(serialSettingsPanel1, "enabled");

        jPanel8.setLayout(new java.awt.GridBagLayout());

        jLabel5.setText("Serial communication is disabled.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel8.add(jLabel5, gridBagConstraints);

        jButton2.setText("Why?");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel8.add(jButton2, gridBagConstraints);

        jPanel7.add(jPanel8, "disabled");

        linksTabbedPane1.addTab("Serial", jPanel7);

        replaySettingsPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel6.setText("Record file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 2, 2);
        replaySettingsPanel1.add(jLabel6, gridBagConstraints);

        recordFileChooseButton1.setText("Choose...");
        recordFileChooseButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recordFileChooseButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 2, 5);
        replaySettingsPanel1.add(recordFileChooseButton1, gridBagConstraints);

        chosenRecordFileTextField1.setEditable(false);
        chosenRecordFileTextField1.setText("No file selected.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 5, 5);
        replaySettingsPanel1.add(chosenRecordFileTextField1, gridBagConstraints);

        linksTabbedPane1.addTab("Replay", replaySettingsPanel1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        linkPanel1.add(linksTabbedPane1, gridBagConstraints);

        runPanel1.setLayout(new java.awt.GridBagLayout());

        runToggleButton1.setText("Run");
        runToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runToggleButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        runPanel1.add(runToggleButton1, gridBagConstraints);

        learnToggleButton1.setText("Learn");
        learnToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                learnToggleButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        runPanel1.add(learnToggleButton1, gridBagConstraints);

        tiltLabel1.setText("X");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        runPanel1.add(tiltLabel1, gridBagConstraints);

        recordToggleButton1.setText("Record");
        recordToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recordToggleButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        runPanel1.add(recordToggleButton1, gridBagConstraints);

        asciiToggleButton1.setText("Ascii");
        asciiToggleButton1.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        runPanel1.add(asciiToggleButton1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        linkPanel1.add(runPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        settingsPanel1.add(linkPanel1, gridBagConstraints);

        projectPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Project"));
        projectPanel1.setLayout(new java.awt.GridBagLayout());

        loadButton1.setText("Load");
        loadButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectPanel1.add(loadButton1, gridBagConstraints);

        saveProjectButton1.setText("Save");
        saveProjectButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjectButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectPanel1.add(saveProjectButton1, gridBagConstraints);

        clearProjectButton1.setText("Clear");
        clearProjectButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearProjectButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectPanel1.add(clearProjectButton1, gridBagConstraints);

        saveProjectAsButton1.setText("Save as");
        saveProjectAsButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjectAsButton1saveProjectButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectPanel1.add(saveProjectAsButton1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        settingsPanel1.add(projectPanel1, gridBagConstraints);

        toolsPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Tools"));
        toolsPanel1.setLayout(new java.awt.GridBagLayout());

        masterToggleButton1.setText("Master");
        masterToggleButton1.setEnabled(false);
        masterToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                masterToggleButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        toolsPanel1.add(masterToggleButton1, gridBagConstraints);

        scriptsToggleButton1.setText("Scripts");
        scriptsToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scriptsToggleButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        toolsPanel1.add(scriptsToggleButton1, gridBagConstraints);

        helpButton1.setText("Help");
        helpButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        toolsPanel1.add(helpButton1, gridBagConstraints);

        consoleToggleButton1.setText("Console");
        consoleToggleButton1.setEnabled(false);
        consoleToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleToggleButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        toolsPanel1.add(consoleToggleButton1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        settingsPanel1.add(toolsPanel1, gridBagConstraints);

        jPanel6.add(settingsPanel1, java.awt.BorderLayout.NORTH);

        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        slavesListView1.setBorder(javax.swing.BorderFactory.createTitledBorder("Modbus slaves"));
        slavesListView1.setLayout(new java.awt.BorderLayout());

        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addModbusSlaveButton1.setText("Add");
        addModbusSlaveButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addModbusSlaveButton1ActionPerformed(evt);
            }
        });
        jPanel9.add(addModbusSlaveButton1);

        enableAllSlavesButton1.setText("Enable all");
        enableAllSlavesButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableAllSlavesButton1ActionPerformed(evt);
            }
        });
        jPanel9.add(enableAllSlavesButton1);

        disableAllSlavesButton1.setText("Disable all");
        disableAllSlavesButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disableAllSlavesButton1ActionPerformed(evt);
            }
        });
        jPanel9.add(disableAllSlavesButton1);

        slavesListView1.add(jPanel9, java.awt.BorderLayout.NORTH);

        slaveListScrollPane1.setPreferredSize(new java.awt.Dimension(300, 150));

        slavesListPanel1.setBackground(javax.swing.UIManager.getDefaults().getColor("List.background"));
        slavesListPanel1.setLayout(null);
        slavesListPanel.setLayout( new ListLayout() );
        slaveListScrollPane1.setViewportView(slavesListPanel1);

        slavesListView1.add(slaveListScrollPane1, java.awt.BorderLayout.CENTER);

        jSplitPane2.setLeftComponent(slavesListView1);

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("Automation"));
        jPanel10.setLayout(new java.awt.BorderLayout());

        jPanel11.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addAutomationButton1.setText("Add");
        addAutomationButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAutomationButton1ActionPerformed(evt);
            }
        });
        jPanel11.add(addAutomationButton1);

        startAllAutomationsButton1.setText("Start all");
        startAllAutomationsButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startAllAutomationsButton1ActionPerformed(evt);
            }
        });
        jPanel11.add(startAllAutomationsButton1);

        stopAllAutomationsButton1.setText("Stop all");
        stopAllAutomationsButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopAllAutomationsButton1ActionPerformed(evt);
            }
        });
        jPanel11.add(stopAllAutomationsButton1);

        jPanel10.add(jPanel11, java.awt.BorderLayout.NORTH);

        automationListScrollPane1.setPreferredSize(new java.awt.Dimension(300, 150));

        automationsListPanel1.setBackground(javax.swing.UIManager.getDefaults().getColor("List.background"));
        automationsListPanel1.setLayout(null);
        automationsListPanel.setLayout( new ListLayout() );
        automationListScrollPane1.setViewportView(automationsListPanel1);

        jPanel10.add(automationListScrollPane1, java.awt.BorderLayout.CENTER);

        jSplitPane2.setRightComponent(jPanel10);

        jPanel6.add(jSplitPane2, java.awt.BorderLayout.CENTER);

        setLayout(new java.awt.BorderLayout());

        settingsPanel.setLayout(new java.awt.GridBagLayout());

        linkPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Link settings"));
        linkPanel.setLayout(new java.awt.GridBagLayout());

        tcpIpSettingsPanel.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("TCP Port:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 2);
        tcpIpSettingsPanel.add(jLabel1, gridBagConstraints);

        portTextField.setText("502");
        portTextField.setPreferredSize(new java.awt.Dimension(40, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 5, 5);
        tcpIpSettingsPanel.add(portTextField, gridBagConstraints);

        linksTabbedPane.addTab("TCP/IP", tcpIpSettingsPanel);

        jPanel1.setLayout(new java.awt.CardLayout());

        serialSettingsPanel.setLayout(new java.awt.GridBagLayout());

        comPortComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "COM 1" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 2, 2);
        serialSettingsPanel.add(comPortComboBox, gridBagConstraints);

        baudRateComboBox.setEditable(true);
        baudRateComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "115200", "57600", "19200", "9600" }));
        baudRateComboBox.setSelectedIndex(2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 2, 5);
        serialSettingsPanel.add(baudRateComboBox, gridBagConstraints);

        parityComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No parity", "Odd parity", "Even parity" }));
        parityComboBox.setSelectedIndex(2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 2);
        serialSettingsPanel.add(parityComboBox, gridBagConstraints);

        xonxoffCheckBox.setText("XON/XOFF");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 5, 2);
        serialSettingsPanel.add(xonxoffCheckBox, gridBagConstraints);

        rtsctsCheckBox.setText("RTS/CTS");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 5, 5);
        serialSettingsPanel.add(rtsctsCheckBox, gridBagConstraints);

        jPanel1.add(serialSettingsPanel, "enabled");

        jPanel5.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Serial communication is disabled.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel5.add(jLabel3, gridBagConstraints);

        jButton1.setText("Why?");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel5.add(jButton1, gridBagConstraints);

        jPanel1.add(jPanel5, "disabled");

        linksTabbedPane.addTab("Serial", jPanel1);

        replaySettingsPanel.setLayout(new java.awt.GridBagLayout());

        jLabel2.setText("Record file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 2, 2);
        replaySettingsPanel.add(jLabel2, gridBagConstraints);

        recordFileChooseButton.setText("Choose...");
        recordFileChooseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recordFileChooseButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 2, 5);
        replaySettingsPanel.add(recordFileChooseButton, gridBagConstraints);

        chosenRecordFileTextField.setEditable(false);
        chosenRecordFileTextField.setText("No file selected.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 5, 5);
        replaySettingsPanel.add(chosenRecordFileTextField, gridBagConstraints);

        linksTabbedPane.addTab("Replay", replaySettingsPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        linkPanel.add(linksTabbedPane, gridBagConstraints);

        runPanel.setLayout(new java.awt.GridBagLayout());

        runToggleButton.setText("Run");
        runToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        runPanel.add(runToggleButton, gridBagConstraints);

        learnToggleButton.setText("Learn");
        learnToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                learnToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        runPanel.add(learnToggleButton, gridBagConstraints);

        tiltLabel.setText("X");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        runPanel.add(tiltLabel, gridBagConstraints);

        recordToggleButton.setText("Record");
        recordToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recordToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        runPanel.add(recordToggleButton, gridBagConstraints);

        asciiToggleButton.setText("Ascii");
        asciiToggleButton.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        runPanel.add(asciiToggleButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        linkPanel.add(runPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        settingsPanel.add(linkPanel, gridBagConstraints);

        projectPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project"));
        projectPanel.setLayout(new java.awt.GridBagLayout());

        loadButton.setText("Load");
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectPanel.add(loadButton, gridBagConstraints);

        saveProjectButton.setText("Save");
        saveProjectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjectButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectPanel.add(saveProjectButton, gridBagConstraints);

        clearProjectButton.setText("Clear");
        clearProjectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearProjectButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectPanel.add(clearProjectButton, gridBagConstraints);

        saveProjectAsButton.setText("Save as");
        saveProjectAsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjectButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        projectPanel.add(saveProjectAsButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        settingsPanel.add(projectPanel, gridBagConstraints);

        toolsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Tools"));
        toolsPanel.setLayout(new java.awt.GridBagLayout());

        masterToggleButton.setText("Master");
        masterToggleButton.setEnabled(false);
        masterToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                masterToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        toolsPanel.add(masterToggleButton, gridBagConstraints);

        scriptsToggleButton.setText("Scripts");
        scriptsToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scriptsToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        toolsPanel.add(scriptsToggleButton, gridBagConstraints);

        helpButton.setText("Help");
        helpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        toolsPanel.add(helpButton, gridBagConstraints);

        consoleToggleButton.setText("Console");
        consoleToggleButton.setEnabled(false);
        consoleToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        toolsPanel.add(consoleToggleButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        settingsPanel.add(toolsPanel, gridBagConstraints);

        add(settingsPanel, java.awt.BorderLayout.NORTH);

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        slavesListView.setBorder(javax.swing.BorderFactory.createTitledBorder("Modbus slaves"));
        slavesListView.setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addModbusSlaveButton.setText("Add");
        addModbusSlaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addModbusSlaveButtonActionPerformed(evt);
            }
        });
        jPanel2.add(addModbusSlaveButton);

        enableAllSlavesButton.setText("Enable all");
        enableAllSlavesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableAllSlavesButtonActionPerformed(evt);
            }
        });
        jPanel2.add(enableAllSlavesButton);

        disableAllSlavesButton.setText("Disable all");
        disableAllSlavesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disableAllSlavesButtonActionPerformed(evt);
            }
        });
        jPanel2.add(disableAllSlavesButton);

        slavesListView.add(jPanel2, java.awt.BorderLayout.NORTH);

        slaveListScrollPane.setPreferredSize(new java.awt.Dimension(300, 150));

        slavesListPanel.setBackground(javax.swing.UIManager.getDefaults().getColor("List.background"));
        slavesListPanel.setLayout(null);
        slavesListPanel.setLayout( new ListLayout() );
        slaveListScrollPane.setViewportView(slavesListPanel);

        slavesListView.add(slaveListScrollPane, java.awt.BorderLayout.CENTER);

        jSplitPane1.setLeftComponent(slavesListView);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Automation"));
        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addAutomationButton.setText("Add");
        addAutomationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAutomationButtonActionPerformed(evt);
            }
        });
        jPanel4.add(addAutomationButton);

        startAllAutomationsButton.setText("Start all");
        startAllAutomationsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startAllAutomationsButtonActionPerformed(evt);
            }
        });
        jPanel4.add(startAllAutomationsButton);

        stopAllAutomationsButton.setText("Stop all");
        stopAllAutomationsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopAllAutomationsButtonActionPerformed(evt);
            }
        });
        jPanel4.add(stopAllAutomationsButton);

        jPanel3.add(jPanel4, java.awt.BorderLayout.NORTH);

        automationListScrollPane.setPreferredSize(new java.awt.Dimension(300, 150));

        automationsListPanel.setBackground(javax.swing.UIManager.getDefaults().getColor("List.background"));
        automationsListPanel.setLayout(null);
        automationsListPanel.setLayout( new ListLayout() );
        automationListScrollPane.setViewportView(automationsListPanel);

        jPanel3.add(automationListScrollPane, java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanel3);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    private void startSerialLink()
    {
        //- - - - - - - - - - - -
        // GET BAUDRATE
        //- - - - - - - - - - - -

        int baudrate = 57600;

        try
        {
            String selectedBaudrate = (String)baudRateComboBox.getSelectedItem();
            baudrate = Integer.valueOf( selectedBaudrate );
        }
        catch(NumberFormatException ex)
        {
            runToggleButton.doClick();
            ErrorMessage dialog = new ErrorMessage("Close");
            dialog.setTitle("Baud rate error");
            dialog.append("Baudrate is not a number.");
            dialog.setVisible(true);
            return;
        }

        //- - - - - - - - - -
        // GET PARITY
        //- - - - - - - - - -
        int parity = parityComboBox.getSelectedIndex();

        //- - - - - - - - - - -
        // GET FLOW CONTROL
        //- - - - - - - - - - -

        boolean xonxoff = xonxoffCheckBox.isSelected();
        boolean rtscts = rtsctsCheckBox.isSelected();

        //- - - - - - - - - - - - -
        // GET COMM PORT AND START
        //- - - - - - - - - - - - -

        try
        {
            int commPortIndex = comPortComboBox.getSelectedIndex();
            currentLink = new ModbusSerialLink(modbusPalProject, commPortIndex, baudrate, parity, xonxoff, rtscts);
            currentLink.start(this);
            modbusMaster.setLink(currentLink);

            ((TiltLabel)tiltLabel).start();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            runToggleButton.doClick();
            ErrorMessage dialog = new ErrorMessage("Close");
            dialog.setTitle("TCP/IP error");
            dialog.append("Cannot bind port " + portTextField.getText() + "\r\n");
            dialog.append("The following exception occured:" + ex.getClass().getSimpleName() + "\r\n");
            dialog.append("Message:"+ex.getLocalizedMessage());
            dialog.setVisible(true);
            return;
        }

    }
    
    private void startTcpIpLink()
    {
        //portTextField.setEnabled(false);
        int port = -1;

        try
        {
            String portNumber = portTextField.getText();
            port = Integer.valueOf(portNumber);
        }
        catch(NumberFormatException ex)
        {
            runToggleButton.doClick();
            ErrorMessage dialog = new ErrorMessage("Close");
            dialog.setTitle("TCP Port error");
            dialog.append("The TCP port number must be a value between 0 and 65535. The default value is 502.");
            dialog.setVisible(true);
            return;
        }

        try
        {
            System.out.printf("[%s] Start TCP/link (port=%d)\r\n", modbusPalProject.getName(), port);
            currentLink = new ModbusTcpIpLink(modbusPalProject, port);
            currentLink.start(this);
            modbusMaster.setLink(currentLink);
            ((TiltLabel)tiltLabel).start();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            runToggleButton.doClick();
            ErrorMessage dialog = new ErrorMessage("Close");
            dialog.setTitle("TCP/IP error");
            dialog.append("Cannot bind port " + portTextField.getText() + "\r\n");
            dialog.append("The following exception occured:" + ex.getClass().getSimpleName() + "\r\n");
            dialog.append("Message:"+ex.getLocalizedMessage());
            dialog.setVisible(true);
            return;
        }
    }
    
    private void startReplayLink()
    {
        File recordFile = null;

        recordFile = (File)chosenRecordFileTextField.getClientProperty("record file");
        if( recordFile==null )
        {
            recordFile = chooseRecordFile();
        }

        try
        {
            currentLink = new ModbusReplayLink(modbusPalProject, recordFile);
            currentLink.start(this);
            modbusMaster.setLink(currentLink);

            ((TiltLabel)tiltLabel).start();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            runToggleButton.doClick();
            ErrorMessage dialog = new ErrorMessage("Close");
            dialog.setTitle("Replay error");
            dialog.append("The following exception occured:" + ex.getClass().getSimpleName() + "\r\n");
            dialog.append("Message:"+ex.getLocalizedMessage());
            dialog.setVisible(true);
            return;
        }
    }


    public void startLink()
    {
        System.out.printf("[%s] Start link\r\n", modbusPalProject.getName());
        GUITools.setAllEnabled(linksTabbedPane,false);

        // if link is tcp/ip
        if( linksTabbedPane.getSelectedComponent()==tcpIpSettingsPanel )
        {
            startTcpIpLink();
        }

        // if lnk is serial
        else if( linksTabbedPane.getSelectedComponent()==serialSettingsPanel )
        {
            startSerialLink();
        }

        // if link is replay:
        else if( linksTabbedPane.getSelectedComponent()==replaySettingsPanel )
        {
            startReplayLink();
        }

        else
        {
            throw new UnsupportedOperationException("not yet implemented");
        }
    }


    public void stopLink()
    {
        System.out.printf("[%s] Stop link\r\n", modbusPalProject.getName());

        if( currentLink != null )
        {
            currentLink.stop();
            ((TiltLabel)tiltLabel).stop();
            currentLink = null;
            modbusMaster.setLink(null);
        }

        GUITools.setAllEnabled(linksTabbedPane,true);
    }

    /**
     * this event is triggered when the user toggle the "run" button.
     * @param evt
     */
    private void runToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runToggleButtonActionPerformed

        // if run button is toggled, start the link
        if( runToggleButton.isSelected() == true )
        {
            startLink();
        }

        // otherwise, stop the link
        else
        {
            stopLink();
        }
}//GEN-LAST:event_runToggleButtonActionPerformed

    private void addModbusSlaveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addModbusSlaveButtonActionPerformed

        AddSlaveDialog dialog = new AddSlaveDialog(modbusPalProject.getModbusSlaves());
        dialog.setVisible(true);
        if( dialog.isAdded() )
        {
            //int id = dialog.getSlaveId();
            //String name = dialog.getSlaveName();
            //ModbusSlave slave = new ModbusSlave(id);
            //slave.setName(name);
            //ModbusPal.addModbusSlave(slave);

            int ids[] = dialog.getSlaveIds();
            String name = dialog.getSlaveName();
            for( int i=0; i<ids.length; i++ )
            {
                ModbusSlave slave = new ModbusSlave(ids[i]);
                slave.setName(name);
                modbusPalProject.addModbusSlave(slave);
            }
        }
    }//GEN-LAST:event_addModbusSlaveButtonActionPerformed

    private void saveProjectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectButtonActionPerformed

        // if no project file is currently defined, then display
        // a FileChooser so that the user can choose where to save
        // the current project.
        //
        if( (modbusPalProject.projectFile==null) || (evt.getSource()==saveProjectAsButton) )
        {
            JFileChooser saveDialog = new XFileChooser(XFileChooser.PROJECT_FILE);
            saveDialog.showSaveDialog(this);
            File projectFile = saveDialog.getSelectedFile();

            // if no project file is selected, do not save
            // the project (leave method)
            if( projectFile == null )
            {
                return;
            }

            // if project file already exists, ask "are you sure?"
            if( projectFile.exists() )
            {
                // TODO: ARE YOUR SURE?
            }

            modbusPalProject.projectFile = projectFile;
        }


        try
        {
            saveProject();
            // TODO: setTitle(APP_STRING+" ("+projectFile.getName()+")");
        }


        catch (FileNotFoundException ex)
        {
            // TODO: display an error message
            Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex)
        {
            // TODO: diplay an error message
            Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
        }
}//GEN-LAST:event_saveProjectButtonActionPerformed

    public ModbusPalProject loadProject(String path)
    throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException
    {
        File projectFile = new File(path);
        ModbusPalProject mpp = ModbusPalProject.load(projectFile);
        setProject(mpp);
        return mpp;
    }

    private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed

        // create loadGenerators dialog
        JFileChooser loadDialog = new XFileChooser(XFileChooser.PROJECT_FILE);
        loadDialog.showOpenDialog(this);
        final File projectFile = loadDialog.getSelectedFile();

        if( projectFile == null )
        {
            return;
        }

        final WorkInProgressDialog dialog = new WorkInProgressDialog("Load project","Loading project...");
        Thread loader = new Thread( new Runnable()
        {
            public void run()
            {
                try
                {
                    ModbusPalProject mpp = ModbusPalProject.load(projectFile);
                    setProject(mpp);
                    //TODO: setTitle(APP_STRING+" ("+projectFile.getName()+")");
                }
                catch (Exception ex)
                {
                    Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
                }

                if( dialog.isVisible() )
                {
                    dialog.setVisible(false);
                }
            }
        });
        loader.setName("loader");
        loader.start();
        GUITools.align(this, dialog);
        dialog.setVisible(true);
}//GEN-LAST:event_loadButtonActionPerformed







    private void addAutomationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAutomationButtonActionPerformed
        
        String name = Automation.DEFAULT_NAME + " #" + String.valueOf( modbusPalProject.idGenerator.createID() );
        Automation automation = new Automation( name );
        modbusPalProject.addAutomation(automation);

    }//GEN-LAST:event_addAutomationButtonActionPerformed

    /**
     * this function is triggered when the user toggles the "master"
     * button.
     * @param evt
     */
    private void masterToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_masterToggleButtonActionPerformed

        if( masterToggleButton.isSelected()==true )
        {
            if( modbusMasterDialog == null )
            {
                modbusMasterDialog = new ModbusMasterDialog(this, modbusMaster);
                modbusMasterDialog.addWindowListener(this);
            }
            modbusMasterDialog.setVisible(true);
        }
        else
        {
            if( modbusMasterDialog != null )
            {
                modbusMasterDialog.setVisible(false);
                modbusMasterDialog = null;
            }
        }
}//GEN-LAST:event_masterToggleButtonActionPerformed

    public void setModbusSlaveEnabled(int id, boolean enabled)
    {
        ModbusSlave slave = modbusPalProject.getModbusSlave(id);
        slave.setEnabled(enabled);
    }


    /**
     * This method is called when the user clicks on the "enable all" button
     * located in the "modbus slaves" frame.
     * @param evt
     */
    private void enableAllSlavesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableAllSlavesButtonActionPerformed

        ModbusSlave slaves[] = modbusPalProject.getModbusSlaves();
        for(int i=0; i<slaves.length; i++)
        {
            if( slaves[i] != null )
            {
                slaves[i].setEnabled(true);
            }
        }
    }//GEN-LAST:event_enableAllSlavesButtonActionPerformed

    private void startAllAutomationsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startAllAutomationsButtonActionPerformed
        startAllAutomations();
}//GEN-LAST:event_startAllAutomationsButtonActionPerformed

    private void disableAllSlavesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disableAllSlavesButtonActionPerformed

        ModbusSlave slaves[] = modbusPalProject.getModbusSlaves();
        for(int i=0; i<slaves.length; i++)
        {
            if( slaves[i] != null )
            {
                slaves[i].setEnabled(false);
            }
        }
    }//GEN-LAST:event_disableAllSlavesButtonActionPerformed

    private void stopAllAutomationsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopAllAutomationsButtonActionPerformed
        stopAllAutomations();
    }//GEN-LAST:event_stopAllAutomationsButtonActionPerformed

    public void startAllAutomations()
    {
        System.out.printf("[%s] Start all automations\r\n", modbusPalProject.getName());
        Automation automations[] = modbusPalProject.getAutomations();
        for(int i=0; i<automations.length; i++)
        {
            automations[i].start();
        }
    }

    public void stopAllAutomations()
    {
        System.out.printf("[%s] Stop all automations\r\n", modbusPalProject.getName());
        Automation automations[] = modbusPalProject.getAutomations();
        for(int i=0; i<automations.length; i++)
        {
            automations[i].stop();
        }
    }

    private void scriptsToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scriptsToggleButtonActionPerformed

        if( scriptManagerDialog==null )
        {
            scriptsToggleButton.setSelected(false);
            // create warning dialog
            ErrorMessage dialog = new ErrorMessage("Close");
            dialog.setTitle("Scripts disabled");
            dialog.append("It seems that Jython is not present on your computer. Scripting is disabled.");
            dialog.append("If you need to use your scripts, go to http://www.jython.org and install Jython on your system.");
            dialog.setVisible(true);
            return;
        }

        else
        {
            if( scriptsToggleButton.isSelected()==true )
            {
                GUITools.align(this, scriptManagerDialog);

                scriptManagerDialog.setVisible(true);
            }
            else
            {
                scriptManagerDialog.setVisible(false);
            }
        }
    }//GEN-LAST:event_scriptsToggleButtonActionPerformed

    private void learnToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_learnToggleButtonActionPerformed
        modbusPalProject.setLearnModeEnabled( learnToggleButton.isSelected() );
    }//GEN-LAST:event_learnToggleButtonActionPerformed

    private void helpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpButtonActionPerformed

        if( helpViewer==null )
        {
            helpViewer = new HelpViewer();
            helpViewer.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            helpViewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        helpViewer.setVisible(true);
        helpViewer.toFront();
        /*if( Desktop.isDesktopSupported()==true )
        {
            try
            {
                URL url = getClass().getResource("../help/index.html");
                Desktop.getDesktop().browse( url.toURI() );
            }

            catch (URISyntaxException ex)
            {
                Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
            }            catch (IOException ex)
            {
                Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
            }
        }*/
    }//GEN-LAST:event_helpButtonActionPerformed

    private void clearProjectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearProjectButtonActionPerformed

        // TODO:
        ModbusPalProject mpp = new ModbusPalProject();
        setProject(mpp);
        //ModbusPal.clearProject();
        // TODO:setTitle(APP_STRING);

    }//GEN-LAST:event_clearProjectButtonActionPerformed

    private void consoleToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleToggleButtonActionPerformed

        if( consoleToggleButton.isSelected()==true )
        {
            GUITools.align(this, console);
            console.setVisible(true);
        }
        else
        {
            console.setVisible(false);
        }
    }//GEN-LAST:event_consoleToggleButtonActionPerformed

    private void recordToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recordToggleButtonActionPerformed

        if( recordToggleButton.isSelected() )
        {
            try {
                ModbusPalRecorder.start();
            } catch (IOException ex) {
                Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
        {
            try {
                ModbusPalRecorder.stop();
            } catch (IOException ex) {
                Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }//GEN-LAST:event_recordToggleButtonActionPerformed



    private File chooseRecordFile()
    {
        XFileChooser fc = new XFileChooser( XFileChooser.RECORDER_FILE );
        fc.showOpenDialog(this);
        File src = fc.getSelectedFile();
        modbusPalProject.linkReplayFile = src;
        setReplayFile(src);
        return src;
    }


    private void recordFileChooseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recordFileChooseButtonActionPerformed

        chooseRecordFile();

    }//GEN-LAST:event_recordFileChooseButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ErrorMessage dialog = new ErrorMessage("Close");
        dialog.setTitle("Serial link disabled");
        dialog.append("It seems that RXTX is not present on your computer. Serial communication is disabled.");
        dialog.append("If you need to use your COM ports, go to http://www.rxtx.org and install the RXTX library that suits your system.");
        dialog.setVisible(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void recordFileChooseButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recordFileChooseButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_recordFileChooseButton1ActionPerformed

    private void runToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runToggleButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_runToggleButton1ActionPerformed

    private void learnToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_learnToggleButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_learnToggleButton1ActionPerformed

    private void recordToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recordToggleButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_recordToggleButton1ActionPerformed

    private void loadButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_loadButton1ActionPerformed

    private void saveProjectButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_saveProjectButton1ActionPerformed

    private void clearProjectButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearProjectButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_clearProjectButton1ActionPerformed

    private void saveProjectAsButton1saveProjectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectAsButton1saveProjectButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_saveProjectAsButton1saveProjectButtonActionPerformed

    private void masterToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_masterToggleButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_masterToggleButton1ActionPerformed

    private void scriptsToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scriptsToggleButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_scriptsToggleButton1ActionPerformed

    private void helpButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_helpButton1ActionPerformed

    private void consoleToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoleToggleButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_consoleToggleButton1ActionPerformed

    private void addModbusSlaveButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addModbusSlaveButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addModbusSlaveButton1ActionPerformed

    private void enableAllSlavesButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableAllSlavesButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_enableAllSlavesButton1ActionPerformed

    private void disableAllSlavesButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disableAllSlavesButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_disableAllSlavesButton1ActionPerformed

    private void addAutomationButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAutomationButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addAutomationButton1ActionPerformed

    private void startAllAutomationsButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startAllAutomationsButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_startAllAutomationsButton1ActionPerformed

    private void stopAllAutomationsButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopAllAutomationsButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_stopAllAutomationsButton1ActionPerformed


    public void startAll()
    {
        System.out.printf("[%s] Start everything\r\n",modbusPalProject.getName());
        startAllAutomations();
        startLink();
    }

    public void stopAll()
    {
        stopLink();
        stopAllAutomations();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addAutomationButton;
    private javax.swing.JButton addAutomationButton1;
    private javax.swing.JButton addModbusSlaveButton;
    private javax.swing.JButton addModbusSlaveButton1;
    private javax.swing.JToggleButton asciiToggleButton;
    private javax.swing.JToggleButton asciiToggleButton1;
    private javax.swing.JScrollPane automationListScrollPane;
    private javax.swing.JScrollPane automationListScrollPane1;
    private javax.swing.JPanel automationsListPanel;
    private javax.swing.JPanel automationsListPanel1;
    private javax.swing.JComboBox baudRateComboBox;
    private javax.swing.JComboBox baudRateComboBox1;
    private javax.swing.JTextField chosenRecordFileTextField;
    private javax.swing.JTextField chosenRecordFileTextField1;
    private javax.swing.JButton clearProjectButton;
    private javax.swing.JButton clearProjectButton1;
    private javax.swing.JComboBox comPortComboBox;
    private javax.swing.JComboBox comPortComboBox1;
    private javax.swing.JToggleButton consoleToggleButton;
    private javax.swing.JToggleButton consoleToggleButton1;
    private javax.swing.JButton disableAllSlavesButton;
    private javax.swing.JButton disableAllSlavesButton1;
    private javax.swing.JButton enableAllSlavesButton;
    private javax.swing.JButton enableAllSlavesButton1;
    private javax.swing.JButton helpButton;
    private javax.swing.JButton helpButton1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JToggleButton learnToggleButton;
    private javax.swing.JToggleButton learnToggleButton1;
    private javax.swing.JPanel linkPanel;
    private javax.swing.JPanel linkPanel1;
    private javax.swing.JTabbedPane linksTabbedPane;
    private javax.swing.JTabbedPane linksTabbedPane1;
    private javax.swing.JButton loadButton;
    private javax.swing.JButton loadButton1;
    private javax.swing.JToggleButton masterToggleButton;
    private javax.swing.JToggleButton masterToggleButton1;
    private javax.swing.JComboBox parityComboBox;
    private javax.swing.JComboBox parityComboBox1;
    private javax.swing.JTextField portTextField;
    private javax.swing.JTextField portTextField1;
    private javax.swing.JPanel projectPanel;
    private javax.swing.JPanel projectPanel1;
    private javax.swing.JButton recordFileChooseButton;
    private javax.swing.JButton recordFileChooseButton1;
    private javax.swing.JToggleButton recordToggleButton;
    private javax.swing.JToggleButton recordToggleButton1;
    private javax.swing.JPanel replaySettingsPanel;
    private javax.swing.JPanel replaySettingsPanel1;
    private javax.swing.JCheckBox rtsctsCheckBox;
    private javax.swing.JCheckBox rtsctsCheckBox1;
    private javax.swing.JPanel runPanel;
    private javax.swing.JPanel runPanel1;
    private javax.swing.JToggleButton runToggleButton;
    private javax.swing.JToggleButton runToggleButton1;
    private javax.swing.JButton saveProjectAsButton;
    private javax.swing.JButton saveProjectAsButton1;
    private javax.swing.JButton saveProjectButton;
    private javax.swing.JButton saveProjectButton1;
    javax.swing.JToggleButton scriptsToggleButton;
    javax.swing.JToggleButton scriptsToggleButton1;
    private javax.swing.JPanel serialSettingsPanel;
    private javax.swing.JPanel serialSettingsPanel1;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JPanel settingsPanel1;
    private javax.swing.JScrollPane slaveListScrollPane;
    private javax.swing.JScrollPane slaveListScrollPane1;
    private javax.swing.JPanel slavesListPanel;
    private javax.swing.JPanel slavesListPanel1;
    private javax.swing.JPanel slavesListView;
    private javax.swing.JPanel slavesListView1;
    private javax.swing.JButton startAllAutomationsButton;
    private javax.swing.JButton startAllAutomationsButton1;
    private javax.swing.JButton stopAllAutomationsButton;
    private javax.swing.JButton stopAllAutomationsButton1;
    private javax.swing.JPanel tcpIpSettingsPanel;
    private javax.swing.JPanel tcpIpSettingsPanel1;
    private javax.swing.JLabel tiltLabel;
    private javax.swing.JLabel tiltLabel1;
    private javax.swing.JPanel toolsPanel;
    private javax.swing.JPanel toolsPanel1;
    private javax.swing.JCheckBox xonxoffCheckBox;
    private javax.swing.JCheckBox xonxoffCheckBox1;
    // End of variables declaration//GEN-END:variables

    public void windowOpened(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
        Object source = e.getSource();
        
        if( source==modbusMasterDialog )
        {
            masterToggleButton.setSelected(false);
        }
        else if( source==scriptManagerDialog )
        {
            scriptsToggleButton.setSelected(false);
        }
        else if( source==console )
        {
            consoleToggleButton.setSelected(false);
        }
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    @Override
    public void modbusSlaveAdded(ModbusSlave slave)
    {
        // add slave panel into the gui and refresh gui
        ModbusSlavePanel panel = new ModbusSlavePanel(this,slave);
        slavesListPanel.add( panel, new Integer(slave.getSlaveId()) );
        slave.addModbusSlaveListener(panel);
        slaveListScrollPane.validate();
    }


    private ModbusSlavePanel findModbusSlavePanel(int slaveId)
    {
        Component panels[] = slavesListPanel.getComponents();
        for( int i=0; i<panels.length; i++ )
        {
            if( panels[i] instanceof ModbusSlavePanel )
            {
                ModbusSlavePanel panel = (ModbusSlavePanel)panels[i];
                if( panel.getSlaveId()==slaveId )
                {
                    return panel;
                }
            }
        }
        return null;
    }

    @Override
    public void modbusSlaveRemoved(ModbusSlave slave)
    {
        int slaveID = slave.getSlaveId();

        ModbusSlavePanel panel = findModbusSlavePanel(slaveID);
        
        if( panel != null )
        {
            // the dialog will be disconnect, so remove it to:
            panel.delete();

            // remove panel from the list
            slavesListPanel.remove( panel );

            // force the list to redo the layout
            slaveListScrollPane.validate();

            // force the list to be repainted
            slavesListPanel.repaint();
        }
    }


    @Override
    public void automationAdded(Automation automation, int index)
    {
        // add slave panel into the gui and refresh gui
        AutomationPanel panel = new AutomationPanel(automation, this );
        //panel.requestFocus();
        automationsListPanel.add( panel, new Integer(index) );
        automationListScrollPane.validate();
        panel.focus();
    }


    private AutomationPanel findAutomationPanel(Automation automation)
    {
        Component panels[] = automationsListPanel.getComponents();
        for( int i=0; i<panels.length; i++ )
        {
            if( panels[i] instanceof AutomationPanel )
            {
                AutomationPanel panel = (AutomationPanel)panels[i];
                if( panel.getAutomation()==automation )
                {
                    return panel;
                }
            }
        }
        return null;
    }


    @Override
    public void automationRemoved(Automation automation)
    {
        AutomationPanel panel = findAutomationPanel(automation);

        if( panel != null )
        {
            // the dialog will be disconnect, so remove it too:
            panel.disconnect();

            // remove panel from the list
            automationsListPanel.remove( panel );

            // force the list to redo the layout
            automationListScrollPane.validate();

            // force the list to be repainted
            automationsListPanel.repaint();
        }
    }

    @Override
    public void linkBroken()
    {
        GUITools.setAllEnabled(linksTabbedPane,true);
        runToggleButton.setSelected(false);
    }



    public void exit()
    {
        stopAll();
        
        try {
            // stop recorder
            ModbusPalRecorder.stop();
        } catch (IOException ex) {
            Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
        }

        // close all windows
    }

    @Deprecated
    public void showScriptManagerDialog(int tabIndex)
    {
        showScriptManagerDialog();
    }

    public void showScriptManagerDialog()
    {
        scriptManagerDialog.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        scriptManagerDialog.setVisible(true);
        scriptsToggleButton.setSelected(true);
    }

    public ModbusPalProject getProject()
    {
        return modbusPalProject;
    }

    public void setSlavesListVisible(boolean b) {
        slavesListView.setVisible(b);
    }


}