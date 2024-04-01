/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package modbuspal.generator.random;

import modbuspal.generator.Generator;
import modbuspal.toolkit.XMLTools;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * the random generator
 * @author nnovic
 */
public class RandomGenerator
extends Generator
{
    private final RandomControlPanel panel;
    double minValue = 0.0;
    double maxValue = 0.0;
    boolean relativeMin = false;
    boolean relativeMax = false;

    /**
     * Creates a new instance of RandomGenerator
     */
    public RandomGenerator()
    {
        setIcon("RandomGenerator.png");
        panel = new RandomControlPanel(this);
    }

    @Override
    public double getValue(double time)
    {
        double lowest = minValue;
        if(relativeMin)
        {
            lowest += getInitialValue();
        }

        double highest = maxValue;
        if(relativeMax)
        {
            highest += lowest;
        }

        double ran = Math.random();
        return lowest + ran * (highest-lowest);
    }

    @Override
    public void saveGeneratorSettings(OutputStream out)
    throws IOException
    {
        String start = "<min" + " value=\"" + minValue + "\"" +
                " relative=\"" + relativeMin + "\"" +
                "/>\r\n";
        out.write(start.getBytes() );

        String end = "<max" + " value=\"" + maxValue + "\"" +
                " relative=\"" + relativeMax + "\"" +
                "/>\r\n";
        out.write(end.getBytes() );
    }

    @Override
    public void loadGeneratorSettings(NodeList childNodes)
    {
        Node minNode = XMLTools.getNode(childNodes, "min");
        loadMin(minNode);

        Node maxNode = XMLTools.getNode(childNodes, "max");
        loadMax(maxNode);
    }

    private void loadMax(Node node)
    {
        // read attributes from xml document
        String maxVal = XMLTools.getAttribute("value", node);
        String maxRel = XMLTools.getAttribute("relative", node);

        // setup generator's values
        maxValue = Double.parseDouble(maxVal);
        relativeMax = Boolean.parseBoolean(maxRel);

        // update generator's panel
        panel.maxTextField.setText(maxVal);
        panel.maxRelativeCheckBox.setSelected(relativeMax);
    }

    private void loadMin(Node node)
    {
        // read attributes from xml document
        String minVal = XMLTools.getAttribute("value", node);
        String minRel = XMLTools.getAttribute("relative", node);

        // setup generator's values
        minValue = Double.parseDouble(minVal);
        relativeMin = Boolean.parseBoolean(minRel);
        
        // update generator's panel
        panel.minTextField.setText( String.valueOf(minValue) );
        panel.minRelativeCheckBox.setSelected(relativeMin);
    }

    @Override
    public JPanel getControlPanel()
    {
        return panel;
    }

}
