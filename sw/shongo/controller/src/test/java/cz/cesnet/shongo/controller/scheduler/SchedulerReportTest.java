package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.report.Report;
import org.joda.time.Interval;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * Tests for {@link cz.cesnet.shongo.controller.scheduler.ReservationTask}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerReportTest extends AbstractSchedulerTest
{
    @Test
    public void test() throws Exception
    {
        RoomSpecification roomSpecification1 = new RoomSpecification();
        roomSpecification1.addTechnology(Technology.H323);
        roomSpecification1.setParticipantCount(5);
        print(Report.MessageType.DOMAIN_ADMIN, roomSpecification1);

        DeviceResource deviceResource1 = new DeviceResource();
        deviceResource1.setAllocatable(true);
        deviceResource1.addTechnology(Technology.H323);
        deviceResource1.addCapability(new RoomProviderCapability(3, AliasType.H323_E164));
        deviceResource1.addCapability(new AliasProviderCapability("1", AliasType.H323_E164, true));
        createResource(deviceResource1);

        DeviceResource deviceResource2 = new DeviceResource();
        deviceResource2.setAllocatable(true);
        deviceResource2.addTechnology(Technology.H323);
        deviceResource2.addCapability(new RoomProviderCapability(100, AliasType.H323_E164));
        createResource(deviceResource2);

        DeviceResource deviceResource3 = new DeviceResource();
        deviceResource3.setAllocatable(true);
        deviceResource3.addTechnology(Technology.H323);
        deviceResource3.addCapability(new RoomProviderCapability(100, AliasType.H323_E164));
        deviceResource3.addCapability(new AliasProviderCapability("2", AliasType.H323_E164, true));
        createResource(deviceResource3);

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
        printProvided(Report.MessageType.DOMAIN_ADMIN, aliasSpecification1, aliasSpecification2);
        print(Report.MessageType.DOMAIN_ADMIN, aliasSpecification1, aliasSpecification2, aliasSpecification3);

        RoomSpecification roomSpecification2 = new RoomSpecification();
        roomSpecification2.addTechnology(Technology.H323);
        roomSpecification2.setParticipantCount(5);
        print(Report.MessageType.DOMAIN_ADMIN, roomSpecification2);

        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.setCallInitiation(CallInitiation.VIRTUAL_ROOM);
        compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));
        compartmentSpecification.addChildSpecification(new ExternalEndpointSpecification(Technology.H323));
        print(Report.MessageType.USER, compartmentSpecification);
    }

    @Test
    public void testRoomWithAliases() throws Exception
    {
            DeviceResource deviceResource1 = new DeviceResource();
            deviceResource1.setAllocatable(true);
            deviceResource1.addTechnology(Technology.H323);
            deviceResource1.addCapability(
                    new RoomProviderCapability(5, new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_E164}));
            deviceResource1.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME, true));
            deviceResource1.addCapability(new AliasProviderCapability("1", AliasType.H323_E164, true));
            createResource(deviceResource1);

            RoomSpecification roomSpecification1 = new RoomSpecification();
            roomSpecification1.addTechnology(Technology.H323);
            roomSpecification1.setParticipantCount(5);
            print(Report.MessageType.USER, roomSpecification1);
    }

    private void print(Report.MessageType messageType, ReservationTaskProvider... reservationTaskProviders)
            throws SchedulerException
    {
        SchedulerContext schedulerContext = createSchedulerContext();
        for (ReservationTaskProvider reservationTaskProvider : reservationTaskProviders) {
            ReservationTask reservationTask = reservationTaskProvider.createReservationTask(schedulerContext);
            print(messageType, reservationTask);
        }
    }

    private void printProvided(Report.MessageType messageType, ReservationTaskProvider reservationTaskProvider1,
            ReservationTaskProvider reservationTaskProvider2) throws SchedulerException
    {
        SchedulerContext schedulerContext = createSchedulerContext();

        ReservationTask reservationTask = reservationTaskProvider1.createReservationTask(schedulerContext);
        Reservation reservation = print(messageType, reservationTask);
        reservation.generateTestingId();
        schedulerContext.addAvailableReservation(reservation, AvailableReservation.Type.REUSABLE);

        reservationTask = reservationTaskProvider2.createReservationTask(schedulerContext);
        print(messageType, reservationTask);
    }

    private Reservation print(Report.MessageType messageType, ReservationTask reservationTask) throws SchedulerException
    {
        try {
            Reservation reservation = reservationTask.perform(null);
            getEntityManager().persist(reservation);
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
