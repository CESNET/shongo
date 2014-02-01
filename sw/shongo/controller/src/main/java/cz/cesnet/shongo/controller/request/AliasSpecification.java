package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.common.AbstractParticipant;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link Specification} for an {@link Alias}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasSpecification extends Specification
        implements ReservationTaskProvider, ObjectHelper.SameCheckable
{
    /**
     * Restricts {@link AliasType} for allocation of {@link Alias}.
     */
    private Set<AliasType> aliasTypes = new HashSet<AliasType>();

    /**
     * Restricts {@link Technology} for allocation of {@link Alias}.
     */
    private Set<Technology> aliasTechnologies = new HashSet<Technology>();

    /**
     * Requested {@link String} value for the {@link Alias}.
     */
    private String value;

    /**
     * {@link AliasProviderCapability} from which the {@link Alias} should be allocated.
     */
    private AliasProviderCapability aliasProviderCapability;

    /**
     * Specifies whether the {@link Alias} should represent a permanent room (should get allocated {@link ResourceRoomEndpoint}).
     */
    private boolean permanentRoom;

    /**
     * List of {@link cz.cesnet.shongo.controller.common.AbstractParticipant}s for the permanent room.
     */
    private List<AbstractParticipant> permanentRoomParticipants = new LinkedList<AbstractParticipant>();

    /**
     * Constructor.
     */
    public AliasSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param aliasType to be added to the {@link #aliasTypes}
     */
    public AliasSpecification(AliasType aliasType)
    {
        addAliasType(aliasType);
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #technologies}
     * @param aliasType  to be added to the {@link #aliasTypes}
     */
    public AliasSpecification(Technology technology, AliasType aliasType)
    {
        addAliasTechnology(technology);
        addAliasType(aliasType);
    }

    /**
     * Constructor.
     *
     * @param technology              to be added to the {@link #technologies}
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public AliasSpecification(Technology technology, AliasProviderCapability aliasProviderCapability)
    {
        addAliasTechnology(technology);
        setAliasProviderCapability(aliasProviderCapability);
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #technologies}
     */
    public AliasSpecification(Technology technology)
    {
        addAliasTechnology(technology);
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<AliasType> getAliasTypes()
    {
        return Collections.unmodifiableSet(aliasTypes);
    }

    /**
     * @param aliasType
     * @return true if the {@link #aliasTypes} contains the given {@code aliasType},
     *         false otherwise
     */
    public boolean hasAliasType(AliasType aliasType)
    {
        return aliasTypes.contains(aliasType);
    }

    /**
     * @param aliasTypes sets the {@link #aliasTypes}
     */
    public void setAliasTypes(Set<AliasType> aliasTypes)
    {
        this.aliasTypes.clear();
        this.aliasTypes.addAll(aliasTypes);
    }

    /**
     * @param aliasType to be added to the {@link #aliasTypes}
     */
    public void addAliasType(AliasType aliasType)
    {
        aliasTypes.add(aliasType);
    }

    /**
     * @param aliasType to be removed from the {@link #aliasTypes}
     */
    public void removeAliasType(AliasType aliasType)
    {
        aliasTypes.remove(aliasType);
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getAliasTechnologies()
    {
        return Collections.unmodifiableSet(aliasTechnologies);
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setAliasTechnologies(Set<Technology> technologies)
    {
        this.aliasTechnologies.clear();
        this.aliasTechnologies.addAll(technologies);
    }

    /**
     * @param technology technology to be added to the set of technologies that the device support.
     */
    public void addAliasTechnology(Technology technology)
    {
        aliasTechnologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeAliasTechnology(Technology technology)
    {
        aliasTechnologies.remove(technology);
    }

    /**
     * @return {@link #value}
     */
    @Column
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return {@link #aliasProviderCapability}
     */
    @OneToOne
    public AliasProviderCapability getAliasProviderCapability()
    {
        return aliasProviderCapability;
    }

    /**
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public void setAliasProviderCapability(AliasProviderCapability aliasProviderCapability)
    {
        this.aliasProviderCapability = aliasProviderCapability;
    }

    /**
     * @return {@link #permanentRoom}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isPermanentRoom()
    {
        return permanentRoom;
    }

    /**
     * @param permanentRoom sets the {@link #permanentRoom}
     */
    public void setPermanentRoom(boolean permanentRoom)
    {
        this.permanentRoom = permanentRoom;
    }

    /**
     * @return {@link #permanentRoomParticipants}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<AbstractParticipant> getPermanentRoomParticipants()
    {
        return Collections.unmodifiableList(permanentRoomParticipants);
    }

    /**
     * @param participants sets the {@link #permanentRoomParticipants}
     */
    public void setPermanentRoomParticipants(List<AbstractParticipant> participants)
    {
        this.permanentRoomParticipants.clear();
        for (AbstractParticipant participant : participants) {
            this.permanentRoomParticipants.add(participant.clone());
        }
    }

    @Override
    public void updateTechnologies()
    {
        clearTechnologies();
        addTechnologies(aliasTechnologies);
        for (AliasType aliasType : aliasTypes) {
            Technology technology = aliasType.getTechnology();
            if (technology.equals(Technology.ALL)) {
                continue;
            }
            addTechnology(technology);
        }
    }

    @Override
    public AliasSpecification clone()
    {
        return (AliasSpecification) super.clone();
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        AliasSpecification aliasSpecification = (AliasSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectHelper.isSame(getAliasTechnologies(), aliasSpecification.getAliasTechnologies())
                || !ObjectHelper.isSame(getAliasTypes(), aliasSpecification.getAliasTypes())
                || !ObjectHelper.isSame(getValue(), aliasSpecification.getValue())
                || !ObjectHelper.isSamePersistent(getAliasProviderCapability(),
                aliasSpecification.getAliasProviderCapability())
                || !ObjectHelper.isSame(isPermanentRoom(), aliasSpecification.isPermanentRoom());

        setAliasTechnologies(aliasSpecification.getAliasTechnologies());
        setAliasTypes(aliasSpecification.getAliasTypes());
        setValue(aliasSpecification.getValue());
        setAliasProviderCapability(aliasSpecification.getAliasProviderCapability());
        setPermanentRoom(aliasSpecification.isPermanentRoom());

        if (!ObjectHelper.isSame(permanentRoomParticipants, aliasSpecification.getPermanentRoomParticipants())) {
            setPermanentRoomParticipants(aliasSpecification.getPermanentRoomParticipants());
            modified = true;
        }

        return modified;
    }

    @Override
    public AliasReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        AliasReservationTask aliasReservationTask = new AliasReservationTask(schedulerContext);
        for (Technology technology : getAliasTechnologies()) {
            aliasReservationTask.addTechnology(technology);
        }
        for (AliasType aliasType : getAliasTypes()) {
            aliasReservationTask.addAliasType(aliasType);
        }
        aliasReservationTask.setRequestedValue(value);
        if (aliasProviderCapability != null) {
            aliasReservationTask.addAliasProviderCapability(aliasProviderCapability);
        }
        aliasReservationTask.setPermanentRoomParticipants(getPermanentRoomParticipants());
        aliasReservationTask.setPermanentRoom(isPermanentRoom());
        return aliasReservationTask;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.AliasSpecification();
    }

    @Override
    public cz.cesnet.shongo.controller.api.AliasSpecification toApi()
    {
        return (cz.cesnet.shongo.controller.api.AliasSpecification) super.toApi();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSpecification) specificationApi;
        for (Technology technology : getAliasTechnologies()) {
            aliasSpecificationApi.addTechnology(technology);
        }
        for (AliasType aliasType : getAliasTypes()) {
            aliasSpecificationApi.addAliasType(aliasType);
        }
        aliasSpecificationApi.setValue(getValue());
        if (getAliasProviderCapability() != null) {
            aliasSpecificationApi.setResourceId(
                    EntityIdentifier.formatId(getAliasProviderCapability().getResource()));
        }
        for (AbstractParticipant participant : getPermanentRoomParticipants()) {
            aliasSpecificationApi.addPermanentRoomParticipant(participant.toApi());
        }
        aliasSpecificationApi.setPermanentRoom(isPermanentRoom());
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, final EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSpecification) specificationApi;

        setValue(aliasSpecificationApi.getValue());

        if (aliasSpecificationApi.getResourceId() == null) {
            setAliasProviderCapability(null);
        }
        else {
            Long resourceId = EntityIdentifier.parseId(
                    cz.cesnet.shongo.controller.resource.Resource.class, aliasSpecificationApi.getResourceId());
            ResourceManager resourceManager = new ResourceManager(entityManager);
            Resource resource = resourceManager.get(resourceId);
            AliasProviderCapability aliasProviderCapability = resource.getCapability(AliasProviderCapability.class);
            if (aliasProviderCapability == null) {
                throw new RuntimeException(String.format("Resource '%s' doesn't have %s.",
                        AliasProviderCapability.class.getSimpleName(), aliasSpecificationApi.getResourceId()));
            }
            setAliasProviderCapability(aliasProviderCapability);
        }
        setPermanentRoom(aliasSpecificationApi.isPermanentRoom());

        Synchronization.synchronizeCollection(aliasTechnologies, aliasSpecificationApi.getTechnologies());
        Synchronization.synchronizeCollection(aliasTypes, aliasSpecificationApi.getAliasTypes());
        Synchronization.synchronizeCollection(
                permanentRoomParticipants, aliasSpecificationApi.getPermanentRoomParticipants(),
                new Synchronization.Handler<AbstractParticipant, cz.cesnet.shongo.controller.api.AbstractParticipant>(
                        AbstractParticipant.class)
                {
                    @Override
                    public AbstractParticipant createFromApi(
                            cz.cesnet.shongo.controller.api.AbstractParticipant objectApi)
                    {
                        return AbstractParticipant.createFromApi(objectApi, entityManager);
                    }

                    @Override
                    public void updateFromApi(AbstractParticipant object,
                            cz.cesnet.shongo.controller.api.AbstractParticipant objectApi)
                    {
                        object.fromApi(objectApi, entityManager);
                    }
                });

        super.fromApi(specificationApi, entityManager);
    }

    @Override
    public boolean isSame(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        AliasSpecification that = (AliasSpecification) object;

        if (permanentRoom != that.permanentRoom) {
            return false;
        }
        if (aliasProviderCapability != null ? !aliasProviderCapability
                .equals(that.aliasProviderCapability) : that.aliasProviderCapability != null) {
            return false;
        }
        if (aliasTechnologies != null ? !aliasTechnologies
                .equals(that.aliasTechnologies) : that.aliasTechnologies != null) {
            return false;
        }
        if (aliasTypes != null ? !aliasTypes.equals(that.aliasTypes) : that.aliasTypes != null) {
            return false;
        }
        if (permanentRoomParticipants != null ? !ObjectHelper.isSame(permanentRoomParticipants,
                that.permanentRoomParticipants) : that.permanentRoomParticipants != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }
}
