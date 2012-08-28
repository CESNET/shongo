package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.common.DateTimeSpecification;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.common.RelativeDateTimeSpecification;
import cz.cesnet.shongo.fault.*;
import cz.cesnet.shongo.fault.EntityNotFoundException;
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
     * Defines a maximum future to which the resource is schedulable (e.g., can be set as relative date/time which
     * means that resource can be always scheduled only e.g., to four month ahead).
     */
    private DateTimeSpecification maximumFuture;

    /**
     * Specifies whether resource can be scheduled by scheduler.
     */
    private boolean schedulable;

    /**
     * Allocations for the resource. Should be never accessed (used only for JPA query access which doesn't work
     * without association {@see http://stackoverflow.com/questions/2837255/jpa-outer-join-without-relation}).
     */
    @OneToMany(mappedBy = "resource")
    @Access(AccessType.FIELD)
    private List<AllocatedResource> allocations;

    /**
     * Constructor.
     */
    Resource()
    {
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
     */
    public <T extends Capability> T getCapability(Class<T> capabilityType)
    {
        for (Capability capability : capabilities) {
            if (capabilityType.isAssignableFrom(capability.getClass())) {
                return capabilityType.cast(capability);
            }
        }
        return null;
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
     * @param dateTime
     * @return true if resource is available at given {@code dateTime},
     *         false otherwise
     */
    public boolean isAvailableAt(DateTime dateTime, DateTime referenceDateTime)
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
     * @return {@link #schedulable}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isSchedulable()
    {
        return schedulable;
    }

    /**
     * @param schedulable sets the {@link #schedulable}
     */
    public void setSchedulable(boolean schedulable)
    {
        this.schedulable = schedulable;
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("name", getName());
        map.put("description", getDescription());
        addCollectionToMap(map, "capabilities", capabilities);
    }

    /**
     * @param domain
     * @return converted resource to API
     * @throws FaultException
     */
    public cz.cesnet.shongo.controller.api.Resource toApi(EntityManager entityManager, Domain domain)
    {
        final Resource resourceImpl = this;
        cz.cesnet.shongo.controller.api.Resource resource = new cz.cesnet.shongo.controller.api.Resource();

        resource.setIdentifier(domain.formatIdentifier(getId()));
        resource.setName(getName());
        resource.setSchedulable(isSchedulable());
        resource.setDescription(getDescription());

        DateTimeSpecification maxFuture = getMaximumFuture();
        if (maxFuture != null) {
            if (maxFuture instanceof AbsoluteDateTimeSpecification) {
                resource.setMaxFuture(((AbsoluteDateTimeSpecification) maxFuture).getDateTime());
            }
            else if (maxFuture instanceof RelativeDateTimeSpecification) {
                resource.setMaxFuture(((RelativeDateTimeSpecification) maxFuture).getDuration());
            }
            else {
                throw new TodoImplementException();
            }
        }

        Resource parentResource = getParentResource();
        if (parentResource != null) {
            resource.setParentIdentifier(domain.formatIdentifier(parentResource.getId()));
        }

        if (this instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) this;
            for (Technology technology : deviceResource.getTechnologies()) {
                resource.addTechnology(technology);
            }

            if (deviceResource.isManaged()) {
                ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
                resource.setMode(new cz.cesnet.shongo.controller.api.ManagedMode(managedMode.getConnectorAgentName()));
            }
            else {
                resource.setMode(cz.cesnet.shongo.controller.api.Resource.UNMANAGED_MODE);
            }
        }

        for (Capability capability : getCapabilities()) {
            resource.addCapability(capability.toApi());
        }

        for (Resource childResource : getChildResources()) {
            resource.addChildResourceIdentifier(domain.formatIdentifier(childResource.getId()));
        }

        return resource;
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
        if (api.getTechnologies().size() > 0) {
            resource = new DeviceResource();
        } else {
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
        if (api.isPropertyFilled(api.SCHEDULABLE)) {
            setSchedulable(api.getSchedulable());
        }
        if (api.isPropertyFilled(api.PARENT_RESOURCE_IDENTIFIER)) {
            Long newParentResourceId = null;
            if (api.getParentIdentifier() != null) {
                newParentResourceId = domain.parseIdentifier(api.getParentIdentifier());
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
        if (api.isPropertyFilled(api.MAX_FUTURE)) {
            Object maxFuture = api.getMaxFuture();
            if (maxFuture == null) {
                setMaximumFuture(null);
            }
            else if (maxFuture instanceof DateTime) {
                setMaximumFuture(new AbsoluteDateTimeSpecification((DateTime) maxFuture));
            }
            else if (maxFuture instanceof Period) {
                setMaximumFuture(new RelativeDateTimeSpecification((Period) maxFuture));
            }
            else {
                throw new TodoImplementException();
            }
        }

        if (this instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) this;
            // Create technologies
            for (Technology technology : api.getTechnologies()) {
                if (api.isCollectionItemMarkedAsNew(api.TECHNOLOGIES, technology)) {
                    deviceResource.addTechnology(technology);
                }
            }
            // Delete technologies
            Set<Technology> technologies = api.getCollectionItemsMarkedAsDeleted(api.TECHNOLOGIES);
            for (Technology technology : technologies) {
                deviceResource.removeTechnology(technology);
            }

            if (api.isPropertyFilled(api.MODE)) {
                Object mode = api.getMode();
                if (mode instanceof String) {
                    if (mode.equals(api.UNMANAGED_MODE)) {
                        deviceResource.setMode(null);
                    }
                    else {
                        throw new FaultException(CommonFault.CLASS_ATTRIBUTE_WRONG_VALUE,
                                api.MODE, api.getClass(), mode);
                    }
                }
                else if (mode instanceof cz.cesnet.shongo.controller.api.ManagedMode) {
                    ManagedMode managedMode;
                    if (deviceResource.isManaged()) {
                        managedMode = (ManagedMode) deviceResource.getMode();
                    }
                    else {
                        managedMode = new ManagedMode();
                        deviceResource.setMode(managedMode);
                    }
                    managedMode.setConnectorAgentName(
                            ((cz.cesnet.shongo.controller.api.ManagedMode) mode).getConnectorAgentName());
                }
                else {
                    throw new FaultException(CommonFault.CLASS_ATTRIBUTE_WRONG_VALUE,
                            api.MODE, api.getClass(), mode);
                }
            }
        }

        // Create/modify capabilities
        for (cz.cesnet.shongo.controller.api.Capability apiCapability : api.getCapabilities()) {
            if (api.isCollectionItemMarkedAsNew(api.CAPABILITIES, apiCapability)) {
                addCapability(Capability.createFromApi(apiCapability, entityManager));
            }
            else {
                Capability capability = getCapabilityById(apiCapability.getId().longValue());
                capability.fromApi(apiCapability, entityManager);
            }
        }
        // Delete capabilities
        Set<cz.cesnet.shongo.controller.api.Capability> apiDeletedCapabilities =
                api.getCollectionItemsMarkedAsDeleted(api.CAPABILITIES);
        for (cz.cesnet.shongo.controller.api.Capability apiCapability : apiDeletedCapabilities) {
            removeCapability(getCapabilityById(apiCapability.getId().longValue()));
        }
    }

    /**
     * Validate resource
     */
    public void validate() throws EntityValidationException
    {
        Set<Class<? extends Capability>> capabilityTypes = new HashSet<Class<? extends Capability>>();
        for (Capability capability : capabilities) {
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
