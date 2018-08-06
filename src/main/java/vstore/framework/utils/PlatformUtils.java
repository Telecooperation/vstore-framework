package vstore.framework.utils;

public class PlatformUtils {

    public static boolean isAndroid() {
        return System.getProperty("java.runtime.name", "").toLowerCase().contains("android");
    }
}
