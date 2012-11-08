package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.util.ClassHelper;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import java.util.Map;

/**
 * Represents an abstract specification of any target for a {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Specification extends PersistentObject
{
    /**
     * Synchronize properties from given {@code specification}.
     *
     * @param specification from which will be copied all properties values to
     *                      this {@link Specification}
     * @return true if some modification was made
     */
    public boolean synchronizeFrom(Specification specification)
    {
        return false;
    }

    /**
     * @param originalSpecifications map of original {@link Specification} instances by the cloned instances which should
     *                               be populated by this cloning
     * @return cloned instance of {@link StatefulSpecification}. If the {@link StatefulSpecification} contains some
     *         child {@link StatefulSpecification} they should be recursively cloned too.
     */
    public Specification clone(Map<Specification, Specification> originalSpecifications)
    {
        Specification newSpecification = ClassHelper.createInstanceFromClassRuntime(getClass());
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

    /**
     * @param domain
     * @return {@link Specification} converted to {@link cz.cesnet.shongo.controller.api.Specification}
     */
    public cz.cesnet.shongo.controller.api.Specification toApi(Domain domain)
    {
        cz.cesnet.shongo.controller.api.Specification api = createApi();
        toApi(api, domain);
        return api;
    }

    /**
     * @param api from which {@link Specification} should be created
     * @return new instance of {@link Specification} for given {@code api}
     */
    public static Specification createFromApi(cz.cesnet.shongo.controller.api.Specification api,
            EntityManager entityManager, Domain domain)
    {
        Specification specification = null;
        if (api instanceof cz.cesnet.shongo.controller.api.CompartmentSpecification) {
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
        else if (api instanceof cz.cesnet.shongo.controller.api.ResourceSpecification) {
            specification = new ResourceSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.VirtualRoomSpecification) {
            specification = new VirtualRoomSpecification();
        }
        else {
            throw new TodoImplementException(api.getClass().getCanonicalName());
        }
        try {
            specification.fromApi(api, entityManager, domain);
        }
        catch (FaultException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
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
     * @param domain
     */
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi, Domain domain)
    {
        specificationApi.setIdentifier(getId());
    }

    /**
     * Synchronize from {@link cz.cesnet.shongo.controller.api.Specification}.
     *
     * @param specificationApi from which this {@link Specification} should be filled
     * @param entityManager
     * @param domain
     */
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain) throws FaultException
    {
    }
}
