package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.Capability;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for {@link Resource}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceListRequest extends SortableListRequest<ResourceListRequest.Sort>
{
    /**
     * Id of resources.
     */
    private Set<String> resourceIds = new HashSet<String>();

    /**
     * Id of resource tag.
     */
    private String tagId;

    /**
     * User-ids of resource owners.
     */
    private Set<String> userIds = new HashSet<String>();

    /**
     * Name of the resource.
     */
    private String name;

    /**
     * {@link Capability}s from which the resources must have at least one.
     */
    private Set<Class<? extends Capability>> capabilityClasses = new HashSet<Class<? extends Capability>>();

    /**
     * Set of {@link Technology}s which the resources must support.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Resource must be allocatable.
     */
    private boolean allocatable;

    /**
     * Constructor.
     */
    public ResourceListRequest()
    {
        super(Sort.class);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public ResourceListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    /**
     * @return {@link #resourceIds}
     */
    public Set<String> getResourceIds()
    {
        return resourceIds;
    }

    /**
     * @param resourceIds sets the {@link #resourceIds}
     */
    public void setResourceIds(Set<String> resourceIds)
    {
        this.resourceIds.clear();
        this.resourceIds.addAll(resourceIds);
    }

    /**
     * @param resourceId to be added to the {@link #resourceIds}
     */
    public void addResourceId(String resourceId)
    {
        this.resourceIds.add(resourceId);
    }

    /**
     * @return {@link #userIds}
     */
    public Set<String> getUserIds()
    {
        return userIds;
    }

    /**
     * @param userId to be added to the {@link #userIds}
     */
    public void addUserId(String userId)
    {
        userIds.add(userId);
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #capabilityClasses}
     */
    public Set<Class<? extends Capability>> getCapabilityClasses()
    {
        return capabilityClasses;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    /**
     * @param capabilityClasses sets the {@link #capabilityClasses}
     */
    public void setCapabilityClasses(Set<Class<? extends Capability>> capabilityClasses)
    {
        this.capabilityClasses.clear();
        this.capabilityClasses.addAll(capabilityClasses);
    }

    /**
     * @param capabilityClass to be added to the {@link #capabilityClasses}
     */
    public void addCapabilityClass(Class<? extends Capability> capabilityClass)
    {
        this.capabilityClasses.add(capabilityClass);
    }

    /**
     * @return {@link #technologies}
     */
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies.clear();
        this.technologies.addAll(technologies);
    }

    /**
     * @param technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @return {@link #allocatable}
     */
    public boolean isAllocatable()
    {
        return allocatable;
    }

    /**
     * @param allocatable sets the {@link #allocatable}
     */
    public void setAllocatable(boolean allocatable)
    {
        this.allocatable = allocatable;
    }

    public static enum Sort
    {
        ID,
        NAME
    }

    private static final String RESOURCE_IDS = "resourceIds";
    private static final String TAG_ID = "tagId";
    private static final String USER_IDS = "userIds";
    private static final String NAME = "name";
    private static final String CAPABILITY_CLASSES = "capabilityClasses";
    private static final String TECHNOLOGIES = "technologies";
    private static final String ALLOCATABLE = "allocatable";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_IDS, resourceIds);
        dataMap.set(TAG_ID,tagId);
        dataMap.set(USER_IDS, userIds);
        dataMap.set(NAME, name);
        dataMap.set(CAPABILITY_CLASSES, capabilityClasses);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(ALLOCATABLE, allocatable);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceIds = dataMap.getSet(RESOURCE_IDS, String.class);
        tagId = dataMap.getString(TAG_ID);
        userIds = dataMap.getSet(USER_IDS, String.class);
        name = dataMap.getString(NAME);
        capabilityClasses = dataMap.getClassSet(CAPABILITY_CLASSES, Capability.class);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        allocatable = dataMap.getBool(ALLOCATABLE);
    }
}
