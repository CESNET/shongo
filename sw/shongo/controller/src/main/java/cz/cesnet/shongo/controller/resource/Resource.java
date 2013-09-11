package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.common.DateTimeSpecification;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.report.AbstractReport;
import cz.cesnet.shongo.report.Reportable;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Resource extends PersistentObject implements Reportable
{
    /**
     * User-id of an user who created the {@link Resource}.
     */
    private String userId;

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
     * List of persons that are notified when the {@link Resource} is allocated or when are
     * encountered any technical issues.
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
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
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
     * @throws CommonReportSet.EntityNotFoundException
     *          when capability doesn't exist
     */
    public Capability getCapabilityById(Long id) throws CommonReportSet.EntityNotFoundException
    {
        for (Capability capability : capabilities) {
            if (capability.getId().equals(id)) {
                return capability;
            }
        }
        return ControllerReportSetHelper.throwEntityNotFoundFault(Capability.class, id);
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
     * @return capability of given {@code capabilityType} if exists, null otherwise
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
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Person> getAdministrators()
    {
        return administrators;
    }

    /**
     * @param id
     * @return administrator with given {@code id}
     * @throws CommonReportSet.EntityNotFoundException
     *          when administrator doesn't exist
     */
    public Person getAdministratorById(Long id) throws CommonReportSet.EntityNotFoundException
    {
        for (Person person : administrators) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return ControllerReportSetHelper.throwEntityNotFoundFault(Person.class, id);
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
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public DateTimeSpecification getMaximumFuture()
    {
        return maximumFuture;
    }

    /**
     * @param referenceDateTime reference date/time used e.g., as base date/time for relative date/time
     * @return {@link DateTime} representing a maximum future fot the {@link Resource}
     */
    public DateTime getMaximumFutureDateTime(DateTime referenceDateTime)
    {
        if (maximumFuture == null) {
            return null;
        }
        return maximumFuture.getEarliest(referenceDateTime);
    }

    /**
     * @param dateTime          date/time which is checked for availability
     * @param referenceDateTime reference date/time used e.g., as base date/time for relative date/time
     * @return true if resource is available at given {@code dateTime},
     *         false otherwise
     */
    public final boolean isAvailableInFuture(DateTime dateTime, DateTime referenceDateTime)
    {
        DateTime maxDateTime = getMaximumFutureDateTime(referenceDateTime);
        return maxDateTime == null || !dateTime.isAfter(maxDateTime);
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
    public void loadLazyProperties()
    {
        getAdministrators().size();
        super.loadLazyProperties();
    }

    @Override
    @Transient
    public String getReportDescription(AbstractReport.MessageType messageType)
    {
        return String.format("resource '%s'", EntityIdentifier.formatId(this));
    }

    /**
     * @return converted capability to API
     */
    public final cz.cesnet.shongo.controller.api.Resource toApi(EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.Resource api = createApi();
        toApi(api, entityManager);
        return api;
    }

    protected cz.cesnet.shongo.controller.api.Resource createApi()
    {
        return new cz.cesnet.shongo.controller.api.Resource();
    }

    /**
     * @return converted resource to API
     */
    protected void toApi(cz.cesnet.shongo.controller.api.Resource resourceApi, EntityManager entityManager)
    {
        resourceApi.setId(EntityIdentifier.formatId(this));
        resourceApi.setUserId(getUserId());
        resourceApi.setName(getName());
        resourceApi.setAllocatable(isAllocatable());
        resourceApi.setDescription(getDescription());

        if (maximumFuture != null) {
            resourceApi.setMaximumFuture(maximumFuture.toApi());
        }

        Resource parentResource = getParentResource();
        if (parentResource != null) {
            resourceApi.setParentResourceId(EntityIdentifier.formatId(parentResource));
        }

        for (Capability capability : getCapabilities()) {
            resourceApi.addCapability(capability.toApi());
        }

        for (Person person : getAdministrators()) {
            resourceApi.addAdministrator(person.toApi());
        }

        for (Resource childResource : getChildResources()) {
            resourceApi.addChildResourceId(EntityIdentifier.formatId(childResource));
        }
    }

    /**
     * @param resourceApi
     * @param entityManager
     * @return resource converted from API
     */
    public static Resource createFromApi(cz.cesnet.shongo.controller.api.Resource resourceApi,
            EntityManager entityManager)
    {
        Resource resource;
        if (resourceApi instanceof cz.cesnet.shongo.controller.api.DeviceResource) {
            resource = new DeviceResource();
        }
        else {
            resource = new Resource();
        }
        resource.fromApi(resourceApi, entityManager);
        return resource;
    }

    /**
     * Synchronize resource from API
     *
     * @param resourceApi
     * @param entityManager
     */
    public void fromApi(cz.cesnet.shongo.controller.api.Resource resourceApi, final EntityManager entityManager)
    {
        setName(resourceApi.getName());
        setDescription(resourceApi.getDescription());
        setAllocatable(resourceApi.getAllocatable());
        Long newParentResourceId = null;
        if (resourceApi.getParentResourceId() != null) {
            newParentResourceId = EntityIdentifier.parseId(
                    cz.cesnet.shongo.controller.resource.Resource.class, resourceApi.getParentResourceId());
        }
        Long oldParentResourceId = parentResource != null ? parentResource.getId() : null;
        if ((newParentResourceId == null && oldParentResourceId != null)
                || (newParentResourceId != null && !newParentResourceId.equals(oldParentResourceId))) {
            ResourceManager resourceManager = new ResourceManager(entityManager);
            Resource parentResource =
                    (newParentResourceId != null ? resourceManager.get(newParentResourceId) : null);
            setParentResource(parentResource);
        }
        Object maximumFuture = resourceApi.getMaximumFuture();
        if (maximumFuture == null) {
            setMaximumFuture(null);
        }
        else {
            setMaximumFuture(DateTimeSpecification.fromApi(maximumFuture, getMaximumFuture()));
        }

        Synchronization.synchronizeCollection(capabilities, resourceApi.getCapabilities(),
                new Synchronization.Handler<Capability, cz.cesnet.shongo.controller.api.Capability>(Capability.class)
                {
                    @Override
                    public void addToCollection(Collection<Capability> objects, Capability object)
                    {
                        addCapability(object);
                    }

                    @Override
                    public Capability createFromApi(cz.cesnet.shongo.controller.api.Capability objectApi)
                    {
                        return Capability.createFromApi(objectApi, entityManager);
                    }

                    @Override
                    public void updateFromApi(Capability object, cz.cesnet.shongo.controller.api.Capability objectApi)
                    {
                        object.fromApi(objectApi, entityManager);
                    }
                });
        Synchronization.synchronizeCollection(administrators, resourceApi.getAdministrators(),
                new Synchronization.Handler<Person, cz.cesnet.shongo.controller.api.Person>(Person.class)
                {
                    @Override
                    public Person createFromApi(cz.cesnet.shongo.controller.api.Person objectApi)
                    {
                        return Person.createFromApi(objectApi);
                    }

                    @Override
                    public void updateFromApi(Person object, cz.cesnet.shongo.controller.api.Person objectApi)
                    {
                        object.fromApi(objectApi);
                    }
                });
    }

    /**
     * Validate resource.
     *
     * @throws CommonReportSet.EntityInvalidException
     *
     */
    public void validate() throws CommonReportSet.EntityInvalidException
    {
        Set<Class<? extends Capability>> capabilityTypes = new HashSet<Class<? extends Capability>>();
        for (Capability capability : capabilities) {
            if (capability instanceof AliasProviderCapability) {
                continue;
            }
            for (Class<? extends Capability> capabilityType : capabilityTypes) {
                if (capabilityType.isAssignableFrom(capability.getClass())) {
                    throw new CommonReportSet.EntityInvalidException(getClass().getSimpleName(),
                            "Resource cannot contain multiple '" + capabilityType.getSimpleName() + "'.");

                }
            }
            capabilityTypes.add(capability.getClass());
        }
    }
}
