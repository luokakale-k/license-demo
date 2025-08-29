package org.example.licenseplatform.util;

import org.example.licenseplatform.model.MachineInfo;

import java.net.NetworkInterface;
import java.util.Enumeration;

public class MachineInfoUtils {

    public static MachineInfo getMachineInfo() {
        MachineInfo info = new MachineInfo();
        info.setCpuSerial(getCPUSerial());
        info.setMacAddress(getFirstMacAddress());
        info.setMainBoardSerial(getMainBoardSerial());
        return info;
    }

    public static String getCPUSerial() {
        // Mac/Linux 示例命令，Windows 可另行适配
        return CommandExecutor.exec("dmidecode -t processor | grep ID");
    }

    public static String getMainBoardSerial() {
        return CommandExecutor.exec("dmidecode -t baseboard | grep Serial");
    }

    public static String getFirstMacAddress() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isLoopback() && iface.getHardwareAddress() != null) {
                    byte[] mac = iface.getHardwareAddress();
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X:", b));
                    }
                    return sb.substring(0, sb.length() - 1);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
