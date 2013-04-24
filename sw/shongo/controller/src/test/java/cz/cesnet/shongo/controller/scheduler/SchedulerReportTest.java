package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.report.Report;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link cz.cesnet.shongo.controller.scheduler.ReservationTask}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerReportTest
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

        RoomSpecification roomSpecification1 = new RoomSpecification();
        roomSpecification1.addTechnology(Technology.H323);
        roomSpecification1.setParticipantCount(5);
        print(cache, roomSpecification1);

        DeviceResource deviceResource1 = new DeviceResource();
        deviceResource1.setAllocatable(true);
        deviceResource1.addTechnology(Technology.H323);
        deviceResource1.addCapability(new RoomProviderCapability(3, AliasType.H323_E164));
        deviceResource1.addCapability(new AliasProviderCapability("1", AliasType.H323_E164, true));
        cache.addResource(deviceResource1);

        DeviceResource deviceResource2 = new DeviceResource();
        deviceResource2.setAllocatable(true);
        deviceResource2.addTechnology(Technology.H323);
        deviceResource2.addCapability(new RoomProviderCapability(100, AliasType.H323_E164));
        cache.addResource(deviceResource2);

        DeviceResource deviceResource3 = new DeviceResource();
        deviceResource3.setAllocatable(true);
        deviceResource3.addTechnology(Technology.H323);
        deviceResource3.addCapability(new RoomProviderCapability(100, AliasType.H323_E164));
        deviceResource3.addCapability(new AliasProviderCapability("2", AliasType.H323_E164, true));
        cache.addResource(deviceResource3);

        AliasSpecification aliasSpecification1 = new AliasSpecification();
        aliasSpecification1.addAliasType(AliasType.H323_E164);
        aliasSpecification1.setValue("2");
        AliasSpecification aliasSpecification2 = new AliasSpecification();
        aliasSpecification2.addAliasType(AliasType.H323_E164);
        aliasSpecification2.setValue("2");
        AliasSpecification aliasSpecification3 = new AliasSpecification();
        aliasSpecification3.addAliasType(AliasType.H323_E164);
        aliasSpecification3.setAliasProviderCapability(deviceResource3.getCapability(AliasProviderCapability.class));
        printProvided(cache, aliasSpecification1, aliasSpecification2);
        print(cache, aliasSpecification1, aliasSpecification2, aliasSpecification3);

        RoomSpecification roomSpecification2 = new RoomSpecification();
        roomSpecification2.addTechnology(Technology.H323);
        roomSpecification2.setParticipantCount(5);
        print(cache, roomSpecification2);

        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.setCallInitiation(CallInitiation.VIRTUAL_ROOM);
        compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));
        compartmentSpecification.addChildSpecification(new ExternalEndpointSpecification(Technology.H323));
        print(cache, compartmentSpecification);
    }

    private void print(Cache cache, ReservationTaskProvider... reservationTaskProviders) throws SchedulerException
    {
        ReservationTask.Context context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));
        for (ReservationTaskProvider reservationTaskProvider : reservationTaskProviders) {
            ReservationTask reservationTask = reservationTaskProvider.createReservationTask(context);
            print(reservationTask);
        }
    }

    private void printProvided(Cache cache, ReservationTaskProvider reservationTaskProvider1,
            ReservationTaskProvider reservationTaskProvider2) throws SchedulerException
    {
        ReservationTask.Context context = new ReservationTask.Context(cache, Interval.parse("2012/2013"));

        ReservationTask reservationTask = reservationTaskProvider1.createReservationTask(context);
        Reservation reservation = print(reservationTask);
        reservation.generateTestingId();
        context.getCacheTransaction().addProvidedReservation(reservation);

        reservationTask = reservationTaskProvider2.createReservationTask(context);
        print(reservationTask);
    }

    private Reservation print(ReservationTask reservationTask) throws SchedulerException
    {
        try {
            Reservation reservation = reservationTask.perform();
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append(reservationTask.getClass().getSimpleName() + " reports:\n");
            builder.append("\n");
            for (SchedulerReport report : reservationTask.getReports()) {
                builder.append(report.getMessageRecursive(Report.MessageType.DOMAIN_ADMIN));
                builder.append("\n");
            }
            builder.append("\n");
            System.out.print(builder.toString());
            System.out.flush();
            return reservation;
        }
        catch (SchedulerException exception) {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append(reservationTask.getClass().getSimpleName() + " error report:\n");
            builder.append("\n");
            builder.append(exception.getTopReport().getMessageRecursive(Report.MessageType.DOMAIN_ADMIN));
            builder.append("\n");
            System.err.print(builder.toString());
            System.err.flush();
            return null;
        }

    }
}
