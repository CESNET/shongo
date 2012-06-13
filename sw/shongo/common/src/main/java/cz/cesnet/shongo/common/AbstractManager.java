package cz.cesnet.shongo.common;

import javax.persistence.EntityManager;

/**
 * Abstract DAO (Data Access Object) for persistent objects.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractManager
{
    /**
     * Entity manager that is used for managing persistent objects.
     */
    protected EntityManager entityManager;

    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public AbstractManager(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    /**
     * @param persistentObject object to be created in the database
     */
    protected void create(PersistentObject persistentObject)
    {
        entityManager.persist(persistentObject);
    }

    /**
     * @param persistentObject object to be updated in the database
     */
    protected void update(PersistentObject persistentObject)
    {
        persistentObject.checkPersisted();
        entityManager.persist(persistentObject);
    }

    /**
     * @param persistentObject object to be removed from the database
     */
    protected void delete(PersistentObject persistentObject)
    {
        persistentObject.checkPersisted();
        entityManager.remove(persistentObject);
    }
}
