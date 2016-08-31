package cz.cesnet.shongo.controller.booking.specification;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.controller.booking.alias.AliasSetSpecification;
import cz.cesnet.shongo.controller.booking.alias.AliasSpecification;
import cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification;
import cz.cesnet.shongo.controller.booking.compartment.MultiCompartmentSpecification;
import cz.cesnet.shongo.controller.booking.recording.RecordingServiceSpecification;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.resource.ResourceSpecification;
import cz.cesnet.shongo.controller.booking.room.RoomSpecification;
import cz.cesnet.shongo.controller.booking.value.ValueSpecification;
import cz.cesnet.shongo.report.ReportableSimple;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an abstract specification of any target for a {@link cz.cesnet.shongo.controller.booking.request.ReservationRequest}.
 * <p/>
 * {@link Specification}s must be able to {@link #clone()} itself, e.g., when {@link cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest}
 * is modified or when they are specified in a {@link cz.cesnet.shongo.controller.booking.request.ReservationRequestSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Specification extends SimplePersistentObject implements ReportableSimple
{
    /**
     * Set of {@link Technology}s which are required/supported by this {@link Specification}.
     */
    protected Set<Technology> technologies = new HashSet<Technology>();

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
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
     * Clear the {@link #technologies}
     */
    public void clearTechnologies()
    {
        technologies.clear();
    }

    /**
     * @param technologies to be added to the {@link #technologies}
     */
    public void addTechnologies(Set<Technology> technologies)
    {
        this.technologies.addAll(technologies);
    }

    /**
     * @param technology technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    /**
     * Update {@link #technologies} for this {@link Specification}.
     * @param entityManager
     */
    public void updateTechnologies(EntityManager entityManager)
    {
    }

    /**
     * @see {@code updateSpecificationSummary}
     */
    public void updateSpecificationSummary(EntityManager entityManager, boolean deleteOnly)
    {
        updateSpecificationSummary(entityManager, deleteOnly, true);
    }

    /**
     * Updates database table specification_summary used mainly for listing reservation requests {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary}.
     * This table is initialized based on view specification_summary_view. For more see init.sql.
     *
     * IMPORTANT: it is necessary to call this method EVERY time change of any entity {@link Specification} is made!!!
     * Otherwise list of reservation requests will be inconsistent.
     *
     * @param entityManager
     * @param deleteOnly
     * @param flush
     */
    public void updateSpecificationSummary(EntityManager entityManager, boolean deleteOnly, boolean flush)
    {
        if (flush) {
            entityManager.flush();
        }
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.updateSpecificationSummary(this, deleteOnly);
    }

    /**
     * Synchronize properties from given {@code specification}.
     *
     *
     * @param specification from which will be copied all properties values to this {@link cz.cesnet.shongo.controller.booking.specification.Specification}
     * @param entityManager
     * @return true if some modification was made, false otherwise
     */
    public boolean synchronizeFrom(Specification specification, EntityManager entityManager)
    {
        boolean modified = false;
        if (!technologies.equals(specification.getTechnologies())) {
            setTechnologies(specification.getTechnologies());
            modified = true;
        }
        if (this instanceof CompositeSpecification && specification instanceof CompositeSpecification) {
            CompositeSpecification compositeSpecification = (CompositeSpecification) this;
            CompositeSpecification compositeSpecificationFrom = (CompositeSpecification) specification;

            // Delete all child specifications
            Set<Specification> childSpecifications =
                    new HashSet<Specification>(compositeSpecification.getChildSpecifications());
            for (Specification childSpecification : childSpecifications) {
                compositeSpecification.removeChildSpecification(childSpecification);
            }

            // Add new child specifications
            for (Specification newSpecification : compositeSpecificationFrom.getChildSpecifications()) {
                compositeSpecification.addChildSpecification(newSpecification.clone(entityManager));
                modified = true;
            }
        }
        return modified;
    }

    /**
     * @return cloned instance of {@link Specification}. If the {@link Specification} is instance of
     *         {@link CompositeSpecification} (it can contain children) the child specifications
     *         should be recursively cloned too.
     */
    public Specification clone(EntityManager entityManager)
    {
        Specification specification = ClassHelper.createInstanceFromClass(getClass());
        specification.synchronizeFrom(this, entityManager);
        specification.updateTechnologies(entityManager);
        return specification;
    }

    @Override
    @Transient
    public String getReportDescription()
    {
        return getClass().getSimpleName();
    }

    /**
     * @return {@link Specification} converted to {@link cz.cesnet.shongo.controller.api.Specification}
     */
    public cz.cesnet.shongo.controller.api.Specification toApi()
    {
        cz.cesnet.shongo.controller.api.Specification api = createApi();
        toApi(api);
        return api;
    }

    /**
     * @param specificationApi from which {@link Specification} should be created
     * @return new instance of {@link Specification} for given {@code api}
     */
    public static Specification createFromApi(cz.cesnet.shongo.controller.api.Specification specificationApi,
            EntityManager entityManager)
    {
        Class<? extends Specification> specificationClass = getClassFromApi(specificationApi.getClass());
        Specification specification = ClassHelper.createInstanceFromClass(specificationClass);
        specification.fromApi(specificationApi, entityManager);
        return specification;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.Specification}
     */
    protected abstract cz.cesnet.shongo.controller.api.Specification createApi();

    /**
     * Synchronize to {@link cz.cesnet.shongo.controller.api.Specification}.
     *
     * @param specificationApi which should be filled from this {@link Specification}
     */
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        specificationApi.setId(getId());
    }

    /**
     * Synchronize from {@link cz.cesnet.shongo.controller.api.Specification}.
     *
     * @param specificationApi from which this {@link Specification} should be filled
     * @param entityManager
     */
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        // Update current technologies
        updateTechnologies(entityManager);
    }

    /**
     * {@link Specification} class by {@link cz.cesnet.shongo.controller.api.Specification} class.
     */
    private static final Map<
            Class<? extends cz.cesnet.shongo.controller.api.Specification>,
            Class<? extends Specification>> CLASS_BY_API = new HashMap<
            Class<? extends cz.cesnet.shongo.controller.api.Specification>,
            Class<? extends Specification>>();

    /**
     * Initialization for {@link #CLASS_BY_API}.
     */
    static {
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ValueSpecification.class,
                ValueSpecification.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.AliasSpecification.class,
                AliasSpecification.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.AliasSetSpecification.class,
                AliasSetSpecification.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ResourceSpecification.class,
                ResourceSpecification.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.RoomSpecification.class,
                RoomSpecification.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.RecordingServiceSpecification.class,
                RecordingServiceSpecification.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.CompartmentSpecification.class,
                CompartmentSpecification.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.MultiCompartmentSpecification.class,
                MultiCompartmentSpecification.class);
    }

    /**
     * @param specificationApiClass
     * @return {@link Specification} for given {@code specificationApiClass}
     */
    public static Class<? extends Specification> getClassFromApi(
            Class<? extends cz.cesnet.shongo.controller.api.Specification> specificationApiClass)
    {
        Class<? extends Specification> specificationClass = CLASS_BY_API.get(specificationApiClass);
        if (specificationClass == null) {
            throw new TodoImplementException(specificationApiClass);
        }
        return specificationClass;
    }
}
