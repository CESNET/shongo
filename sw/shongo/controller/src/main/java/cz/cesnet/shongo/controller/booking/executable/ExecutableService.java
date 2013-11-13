package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Executor;

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
    @Column(nullable = false)
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
        executableServiceApi.setActive(State.ACTIVE.equals(state));
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
        if (!getState().equals(State.ACTIVE)) {
            throw new IllegalStateException(
                    String.format("Executable service '%d' can be deactivated only if it is activated.", getId()));
        }
        State state = onDeactivate(executor, executableManager);
        setState(state);
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
        return State.NOT_ACTIVE;
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
     * Enumeration of possible states for {@link ExecutableService}.
     */
    public static enum State
    {
        /**
         * {@link ExecutableService} isn't active (isn't started)
         */
        NOT_ACTIVE,

        /**
         * {@link ExecutableService} should be started.
         */
        PREPARED,

        /**
         * {@link ExecutableService} is active (is started).
         */
        ACTIVE,

        /**
         * {@link ExecutableService} activation failed.
         */
        ACTIVATION_FAILED,

        /**
         * {@link ExecutableService} deactivation failed.
         */
        DEACTIVATION_FAILED
    }
}
