package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.request.CompartmentSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSetSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import org.joda.time.Interval;
import org.junit.Test;

/**
 * Tests for {@link cz.cesnet.shongo.controller.scheduler.ReservationTask}
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
        deviceResource.setAllocatable(true);
        deviceResource.addTechnology(Technology.H323);
        deviceResource.addCapability(new RoomProviderCapability(100));
        deviceResource.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164));
        cache.addResource(deviceResource);

        ReservationTask.Context context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));

        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));
        compartmentSpecification.addChildSpecification(new ExternalEndpointSpecification(Technology.H323));
        CompartmentReservationTask reservationTask = compartmentSpecification.createReservationTask(context);
        reservationTask.createReservation();

        for (Report report : reservationTask.getReports()) {
            System.out.println(report.toString());
        }
    }
}
