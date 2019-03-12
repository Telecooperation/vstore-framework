package vstore.framework.utils;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utilities for working with numbers.
 */
public class NumberUtils {

    private static final Logger LOGGER = LogManager.getLogger(NumberUtils.class);

    /**
     * Maps the given number x from the given range to the given new range.
     * @param x The number to map from range [from_min, from_max] to [to_min, to_max].
     * @param from_min The lower bound of the input range.
     * @param from_max The upper bound of the input range.
     * @param to_min The lower bound of the target range
     * @param to_max The upper bound of the target range.
     * @return The number of the new range.
     */
    public static float mapToRange(float x, float from_min, float from_max, float to_min, float to_max) {
        if(from_max - from_min != 0) {
            float result = (x - from_min) * (to_max - to_min) / (from_max - from_min) + to_min;
            LOGGER.debug("mapToRange: x=" + x + ",from_min=" + from_min + ",from_max=" + from_max + ",to_min=" + to_min + ",to_max=" + to_max);
            LOGGER.debug("mapToRange result=" + result);
            return result;
        } else {
            LOGGER.warn("mapToRange: max-min is 0");
            return 0;
        }
    }

    /**
     * Returns the index of the number from the given list that is closest to the second parameter number.
     * @param numbers The list of numbers to check against the second parameter.
     * @param check The number for which to find the closest one of the list.
     * @return The closest number from the list.
     */
    public static int getClosestNumber(List<Float> numbers, float check) {
        if(numbers != null && numbers.size() > 0) {
            float lastDistance = Math.abs(numbers.get(0) - check);
            int lastNumber = 0;
            for(int i = 1; i<numbers.size(); ++i) {
                if(Math.abs(numbers.get(i) - check) < lastDistance) {
                    lastNumber = i;
                    lastDistance = Math.abs(numbers.get(i) - check);
                }
            }
            LOGGER.debug("getClosestNumber to " + check + "is " + lastNumber);
            return lastNumber;
        }
        LOGGER.warn("getClosestNumber is 0");
        return 0;
    }

}
