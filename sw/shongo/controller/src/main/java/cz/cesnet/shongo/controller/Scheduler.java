package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.controller.allocation.*;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.request.CompartmentRequest;
import cz.cesnet.shongo.controller.request.CompartmentRequestManager;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.request.ResourceSpecification;
import cz.cesnet.shongo.controller.resource.Resource;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Represents a component of a domain controller that is responsible for scheduling resources for compartment requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler extends Component
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /**
     * @see {@link ResourceDatabase}
     */
    private ResourceDatabase resourceDatabase;

    /**
     * @param resourceDatabase sets the {@link #resourceDatabase}
     */
    public void setResourceDatabase(ResourceDatabase resourceDatabase)
    {
        this.resourceDatabase = resourceDatabase;
    }

    @Override
    public void init()
    {
        if (resourceDatabase == null) {
            throw new IllegalStateException("Component " + getClass().getName()
                    + " doesn't have the resource database set!");
        }
        super.init();
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Run scheduler for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval) throws FaultException
    {
        checkInitialized();

        logger.info("Running scheduler for interval '{}'...", formatInterval(interval));

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        VirtualRoomDatabase virtualRoomDatabase = resourceDatabase.getVirtualRoomDatabase();
        virtualRoomDatabase.setWorkingInterval(interval, entityManager);

        try {
            CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(getEntityManager());
            List<CompartmentRequest> compartmentRequests = compartmentRequestManager.listCompleted(interval);

            // TODO: Apply some priority

            for (CompartmentRequest compartmentRequest : compartmentRequests) {
                allocateCompartmentRequest(compartmentRequest, entityManager);
            }

            entityManager.getTransaction().commit();
        }
        catch (Exception exception) {
            entityManager.getTransaction().rollback();
            throw new FaultException(exception, ControllerFault.SCHEDULER_FAILED);
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * Allocate compartment request.
     *
     * @param compartmentRequest
     */
    private void allocateCompartmentRequest(CompartmentRequest compartmentRequest, EntityManager entityManager)
            throws FaultException
    {
        logger.info("Allocating compartment request '{}'...", compartmentRequest.getId());

        VirtualRoomDatabase virtualRoomDatabase = resourceDatabase.getVirtualRoomDatabase();
        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);

        // Get requested slot
        Interval requestedSlot = compartmentRequest.getRequestedSlot();

        // Get existing allocated compartment
        AllocatedCompartment allocatedCompartment =
                allocatedCompartmentManager.getByCompartmentRequest(compartmentRequest);
        // If doesn't exists create a new
        if (allocatedCompartment == null) {
            allocatedCompartment = new AllocatedCompartment();
            allocatedCompartment.setCompartmentRequest(compartmentRequest);
            allocatedCompartmentManager.create(allocatedCompartment);
        }

        // Get list of requested resources
        Map<ResourceSpecification, List<Person>> requestedResourcesWithPersons =
                compartmentRequest.getRequestedResourcesWithPersons();

        // Get list of already allocated resources
        List<AllocatedResource> allocatedResources = allocatedCompartment.getAllocatedResources();

        // Schedule a new allocation
        if (allocatedResources.size() == 0) {
            // Track how many ports for each technology is needed
            Map<Technology, Integer> technologyPorts = new HashMap<Technology, Integer>();

            // Iterate though all requested resource and increment requested ports
            for (ResourceSpecification resource : requestedResourcesWithPersons.keySet()) {
                if (resource instanceof ExternalEndpointSpecification) {
                    ExternalEndpointSpecification externalEndpoint = (ExternalEndpointSpecification) resource;
                    for (Technology technology : externalEndpoint.getTechnologies()) {
                        Integer currentPortCount = technologyPorts.get(technology);
                        if (currentPortCount == null) {
                            currentPortCount = 0;
                        }
                        currentPortCount += externalEndpoint.getCount();
                        technologyPorts.put(technology, currentPortCount);
                    }
                }
                else {
                    throw new FaultException("Implement allocation of '%s' resource.", resource.getClass());
                }
            }

            // TODO: Allocate endpoints
            // TODO: Allocate aliases for endpoint (if needed)

            if (technologyPorts.size() == 0) {
                throw new FaultException("No resources are requested for allocation.");
            }
            else if (technologyPorts.size() > 1) {
                throw new FaultException("Only resources of a single technology is allowed for now.");
            }
            // For now only single technology is possible to allocate
            Technology technology = technologyPorts.keySet().iterator().next();
            int requestedPortCount = technologyPorts.get(technology);

            // List available virtual rooms which can connect all requested endpoints
            List<AvailableVirtualRoom> availableVirtualRooms = virtualRoomDatabase.findAvailableVirtualRooms(
                    requestedSlot, requestedPortCount, new Technology[]{technology}, entityManager);
            if (availableVirtualRooms.size() > 0) {
                Collections.sort(availableVirtualRooms, new Comparator<AvailableVirtualRoom>()
                {
                    @Override
                    public int compare(AvailableVirtualRoom first, AvailableVirtualRoom second)
                    {
                        return Integer.valueOf(first.getAvailablePortCount()).compareTo(second.getAvailablePortCount());
                    }
                });
                AvailableVirtualRoom availableVirtualRoom = availableVirtualRooms.get(0);
                AllocatedVirtualRoom allocatedVirtualRoom = new AllocatedVirtualRoom();
                allocatedVirtualRoom.setResource(availableVirtualRoom.getDeviceResource());
                allocatedVirtualRoom.setSlot(requestedSlot);
                allocatedVirtualRoom.setPortCount(requestedPortCount);
                if (requestedSlot.toDuration().isLongerThan(AllocatedVirtualRoom.MAXIMUM_DURATION)) {
                    throw new FaultException("Requested slot '%s' is longer than maximum '%s'!",
                            requestedSlot.toDuration().toString(), AllocatedVirtualRoom.MAXIMUM_DURATION.toString());
                }
                allocatedCompartment.addAllocatedResource(allocatedVirtualRoom);

                virtualRoomDatabase.addAllocatedVirtualRoom(allocatedVirtualRoom);
            }
            else {
                compartmentRequest.setState(CompartmentRequest.State.ALLOCATION_FAILED);
                compartmentRequestManager.update(compartmentRequest);
                allocatedCompartmentManager.delete(allocatedCompartment);

                // TODO: Save a reason somewhere to compartment request

                // TODO: Resolve multiple virtual rooms and/or gateways for connecting endpoints
                /*throw new FaultException("No virtual room was found for following specification:\n"
                        + "       Interval: %s\n"
                        + "     Technology: %s\n"
                        + "Number of ports: %d\n",
                        requestedSlot.toString(), technology.toString(), requestedPortCount);*/
            }
        }
        // Reschedule existing allocation
        else {
            if (true) {
                throw new RuntimeException("TODO: Implement reallocation");
            }
        }

        compartmentRequest.setState(CompartmentRequest.State.ALLOCATED);
        compartmentRequestManager.update(compartmentRequest);
        allocatedCompartmentManager.update(allocatedCompartment);
    }

    /**
     * Run scheduler on given entityManagerFactory and interval.
     *
     * @param entityManagerFactory
     * @param interval
     */
    public static void run(EntityManagerFactory entityManagerFactory, Interval interval) throws FaultException
    {
        Scheduler scheduler = new Scheduler();
        scheduler.setEntityManagerFactory(entityManagerFactory);
        scheduler.init();
        scheduler.run(interval);
        scheduler.destroy();
    }
}
