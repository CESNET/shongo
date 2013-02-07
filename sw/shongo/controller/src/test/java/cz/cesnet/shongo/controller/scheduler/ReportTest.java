package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.CompartmentSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSetSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.request.RoomSpecification;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link cz.cesnet.shongo.controller.scheduler.ReservationTask}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReportTest
{
    @Before
    public void before() throws Exception
    {
        Domain.setLocalDomain(new Domain("test"));
    }

    @After
    public void after() throws Exception
    {
        Domain.setLocalDomain(null);
    }

    @Test
    public void test() throws Exception
    {
        Cache cache = Cache.createTestingCache();

        /*DeviceResource deviceResource1 = new DeviceResource();
        deviceResource1.setAllocatable(true);
        deviceResource1.addTechnology(Technology.H323);
        deviceResource1.addCapability(new RoomProviderCapability(3, AliasType.H323_E164));
        deviceResource1.addCapability(new AliasProviderCapability("{digit:1}", AliasType.H323_E164, true));
        cache.addResource(deviceResource1);

        DeviceResource deviceResource2 = new DeviceResource();
        deviceResource2.setAllocatable(true);
        deviceResource2.addTechnology(Technology.H323);
        deviceResource2.addCapability(new RoomProviderCapability(100, AliasType.H323_E164));
        cache.addResource(deviceResource2);*/

        DeviceResource deviceResource3 = new DeviceResource();
        deviceResource3.setAllocatable(true);
        deviceResource3.addTechnology(Technology.H323);
        deviceResource3.addCapability(new RoomProviderCapability(100, AliasType.H323_E164));
        deviceResource3.addCapability(new AliasProviderCapability("{digit:1}", AliasType.H323_E164, true));
        cache.addResource(deviceResource3);

        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        print(roomSpecification, cache);

        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));
        compartmentSpecification.addChildSpecification(new ExternalEndpointSpecification(Technology.H323));
        print(compartmentSpecification, cache);
    }

    private void print(ReservationTaskProvider reservationTaskProvider, Cache cache) throws ReportException
    {
        try {
            ReservationTask.Context context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
            ReservationTask reservationTask = reservationTaskProvider.createReservationTask(context);
            reservationTask.perform();
            System.out.println();
            System.out.println(reservationTask.getClass().getSimpleName() + " reports:");
            System.out.println();
            for (Report report : reservationTask.getReports()) {
                System.out.println(report.toString());
            }
            System.out.println();
        }
        catch (ReportException exception) {
            exception.printStackTrace();
            Assert.fail("Exception should not be thrown");
        }
    }
}
