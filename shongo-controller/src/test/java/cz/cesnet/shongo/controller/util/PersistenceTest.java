package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.acl.*;
import cz.cesnet.shongo.controller.authorization.AclEntryDependency;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
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
    public void testAclEntryUniqueness() throws Exception
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            TestReservation reservation = new TestReservation();
            reservation.setId(1l);

            AclProvider aclProvider = new AclProvider(entityManagerFactory)
            {
                @Override
                protected String getObjectClassName(Class<? extends PersistentObject> objectClass)
                {
                    return objectClass.getSimpleName();
                }

                @Override
                protected Long getObjectId(PersistentObject object)
                {
                    return object.getId();
                }
            };
            AclIdentity aclIdentity = aclProvider.getIdentity(AclIdentityType.USER, "0");
            AclObjectIdentity aclObjectIdentity = aclProvider.getObjectIdentity(reservation);

            AclEntry aclEntry1 = new AclEntry();
            aclEntry1.setIdentity(aclIdentity);
            aclEntry1.setObjectIdentity(aclObjectIdentity);
            aclEntry1.setRole(ObjectRole.OWNER.toString());
            entityManager.persist(aclEntry1);

            AclEntry aclEntry2 = new AclEntry();
            aclEntry2.setIdentity(aclIdentity);
            aclEntry2.setObjectIdentity(aclObjectIdentity);
            aclEntry2.setRole(ObjectRole.OWNER.toString());
            entityManager.persist(aclEntry2);

            AclEntryDependency aclEntryDependency1 = new AclEntryDependency();
            aclEntryDependency1.setParentAclEntry(aclEntry1);
            aclEntryDependency1.setChildAclEntry(aclEntry2);
            entityManager.persist(aclEntryDependency1);

            AclEntryDependency aclEntryDependency2 = new AclEntryDependency();
            aclEntryDependency2.setParentAclEntry(aclEntry1);
            aclEntryDependency2.setChildAclEntry(aclEntry2);
            entityManager.persist(aclEntryDependency2);

            entityManager.getTransaction().commit();

            Assert.fail("Constraint violation exception should be thrown.");
        }
        catch (javax.persistence.PersistenceException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            Assert.assertEquals("Constraint violation exception should be thrown.",
                    org.hibernate.exception.ConstraintViolationException.class, exception.getCause().getCause().getClass());
        }
        finally {
            entityManager.close();
        }
    }

    public static class TestReservation extends Reservation
    {
        public void setId(Long id)
        {
            this.id = id;
        }
    }
}
