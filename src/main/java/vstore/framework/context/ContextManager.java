package vstore.framework.context;

public class ContextManager {
    /**
     * This field contains the current context description.
     */
    private static ContextDescription mCurrentContext;

    private static ContextManager instance;

    private ContextManager() {}

    /**
     * Initializes the context manager by reading the context data
     * from the persistent file, if available.
     */
    public static void initialize() {
        if(instance == null)
        {
            instance = new ContextManager();
        }

        //Initialize the current context description
        mCurrentContext = new ContextDescription();
        //Check if we have persistent context in the context file
        ContextDescription tmpCtx = ContextFile.getContext();
        if(tmpCtx != null)
        {
            mCurrentContext = tmpCtx;
        }
    }

    public static ContextManager get() {
        initialize();
        return instance;
    }

    /**
     * Use this method to provide new context information to the framework.
     * If the new information should be persistent after a restart of the
     * framework, {@link ContextManager#persistContext(boolean)} should be called.
     *
     * @param context The new context information
     *
     * @return Returns the context manager instance again to simplify method-chaining.
     */
    public ContextManager provideContext(ContextDescription context) {
        mCurrentContext = context;
        return this;
    }

    /**
     * Use this method to make the currently configured usage context
     * persistent after a restart of the framework.
     *
     * @param makePersistent True if the context should be persistent.
     *                       False if you want to undo this.
     *
     * @return Returns the context manager instance again to simplify method-chaining.
     */
    public ContextManager persistContext(boolean makePersistent) {
        if(makePersistent && mCurrentContext != null)
        {
            ContextFile.write(mCurrentContext.getJson());
            return this;
        }
        if(!makePersistent)
        {
            ContextFile.clearContext();
        }
        return this;
    }


    /**
     * This method clears the current usage context and resets it
     * to an empty state.
     *
     * @param keepPersistent If set to true, the context currently stored
     * persistently (if any) will not be deleted.
     *
     * @return Returns the context manager instance again to simplify method-chaining.
     */
    public ContextManager clearCurrentContext(boolean keepPersistent) {
        mCurrentContext = new ContextDescription();
        if(!keepPersistent)
        {
            ContextFile.clearContext();
        }
        return this;
    }

    /**
     * This method returns the usage context currently used for matching.
     * If you want to refresh it, use {@link ContextManager#provideContext(ContextDescription)}
     *
     * @return A ContextDescription-object containing the current usage
     *         context description
     */
    public final ContextDescription getCurrentContext() {
        return mCurrentContext;
    }

}
