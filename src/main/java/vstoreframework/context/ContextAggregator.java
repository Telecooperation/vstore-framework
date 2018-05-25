package vstoreframework.context;

import vstoreframework.context.types.activity.VActivity;
import vstoreframework.context.types.location.VLocation;
import vstoreframework.context.types.network.VNetwork;
import vstoreframework.context.types.noise.VNoise;

/**
 * This class saves new context information to the database as it arrives.
 * 
 * TODO
 */
public class ContextAggregator extends Thread {
       
    public static void storeLocationContext(VLocation location) {
        
    }

    /**
     * Updates the activity context.
     * If the given one is a different type, we update the value used by the framework.
     * If the type is still the same, we only update the timestamp to reflect the duration this
     * activity is already given.
     *
     * @param c The Android context
     */
    public static void storeActivityContext( VActivity activity) {
    
    }

    /**
     * Updates the network context in the shared pref to the given one.
     *
     * @param c The Android context
     * @param net The VNetwork context object to store in the framework.
     */
    public static void storeNetworkContext(VNetwork net) {
    
    }

    /**
     * Updates the noise context in the shared pref to the given one.
     *
     * @param c The Android context
     * @param noise The VNoise context object to store in the framework.
     */
    public static void storeNoiseContext(VNoise noise) {

    }
}
