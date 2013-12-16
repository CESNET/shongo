package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclEntryListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for creating, updating and deleting {@link cz.cesnet.shongo.controller.api.AclEntry}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationTest extends AbstractControllerTest
{
    @Test
    public void testSystemPermissions() throws Exception
    {
        AuthorizationService service = getAuthorizationService();

        Assert.assertFalse("Ordinary user shouldn't have the administration permission",
                service.hasSystemPermission(SECURITY_TOKEN_USER1, SystemPermission.ADMINISTRATION));

        getAuthorization().addAdministratorUserId(getUserId(SECURITY_TOKEN_USER1));
        Assert.assertTrue("Administrator should have the administration permission",
                service.hasSystemPermission(SECURITY_TOKEN_USER1, SystemPermission.ADMINISTRATION));
    }

    @Test
    public void testResource() throws Exception
    {
        String userId = getUserId(SECURITY_TOKEN);
        Set<AclEntry> aclEntries = new HashSet<AclEntry>();

        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability("{hash}",
                AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        aclEntries.add(new AclEntry(userId, resourceId, ObjectRole.OWNER));
        Assert.assertEquals(aclEntries, getAclEntries());

        getResourceService().deleteResource(SECURITY_TOKEN, resourceId);

        aclEntries.remove(new AclEntry(userId, resourceId, ObjectRole.OWNER));
        Assert.assertEquals(0, aclEntries.size());
    }

    @Test
    public void testReservationRequest() throws Exception
    {
        String userId = getUserId(SECURITY_TOKEN);
        Set<AclEntry> aclEntries = new HashSet<AclEntry>();

        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability("{hash}",
                AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        aclEntries.add(new AclEntry(userId, resourceId, ObjectRole.OWNER));
        Assert.assertEquals(aclEntries, getAclEntries());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String reservationRequestId = allocate(reservationRequest);
        Reservation reservation = checkAllocated(reservationRequestId);

        aclEntries.add(new AclEntry(userId, reservationRequestId, ObjectRole.OWNER));
        aclEntries.add(new AclEntry(userId, reservation.getId(), ObjectRole.OWNER));
        Assert.assertEquals(aclEntries, getAclEntries());

        deleteAclEntry(userId, reservationRequestId, ObjectRole.OWNER);

        aclEntries.clear();
        aclEntries.add(new AclEntry(userId, resourceId, ObjectRole.OWNER));
        Assert.assertEquals(aclEntries, getAclEntries());

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

        aclEntries.add(new AclEntry(userId, reservationRequestId, ObjectRole.OWNER));
        aclEntries.add(new AclEntry(userId, reservation.getId(), ObjectRole.OWNER));
        aclEntries.add(new AclEntry(userId, aliasReservation1.getId(), ObjectRole.OWNER));
        aclEntries.add(new AclEntry(userId, aliasReservation2.getId(), ObjectRole.OWNER));
        aclEntries.add(new AclEntry(userId, valueReservation1, ObjectRole.OWNER));
        aclEntries.add(new AclEntry(userId, valueReservation2, ObjectRole.OWNER));
        Assert.assertEquals(aclEntries, getAclEntries());

        deleteAclEntry(userId, reservationRequestId, ObjectRole.OWNER);

        aclEntries.clear();
        aclEntries.add(new AclEntry(userId, resourceId, ObjectRole.OWNER));
        Assert.assertEquals(aclEntries, getAclEntries());
    }

    @Test
    public void testMultipleReservationRequest() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(
                new RoomProviderCapability(10, new AliasType[]{AliasType.ROOM_NAME}));
        mcu.addCapability(
                new AliasProviderCapability("{hash}", AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2013-01-01T12:00", "PT2H");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new RoomSpecification(5, Technology.H323));
        String reservationRequest1Id = allocate(reservationRequest1);
        checkAllocated(reservationRequest1Id);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2013-01-01T12:00", "PT2H");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new RoomSpecification(5, Technology.H323));
        String reservationRequest2Id = allocate(reservationRequest2);
        checkAllocated(reservationRequest2Id);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequest1Id);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequest2Id);

        Authorization.getInstance().clearCache();
        runScheduler();
    }

    @Test
    public void testReusedReservationRequest() throws Exception
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
        aliasReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String aliasReservationRequestId = allocate(SECURITY_TOKEN_USER1, aliasReservationRequest);
        Reservation aliasReservation = checkAllocated(aliasReservationRequestId);

        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2013-01-01T12:00", "PT2H");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        reservationRequest1.setReusedReservationRequestId(aliasReservationRequestId);
        String reservationRequest1Id = allocate(SECURITY_TOKEN_USER1, reservationRequest1);
        ExistingReservation reservation1 = (ExistingReservation) checkAllocated(reservationRequest1Id);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2013-01-01T14:00", "PT2H");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        reservationRequest2.setReusedReservationRequestId(aliasReservationRequestId);
        String reservationRequest2Id = allocate(SECURITY_TOKEN_USER1, reservationRequest2);
        ExistingReservation reservation2 = (ExistingReservation) checkAllocated(reservationRequest2Id);

        Assert.assertEquals(aliasReservation.getId(), reservation1.getReservation().getId());
        Assert.assertEquals(aliasReservation.getId(), reservation2.getReservation().getId());

        getAuthorizationService().createAclEntry(SECURITY_TOKEN_USER1, user2Id, reservationRequest1Id, ObjectRole.OWNER);
        getAuthorizationService().createAclEntry(SECURITY_TOKEN_USER1, user2Id, reservationRequest2Id, ObjectRole.OWNER);

        Assert.assertNotNull("Reader role should be created",
                getAclEntry(user2Id, aliasReservation.getId(), ObjectRole.READER));

        deleteAclEntry(user2Id, reservationRequest1Id, ObjectRole.OWNER);

        Assert.assertNotNull("Reader role should be kept for the second request",
                getAclEntry(user2Id, aliasReservation.getId(), ObjectRole.READER));

        deleteAclEntry(user2Id, reservationRequest2Id, ObjectRole.OWNER);

        Assert.assertNull("Reader role should be deleted",
                getAclEntry(user2Id, aliasReservation.getId(), ObjectRole.READER));

        getReservationService().deleteReservationRequest(SECURITY_TOKEN_USER1, reservationRequest1Id);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN_USER1, reservationRequest2Id);
    }

    /**
     * @return collection of all {@link cz.cesnet.shongo.controller.api.AclEntry} for user with {@link #SECURITY_TOKEN}
     * @throws Exception
     */
    private Set<AclEntry> getAclEntries() throws Exception
    {
        ListResponse<AclEntry> aclEntries = getAuthorizationService().listAclEntries(
                new AclEntryListRequest(SECURITY_TOKEN, getUserId(SECURITY_TOKEN)));
        return new HashSet<AclEntry>(aclEntries.getItems());
    }
}
