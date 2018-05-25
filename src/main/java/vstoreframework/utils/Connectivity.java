package vstoreframework.utils;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

/**
 * This class provides methods to check device's network connectivity and speed
 * A lot of code in this class comes from user emil on stackoverflow
 * (http://stackoverflow.com/users/220710/emil)
 */
public class Connectivity {

    /**
     * Get the network information
     * @param c The Android context
     * @return A NetworkInfo object
     */
    public static NetworkInfo getNetworkInfo(){
        ConnectivityManager cm
                = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }

    /**
     * Check if there is any connectivity at all.
     * @param c The Android context
     * @return True, if connectivity is available
     */
    public static boolean isConnected(){
        NetworkInfo info = Connectivity.getNetworkInfo(c);
        return (info != null && info.isConnected());
    }

    /**
     * Check if there is any connectivity to a Wifi network
     * @param c The Android context
     * @return True, if connected to WiFi
     */
    public static boolean isConnectedWifi(){
        NetworkInfo info = Connectivity.getNetworkInfo(c);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }

    /**
     * Check if there is any connectivity to a mobile network
     * @param c The Android context
     * @return True, if mobile is connected
     */
    public static boolean isConnectedMobile(){
        NetworkInfo info = Connectivity.getNetworkInfo(c);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * Check if there is fast connectivity (e.g. > 1Mbps)
     * @param c The Android context
     * @return True, if connection is faster than 1Mbps
     */
    public static boolean isConnectedFast(){
        NetworkInfo info = Connectivity.getNetworkInfo(c);
        return (info != null && info.isConnected() && Connectivity.isConnectionFast(info.getType(),info.getSubtype()));
    }

    /**
     * Check if the connection is fast (e.g. > 1Mbps)
     * @param type The type of network, see constants in {@link TelephonyManager}.
     * @param subType The subtype of network, see constants in {@link TelephonyManager}.
     * @return True, if the network is fast
     */
    public static boolean isConnectionFast(int type, int subType){
        if(type == ConnectivityManager.TYPE_WIFI) {
            return true;
        } else if(type==ConnectivityManager.TYPE_MOBILE) {
            switch(subType) {
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return false; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
                    return false; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return false; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return false; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return false; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return true; // ~ 400-7000 kbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return true; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return true; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return true; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11
                    return true; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
                    return true; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
                    return true; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
                    return true; // ~ 10+ Mbps
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Determines, to what type of mobile network the device is connected.
     * @param c The Android context
     * @return "Unknown" or
     *          for GPRS, EDGE, CDMA it returns 2G.
     *          for UMTS it returns 3G.
     *          for HSUPA, HSDPA it returns 3.5G.
     *          for LTE it returns 4G.
     */
    public static String getMobileNetworkClass() {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                c.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3.5G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

    /**
     * Reads the name of the current WiFi from the Android WiFi manager, if a WiFi is available.
     *
     * @param c The Android context
     * @return The WiFi name, if available. An empty string otherwise.
     */
    public static String getWifiName() {
        WifiManager manager = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID().replace("\"", "");
                }
            }
        }
        return "";
    }

    /**
     * Returns the type of the current network connection, if available.
     * @param c The Android context.
     * @return Either "WiFi", "4G", "3.5G", "3G", "2G" or "Unknown".
     */
    public static String getCurrentConnectionType() {
        if(isConnectedWifi(c)) {
            return "WiFi";
        }
        if(isConnectedMobile(c)) {
            return getMobileNetworkClass(c);
        }
        return "Unknown";
    }

}
