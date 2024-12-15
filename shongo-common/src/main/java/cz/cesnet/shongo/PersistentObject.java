package cz.cesnet.shongo;

import org.hibernate.Hibernate;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * Represents an object that can be persisted to a database (must contain unique identifier),
 * but without the {@link Id} mapping.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@MappedSuperclass
public abstract class PersistentObject
{
    /**
     * Persistent object must have an unique identifier.
     */
    protected Long id;

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
     * Set id to null.
     */
    protected void setIdNull()
    {
        this.id = null;
    }

    /**
     * @return {@link #id}
     */
    @Transient
    public abstract Long getId();

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
        return this.id != null && this.id.equals(longId);
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
        return this.id != null && this.id.equals(id);
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
     * Load all lazy properties.
     */
    public void loadLazyProperties()
    {
    }

    /**
     * Get implementation for given {@code object} in case that it is a {@link org.hibernate.proxy.HibernateProxy}.
     * <p/>
     * Getters which uses this method for retrieving the values should not be annotated with
     * {@link jakarta.persistence.Access} equaled to {@link jakarta.persistence.AccessType#FIELD},
     * because it may end up in "org.hibernate.AssertionFailure: Unable to perform un-delete for instance ..."
     * exception.
     *
     * @param object from which the implementation should be returned
     * @return implementation of given lazy {@code object}
     */
    @SuppressWarnings("unchecked")
    public static <T> T getLazyImplementation(T object)
    {
        return (T) Hibernate.unproxy(object);
    }
}
