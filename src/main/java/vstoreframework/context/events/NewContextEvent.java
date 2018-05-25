package vstoreframework.context.events;

/**
 * This event gets published when the Context Aggregator received new context.
 */
public class NewContextEvent {
    public boolean isUpdated_all = false;
    public boolean isUpdated_location = false;
    public boolean isUpdated_places = false;
    public boolean isUpdated_activity = false;
    public boolean isUpdated_network = false;
    public boolean isUpdated_noise = false;

    /**
     *
     */
    public NewContextEvent() {

    }

    /**
     *
     * @param all Set this to true if the whole context is new.
     */
    public NewContextEvent(boolean all) {
        this(all, true, true, true, true, true);
    }


    /**
     * @param all Set this to true if the whole context is new.
     * @param location Set this to true if the location has been updated.
     * @param places Set this to true if the places have been updated.
     * @param activity Set this to true if the user activity has been updated.
     * @param network Set this to true if the network has been updated.
     * @param noise Set this to true if the noise has been updated.
     */
    public NewContextEvent(boolean all, boolean location, boolean places, boolean activity,
                           boolean noise, boolean network) {
        isUpdated_all = all;
        isUpdated_location = location;
        isUpdated_places = places;
        isUpdated_activity = activity;
        isUpdated_network = network;
        isUpdated_noise = noise;
    }
}
