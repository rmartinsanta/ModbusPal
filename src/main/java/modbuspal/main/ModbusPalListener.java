/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package modbuspal.main;

import modbuspal.automation.Automation;
import modbuspal.master.ModbusMasterTask;
import modbuspal.slave.ModbusSlave;

/**
 * An object interested in ModbusPal related events should implement
 * this interface.
 * @author nnovic
 */
public interface ModbusPalListener
{
    /**
     * This method is triggered when a new MODBUS slave is added into the project.
     * @param slave the slave that has been added
     */
    void modbusSlaveAdded(ModbusSlave slave);

    /**
     * This method is triggered when a MODBUS slave is removed from the project.
     * @param slave the slave being removed
     */
    void modbusSlaveRemoved(ModbusSlave slave);

    /**
     * This method is triggered when an automation is added into the project.
     * @param automation the automation that has been added
     * @param index the index of this automation into the list of automations
     */
    void automationAdded(Automation automation, int index);

    /**
     * This method is triggered when an automation is removed from the project.
     * @param automation the automation being removed
     */
    void automationRemoved(Automation automation);

    /**
     * This method is triggered when a PDU has been successfully processed
     * by ModbusPal.
     */
    void pduProcessed();

    /**
     * This method is triggered when a PDU has been processed and the result
     * is an exception reply.
     */
    void pduException();

    /**
     * This method is triggered when a PDU has been received but no reply is
     * given to it.
     */
    void pduNotServiced();

    void modbusMasterTaskRemoved(ModbusMasterTask mmt);

    void modbusMasterTaskAdded(ModbusMasterTask mmt);
}
