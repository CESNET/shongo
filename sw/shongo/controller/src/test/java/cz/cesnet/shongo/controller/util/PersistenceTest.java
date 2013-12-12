package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.controller.EntityRole;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.AclRecordDependency;
import cz.cesnet.shongo.util.Timer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for JPA.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistenceTest
{
    private static Logger logger = LoggerFactory.getLogger(PersistenceTest.class);

    private EntityManagerFactory entityManagerFactory;

    @Before
    public void before() throws Exception
    {
        // For testing purposes use only in-memory database
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        properties.put("hibernate.connection.url", "jdbc:hsqldb:mem:test; shutdown=true;");
        properties.put("hibernate.connection.username", "sa");
        properties.put("hibernate.connection.password", "");

        logger.info("Creating entity manager factory...");
        Timer timer = new Timer();
        entityManagerFactory = Persistence.createEntityManagerFactory("persistence-test", properties);
        logger.info("Entity manager factory created in {} ms.", timer.stop());
    }

    @After
    public void after() throws Exception
    {
        entityManagerFactory.close();
    }

    @Test
    public void test() throws Exception
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            AclRecord aclRecord1 = new AclRecord();
            aclRecord1.setUserId("0");
            aclRecord1.setEntityId(new AclRecord.EntityId(AclRecord.EntityType.RESERVATION, 1l));
            aclRecord1.setEntityRole(EntityRole.OWNER);
            entityManager.persist(aclRecord1);

            AclRecord aclRecord2 = new AclRecord();
            aclRecord2.setUserId("0");
            aclRecord2.setEntityId(new AclRecord.EntityId(AclRecord.EntityType.RESERVATION, 1l));
            aclRecord2.setEntityRole(EntityRole.OWNER);
            entityManager.persist(aclRecord2);

            AclRecordDependency aclRecordDependency1 = new AclRecordDependency();
            aclRecordDependency1.setParentAclRecord(aclRecord1);
            aclRecordDependency1.setChildAclRecord(aclRecord2);
            entityManager.persist(aclRecordDependency1);

            AclRecordDependency aclRecordDependency2 = new AclRecordDependency();
            aclRecordDependency2.setParentAclRecord(aclRecord1);
            aclRecordDependency2.setChildAclRecord(aclRecord2);
            entityManager.persist(aclRecordDependency2);

            entityManager.getTransaction().commit();

            Assert.fail("Constraint violation exception should be thrown.");
        }
        catch (javax.persistence.PersistenceException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            Assert.assertEquals("Constraint violation exception should be thrown.",
                    org.hibernate.exception.ConstraintViolationException.class, exception.getCause().getClass());
        }
        finally {
            entityManager.close();
        }
    }
}
