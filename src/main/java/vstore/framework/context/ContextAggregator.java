package vstore.framework.context;

import vstore.framework.context.types.activity.VActivity;
import vstore.framework.context.types.location.VLocation;
import vstore.framework.context.types.network.VNetwork;
import vstore.framework.context.types.noise.VNoise;

/**
 * This class saves new context information to the context file as it arrives.
 */
public class ContextAggregator extends Thread {
       
    public static void storeLocationContext(VLocation location) {
        
    }

    /**
     * Updates the activity context.
     * If the given one is a different type, we update the value used by the framework.
     * If the type is still the same, we only update the timestamp to reflect the duration this
     * activity is already given.
     */
    public static void storeActivityContext(VActivity activity) {
    
    }

    /**
     * Updates the network context in the shared pref to the given one.
     *
     * @param net The VNetwork context object to store in the framework.
     */
    public static void storeNetworkContext(VNetwork net) {
    
    }

    /**
     * Updates the noise context in the shared pref to the given one.
     *
     * @param noise The VNoise context object to store in the framework.
     */
    public static void storeNoiseContext(VNoise noise) {

    }
}
