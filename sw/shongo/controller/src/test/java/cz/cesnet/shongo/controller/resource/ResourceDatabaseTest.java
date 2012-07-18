package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.ResourceDatabase;
import cz.cesnet.shongo.api.Technology;
import org.junit.Test;

import javax.persistence.EntityManager;
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
        EntityManager entityManager = getEntityManager();

        // Fill resource database
        ResourceDatabase resourceDatabase = new ResourceDatabase();
        resourceDatabase.setEntityManagerFactory(getEntityManagerFactory());
        resourceDatabase.init();

        DeviceResource terminal = new DeviceResource();
        terminal.setTechnology(Technology.H323);
        terminal.setDescription("Software Mirial endpoint of Martin Srom");
        terminal.addCapability(new StandaloneTerminalCapability());
        resourceDatabase.addResource(terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setTechnology(Technology.H323);
        mcu.addCapability(new VirtualRoomsCapability());
        resourceDatabase.addResource(mcu);

        resourceDatabase.destroy();

        // Load stored resource database
        resourceDatabase = new ResourceDatabase();
        resourceDatabase.setEntityManagerFactory(getEntityManagerFactory());
        resourceDatabase.init();

        // List resources
        List<Resource> resourceList = resourceDatabase.listResources();
        for (Resource resource : resourceList) {
            System.err.println(resource.toString());
        }
        System.err.println(resourceDatabase.getDeviceTopology().toString());
    }
}
