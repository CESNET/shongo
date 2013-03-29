package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.ResourceEndpoint;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestSet;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.fault.TodoImplementException;
import junitx.framework.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Collection;

/**
 * TODO: Delete this tests, it was only testing implementation of in-memory propagation of ACL records.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class InMemoryAclPropagationCacheTest extends AbstractDatabaseTest
{
    /*private AclRecordCache aclRecordCache;

    @Override
    public void before() throws Exception
    {
        super.before();

        aclRecordCache = new AclRecordCache(getEntityManagerFactory());

        Domain.setLocalDomain(new Domain("cz.cesnet"));
    }

    @Override
    public void after()
    {
        super.after();
    }

    @Test
    public void testSingleParent() throws Exception
    {
        String user1Id = "1";
        String user2Id = "2";

        EntityManager entityManager = getEntityManager();

        entityManager.getTransaction().begin();

        Reservation reservation = new Reservation();
        entityManager.persist(reservation);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setUserId(user1Id);
        reservationRequest.setReservation(reservation);
        entityManager.persist(reservationRequest);

        entityManager.getTransaction().commit();

        EntityIdentifier reservationRequestId = new EntityIdentifier(reservationRequest);
        EntityIdentifier reservationId = new EntityIdentifier(reservation);

        aclRecordCache.addAclRecord(new AclRecord(user1Id, reservationRequestId, Role.OWNER));
        Assert.assertEquals("Reservation request should be added to cache",
                1, aclRecordCache.getEntityCount());
        Assert.assertEquals("One ACL record for reservation request should be created",
                1, aclRecordCache.getAclRecordCount(reservationRequestId));

        AclRecord aclRecord = getAclRecord(user1Id, reservationId, Role.OWNER);
        Assert.assertEquals("Reservation should be automatically added to cache", 2, aclRecordCache.getEntityCount());
        Assert.assertNotNull("ACL record for reservation should be automatically created", aclRecord);

        aclRecordCache.addAclRecord(new AclRecord(user2Id, reservationRequestId, Role.OWNER));
        Assert.assertEquals("Another ACL record for reservation request should be created",
                2, aclRecordCache.getAclRecordCount(reservationRequestId));
        Assert.assertNotNull("ACL record should have been created", getAclRecord(user2Id, reservationId, Role.OWNER));
    }

    @Test
    public void testMultiParent() throws Exception
    {
        String userId = "1";

        EntityManager entityManager = getEntityManager();

        entityManager.getTransaction().begin();

        Executable executable = new ResourceEndpoint();

        Reservation reservation = new Reservation();
        reservation.setExecutable(executable);
        entityManager.persist(reservation);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setUserId(userId);
        reservationRequest.setReservation(reservation);
        entityManager.persist(reservationRequest);

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setUserId(userId);
        reservationRequestSet.addReservationRequest(reservationRequest);
        entityManager.persist(reservationRequestSet);

        entityManager.getTransaction().commit();

        EntityIdentifier reservationRequestSetId = new EntityIdentifier(reservationRequestSet);
        EntityIdentifier executableId = new EntityIdentifier(executable);

        aclRecordCache.addAclRecord(new AclRecord(userId, reservationRequestSetId, Role.OWNER));
        AclRecord aclRecord = getAclRecord(userId, executableId, Role.OWNER);
        Assert.assertEquals("Reservation request, reservation and executable should be automatically added to cache",
                4, aclRecordCache.getEntityCount());
        Assert.assertNotNull("ACL record for executable should be automatically created", aclRecord);
    }

    @Test
    public void testFullTree() throws Exception
    {
        String userId = "1";

        EntityManager entityManager = getEntityManager();

        entityManager.getTransaction().begin();

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setUserId(userId);
        entityManager.persist(reservationRequestSet);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setUserId(userId);
        reservationRequest.setReservationRequestSet(reservationRequestSet);
        entityManager.persist(reservationRequest);

        Executable executable = new ResourceEndpoint();

        Reservation reservation = new Reservation();
        reservation.setExecutable(executable);
        reservation.setReservationRequest(reservationRequest);
        entityManager.persist(reservation);

        Reservation childReservation1 = new Reservation();
        childReservation1.setParentReservation(reservation);
        entityManager.persist(childReservation1);

        ExistingReservation childReservation2 = new ExistingReservation();
        childReservation2.setParentReservation(reservation);
        entityManager.persist(childReservation2);

        Reservation reusedReservation = new Reservation();
        entityManager.persist(reusedReservation);

        childReservation2.setReservation(reusedReservation);
        entityManager.persist(childReservation2);

        entityManager.getTransaction().commit();

        EntityIdentifier reservationRequestSetId = new EntityIdentifier(reservationRequestSet);
        EntityIdentifier executableId = new EntityIdentifier(executable);
        EntityIdentifier childReservationId = new EntityIdentifier(childReservation1);
        EntityIdentifier reusedReservationId = new EntityIdentifier(reusedReservation);

        aclRecordCache.addAclRecord(new AclRecord(userId, reservationRequestSetId, Role.OWNER));
        AclRecord executableAclRecord = getAclRecord(userId, executableId, Role.OWNER);
        AclRecord childReservationAclRecord = getAclRecord(userId, childReservationId, Role.OWNER);
        AclRecord reusedReservationAclRecord = getAclRecord(userId, reusedReservationId, Role.OWNER);
        Assert.assertEquals("All child entities for reservation request set should be automatically added to cache",
                7, aclRecordCache.getEntityCount());
    }

    private AclRecord getAclRecord(String userId, EntityIdentifier entityId, Role role) throws Exception
    {
        Collection<AclRecord> aclRecords = aclRecordCache.getAclRecords(userId, entityId);
        for (AclRecord aclRecord : aclRecords) {
            if (aclRecord.getRole().equals(role)) {
                return aclRecord;
            }
        }
        return null;
    }*/
}
