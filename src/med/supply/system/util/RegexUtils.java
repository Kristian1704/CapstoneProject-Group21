
package med.supply.system.util;

import java.util.regex.Pattern;

public class RegexUtils {
    private static final Pattern EQUIPMENT = Pattern.compile("^[A-Za-z0-9_-]{2,40}$");
    public static boolean isValidEquipment(String s) {
        return s != null && EQUIPMENT.matcher(s).matches();
    }

    private static final Pattern DATE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
    public static boolean isIsoDate(String s) {
        return s != null && DATE.matcher(s).matches();
    }
}
