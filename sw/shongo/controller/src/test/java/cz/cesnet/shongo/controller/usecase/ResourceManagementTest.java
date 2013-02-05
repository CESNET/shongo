package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.FilterType;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.junit.Test;

import java.util.HashSet;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

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
        assertEquals("resource", resource.getName());
        assertEquals(Boolean.FALSE, resource.getAllocatable());

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
        assertEquals("resourceModified", resource.getName());
        assertEquals(Boolean.TRUE, resource.getAllocatable());

        // Delete resource
        getResourceService().deleteResource(SECURITY_TOKEN, resourceId);

        // Check deleted resource
        try {
            getResourceService().getResource(SECURITY_TOKEN, resourceId);
            fail("Resource should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(Resource.class, exception.getEntityType());
            assertEquals(resourceId, exception.getEntityId());
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
        assertEquals(new HashSet<Technology>()
        {{
                add(Technology.H323);
            }}, deviceResource.getTechnologies());

        // Modify device resource
        deviceResource.addTechnology(Technology.SIP);
        getResourceService().modifyResource(SECURITY_TOKEN, deviceResource);

        // Check modified device resource
        deviceResource = (DeviceResource) getResourceService().getResource(SECURITY_TOKEN, deviceResourceId);
        assertEquals(new HashSet<Technology>()
        {{
                add(Technology.H323);
                add(Technology.SIP);
            }}, deviceResource.getTechnologies());

        // Delete resource
        getResourceService().deleteResource(SECURITY_TOKEN, deviceResourceId);

        // Check deleted resource
        try {
            getResourceService().getResource(SECURITY_TOKEN, deviceResourceId);
            fail("Device resource should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(Resource.class, exception.getEntityType());
            assertEquals(deviceResourceId, exception.getEntityId());
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
}
