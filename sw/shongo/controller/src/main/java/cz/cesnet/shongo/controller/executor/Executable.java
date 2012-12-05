package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an object which can be executed by an {@link cz.cesnet.shongo.controller.Executor}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Executable extends PersistentObject
{
    /**
     * Identifier of an user who is owner of the {@link Executable}.
     */
    private Long userId;

    /**
     * Interval start date/time.
     */
    private DateTime slotStart;

    /**
     * Interval end date/time.
     */
    private DateTime slotEnd;

    /**
     * Current state of the {@link Compartment}.
     */
    private State state;

    /**
     * List of child {@link Executable}s.
     */
    private List<Executable> childExecutables = new ArrayList<Executable>();

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false)
    public Long getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #slotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotStart()
    {
        return slotStart;
    }

    /**
     * @param slotStart sets the {@link #slotStart}
     */
    public void setSlotStart(DateTime slotStart)
    {
        this.slotStart = slotStart;
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotEnd()
    {
        return slotEnd;
    }

    /**
     * @param slotEnd sets the {@link #slotEnd}
     */
    public void setSlotEnd(DateTime slotEnd)
    {
        this.slotEnd = slotEnd;
    }

    /**
     * @return slot ({@link #slotStart}, {@link #slotEnd})
     */
    @Transient
    public Interval getSlot()
    {
        return new Interval(slotStart, slotEnd);
    }

    /**
     * @param slot sets the slot
     */
    public void setSlot(Interval slot)
    {
        setSlotStart(slot.getStart());
        setSlotEnd(slot.getEnd());
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param start
     * @param end
     */
    public void setSlot(DateTime start, DateTime end)
    {
        setSlotStart(start);
        setSlotEnd(end);
    }

    /**
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;

        // Apply NOT_STARTED state to all children (recursively)
        if (state == State.NOT_STARTED) {
            for (Executable childExecutable : childExecutables) {
                State childExecutableState = childExecutable.getState();
                if (childExecutableState == null || childExecutableState == State.NOT_ALLOCATED) {
                    childExecutable.setState(State.NOT_STARTED);
                }
            }
        }
    }

    /**
     * @return {@link #childExecutables}
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(inverseJoinColumns = {@JoinColumn(name = "child_executable_id")})
    @Access(AccessType.FIELD)
    public List<Executable> getChildExecutables()
    {
        return childExecutables;
    }

    /**
     * @param executable to be added to the {@link #childExecutables}
     */
    public void addChildExecutable(Executable executable)
    {
        childExecutables.add(executable);
    }

    @PrePersist
    protected void onCreate()
    {
        if (state == null) {
            state = State.NOT_ALLOCATED;
        }
    }

    /**
     * @param domain
     * @return {@link Executable} converted to {@link cz.cesnet.shongo.controller.api.Executable}
     */
    public cz.cesnet.shongo.controller.api.Executable toApi(Domain domain)
    {
        cz.cesnet.shongo.controller.api.Executable api = createApi();
        toApi(api, domain);
        return api;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.Executable}
     */
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        throw new TodoImplementException(getClass().getCanonicalName());
    }

    /**
     * Synchronize to {@link cz.cesnet.shongo.controller.api.Executable}.
     *
     * @param executableApi which should be filled from this {@link cz.cesnet.shongo.controller.executor.Executable}
     * @param domain
     */
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, Domain domain)
    {
        executableApi.setIdentifier(domain.formatIdentifier(getId()));
        executableApi.setUserId(getUserId().intValue());
    }

    /**
     * @return printable name of this {@link Executable}
     */
    @Transient
    public String getName()
    {
        return String.format("executable '%d'", getId());
    }

    /**
     * Start given {@code executable}.
     *
     * @param executorThread thread which is executing
     * @param entityManager  which can be used for starting
     */
    public final void start(ExecutorThread executorThread, EntityManager entityManager)
    {
        if (getState() != State.NOT_STARTED) {
            throw new IllegalStateException(getName() + " can be started only if it is not started yet.");
        }
        State state = onStart(executorThread, entityManager);
        entityManager.getTransaction().begin();
        setState(state);
        entityManager.getTransaction().commit();
    }

    /**
     * Resume given {@code executable}.
     *
     * @param executorThread thread which is executing
     * @param entityManager  which can be used for resuming
     */
    public final void resume(ExecutorThread executorThread, EntityManager entityManager)
    {
        if (getState() != State.STARTED) {
            throw new IllegalStateException(getName() + " can be resumed only if it is started.");
        }
        State state = onResume(executorThread, entityManager);
        entityManager.getTransaction().begin();
        setState(state);
        entityManager.getTransaction().commit();
    }

    /**
     * Start given {@code executable}.
     *
     * @param executorThread thread which is executing
     * @param entityManager  which can be used for starting
     */
    public final void stop(ExecutorThread executorThread, EntityManager entityManager)
    {
        if (getState() != State.STARTED) {
            throw new IllegalStateException(getName() + " can be stopped only if it is started.");
        }
        State state = onStop(executorThread, entityManager);
        entityManager.getTransaction().begin();
        setState(state);
        entityManager.getTransaction().commit();
    }

    /**
     * Start all {@link #childExecutables} which are instance of {@code childrenType}.
     *
     * @param childrenType
     * @param executorThread
     * @param entityManager
     * @return number of child {@link Executable}s which were started by the method invocation
     */
    protected final int startChildren(Class<? extends Executable> childrenType, ExecutorThread executorThread,
            EntityManager entityManager)
    {
        int count = 0;
        for (Executable childExecutable : childExecutables) {
            if (!childrenType.isInstance(childExecutable)) {
                continue;
            }
            if (childExecutable.getState() == State.NOT_STARTED) {
                childExecutable.start(executorThread, entityManager);
            }
        }
        return count;
    }

    /**
     * Stop all {@link #childExecutables} which are instance of {@code childrenType}.
     *
     * @param childrenType
     * @param executorThread
     * @param entityManager
     * @return number of child {@link Executable}s which were started by the method invocation
     */
    protected final int stopChildren(Class<? extends Executable> childrenType, ExecutorThread executorThread,
            EntityManager entityManager)
    {
        int count = 0;
        for (Executable childExecutable : childExecutables) {
            if (!childrenType.isInstance(childExecutable)) {
                continue;
            }
            if (childExecutable.getState() == State.STARTED) {
                childExecutable.stop(executorThread, entityManager);
            }
        }
        return count;
    }

    /**
     * Start given {@code executable}.
     *
     * @param executorThread thread which is executing
     * @param entityManager  which can be used for starting
     * @return new {@link State}
     */
    protected State onStart(ExecutorThread executorThread, EntityManager entityManager)
    {
        return State.STARTED;
    }

    /**
     * Resume given {@code executable}.
     *
     * @param executorThread thread which is executing
     * @param entityManager  which can be used for resuming
     * @return new {@link State}
     */
    protected State onResume(ExecutorThread executorThread, EntityManager entityManager)
    {
        return State.STARTED;
    }

    /**
     * Stop given {@code executable}.
     *
     * @param executorThread thread which is executing
     * @param entityManager  which can be used for stopping
     * @return new {@link State}
     */
    protected State onStop(ExecutorThread executorThread, EntityManager entityManager)
    {
        return State.STOPPED;
    }

    /**
     * State of the {@link Executable}.
     */
    public static enum State
    {
        /**
         * {@link Executable} which has not been fully allocated yet (e.g., {@link Executable} is stored for
         * {@link Report}).
         */
        NOT_ALLOCATED,

        /**
         * {@link Executable} has not been started yet.
         */
        NOT_STARTED,

        /**
         * {@link Executable} is already started.
         */
        STARTED,

        /**
         * {@link Executable} failed to start.
         */
        STARTING_FAILED,

        /**
         * {@link Executable} has been already stopped.
         */
        STOPPED;

        /**
         * @return converted to {@link cz.cesnet.shongo.controller.api.Executable.State}
         */
        public cz.cesnet.shongo.controller.api.Executable.State toApi()
        {
            switch (this) {
                case NOT_ALLOCATED:
                    throw new IllegalStateException(this.toString() + " should not be converted to API.");
                case NOT_STARTED:
                    return cz.cesnet.shongo.controller.api.Executable.State.NOT_STARTED;
                case STARTED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STARTED;
                case STARTING_FAILED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STARTING_FAILED;
                case STOPPED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STOPPED;
                default:
                    throw new IllegalStateException("Cannot convert " + this.toString() + " to API.");
            }
        }
    }
}
