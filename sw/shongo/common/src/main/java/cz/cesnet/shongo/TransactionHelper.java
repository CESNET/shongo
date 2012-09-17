package cz.cesnet.shongo;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * TOdO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TransactionHelper
{
    /**
     * Represents a transaction that can be committed.
     * If null is passed in constructor, commit do nothing.
     */
    public static class Transaction
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
        private Transaction(EntityTransaction entityTransaction)
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
         */
        public void rollback()
        {
            if (entityTransaction != null) {
                if (entityTransaction.isActive()) {
                    entityTransaction.rollback();
                }
            }
        }
    }

    private static Transaction transactionNone = new Transaction(null);

    /**
     * Begin transaction if no transaction in entity manager is active,
     * Transaction object is always returned, and commit must be called on it,
     * but only when a transaction has been started it has an effect.
     *
     * @return transaction object
     */
    public static Transaction beginTransaction(EntityManager entityManager)
    {
        EntityTransaction entityTransaction = entityManager.getTransaction();
        if (entityTransaction.isActive()) {
            return transactionNone;
        }
        return new Transaction(entityTransaction);
    }
}
