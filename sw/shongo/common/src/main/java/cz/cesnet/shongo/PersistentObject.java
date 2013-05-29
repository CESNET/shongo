package cz.cesnet.shongo;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * Represents an object that can be persisted to a database (must contain unique identifier).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@MappedSuperclass
public abstract class PersistentObject
{
    /**
     * Persistent object must have an unique identifier.
     */
    private Long id;

    /**
     * Auto generated id counter.
     */
    private static long testingIdCounter = 0;

    /**
     * Sets auto generated testing in-memory identifier.
     */
    @Transient
    public static void resetTestingId()
    {
        synchronized (PersistentObject.class) {
            testingIdCounter = 0;
        }
    }

    /**
     * @return {@link #id}
     */
    @Id
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    /**
     * @param id
     * @return true whether {@link #id} equals given {@code id},
     *         false otherwise
     */
    public boolean equalsId(String id)
    {
        Long longId = null;
        if (id != null) {
            longId = Long.valueOf(id);
        }
        return this.id.equals(longId);
    }

    /**
     * @param id
     * @return true whether {@link #id} equals given {@code id},
     *         false otherwise
     */
    public boolean equalsId(Long id)
    {
        if (this.id == null && id != null) {
            return true;
        }
        return this.id.equals(id);
    }

    /**
     * @param id sets the {@link #id}
     */
    private void setId(Long id)
    {
        this.id = id;
    }

    @Transient
    public void generateTestingId()
    {
        synchronized (PersistentObject.class) {
            this.id = ++testingIdCounter;
        }
    }

    /**
     * @return true if object has already been persisted, otherwise false
     */
    @Transient
    public boolean isPersisted()
    {
        return id != null;
    }

    /**
     * Checks whether object has already been persisted.
     *
     * @throws RuntimeException
     */
    public void checkPersisted() throws RuntimeException
    {
        if (!isPersisted()) {
            throw new RuntimeException(this.getClass().getSimpleName() + " hasn't been persisted yet!");
        }
    }

    /**
     * Checks whether object has not been persisted yet.
     *
     * @throws RuntimeException
     */
    public void checkNotPersisted() throws RuntimeException
    {
        if (isPersisted()) {
            throw new RuntimeException(this.getClass().getSimpleName() + " has already been persisted!");
        }
    }

    /**
     * Load all lazy collections.
     */
    public void loadLazyCollections()
    {
    }
}
