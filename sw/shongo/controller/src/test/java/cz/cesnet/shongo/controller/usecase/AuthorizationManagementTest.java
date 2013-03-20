package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

/**
 * Tests for creating, updating and deleting {@link cz.cesnet.shongo.controller.api.Resource}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationManagementTest extends AbstractControllerTest
{
    @Test
    public void testResource() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability("{hash}",
                AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        Collection<AclRecord> aclRecords = getAclRecords();
        Assert.assertEquals(1, aclRecords.size());

        getResourceService().deleteResource(SECURITY_TOKEN, resourceId);

        aclRecords = getAclRecords();
        Assert.assertEquals(0, aclRecords.size());
    }

    @Test
    public void testReservationRequest() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.addCapability(new AliasProviderCapability("{hash}",
                AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        Collection<AclRecord> aclRecords = getAclRecords();
        Assert.assertEquals(1, aclRecords.size());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        allocateAndCheck(reservationRequest);

        aclRecords = getAclRecords();
        Assert.assertEquals(1 + 2, aclRecords.size());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-02T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("test1"));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue("test2"));
        reservationRequest.setSpecification(aliasSetSpecification);
        allocateAndCheck(reservationRequest);

        aclRecords = getAclRecords();
        Assert.assertEquals(1 + 2 + 6, aclRecords.size());
    }

    private Collection<AclRecord> getAclRecords() throws Exception
    {
        Collection<AclRecord> aclRecords = getAuthorizationService().listAclRecords(SECURITY_TOKEN,
                DummyAuthorization.TESTING_USER_ID, null, null);
        for (AclRecord aclRecord : aclRecords) {
            System.out.println(aclRecord.toString());
        }
        return aclRecords;
    }
}
