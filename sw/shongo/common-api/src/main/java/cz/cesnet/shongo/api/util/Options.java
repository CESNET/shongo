package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.annotation.ReadOnly;

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
     * {@link Converter#convertToBasic(Object, Options)} should store changes in client.
     * <p/>
     * <code>{@link #forceAccessible} = true<code/>
     * On client side we want to serialize/deserialize all properties.
     */
    public static final Options CLIENT = new Options(true, true, true, false);

    /**
     * Options for server.
     * <p/>
     * <code>{@link #storeChanges} = false</code>
     * We don't want to propagate changes in entities from server to client. Client always get
     * "clean" object without changes. And thus {@link Converter#convertToBasic(Object, Options)} should not
     * store changes on server.
     * <p/>
     * <code>{@link #forceAccessible} = false</code>
     * On server side we want to serialize/deserialize only "public" properties.
     */
    public static final Options SERVER = new Options(false, false, false, true);

    /**
     * Specifies whether marks for filled properties should be used and whether collections
     * should be stored as Maps with {@link ChangesTrackingObject#COLLECTION_NEW},
     * {@link ChangesTrackingObject#COLLECTION_MODIFIED}, {@link ChangesTrackingObject#COLLECTION_DELETED} lists.
     */
    private boolean storeChanges = false;

    /**
     * Options whether properties should be set with forced accessibility (e.g., to use private fields or setters).
     */
    private boolean forceAccessible = false;

    /**
     * Specifies whether {@link ReadOnly} properties should be loaded.
     */
    private boolean loadReadOnly = false;

    /**
     * Specifies whether {@link ReadOnly} properties should be stored.
     */
    private boolean storeReadOnly = false;

    /**
     * Constructor.
     *
     * @param storeChanges  sets the {@link #storeChanges}
     * @param loadReadOnly  sets the {@link #loadReadOnly}
     * @param storeReadOnly sets the {@link #storeReadOnly}
     */
    public Options(boolean storeChanges, boolean forceAccessible, boolean loadReadOnly, boolean storeReadOnly)
    {
        this.storeChanges = storeChanges;
        this.forceAccessible = forceAccessible;
        this.loadReadOnly = loadReadOnly;
        this.storeReadOnly = storeReadOnly;
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

    /**
     * @return {@link #loadReadOnly}
     */
    public boolean isLoadReadOnly()
    {
        return loadReadOnly;
    }

    /**
     * @return {@link #storeReadOnly}
     */
    public boolean isStoreReadOnly()
    {
        return storeReadOnly;
    }
}
