package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.compartment.Compartment;
import cz.cesnet.shongo.controller.booking.compartment.Connection;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.executor.ExecutionReport;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.scheduler.SchedulerReport;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.ReportException;
import org.hibernate.annotations.Proxy;
import org.joda.time.DateTime;

import jakarta.persistence.*;
import java.util.*;

/**
 * Represents an object which can be executed by an {@link cz.cesnet.shongo.controller.executor.Executor}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Proxy(lazy = false)
public abstract class Executable extends ExecutionTarget
{
    /**
     * Current state of the {@link cz.cesnet.shongo.controller.booking.compartment.Compartment}.
     */
    protected State state;

    /**
     * Specifies whether this {@link Executable} should be updated.
     */
    private boolean modified;

    /**
     * {@link Executable} from which this {@link Executable} should be initialized from.
     */
    private Executable migrateFromExecutable;

    /**
     * {@link Executable} which should be initialized from this {@link Executable}.
     */
    private Executable migrateToExecutable;

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
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
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
     * @return {@link #modified}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isModified()
    {
        return modified;
    }

    /**
     * @param update {@link #modified}
     */
    public void setModified(boolean update)
    {
        this.modified = update;
    }

    /**
     * @return true whether this {@link Executable} can be modified,
     *         false otherwise
     */
    public boolean canBeModified()
    {
        return MODIFIABLE_STATES.contains(state);
    }

    /**
     * @return {@link #migrateFromExecutable}
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "migrate_from_executable_id")
    @Access(AccessType.FIELD)
    public Executable getMigrateFromExecutable()
    {
        return getLazyImplementation(migrateFromExecutable);
    }

    /**
     * @param migrateFromExecutable sets the {@link #migrateFromExecutable}
     */
    public void setMigrateFromExecutable(Executable migrateFromExecutable)
    {
        // Manage bidirectional association
        if (migrateFromExecutable != getMigrateFromExecutable()) {
            if (this.migrateFromExecutable != null) {
                Executable oldMigrateFromExecutable = this.migrateFromExecutable;
                this.migrateFromExecutable = null;
                oldMigrateFromExecutable.setMigrateToExecutable(null);
            }
            if (migrateFromExecutable != null) {
                this.migrateFromExecutable = migrateFromExecutable;
                this.migrateFromExecutable.setMigrateToExecutable(this);
            }
        }
    }

    /**
     * @return {@link #migrateToExecutable}
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "migrate_to_executable_id")
    @Access(AccessType.FIELD)
    public Executable getMigrateToExecutable()
    {
        return getLazyImplementation(migrateToExecutable);
    }

    /**
     * @param migrateToExecutable sets the {@link #migrateToExecutable}
     */
    public void setMigrateToExecutable(Executable migrateToExecutable)
    {
        // Manage bidirectional association
        if (migrateToExecutable != this.migrateToExecutable) {
            if (this.migrateToExecutable != null) {
                Executable oldMigrateToExecutable = this.migrateToExecutable;
                this.migrateToExecutable = null;
                oldMigrateToExecutable.setMigrateFromExecutable(null);
            }
            if (migrateToExecutable != null) {
                this.migrateToExecutable = migrateToExecutable;
                this.migrateToExecutable.setMigrateFromExecutable(this);
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
        return Collections.unmodifiableList(services);
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
        throw new CommonReportSet.ObjectNotExistsException(
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

    @PrePersist
    protected void onCreate()
    {
        if (state == null) {
            state = State.NOT_ALLOCATED;
        }
    }

    @PreRemove
    public void preRemove()
    {
        if (migrateFromExecutable != null) {
            setMigrateFromExecutable(null);
        }
        if (migrateToExecutable != null) {
            setMigrateToExecutable(null);
        }
    }

    @Transient
    @Override
    public String getReportDescription()
    {
        return ObjectIdentifier.formatId(this);
    }

    @Transient
    @Override
    public String getReportContextName()
    {
        return "executable " + ObjectIdentifier.formatId(this);
    }

    @Transient
    @Override
    public String getReportContextDetail()
    {
        return null;
    }

    /**
     * @param entityManager
     * @param administrator
     * @return {@link Executable} converted to {@link cz.cesnet.shongo.controller.api.Executable}
     */
    public final cz.cesnet.shongo.controller.api.Executable toApi(EntityManager entityManager, boolean administrator)
    {
        return toApi(entityManager, administrator ? Report.UserType.DOMAIN_ADMIN : Report.UserType.USER);
    }

    /**
     * @return {@link Executable} converted to {@link cz.cesnet.shongo.controller.api.Executable}
     */
    public cz.cesnet.shongo.controller.api.Executable toApi(EntityManager entityManager, Report.UserType userType)
    {
        cz.cesnet.shongo.controller.api.Executable executableApi = createApi();
        executableApi.setId(ObjectIdentifier.formatId(this));
        toApi(executableApi, entityManager, userType);
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
     * @param executableApi which should be filled from this {@link cz.cesnet.shongo.controller.booking.executable.Executable}
     * @param entityManager
     * @param userType
     */
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, EntityManager entityManager,
            Report.UserType userType)
    {
        executableApi.setId(ObjectIdentifier.formatId(this));
        executableApi.setSlot(getSlot());
        executableApi.setState(getState().toApi());
        executableApi.setStateReport(getExecutionReport(userType));
        if (migrateFromExecutable != null) {
            executableApi.setMigratedExecutable(migrateFromExecutable.toApi(entityManager, userType));
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
        if (state.isStarted()) {
            onAfterStart(executor);
        }
    }

    /**
     * Update this {@link Executable}.
     *
     * @param executor
     * @param executableManager
     * @return new {@link State} or null when the state hasn't changed
     */
    public final Boolean update(Executor executor, ExecutableManager executableManager)
    {
        if (!isModified()) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be updated only if it is modified.", getId()));
        }
        Boolean result = onUpdate(executor, executableManager);
        if (result == null || Boolean.TRUE.equals(result)) {
            setModified(false);
        }
        return result;
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
     * Finalize this {@link Executable}.
     *
     * @param executor
     * @param executableManager
     */
    public final void finalize(Executor executor, ExecutableManager executableManager)
    {
        if (getState().isStarted()) {
            throw new IllegalStateException(
                    String.format("Executable '%d' can be finalized only if it is stopped.", getId()));
        }
        State state = onFinalize(executor, executableManager);
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
     * Event called after executable is successfully started.
     *
     * @param executor
     */
    protected void onAfterStart(Executor executor)
    {
    }

    /**
     * Update this {@link Executable}.
     *
     * @param executor
     * @param executableManager
     * @return {@link Boolean#TRUE} when the updating succeeds, {@link Boolean#FALSE} when it fails and {@code null}
     *         when the updating isn't needed and it was skipped
     */
    protected Boolean onUpdate(Executor executor, ExecutableManager executableManager)
    {
        return Boolean.TRUE;
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
     * Finalize this {@link Executable}.
     *
     * @param executor
     * @param executableManager
     * @return new {@link State}
     */
    protected State onFinalize(Executor executor, ExecutableManager executableManager)
    {
        return State.FINALIZED;
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
     * @param configuration
     * @param entityManager
     * @return true whether same change was made, false otherwise
     * @throws ControllerReportSet.ExecutableInvalidConfigurationException
     *          when this {@link Executable} cannot be updated from given {@code executableConfiguration}
     */
    public boolean updateFromExecutableConfigurationApi(ExecutableConfiguration configuration,
            EntityManager entityManager)
            throws ControllerReportSet.ExecutableInvalidConfigurationException
    {
        throw new ControllerReportSet.ExecutableInvalidConfigurationException(
                ObjectIdentifier.formatId(this), ClassHelper.getClassShortName(configuration.getClass()));
    }

    /**
     * Updates database table executable_summary used mainly for listing reservation requests {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary}.
     * This table is initialized based on view executable_summary_view. For more see init.sql.
     *
     * IMPORTANT: it is necessary to call this method EVERY time change of any entity {@link Executable} is made!!!
     * Otherwise list of reservation requests will be inconsistent.
     *
     * @param entityManager
     * @param deleteOnly
     */
    public void updateExecutableSummary(EntityManager entityManager, boolean deleteOnly)
    {
        entityManager.flush();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        executableManager.updateExecutableSummary(this, deleteOnly);
    }

    /**
     * State of the {@link Executable}.
     */
    public static enum State
    {
        /**
         * {@link Executable} has not been fully allocated (e.g., {@link Executable} is stored for
         * {@link SchedulerReport}).
         */
        NOT_ALLOCATED(false),

        /**
         * {@link Executable} which has not been fully allocated (e.g., {@link Executable} has been stored for
         * {@link ExecutionReport}) and the object for which it has been stored has been deleted
         * and thus the {@link Executable} should be also deleted.
         */
        TO_DELETE(false),

        /**
         * {@link Executable} has not been started yet.
         */
        NOT_STARTED(false),

        /**
         * {@link Executable} starting/stopping was skipped (starting/stopping doesn't make sense).
         */
        SKIPPED(false),

        /**
         * {@link Executable} is started.
         */
        STARTED(true),

        /**
         * {@link Executable} is partially started (e.g., some child executables failed to start).
         */
        PARTIALLY_STARTED(true),

        /**
         * {@link Executable} failed to start.
         */
        STARTING_FAILED(false),

        /**
         * {@link Executable} has been stopped.
         */
        STOPPED(false),

        /**
         * {@link Executable} failed to stop.
         */
        STOPPING_FAILED(true),

        /**
         * All additional resources allocated for the {@link Executable} has been freed.
         */
        FINALIZED(false),

        /**
         * Finalization has failed.
         */
        FINALIZATION_FAILED(false);

        /**
         * Specifies whether state means that executable is started.
         */
        private final boolean started;

        /**
         * Constructor.
         *
         * @param started sets the {@link #started}
         */
        private State(boolean started)
        {
            this.started = started;
        }

        /**
         * @return {@link #started}
         */
        public boolean isStarted()
        {
            return started;
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
                    return ExecutableState.STARTED;
                case STARTING_FAILED:
                    return ExecutableState.STARTING_FAILED;
                case STOPPED:
                case FINALIZED:
                case FINALIZATION_FAILED:
                    return ExecutableState.STOPPED;
                case STOPPING_FAILED:
                    return ExecutableState.STOPPING_FAILED;
                default:
                    throw new RuntimeException("Cannot convert " + this.toString() + " to API.");
            }
        }
    }

    /**
     * {@link State}s in which the {@link Executable} can be modified.
     */
    public static final Set<State> MODIFIABLE_STATES = new HashSet<State>();

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
        for (State state : State.values()) {
            if (state.compareTo(State.STARTED) >= 0 && state.compareTo(State.FINALIZED) < 0) {
                MODIFIABLE_STATES.add(state);
            }
        }
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
