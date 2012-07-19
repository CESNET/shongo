package cz.cesnet.shongo.api.util;

/**
 * Options for {@link Converter}.
 */
public final class Options
{
    /**
     * Options for client.
     * <p/>
     * <code>{@link #storeChanges} = true</code>
     * We want to propagate changes in entities from client to server. And thus
     * {@link Converter#convertObjectToMap(Object, Options)} should store changes in client.
     * <p/>
     * <code>{@link #forceAccessible} = true<code/>
     * On client side we want to serialize/deserialize all properties.
     */
    public static final Options CLIENT = new Options(true, true);

    /**
     * Options for server.
     * <p/>
     * <code>{@link #storeChanges} = false</code>
     * We don't want to propagate changes in entities from server to client. Client always get
     * "clean" object without changes. And thus {@link Converter#convertObjectToMap(Object, Options)} should not
     * store changes on server.
     * <p/>
     * <code>{@link #forceAccessible} = false</code>
     * On server side we want to serialize/deserialize only "public" properties.
     */
    public static final Options SERVER = new Options(false, false);

    /**
     * Specifies whether marks for filled properties should be used and whether collections
     * should be stored as Maps with {@link cz.cesnet.shongo.api.ChangesTrackingObject#COLLECTION_NEW},
     * {@link cz.cesnet.shongo.api.ChangesTrackingObject#COLLECTION_MODIFIED}, {@link cz.cesnet.shongo.api.ChangesTrackingObject#COLLECTION_DELETED} lists.
     */
    private boolean storeChanges = false;

    /**
     * Options whether properties should be set with forced accessibility (e.g., to use private fields or setters).
     */
    private boolean forceAccessible = false;

    /**
     * Constructor.
     *
     * @param storeChanges    sets the {@link #storeChanges}
     * @param forceAccessible sets the {@link #forceAccessible}
     */
    public Options(boolean storeChanges, boolean forceAccessible)
    {
        this.storeChanges = storeChanges;
        this.forceAccessible = forceAccessible;
    }

    /**
     * @return {@link #storeChanges}
     */
    public boolean isStoreChanges()
    {
        return storeChanges;
    }

    /**
     * @return {@link #forceAccessible}
     */
    public boolean isForceAccessible()
    {
        return forceAccessible;
    }
}
