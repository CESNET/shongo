package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.AbstractSchedulerTest;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.booking.alias.AliasSpecification;
import cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification;
import cz.cesnet.shongo.controller.booking.participant.ExternalEndpointParticipant;
import cz.cesnet.shongo.controller.booking.participant.ExternalEndpointSetParticipant;
import cz.cesnet.shongo.controller.booking.room.RoomSpecification;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.report.Report;
import org.junit.Test;

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
        print(Report.UserType.DOMAIN_ADMIN, roomSpecification1);

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
        aliasSpecification3.setAliasProviderCapability(deviceResource3.getCapability(AliasProviderCapability.class));
        printReusable(Report.UserType.DOMAIN_ADMIN, aliasSpecification1, aliasSpecification2);
        print(Report.UserType.DOMAIN_ADMIN, aliasSpecification1, aliasSpecification2, aliasSpecification3);

        RoomSpecification roomSpecification2 = new RoomSpecification();
        roomSpecification2.addTechnology(Technology.H323);
        roomSpecification2.setParticipantCount(5);
        print(Report.UserType.DOMAIN_ADMIN, roomSpecification2);

        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.setCallInitiation(CallInitiation.VIRTUAL_ROOM);
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 2));
        compartmentSpecification.addParticipant(new ExternalEndpointParticipant(Technology.H323));
        print(Report.UserType.USER, compartmentSpecification);
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
        print(Report.UserType.USER, roomSpecification1);
    }

    private void print(Report.UserType userType, ReservationTaskProvider... reservationTaskProviders)
            throws SchedulerException
    {
        SchedulerContext schedulerContext = createSchedulerContext();
        for (ReservationTaskProvider reservationTaskProvider : reservationTaskProviders) {
            ReservationTask reservationTask =
                    reservationTaskProvider.createReservationTask(schedulerContext, Temporal.INTERVAL_INFINITE);
            print(userType, reservationTask);
        }
    }

    private void printReusable(Report.UserType userType, ReservationTaskProvider reservationTaskProvider1,
            ReservationTaskProvider reservationTaskProvider2) throws SchedulerException
    {
        SchedulerContext schedulerContext = createSchedulerContext();
        SchedulerContextState schedulerContextState = schedulerContext.getState();

        ReservationTask reservationTask =
                reservationTaskProvider1.createReservationTask(schedulerContext, Temporal.INTERVAL_INFINITE);
        Reservation reservation = print(userType, reservationTask);
        if (reservation != null) {
            reservation.generateTestingId();
            schedulerContextState.addAvailableReservation(reservation, AvailableReservation.Type.REUSABLE);

            reservationTask =
                    reservationTaskProvider2.createReservationTask(schedulerContext, Temporal.INTERVAL_INFINITE);
            print(userType, reservationTask);
        }
    }

    private Reservation print(Report.UserType userType, ReservationTask reservationTask) throws SchedulerException
    {
        try {
            Reservation reservation = reservationTask.perform();
            reservation.setUserId("0");
            getEntityManager().persist(reservation);
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append(reservationTask.getClass().getSimpleName() + " reports:\n");
            builder.append("\n");
            for (SchedulerReport report : reservationTask.getReports()) {
                builder.append(report.toAllocationStateReport(userType));
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
            builder.append(exception.getTopReport().toAllocationStateReport(userType));
            builder.append("\n");
            System.err.print(builder.toString());
            System.err.flush();
            return null;
        }

    }
}
