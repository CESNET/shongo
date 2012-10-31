package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.reservation.CompartmentReservation;
import cz.cesnet.shongo.controller.scheduler.report.AllocatingCompartmentReport;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Manager for {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableManager extends AbstractManager
{
    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public ExecutableManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param executable to be created in the database
     */
    public void create(Executable executable)
    {
        super.create(executable);
    }

    /**
     * @param executable to be updated in the database
     */
    public void update(Executable executable)
    {
        super.update(executable);
    }

    /**
     * @param executable to be deleted in the database
     */
    public void delete(Executable executable)
    {
        super.delete(executable);
    }

    /**
     * @param executableId of the {@link Executable}
     * @return {@link Executable} with given identifier
     * @throws cz.cesnet.shongo.fault.EntityNotFoundException
     *          when the {@link Executable} doesn't exist
     */
    public Executable get(Long executableId) throws EntityNotFoundException
    {
        try {
            Executable executable = entityManager.createQuery(
                    "SELECT executable FROM Executable executable"
                            + " WHERE executable.id = :id AND executable.state != :notAllocated",
                    Executable.class)
                    .setParameter("id", executableId)
                    .setParameter("notAllocated", Executable.State.NOT_ALLOCATED)
                    .getSingleResult();
            return executable;
        }
        catch (NoResultException exception) {
            throw new EntityNotFoundException(Executable.class, executableId);
        }
    }

    /**
     * @param interval
     * @return list of {@link Executable}s which should be executed by an {@link Executor} in given {@code interval}
     */
    public List<Executable> listExecutablesForExecution(Interval interval)
    {
        List<Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE (executable.state = :notStarted AND "
                        + "        executable.slotStart < :end AND executable.slotEnd > :start)"
                        + " OR (executable.state = :started)",
                Executable.class)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .setParameter("notStarted", Executable.State.NOT_STARTED)
                .setParameter("started", Executable.State.STARTED)
                .getResultList();
        return executables;
    }

    /**
     * @return list of all allocated {@link Executable}s
     */
    public List<Executable> list()
    {
        List<Executable> executables = entityManager
                .createQuery("SELECT executable FROM Executable executable WHERE executable.state != :notAllocated",
                        Executable.class)
                .setParameter("notAllocated", Executable.State.NOT_ALLOCATED)
                .getResultList();
        return executables;
    }

    /**
     * Delete all {@link Executable}s which are not referenced by {@link CompartmentReservation}
     * and/or {@link AllocatingCompartmentReport} and should be deleted.
     */
    public void deleteAllNotReferenced()
    {
        List<Executable> executables = entityManager
                .createQuery("SELECT executable FROM Executable executable"
                        + " WHERE (executable.state = :notAllocated AND executable"
                        + "  NOT IN (SELECT report.compartment FROM AllocatingCompartmentReport report))"
                        + " OR (executable.state = :notStarted AND executable"
                        + "  NOT IN (SELECT reservation.compartment FROM CompartmentReservation reservation))",
                        Executable.class)
                .setParameter("notAllocated", Executable.State.NOT_ALLOCATED)
                .setParameter("notStarted", Executable.State.NOT_STARTED)
                .getResultList();
        for (Executable executable : executables) {
            delete(executable);
        }
    }
}
