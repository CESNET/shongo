package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.*;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.report.Report;
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
public abstract class Executable extends PersistentObject
{
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
    private List<Executable> childExecutables = new LinkedList<Executable>();

    /**
     * List of report for this object.
     */
    private List<ExecutableReport> reports = new LinkedList<ExecutableReport>();

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

    /**
     * @return {@link #reports}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<ExecutableReport> getReports()
    {
        return Collections.unmodifiableList(reports);
    }

    /**
     * @param reports sets the {@link #reports}
     */
    public void setReports(List<ExecutableReport> reports)
    {
        this.reports.clear();
        for (ExecutableReport report : reports) {
            this.reports.add(report);
        }
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    public void addReport(ExecutableReport report)
    {
        reports.add(report);
    }

    /**
     * @param report to be removed from the {@link #reports}
     */
    public void removeReport(ExecutableReport report)
    {
        reports.remove(report);
    }

    /**
     * Remove all {@link Report}s from the {@link #reports}.
     */
    public void clearReports()
    {
        reports.clear();
    }

    /**
     * @return formatted {@link #reports} as string
     */
    @Transient
    protected String getReportText()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (ExecutableReport report : reports) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }
            String dateTime = cz.cesnet.shongo.Temporal.formatDateTime(report.getDateTime());
            stringBuilder.append("[");
            stringBuilder.append(dateTime);
            stringBuilder.append("] ");
            stringBuilder.append(report.getMessage());
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
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
        cz.cesnet.shongo.controller.api.Executable executableApi = createApi();
        executableApi.setId(EntityIdentifier.formatId(this));
        toApi(executableApi);
        return executableApi;
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
        executableApi.setId(EntityIdentifier.formatId(this));
        executableApi.setSlot(getSlot());
        executableApi.setState(getState().toApi());
        executableApi.setStateReport(getReportText());
    }

    /**
     * Start given {@code executable}.
     *
     * @param executor which is executing
     * @param executableManager
     */
    public final void start(Executor executor, ExecutableManager executableManager)
    {
        if (!STATES_NOT_STARTED.contains(getState())) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be started only if it is not started yet.", getId()));
        }
        State state = onStart(executor, executableManager);
        setState(state);
    }

    /**
     * Update given {@code executable}.
     *
     * @param executor which is executing
     * @param executableManager
     */
    public final void update(Executor executor, ExecutableManager executableManager)
    {
        if (!STATES_STARTED.contains(getState())) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be updated only if it is started.", getId()));
        }
        State state = onUpdate(executor, executableManager);
        if (state != null) {
            setState(state);
        }
    }

    /**
     * Start given {@code executable}.
     *
     * @param executor which is executing
     * @param executableManager
     */
    public final void stop(Executor executor, ExecutableManager executableManager)
    {
        if (!STATES_STARTED.contains(getState())) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be stopped only if it is started.", getId()));
        }
        State state = onStop(executor, executableManager);
        setState(state);
    }

    /**
     * Start this {@link Executable}.
     *
     *
     * @param executor which is executing
     * @param executableManager
     * @return new {@link State}
     */
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        return getDefaultState();
    }

    /**
     * Update this {@link Executable}.
     *
     *
     *
     * @param executor which is executing
     * @param executableManager
     * @return new {@link State} or null when the state should not change
     */
    protected State onUpdate(Executor executor, ExecutableManager executableManager)
    {
        return null;
    }

    /**
     * Stop this {@link Executable}.
     *
     *
     * @param executor which is executing
     * @param executableManager
     * @return new {@link State}
     */
    protected State onStop(Executor executor, ExecutableManager executableManager)
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
         * {@link Executable} has not been fully allocated (e.g., {@link Executable} is stored for {@link Report}).
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
         * {@link Executable} is started.
         */
        STARTED,

        /**
         * {@link Executable} is started, but the {@link Executable} has been modified and the change(s) has
         * not been propagated to the device yet.
         */
        MODIFIED,

        /**
         * {@link Executable} is partially started (e.g., some child executables failed to start).
         */
        PARTIALLY_STARTED,

        /**
         * {@link Executable} failed to start.
         */
        STARTING_FAILED,

        /**
         * {@link Executable} has been stopped.
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
                case MODIFIED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STARTED;
                case STARTING_FAILED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STARTING_FAILED;
                case STOPPED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STOPPED;
                case STOPPING_FAILED:
                    return cz.cesnet.shongo.controller.api.Executable.State.STOPPING_FAILED;
                default:
                    throw new RuntimeException("Cannot convert " + this.toString() + " to API.");
            }
        }
    }

    /**
     * Collection of {@link Executable.State}s which represents that the {@link Executable}s is not started and
     * can be started.
     */
    public static Set<State> STATES_NOT_STARTED = new HashSet<State>()
    {{
            add(State.NOT_STARTED);
        }};

    /**
     * Collection of {@link Executable.State}s for {@link Executable}s which represents that the {@link Executable}
     * is started and can should be updated.
     */
    public static Set<Executable.State> STATES_MODIFIED = new HashSet<Executable.State>()
    {{
            add(State.MODIFIED);
        }};

    /**
     * Collection of {@link Executable.State}s for {@link Executable}s which represents that the {@link Executable}
     * is started and can be updated or stopped.
     */
    public static Set<Executable.State> STATES_STARTED = new HashSet<Executable.State>()
    {{
            add(State.STARTED);
            add(State.MODIFIED);
            add(State.PARTIALLY_STARTED);
            add(State.STOPPING_FAILED);
        }};
}
