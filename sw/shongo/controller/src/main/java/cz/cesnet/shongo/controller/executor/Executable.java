package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportablePersistentObject;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an object which can be executed by an {@link cz.cesnet.shongo.controller.Executor}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Executable extends ReportablePersistentObject
{
    /**
     * User-id of an user who is owner of the {@link Executable}.
     */
    private String userId;

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
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
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

    /**
     * @return collection of execution dependencies
     */
    @Transient
    public Collection<Executable> getExecutionDependencies()
    {
        return childExecutables;
    }

    @PrePersist
    protected void onCreate()
    {
        if (state == null) {
            state = State.NOT_ALLOCATED;
        }
    }

    /**
     * @return {@link Executable} converted to {@link cz.cesnet.shongo.controller.api.Executable}
     */
    public cz.cesnet.shongo.controller.api.Executable toApi()
    {
        cz.cesnet.shongo.controller.api.Executable api = createApi();
        toApi(api);
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
     */
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi)
    {
        executableApi.setId(IdentifierFormat.formatGlobalId(this));
        executableApi.setUserId(getUserId());
    }

    /**
     * Start given {@code executable}.
     *
     * @param executor which is executing
     */
    public final void start(Executor executor)
    {
        if (!STATES_FOR_STARTING.contains(getState())) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be started only if it is not started yet.", getId()));
        }
        State state = onStart(executor);
        setState(state);
    }

    /**
     * Start given {@code executable}.
     *
     * @param executor which is executing
     */
    public final void stop(Executor executor)
    {
        if (!STATES_FOR_STOPPING.contains(getState())) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be stopped only if it is started.", getId()));
        }
        State state = onStop(executor);
        setState(state);
    }

    /**
     * Start this {@link Executable}.
     *
     * @param executor which is executing
     * @return new {@link State}
     */
    protected State onStart(Executor executor)
    {
        return getDefaultState();
    }

    /**
     * Stop this {@link Executable}.
     *
     * @param executor which is executing
     * @return new {@link State}
     */
    protected State onStop(Executor executor)
    {
        return State.SKIPPED;
    }

    /**
     * @return {@link State} by children or {@link State#SKIPPED}
     */
    @Transient
    public State getDefaultState()
    {
        State state = State.SKIPPED;
        for (Executable childExecutable : childExecutables) {
            State childExecutableState = childExecutable.getState();
            switch (state) {
                case SKIPPED:
                    switch (childExecutableState) {
                        case STARTED:
                            state = State.STARTED;
                            break;
                        case STARTING_FAILED:
                            state = State.STARTING_FAILED;
                            break;
                        case STOPPED:
                            state = State.STOPPED;
                            break;
                        case STOPPING_FAILED:
                            state = State.STOPPING_FAILED;
                            break;
                    }
                    break;
                case STARTED:
                    switch (childExecutableState) {
                        case STARTING_FAILED:
                        case STOPPED:
                            state = State.PARTIALLY_STARTED;
                            break;
                    }
                    break;
                case STARTING_FAILED:
                    switch (childExecutableState) {
                        case STARTED:
                        case STOPPING_FAILED:
                            state = State.PARTIALLY_STARTED;
                            break;
                        case STOPPED:
                            state = State.STOPPED;
                            break;
                    }
                    break;
                case STOPPED:
                    switch (childExecutableState) {
                        case STARTED:
                        case STOPPING_FAILED:
                            state = State.PARTIALLY_STARTED;
                            break;
                    }
                    break;
            }
        }
        return state;
    }

    /**
     * State of the {@link Executable}.
     */
    public static enum State
    {
        /**
         * {@link Executable} which has not been fully allocated (e.g., {@link Executable} is stored for
         * {@link Report}).
         */
        NOT_ALLOCATED,

        /**
         * {@link Executable} which has not been fully allocated (e.g., {@link Executable} has been stored for
         * {@link Report}) and the entity for which it has been stored has been deleted so the {@link Executable}
         * should be also deleted.
         */
        TO_DELETE,

        /**
         * {@link Executable} has not been started yet.
         */
        NOT_STARTED,

        /**
         * {@link Executable} starting/stopping was skipped (starting/stopping doesn't make sense).
         */
        SKIPPED,

        /**
         * {@link Executable} is already started.
         */
        STARTED,

        /**
         * {@link Executable} is started partially (some child executables failed to start).
         */
        PARTIALLY_STARTED,

        /**
         * {@link Executable} failed to start.
         */
        STARTING_FAILED,

        /**
         * {@link Executable} has been already stopped.
         */
        STOPPED,

        /**
         * {@link Executable} failed to stop.
         */
        STOPPING_FAILED;

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
                case SKIPPED:
                    return cz.cesnet.shongo.controller.api.Executable.State.NOT_STARTED;
                case STARTED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STARTED;
                case STARTING_FAILED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STARTING_FAILED;
                case STOPPED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STOPPED;
                case STOPPING_FAILED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STOPPING_FAILED;
                default:
                    throw new IllegalStateException("Cannot convert " + this.toString() + " to API.");
            }
        }
    }

    /**
     * Collection of {@link Executable.State}s for {@link Executable}s which should be started.
     */
    public static Set<State> STATES_FOR_STARTING = new HashSet<State>()
    {{
            add(Executable.State.NOT_STARTED);
        }};

    /**
     * Collection of {@link Executable.State}s for {@link Executable}s which should be stopped.
     */
    public static Set<Executable.State> STATES_FOR_STOPPING = new HashSet<Executable.State>()
    {{
            add(Executable.State.STARTED);
            add(Executable.State.PARTIALLY_STARTED);
            add(Executable.State.STOPPING_FAILED);
        }};
}
