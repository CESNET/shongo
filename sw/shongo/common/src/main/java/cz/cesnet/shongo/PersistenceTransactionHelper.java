package cz.cesnet.shongo;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * Represents a transaction in {@link EntityManager}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistenceTransactionHelper
{
    /**
     * Represents a transaction that can be committed.
     * If null is passed in constructor, commit do nothing.
     */
    public static class PersistenceTransaction
    {
        /**
         * Entity transaction
         */
        private EntityTransaction entityTransaction;

        /**
         * Constructor.
         *
         * @param entityTransaction
         */
        private PersistenceTransaction(EntityTransaction entityTransaction)
        {
            this.entityTransaction = entityTransaction;
            if (this.entityTransaction != null) {
                this.entityTransaction.begin();
            }
        }

        /**
         * Commit transaction.
         */
        public void commit()
        {
            if (entityTransaction != null) {
                entityTransaction.commit();
            }
        }

        /**
         * Rollback transaction.
         *
         * @return true if rollback was performed,
         *         false otherwise
         */
        public boolean rollback()
        {
            if (entityTransaction != null) {
                if (entityTransaction.isActive()) {
                    entityTransaction.rollback();
                }
                return true;
            }
            return false;
        }
    }

    private static final PersistenceTransaction TRANSACTION_NONE = new PersistenceTransaction(null);

    /**
     * Begin transaction if no transaction in entity manager is active,
     * Transaction object is always returned, and commit must be called on it,
     * but only when a transaction has been started it has an effect.
     *
     * @return transaction object
     */
    public static PersistenceTransaction beginTransaction(EntityManager entityManager)
    {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        if (entityTransaction.isActive()) {
            return TRANSACTION_NONE;
        }
        return new PersistenceTransaction(entityTransaction);
    }
}
