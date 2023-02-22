/*
package cz.cesnet.shongo.controller.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

*/
/**
 * Tests for {@link DatabaseMigration}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 *//*

public class DatabaseMigrationTest
{
    private static Logger logger = LoggerFactory.getLogger(DatabaseMigrationTest.class);

    */
/**
     * Test whether transactional DDL are allowed.
     *
     * @throws Exception
     *//*

    @Test
    public void testTransactionDDL() throws Exception
    {
        */
/*Map<String, String> properties = new HashMap<String, String>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("controller", properties);
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            entityManager.getTransaction().begin();
            entityManager.createNativeQuery("CREATE TABLE test (test bigint);").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE test add column test2 bigintx;").executeUpdate();
            entityManager.getTransaction().commit();
            Assert.fail("Alter table should fail.");
        }
        catch (Exception exception) {
            entityManager.getTransaction().rollback();
        }

        try {
            entityManager.getTransaction().begin();
            entityManager.createNativeQuery("DROP TABLE test").executeUpdate();
            entityManager.getTransaction().commit();
            Assert.fail("Table should not be created.");
        }
        catch (Exception exception) {
            entityManager.getTransaction().rollback();
        }

        entityManager.close();
        entityManagerFactory.close();*//*

    }
}
*/
