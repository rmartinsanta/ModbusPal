/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package modbuspal.generator.linear;

import modbuspal.generator.Generator;
import modbuspal.toolkit.XMLTools;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The Linear generator
 * @author nnovic
 */
public class LinearGenerator
extends Generator
{
    private final LinearControlPanel panel;
    double startValue = 0.0;
    double endValue = 0.0;
    boolean relativeStart = false;
    boolean relativeEnd = false;

    /**
     * Creates a new instance of LinearGenerator.
     */
    public LinearGenerator()
    {
        setIcon("LinearGenerator.png");
        panel = new LinearControlPanel(this);
    }

    @Override
    public double getValue(double time)
    {
        double y1 = startValue;
        if(relativeStart)
        {
            y1 += getInitialValue();
        }

        double y2 = endValue;
        if(relativeEnd)
        {
            y2 += y1;
        }

        return y1 + time * (y2-y1) / getDuration();
    }

    @Override
    public void saveGeneratorSettings(OutputStream out)
    throws IOException
    {
        String start = "<start" + " value=\"" + startValue + "\"" +
                " relative=\"" + relativeStart + "\"" +
                "/>\r\n";
        out.write(start.getBytes() );

        String end = "<end" + " value=\"" + endValue + "\"" +
                " relative=\"" + relativeEnd + "\"" +
                "/>\r\n";
        out.write(end.getBytes() );
    }

    @Override
    public void loadGeneratorSettings(NodeList childNodes)
    {
        Node startNode = XMLTools.getNode(childNodes, "start");
        loadStart(startNode);

        Node endNode = XMLTools.getNode(childNodes, "end");
        loadEnd(endNode);
    }

    private void loadEnd(Node node)
    {
        // read attributes from xml document
        String endVal = XMLTools.getAttribute("value", node);
        String endRel = XMLTools.getAttribute("relative", node);

        // setup generator's values
        endValue = Double.parseDouble(endVal);
        relativeEnd = Boolean.parseBoolean(endRel);

        // update generator's panel
        panel.endTextField.setText(endVal);
        panel.endRelativeCheckBox.setSelected(relativeEnd);
    }

    private void loadStart(Node node)
    {
        // read attributes from xml document
        String startVal = XMLTools.getAttribute("value", node);
        String startRel = XMLTools.getAttribute("relative", node);

        // setup generator's values
        startValue = Double.parseDouble(startVal);
        relativeStart = Boolean.parseBoolean(startRel);
        
        // update generator's panel
        panel.startTextField.setText( String.valueOf(startValue) );
        panel.startRelativeCheckBox.setSelected(relativeStart);
    }

    @Override
    public JPanel getControlPanel()
    {
        return panel;
    }

}
