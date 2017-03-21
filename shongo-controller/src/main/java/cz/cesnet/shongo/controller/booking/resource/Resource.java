package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.Controller;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.datetime.DateTimeSpecification;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;
import cz.cesnet.shongo.report.ReportableComplex;
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
public class Resource extends PersistentObject implements ReportableComplex
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
     * List of additional administrator emails (basic administrators are specified by owners in
     * {@link cz.cesnet.shongo.controller.acl.AclEntry}s).
     */
    private List<String> administratorEmails = new ArrayList<String>();

    /**
     * Temporary {@link PersonInformation} for {@link #administratorEmails}.
     */
    private List<PersonInformation> tmpAdministrators;

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
     * Order in which the resource should be tried to be allocated ({@code null} means the last).
     */
    private Integer allocationOrder;

    /**
     * Are reservations of this resource public.
     */
    private boolean calendarPublic;

    /**
     * If all reservation request must be first confirmed by owner.
     */
    private boolean confirmByOwner;

    /**
     * Hash key used for public calendar URL
     */
    private String calendarUriKey;

    /**
     * Name of calendar on calendar server.
     */
    private String remoteCalendarName;

    /**
     * Constructor.
     */
    public Resource()
    {
    }

    @Id
    @SequenceGenerator(name = "resource_id", sequenceName = "resource_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "resource_id")
    @Override
    public Long getId()
    {
        return id;
    }

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false, length = Controller.USER_ID_COLUMN_LENGTH)
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
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
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
     * @return {@link #calendarPublic}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isCalendarPublic() {
        return calendarPublic;
    }

    public void setCalendarPublic(boolean calendarPublic) {
        this.calendarPublic = calendarPublic;
    }

    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isConfirmByOwner()
    {
        return confirmByOwner;
    }

    public void setConfirmByOwner(boolean confirmByOwner)
    {
        this.confirmByOwner = confirmByOwner;
    }

    @Column(columnDefinition = "varchar(8)", unique = true)
    public String getCalendarUriKey() {
        return calendarUriKey;
    }

    public void setCalendarUriKey(String calendarUriKey) {
        this.calendarUriKey = calendarUriKey;
    }

    @Column
    public String getRemoteCalendarName () {
        return remoteCalendarName;
    }

    public void setRemoteCalendarName (String calendarName) {
        this.remoteCalendarName = calendarName;
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
     * @return capability of given {@code capabilityType} if exists, null otherwise
     * @throws IllegalStateException when multiple capabilities of given {@code capabilityType} exists
     */
    public <T extends Capability> T getCapabilityRequired(Class<T> capabilityType)
    {
        T capability = getCapability(capabilityType);
        if (capability == null) {
            throw new RuntimeException("Resource '" + ObjectIdentifier.formatId(this)
                    + "' doesn't have required capability " + capabilityType.getSimpleName() + ".");
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
     * @return {@link #administratorEmails}
     */
    @ElementCollection
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    @Access(AccessType.FIELD)
    public List<String> getAdministratorEmails()
    {
        return Collections.unmodifiableList(administratorEmails);
    }

    /**
     * @param administratorEmail to be added to the {@link #administratorEmails}
     */
    public void addAdministratorEmail(String administratorEmail)
    {
        administratorEmails.add(administratorEmail);
        synchronized (this) {
            tmpAdministrators = null;
        }
    }

    /**
     * @param administratorEmail to be removed from the {@link #administratorEmails}
     */
    public void removeAdministratorEmail(String administratorEmail)
    {
        administratorEmails.remove(administratorEmail);
        synchronized (this) {
            tmpAdministrators = null;
        }
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

    /**
     * @return {@link #allocationOrder}
     */
    @Column(name = "allocation_order")
    public Integer getAllocationOrder()
    {
        return allocationOrder;
    }

    /**
     * @param allocationOrder sets the {@link #allocationOrder}
     */
    public void setAllocationOrder(Integer allocationOrder)
    {
        this.allocationOrder = allocationOrder;
    }

    @Override
    public void loadLazyProperties()
    {
        getAdministratorEmails().size();
        super.loadLazyProperties();
    }

    @Override
    @Transient
    public Map<String, Object> getReportDescription()
    {
        Map<String, Object> reportDescription = new HashMap<String, Object>();
        reportDescription.put("id", ObjectIdentifier.formatId(this));
        reportDescription.put("name", name);
        return reportDescription;
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
        resourceApi.setId(ObjectIdentifier.formatId(this));
        resourceApi.setUserId(getUserId());
        resourceApi.setName(getName());
        resourceApi.setAllocatable(isAllocatable());
        resourceApi.setAllocationOrder(getAllocationOrder());
        resourceApi.setDescription(getDescription());
        resourceApi.setCalendarPublic(isCalendarPublic());
        resourceApi.setCalendarUriKey(getCalendarUriKey());
        resourceApi.setRemoteCalendarName(getRemoteCalendarName());
        resourceApi.setConfirmByOwner(isConfirmByOwner());

        if (maximumFuture != null) {
            resourceApi.setMaximumFuture(maximumFuture.toApi());
        }

        Resource parentResource = getParentResource();
        if (parentResource != null) {
            resourceApi.setParentResourceId(ObjectIdentifier.formatId(parentResource));
        }

        for (Capability capability : getCapabilities()) {
            resourceApi.addCapability(capability.toApi());
        }

        for (String administratorEmail : getAdministratorEmails()) {
            resourceApi.addAdministratorEmail(administratorEmail);
        }

        for (Resource childResource : getChildResources()) {
            resourceApi.addChildResourceId(ObjectIdentifier.formatId(childResource));
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
        setAllocationOrder(resourceApi.getAllocationOrder());
        setCalendarPublic(resourceApi.isCalendarPublic());
        setCalendarUriKey(resourceApi.getCalendarUriKey());
        setRemoteCalendarName(resourceApi.getRemoteCalendarName());
        setConfirmByOwner(resourceApi.isConfirmByOwner());
        Long newParentResourceId = null;
        if (resourceApi.getParentResourceId() != null) {
            newParentResourceId = ObjectIdentifier.parseLocalId(resourceApi.getParentResourceId(), ObjectType.RESOURCE);
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
        administratorEmails.clear();
        administratorEmails.addAll(resourceApi.getAdministratorEmails());
    }

    /**
     * Validate resource.
     *
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectInvalidException
     *
     */
    public void validate() throws CommonReportSet.ObjectInvalidException
    {
        Set<Class<? extends Capability>> capabilityTypes = new HashSet<Class<? extends Capability>>();
        for (Capability capability : capabilities) {
            if (capability instanceof AliasProviderCapability) {
                continue;
            }
            for (Class<? extends Capability> capabilityType : capabilityTypes) {
                if (capabilityType.isAssignableFrom(capability.getClass())) {
                    throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                            "Resource cannot contain multiple '" + capabilityType.getSimpleName() + "'.");

                }
            }
            capabilityTypes.add(capability.getClass());
        }
    }

    /**
     * Get list of administrators to be notified.
     * The list is constructed from {@link Resource} owners and {@link #administratorEmails}.
     *
     * @param authorizationManager to be used for determining {@link Resource} owners
     * @return list of administrator {@link PersonInformation}s
     */
    public List<PersonInformation> getAdministrators(AuthorizationManager authorizationManager)
    {
        return getAdministrators(authorizationManager, true);
    }

    /**
     * Get list of administrators to be notified.
     * The list is constructed from {@link Resource} owners and {@link #administratorEmails}.
     *
     * @param authorizationManager to be used for determining {@link Resource} owners
     * @param owners include owners of the resource
     * @return list of administrator {@link PersonInformation}s
     */
    public List<PersonInformation> getAdministrators(AuthorizationManager authorizationManager, boolean owners)
    {
        EntityManager entityManager = authorizationManager.getEntityManager();
        Authorization authorization = authorizationManager.getAuthorization();
        UserSettingsManager userSettingsManager = new UserSettingsManager(entityManager, authorization);
        List<PersonInformation> administrators = new LinkedList<PersonInformation>();
        if (owners) {
            for (UserInformation administrator : authorizationManager.getUsersWithRole(this, ObjectRole.OWNER)) {
                UserSettings userSettings = userSettingsManager.getUserSettings(administrator.getUserId(), null);
                if (userSettings.isResourceAdministratorNotifications()) {
                    administrators.add(administrator);
                }
            }
        }
        synchronized (this) {
            if (tmpAdministrators == null) {
                tmpAdministrators = new LinkedList<PersonInformation>();
                for (String administratorEmail : administratorEmails) {
                    tmpAdministrators.add(new AnonymousAdministrator(administratorEmail));
                }
            }
            administrators.addAll(tmpAdministrators);
        }
        return administrators;
    }

    /**
     * Get list of administrators to be notified.
     * The list is constructed from {@link Resource} owners and {@link #administratorEmails}.
     *
     *
     * @param authorization to be used for determining {@link cz.cesnet.shongo.controller.booking.resource.Resource} owners
     * @return list of administrator {@link PersonInformation}s
     */
    public List<PersonInformation> getAdministrators(EntityManager entityManager, Authorization authorization)
    {
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        return getAdministrators(authorizationManager);
    }

    /**
     * Additional resource administrator.
     */
    public static class AnonymousAdministrator implements PersonInformation
    {
        private final String administratorEmail;

        public AnonymousAdministrator(String administratorEmail)
        {
            this.administratorEmail = administratorEmail;
        }

        @Override
        public String getFullName()
        {
            return "Administrator";
        }

        @Override
        public String getRootOrganization()
        {
            return null;
        }

        @Override
        public String getPrimaryEmail()
        {
            return administratorEmail;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AnonymousAdministrator that = (AnonymousAdministrator) o;

            if (!administratorEmail.equals(that.administratorEmail)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            return administratorEmail.hashCode();
        }

        @Override
        public String toString()
        {
            return "Administrator (" + administratorEmail + ")";
        }
    }
}
