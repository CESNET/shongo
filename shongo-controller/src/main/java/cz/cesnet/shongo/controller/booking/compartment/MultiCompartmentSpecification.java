package cz.cesnet.shongo.controller.booking.compartment;

import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.specification.CompositeSpecification;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.specification.StatefulSpecification;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a group of {@link cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class MultiCompartmentSpecification extends Specification
        implements StatefulSpecification, CompositeSpecification, ReservationTaskProvider
{
    /**
     * List of {@link cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification}s.
     */
    private List<CompartmentSpecification> compartmentSpecifications = new LinkedList<CompartmentSpecification>();

    /**
     * Constructor.
     */
    public MultiCompartmentSpecification()
    {
    }

    /**
     * @return {@link #compartmentSpecifications}
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<CompartmentSpecification> getCompartmentSpecifications()
    {
        return Collections.unmodifiableList(compartmentSpecifications);
    }

    @Override
    @Transient
    public List<? extends Specification> getChildSpecifications()
    {
        return Collections.unmodifiableList(compartmentSpecifications);
    }

    /**
     * @param specification to be added to the {@link #compartmentSpecifications}
     */
    public void addSpecification(CompartmentSpecification specification)
    {
        compartmentSpecifications.add(specification);
    }

    /**
     * @param specification to be removed from the {@link #compartmentSpecifications}
     */
    public void removeSpecification(CompartmentSpecification specification)
    {
        compartmentSpecifications.remove(specification);
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
        for (Specification specification : compartmentSpecifications) {
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
    public void updateSpecificationSummary(EntityManager entityManager, boolean deleteOnly, boolean flush)
    {
        super.updateSpecificationSummary(entityManager, deleteOnly, flush);
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        for (CompartmentSpecification compartmentSpecification : compartmentSpecifications) {
            reservationRequestManager.updateSpecificationSummary(compartmentSpecification, deleteOnly);
        }
    }

    @Override
    public boolean synchronizeFrom(Specification specification, EntityManager entityManager)
    {
        return super.synchronizeFrom(specification, entityManager);
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot) throws SchedulerException
    {
        return new ReservationTask(schedulerContext, slot)
        {
            @Override
            protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
            {
                Reservation multiCompartmentReservation = new Reservation();
                multiCompartmentReservation.setSlot(slot);
                for (CompartmentSpecification compartmentSpecification : getCompartmentSpecifications()) {
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
        for (CompartmentSpecification specification : getCompartmentSpecifications()) {
            multiCompartmentSpecificationApi.addSpecification(specification.toApi());
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi,
            final EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.MultiCompartmentSpecification multiCompartmentSpecificationApi =
                (cz.cesnet.shongo.controller.api.MultiCompartmentSpecification) specificationApi;

        Synchronization.synchronizeCollection(
                compartmentSpecifications, multiCompartmentSpecificationApi.getCompartmentSpecifications(),
                new Synchronization.Handler<CompartmentSpecification,
                        cz.cesnet.shongo.controller.api.CompartmentSpecification>(CompartmentSpecification.class)
                {
                    @Override
                    public CompartmentSpecification createFromApi(
                            cz.cesnet.shongo.controller.api.CompartmentSpecification objectApi)
                    {
                        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
                        compartmentSpecification.fromApi(objectApi, entityManager);
                        return compartmentSpecification;
                    }

                    @Override
                    public void updateFromApi(CompartmentSpecification object,
                            cz.cesnet.shongo.controller.api.CompartmentSpecification objectApi)
                    {
                        object.fromApi(objectApi, entityManager);
                    }
                });

        super.fromApi(specificationApi, entityManager);
    }
}
