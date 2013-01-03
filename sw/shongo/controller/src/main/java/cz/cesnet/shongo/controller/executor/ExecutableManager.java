package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.scheduler.report.AllocatingCompartmentReport;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
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
     * @return {@link Executable} with given id
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
                        + " WHERE executable NOT IN("
                        + "    SELECT childExecutable FROM Executable executable "
                        + "   INNER JOIN executable.childExecutables childExecutable"
                        + " ) AND ("
                        + "   (executable.state = :notStarted AND "
                        + "        executable.slotStart < :end AND executable.slotEnd > :start)"
                        + "   OR (executable.state = :started)"
                        + " )",
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
    public List<Executable> list(String userId)
    {
        DatabaseFilter filter = new DatabaseFilter("executable");
        filter.addUserId(userId);
        TypedQuery<Executable> query = entityManager.createQuery("SELECT executable FROM Executable executable"
                + " WHERE executable.state != :notAllocated"
                + " AND executable NOT IN("
                + "    SELECT childExecutable FROM Executable executable "
                + "   INNER JOIN executable.childExecutables childExecutable"
                + " )"
                + " AND " + filter.toQueryWhere(),
                Executable.class);
        query.setParameter("notAllocated", Executable.State.NOT_ALLOCATED);
        filter.fillQueryParameters(query);
        List<Executable> executables = query.getResultList();
        return executables;
    }

    /**
     * Delete all {@link Executable}s which are not placed inside another {@link Executable} and not referenced by
     * any {@link Reservation} or {@link AllocatingCompartmentReport} and which should be automatically
     * deleted ({@link Executable.State#NOT_ALLOCATED} or {@link Executable.State#NOT_STARTED}).
     */
    public void deleteAllNotReferenced()
    {
        List<Executable> executables = entityManager
                .createQuery("SELECT executable FROM Executable executable"
                        + " WHERE executable NOT IN("
                        + "   SELECT childExecutable FROM Executable executable "
                        + "   INNER JOIN executable.childExecutables childExecutable"
                        + " ) AND ("
                        + "   (executable.state = :notAllocated AND executable"
                        + "     NOT IN (SELECT report.compartment FROM AllocatingCompartmentReport report))"
                        + "   OR (executable.state = :notStarted"
                        + "     AND executable NOT IN (SELECT reservation.executable FROM Reservation reservation))"
                        + " )",
                        Executable.class)
                .setParameter("notAllocated", Executable.State.NOT_ALLOCATED)
                .setParameter("notStarted", Executable.State.NOT_STARTED)
                .getResultList();
        for (Executable executable : executables) {
            delete(executable);
        }
    }
}
