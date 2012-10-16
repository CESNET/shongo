package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.reservation.CompartmentReservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.scheduler.report.AllocatingCompartmentReport;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Manager for {@link cz.cesnet.shongo.controller.compartment.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentManager extends AbstractManager
{
    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public CompartmentManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param compartment to be created in the database
     */
    public void create(Compartment compartment)
    {
        super.create(compartment);
    }

    /**
     * @param compartment to be updated in the database
     */
    public void update(Compartment compartment)
    {
        super.update(compartment);
    }

    /**
     * @param compartment to be deleted in the database
     */
    public void delete(Compartment compartment)
    {
        super.delete(compartment);
    }

    /**
     * @param compartmentId of the {@link Compartment}
     * @return {@link Compartment} with given identifier
     * @throws cz.cesnet.shongo.fault.EntityNotFoundException
     *          when the {@link Compartment} doesn't exist
     */
    public Compartment get(Long compartmentId) throws EntityNotFoundException
    {
        try {
            Compartment compartment = entityManager.createQuery(
                    "SELECT compartment FROM Compartment compartment"
                            + " WHERE compartment.id = :id AND compartment.state != :notAllocated",
                    Compartment.class)
                    .setParameter("id", compartmentId)
                    .setParameter("notAllocated", Compartment.State.NOT_ALLOCATED)
                    .getSingleResult();
            return compartment;
        }
        catch (NoResultException exception) {
            throw new EntityNotFoundException(Compartment.class, compartmentId);
        }
    }

    /**
     * @param interval
     * @return list of {@link Compartment} which should be executed by an {@link Executor} in given {@code interval}
     */
    public List<Compartment> listCompartmentsForExecution(Interval interval)
    {
        List<Compartment> compartments = entityManager.createQuery(
                "SELECT compartment FROM Compartment compartment"
                        + " WHERE (compartment.state = :notStarted AND "
                        + "        compartment.slotStart < :end AND compartment.slotEnd > :start)"
                        + " OR (compartment.state = :started)",
                Compartment.class)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .setParameter("notStarted", Compartment.State.NOT_STARTED)
                .setParameter("started", Compartment.State.STARTED)
                .getResultList();
        return compartments;
    }

    /**
     * @return list of all allocated compartment
     */
    public List<Compartment> list()
    {
        List<Compartment> compartments = entityManager
                .createQuery("SELECT compartment FROM Compartment compartment WHERE compartment.state != :notAllocated",
                        Compartment.class)
                .setParameter("notAllocated", Compartment.State.NOT_ALLOCATED)
                .getResultList();
        return compartments;
    }

    /**
     * Delete all {@link Compartment} which are not referenced by {@link CompartmentReservation}
     * and/or {@link AllocatingCompartmentReport} and should be deleted.
     */
    public void deleteAllNotReferenced()
    {
        List<Compartment> compartments = entityManager
                .createQuery("SELECT compartment FROM Compartment compartment"
                        + " WHERE (compartment.state = :notAllocated AND compartment"
                        + "  NOT IN (SELECT report.compartment FROM AllocatingCompartmentReport report))"
                        + " OR (compartment.state = :notStarted AND compartment"
                        + "  NOT IN (SELECT reservation.compartment FROM CompartmentReservation reservation))",
                        Compartment.class)
                .setParameter("notAllocated", Compartment.State.NOT_ALLOCATED)
                .setParameter("notStarted", Compartment.State.NOT_STARTED)
                .getResultList();
        for (Compartment compartment : compartments) {
            delete(compartment);
        }
    }
}
