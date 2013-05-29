package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an {@link Executor} action.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ExecutionAction extends Thread
{
    /**
     * {@link ExecutionPlan} from which the {@link ExecutionAction} should be removed when it is done.
     */
    protected ExecutionPlan executionPlan;

    /**
     * Set of {@link ExecutionAction}s which are required by this {@link ExecutionAction}
     * (e.g., must be performed before this action is performed).
     */
    final Set<ExecutionAction> dependencies = new HashSet<ExecutionAction>();

    /**
     * Set of {@link ExecutionAction}s which requires this {@link ExecutionAction}
     * (e.g., must be performed after this action is performed).
     */
    final Set<ExecutionAction> parents = new HashSet<ExecutionAction>();

    /**
     * Constructor.
     */
    public ExecutionAction()
    {
    }

    /**
     * @param executionPlan   sets the {@link #executionPlan}
     */
    public void init(ExecutionPlan executionPlan)
    {
        if (executionPlan == null) {
            throw new IllegalArgumentException("Execution plan must not be null.");
        }
        this.executionPlan = executionPlan;

        afterInit();
    }

    /**
     * @return {@link Executor}
     */
    public Executor getExecutor()
    {
        return executionPlan.getExecutor();
    }

    /**
     * @return {@link ExecutionResult}
     */
    public ExecutionResult getExecutionResult()
    {
        return executionPlan.getExecutionResult();
    }

    /**
     * @return priority of this {@link ExecutionAction} (highest number means highest priority)
     */
    public int getExecutionPriority()
    {
        return 0;
    }

    /**
     * @return true if this {@link ExecutionAction}s has some parents ({@link #parents}),
     *         false otherwise
     */
    public boolean hasParents()
    {
        return !parents.isEmpty();
    }

    /**
     * Build dependencies to other {@link ExecutionAction}s from the {@link #executionPlan}.
     */
    public abstract void buildDependencies();

    /**
     * Perform this {@link ExecutionAction}.
     *
     * @param executor
     * @param executableManager
     * @return true whether action succeeded,
     *         otherwise false
     */
    protected abstract boolean perform(Executor executor, ExecutableManager executableManager);

    /**
     * Event called after initialization of the action.
     */
    protected void afterInit()
    {
    }

    /**
     * Event called after the execution succeeds.
     */
    protected void afterSuccess()
    {
    }

    /**
     * Event called after the execution fails.
     */
    protected void afterFailure()
    {
    }

    @Override
    public void run()
    {
        if (executionPlan == null) {
            throw new IllegalStateException("Execution action is not properly initialized.");
        }
        Executor executor = getExecutor();
        EntityManager entityManager = executor.getEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        try {
            entityManager.getTransaction().begin();

            // Perform action
            if (perform(executor, executableManager)) {
                afterSuccess();
            }
            else {
                afterFailure();
            }

            // Remove action from plan
            executionPlan.removeExecutionAction(this);

            entityManager.getTransaction().commit();

            // Reporting
            for (ExecutableReport executableReport : executableManager.getExecutableReports()) {
                Reporter.report(executableReport.getExecutable(), executableReport);
            }
        }
        catch (Exception exception) {
            Reporter.reportInternalError(Reporter.EXECUTOR, exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    /**
     * Abstract {@link ExecutionAction} for {@link Executable}s.
     */
    public static abstract class AbstractExecutableAction extends ExecutionAction
    {
        /**
         * {@link Executable} which should be started ({@link Executable#start} should be invoked).
         */
        protected Executable executable;

        /**
         * Constructor.
         *
         * @param executable sets the {@link #executable}
         */
        public AbstractExecutableAction(Executable executable)
        {
            this.executable = executable;
        }

        /**
         * @return {@link #executable}
         */
        public Executable getExecutable()
        {
            return executable;
        }

        @Override
        public void buildDependencies()
        {
            // Setup dependencies in this action and parents in child actions
            for (Executable childExecutable : executable.getExecutionDependencies()) {
                ExecutionAction childExecutionAction = executionPlan.actionByExecutableId.get(childExecutable.getId());

                // Child executable doesn't exists in the plan, so it is automatically satisfied
                if (childExecutionAction == null) {
                    continue;
                }

                // Create dependency
                createDependency(this, childExecutionAction);
            }
        }

        /**
         * Create dependency from {@code actionFrom} to {@code actionTo}.
         *
         * @param actionFrom
         * @param actionTo
         */
        protected void createDependency(ExecutionAction actionFrom, ExecutionAction actionTo)
        {
            // This action is dependent to all child actions (requires them)
            actionFrom.dependencies.add(actionTo);
            // Child action has new parent (is required by him)
            actionTo.parents.add(actionFrom);
        }

        @Override
        protected void afterInit()
        {
            super.afterInit();

            Long executableId = executable.getId();
            if (executionPlan.actionByExecutableId.containsKey(executableId)) {
                throw new IllegalStateException("Executable already exists in the execution plan.");
            }
            executionPlan.actionByExecutableId.put(executableId, this);
        }

        @Override
        protected void afterSuccess()
        {
            super.afterSuccess();

            executable.setNextAttempt(null);
            executable.setAttemptCount(0);
        }

        @Override
        protected void afterFailure()
        {
            super.afterFailure();

            executable.setNextAttempt(null);

            ExecutableReport lastReport = executable.getLastReport();
            if (lastReport != null && lastReport.getResolution().equals(Report.Resolution.TRY_AGAIN)) {
                Executor executor = getExecutor();
                if ((executable.getAttemptCount() + 1) < executor.getMaxAttemptCount()) {
                    executable.setNextAttempt(DateTime.now().plus(executor.getNextAttempt()));
                    executable.setAttemptCount(executable.getAttemptCount() + 1);
                }
            }
        }
    }

    /**
     * {@link ExecutionAction} for starting {@link Executable}.
     */
    public static class StartExecutableAction extends AbstractExecutableAction
    {
        /**
         * Constructor.
         *
         * @param executable sets the {@link #executable}
         */
        public StartExecutableAction(Executable executable)
        {
            super(executable);
        }

        @Override
        public int getExecutionPriority()
        {
            return 2;
        }

        @Override
        protected boolean perform(Executor executor, ExecutableManager executableManager)
        {
            try {
                executable = executableManager.get(this.executable.getId());
                executable.start(executor, executableManager);
                if (executable.getState().isStarted()) {
                    if (executable instanceof RoomEndpoint && hasParents()) {
                        executor.getLogger().info("Waiting for room '{}' to be created...", executable.getId());
                        try {
                            Thread.sleep(executor.getStartingDurationRoom().getMillis());
                        }
                        catch (InterruptedException exception) {
                            executor.getLogger().error("Waiting for room was interrupted...", exception);
                        }
                    }
                    return true;
                }
                else {
                    return false;
                }
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.EXECUTOR, "Starting failed", exception);
                return false;
            }
        }

        @Override
        protected void afterSuccess()
        {
            super.afterSuccess();

            getExecutionResult().addStartedExecutable(executable);
        }
    }

    /**
     * {@link ExecutionAction} for updating {@link Executable}.
     */
    public static class UpdateExecutableAction extends AbstractExecutableAction
    {
        /**
         * Constructor.
         *
         * @param executable sets the {@link #executable}
         */
        public UpdateExecutableAction(Executable executable)
        {
            super(executable);
        }

        @Override
        public int getExecutionPriority()
        {
            return 1;
        }

        @Override
        protected boolean perform(Executor executor, ExecutableManager executableManager)
        {
            try {
                executable = executableManager.get(this.executable.getId());
                executable.update(executor, executableManager);
                return !executable.getState().isModified();
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.EXECUTOR, "Updating failed", exception);
                return false;
            }
        }

        @Override
        protected void afterSuccess()
        {
            super.afterSuccess();

            getExecutionResult().addUpdatedExecutable(executable);
        }
    }

    /**
     * {@link ExecutionAction} for stopping {@link Executable}.
     */
    public static class StopExecutableAction extends AbstractExecutableAction
    {
        /**
         * Constructor.
         *
         * @param executable sets the {@link #executable}
         */
        public StopExecutableAction(Executable executable)
        {
            super(executable);
        }

        @Override
        public int getExecutionPriority()
        {
            return 3;
        }

        @Override
        protected void createDependency(ExecutionAction actionFrom, ExecutionAction actionTo)
        {
            if (actionFrom instanceof StopExecutableAction && actionTo instanceof StopExecutableAction) {
                // Stopping must be done in reverse order than starting
                super.createDependency(actionTo, actionFrom);
            }
            else {
                super.createDependency(actionFrom, actionTo);
            }
        }

        @Override
        protected boolean perform(Executor executor, ExecutableManager executableManager)
        {
            try {
                executable = executableManager.get(this.executable.getId());
                executable.stop(executor, executableManager);
                return !executable.getState().isStarted();
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.EXECUTOR, "Stopping failed", exception);
                return false;
            }
        }

        @Override
        protected void afterSuccess()
        {
            super.afterSuccess();

            getExecutionResult().addStoppedExecutable(executable);
        }
    }
}
