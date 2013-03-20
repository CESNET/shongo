package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.HashSet;

/**
 * Tests for creating, updating and deleting {@link Resource}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceManagementTest extends AbstractControllerTest
{
    /**
     * Test basic resource.
     *
     * @throws Exception
     */
    @Test
    public void testResource() throws Exception
    {
        Resource resource;
        String resourceId;

        // Create resource
        resource = new Resource();
        resource.setName("resource");
        resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        // Check created resource
        resource = getResourceService().getResource(SECURITY_TOKEN, resourceId);
        Assert.assertEquals("resource", resource.getName());
        Assert.assertEquals(Boolean.FALSE, resource.getAllocatable());

        // Modify resource by retrieved instance of Resource
        resource.setName("resourceModified");
        getResourceService().modifyResource(SECURITY_TOKEN, resource);

        // Modify resource by new instance of Resource
        resource = new Resource();
        resource.setId(resourceId);
        resource.setAllocatable(true);
        getResourceService().modifyResource(SECURITY_TOKEN, resource);

        // Check modified resource
        resource = getResourceService().getResource(SECURITY_TOKEN, resourceId);
        Assert.assertEquals("resourceModified", resource.getName());
        Assert.assertEquals(Boolean.TRUE, resource.getAllocatable());

        // Delete resource
        getResourceService().deleteResource(SECURITY_TOKEN, resourceId);

        // Check deleted resource
        try {
            getResourceService().getResource(SECURITY_TOKEN, resourceId);
            Assert.fail("Resource should not exist.");
        }
        catch (FaultException exception) {
            ControllerFaultSet.EntityNotFoundFault entityNotFoundFault =
                    exception.getFault(ControllerFaultSet.EntityNotFoundFault.class);
            Assert.assertEquals(resourceId, entityNotFoundFault.getId());
        }
    }

    /**
     * Test device resource.
     *
     * @throws Exception
     */
    @Test
    public void testDeviceResource() throws Exception
    {
        DeviceResource deviceResource;
        String deviceResourceId;

        // Create device resource
        deviceResource = new DeviceResource();
        deviceResource.setName("deviceResource");
        deviceResource.addTechnology(Technology.H323);
        deviceResourceId = getResourceService().createResource(SECURITY_TOKEN, deviceResource);

        // Check created device resource
        deviceResource = (DeviceResource) getResourceService().getResource(SECURITY_TOKEN, deviceResourceId);
        Assert.assertEquals(new HashSet<Technology>()
        {{
                add(Technology.H323);
            }}, deviceResource.getTechnologies());

        // Modify device resource
        deviceResource.addTechnology(Technology.SIP);
        getResourceService().modifyResource(SECURITY_TOKEN, deviceResource);

        // Check modified device resource
        deviceResource = (DeviceResource) getResourceService().getResource(SECURITY_TOKEN, deviceResourceId);
        Assert.assertEquals(new HashSet<Technology>()
        {{
                add(Technology.H323);
                add(Technology.SIP);
            }}, deviceResource.getTechnologies());

        // Delete resource
        getResourceService().deleteResource(SECURITY_TOKEN, deviceResourceId);

        // Check deleted resource
        try {
            getResourceService().getResource(SECURITY_TOKEN, deviceResourceId);
            Assert.fail("Device resource should not exist.");
        }
        catch (FaultException exception) {
            ControllerFaultSet.EntityNotFoundFault entityNotFoundFault =
                    exception.getFault(ControllerFaultSet.EntityNotFoundFault.class);
            Assert.assertEquals(deviceResourceId, entityNotFoundFault.getId());
        }
    }

    /**
     * Test creating of specific resources.
     *
     * @throws Exception
     */
    @Test
    public void testSpecificResourceCreation() throws Exception
    {
        // Create terminal
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.setAllocatable(true);
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new StandaloneTerminalCapability());
        getResourceService().createResource(SECURITY_TOKEN, terminal);

        // Create MCU
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(100));
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create alias provider
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);
    }

    /**
     * Test deletion of resources with value providers.
     *
     * @throws Exception
     */
    @Test
    public void testValueProviderDeletion() throws Exception
    {
        Resource firstResource = new Resource();
        firstResource.setName("resource");
        firstResource.addCapability(new ValueProviderCapability("test"));
        String firstResourceId = getResourceService().createResource(SECURITY_TOKEN, firstResource);

        Resource secondResource = new Resource();
        secondResource.setName("resource");
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, firstResourceId));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        secondResource.addCapability(aliasProviderCapability);
        String secondResourceId = getResourceService().createResource(SECURITY_TOKEN, secondResource);

        Resource thirdResource = new Resource();
        thirdResource.setName("resource");
        thirdResource.addCapability(new AliasProviderCapability("test", AliasType.H323_E164));
        String thirdResourceId = getResourceService().createResource(SECURITY_TOKEN, thirdResource);

        getResourceService().deleteResource(SECURITY_TOKEN, secondResourceId);
        getResourceService().deleteResource(SECURITY_TOKEN, firstResourceId);
        getResourceService().deleteResource(SECURITY_TOKEN, thirdResourceId);
    }

    /**
     * Test resource maximum future.
     *
     * @throws Exception
     */
    @Test
    public void testMaximumFuture() throws Exception
    {
        // Create resource with relative maximum future P1M
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.setMaximumFuture("P1M");
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        // Create reservation request before P1M -> success
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setDescription("request");
        reservationRequest.setSlot("2012-01-01T12:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String reservationRequestId = getReservationService().createReservationRequest(SECURITY_TOKEN,
                reservationRequest);
        runScheduler(Interval.parse("2012-01-01/2012-12-01"));
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                reservationRequestId);
        Assert.assertEquals(ReservationRequestState.ALLOCATED, reservationRequest.getState());

        // Create reservation request after P1M -> failure
        reservationRequest.setSlot("2012-02-01T12:00", "PT2H");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler(Interval.parse("2012-01-01/2012-12-01"));
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                reservationRequestId);
        Assert.assertEquals(ReservationRequestState.ALLOCATION_FAILED, reservationRequest.getState());

        // Modify resource absolute maximum future to 2012-03-01
        resource = getResourceService().getResource(SECURITY_TOKEN, resourceId);
        resource.setMaximumFuture("2012-03-01T00:00");
        getResourceService().modifyResource(SECURITY_TOKEN, resource);

        // Create reservation request before maximum future -> success
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler(Interval.parse("2012-01-01/2012-12-01"));
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                reservationRequestId);
        Assert.assertEquals(ReservationRequestState.ALLOCATED, reservationRequest.getState());

        // Create reservation request after maximum future -> failure
        reservationRequest.setSlot("2012-03-01T12:00", "PT2H");
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler(Interval.parse("2012-01-01/2012-12-01"));
        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN,
                reservationRequestId);
        Assert.assertEquals(ReservationRequestState.ALLOCATION_FAILED, reservationRequest.getState());
    }

    /**
     * Test {@link ReservationRequestPurpose#OWNER} and {@link ReservationRequestPurpose#MAINTENANCE}.
     *
     * @throws Exception
     */
    @Test
    public void testOwner() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.setMaximumFuture("P1M");
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        String rootUserId = getUserId(SECURITY_TOKEN_ROOT);
        getAuthorizationService().setEntityUser(SECURITY_TOKEN_ROOT, resourceId, rootUserId);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-01-01T12:00", "P1Y");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.OWNER);
        firstReservationRequest.setSpecification(new ResourceSpecification(resourceId));
        final String firstReservationRequestId = allocate(firstReservationRequest);
        checkAllocationFailed(firstReservationRequestId);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2013-01-01T12:00", "P1Y");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        secondReservationRequest.setSpecification(new ResourceSpecification(resourceId));
        final String secondReservationRequestId = allocate(secondReservationRequest);
        checkAllocationFailed(secondReservationRequestId);

        String userId = getUserId(SECURITY_TOKEN);
        getAuthorizationService().setEntityUser(SECURITY_TOKEN_ROOT, resourceId, userId);

        reallocate(firstReservationRequestId);
        checkAllocated(firstReservationRequestId);

        reallocate(secondReservationRequestId);
        checkAllocated(secondReservationRequestId);
    }
}
