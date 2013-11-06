package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.executable.*;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Tests for allocation of migrations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerMigrationTest extends AbstractControllerTest
{
    @Test
    public void testRoomMigration() throws Exception
    {
        DeviceResource mcu1 = new DeviceResource();
        mcu1.setName("mcu1");
        mcu1.setAllocatable(true);
        mcu1.addTechnology(Technology.H323);
        mcu1.addCapability(new RoomProviderCapability(5));
        String mcu1Id = getResourceService().createResource(SECURITY_TOKEN, mcu1);

        DeviceResource mcu2 = new DeviceResource();
        mcu2.setName("mcu2");
        mcu2.setAllocatable(true);
        mcu2.addTechnology(Technology.H323);
        mcu2.addCapability(new RoomProviderCapability(10));
        String mcu2Id = getResourceService().createResource(SECURITY_TOKEN, mcu2);

        ReservationService service = getReservationService();

        // Allocate room
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T12:00", "2012-01-01T14:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5, Technology.H323);
        roomSpecification.setResourceId(mcu1Id);
        reservationRequest.setSpecification(roomSpecification);
        String requestId = service.createReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-01T12:00/2012-02-01T12:00"));
        Reservation reservation = checkAllocated(requestId);

        // Set the allocated room as started, because a migration is allocated only for started rooms
        EntityManager entityManager = createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        cz.cesnet.shongo.controller.booking.executable.Executable executable = executableManager.get(EntityIdentifier.parseId(
                cz.cesnet.shongo.controller.booking.executable.Executable.class, reservation.getExecutable().getId()));
        executable.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.STARTED);
        executableManager.update(executable);
        entityManager.close();

        // Modify room
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        roomSpecification = ((RoomSpecification) reservationRequest.getSpecification());
        roomSpecification.setParticipantCount(10);
        roomSpecification.setResourceId(null);
        requestId = service.modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        runScheduler(Interval.parse("2012-01-01T13:00/2012-02-01T13:00"));
        checkAllocated(requestId);

        // Check all reservations
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, requestId);
        List<String> reservationIds = reservationRequest.getReservationIds();
        Assert.assertEquals(2, reservationIds.size());
        RoomReservation reservation1 = (RoomReservation) service.getReservation(SECURITY_TOKEN, reservationIds.get(0));
        Assert.assertEquals(Interval.parse("2012-01-01T12:00/2012-01-01T13:00"), reservation1.getSlot());
        Assert.assertEquals(mcu1Id, reservation1.getResourceId());
        RoomReservation reservation2 = (RoomReservation) service.getReservation(SECURITY_TOKEN, reservationIds.get(1));
        Assert.assertEquals(Interval.parse("2012-01-01T13:00/2012-01-01T14:00"), reservation2.getSlot());
        Assert.assertEquals(mcu2Id, reservation2.getResourceId());
        RoomExecutable roomExecutable1 = (RoomExecutable) reservation1.getExecutable();
        Assert.assertEquals(Interval.parse("2012-01-01T12:00/2012-01-01T13:00"), roomExecutable1.getSlot());
        Assert.assertNotNull(mcu1Id, roomExecutable1.getResourceId());
        RoomExecutable roomExecutable2 = (RoomExecutable) reservation2.getExecutable();
        Assert.assertEquals(Interval.parse("2012-01-01T13:00/2012-01-01T14:00"), roomExecutable2.getSlot());
        Assert.assertNotNull(mcu2Id, roomExecutable2.getResourceId());
        Assert.assertEquals(roomExecutable1.getId(), roomExecutable2.getMigratedExecutable().getId());
    }
}
