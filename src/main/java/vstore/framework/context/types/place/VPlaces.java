package vstore.framework.context.types.place;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import vstore.framework.context.types.location.VLocation;

/**
 * A representation of the "current places" context data.
 */
@SuppressWarnings({ "rawtypes", "serial" })
public class VPlaces implements Serializable {
    private ArrayList<VSinglePlace> mPlaceList;
    
    private long timestamp;

    /**
     * @param list A list of places
     * @param timestamp The timestamp of creation of this list in milliseconds.
     */
    public VPlaces(ArrayList<VSinglePlace> list, long timestamp) {
        mPlaceList = new ArrayList<>(list);
        setTimestamp(timestamp);
    }

    /**
     * Create a places object from json.
     * @param json The json string
     * @throws ParseException is thrown if the given JSON is malformed.
     */
    public VPlaces(String json) throws ParseException {
        mPlaceList = new ArrayList<>();
        
    	JSONParser jP = new JSONParser();
        JSONObject j = (JSONObject) jP.parse(json);
        JSONObject jsonPlaces = (JSONObject)j.get("places");
        if (jsonPlaces == null) { return; }
        
        Iterator keys = jsonPlaces.keySet().iterator();
        while (keys.hasNext()) 
        {
            String sKey = (String)keys.next();
            mPlaceList.add(new VSinglePlace((JSONObject)jsonPlaces.get(sKey)));
        }
        setTimestamp((long)j.get("time"));
            
    }
    
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public long getTimestamp() { return timestamp; }

    /**
     * Appends the given place to the list of places of this places context.
     * @param p The place to append.
     */
    public void appendPlace(VSinglePlace p) {
        if(mPlaceList == null) 
        {
            mPlaceList = new ArrayList<>();
        }
        mPlaceList.add(p);
    }

    /**
     * @return A list of all nearby places in this places context.
     */
    public ArrayList<VSinglePlace> getPlaceList() {
        return new ArrayList<>(mPlaceList);
    }

    /**
     * @return The place from the list that is classified as most likely by context aggregator.
     */
    public VSinglePlace getMostLikelyPlace() {
        if(mPlaceList != null && mPlaceList.size() > 0) 
        {
            VSinglePlace mostLikelyPlace = null;
            for(int i = 0; i<mPlaceList.size(); ++i) 
            {
                VSinglePlace p = mPlaceList.get(i);
                //Find first likely place
                if(mostLikelyPlace == null) 
                {
                    if(p.isLikely()) 
                    {
                        mostLikelyPlace = p;
                    }
                //Find a better likely place
                }
                else if(p.isLikely() && p.getLikelihood() > mostLikelyPlace.getLikelihood()) 
                {
                    mostLikelyPlace = p;
                }
            }
            return mostLikelyPlace;
        }
        return null;
    }

    /**
     * Returns only those places from this Places Context, that have a certain likelihood.
     * If no place is returned, you can try to use {@link VPlaces#getMostLikelyPlace}.
     * @param likelihood The likelihood threshold in percent that has to be crossed by the
     *                   place likelihood.
     * @return A list of places from this Places Context, that have a likelihood
     * over the given threshold.
     */
    public List<VSinglePlace> filterPlaces(float likelihood) {
        List<VSinglePlace> results = new ArrayList<>();
        for(VSinglePlace p : mPlaceList) 
        {
            if(p.getLikelihood() >= likelihood) 
            {
                results.add(p);
            }
        }
        return results;
    }

    /**
     * Returns a list containing the place types that are nearby and that the framework knows.
     * 
     * @return A list containing the place types defined in {@link PlaceType}.
     * List might be empty if no places are nearby.
     */
    public List<PlaceType> getNearbyPlaceTypes() {
        List<PlaceType> types = new ArrayList<>();
        if(mPlaceList != null) 
        {
            for(VSinglePlace p : mPlaceList) 
            {
                if (!types.contains(p.getPlaceType())) 
                {
                    types.add(p.getPlaceType());
                }
            }
        }
        return types;
    }

    @SuppressWarnings("unchecked")
	public JSONObject getJson() {
        JSONObject j = new JSONObject();
        if(mPlaceList == null) { return j; }
            
        JSONObject jsonPlaces = new JSONObject();
        int i = 0;
        for (VSinglePlace p : mPlaceList) 
        {
            JSONObject jsonPlace = p.getJson();
            if(jsonPlace != null) 
            {
                jsonPlaces.put("" + i, jsonPlace);
                ++i;
            }
        }
        j.put("places", jsonPlaces);
        j.put("time", getTimestamp());
        
        return j;
    }

    /**
     * This function calculactes the approximate distance in km for every place in this
     * VPlaces object.
     * @param location The location to take as starting point for the calculation
     */
    public void calculateDistancesFrom(VLocation location) {
        for(VSinglePlace p : mPlaceList) {
            p.calculateDistanceFrom(location);
        }
    }

    /**
     * A context function that checks, if this VPlaces context contains at least one of the
     * place types that are contained in the given list.
     * 
     * @param otherTypes A list containing place types to check for in his context.
     * @return True, if at least one place type matches. False, if not.
     */
    public boolean hasAtLeastOnePlaceType(List<PlaceType> otherTypes) {
        if(mPlaceList == null) { return false; }
        
        List<PlaceType> thisTypes = getNearbyPlaceTypes();
        for(PlaceType t : otherTypes) 
        {
            if(thisTypes.contains(t)) {
                return true;
            }
        }
        return false;
    }
    /**
     * A context function that checks, if this VPlaces context contains all 
     * place types that are contained in the given list.
     * 
     * @param otherTypes A list containing place types to check for in his context.
     * @return True, if all given place types are contained in this context. 
     *         False, if not.
     */
    public boolean hasAllPlaceTypes(List<PlaceType> otherTypes) {
        if(mPlaceList == null) { return false; }
        
        List<PlaceType> thisTypes = getNearbyPlaceTypes();
        return thisTypes.containsAll(otherTypes);
    }
}
