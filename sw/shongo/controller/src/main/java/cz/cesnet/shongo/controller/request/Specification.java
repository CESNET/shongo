package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.util.ClassHelper;
import cz.cesnet.shongo.TodoImplementException;
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
     * @param specification from which will be copied all properties values to
     *                      this {@link Specification}
     * @return true if some modification was made
     */
    public boolean synchronizeFrom(Specification specification)
    {
        boolean modified = false;
        if (!technologies.equals(specification.getTechnologies())) {
            setTechnologies(specification.getTechnologies());
            modified = true;
        }
        return modified;
    }

    /**
     * @param originalSpecifications map of original {@link Specification} instances by the cloned instances which should
     *                               be populated by this cloning
     * @return cloned instance of {@link StatefulSpecification}. If the {@link StatefulSpecification} contains some
     *         child {@link StatefulSpecification} they should be recursively cloned too.
     */
    public Specification clone(Map<Specification, Specification> originalSpecifications)
    {
        Specification newSpecification = ClassHelper.createInstanceFromClass(getClass());
        newSpecification.synchronizeFrom(this);
        if (this instanceof CompositeSpecification) {
            CompositeSpecification compositeSpecification = (CompositeSpecification) this;
            CompositeSpecification newCompositeSpecification = (CompositeSpecification) newSpecification;
            for (Specification childSpecification : compositeSpecification.getChildSpecifications()) {
                newCompositeSpecification.addChildSpecification(childSpecification.clone(originalSpecifications));
            }
        }

        originalSpecifications.put(newSpecification, this);

        return newSpecification;
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
        Specification specification = null;
        if (api instanceof cz.cesnet.shongo.controller.api.MultiCompartmentSpecification) {
            specification = new MultiCompartmentSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.CompartmentSpecification) {
            specification = new CompartmentSpecification();
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
