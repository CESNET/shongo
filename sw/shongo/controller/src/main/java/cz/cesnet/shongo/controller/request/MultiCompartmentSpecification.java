package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a group of {@link CompartmentSpecification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class MultiCompartmentSpecification extends Specification
        implements StatefulSpecification, CompositeSpecification, ReservationTaskProvider
{
    /**
     * List of {@link CompartmentSpecification}s.
     */
    private List<CompartmentSpecification> specifications = new ArrayList<CompartmentSpecification>();

    /**
     * Constructor.
     */
    public MultiCompartmentSpecification()
    {
    }

    /**
     * @return {@link #specifications}
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<CompartmentSpecification> getSpecifications()
    {
        return Collections.unmodifiableList(specifications);
    }

    @Override
    @Transient
    public Collection<Specification> getChildSpecifications()
    {
        Collection<Specification> specifications = new ArrayList<Specification>();
        for (CompartmentSpecification compartmentSpecification : this.specifications) {
            specifications.add(compartmentSpecification);
        }
        return specifications;
    }

    /**
     * @param id of the requested {@link cz.cesnet.shongo.controller.request.CompartmentSpecification}
     * @return {@link cz.cesnet.shongo.controller.request.CompartmentSpecification} with given {@code id}
     * @throws cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException when the {@link cz.cesnet.shongo.controller.request.Specification} doesn't exist
     */
    @Transient
    private CompartmentSpecification getSpecificationById(Long id) throws PersistentEntityNotFoundException
    {
        for (CompartmentSpecification compartmentSpecification : specifications) {
            if (compartmentSpecification.getId().equals(id)) {
                return compartmentSpecification;
            }
        }
        throw new PersistentEntityNotFoundException(CompartmentSpecification.class, id);
    }

    /**
     * @param specification to be added to the {@link #specifications}
     */
    public void addSpecification(CompartmentSpecification specification)
    {
        specifications.add(specification);
    }

    /**
     * @param specification to be removed from the {@link #specifications}
     */
    public void removeSpecification(CompartmentSpecification specification)
    {
        specifications.remove(specification);
    }

    @Override
    public void addChildSpecification(Specification specification)
    {
        addSpecification((CompartmentSpecification) specification);
    }

    @Override
    public void removeChildSpecification(Specification specification)
    {
        removeSpecification((CompartmentSpecification) specification);
    }

    @Override
    @Transient
    public State getCurrentState()
    {
        State state = State.READY;
        for (Specification specification : specifications) {
            if (specification instanceof StatefulSpecification) {
                StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
                if (statefulSpecification.getCurrentState().equals(State.NOT_READY)) {
                    state = State.NOT_READY;
                    break;
                }
            }
        }
        return state;
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        return false;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        return new ReservationTask(context){

            @Override
            protected Reservation createReservation() throws ReportException
            {
                Reservation multiCompartmentReservation = new Reservation();
                multiCompartmentReservation.setSlot(getInterval());
                for (CompartmentSpecification compartmentSpecification : getSpecifications()) {
                    multiCompartmentReservation.addChildReservation(addChildReservation(compartmentSpecification));
                }
                return multiCompartmentReservation;
            }
        };
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.MultiCompartmentSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.MultiCompartmentSpecification multiCompartmentSpecificationApi =
                (cz.cesnet.shongo.controller.api.MultiCompartmentSpecification) specificationApi;
        for (CompartmentSpecification specification : getSpecifications()) {
            multiCompartmentSpecificationApi.addSpecification(specification.toApi());
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.MultiCompartmentSpecification multiCompartmentSpecificationApi =
                (cz.cesnet.shongo.controller.api.MultiCompartmentSpecification) specificationApi;

        // Create/modify specifications
        for (cz.cesnet.shongo.controller.api.CompartmentSpecification specApi :
                multiCompartmentSpecificationApi.getSpecifications()) {
            if (multiCompartmentSpecificationApi.isPropertyItemMarkedAsNew(
                    multiCompartmentSpecificationApi.SPECIFICATIONS, specApi)) {
                addChildSpecification(Specification.createFromApi(specApi, entityManager));
            }
            else {
                Specification specification = getSpecificationById(specApi.notNullIdAsLong());
                specification.fromApi(specApi, entityManager);
            }
        }
        // Delete specifications
        Set<cz.cesnet.shongo.controller.api.CompartmentSpecification> apiDeletedSpecifications =
                multiCompartmentSpecificationApi.getPropertyItemsMarkedAsDeleted(
                        multiCompartmentSpecificationApi.SPECIFICATIONS);
        for (cz.cesnet.shongo.controller.api.CompartmentSpecification specApi : apiDeletedSpecifications) {
            removeSpecification(getSpecificationById(specApi.notNullIdAsLong()));
        }

        super.fromApi(specificationApi, entityManager);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("specifications", specifications);
    }
}
