package cz.cesnet.shongo.controller.booking.alias;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.specification.Specification} for an {@link Alias}.
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
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<AliasType> getAliasTypes()
    {
        return Collections.unmodifiableSet(aliasTypes);
    }

    /**
     * @param aliasType
     * @return true if the {@link #aliasTypes} contains the given {@code aliasType},
     * false otherwise
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
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
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
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
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

    @Override
    public void updateTechnologies(EntityManager entityManager)
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
    public AliasSpecification clone(EntityManager entityManager)
    {
        return (AliasSpecification) super.clone(entityManager);
    }

    @Override
    public boolean synchronizeFrom(Specification specification, EntityManager entityManager)
    {
        AliasSpecification aliasSpecification = (AliasSpecification) specification;

        boolean modified = super.synchronizeFrom(specification, entityManager);
        modified |= !ObjectHelper.isSameIgnoreOrder(getAliasTechnologies(), aliasSpecification.getAliasTechnologies())
                || !ObjectHelper.isSameIgnoreOrder(getAliasTypes(), aliasSpecification.getAliasTypes())
                || !ObjectHelper.isSame(getValue(), aliasSpecification.getValue())
                || !ObjectHelper.isSamePersistent(getAliasProviderCapability(),
                aliasSpecification.getAliasProviderCapability());

        setAliasTechnologies(aliasSpecification.getAliasTechnologies());
        setAliasTypes(aliasSpecification.getAliasTypes());
        setValue(aliasSpecification.getValue());
        setAliasProviderCapability(aliasSpecification.getAliasProviderCapability());

        return modified;
    }

    @Override
    public AliasReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot) throws SchedulerException
    {
        AliasReservationTask aliasReservationTask = new AliasReservationTask(schedulerContext, slot);
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
                    ObjectIdentifier.formatId(getAliasProviderCapability().getResource()));
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi,
            final EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSpecification) specificationApi;

        setValue(aliasSpecificationApi.getValue());

        if (aliasSpecificationApi.getResourceId() == null) {
            setAliasProviderCapability(null);
        }
        else {
            Long resourceId = ObjectIdentifier.parseLocalId(
                    aliasSpecificationApi.getResourceId(), ObjectType.RESOURCE);
            ResourceManager resourceManager = new ResourceManager(entityManager);
            Resource resource = resourceManager.get(resourceId);
            AliasProviderCapability aliasProviderCapability = resource
                    .getCapabilityRequired(AliasProviderCapability.class);
            setAliasProviderCapability(aliasProviderCapability);
        }

        Synchronization.synchronizeCollection(aliasTechnologies, aliasSpecificationApi.getTechnologies());
        Synchronization.synchronizeCollection(aliasTypes, aliasSpecificationApi.getAliasTypes());

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
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }
}
