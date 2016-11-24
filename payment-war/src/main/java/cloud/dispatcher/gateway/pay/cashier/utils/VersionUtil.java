package cloud.dispatcher.gateway.pay.cashier.utils;


import org.apache.commons.lang.StringUtils;

public class VersionUtil {

    public static boolean compare(String newVersion, String oldVersion) {
        if (StringUtils.isEmpty(newVersion) || StringUtils.isEmpty(oldVersion))
            throw new IllegalArgumentException("Invalid parameter!");

        int index1 = 0;
        int index2 = 0;
        while(index1 < newVersion.length() && index2 < oldVersion.length()) {
            int[] number1 = getValue(newVersion, index1);
            int[] number2 = getValue(oldVersion, index2);

            if (number1[0] < number2[0]) {
                return false;
            }
            if (number1[0] > number2[0]) {
                return true;
            }
            index1 = number1[1] + 1;
            index2 = number2[1] + 1;
        }
        if(index1 == newVersion.length() && index2 == oldVersion.length()) return true;

        return index1 < newVersion.length() ? true : false;
    }

    public static int[] getValue(String version, int index) {
        int[] valueIndex = new int[2];
        StringBuilder stringBuilder = new StringBuilder();
        while(index < version.length() && version.charAt(index) != '.') {
            stringBuilder.append(version.charAt(index));
            index++;
        }
        valueIndex[0] = Integer.parseInt(stringBuilder.toString());
        valueIndex[1] = index;

        return valueIndex;
    }
}
