package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.executor.ExecutionReport;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.report.ReportException;

import javax.persistence.*;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Represents a service for an {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class ExecutableService extends ExecutionTarget
{
    /**
     * {@link Executable} for which the {@link ExecutableService} is allocated.
     */
    protected Executable executable;

    /**
     * @see State
     */
    private State state;

    /**
     * @return {@link #executable}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Executable getExecutable()
    {
        return executable;
    }

    /**
     * @param executable sets the {@link #executable}
     */
    public void setExecutable(Executable executable)
    {
        // Manage bidirectional association
        if (executable != this.executable) {
            if (this.executable != null) {
                Executable oldExecutable = this.executable;
                this.executable = null;
                oldExecutable.removeService(this);
            }
            if (executable != null) {
                this.executable = executable;
                this.executable.addService(this);
            }
        }
    }

    /**
     * @return {@link #state}
     */
    @Column(nullable = false, length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @return {@link #state} equals {@link State#ACTIVE}
     */
    @Transient
    public boolean isActive()
    {
        return state.isActive();
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    @Transient
    @Override
    public Collection<? extends ExecutionTarget> getExecutionDependencies()
    {
        Collection<ExecutionTarget> executionTargets = new LinkedList<ExecutionTarget>();
        executionTargets.add(executable);
        return executionTargets;
    }

    /**
     * @return {@link Executable} converted to {@link cz.cesnet.shongo.controller.api.ExecutableService}
     */
    public final cz.cesnet.shongo.controller.api.ExecutableService toApi()
    {
        cz.cesnet.shongo.controller.api.ExecutableService executableServiceApi = createApi();
        toApi(executableServiceApi);
        return executableServiceApi;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.ExecutableService}
     */
    protected abstract cz.cesnet.shongo.controller.api.ExecutableService createApi();

    /**
     * Synchronize to {@link cz.cesnet.shongo.controller.api.ExecutableService}.
     *
     * @param executableServiceApi which should be filled from this {@link ExecutableService}
     */
    public void toApi(cz.cesnet.shongo.controller.api.ExecutableService executableServiceApi)
    {
        executableServiceApi.setId(getId());
        executableServiceApi.setExecutableId(ObjectIdentifier.formatId(executable));
        executableServiceApi.setActive(state.isActive());
        executableServiceApi.setSlot(getSlot());
    }

    /**
     * Activate this {@link ExecutableService}.
     *
     * @param executor
     * @param executableManager
     */
    public final void activate(Executor executor, ExecutableManager executableManager)
    {
        if (getState().equals(State.ACTIVE)) {
            throw new IllegalStateException(
                    String.format("Executable service '%d' can be activated only if it is not activated yet.",
                            getId()));
        }
        State state = onActivate(executor, executableManager);
        setState(state);
    }

    /**
     * Deactivate this {@link ExecutableService}.
     *
     * @param executor
     * @param executableManager
     */
    public final void deactivate(Executor executor, ExecutableManager executableManager)
    {
        if (!isActive()) {
            throw new IllegalStateException(
                    String.format("Executable service '%d' can be deactivated only if it is activated.", getId()));
        }
        State state = onDeactivate(executor, executableManager);
        setState(state);
    }

    /**
     * Check this {@link ExecutableService}.
     *
     * @param executor
     * @param executableManager
     */
    public final void check(Executor executor, ExecutableManager executableManager)
    {
        onCheck(executor, executableManager);
    }

    /**
     * @param executableService which can be used as migration source for this {@link ExecutableService}.
     *                          Only instances of the same class as this {@link ExecutableService} will be passed here.
     * @return true whether migration was successful,
     *         false if the migration cannot be made
     */
    public boolean migrate(ExecutableService executableService)
    {
        return false;
    }

    /**
     * Activate this {@link Executable}.
     *
     * @param executor
     * @param executableManager
     * @return new {@link State}
     */
    protected State onActivate(Executor executor, ExecutableManager executableManager)
    {
        // We set current state as ACTIVE for the executable to see this service as ACTIVE
        State oldState = getState();
        setState(State.ACTIVE);

        // Inform executable about service activation
        try {
            executable.onServiceActivation(this, executor, executableManager);
        }
        catch (ReportException exception) {
            ExecutionReport executionReport = (ExecutionReport) exception.getReport();
            executableManager.createExecutionReport(this, executionReport);
            return State.ACTIVATION_FAILED;
        }
        finally {
            // Return the old state
            setState(oldState);
        }

        return State.ACTIVE;
    }

    /**
     * Deactivate this {@link Executable}.
     *
     * @param executor
     * @param executableManager
     * @return new {@link State}
     */
    protected State onDeactivate(Executor executor, ExecutableManager executableManager)
    {
        // We set current state as NOT_ACTIVE for the executable to see this service as NOT_ACTIVE
        State oldState = getState();
        setState(State.NOT_ACTIVE);

        // Inform executable about service activation
        try {
            executable.onServiceDeactivation(this, executor, executableManager);
        }
        catch (ReportException exception) {
            ExecutionReport executionReport = (ExecutionReport) exception.getReport();
            executableManager.createExecutionReport(this, executionReport);
            return State.DEACTIVATION_FAILED;
        }
        finally {
            // Return the old state
            setState(oldState);
        }

        return State.NOT_ACTIVE;
    }

    /**
     * Check this {@link ExecutableService}.
     *
     * @param executor
     * @param executableManager
     */
    protected void onCheck(Executor executor, ExecutableManager executableManager)
    {
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
        return getClass().getSimpleName() + " " + getId();
    }

    @Transient
    @Override
    public String getReportContextDetail()
    {
        return null;
    }

    /**
     * Enumeration of possible states for {@link ExecutableService}.
     */
    public static enum State
    {
        /**
         * {@link ExecutableService} isn't active (isn't started)
         */
        NOT_ACTIVE(false),

        /**
         * {@link ExecutableService} should be started.
         */
        PREPARED(false),

        /**
         * {@link ExecutableService} is active (is started).
         */
        ACTIVE(true),

        /**
         * {@link ExecutableService} activation failed.
         */
        ACTIVATION_FAILED(false),

        /**
         * {@link ExecutableService} deactivation failed.
         */
        DEACTIVATION_FAILED(true);

        /**
         * Specifies whether {@link ExecutableService} is active.
         */
        private boolean active;

        /**
         * Constructor.
         *
         * @param active sets the {@link #active}
         */
        private State(boolean active)
        {
            this.active = active;
        }

        /**
         * @return {@link #active}
         */
        public boolean isActive()
        {
            return active;
        }
    }
}
