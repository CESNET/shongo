package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Domain;
import org.junit.Test;

import java.util.List;

/**
 * Resource database test.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceDatabaseTest extends AbstractDatabaseTest
{
    @Test
    public void test() throws Exception
    {
        // Fill resource database
        ResourceDatabase resourceDatabase = new ResourceDatabase(new Domain("cz.cesnet"), entityManager);

        DeviceResource terminal = new DeviceResource();
        terminal.createNewIdentifier("cz.cesnet");
        terminal.setTechnology(Technology.H323);
        terminal.setDescription("Software Mirial endpoint of Martin Srom");
        terminal.addCapability(new StandaloneTerminalCapability());
        resourceDatabase.addResource(terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.createNewIdentifier("cz.cesnet");
        mcu.setTechnology(Technology.H323);
        mcu.addCapability(new VirtualRoomsCapability());
        resourceDatabase.addResource(mcu);

        // Load stored resource database and list resources
        resourceDatabase = new ResourceDatabase(new Domain("cz.cesnet"), entityManager);
        List<Resource> resourceList = resourceDatabase.listResources();
        for ( Resource resource : resourceList ) {
            System.err.println(resource.toString());
        }
    }
}
