package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.controller.allocation.*;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.resource.DeviceResource;
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
public class Scheduler extends Component.WithDomain
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
            throw new IllegalStateException(
                    "Component " + getClass().getName() + " doesn't have the resource database set!");
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

        // Delete all allocated compartments which was marked for deletion
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);
        allocatedCompartmentManager.deleteAllMarked(resourceDatabase);

        // Set current interval as working to resource database (it will reinialize only when it changes)
        resourceDatabase.setWorkingInterval(interval, entityManager);

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

        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);

        // Get existing allocated compartment
        AllocatedCompartment allocatedCompartment =
                allocatedCompartmentManager.getByCompartmentRequest(compartmentRequest);

        // Delete old allocation
        if (allocatedCompartment != null) {
            allocatedCompartmentManager.delete(allocatedCompartment, resourceDatabase);
        }

        // Schedule a new allocation
        allocatedCompartment = new AllocatedCompartment();
        allocatedCompartment.setCompartmentRequest(compartmentRequest);

        // Get requested slot
        Interval requestedSlot = compartmentRequest.getRequestedSlot();

        // Get map of requested resources with requested persons for them
        Map<ResourceSpecification, List<Person>> requestedResourcesWithPersons =
                compartmentRequest.getRequestedResourcesWithPersons();

        // Create list of requested resources from which will be removed all processed resources
        List<ResourceSpecification> resourceSpecifications = new ArrayList<ResourceSpecification>();
        for (ResourceSpecification resource : requestedResourcesWithPersons.keySet()) {
            resourceSpecifications.add(resource);
        }

        // Track how many ports for each technology is needed
        /*Map<Technology, Integer> technologyPorts = new HashMap<Technology, Integer>();
        for (Technology technology : externalEndpoint.getTechnologies()) {
            Integer currentPortCount = technologyPorts.get(technology);
            if (currentPortCount == null) {
                currentPortCount = 0;
            }
            currentPortCount += externalEndpoint.getCount();
            technologyPorts.put(technology, currentPortCount);
        }*/

        Task task = new Task();

        // Process all external endpoint and existing resources
        for (Iterator<ResourceSpecification> iterator = resourceSpecifications.iterator(); iterator.hasNext(); ) {
            ResourceSpecification resourceSpecification = iterator.next();
            if (resourceSpecification instanceof ExternalEndpointSpecification) {
                ExternalEndpointSpecification externalEndpoint = (ExternalEndpointSpecification) resourceSpecification;
                task.add(externalEndpoint);
                iterator.remove();
            }
            else if (resourceSpecification instanceof ExistingResourceSpecification) {
                ExistingResourceSpecification existingResource = (ExistingResourceSpecification) resourceSpecification;
                Resource resource = existingResource.getResource();
                // If resource is not already allocated on requested time slot
                if (resourceDatabase.isResourceAvailable(resource, requestedSlot)) {
                    // Device resources are add to the task
                    if (resource instanceof DeviceResource) {
                        task.add((DeviceResource) resource);
                    }
                    // Not device resources are directly allocated
                    else {
                        AllocatedResource allocatedResource = new AllocatedResource();
                        allocatedResource.setSlot(requestedSlot);
                        allocatedResource.setResource(resource);
                        allocatedCompartment.addAllocatedResource(allocatedResource);
                    }
                }
                else {
                    // Requested resource is not available
                    setCompartmentRequestAllocationFailed(compartmentRequestManager, compartmentRequest,
                            "Requested resource is not available in specified time slot:\n"
                                    + " Time Slot: %s\n"
                                    + "  Resource: %s\n",
                            requestedSlot.toString(), domain.formatIdentifier(resource.getId()));
                    return;
                }
                iterator.remove();
            }
        }

        // Process all lookup resource specifications
        for (Iterator<ResourceSpecification> iterator = resourceSpecifications.iterator(); iterator.hasNext(); ) {
            ResourceSpecification resourceSpecification = iterator.next();
            if (resourceSpecification instanceof LookupResourceSpecification) {
                LookupResourceSpecification lookupResource = (LookupResourceSpecification) resourceSpecification;
                Set<Technology> technologies = lookupResource.getTechnologies();
                // Lookup device resources
                List<DeviceResource> deviceResources = resourceDatabase.findAvailableTerminal(requestedSlot,
                        technologies.toArray(new Technology[technologies.size()]));
                // If some was found, select the best one and it as group for scheduling
                if (deviceResources.size() > 0) {
                    // TODO: Select best resource based on some criteria
                    DeviceResource deviceResource = deviceResources.get(0);
                    task.add(deviceResource);
                }
                else {
                    // Resource was not found
                    StringBuilder builder = new StringBuilder();
                    for (Technology technology : technologies) {
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }
                        builder.append(technology.getName());
                    }
                    setCompartmentRequestAllocationFailed(compartmentRequestManager, compartmentRequest,
                            "No resource was found for following specification:\n"
                                    + "    Time Slot: %s\n"
                                    + " Technologies: %s\n",
                            requestedSlot.toString(), builder.toString());
                    return;
                }
                iterator.remove();
            }
        }

        // Check if all specification was processed
        if (resourceSpecifications.size() > 0) {
            ResourceSpecification resourceSpecification = resourceSpecifications.get(0);
            throw new FaultException("Implement allocation of '%s' resource.", resourceSpecification.getClass());
        }

        // Merge groups
        task.merge();

        // TODO: Allocate endpoints
        // TODO: Allocate aliases for endpoint (if needed)

        if (task.size() == 0) {
            throw new FaultException("No resources are requested for allocation.");
        }
        else if (task.size() > 1) {
            throw new FaultException("Only single virtual rooms can be allocated for now.");
        }
        // For now only single technology is possible to allocate
        TaskItem taskItem = task.get(0);

        // List available virtual rooms which can connect all requested endpoints
        List<AvailableVirtualRoom> availableVirtualRooms = resourceDatabase.findAvailableVirtualRooms(
                requestedSlot, taskItem.getPortCount(), taskItem.getTechnologies(), entityManager);
        if (availableVirtualRooms.size() > 0) {
            // Sort virtual rooms by theirs fullness
            Collections.sort(availableVirtualRooms, new Comparator<AvailableVirtualRoom>()
            {
                @Override
                public int compare(AvailableVirtualRoom first, AvailableVirtualRoom second)
                {
                    return -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
                }
            });
            AvailableVirtualRoom availableVirtualRoom = availableVirtualRooms.get(0);
            AllocatedVirtualRoom allocatedVirtualRoom = new AllocatedVirtualRoom();
            allocatedVirtualRoom.setResource(availableVirtualRoom.getDeviceResource());
            allocatedVirtualRoom.setSlot(requestedSlot);
            allocatedVirtualRoom.setPortCount(taskItem.getPortCount());
            if (requestedSlot.toDuration().isLongerThan(AllocatedVirtualRoom.MAXIMUM_DURATION)) {
                throw new FaultException("Requested slot '%s' is longer than maximum '%s'!",
                        requestedSlot.toDuration().toString(), AllocatedVirtualRoom.MAXIMUM_DURATION.toString());
            }
            allocatedCompartment.addAllocatedResource(allocatedVirtualRoom);
            allocatedCompartmentManager.create(allocatedCompartment);

            // Add allocated resources to the resource database
            for (AllocatedResource allocatedResource : allocatedCompartment.getAllocatedResources()) {
                resourceDatabase.addAllocatedResource(allocatedResource);
            }

            // Set compartment state to allocated
            compartmentRequest.setState(CompartmentRequest.State.ALLOCATED);
            compartmentRequestManager.update(compartmentRequest);
        }
        else {
            // TODO: Resolve multiple virtual rooms and/or gateways for connecting endpoints

            setCompartmentRequestAllocationFailed(compartmentRequestManager, compartmentRequest,
                    "No single virtual room was found for following specification:\n"
                            + "       Time slot: %s\n"
                            + "      Technology: %s\n"
                            + " Number of ports: %d\n",
                    requestedSlot.toString(), taskItem.getTechnologiesAsString(), taskItem.getPortCount());
        }
    }

    /**
     * Set allocation failed state to compartment request.
     *
     * @param compartmentRequestManager
     * @param compartmentRequest
     * @param format
     * @param objects
     */
    private static void setCompartmentRequestAllocationFailed(CompartmentRequestManager compartmentRequestManager,
            CompartmentRequest compartmentRequest, String format, Object... objects)
    {
        compartmentRequest.setState(CompartmentRequest.State.ALLOCATION_FAILED,
                String.format(format, objects));
        compartmentRequestManager.update(compartmentRequest);
    }

    /**
     * Run scheduler on given entityManagerFactory and interval.
     *
     * @param entityManagerFactory
     * @param interval
     */
    public static void run(EntityManagerFactory entityManagerFactory, Interval interval,
            ResourceDatabase resourceDatabase, Domain domain) throws FaultException
    {
        Scheduler scheduler = new Scheduler();
        scheduler.setEntityManagerFactory(entityManagerFactory);
        scheduler.setResourceDatabase(resourceDatabase);
        scheduler.setDomain(domain);
        scheduler.init();
        scheduler.run(interval);
        scheduler.destroy();
    }

    public static class Task extends ArrayList<TaskItem>
    {
        public void add(ExternalEndpointSpecification externalEndpointSpecification)
        {
            throw new RuntimeException("TODO: Implement");
        }

        public void add(DeviceResource deviceResource)
        {
            throw new RuntimeException("TODO: Implement");
        }

        public void merge()
        {
            throw new RuntimeException("TODO: Implement");
        }
    }

    public static class TaskItem
    {
        private Set<Technology> technologies = new HashSet<Technology>();

        private int portCount = 0;

        private Set<DeviceResource> deviceResources = new HashSet<DeviceResource>();

        public Technology[] getTechnologies()
        {
            return technologies.toArray(new Technology[technologies.size()]);
        }
        
        public String getTechnologiesAsString()
        {
            StringBuilder builder = new StringBuilder();
            for (Technology technology : technologies) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(technology.getName());
            }
            return builder.toString();
        }
        
        public int getPortCount()
        {
            return portCount;
        }
    }
}
