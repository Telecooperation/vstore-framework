package vstore.framework.file.SimplePrefFile;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.util.Set;

import vstore.framework.utils.FileUtils;

public class PrefFile {

    File origin;
    JSONObject fileContent;

    public PrefFile(File f) {
        if(!f.exists()) { return; }
        origin = f;
        String jsonStr = FileUtils.readStringFromFile(f);

        try
        {
            JSONParser p = new JSONParser();
            fileContent = (JSONObject) p.parse(jsonStr);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            fileContent = new JSONObject();
            flush();
        }
    }

    public String get(String key, String defaultStr) {
        if(fileContent.containsKey(key))
        {
            return (String)fileContent.get(key);
        }
        return defaultStr;
    }
    public boolean getBoolean(String key, boolean defaultVal) {
        if(fileContent.containsKey(key))
        {
            return (boolean)fileContent.get(key);
        }
        return defaultVal;
    }

    public int getInt(String key, int defaultInt) {
        if(fileContent.containsKey(key)) {
            return (int)fileContent.get(key);
        }
        return defaultInt;
    }

    public String[] keys() {
        Set keys = fileContent.keySet();
        String[] keyArr = new String[keys.size()];
        int count = 0;
        for(Object key : keys) {
            keyArr[count] = (String)key;
            ++count;
        }
        return keyArr;
    }

    public void put(String key, String value) {
        fileContent.put(key, value);
    }
    public void putBoolean(String key, boolean value) {
        fileContent.put(key, value);
    }
    public void putInt(String key, int value) {
        fileContent.put(key, value);
    }

    public void remove(String key) {
        fileContent.remove(key);
    }

    public File getOrigin() {
        return origin;
    }

    public void flush() {
        String jsonStr = fileContent.toJSONString();
        FileUtils.writeStringToFile(jsonStr, origin.getParentFile(), origin.getName());
    }

    public void clear() {
        fileContent.clear();
        flush();
    }
}
