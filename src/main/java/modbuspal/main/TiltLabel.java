/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package modbuspal.main;

import modbuspal.toolkit.FileTools;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nnovic
 */
class TiltLabel
extends JLabel
implements Runnable
{
    //private int tiltCount=0;
    public static final int RED = 1;
    public static final int YELLOW = 2;
    public static final int GREEN = 3;
    private int tiltColor=0;
    private boolean execute=false;
    private Thread thread=null;
    private ImageIcon grayIcon;
    private ImageIcon greenIcon;
    private ImageIcon yellowIcon;
    private ImageIcon redIcon;

    public TiltLabel()
    {
        super();
        loadImages();
        setIcon(grayIcon);
    }

    @Override
    public void setText(String text)
    {
    }


    private void loadImages()
    {
        grayIcon = new ImageIcon(FileTools.getImage("/img/grayTilt.png"));
        greenIcon = new ImageIcon(FileTools.getImage("/img/greenTilt.png"));
        yellowIcon = new ImageIcon(FileTools.getImage("/img/yellowTilt.png"));
        redIcon = new ImageIcon(FileTools.getImage("/img/redTilt.png"));
    }

    public void start()
    {
        execute=true;
        thread = new Thread(this,"tilt");
        thread.start();
    }


    public void stop()
    {
        execute=false;
        try {
            thread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(TiltLabel.class.getName()).log(Level.SEVERE, null, ex);
        }
        thread=null;
    }

    public void tilt(int c)
    {
        synchronized(this)
        {
            tiltColor=c;
        }
    }

    @Override
    public void run()
    {
        boolean tilted = false;

        while(execute)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(TiltLabel.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            synchronized(this)
            {
                if(tilted)
                {
                    setIcon(grayIcon);
                    tilted=false;
                }
                else
                {
                    tilted = true;
                    switch(tiltColor)
                    {
                        case RED: setIcon(redIcon); break;
                        case YELLOW: setIcon(yellowIcon); break;
                        case GREEN: setIcon(greenIcon); break;
                        default: tilted = false; break;
                    }
                }
                tiltColor=0;
            }
        } // end of while
        setIcon(grayIcon);
    }

}
