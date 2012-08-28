package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TransactionHelper;
import cz.cesnet.shongo.controller.allocation.*;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.request.CompartmentRequest;
import cz.cesnet.shongo.controller.request.CompartmentRequestManager;
import cz.cesnet.shongo.controller.request.ResourceSpecification;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.StandaloneTerminalCapability;
import cz.cesnet.shongo.controller.scheduler.InterconnectableGroup;
import cz.cesnet.shongo.controller.scheduler.Task;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a component of a domain controller that is responsible for allocating resources
 * from a {@link CompartmentRequest} to the {@link AllocatedCompartment}.
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

    /**
     * Run scheduler on given entityManagerFactory and interval.
     *
     * @param entityManager
     * @param interval
     */
    public static void createAndRun(Interval interval, EntityManager entityManager, ResourceDatabase resourceDatabase)
            throws FaultException
    {
        Scheduler scheduler = new Scheduler();
        scheduler.setResourceDatabase(resourceDatabase);
        scheduler.init();
        scheduler.run(interval, entityManager);
        scheduler.destroy();
    }

    /**
     * Run scheduler for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval, EntityManager entityManager) throws FaultException
    {
        logger.info("Running scheduler for interval '{}'...", TemporalHelper.formatInterval(interval));

        TransactionHelper.Transaction transaction = TransactionHelper.beginTransaction(entityManager);

        // Delete all allocated compartments which was marked for deletion
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);
        allocatedCompartmentManager.deleteAllMarked(resourceDatabase);

        // Set current interval as working to resource database (it will reload allocations only when
        // the interval changes)
        resourceDatabase.setWorkingInterval(interval, entityManager);

        try {
            CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
            List<CompartmentRequest> compartmentRequests = compartmentRequestManager.listCompleted(interval);

            // TODO: Process permanent first
            // TODO: Apply some other priority to compartment requests

            for (CompartmentRequest compartmentRequest : compartmentRequests) {
                allocateCompartmentRequest(compartmentRequest, entityManager);
            }

            transaction.commit();
        }
        catch (Exception exception) {
            transaction.rollback();
            throw new FaultException(exception, ControllerFault.SCHEDULER_FAILED);
        }
    }

    /**
     * Allocate compartment request.
     *
     * @param compartmentRequest
     */
    private void allocateCompartmentRequest(CompartmentRequest compartmentRequest, EntityManager entityManager)
    {
        logger.info("Allocating compartment request '{}'...", compartmentRequest.getId());

        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);

        // Get existing allocated compartment
        AllocatedCompartment allocatedCompartment =
                allocatedCompartmentManager.getByCompartmentRequest(compartmentRequest);

        // TODO: Try to intelligently reallocate and not delete old allocation
        // Delete old allocation
        if (allocatedCompartment != null) {
            allocatedCompartmentManager.delete(allocatedCompartment, resourceDatabase);
        }

        // Schedule a new allocation
        allocatedCompartment = new AllocatedCompartment();
        allocatedCompartment.setCompartmentRequest(compartmentRequest);

        try {
            // Get requested slot and check it's maximum duration
            Interval requestedSlot = compartmentRequest.getRequestedSlot();
            if (requestedSlot.toDuration().isLongerThan(resourceDatabase.getDeviceAllocationMaximumDuration())) {
                throw new FaultException("Requested slot '%s' is longer than maximum '%s'!",
                        requestedSlot.toPeriod().normalizedStandard().toString(),
                        resourceDatabase.getDeviceAllocationMaximumDuration().toString());
            }

            // Get map of requested resources with requested persons for them
            Map<ResourceSpecification, List<Person>> requestedResourcesWithPersons =
                    compartmentRequest.getRequestedResourcesWithPersons();

            // Initialize scheduler task (by adding all requested resources to it)
            Task task = new Task();
            for (ResourceSpecification resourceSpecification : requestedResourcesWithPersons.keySet()) {
                task.addResourceSpecification(resourceSpecification);
            }

            // Perform scheduling task
            task.fillInterconnectableGroups(requestedSlot, entityManager, resourceDatabase);
            task.mergeInterconnectableGroups();

            // Check some resources are requested
            List<InterconnectableGroup> interconnectableGroups = task.getInterconnectableGroups();
            if (interconnectableGroups.size() == 0) {
                throw new FaultException("No resources are requested for allocation.");
            }

            // Check if virtual rooms is needed
            boolean virtualRoomIsNeeded = true;
            if (interconnectableGroups.size() == 1) {
                InterconnectableGroup group = interconnectableGroups.get(0);
                int portCount = group.getPortCount();
                if (portCount < 2) {
                    throw new FaultException("At least two devices/ports must be requested.");
                }
                else if (portCount == 2 && group.getDeviceResourcesCount(StandaloneTerminalCapability.class) == 2) {
                    // No virtual room is needed
                    virtualRoomIsNeeded = false;
                }
            }

            // Allocate virtual room
            if (virtualRoomIsNeeded) {
                // Try to allocate single virtual room
                int requestedPortCount = task.getTotalPortCount();
                Set<Set<Technology>> technologiesVariants = task.getInterconnectingTechnologies();

                // TODO: Try to use virtual rooms from special terminals

                // Get available virtual rooms
                List<AvailableVirtualRoom> availableVirtualRooms = resourceDatabase.findAvailableVirtualRoomsByVariants(
                        requestedSlot, requestedPortCount, technologiesVariants, entityManager);
                if (availableVirtualRooms.size() == 0) {
                    // TODO: Resolve multiple virtual rooms and/or gateways for connecting endpoints

                    // No virtual rooms is available
                    throw new FaultException("No single virtual room was found for following specification:\n"
                            + "       Time slot: %s\n"
                            + "      Technology: %s\n"
                            + " Number of ports: %d",
                            TemporalHelper.formatInterval(requestedSlot),
                            Technology.formatTechnologiesVariants(technologiesVariants),
                            requestedPortCount);
                }

                // Sort virtual rooms from the most filled to the least filled
                Collections.sort(availableVirtualRooms, new Comparator<AvailableVirtualRoom>()
                {
                    @Override
                    public int compare(AvailableVirtualRoom first, AvailableVirtualRoom second)
                    {
                        return -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
                    }
                });

                // Allocate virtual room
                AvailableVirtualRoom availableVirtualRoom = availableVirtualRooms.get(0);
                AllocatedVirtualRoom allocatedVirtualRoom = new AllocatedVirtualRoom();
                allocatedVirtualRoom.setResource(availableVirtualRoom.getDeviceResource());
                allocatedVirtualRoom.setSlot(requestedSlot);
                allocatedVirtualRoom.setPortCount(requestedPortCount);
                allocatedCompartment.addAllocatedResource(allocatedVirtualRoom);
            }

            // Allocated other resources
            List<AllocatedResource> allocatedResources = allocateResources(task, requestedSlot);
            for (AllocatedResource allocatedResource : allocatedResources) {
                allocatedCompartment.addAllocatedResource(allocatedResource);
            }

            // Create allocated compartment
            allocatedCompartmentManager.create(allocatedCompartment);

            // Add allocated resources to the resource database
            for (AllocatedResource allocatedResource : allocatedCompartment.getAllocatedResources()) {
                resourceDatabase.addAllocatedResource(allocatedResource);
            }

            // Set compartment state to allocated
            compartmentRequest.setState(CompartmentRequest.State.ALLOCATED);
            compartmentRequestManager.update(compartmentRequest);
        }
        catch (FaultException exception) {
            compartmentRequest.setState(CompartmentRequest.State.ALLOCATION_FAILED, exception.getMessage());
            compartmentRequestManager.update(compartmentRequest);
        }
    }

    private List<AllocatedResource> allocateResources(Task task, Interval requestedSlot)
    {
        List<AllocatedResource> allocatedResources = new ArrayList<AllocatedResource>();
        for (Resource resource : task.getResources()) {
            AllocatedResource allocatedResource;
            if (resource instanceof DeviceResource) {
                AllocatedDevice allocatedDevice = new AllocatedDevice();
                // TODO: Add persons for allocated device
                allocatedResource = allocatedDevice;
            }
            else {
                allocatedResource = new AllocatedResource();
            }
            allocatedResource.setSlot(requestedSlot);
            allocatedResource.setResource(resource);
            allocatedResources.add(allocatedResource);
        }
        return allocatedResources;
    }
}
