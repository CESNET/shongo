package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.request.CompartmentSpecification;
import cz.cesnet.shongo.controller.request.ExistingEndpointSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSetSpecification;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.TerminalCapability;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * Tests for allocation of single virtual room in a {@link cz.cesnet.shongo.controller.compartment.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ProvidedReservationTest extends AbstractTest
{
    @Test
    public void testCloneProvidedReservationsFromSet() throws Exception
    {
        throw new TodoImplementException();
    }

    @Test
    public void testTerminal() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        EntityManager entityManager = getEntityManager();

        DeviceResource terminal = new DeviceResource();
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        cache.addResource(terminal, entityManager);

        ResourceReservation terminalReservation = new ResourceReservation();
        terminalReservation.setCreatedBy(Reservation.CreatedBy.USER);
        terminalReservation.setSlot("2012-01-01", "2013-01-01");
        terminalReservation.setResource(terminal);
        cache.addReservation(terminalReservation, entityManager);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setRequestedSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setSpecification(new ExistingEndpointSpecification(terminal));
        reservationRequest.addProvidedReservation(terminalReservation);

        checkSuccessfulAllocation(reservationRequest, cache);

        entityManager.close();
    }

    @Test
    public void testNotDeletingProvidedReservations() throws Exception
    {
        throw new TodoImplementException("Do not delete child reservations which hasn't set the parent reservation.");
    }

    @Test
    public void testUseOnlyValidProvidedReservations() throws Exception
    {
        throw new TodoImplementException("Use only reservations in allocated interval.");
    }
}
