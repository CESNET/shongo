package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.FilterType;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.AliasProviderCapability;
import cz.cesnet.shongo.controller.api.DeviceResource;
import cz.cesnet.shongo.controller.api.ResourceSpecification;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import cz.cesnet.shongo.controller.api.StandaloneTerminalCapability;
import cz.cesnet.shongo.controller.api.ValueProviderCapability;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;

/**
 * Tests for creating, updating and deleting {@link cz.cesnet.shongo.controller.api.Resource}s.
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
        cz.cesnet.shongo.controller.api.Resource resource;
        String resourceId;

        // Create resource
        resource = new cz.cesnet.shongo.controller.api.Resource();
        resource.setName("resource");
        resourceId = createResource(SECURITY_TOKEN, resource);

        // Check created resource
        resource = getResource(resourceId, Resource.class);
        Assert.assertEquals("resource", resource.getName());
        Assert.assertEquals(Boolean.FALSE, resource.getAllocatable());

        // Modify resource by retrieved instance of Resource
        resource.setName("resourceModified");
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
        catch (CommonReportSet.ObjectNotExistsException exception) {
            Assert.assertEquals(resourceId, exception.getObjectId());
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
        cz.cesnet.shongo.controller.api.DeviceResource deviceResource;
        String deviceResourceId;

        // Create device resource
        deviceResource = new cz.cesnet.shongo.controller.api.DeviceResource();
        deviceResource.setName("deviceResource");
        deviceResource.addTechnology(Technology.H323);
        deviceResourceId = createResource(SECURITY_TOKEN, deviceResource);

        // Check created device resource
        deviceResource = getResource(deviceResourceId, cz.cesnet.shongo.controller.api.DeviceResource.class);
        Assert.assertEquals(new HashSet<Technology>()
        {{
                add(Technology.H323);
            }}, deviceResource.getTechnologies());

        // Modify device resource
        deviceResource.addTechnology(Technology.SIP);
        getResourceService().modifyResource(SECURITY_TOKEN, deviceResource);

        // Check modified device resource
        deviceResource = (cz.cesnet.shongo.controller.api.DeviceResource) getResourceService().getResource(SECURITY_TOKEN, deviceResourceId);
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
        catch (CommonReportSet.ObjectNotExistsException exception) {
            Assert.assertEquals(deviceResourceId, exception.getObjectId());
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
        cz.cesnet.shongo.controller.api.DeviceResource terminal = new cz.cesnet.shongo.controller.api.DeviceResource();
        terminal.setName("terminal");
        terminal.setAllocatable(true);
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new StandaloneTerminalCapability());
        createResource(terminal);

        // Create MCU
        cz.cesnet.shongo.controller.api.DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(100));
        createResource(mcu);

        // Create alias provider
        cz.cesnet.shongo.controller.api.Resource aliasProvider = new cz.cesnet.shongo.controller.api.Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new cz.cesnet.shongo.controller.api.AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        createResource(aliasProvider);
    }

    /**
     * Test deletion of resources with value providers.
     *
     * @throws Exception
     */
    @Test
    public void testValueProviderDeletion() throws Exception
    {
        cz.cesnet.shongo.controller.api.Resource firstResource = new cz.cesnet.shongo.controller.api.Resource();
        firstResource.setName("resource");
        firstResource.addCapability(new ValueProviderCapability("test"));
        String firstResourceId = createResource(SECURITY_TOKEN, firstResource);

        cz.cesnet.shongo.controller.api.Resource secondResource = new cz.cesnet.shongo.controller.api.Resource();
        secondResource.setName("resource");
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderCapability = new cz.cesnet.shongo.controller.api.AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, firstResourceId));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        secondResource.addCapability(aliasProviderCapability);
        String secondResourceId = createResource(SECURITY_TOKEN, secondResource);

        cz.cesnet.shongo.controller.api.Resource thirdResource = new cz.cesnet.shongo.controller.api.Resource();
        thirdResource.setName("resource");
        thirdResource.addCapability(new AliasProviderCapability("test", AliasType.H323_E164));
        String thirdResourceId = createResource(SECURITY_TOKEN, thirdResource);

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
        cz.cesnet.shongo.controller.api.Resource resource = new cz.cesnet.shongo.controller.api.Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.setMaximumFuture("P1M");
        String resourceId = createResource(SECURITY_TOKEN, resource);

        // Create reservation request before P1M -> success
        ReservationRequest request = new ReservationRequest();
        request.setDescription("request");
        request.setSlot("2012-01-01T12:00", "PT2H");
        request.setPurpose(ReservationRequestPurpose.SCIENCE);
        request.setSpecification(new ResourceSpecification(resourceId));
        String requestId = getReservationService().createReservationRequest(SECURITY_TOKEN, request);
        runScheduler(Interval.parse("2012-01-01/2012-12-01"));
        request = getReservationRequest(requestId, ReservationRequest.class);
        Assert.assertEquals(AllocationState.ALLOCATED, request.getAllocationState());

        // Modify reservation request after P1M -> failure
        request.setSlot("2012-02-01T12:00", "PT2H");
        requestId = getReservationService().modifyReservationRequest(SECURITY_TOKEN, request);
        runScheduler(Interval.parse("2012-01-01/2012-12-01"));
        request = getReservationRequest(requestId, ReservationRequest.class);
        Assert.assertEquals(AllocationState.ALLOCATION_FAILED, request.getAllocationState());

        // Modify resource absolute maximum future to 2012-03-01
        resource = getResourceService().getResource(SECURITY_TOKEN, resourceId);
        resource.setMaximumFuture("2012-03-01T00:00");
        getResourceService().modifyResource(SECURITY_TOKEN, resource);

        // Create reservation request before maximum future -> success
        requestId = getReservationService().modifyReservationRequest(SECURITY_TOKEN, request);
        runScheduler(Interval.parse("2012-01-01/2012-12-01"));
        request = getReservationRequest(requestId, ReservationRequest.class);
        Assert.assertEquals(AllocationState.ALLOCATED, request.getAllocationState());

        // Modify reservation request after maximum future -> failure
        request.setSlot("2012-03-01T12:00", "PT2H");
        requestId = getReservationService().modifyReservationRequest(SECURITY_TOKEN, request);
        runScheduler(Interval.parse("2012-01-01/2012-12-01"));
        request = getReservationRequest(requestId, ReservationRequest.class);
        Assert.assertEquals(AllocationState.ALLOCATION_FAILED, request.getAllocationState());
    }

    /**
     * Test {@link ReservationRequestPurpose#OWNER} and {@link ReservationRequestPurpose#MAINTENANCE}.
     *
     * @throws Exception
     */
    @Test
    public void testOwner() throws Exception
    {
        cz.cesnet.shongo.controller.api.Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.setMaximumFuture("P1M");
        String resourceId = createResource(resource);

        String rootUserId = getUserId(SECURITY_TOKEN_ROOT);
        getAuthorizationService().setObjectUser(SECURITY_TOKEN_ROOT, resourceId, rootUserId);

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
        getAuthorizationService().setObjectUser(SECURITY_TOKEN_ROOT, resourceId, userId);

        reallocate(firstReservationRequestId);
        checkAllocated(firstReservationRequestId);

        reallocate(secondReservationRequestId);
        checkAllocated(secondReservationRequestId);
    }
}
