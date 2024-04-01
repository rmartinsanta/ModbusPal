/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package modbuspal.main;

/**
 * The tags used by ModbusPal when generating xml files. Incomplete.
 * @author nnovic
 */
public interface ModbusPalXML
{
    /* SLAVE */
    String XML_SLAVE_ID_ATTRIBUTE = "id";
    String XML_SLAVE_ID2_ATTRIBUTE = "id2";
    String XML_SLAVE_ENABLED_ATTRIBUTE = "enabled";
    String XML_SLAVE_NAME_ATTRIBUTE = "name";
    String XML_SLAVE_IMPLEMENTATION_ATTRIBUTE = "implementation";
    String XML_SLAVE_IMPLEMENTATION_MODBUS_VALUE = "modbus";
    String XML_SLAVE_IMPLEMENTATION_JBUS_VALUE = "j-bus";

    /* REGISTER */
    String XML_HOLDING_REGISTERS_TAG = "holding_registers";
    String XML_ADDRESS_ATTRIBUTE = "address";

    /* COILS */
    String XML_COILS_TAG = "coils";

    /* FILES */
    String XML_FILE_RELATIVE_PATH_TAG = "rel";
    String XML_FILE_ABSOLUTE_PATH_TAG = "abs";

    /* SCRIPTS */
    String XML_SCRIPT_TYPE_ATTRIBUTE = "type";
    String XML_SCRIPT_TYPE_ONDEMAND = "ondemand";
    String XML_SCRIPT_TYPE_AFTERINIT = "afterinit";
    String XML_SCRIPT_TYPE_BEFOREINIT = "beforeinit";

    /* FUNCTIONS */
    String XML_FUNCTIONS_TAG = "functions";
    String XML_FUNCTION_INSTANCE_TAG = "instance";
    String XML_FUNCTION_TAG = "function";
    String XML_FUNCTION_CODE_ATTRIBUTE = "code";
    String XML_FUNCTION_SETTINGS_TAG = "settings";

    /* TUNING */
    String XML_TUNING_TAG = "tuning";
    String XML_REPLYDELAY_TAG = "reply_delay";
    String XML_REPLYDELAY_MIN_ATTRIBUTE = "min";
    String XML_REPLYDELAY_MAX_ATTRIBUTE = "max";
    String XML_ERRORRATES_TAG = "error_rates";
    String XML_ERRORRATES_NOREPLY_ATTRIBUTE = "no_reply";
}
