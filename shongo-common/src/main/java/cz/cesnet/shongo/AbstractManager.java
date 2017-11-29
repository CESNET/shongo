package cz.cesnet.shongo;


import javax.persistence.EntityManager;

/**
 * Abstract DAO (Data Access Object) for persistent objects.
 * <p/>
 * Managers are responsible for loading/saving entities and
 * they represents a single place where all queries for a single
 * entity are placed.
 * <p/>
 * TODO: how to delete one-to-one referenced entities when are replaced by another one?
 * https://hibernate.onjira.com/browse/HHH-6484?page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractManager extends PersistenceTransactionHelper
{
    /**
     * Entity manager that is used for managing persistent objects.
     */
    protected EntityManager entityManager;

    /**
     * Constructor.
     *
     * @param entityManager sets the {@link #entityManager}
     */
    public AbstractManager(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    /**
     * @return {@link #entityManager}
     */
    public EntityManager getEntityManager()
    {
        return entityManager;
    }

    /**
     * @param persistentObject object to be created in the database
     */
    protected void create(PersistentObject persistentObject)
    {
        persistentObject.checkNotPersisted();
        PersistenceTransaction transaction = beginPersistenceTransaction();
        entityManager.persist(persistentObject);
        transaction.commit();
    }

    /**
     * @param persistentObject object to be updated in the database
     */
    protected void update(PersistentObject persistentObject)
    {
        persistentObject.checkPersisted();
        PersistenceTransaction transaction = beginPersistenceTransaction();
        if (!entityManager.contains(persistentObject)) {
            entityManager.merge(persistentObject);
        }
        else {
            entityManager.persist(persistentObject);
        }
        transaction.commit();
    }

    /**
     * @param persistentObject object to be removed from the database
     */
    protected void delete(PersistentObject persistentObject)
    {
        persistentObject.checkPersisted();
        PersistenceTransaction transaction = beginPersistenceTransaction();
        entityManager.remove(persistentObject);
        transaction.commit();
    }

    /**
     * Begin transaction if no transaction in entity manager is active,
     * Transaction object is always returned, and commit must be called on it,
     * but only when a transaction has been started it has an effect.
     *
     * @return transaction object
     */
    protected PersistenceTransaction beginPersistenceTransaction()
    {
        return beginTransaction(entityManager);
    }
}
