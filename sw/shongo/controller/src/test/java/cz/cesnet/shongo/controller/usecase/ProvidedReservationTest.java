package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.fault.EntityToDeleteIsReferencedException;
import org.junit.Test;

import javax.persistence.EntityManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Tests for allocation of single virtual room in a {@link cz.cesnet.shongo.controller.compartment.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ProvidedReservationTest extends AbstractTest
{
    @Test
    public void testTerminal() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        EntityManager entityManager = getEntityManager();

        DeviceResource terminal = new DeviceResource();
        terminal.setAllocatable(true);
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
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

        Reservation reservation = checkSuccessfulAllocation(reservationRequest, cache, entityManager);
        assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        assertEquals(terminalReservation.getId(), existingReservation.getReservation().getId());

        entityManager.close();
    }

    @Test
    public void testTerminalWithParent() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        EntityManager entityManager = getEntityManager();

        Resource room = new Resource();
        room.setAllocatable(true);
        cache.addResource(room, entityManager);

        DeviceResource terminal = new DeviceResource();
        terminal.setParentResource(room);
        terminal.setAllocatable(true);
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        cache.addResource(terminal, entityManager);

        ResourceReservation roomReservation = new ResourceReservation();
        roomReservation.setCreatedBy(Reservation.CreatedBy.USER);
        roomReservation.setSlot("2012-01-01", "2013-01-01");
        roomReservation.setResource(room);
        cache.addReservation(roomReservation, entityManager);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setRequestedSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setSpecification(new ExistingEndpointSpecification(terminal));

        checkFailedAllocation(reservationRequest, cache, entityManager);

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequest = reservationRequestManager.getReservationRequest(reservationRequest.getId());
        reservationRequest.addProvidedReservation(roomReservation);

        Reservation reservation = checkSuccessfulAllocation(reservationRequest, cache, entityManager);
        assertEquals(1, reservation.getChildReservations().size());
        Reservation childReservation = reservation.getChildReservations().get(0);
        assertEquals(ExistingReservation.class, childReservation.getClass());
        ExistingReservation childExistingReservation = (ExistingReservation) childReservation;
        assertEquals(roomReservation.getId(), childExistingReservation.getReservation().getId());

        entityManager.close();
    }

    @Test
    public void testAlias() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        EntityManager entityManager = getEntityManager();

        Resource aliasProvider = new Resource();
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability(Technology.H323, AliasType.E164, "95000000[d]"));
        cache.addResource(aliasProvider, entityManager);

        AliasReservation aliasReservation = new AliasReservation();
        aliasReservation.setCreatedBy(Reservation.CreatedBy.USER);
        aliasReservation.setSlot("2012-01-01", "2013-01-01");
        aliasReservation.setAliasProviderCapability(aliasProvider.getCapability(AliasProviderCapability.class));
        aliasReservation.setAlias(new Alias(Technology.H323, AliasType.E164, "950000005"));
        cache.addReservation(aliasReservation, entityManager);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setRequestedSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setSpecification(new AliasSpecification(Technology.H323));
        reservationRequest.addProvidedReservation(aliasReservation);

        Reservation reservation = checkSuccessfulAllocation(reservationRequest, cache, entityManager);
        assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        assertEquals(aliasReservation.getId(), existingReservation.getReservation().getId());

        entityManager.close();
    }

    @Test
    public void testAliasInCompartment() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        EntityManager entityManager = getEntityManager();

        DeviceResource mcu = new DeviceResource();
        mcu.setAllocatable(true);
        mcu.setTechnology(Technology.H323);
        mcu.addCapability(new VirtualRoomsCapability(100));
        cache.addResource(mcu, entityManager);

        Resource aliasProvider = new Resource();
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability(Technology.H323, AliasType.E164, "950000001"));
        cache.addResource(aliasProvider, entityManager);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setType(ReservationRequestType.NORMAL);
        aliasReservationRequest.setRequestedSlot("2012-01-01", "P1Y");
        aliasReservationRequest.setSpecification(new AliasSpecification(Technology.H323));

        AliasReservation aliasReservation = (AliasReservation) checkSuccessfulAllocation(aliasReservationRequest, cache,
                entityManager);
        assertEquals(aliasReservation.getAlias().getValue(), "950000001");

        ReservationRequest compartmentReservationRequest = new ReservationRequest();
        compartmentReservationRequest.setType(ReservationRequestType.NORMAL);
        compartmentReservationRequest.setRequestedSlot("2012-06-22T14:00", "PT2H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 3));
        compartmentReservationRequest.setSpecification(compartmentSpecification);
        compartmentReservationRequest.addProvidedReservation(aliasReservation);

        Reservation reservation = checkSuccessfulAllocation(compartmentReservationRequest, cache, entityManager);

        try {
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            reservationRequestManager.delete(aliasReservationRequest);
            fail("Exception that reservation request is still referenced should be thrown");
        }
        catch (EntityToDeleteIsReferencedException exception) {
        }

        entityManager.close();
    }

    @Test
    public void testUseOnlyValidProvidedReservations() throws Exception
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
        terminalReservation.setSlot("2012-01-01", "2012-06-22T15:00");
        terminalReservation.setResource(terminal);
        cache.addReservation(terminalReservation, entityManager);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setRequestedSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setSpecification(new ExistingEndpointSpecification(terminal));
        reservationRequest.addProvidedReservation(terminalReservation);

        checkFailedAllocation(reservationRequest, cache, entityManager);

        entityManager.close();
    }

    @Test
    public void testProvidedReservationsFromSet() throws Exception
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

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setType(ReservationRequestType.NORMAL);
        reservationRequestSet.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), "PT2H");
        reservationRequestSet.addSpecification(new ExistingEndpointSpecification(terminal));
        reservationRequestSet.addProvidedReservation(terminalReservation);

        Reservation reservation = checkSuccessfulAllocation(reservationRequestSet, cache, entityManager);
        assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        assertEquals(terminalReservation.getId(), existingReservation.getReservation().getId());

        entityManager.close();
    }
}
