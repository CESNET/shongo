package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.util.ClassHelper;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.Reportable;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents an abstract specification of any target for a {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Specification extends PersistentObject implements Reportable
{
    /**
     * Set of {@link Technology}s which are required/supported by this {@link Specification}.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
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
     */
    public void updateTechnologies()
    {
    }

    /**
     * Synchronize properties from given {@code specification}.
     *
     * @param specification from which will be copied all properties values to this {@link Specification}
     * @param originalMap   map of original {@link Specification} instances by the new cloned instances
     * @return true if some modification was made, false otherwise
     */
    public boolean synchronizeFrom(Specification specification, Map<Specification, Specification> originalMap)
    {
        boolean modified = false;
        if (!technologies.equals(specification.getTechnologies())) {
            setTechnologies(specification.getTechnologies());
            modified = true;
        }
        if (this instanceof CompositeSpecification && specification instanceof CompositeSpecification) {
            CompositeSpecification compositeSpecification = (CompositeSpecification) this;
            CompositeSpecification compositeSpecificationFrom = (CompositeSpecification) specification;

            // Build set of new specifications
            Set<Specification> newSpecifications = new HashSet<Specification>();
            for (Specification newSpecification : compositeSpecificationFrom.getChildSpecifications()) {
                newSpecifications.add(newSpecification);
            }

            if (originalMap != null) {
                // We have mapping of original specifications and thus iterate over existing child specifications and:
                // 1) update specifications whose original specification still exists
                // 2) delete specifications whose original specification doesn't exist anymore
                Set<Specification> deleteSpecifications = new HashSet<Specification>();
                for (Specification childSpecification : compositeSpecification.getChildSpecifications()) {
                    Specification originalSpecification = originalMap.get(childSpecification);
                    if (originalSpecification != null) {
                        if (originalSpecification == childSpecification) {
                            // Specification should not be newly created (it can be only updated)
                            newSpecifications.remove(originalSpecification);
                            // No need to update (it is the same specification and thus it is already updated)
                            continue;
                        }
                        else if (newSpecifications.contains(originalSpecification)) {
                            // Specification should not be newly created (it can be only updated)
                            newSpecifications.remove(originalSpecification);
                            // Update specification
                            modified |= childSpecification.synchronizeFrom(originalSpecification, originalMap);
                            continue;
                        }
                    }
                    // Original specification doesn't exist anymore and thus the specification should be deleted
                    deleteSpecifications.add(childSpecification);
                }
                // Delete all child specifications whose original specification doesn't exist
                for (Specification deletedSpecification : deleteSpecifications) {
                    compositeSpecification.removeChildSpecification(deletedSpecification);
                    modified = true;
                }
            }
            else {
                // We don't have mapping of original specifications and thus delete all child specifications
                Set<Specification> childSpecifications =
                        new HashSet<Specification>(compositeSpecification.getChildSpecifications());
                for (Specification childSpecification : childSpecifications) {
                    compositeSpecification.removeChildSpecification(childSpecification);
                }
            }

            // Add new child specifications
            for (Specification newSpecification : newSpecifications) {
                compositeSpecification.addChildSpecification(newSpecification.clone(originalMap));
                modified = true;
            }
        }
        return modified;
    }

    /**
     * @param originalMap map of original {@link Specification} instances by the new cloned instances
     *                    which should be populated by this cloning
     * @return cloned instance of {@link Specification}. If the {@link Specification} is instance of
     *         {@link CompositeSpecification} (it can contain children) the child specifications
     *         should be recursively cloned too.
     */
    public Specification clone(Map<Specification, Specification> originalMap)
    {
        Specification specification = ClassHelper.createInstanceFromClass(getClass());
        if (originalMap != null) {
            originalMap.put(specification, this);
        }
        specification.synchronizeFrom(this, originalMap);
        return specification;
    }

    @Override
    @Transient
    public String getReportDescription(Report.MessageType messageType)
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
     * @param api from which {@link Specification} should be created
     * @return new instance of {@link Specification} for given {@code api}
     */
    public static Specification createFromApi(cz.cesnet.shongo.controller.api.Specification api,
            EntityManager entityManager)
    {
        Specification specification;
        if (api instanceof cz.cesnet.shongo.controller.api.ValueSpecification) {
            specification = new ValueSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.AliasSpecification) {
            specification = new AliasSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.AliasSetSpecification) {
            specification = new AliasSetSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ResourceSpecification) {
            specification = new ResourceSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.RoomSpecification) {
            specification = new RoomSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.CompartmentSpecification) {
            specification = new CompartmentSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.MultiCompartmentSpecification) {
            specification = new MultiCompartmentSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ExistingEndpointSpecification) {
            specification = new ExistingEndpointSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ExternalEndpointSpecification) {
            specification = new ExternalEndpointSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification) {
            specification = new ExternalEndpointSetSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.LookupEndpointSpecification) {
            specification = new LookupEndpointSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.PersonSpecification) {
            specification = new PersonSpecification();
        }
        else {
            throw new TodoImplementException(api.getClass().getCanonicalName());
        }
        specification.fromApi(api, entityManager);
        return specification;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.Specification}
     */
    protected abstract cz.cesnet.shongo.controller.api.Specification createApi();

    /**
     * Synchronize to {@link cz.cesnet.shongo.controller.api.Specification}.
     *
     * @param specificationApi which should be filled from this {@link cz.cesnet.shongo.controller.request.Specification}
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
        updateTechnologies();
    }
}
