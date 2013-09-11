package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.util.StateReportSerializer;
import cz.cesnet.shongo.report.AbstractReport;
import cz.cesnet.shongo.report.Reportable;
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
public abstract class Executable extends PersistentObject implements Reportable, Reporter.ReportContext
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
     * Attempt count.
     */
    private int attemptCount;

    /**
     * Date/time for next attempt to start/stop/update executable. If this date/time is empty, the executable
     * should be started/updated/stopped as soon as possible.
     */
    private DateTime nextAttempt;

    /**
     * {@link Migration} to be performed to initialize this {@link Executable} from another {@link Executable}.
     */
    private Migration migration;

    /**
     * List of child {@link Executable}s.
     */
    private List<Executable> childExecutables = new LinkedList<Executable>();

    /**
     * List of report for this object.
     */
    private List<ExecutableReport> reports = new LinkedList<ExecutableReport>();

    /**
     * Cached sorted {@link #reports}.
     */
    private List<ExecutableReport> cachedSortedReports;

    /**
     * @return {@link #slotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.FIELD)
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
        for (Executable childExecutable : childExecutables) {
            childExecutable.setSlotStart(slotStart);
        }
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.FIELD)
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
        for (Executable childExecutable : childExecutables) {
            childExecutable.setSlotEnd(slotEnd);
        }
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
        setSlot(slot.getStart(), slot.getEnd());
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param slotStart
     * @param slotEnd
     */
    public void setSlot(DateTime slotStart, DateTime slotEnd)
    {
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        for (Executable childExecutable : childExecutables) {
            childExecutable.setSlot(slotStart, slotEnd);
        }
    }

    /**
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
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

        // Apply resetting state to all children (recursively)
        if (state == null || state == State.NOT_ALLOCATED) {
            for (Executable childExecutable : childExecutables) {
                childExecutable.setState(state);
            }
        }
        // Apply NOT_STARTED state to all children (recursively)
        else if (state == State.NOT_STARTED) {
            for (Executable childExecutable : childExecutables) {
                State childExecutableState = childExecutable.getState();
                if (childExecutableState == null || childExecutableState == State.NOT_ALLOCATED) {
                    childExecutable.setState(State.NOT_STARTED);
                }
            }
        }
    }

    /**
     * @return {@link #attemptCount}
     */
    @Column(nullable = false, columnDefinition = "integer default 0")
    public int getAttemptCount()
    {
        return attemptCount;
    }

    /**
     * @param attemptCount sets the {@link #attemptCount}
     */
    public void setAttemptCount(int attemptCount)
    {
        this.attemptCount = attemptCount;
    }

    /**
     * @return {@link #nextAttempt}
     */
    @Column
    @Type(type = "DateTime")
    public DateTime getNextAttempt()
    {
        return nextAttempt;
    }

    /**
     * @param nextAttempt sets the {@link #nextAttempt}
     */
    public void setNextAttempt(DateTime nextAttempt)
    {
        this.nextAttempt = nextAttempt;
    }

    /**
     * @return {@link #migration}
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "targetExecutable")
    @Access(AccessType.FIELD)
    public Migration getMigration()
    {
        return migration;
    }

    /**
     * @param migration sets the {@link #migration}
     */
    public void setMigration(Migration migration)
    {
        // Manage bidirectional association
        if (migration != this.migration) {
            if (this.migration != null) {
                Migration oldMigration = this.migration;
                this.migration = null;
                oldMigration.setTargetExecutable(null);
            }
            if (migration != null) {
                this.migration = migration;
                this.migration.setTargetExecutable(this);
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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "executable", orphanRemoval = true)
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
        cachedSortedReports = null;
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    public void addReport(ExecutableReport report)
    {
        // Manage bidirectional association
        if (reports.contains(report) == false) {
            reports.add(report);
            report.setExecutable(this);
        }
        cachedSortedReports = null;
    }

    /**
     * @param report to be removed from the {@link #reports}
     */
    public void removeReport(ExecutableReport report)
    {
        // Manage bidirectional association
        if (reports.contains(report)) {
            reports.remove(report);
            report.setExecutable(null);
        }
        cachedSortedReports = null;
    }

    /**
     * Remove all {@link ExecutableReport}s from the {@link #reports}.
     */
    public void clearReports()
    {
        reports.clear();
        cachedSortedReports.clear();
    }

    /**
     * @return last added {@link ExecutableReport}
     */
    @Transient
    public ExecutableReport getLastReport()
    {
        return (reports.size() > 0 ? getCachedSortedReports().get(0) : null);
    }

    /**
     * @return number of {@link ExecutableReport}s
     */
    @Transient
    public int getReportCount()
    {
        return reports.size();
    }

    /**
     * @return {@link #cachedSortedReports}
     */
    @Transient
    private List<ExecutableReport> getCachedSortedReports()
    {
        if (cachedSortedReports == null) {
            cachedSortedReports = new LinkedList<ExecutableReport>();
            cachedSortedReports.addAll(reports);
            Collections.sort(cachedSortedReports, new Comparator<ExecutableReport>()
            {
                @Override
                public int compare(ExecutableReport o1, ExecutableReport o2)
                {
                    return -o1.getDateTime().compareTo(o2.getDateTime());
                }
            });
        }
        return cachedSortedReports;
    }

    /**
     * @return formatted {@link #reports} as string
     */
    @Transient
    protected ExecutableStateReport getExecutableStateReport(AbstractReport.MessageType messageType)
    {
        ExecutableStateReport executableStateReport = new ExecutableStateReport();
        for (ExecutableReport report : getCachedSortedReports()) {
            executableStateReport.addReport(new StateReportSerializer(report));
        }
        return executableStateReport;
    }

    @PrePersist
    protected void onCreate()
    {
        if (state == null) {
            state = State.NOT_ALLOCATED;
        }
    }

    @Transient
    @Override
    public String getReportDescription(AbstractReport.MessageType messageType)
    {
        return String.format("executable '%s'", EntityIdentifier.formatId(this));
    }

    @Transient
    @Override
    public String getReportContextName()
    {
        return "executable " + EntityIdentifier.formatId(this);
    }

    @Transient
    @Override
    public String getReportContextDetail()
    {
        return null;
    }

    /**
     * @return {@link Executable} converted to {@link cz.cesnet.shongo.controller.api.Executable}
     */
    public final cz.cesnet.shongo.controller.api.Executable toApi(boolean admin)
    {
        return toApi(admin ? AbstractReport.MessageType.DOMAIN_ADMIN : AbstractReport.MessageType.USER);
    }

    /**
     * @return {@link Executable} converted to {@link cz.cesnet.shongo.controller.api.Executable}
     */
    public cz.cesnet.shongo.controller.api.Executable toApi(AbstractReport.MessageType messageType)
    {
        cz.cesnet.shongo.controller.api.Executable executableApi = createApi();
        executableApi.setId(EntityIdentifier.formatId(this));
        toApi(executableApi, messageType);
        return executableApi;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.Executable}
     */
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        throw new TodoImplementException(getClass());
    }

    /**
     * Synchronize to {@link cz.cesnet.shongo.controller.api.Executable}.
     *
     * @param executableApi which should be filled from this {@link Executable}
     * @param messageType
     */
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, AbstractReport.MessageType messageType)
    {
        executableApi.setId(EntityIdentifier.formatId(this));
        executableApi.setSlot(getSlot());
        executableApi.setState(getState().toApi());
        executableApi.setStateReport(getExecutableStateReport(messageType));
        if (migration != null) {
            executableApi.setMigratedExecutable(migration.getSourceExecutable().toApi(messageType));
        }
    }

    /**
     * Start given {@code executable}.
     *
     * @param executor          which is executing
     * @param executableManager
     */
    public final void start(Executor executor, ExecutableManager executableManager)
    {
        if (getState().isStarted()) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be started only if it is not started yet.", getId()));
        }
        State state = onStart(executor, executableManager);
        setState(state);
    }

    /**
     * Update given {@code executable}.
     *
     * @param executor          which is executing
     * @param executableManager
     */
    public final void update(Executor executor, ExecutableManager executableManager)
    {
        if (!getState().isStarted()) {
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
     * @param executor          which is executing
     * @param executableManager
     */
    public final void stop(Executor executor, ExecutableManager executableManager)
    {
        if (!getState().isStarted()) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be stopped only if it is started.", getId()));
        }
        State state = onStop(executor, executableManager);
        setState(state);
    }

    /**
     * Start this {@link Executable}.
     *
     * @param executor          which is executing
     * @param executableManager
     * @return new {@link State}
     */
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        return State.SKIPPED;
    }

    /**
     * Update this {@link Executable}.
     *
     * @param executor          which is executing
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
     * @param executor          which is executing
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
         * {@link Executable} has not been fully allocated (e.g., {@link Executable} is stored for
         * {@link cz.cesnet.shongo.controller.scheduler.SchedulerReport}).
         */
        NOT_ALLOCATED(false, false),

        /**
         * {@link Executable} which has not been fully allocated (e.g., {@link Executable} has been stored for
         * {@link ExecutableReport}) and the entity for which it has been stored has been deleted
         * and thus the {@link Executable} should be also deleted.
         */
        TO_DELETE(false, false),

        /**
         * {@link Executable} has not been started yet.
         */
        NOT_STARTED(false, false),

        /**
         * {@link Executable} starting/stopping was skipped (starting/stopping doesn't make sense).
         */
        SKIPPED(false, false),

        /**
         * {@link Executable} is started.
         */
        STARTED(true, false),

        /**
         * {@link Executable} is started, but the {@link Executable} has been modified and the change(s) has
         * not been propagated to the device yet.
         */
        MODIFIED(true, true),

        /**
         * {@link Executable} is partially started (e.g., some child executables failed to start).
         */
        PARTIALLY_STARTED(true, false),

        /**
         * {@link Executable} failed to start.
         */
        STARTING_FAILED(false, false),

        /**
         * {@link Executable} has been stopped.
         */
        STOPPED(false, false),

        /**
         * {@link Executable} failed to stop.
         */
        STOPPING_FAILED(true, false);

        /**
         * Specifies whether state means that executable is started.
         */
        private final boolean started;

        /**
         * Specifies whether state means that executable is modified.
         */
        private final boolean modified;

        /**
         * Constructor.
         *
         * @param started  sets the {@link #started}
         * @param modified sets the {@link #modified}
         */
        private State(boolean started, boolean modified)
        {
            this.started = started;
            this.modified = modified;
        }

        /**
         * @return {@link #started}
         */
        public boolean isStarted()
        {
            return started;
        }

        /**
         * @return {@link #modified}
         */
        public boolean isModified()
        {
            return modified;
        }

        /**
         * @return converted to {@link cz.cesnet.shongo.controller.api.ExecutableState}
         */
        public ExecutableState toApi()
        {
            switch (this) {
                case NOT_ALLOCATED:
                    throw new IllegalStateException(this.toString() + " should not be converted to API.");
                case NOT_STARTED:
                    return ExecutableState.NOT_STARTED;
                case SKIPPED:
                    return ExecutableState.NOT_STARTED;
                case STARTED:
                case MODIFIED:
                    return ExecutableState.STARTED;
                case STARTING_FAILED:
                    return ExecutableState.STARTING_FAILED;
                case STOPPED:
                    return ExecutableState.STOPPED;
                case STOPPING_FAILED:
                    return ExecutableState.STOPPING_FAILED;
                default:
                    throw new RuntimeException("Cannot convert " + this.toString() + " to API.");
            }
        }
    }

    /**
     * States which represents {@link Executable} which is is created only for
     * {@link cz.cesnet.shongo.controller.scheduler.SchedulerReport} and thus it is not allocated.
     */
    public static final Set<State> NOT_ALLOCATED_STATES = new HashSet<State>()
    {{
            add(State.NOT_ALLOCATED);
            add(State.TO_DELETE);
        }};

    /**
     * {@link Executable} class by {@link cz.cesnet.shongo.controller.api.Executable} class.
     */
    private static final Map<
            Class<? extends cz.cesnet.shongo.controller.api.Executable>,
            Set<Class<? extends Executable>>> CLASS_BY_API = new HashMap<
            Class<? extends cz.cesnet.shongo.controller.api.Executable>,
            Set<Class<? extends Executable>>>();

    /**
     * Initialization for {@link #CLASS_BY_API}.
     */
    static {
        CLASS_BY_API.put(RoomExecutable.class,
                new HashSet<Class<? extends Executable>>()
                {{
                        add(ResourceRoomEndpoint.class);
                        add(UsedRoomEndpoint.class);
                    }});
        CLASS_BY_API.put(CompartmentExecutable.class,
                new HashSet<Class<? extends Executable>>()
                {{
                        add(Compartment.class);
                    }});
        CLASS_BY_API.put(ConnectionExecutable.class,
                new HashSet<Class<? extends Executable>>()
                {{
                        add(Connection.class);
                    }});
        CLASS_BY_API.put(EndpointExecutable.class,
                new HashSet<Class<? extends Executable>>()
                {{
                        add(ResourceEndpoint.class);
                    }});
    }

    /**
     * @param executableApiClass
     * @return {@link Executable} for given {@code executableApiClass}
     */
    public static Set<Class<? extends Executable>> getClassesFromApi(
            Class<? extends cz.cesnet.shongo.controller.api.Executable> executableApiClass)
    {
        Set<Class<? extends Executable>> executableClass = CLASS_BY_API.get(executableApiClass);
        if (executableClass == null) {
            throw new TodoImplementException(executableApiClass);
        }
        return executableClass;
    }
}
