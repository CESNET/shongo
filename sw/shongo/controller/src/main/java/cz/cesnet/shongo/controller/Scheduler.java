package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.allocation.*;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.TerminalCapability;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;
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
            CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
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

        // Setup scheduler task
        Task task = new Task();

        try {
            // Check maximum slot duration
            if (requestedSlot.toDuration().isLongerThan(AllocatedResource.MAXIMUM_DURATION)) {
                throw new FaultException("Requested slot '%s' is longer than maximum '%s'!",
                        requestedSlot.toPeriod().normalizedStandard().toString(),
                        AllocatedResource.MAXIMUM_DURATION.toString());
            }

            // Add all requested resources to the task
            try {
                // First process all external endpoint and existing resources
                for (Iterator<ResourceSpecification> iterator = resourceSpecifications.iterator();
                     iterator.hasNext(); ) {
                    ResourceSpecification resourceSpecification = iterator.next();
                    if (resourceSpecification instanceof ExternalEndpointSpecification) {
                        ExternalEndpointSpecification externalEndpoint = (ExternalEndpointSpecification) resourceSpecification;
                        task.add(externalEndpoint);
                        iterator.remove();
                    }
                    else if (resourceSpecification instanceof ExistingResourceSpecification) {
                        ExistingResourceSpecification existingResource = (ExistingResourceSpecification) resourceSpecification;
                        Resource resource = existingResource.getResource();
                        if (task.hasResource(resource)) {
                            // Same resource is requested multiple times
                            throw new FaultException("Resource is requested multiple times in specified time slot:\n"
                                    + "  Resource: %s\n",
                                    domain.formatIdentifier(resource.getId()));
                        }
                        if (!resource.isSchedulable()) {
                            // Requested resource cannot be allocated
                            throw new FaultException("Requested resource cannot be allocated:\n"
                                    + "  Resource: %s\n",
                                    domain.formatIdentifier(resource.getId()));
                        }
                        if (!resourceDatabase.isResourceAvailable(resource, requestedSlot)) {
                            // Requested resource is not available in requested slot
                            throw new FaultException("Requested resource is not available in specified time slot:\n"
                                    + " Time Slot: %s\n"
                                    + "  Resource: %s\n",
                                    TemporalHelper.formatInterval(requestedSlot),
                                    domain.formatIdentifier(resource.getId()));
                        }
                        task.add(resource);
                        iterator.remove();
                    }
                }

                // Then process all lookup resource specifications
                for (Iterator<ResourceSpecification> iterator = resourceSpecifications.iterator();
                     iterator.hasNext(); ) {
                    ResourceSpecification resourceSpecification = iterator.next();
                    if (resourceSpecification instanceof LookupResourceSpecification) {
                        LookupResourceSpecification lookupResource = (LookupResourceSpecification) resourceSpecification;
                        Set<Technology> technologies = lookupResource.getTechnologies();
                        // Lookup device resources
                        List<DeviceResource> deviceResources = resourceDatabase.findAvailableTerminal(requestedSlot,
                                technologies, entityManager);

                        // Select first available device resource
                        // TODO: Select best resource based on some criteria
                        DeviceResource deviceResource = null;
                        for (DeviceResource possibleDeviceResource : deviceResources) {
                            if (task.hasResource(possibleDeviceResource)) {
                                continue;
                            }
                            deviceResource = possibleDeviceResource;
                            break;
                        }

                        // If some was found
                        if (deviceResource != null) {
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
                            throw new FaultException(
                                    "No available resource was found for the following specification:\n"
                                            + "    Time Slot: %s\n"
                                            + " Technologies: %s\n",
                                    TemporalHelper.formatInterval(requestedSlot), builder.toString());
                        }
                        iterator.remove();
                    }
                }

                // TODO: Allocate aliases for endpoint (if needed)

                // Check if all specification was processed
                if (resourceSpecifications.size() > 0) {
                    ResourceSpecification resourceSpecification = resourceSpecifications.get(0);
                    throw new FaultException("Allocation of '%s' resource is not implemented yet.",
                            resourceSpecification.getClass());
                }
            }
            // Handle exceptions that resource cannot be added
            catch (Task.AddResourceException exception) {
                throw new FaultException("Requested resource is not terminal:\n"
                        + "  Resource: %s\n",
                        TemporalHelper.formatInterval(requestedSlot),
                        domain.formatIdentifier(exception.getResource().getId()));
            }

            // Merge task content
            task.merge();
            if (task.size() == 0) {
                throw new FaultException("No resources are requested for allocation.");
            }

            // For now only single virtual room is possible to allocate
            else if (task.size() > 1) {
                throw new FaultException("Only single virtual room can be allocated for now.");
            }
            TaskGroup taskGroup = task.get(0);

            // TODO: Try to use virtual rooms from special terminals

            // List available virtual rooms which can connect all requested endpoints
            List<AvailableVirtualRoom> availableVirtualRooms = resourceDatabase.findAvailableVirtualRooms(
                    requestedSlot, taskGroup.getPortCount(), taskGroup.getTechnologies(), entityManager);
            // Sort virtual rooms from the most filled to the least filled
            Collections.sort(availableVirtualRooms, new Comparator<AvailableVirtualRoom>()
            {
                @Override
                public int compare(AvailableVirtualRoom first, AvailableVirtualRoom second)
                {
                    return -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
                }
            });
            if (availableVirtualRooms.size() == 0) {
                // TODO: Resolve multiple virtual rooms and/or gateways for connecting endpoints

                // No virtual rooms is available
                throw new FaultException("No single virtual room was found for following specification:\n"
                        + "       Time slot: %s\n"
                        + "      Technology: %s\n"
                        + " Number of ports: %d\n",
                        TemporalHelper.formatInterval(requestedSlot), taskGroup.getTechnologiesAsString(),
                        taskGroup.getPortCount());
            }

            // Allocate virtual room
            AvailableVirtualRoom availableVirtualRoom = availableVirtualRooms.get(0);
            AllocatedVirtualRoom allocatedVirtualRoom = new AllocatedVirtualRoom();
            allocatedVirtualRoom.setResource(availableVirtualRoom.getDeviceResource());
            allocatedVirtualRoom.setSlot(requestedSlot);
            allocatedVirtualRoom.setPortCount(taskGroup.getPortCount());
            allocatedCompartment.addAllocatedResource(allocatedVirtualRoom);

            // Allocate other resources
            for (Resource resource : task.getResources()) {
                if (resource instanceof DeviceResource) {
                    AllocatedDevice allocatedDevice = new AllocatedDevice();
                    allocatedDevice.setSlot(requestedSlot);
                    allocatedDevice.setResource(resource);

                    // TODO: Add persons for allocated device

                    allocatedCompartment.addAllocatedResource(allocatedDevice);

                }
                else {
                    AllocatedResource allocatedResource = new AllocatedResource();
                    allocatedResource.setSlot(requestedSlot);
                    allocatedResource.setResource(resource);
                    allocatedCompartment.addAllocatedResource(allocatedResource);
                }
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

    /**
     * Represents a scheduler task for allocating resources
     */
    public static class Task extends ArrayList<TaskGroup>
    {
        /**
         * Set of resources which should be allocated.
         */
        private Set<Resource> resources = new HashSet<Resource>();

        /**
         * Append requested {@link ExternalEndpointSpecification} to the task.
         *
         * @param externalEndpointSpecification
         */
        public void add(ExternalEndpointSpecification externalEndpointSpecification)
        {
            TaskGroup taskItem = new TaskGroup();
            taskItem.portCount = externalEndpointSpecification.getCount();
            taskItem.technologies.addAll(externalEndpointSpecification.getTechnologies());
            add(taskItem);
        }

        /**
         * Append requested {@link Resource}.
         *
         * @param resource
         * @throws AddResourceException
         */
        public void add(Resource resource) throws AddResourceException
        {
            if (resource instanceof DeviceResource) {
                DeviceResource deviceResource = (DeviceResource) resource;
                if (deviceResource.isTerminal()) {
                    TaskGroup taskItem = new TaskGroup();
                    taskItem.portCount = 1;
                    taskItem.technologies.addAll(deviceResource.getCapabilityTechnologies(TerminalCapability.class));
                    taskItem.deviceResources.add(deviceResource);
                    add(taskItem);
                }
                else {
                    // Requested resource is not available
                    throw new AddResourceException(resource);
                }
            }
            resources.add(resource);
        }

        /**
         * @param resource
         * @return true if given {@code resource} was already added to the task,
         *         false otherwise
         */
        public boolean hasResource(Resource resource)
        {
            return resources.contains(resource);
        }

        /**
         * @return collection of all resources which were added to the task
         */
        public Collection<Resource> getResources()
        {
            return resources;
        }

        /**
         * Merge definitions to minimize number of different groups
         */
        public void merge()
        {
            for (int index = 0; index < size(); index++) {
                TaskGroup taskItem = get(index);
                for (int mergeIndex = index + 1; mergeIndex < size(); mergeIndex++) {
                    TaskGroup mergeTaskItem = get(mergeIndex);
                    if (taskItem.merge(mergeTaskItem)) {
                        remove(mergeIndex);
                        mergeIndex--;
                    }
                }
            }
        }

        /**
         * Exception which is thrown when {@link Resource} cannot be added to the {@link Task}
         */
        public static class AddResourceException extends Exception
        {
            /**
             * Specifies which {@link Resource}.
             */
            private Resource resource;

            /**
             * Constructor.
             *
             * @param resource sets the {@link #resource}
             */
            AddResourceException(Resource resource)
            {
                this.resource = resource;
            }

            /**
             * @return {@link #resource}
             */
            public Resource getResource()
            {
                return resource;
            }
        }
    }

    /**
     * Represents a group of compatible resources/ports in {@link Task}.
     */
    public static class TaskGroup
    {
        /**
         * Supported technologies of a group of resources/ports.
         */
        private Set<Technology> technologies = new HashSet<Technology>();

        /**
         * Number of ports requested by the group.
         */
        private int portCount = 0;

        /**
         * Set of device resources in the group.
         */
        private Set<DeviceResource> deviceResources = new HashSet<DeviceResource>();

        /**
         * @return {@link #technologies}
         */
        public Set<Technology> getTechnologies()
        {
            return technologies;
        }

        /**
         * @return formatted {@link #technologies} as string
         */
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

        /**
         * @return {@link #portCount}
         */
        public int getPortCount()
        {
            return portCount;
        }

        /**
         * Try to merge given {@code taskGroup} to this {@link TaskGroup}.
         *
         * @param taskGroup
         * @return true if merge was done,
         *         false otherwise
         */
        public boolean merge(TaskGroup taskGroup)
        {
            if (!technologies.equals(taskGroup.technologies)) {
                return false;
            }
            this.portCount += taskGroup.portCount;
            this.deviceResources.addAll(deviceResources);
            return true;
        }
    }
}
