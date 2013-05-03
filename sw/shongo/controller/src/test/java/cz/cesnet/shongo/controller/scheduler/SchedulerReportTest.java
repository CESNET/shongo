package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
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

import javax.persistence.EntityManager;

/**
 * Tests for {@link cz.cesnet.shongo.controller.scheduler.ReservationTask}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerReportTest extends AbstractDatabaseTest
{
    @Before
    public void before() throws Exception
    {
        super.before();

        Domain.setLocalDomain(new Domain("test"));
    }

    @After
    public void after() throws Exception
    {
        super.after();

        Domain.setLocalDomain(null);
    }

    @Test
    public void test() throws Exception
    {
        EntityManager entityManager = getEntityManager();
        try {
            Cache cache = new Cache();
            cache.init();

            RoomSpecification roomSpecification1 = new RoomSpecification();
            roomSpecification1.addTechnology(Technology.H323);
            roomSpecification1.setParticipantCount(5);
            print(Report.MessageType.DOMAIN_ADMIN, cache, entityManager, roomSpecification1);

            DeviceResource deviceResource1 = new DeviceResource();
            deviceResource1.setUserId("0");
            deviceResource1.setAllocatable(true);
            deviceResource1.addTechnology(Technology.H323);
            deviceResource1.addCapability(new RoomProviderCapability(3, AliasType.H323_E164));
            deviceResource1.addCapability(new AliasProviderCapability("1", AliasType.H323_E164, true));
            cache.addResource(deviceResource1, entityManager);

            DeviceResource deviceResource2 = new DeviceResource();
            deviceResource2.setUserId("0");
            deviceResource2.setAllocatable(true);
            deviceResource2.addTechnology(Technology.H323);
            deviceResource2.addCapability(new RoomProviderCapability(100, AliasType.H323_E164));
            cache.addResource(deviceResource2, entityManager);

            DeviceResource deviceResource3 = new DeviceResource();
            deviceResource3.setUserId("0");
            deviceResource3.setAllocatable(true);
            deviceResource3.addTechnology(Technology.H323);
            deviceResource3.addCapability(new RoomProviderCapability(100, AliasType.H323_E164));
            deviceResource3.addCapability(new AliasProviderCapability("2", AliasType.H323_E164, true));
            cache.addResource(deviceResource3, entityManager);

            AliasSpecification aliasSpecification1 = new AliasSpecification();
            aliasSpecification1.addAliasType(AliasType.H323_E164);
            aliasSpecification1.setValue("2");
            AliasSpecification aliasSpecification2 = new AliasSpecification();
            aliasSpecification2.addAliasType(AliasType.H323_E164);
            aliasSpecification2.setValue("2");
            AliasSpecification aliasSpecification3 = new AliasSpecification();
            aliasSpecification3.addAliasType(AliasType.H323_E164);
            aliasSpecification3
                    .setAliasProviderCapability(deviceResource3.getCapability(AliasProviderCapability.class));
            printProvided(Report.MessageType.DOMAIN_ADMIN, cache, entityManager, aliasSpecification1,
                    aliasSpecification2);
            print(Report.MessageType.DOMAIN_ADMIN, cache, entityManager, aliasSpecification1, aliasSpecification2,
                    aliasSpecification3);

            RoomSpecification roomSpecification2 = new RoomSpecification();
            roomSpecification2.addTechnology(Technology.H323);
            roomSpecification2.setParticipantCount(5);
            print(Report.MessageType.DOMAIN_ADMIN, cache, entityManager, roomSpecification2);

            CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
            compartmentSpecification.setCallInitiation(CallInitiation.VIRTUAL_ROOM);
            compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));
            compartmentSpecification.addChildSpecification(new ExternalEndpointSpecification(Technology.H323));
            print(Report.MessageType.USER, cache, entityManager, compartmentSpecification);
        }
        finally {
            entityManager.close();
        }
    }

    @Test
    public void testRoomWithAliases() throws Exception
    {
        EntityManager entityManager = getEntityManager();
        try {
            Cache cache = new Cache();
            cache.init();

            DeviceResource deviceResource1 = new DeviceResource();
            deviceResource1.setUserId("0");
            deviceResource1.setAllocatable(true);
            deviceResource1.addTechnology(Technology.H323);
            deviceResource1.addCapability(
                    new RoomProviderCapability(5, new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_E164}));
            deviceResource1.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME, true));
            deviceResource1.addCapability(new AliasProviderCapability("1", AliasType.H323_E164, true));
            cache.addResource(deviceResource1, entityManager);

            RoomSpecification roomSpecification1 = new RoomSpecification();
            roomSpecification1.addTechnology(Technology.H323);
            roomSpecification1.setParticipantCount(5);
            print(Report.MessageType.USER, cache, entityManager, roomSpecification1);
        }
        finally {
            entityManager.close();
        }
    }

    private void print(Report.MessageType messageType, Cache cache, EntityManager entityManager,
            ReservationTaskProvider... reservationTaskProviders)
            throws SchedulerException
    {
        ReservationTask.Context context = new ReservationTask.Context(
                Interval.parse("2012/2013"), cache, entityManager);
        for (ReservationTaskProvider reservationTaskProvider : reservationTaskProviders) {
            ReservationTask reservationTask = reservationTaskProvider.createReservationTask(context);
            print(messageType, reservationTask);
        }
    }

    private void printProvided(Report.MessageType messageType, Cache cache, EntityManager entityManager,
            ReservationTaskProvider reservationTaskProvider1,
            ReservationTaskProvider reservationTaskProvider2) throws SchedulerException
    {
        ReservationTask.Context context = new ReservationTask.Context(
                Interval.parse("2012/2013"), cache, entityManager);

        ReservationTask reservationTask = reservationTaskProvider1.createReservationTask(context);
        Reservation reservation = print(messageType, reservationTask);
        reservation.generateTestingId();
        context.getCacheTransaction().addProvidedReservation(reservation);

        reservationTask = reservationTaskProvider2.createReservationTask(context);
        print(messageType, reservationTask);
    }

    private Reservation print(Report.MessageType messageType, ReservationTask reservationTask) throws SchedulerException
    {
        try {
            Reservation reservation = reservationTask.perform();
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append(reservationTask.getClass().getSimpleName() + " reports:\n");
            builder.append("\n");
            for (SchedulerReport report : reservationTask.getReports()) {
                builder.append(report.getMessageRecursive(messageType));
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
            builder.append(exception.getTopReport().getMessageRecursive(messageType));
            builder.append("\n");
            System.err.print(builder.toString());
            System.err.flush();
            return null;
        }

    }
}
