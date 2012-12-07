package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
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
        String resourceIdentifier;

        // Create resource
        resource = new Resource();
        resource.setName("resource");
        resourceIdentifier = getResourceService().createResource(SECURITY_TOKEN, resource);

        // Check created resource
        resource = getResourceService().getResource(SECURITY_TOKEN, resourceIdentifier);
        assertEquals("resource", resource.getName());
        assertEquals(Boolean.FALSE, resource.getAllocatable());

        // Modify resource by retrieved instance of Resource
        resource.setName("resourceModified");
        getResourceService().modifyResource(SECURITY_TOKEN, resource);

        // Modify resource by new instance of Resource
        resource = new Resource();
        resource.setIdentifier(resourceIdentifier);
        resource.setAllocatable(true);
        getResourceService().modifyResource(SECURITY_TOKEN, resource);

        // Check modified resource
        resource = getResourceService().getResource(SECURITY_TOKEN, resourceIdentifier);
        assertEquals("resourceModified", resource.getName());
        assertEquals(Boolean.TRUE, resource.getAllocatable());

        // Delete resource
        getResourceService().deleteResource(SECURITY_TOKEN, resourceIdentifier);

        // Check deleted resource
        try {
            getResourceService().getResource(SECURITY_TOKEN, resourceIdentifier);
            fail("Resource should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(Resource.class, exception.getEntityType());
            assertEquals(Domain.getLocalIdentifier(resourceIdentifier), exception.getEntityIdentifier());
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
        String deviceResourceIdentifier;

        // Create device resource
        deviceResource = new DeviceResource();
        deviceResource.setName("deviceResource");
        deviceResource.addTechnology(Technology.H323);
        deviceResourceIdentifier = getResourceService().createResource(SECURITY_TOKEN, deviceResource);

        // Check created device resource
        deviceResource = (DeviceResource) getResourceService().getResource(SECURITY_TOKEN, deviceResourceIdentifier);
        assertEquals(new HashSet<Technology>()
        {{
                add(Technology.H323);
            }}, deviceResource.getTechnologies());

        // Modify device resource
        deviceResource.addTechnology(Technology.SIP);
        getResourceService().modifyResource(SECURITY_TOKEN, deviceResource);

        // Check modified device resource
        deviceResource = (DeviceResource) getResourceService().getResource(SECURITY_TOKEN, deviceResourceIdentifier);
        assertEquals(new HashSet<Technology>()
        {{
                add(Technology.H323);
                add(Technology.SIP);
            }}, deviceResource.getTechnologies());

        // Delete resource
        getResourceService().deleteResource(SECURITY_TOKEN, deviceResourceIdentifier);

        // Check deleted resource
        try {
            getResourceService().getResource(SECURITY_TOKEN, deviceResourceIdentifier);
            fail("Device resource should not exist.");
        }
        catch (EntityNotFoundException exception) {
            assertEquals(Resource.class, exception.getEntityType());
            assertEquals(Domain.getLocalIdentifier(deviceResourceIdentifier), exception.getEntityIdentifier());
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
        mcu.addCapability(new AliasProviderCapability(AliasType.H323_E164, "95008721[d]", true));
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create alias provider
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability(AliasType.H323_E164, "95008722[d]", true));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);
    }
}
