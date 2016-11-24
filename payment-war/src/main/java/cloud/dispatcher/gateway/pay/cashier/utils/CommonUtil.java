package cloud.dispatcher.gateway.pay.cashier.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class CommonUtil {

    @SuppressWarnings("rawtypes")
    public static String getThisInstanceIpAddress() {
        try {
            String ipAddress = InetAddress.getLocalHost().toString();
            String[] ipArray = ipAddress.split("/");
            ipAddress = ipArray.length > 1 ? ipArray[1] : ipArray[0];
            if (ipAddress.startsWith("172.16") || ipAddress.startsWith("10.1"))
                return ipAddress;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress addr = (InetAddress) inetAddresses.nextElement();
                    if (addr.getHostAddress().startsWith("172.16") ||
                            addr.getHostAddress().startsWith("10.1"))
                        return addr.getHostAddress();
                }
            }
            return "127.0.0.1";
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}
