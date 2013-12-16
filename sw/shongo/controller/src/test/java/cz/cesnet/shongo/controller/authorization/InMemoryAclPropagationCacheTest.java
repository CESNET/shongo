package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import org.junit.Test;

/**
 * TODO: Delete this tests, it was only testing implementation of in-memory propagation of ACL entries.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class InMemoryAclPropagationCacheTest extends AbstractDatabaseTest
{
    @Test
    public void test() throws Exception
    {
    }

    /*private AclEntryCache aclEntryCache;

    @Override
    public void before() throws Exception
    {
        super.before();

        aclEntryCache = new AclEntryCache(getEntityManagerFactory());

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

        EntityManager entityManager = createEntityManager();

        entityManager.getTransaction().begin();

        Reservation reservation = new Reservation();
        entityManager.persist(reservation);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setUserId(user1Id);
        reservationRequest.setReservation(reservation);
        entityManager.persist(reservationRequest);

        entityManager.getTransaction().commit();

        ObjectIdentifier reservationRequestId = new ObjectIdentifier(reservationRequest);
        ObjectIdentifier reservationId = new ObjectIdentifier(reservation);

        aclEntryCache.addAclEntry(new AclEntry(user1Id, reservationRequestId, Role.OWNER));
        Assert.assertEquals("Reservation request should be added to cache",
                1, aclEntryCache.getEntityCount());
        Assert.assertEquals("One ACL entry for reservation request should be created",
                1, aclEntryCache.getAclEntryCount(reservationRequestId));

        AclEntry aclEntry = getAclEntry(user1Id, reservationId, Role.OWNER);
        Assert.assertEquals("Reservation should be automatically added to cache", 2, aclEntryCache.getEntityCount());
        Assert.assertNotNull("ACL entry for reservation should be automatically created", aclEntry);

        aclEntryCache.addAclEntry(new AclEntry(user2Id, reservationRequestId, Role.OWNER));
        Assert.assertEquals("Another ACL entry for reservation request should be created",
                2, aclEntryCache.getAclEntryCount(reservationRequestId));
        Assert.assertNotNull("ACL entry should have been created", getAclEntry(user2Id, reservationId, Role.OWNER));
    }

    @Test
    public void testMultiParent() throws Exception
    {
        String userId = "1";

        EntityManager entityManager = createEntityManager();

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

        ObjectIdentifier reservationRequestSetId = new ObjectIdentifier(reservationRequestSet);
        ObjectIdentifier executableId = new ObjectIdentifier(executable);

        aclEntryCache.addAclEntry(new AclEntry(userId, reservationRequestSetId, Role.OWNER));
        AclEntry aclEntry = getAclEntry(userId, executableId, Role.OWNER);
        Assert.assertEquals("Reservation request, reservation and executable should be automatically added to cache",
                4, aclEntryCache.getEntityCount());
        Assert.assertNotNull("ACL entry for executable should be automatically created", aclEntry);
    }

    @Test
    public void testFullTree() throws Exception
    {
        String userId = "1";

        EntityManager entityManager = createEntityManager();

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

        ObjectIdentifier reservationRequestSetId = new ObjectIdentifier(reservationRequestSet);
        ObjectIdentifier executableId = new ObjectIdentifier(executable);
        ObjectIdentifier childReservationId = new ObjectIdentifier(childReservation1);
        ObjectIdentifier reusedReservationId = new ObjectIdentifier(reusedReservation);

        aclEntryCache.addAclEntry(new AclEntry(userId, reservationRequestSetId, Role.OWNER));
        AclEntry executableAclEntry = getAclEntry(userId, executableId, Role.OWNER);
        AclEntry childReservationAclEntry = getAclEntry(userId, childReservationId, Role.OWNER);
        AclEntry reusedReservationAclEntry = getAclEntry(userId, reusedReservationId, Role.OWNER);
        Assert.assertEquals("All child entities for reservation request set should be automatically added to cache",
                7, aclEntryCache.getEntityCount());
    }

    private AclEntry getAclEntry(String userId, ObjectIdentifier objectId, Role role) throws Exception
    {
        Collection<AclEntry> aclEntrys = aclEntryCache.getAclEntries(userId, objectId);
        for (AclEntry aclEntry : aclEntrys) {
            if (aclEntry.getRole().equals(role)) {
                return aclEntry;
            }
        }
        return null;
    }*/
}
