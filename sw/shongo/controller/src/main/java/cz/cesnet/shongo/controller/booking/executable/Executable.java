package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.compartment.Compartment;
import cz.cesnet.shongo.controller.booking.compartment.Connection;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.executor.ExecutionReport;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.util.StateReportSerializer;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.ReportException;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an object which can be executed by an {@link cz.cesnet.shongo.controller.executor.Executor}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class Executable extends ExecutionTarget
{

    /**
     * Current state of the {@link cz.cesnet.shongo.controller.booking.compartment.Compartment}.
     */
    private State state;

    /**
     * {@link Migration} to be performed to initialize this {@link Executable} from another {@link Executable}.
     */
    private Migration migration;

    /**
     * List of child {@link Executable}s.
     */
    private List<Executable> childExecutables = new LinkedList<Executable>();

    /**
     * {@link ExecutableService}s for this {@link Endpoint}.
     */
    protected List<ExecutableService> services = new ArrayList<ExecutableService>();

    @Override
    public void setSlotEnd(DateTime slotEnd)
    {
        super.setSlotEnd(slotEnd);
        for (Executable childExecutable : childExecutables) {
            childExecutable.setSlotEnd(slotEnd);
        }
    }

    @Override
    public void setSlot(DateTime slotStart, DateTime slotEnd)
    {
        super.setSlot(slotStart, slotEnd);
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

    @Transient
    @Override
    public Collection<? extends ExecutionTarget> getExecutionDependencies()
    {
        return childExecutables;
    }

    /**
     * @return {@link #services}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "executable")
    @Access(AccessType.FIELD)
    public List<ExecutableService> getServices()
    {
        return services;
    }

    /**
     * @param serviceId
     * @return {@link ExecutableService} for given {@code serviceId}
     */
    @Transient
    public ExecutableService getServiceById(Long serviceId)
    {
        for (ExecutableService service : services) {
            if (service.getId().equals(serviceId)) {
                return service;
            }
        }
        throw new CommonReportSet.EntityNotFoundException(
                ExecutableService.class.getSimpleName(), serviceId.toString());
    }

    /**
     * @param service to be added to the {@link #services}
     */
    public void addService(ExecutableService service)
    {
        // Manage bidirectional association
        if (!services.contains(service)) {
            services.add(service);
            service.setExecutable(this);
        }
    }

    /**
     * @param service to be removed from the {@link #services}
     */
    public void removeService(ExecutableService service)
    {
        // Manage bidirectional association
        if (services.contains(service)) {
            services.remove(service);
            service.setExecutable(null);
        }
    }

    /**
     * @return formatted {@link #reports} as string
     */
    @Transient
    protected ExecutableStateReport getExecutableStateReport(Report.UserType userType)
    {
        ExecutableStateReport executableStateReport = new ExecutableStateReport(userType);
        for (ExecutionReport report : getCachedSortedReports()) {
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
    public String getReportDescription()
    {
        return EntityIdentifier.formatId(this);
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
        return toApi(admin ? Report.UserType.DOMAIN_ADMIN : Report.UserType.USER);
    }

    /**
     * @return {@link Executable} converted to {@link cz.cesnet.shongo.controller.api.Executable}
     */
    public cz.cesnet.shongo.controller.api.Executable toApi(Report.UserType userType)
    {
        cz.cesnet.shongo.controller.api.Executable executableApi = createApi();
        executableApi.setId(EntityIdentifier.formatId(this));
        toApi(executableApi, userType);
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
     * @param userType
     */
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, Report.UserType userType)
    {
        executableApi.setId(EntityIdentifier.formatId(this));
        executableApi.setSlot(getSlot());
        executableApi.setState(getState().toApi());
        executableApi.setStateReport(getExecutableStateReport(userType));
        if (migration != null) {
            executableApi.setMigratedExecutable(migration.getSourceExecutable().toApi(userType));
        }
        for (ExecutableService service : getServices()) {
            executableApi.addService(service.toApi());
        }
    }

    /**
     * Start this {@link Executable}.
     *
     * @param executor
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
     * Update this {@link Executable}.
     *
     * @param executor
     * @param executableManager
     * @return new {@link State} or null when the state hasn't changed
     */
    public final State update(Executor executor, ExecutableManager executableManager)
    {
        if (!getState().isStarted()) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be updated only if it is started.", getId()));
        }
        State state = onUpdate(executor, executableManager);
        if (state != null) {
            setState(state);
        }
        return state;
    }

    /**
     * Start this {@link Executable}.
     *
     * @param executor
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
     * @param executor
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
     * @param executor
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
     * @param executor
     * @param executableManager
     * @return new {@link State}
     */
    protected State onStop(Executor executor, ExecutableManager executableManager)
    {
        return State.SKIPPED;
    }

    /**
     * @param service           which is being activated (it is in {@link ExecutableService.State#ACTIVE} state)
     * @param executor          to be used for preparation
     * @param executableManager to be used for preparation
     * @throws ReportException with {@link ExecutionReport} in {@link ReportException#report}
     *                         when this {@link ExecutionTarget} can't be prepared for the service activation
     */
    protected void onServiceActivation(ExecutableService service, Executor executor,
            ExecutableManager executableManager) throws ReportException
    {
    }

    /**
     * @param service           which is being deactivated (it is in {@link ExecutableService.State#NOT_ACTIVE} state)
     * @param executor          to be used for preparation
     * @param executableManager to be used for preparation
     * @throws ReportException with {@link ExecutionReport} in {@link ReportException#report}
     *                         when this {@link ExecutionTarget} can't be prepared for the service deactivation
     */
    protected void onServiceDeactivation(ExecutableService service, Executor executor,
            ExecutableManager executableManager) throws ReportException
    {
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
     * Update this {@link Executable} from given {@code executableConfiguration}.
     *
     * @param executableConfiguration
     * @param entityManager
     * @return true whether same change was made, false otherwise
     * @throws ControllerReportSet.ExecutableInvalidConfigurationException
     *          when this {@link Executable} cannot be updated from given {@code executableConfiguration}
     */
    public boolean updateFromExecutableConfigurationApi(ExecutableConfiguration executableConfiguration,
            EntityManager entityManager)
            throws ControllerReportSet.ExecutableInvalidConfigurationException
    {
        throw new ControllerReportSet.ExecutableInvalidConfigurationException(
                EntityIdentifier.formatId(this), ClassHelper.getClassShortName(executableConfiguration.getClass()));
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
         * {@link ExecutionReport}) and the entity for which it has been stored has been deleted
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
     * States which represents {@link Executable} which is created only for
     * {@link cz.cesnet.shongo.controller.scheduler.SchedulerReport} and thus it is not allocated.
     */
    public static final Set<State> NOT_ALLOCATED_STATES = new HashSet<State>()
    {{
            add(State.NOT_ALLOCATED);
            add(State.TO_DELETE);
        }};

    /**
     * States which represents {@link Executable} which is started.
     */
    public static final Set<State> STARTED_STATES = new HashSet<State>()
    {{
            add(State.STARTED);
            add(State.MODIFIED);
            add(State.STOPPING_FAILED);
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
