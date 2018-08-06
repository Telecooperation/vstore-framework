package vstore.framework.context.types.place;

import java.util.Arrays;
import java.util.List;

public class PlaceConstants {

    private static final String TEXT_POI = "Point of interest";
    private static final String TEXT_SHOPPING = "Shopping";
    private static final String TEXT_EVENT = "Event";
    private static final String TEXT_SOCIAL = "Social";

    private static final String TEXT_UNKNOWN = "Unknown";

    public static List<PlaceType> getPossiblePlaceTypes() {
        return Arrays.asList(PlaceType.values());
    }

    /**
     * Returns a readable english string for the given place type.
     * 
     * @param type The place type to get the string for. See {@link PlaceType}.
     * @return The readable string.
     */
    public static String getReadableString(PlaceType type) {
        String result;
        switch(type) 
        {
            case EVENT:
                result = TEXT_EVENT;
                break;
            case POI:
                result = TEXT_POI;
                break;
            case SHOPPING:
                result = TEXT_SHOPPING;
                break;
            case SOCIAL:
                result = TEXT_SOCIAL;
                break;
            case UNKNOWN:
            default:
                result = TEXT_UNKNOWN;
                break;
        }
        return result;
    }

    /**
     * Returns a place type from the given readable string.
     * 
     * @param type A readable place type string (must be supported by the framework).
     * @return Type "UNKNOWN" if the string is not supported. A type from 
     *         {@link PlaceType} otherwise.
     */
    public static PlaceType getPlaceTypeFromString(String type) {
        PlaceType result;
        switch(type) 
        {
            case TEXT_EVENT:
                result = PlaceType.EVENT;
                break;
            case TEXT_POI:
                result = PlaceType.POI;
                break;
            case TEXT_SHOPPING:
                result = PlaceType.SHOPPING;
                break;
            case TEXT_SOCIAL:
                result = PlaceType.SOCIAL;
                break;
            case TEXT_UNKNOWN:
            default:
                result = PlaceType.UNKNOWN;
                break;
        }
        return result;
    }

    private PlaceConstants() {}
}
