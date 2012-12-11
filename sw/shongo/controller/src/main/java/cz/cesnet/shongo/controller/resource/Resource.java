package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.common.DateTimeSpecification;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.common.RelativeDateTimeSpecification;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.EntityValidationException;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Resource extends PersistentObject
{
    /**
     * User-id of an user who is owner of the {@link Resource}.
     */
    private Long userId;

    /**
     * Name of a resource that is visible to users.
     */
    private String name;

    /**
     * Description for a resource.
     */
    private String description;

    /**
     * List of capabilities.
     */
    private List<Capability> capabilities = new ArrayList<Capability>();

    /**
     * Parent resource in which is this resource contained (e.g., endpoint can be located
     * in parent physical room resource).
     */
    private Resource parentResource;

    /**
     * List of child resources (e.g., physical room can contain some videoconferencing equipment).
     */
    private List<Resource> childResources = new ArrayList<Resource>();

    /**
     * List of persons that are contacted when are encountered any technical issues.
     */
    private List<Person> administrators = new ArrayList<Person>();

    /**
     * Defines a maximum future to which the resource is allocatable (e.g., can be set as relative date/time which
     * means that resource can be always scheduled only e.g., to four month ahead).
     */
    private DateTimeSpecification maximumFuture;

    /**
     * Specifies whether resource can be allocated by a scheduler.
     */
    private boolean allocatable;

    /**
     * Constructor.
     */
    public Resource()
    {
    }

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false)
    public Long getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #name}
     */
    @Column
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
     * @return {@link #description}
     */
    @Column
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return {@link #capabilities}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "resource", orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<Capability> getCapabilities()
    {
        return Collections.unmodifiableList(capabilities);
    }

    /**
     * @param id
     * @return capability with given {@code id}
     * @throws EntityNotFoundException when capability doesn't exist
     */
    public Capability getCapabilityById(Long id) throws EntityNotFoundException
    {
        for (Capability capability : capabilities) {
            if (capability.getId().equals(id)) {
                return capability;
            }
        }
        throw new EntityNotFoundException(Capability.class, id);
    }

    /**
     * @param capabilityType
     * @return true whether resource has capability of given {@code capabilityType},
     *         false otherwise
     */
    public boolean hasCapability(Class<? extends Capability> capabilityType)
    {
        return getCapability(capabilityType) != null;
    }

    /**
     * @param capabilityType
     * @return capability of given {@code capabilityType} if exits, null otherwise
     * @throws IllegalStateException when multiple capabilities of given {@code capabilityType} exists
     */
    public <T extends Capability> T getCapability(Class<T> capabilityType)
    {
        T capability = null;
        for (Capability possibleCapability : capabilities) {
            if (capabilityType.isAssignableFrom(possibleCapability.getClass())) {
                if (capability != null) {
                    throw new IllegalStateException("Multiple capabilities of type '" + capabilityType.getSimpleName()
                            + "' exists in resource with id '" + getId().toString() + "'.");
                }
                capability = capabilityType.cast(possibleCapability);
            }
        }
        return capability;
    }

    /**
     * @param capabilityType
     * @return list of capabilities with given {@code capabilityType}
     */
    public <T extends Capability> List<T> getCapabilities(Class<T> capabilityType)
    {
        List<T> capabilities = new ArrayList<T>();
        for (Capability possibleCapability : this.capabilities) {
            if (capabilityType.isAssignableFrom(possibleCapability.getClass())) {
                capabilities.add(capabilityType.cast(possibleCapability));
            }
        }
        return capabilities;
    }

    /**
     * @param capability capability to be added to the {@link #capabilities}
     */
    public void addCapability(Capability capability)
    {
        // Manage bidirectional association
        if (capabilities.contains(capability) == false) {
            capabilities.add(capability);
            capability.setResource(this);
        }
    }

    /**
     * @param capability capability to be removed from the {@link #capabilities}
     */
    public void removeCapability(Capability capability)
    {
        // Manage bidirectional association
        if (capabilities.contains(capability)) {
            capabilities.remove(capability);
            capability.setResource(null);
        }
    }

    /**
     * @return {@link #parentResource}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Resource getParentResource()
    {
        return parentResource;
    }

    /**
     * @param parentResource sets the {@link #parentResource}
     */
    public void setParentResource(Resource parentResource)
    {
        // Manage bidirectional association
        if (parentResource != this.parentResource) {
            if (this.parentResource != null) {
                Resource oldParentResource = this.parentResource;
                this.parentResource = null;
                oldParentResource.removeChildResource(this);
            }
            if (parentResource != null) {
                this.parentResource = parentResource;
                this.parentResource.addChildResource(this);
            }
        }
    }

    /**
     * @return {@link #childResources}
     */
    @OneToMany(mappedBy = "parentResource")
    @Access(AccessType.FIELD)
    public List<Resource> getChildResources()
    {
        return childResources;
    }

    /**
     * @param resource resource to be added to the {@link #childResources}
     */
    public void addChildResource(Resource resource)
    {
        // Manage bidirectional association
        if (childResources.contains(resource) == false) {
            childResources.add(resource);
            resource.setParentResource(this);
        }
    }

    /**
     * @param resource resource to be removed from the {@link #childResources}
     */
    public void removeChildResource(Resource resource)
    {
        // Manage bidirectional association
        if (childResources.contains(resource)) {
            childResources.remove(resource);
            resource.setParentResource(null);
        }
    }

    /**
     * @return {@link #administrators}
     */
    @OneToMany
    @Access(AccessType.FIELD)
    public List<Person> getAdministrators()
    {
        return administrators;
    }

    /**
     * @param person person to be added to the {@link #administrators}
     */
    public void addAdministrator(Person person)
    {
        administrators.add(person);
    }

    /**
     * @param person person to be removed from the {@link #administrators}
     */
    public void removeAdministrator(Person person)
    {
        administrators.remove(person);
    }

    /**
     * @return {@link #maximumFuture}
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public DateTimeSpecification getMaximumFuture()
    {
        return maximumFuture;
    }

    /**
     * @param dateTime          date/time which is checked for availability
     * @param referenceDateTime reference date/time used e.g., as base date/time for relative date/time
     * @return true if resource is available at given {@code dateTime},
     *         false otherwise
     */
    public boolean isAvailableInFuture(DateTime dateTime, DateTime referenceDateTime)
    {
        if (maximumFuture == null) {
            return true;
        }
        DateTime earliestDateTime = maximumFuture.getEarliest(referenceDateTime);
        return !dateTime.isAfter(earliestDateTime);
    }

    /**
     * @param maximumFuture sets the {@link #maximumFuture}
     */
    public void setMaximumFuture(DateTimeSpecification maximumFuture)
    {
        this.maximumFuture = maximumFuture;
    }

    /**
     * @return {@link #allocatable}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
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

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("name", getName());
        map.put("description", getDescription());
        map.put("capabilities", capabilities);
    }

    /**
     * @return converted capability to API
     * @throws FaultException
     */
    public final cz.cesnet.shongo.controller.api.Resource toApi(EntityManager entityManager, Domain domain)
    {
        cz.cesnet.shongo.controller.api.Resource api = createApi();
        toApi(api, entityManager, domain);
        return api;
    }

    protected cz.cesnet.shongo.controller.api.Resource createApi()
    {
        return new cz.cesnet.shongo.controller.api.Resource();
    }

    /**
     * @param domain
     * @return converted resource to API
     * @throws FaultException
     */
    protected void toApi(cz.cesnet.shongo.controller.api.Resource resource, EntityManager entityManager, Domain domain)
    {
        resource.setId(domain.formatId(getId()));
        resource.setUserId(getUserId().intValue());
        resource.setName(getName());
        resource.setAllocatable(isAllocatable());
        resource.setDescription(getDescription());

        DateTimeSpecification maximumFuture = getMaximumFuture();
        if (maximumFuture != null) {
            if (maximumFuture instanceof AbsoluteDateTimeSpecification) {
                resource.setMaximumFuture(((AbsoluteDateTimeSpecification) maximumFuture).getDateTime());
            }
            else if (maximumFuture instanceof RelativeDateTimeSpecification) {
                resource.setMaximumFuture(((RelativeDateTimeSpecification) maximumFuture).getDuration());
            }
            else {
                throw new TodoImplementException();
            }
        }

        Resource parentResource = getParentResource();
        if (parentResource != null) {
            resource.setParentResourceId(domain.formatId(parentResource.getId()));
        }

        for (Capability capability : getCapabilities()) {
            resource.addCapability(capability.toApi());
        }

        for (Resource childResource : getChildResources()) {
            resource.addChildResourceId(domain.formatId(childResource.getId()));
        }
    }

    /**
     * @param api
     * @param entityManager
     * @param domain
     * @return resource converted from API
     */
    public static Resource createFromApi(cz.cesnet.shongo.controller.api.Resource api, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        Resource resource;
        if (api instanceof cz.cesnet.shongo.controller.api.DeviceResource) {
            resource = new DeviceResource();
        }
        else {
            resource = new Resource();
        }
        resource.fromApi(api, entityManager, domain);
        return resource;
    }

    /**
     * Synchronize resource from API
     *
     * @param api
     * @param entityManager
     * @throws FaultException
     */
    public void fromApi(cz.cesnet.shongo.controller.api.Resource api, EntityManager entityManager, Domain domain)
            throws FaultException
    {
        // Modify attributes
        if (api.isPropertyFilled(api.NAME)) {
            setName(api.getName());
        }
        if (api.isPropertyFilled(api.DESCRIPTION)) {
            setDescription(api.getDescription());
        }
        if (api.isPropertyFilled(api.ALLOCATABLE)) {
            setAllocatable(api.getAllocatable());
        }
        if (api.isPropertyFilled(api.PARENT_RESOURCE_ID)) {
            Long newParentResourceId = null;
            if (api.getParentResourceId() != null) {
                newParentResourceId = domain.parseId(api.getParentResourceId());
            }
            Long oldParentResourceId = parentResource != null ? parentResource.getId() : null;
            if ((newParentResourceId == null && oldParentResourceId != null)
                    || (newParentResourceId != null && !newParentResourceId.equals(oldParentResourceId))) {
                ResourceManager resourceManager = new ResourceManager(entityManager);
                Resource parentResource =
                        (newParentResourceId != null ? resourceManager.get(newParentResourceId) : null);
                setParentResource(parentResource);
            }
        }
        if (api.isPropertyFilled(api.MAXIMUM_FUTURE)) {
            Object maximumFuture = api.getMaximumFuture();
            if (maximumFuture == null) {
                setMaximumFuture(null);
            }
            else if (maximumFuture instanceof DateTime) {
                setMaximumFuture(new AbsoluteDateTimeSpecification((DateTime) maximumFuture));
            }
            else if (maximumFuture instanceof Period) {
                setMaximumFuture(new RelativeDateTimeSpecification((Period) maximumFuture));
            }
            else {
                throw new TodoImplementException();
            }
        }

        // Create/modify capabilities
        for (cz.cesnet.shongo.controller.api.Capability apiCapability : api.getCapabilities()) {
            if (api.isPropertyItemMarkedAsNew(api.CAPABILITIES, apiCapability)) {
                addCapability(Capability.createFromApi(apiCapability, entityManager));
            }
            else {
                Capability capability = getCapabilityById(apiCapability.notNullIdAsLong());
                capability.fromApi(apiCapability, entityManager);
            }
        }
        // Delete capabilities
        Set<cz.cesnet.shongo.controller.api.Capability> apiDeletedCapabilities =
                api.getPropertyItemsMarkedAsDeleted(api.CAPABILITIES);
        for (cz.cesnet.shongo.controller.api.Capability apiCapability : apiDeletedCapabilities) {
            removeCapability(getCapabilityById(apiCapability.notNullIdAsLong()));
        }
    }

    /**
     * Validate resource
     */
    public void validate() throws EntityValidationException
    {
        Set<Class<? extends Capability>> capabilityTypes = new HashSet<Class<? extends Capability>>();
        for (Capability capability : capabilities) {
            if (capability instanceof AliasProviderCapability) {
                continue;
            }
            for (Class<? extends Capability> capabilityType : capabilityTypes) {
                if (capabilityType.isAssignableFrom(capability.getClass())) {
                    throw new EntityValidationException(getClass(), getId(), "Resource cannot contain multiple '"
                            + capabilityType.getSimpleName() + "'.");

                }
            }
            capabilityTypes.add(capability.getClass());
        }
    }
}
