package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.*;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for creating, updating and deleting {@link cz.cesnet.shongo.controller.api.Resource}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationTest extends AbstractControllerTest
{
    @Test
    public void testResource() throws Exception
    {
        String userId = getUserId(SECURITY_TOKEN);
        Set<AclRecord> aclRecords = new HashSet<AclRecord>();

        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability("{hash}",
                AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        aclRecords.add(new AclRecord(userId, resourceId, Role.OWNER));
        Assert.assertEquals(aclRecords, getAclRecords());

        getResourceService().deleteResource(SECURITY_TOKEN, resourceId);

        aclRecords.remove(new AclRecord(userId, resourceId, Role.OWNER));
        Assert.assertEquals(0, aclRecords.size());
    }

    @Test
    public void testReservationRequest() throws Exception
    {
        String userId = getUserId(SECURITY_TOKEN);
        Set<AclRecord> aclRecords = new HashSet<AclRecord>();

        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability("{hash}",
                AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        aclRecords.add(new AclRecord(userId, resourceId, Role.OWNER));
        Assert.assertEquals(aclRecords, getAclRecords());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String reservationRequestId = allocate(reservationRequest);
        Reservation reservation = checkAllocated(reservationRequestId);

        aclRecords.add(new AclRecord(userId, reservationRequestId, Role.OWNER));
        aclRecords.add(new AclRecord(userId, reservation.getId(), Role.OWNER));
        Assert.assertEquals(aclRecords, getAclRecords());

        deleteAclRecord(userId, reservationRequestId, Role.OWNER);

        aclRecords.clear();
        aclRecords.add(new AclRecord(userId, resourceId, Role.OWNER));
        Assert.assertEquals(aclRecords, getAclRecords());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-02T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("test1"));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("test2"));
        reservationRequest.setSpecification(aliasSetSpecification);
        reservationRequestId = allocate(reservationRequest);
        reservation = checkAllocated(reservationRequestId);
        Reservation aliasReservation1 =
                getReservationService().getReservation(SECURITY_TOKEN, reservation.getChildReservationIds().get(0));
        Reservation aliasReservation2 =
                getReservationService().getReservation(SECURITY_TOKEN, reservation.getChildReservationIds().get(1));
        String valueReservation1 = aliasReservation1.getChildReservationIds().get(0);
        String valueReservation2 = aliasReservation2.getChildReservationIds().get(0);

        aclRecords.add(new AclRecord(userId, reservationRequestId, Role.OWNER));
        aclRecords.add(new AclRecord(userId, reservation.getId(), Role.OWNER));
        aclRecords.add(new AclRecord(userId, aliasReservation1.getId(), Role.OWNER));
        aclRecords.add(new AclRecord(userId, aliasReservation2.getId(), Role.OWNER));
        aclRecords.add(new AclRecord(userId, valueReservation1, Role.OWNER));
        aclRecords.add(new AclRecord(userId, valueReservation2, Role.OWNER));
        Assert.assertEquals(aclRecords, getAclRecords());

        deleteAclRecord(userId, reservationRequestId, Role.OWNER);

        aclRecords.clear();
        aclRecords.add(new AclRecord(userId, resourceId, Role.OWNER));
        Assert.assertEquals(aclRecords, getAclRecords());
    }

    @Test
    public void testProvidedReservation() throws Exception
    {
        String user1Id = getUserId(SECURITY_TOKEN_USER1);
        String user2Id = getUserId(SECURITY_TOKEN_USER2);

        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability("{hash}",
                AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        getResourceService().createResource(SECURITY_TOKEN_ROOT, resource);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2013-01-01T12:00", "PT4H");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        String aliasReservationRequestId = allocate(SECURITY_TOKEN_USER1, aliasReservationRequest);
        Reservation aliasReservation = checkAllocated(aliasReservationRequestId);

        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2013-01-01T12:00", "PT2H");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        reservationRequest1.addProvidedReservationId(aliasReservation.getId());
        String reservationRequest1Id = allocate(SECURITY_TOKEN_USER1, reservationRequest1);
        ExistingReservation reservation1 = (ExistingReservation) checkAllocated(reservationRequest1Id);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2013-01-01T14:00", "PT2H");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        reservationRequest2.addProvidedReservationId(aliasReservation.getId());
        String reservationRequest2Id = allocate(SECURITY_TOKEN_USER1, reservationRequest2);
        ExistingReservation reservation2 = (ExistingReservation) checkAllocated(reservationRequest2Id);

        Assert.assertEquals(aliasReservation.getId(), reservation1.getReservation().getId());
        Assert.assertEquals(aliasReservation.getId(), reservation2.getReservation().getId());

        getAuthorizationService().createAclRecord(SECURITY_TOKEN_USER1, user2Id, reservationRequest1Id, Role.OWNER);
        getAuthorizationService().createAclRecord(SECURITY_TOKEN_USER1, user2Id, reservationRequest2Id, Role.OWNER);

        Assert.assertNotNull("Reader role should be created",
                getAclRecord(user2Id, aliasReservation.getId(), Role.READER));

        deleteAclRecord(user2Id, reservationRequest1Id, Role.OWNER);

        Assert.assertNotNull("Reader role should be kept for the second request",
                getAclRecord(user2Id, aliasReservation.getId(), Role.READER));

        deleteAclRecord(user2Id, reservationRequest2Id, Role.OWNER);

        Assert.assertNull("Reader role should be deleted",
                getAclRecord(user2Id, aliasReservation.getId(), Role.READER));
    }

    /**
     * @return collection of all {@link AclRecord} for user with {@link #SECURITY_TOKEN}
     * @throws Exception
     */
    private Set<AclRecord> getAclRecords() throws Exception
    {
        return new HashSet<AclRecord>(getAuthorizationService().listAclRecords(
                SECURITY_TOKEN, getUserId(SECURITY_TOKEN), null, null));
    }

    /**
     * @param userId
     * @param entityId
     * @param role
     * @return {@link AclRecord} with given parameters
     * @throws Exception
     */
    private AclRecord getAclRecord(String userId, String entityId, Role role) throws Exception
    {
        Collection<AclRecord> aclRecords =
                getAuthorizationService().listAclRecords(SECURITY_TOKEN, userId, entityId, role);
        if (aclRecords.size() == 0) {
            return null;
        }
        if (aclRecords.size() > 1) {
            throw new RuntimeException("Multiple " + new AclRecord(userId, entityId, role).toString() + ".");
        }
        return aclRecords.iterator().next();
    }

    /**
     * Delete {@link AclRecord} with given parameters.
     *
     * @param userId
     * @param entityId
     * @param role
     * @throws Exception
     */
    private void deleteAclRecord(String userId, String entityId, Role role) throws Exception
    {
        AclRecord aclRecord = getAclRecord(userId, entityId, role);
        if (aclRecord == null) {
            throw new RuntimeException(new AclRecord(userId, entityId, role).toString() + " doesn't exist.");
        }
        getAuthorizationService().deleteAclRecord(SECURITY_TOKEN, aclRecord.getId());
    }
}
