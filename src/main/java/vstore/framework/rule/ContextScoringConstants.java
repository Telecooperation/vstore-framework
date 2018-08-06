package vstore.framework.rule;


import java.util.Arrays;
import java.util.List;

public class ContextScoringConstants {
    private ContextScoringConstants() {}
    public static final String KEY_LOCATION = "s_location";
    public static final String KEY_WEEKDAYS = "s_weekdays";
    public static final String KEY_TIMESPAN = "s_timespan";
    public static final String KEY_PLACES = "s_places";
    public static final String KEY_SHARINGDOMAIN = "s_sharingd";
    public static final String KEY_ACTIVITY = "s_activity";
    public static final String KEY_NETWORK = "s_network";
    public static final String KEY_NOISE = "s_noise";

    public static final List<String> keys = Arrays.asList(KEY_LOCATION, KEY_WEEKDAYS, KEY_TIMESPAN, KEY_PLACES,
            KEY_SHARINGDOMAIN, KEY_ACTIVITY, KEY_NETWORK, KEY_NOISE);
}
