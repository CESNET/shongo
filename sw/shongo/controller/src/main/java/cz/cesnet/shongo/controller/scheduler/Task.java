package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ResourceDatabase;
import cz.cesnet.shongo.controller.request.ExistingResourceSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.request.LookupResourceSpecification;
import cz.cesnet.shongo.controller.request.ResourceSpecification;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a scheduler task for allocating resources.
 */
public class Task
{
    /**
     * List of resource specification.
     */
    private List<ResourceSpecification> resourceSpecifications = new ArrayList<ResourceSpecification>();

    /**
     * List of {@link InterconnectableGroup}.
     */
    private List<InterconnectableGroup> interconnectableGroups = new ArrayList<InterconnectableGroup>();

    /**
     * Set of resources which should be allocated.
     */
    private Set<Resource> resources = new HashSet<Resource>();

    /**
     * @return {@link #interconnectableGroups}
     */
    public List<InterconnectableGroup> getInterconnectableGroups()
    {
        return interconnectableGroups;
    }

    /**
     * @param interconnectableGroup to be added to the {@link #interconnectableGroups}
     */
    public void addInterconnectableGroup(InterconnectableGroup interconnectableGroup)
    {
        interconnectableGroups.add(interconnectableGroup);
    }

    /**
     * @param resourceSpecification
     */
    public void addResourceSpecification(ResourceSpecification resourceSpecification)
    {
        resourceSpecifications.add(resourceSpecification);
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
     * Fill {@link #interconnectableGroups} from {@link #resourceSpecifications}.
     *
     * @param requestedSlot
     * @param entityManager
     * @param resourceDatabase
     * @throws FaultException
     */
    public void fillInterconnectableGroups(Interval requestedSlot, EntityManager entityManager,
            ResourceDatabase resourceDatabase) throws FaultException
    {
        // Create list of requested resources
        List<ResourceSpecification> resourceSpecifications = new ArrayList<ResourceSpecification>();
        for (ResourceSpecification resource : this.resourceSpecifications) {
            resourceSpecifications.add(resource);
        }

        try {
            // First process all external endpoint and existing resources
            for (Iterator<ResourceSpecification> iterator = resourceSpecifications.iterator();
                 iterator.hasNext(); ) {
                ResourceSpecification resourceSpecification = iterator.next();
                if (resourceSpecification instanceof ExternalEndpointSpecification) {
                    ExternalEndpointSpecification externalEndpoint = (ExternalEndpointSpecification) resourceSpecification;
                    interconnectableGroups.add(new InterconnectableGroup(externalEndpoint));
                    iterator.remove();
                }
                else if (resourceSpecification instanceof ExistingResourceSpecification) {
                    ExistingResourceSpecification existingResource = (ExistingResourceSpecification) resourceSpecification;
                    Resource resource = existingResource.getResource();
                    if (hasResource(resource)) {
                        // Same resource is requested multiple times
                        throw new FaultException("Resource is requested multiple times in specified time slot:\n"
                                + "  Resource: %s",
                                resource.getId().toString());
                    }
                    if (!resource.isSchedulable()) {
                        // Requested resource cannot be allocated
                        throw new FaultException("Requested resource cannot be allocated (schedulable = false):\n"
                                + "  Resource: %s",
                                resource.getId().toString());
                    }
                    if (!resourceDatabase.isResourceAvailable(resource, requestedSlot)) {
                        // Requested resource is not available in requested slot
                        throw new FaultException("Requested resource is not available in specified time slot:\n"
                                + " Time Slot: %s\n"
                                + "  Resource: %s",
                                TemporalHelper.formatInterval(requestedSlot),
                                resource.getId().toString());
                    }
                    resources.add(resource);
                    interconnectableGroups.add(new InterconnectableGroup(resource));
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
                        if (hasResource(possibleDeviceResource)) {
                            continue;
                        }
                        deviceResource = possibleDeviceResource;
                        break;
                    }

                    // If some was found
                    if (deviceResource != null) {
                        resources.add(deviceResource);
                        interconnectableGroups.add(new InterconnectableGroup(deviceResource));
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
                                        + " Technologies: %s",
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
        catch (InterconnectableGroup.DeviceResourceIsNotTerminalException exception) {
            throw new FaultException("Requested resource is not terminal:\n"
                    + "  Resource: %s",
                    exception.getResource().getId().toString());
        }
    }

    /**
     * Merge compatible {@link #interconnectableGroups}
     */
    public void mergeInterconnectableGroups()
    {
        // TODO: Implement better merging, each to each and select the best result

        for (int index = 0; index < interconnectableGroups.size(); index++) {
            InterconnectableGroup taskItem = interconnectableGroups.get(index);
            for (int mergeIndex = index + 1; mergeIndex < interconnectableGroups.size(); mergeIndex++) {
                InterconnectableGroup mergeTaskItem = interconnectableGroups.get(mergeIndex);
                if (taskItem.merge(mergeTaskItem)) {
                    interconnectableGroups.remove(mergeIndex);
                    mergeIndex--;
                }
            }
        }
    }

    /**
     * Recursive implementation of {@link #getInterconnectingTechnologies}.
     * Each recursive level process single {@link InterconnectableGroup}.
     *
     * @param currentTechnologies        current (incomplete) variant
     * @param interconnectableGroupIndex specifies recursive level
     * @param result                     result
     */
    private void getInterconnectingTechnologies(Set<Technology> currentTechnologies, int interconnectableGroupIndex,
            Set<Set<Technology>> result)
    {
        // Stop recursion
        if (interconnectableGroupIndex < 0) {
            // Finally remove all technologies which are not needed
            for (Iterator<Technology> iterator = currentTechnologies.iterator(); iterator.hasNext(); ) {
                Technology possibleTechnology = iterator.next();
                // Technology is not needed when each group is connected also by another technology
                for (InterconnectableGroup interconnectableGroup : interconnectableGroups) {
                    boolean connectedAlsoByAnotherTechnology = false;
                    for (Technology technology : interconnectableGroup.getTechnologies()) {
                        if (technology.equals(possibleTechnology)) {
                            continue;
                        }
                        if (currentTechnologies.contains(technology)) {
                            connectedAlsoByAnotherTechnology = true;
                            break;
                        }
                    }
                    // Group is connected only by this technology and thus it cannot be removed
                    if (!connectedAlsoByAnotherTechnology) {
                        possibleTechnology = null;
                        break;
                    }
                }
                // All groups are connected also  by another technology so we can remove possible technology
                if (possibleTechnology != null) {
                    iterator.remove();
                }
            }
            result.add(currentTechnologies);
            return;
        }

        // Get current group in recursion
        InterconnectableGroup interconnectableGroup = interconnectableGroups.get(interconnectableGroupIndex);
        // Build all variants of technology set for current group and call next recursive level
        for (Technology technology : interconnectableGroup.getTechnologies()) {
            // Build new instance of technologies
            Set<Technology> newTechnologies = new HashSet<Technology>();
            newTechnologies.addAll(currentTechnologies);
            // Add new technology
            newTechnologies.add(technology);
            // Call next recursive level
            getInterconnectingTechnologies(newTechnologies, interconnectableGroupIndex - 1, result);
        }
    }

    /**
     * @return all variants of technologies where each variant interconnects all groups
     */
    public Set<Set<Technology>> getInterconnectingTechnologies()
    {
        Set<Set<Technology>> technologiesSet = new HashSet<Set<Technology>>();
        getInterconnectingTechnologies(new HashSet<Technology>(), interconnectableGroups.size() - 1,
                technologiesSet);
        return technologiesSet;
    }

    /**
     * @return sum of port count from all {@link InterconnectableGroup}s
     */
    public int getTotalPortCount()
    {
        int portCount = 0;
        for (InterconnectableGroup interconnectableGroup : interconnectableGroups) {
            portCount += interconnectableGroup.getPortCount();
        }
        return portCount;
    }

}
