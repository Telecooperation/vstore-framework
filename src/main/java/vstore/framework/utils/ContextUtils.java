package vstore.framework.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import vstore.framework.context.ContextDescription;
import vstore.framework.context.ContextFilter;
import vstore.framework.context.SearchContextDescription;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.context.types.network.cellular.CellularNetwork;

/**
 * This class provides some convenience functions for working with context.
 */
public class ContextUtils {
    /**
     * Calculates the distance between two given latitude/longitude pairs in meters.
     * 
     * Taken from: https://introcs.cs.princeton.edu/java/44st/Location.java.html
     * 
     * @param one The first pair.
     * @param two The second pair.
     * @return The distance between the two pairs in meters.
     */
    public static float distanceBetween(VLatLng one, VLatLng two) {
        if(one == null || two == null) { return -1; }
        
        double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
        double lat1 = Math.toRadians(one.getLatitude());
        double lon1 = Math.toRadians(one.getLongitude());
        double lat2 = Math.toRadians(two.getLatitude());
        double lon2 = Math.toRadians(two.getLongitude());
        
        // Great circle distance in radians, using law of cosines formula
        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) 
                                 * Math.cos(lon1 - lon2));
        
        // each degree on a great circle of Earth is 60 nautical miles
        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return ((float)statuteMiles) * (1609.34f); //1609.34 == conversion factor miles <=> meters
    }

    /**
     * Determines if the current time is between the given time.
     * @param startHour The starting hour of the time span to check for (0-23)
     * @param startMinutes The starting minutes of the time span to check for (0-23)
     * @param endHour The end hour of the time span to check for
     * @param endMinutes The end minutes of the time span to check for
     * @return True, if the current time of today lies in the given time span.
     */
    public static boolean isNowBetween(int startHour, int startMinutes, int endHour, int endMinutes) {
        return isDateBetween(new Date(), startHour, startMinutes, endHour, endMinutes);
    }

    /**
     * Determines if the time of the given date object lies in the given time span.
     *
     * @param d The date object for which the time is checked.
     * @param startHour The starting hour of the time span to check for (0-23)
     * @param startMinutes The starting minutes of the time span to check for (0-23)
     * @param endHour The end hour of the time span to check for
     * @param endMinutes The end minutes of the time span to check for
     * @return True, if the time of the given date lies in the given time span.
     */
    public static boolean isDateBetween(Date d, int startHour, int startMinutes, int endHour, int endMinutes) {
        if(d == null) {
            d = new Date();
        }
        if(startHour >= 0 && startHour <= 23 && startMinutes >=0 && startMinutes <=59 &&
                endHour >= 0 && endHour <= 23 && endMinutes >=0 && endMinutes <=59) {
            int start = startHour * 60 + startMinutes;
            int end = endHour * 60 + endMinutes;
            if (start < end) {
                Calendar cal = GregorianCalendar.getInstance();
                cal.setTime(d);
                int hNow = cal.get(Calendar.HOUR_OF_DAY);
                int mNow = cal.get(Calendar.MINUTE);
                int now = hNow * 60 + mNow;
                if (now >= start && now <= end) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks, if the given date is on a weekend or not.
     *
     * @param d The date object for which the weekday is checked.
     * @return True, if it is weekend. False, if not.
     */
    public static boolean isWeekend(Date d) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(d);
        int day = cal.get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
    }

    /**
     * @return The current day of the week (1=monday and 7=sunday).
     */
    public static int getDayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        //Fix the day number (Calendar API starts with 1 = sunday, but vStore uses 1 = monday)
        if (day == Calendar.SUNDAY) {
            day = 7;
        } else {
            day = day - 1;
        }
        return day;
    }

    /**
     * Checks, if sharing domain 1 is included in sharing domain 2.
     * @param domain1 The sharing domain to check against sharing domain 2.
     * @param domain2 The sharing domain that is checked against.
     * @return True, if the sharing domain 1 is included in the sharing domain 2.
     */
    public static boolean isIncludedInSharingDomain(int domain1, int domain2) {
        switch(domain2) {
            case -1:
                return true;
            case 0:
            case 1:
                return domain1 == domain2;
            default:
                return false;
        }
    }

    public static SearchContextDescription applyFilter(ContextDescription usageContext, ContextFilter filter) {
        //Apply the given filter to the context and create a new SearchContextDescription
        SearchContextDescription ctx = new SearchContextDescription();
        if(filter != null) {
            if(filter.isLocationEnabled() && usageContext.hasLocationContext()) {
                ctx.setLocationContext(usageContext.getLocationContext());
                ctx.setRadius(filter.getRadius());
            }
            if(filter.isPlaceEnabled()) {
                if(usageContext.getMostLikelyPlace() != null &&
                        usageContext.getMostLikelyPlace().getLikelihood() > 0.05) {
                    ctx.setPlace(usageContext.getMostLikelyPlace().getName());
                }
            }
            if(filter.isActivityEnabled()) {
                ctx.setActivityContext(usageContext.getActivityContext());
            }
            if(filter.isNetworkEnabled()) {
                ctx.setNetworkContext(usageContext.getNetworkContext());
            }
            if(filter.isNoiseEnabled()) {
                ctx.setNoiseContext(usageContext.getNoiseContext());
            }
            if(filter.isWeekdayEnabled()) {
                ctx.setDayOfWeek(usageContext.getDayOfWeek());
            }
            if(filter.isTimeOfDayEnabled()) {
                ctx.enableTimeOfDay(filter.isTimeOfDayEnabled());
                ctx.setTimespanInMs(filter.getTimeSpanMS());
            }
        }
        return ctx;
    }

    /**
     * @return A list of supported mobile network types
     */
    public static CellularNetwork.MobileType[] getSupportedMobileTypes() {
        return CellularNetwork.MobileType.values();
    }
}
