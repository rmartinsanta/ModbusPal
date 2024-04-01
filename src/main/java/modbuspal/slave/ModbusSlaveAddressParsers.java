package modbuspal.slave;

import modbuspal.main.AddSlaveDialog;
import modbuspal.main.ModbusConst;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ModbusSlaveAddressParsers {

    public static final String RTU_PATTERN = "(\\d+)(?:\\s*-\\s*(\\d+))?";

    public static List<ModbusSlaveAddress> tryParseRtuAddress(String s) {
        Pattern rtuPattern = Pattern.compile(RTU_PATTERN);
        Matcher m = rtuPattern.matcher(s.trim());
        if (m.matches()) {
            String group1 = m.group(1);
            int startIndex = Integer.parseInt(group1);
            int endIndex = startIndex;
            int groupCount = m.groupCount();
            if (groupCount == 2) {
                String group2 = m.group(2);
                if (group2 != null) {
                    endIndex = Integer.parseInt(group2);
                }
            }

            if ((startIndex < ModbusConst.FIRST_MODBUS_SLAVE)
                    || (startIndex > ModbusConst.LAST_MODBUS_SLAVE)) {
                throw new ArrayIndexOutOfBoundsException();
            }

            if ((endIndex < ModbusConst.FIRST_MODBUS_SLAVE)
                    || (endIndex > ModbusConst.LAST_MODBUS_SLAVE)) {
                throw new ArrayIndexOutOfBoundsException();
            }

            if (startIndex > endIndex) {
                throw new IllegalArgumentException();
            }

            int count = 1 + (endIndex - startIndex);
            ArrayList<ModbusSlaveAddress> output = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ModbusSlaveAddress msa = new ModbusSlaveAddress(startIndex + i);
                output.add(msa);
            }
            return output;
        }
        return null;
    }


    public static int[] parseIpv4(String s) {
        Pattern ipv4Pattern = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
        Matcher m = ipv4Pattern.matcher(s);
        if (m.find()) {
            String a = m.group(1);
            String b = m.group(2);
            String c = m.group(3);
            String d = m.group(4);

            int[] output = new int[4];

            output[0] = Integer.parseInt(a);
            output[1] = Integer.parseInt(b);
            output[2] = Integer.parseInt(c);
            output[3] = Integer.parseInt(d);

            if ((output[0] > 255) || (output[1] > 255) || (output[2] > 255) || (output[3] > 255)) {
                return null;
            }

            return output;
        }
        return null;
    }

    public static List<ModbusSlaveAddress> tryParseIpAddress_1(String s) {

        // part of the pattern that finds an ip v4 address
        // (group 1)

        String sb = "([\\d\\.]+)" +

                // part of the pattern that finds the optionnal second ip v4 address.
                // that second ip address defines a range for multiple slave creation.
                // (group 2)
                "(?:\\s*-\\s*([\\d\\.]+))?" +

                // part of the pattern that finds the optionnal rtu address
                // associated with the ip
                // (group 3&4)
                "(?:\\s*\\(\\s*(\\d+)\\s*\\))?";

        Pattern p = Pattern.compile(sb);
        Matcher m = p.matcher(s.trim());
        if (m.find()) {
            int count = m.groupCount();
            String firstIp = m.group(1);
            String lastIp = m.group(2);
            String rtuAddr = m.group(3);
            int slaveAddress = -1;

            if (lastIp == null) {
                lastIp = firstIp;
            }

            if (rtuAddr != null) {
                try {
                    slaveAddress = Integer.parseInt(rtuAddr);
                } catch (NumberFormatException ex) {
                    slaveAddress = -1;
                }
            }

            int[] startIp = parseIpv4(firstIp);
            int[] endIp = parseIpv4(lastIp);
            ArrayList<ModbusSlaveAddress> output = new ArrayList<>(count);
            for (int a = startIp[0]; a <= endIp[0]; a++) {
                for (int b = startIp[1]; b <= endIp[1]; b++) {
                    for (int c = startIp[2]; c <= endIp[2]; c++) {
                        for (int d = startIp[3]; d <= endIp[3]; d++) {
                            try {
                                byte[] ip = new byte[]{(byte) a, (byte) b, (byte) c, (byte) d};
                                InetAddress addr = Inet4Address.getByAddress(ip);
                                ModbusSlaveAddress msa = new ModbusSlaveAddress(addr, slaveAddress);
                                output.add(msa);
                            } catch (UnknownHostException ex) {
                                Logger.getLogger(AddSlaveDialog.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
            return output;
        }
        return null;
    }


    public static List<ModbusSlaveAddress> tryParseIpAddress_2(String s) throws UnknownHostException {

        // part of the pattern that finds an ip v4 address
        // (group 1)

        String sb = "([\\d\\.]+)" +

                // ignore white spaces and match parenthesis
                "\\s*\\(\\s*" +

                // match RTU PATTERN
                RTU_PATTERN + // = "([\\d]+)(?:[\\s]*-[\\s]*([\\d]+))?";

                // ignore white spaces and mathc parenthesis
                "\\s*\\)";

        Pattern p = Pattern.compile(sb);
        Matcher m = p.matcher(s.trim());
        if (m.find()) {
            String ipAddress = m.group(1);
            String group2 = m.group(2);
            int startIndex = Integer.parseInt(group2);
            int endIndex = startIndex;
            int groupCount = m.groupCount();
            if (groupCount == 3) {
                String group3 = m.group(3);
                if (group3 != null) {
                    endIndex = Integer.parseInt(group3);
                }
            }

            if ((startIndex < ModbusConst.FIRST_MODBUS_SLAVE)
                    || (startIndex > ModbusConst.LAST_MODBUS_SLAVE)) {
                throw new ArrayIndexOutOfBoundsException();
            }

            if ((endIndex < ModbusConst.FIRST_MODBUS_SLAVE)
                    || (endIndex > ModbusConst.LAST_MODBUS_SLAVE)) {
                throw new ArrayIndexOutOfBoundsException();
            }

            if (startIndex > endIndex) {
                throw new IllegalArgumentException();
            }

            int count = 1 + (endIndex - startIndex);
            ArrayList<ModbusSlaveAddress> output = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                //byte[] ip = new byte[]{ (byte)a, (byte)b, (byte)c, (byte)d };
                InetAddress addr = Inet4Address.getByName(ipAddress);
                ModbusSlaveAddress msa = new ModbusSlaveAddress(addr, startIndex + i);
                output.add(msa);
            }


            return output;
        }
        return null;
    }

    public static List<ModbusSlaveAddress> tryAnyParser(String chunk) {
        var output = new ArrayList<ModbusSlaveAddress>();
        try {
            List<ModbusSlaveAddress> msa = ModbusSlaveAddressParsers.tryParseRtuAddress(chunk);
            if (msa == null) {
                msa = ModbusSlaveAddressParsers.tryParseIpAddress_2(chunk);
                if (msa == null) {
                    msa = ModbusSlaveAddressParsers.tryParseIpAddress_1(chunk);
                }
            }

            if (msa != null) {
                output.addAll(msa);
            }
        } catch (Exception e) {
        }
        return output;
    }
}
