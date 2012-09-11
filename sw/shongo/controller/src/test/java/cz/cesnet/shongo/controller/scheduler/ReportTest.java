package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.allocation.AllocatedExternalEndpoint;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableVirtualRoomReport;
import cz.cesnet.shongo.controller.scheduler.report.NotEnoughEndpointInCompartmentReport;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hsqldb.util.DatabaseManagerSwing;
import org.joda.time.Interval;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link cz.cesnet.shongo.controller.scheduler.Task}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReportTest
{
    @Test
    public void test() throws Exception
    {
        Cache cache = Cache.createTestingCache();

        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setAddress(Address.LOCALHOST);
        deviceResource.setAllocatable(true);
        deviceResource.addTechnology(Technology.H323);
        deviceResource.addCapability(new VirtualRoomsCapability(100));
        cache.addResource(deviceResource);

        Task task = new Task(Interval.parse("2012/2013"), cache);

        task.clear();
        task.addAllocatedItem(new AllocatedExternalEndpoint(new ExternalEndpointSpecification(Technology.H323, 2)));
        task.addAllocatedItem(new AllocatedExternalEndpoint(new ExternalEndpointSpecification(Technology.H323, 1)));
        task.createAllocatedCompartment();

        for (Report report : task.getReports()) {
            System.out.println(report.toString());
        }
    }
}
