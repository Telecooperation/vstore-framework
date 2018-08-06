package vstore.framework.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JsonUtils {

    private JsonUtils() {}

    public static int getIntFromJson(String key, JSONObject json, int defaultVal) {
        Object obj = json.get(key);
        return (obj != null) ? (Long.valueOf((long) obj).intValue()) : defaultVal;
    }
    public static long getLongFromJson(String key, JSONObject json, long defaultVal) {
        Object obj = json.get(key);
        return (obj != null) ? ((long) obj) : defaultVal;
    }

    public static String getStringFromJson(String key, JSONObject json, String defaultVal) {
        Object obj = json.get(key);
        return (obj != null) ? (String) obj : defaultVal;
    }

    public static boolean getBoolFromJson(String key, JSONObject json, boolean defaultVal) {
        Object obj = json.get(key);
        return (obj != null) ? (boolean) obj : defaultVal;
    }

    /**
     * "Robust" method for reading a float from a json.
     *
     * @param key The key in the json
     * @param json The json
     * @param defaultVal The default value to return when no value is found in the json.
     * @return The value corresponding to the key, or the default value if the key is not found.
     */
    public static float getFloatFromJson(String key, JSONObject json, float defaultVal) {
        Object obj = json.get(key);
        if(obj == null) { return defaultVal; }
        try
        {
            //This one fails, if the value in the json does not have a decimal point.
            return (float)obj;
        }
        catch(ClassCastException e1)
        {
            try {
                return (float)(double)obj;
            }
            catch(ClassCastException e2)
            {
                try {
                    return (float) (int) obj;
                }
                catch (ClassCastException e3)
                {
                    try
                    {
                        return (float) (long) obj;
                    }
                    catch (ClassCastException e4)
                    { }
                }
            }
        }
        return defaultVal;
    }

    public static JSONObject getJSONObjectFromJson(String key, JSONObject json, JSONObject defaultVal) {
        Object obj = json.get(key);
        return (obj != null) ? (JSONObject) obj : defaultVal;
    }

    public static JSONArray getJSONArrayFromJson(String key, JSONObject json, JSONArray defaultVal) {
        Object obj = json.get(key);
        return (obj != null) ? (JSONArray) obj : defaultVal;
    }
}
