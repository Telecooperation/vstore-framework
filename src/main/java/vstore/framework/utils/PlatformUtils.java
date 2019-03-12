package vstore.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlatformUtils {

    private static final Logger LOGGER = LogManager.getLogger(PlatformUtils.class);

    public static boolean isAndroid() {
        boolean result = System.getProperty("java.runtime.name", "").toLowerCase().contains("android");
        LOGGER.info("isAndroid() returned " + result);
        return result;
    }
}
