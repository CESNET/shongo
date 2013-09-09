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
     * Specifies whether this {@link ExecutionAction} should not be performed (e.g., a migration disables this action
     * and performs some other action).
     */
    private boolean skipPerform = false;

    /**
     * Constructor.
     */
    public ExecutionAction()
    {
    }

    /**
     * @param executionPlan sets the {@link #executionPlan}
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
     * @return priority of this {@link ExecutionAction} (highest number means highest priority)
     */
    public int getExecutionPriority()
    {
        return PRIORITY_DEFAULT;
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
     * @return {@link #skipPerform}
     */
    public boolean isSkipPerform()
    {
        return skipPerform;
    }

    /**
     * @param skipPerform sets the {@link #skipPerform}
     */
    public void setSkipPerform(boolean skipPerform)
    {
        this.skipPerform = skipPerform;
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

    /**
     * Build dependencies to other {@link ExecutionAction}s from the {@link #executionPlan}.
     */
    public abstract void buildDependencies();

    /**
     * Perform this {@link ExecutionAction}.
     *
     * @param executableManager
     */
    protected abstract void perform(ExecutableManager executableManager);

    /**
     * Event called after initialization of the action.
     */
    protected void afterInit()
    {
    }

    /**
     * @param entityManager
     * @param executionResult to be filled
     * @return true whether this action succeeds,
     *         false otherwise
     */
    public abstract boolean finish(EntityManager entityManager, ExecutionResult executionResult);

    @Override
    public abstract String toString();

    @Override
    public void run()
    {
        if (executionPlan == null) {
            throw new IllegalStateException("Execution action is not properly initialized.");
        }
        if (skipPerform) {
            throw new IllegalStateException("Execution action should not be performed it is marked for skipping.");
        }
        EntityManager entityManager = getExecutor().getEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        try {
            entityManager.getTransaction().begin();

            // Perform action
            perform(executableManager);

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
            // Remove action from plan
            executionPlan.removeExecutionAction(this);

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
                ExecutionAction childExecutionAction = executionPlan.getActionByExecutable(childExecutable);

                // Child executable doesn't exists in the plan, so it is automatically satisfied
                if (childExecutionAction == null) {
                    continue;
                }

                // Create dependency
                createDependency(this, childExecutionAction);
            }
        }

        @Override
        protected void afterInit()
        {
            super.afterInit();

            if (executionPlan.getActionByExecutable(executable) != null) {
                throw new IllegalStateException("Executable already exists in the execution plan.");
            }
            executionPlan.setActionByExecutable(executable, this);
        }

        @Override
        public final boolean finish(EntityManager entityManager, ExecutionResult executionResult)
        {
            entityManager.refresh(executable);
            if (executable.getState().equals(Executable.State.SKIPPED)) {
                executable.setState(executable.getDefaultState());
            }

            if (performFinish(executionResult)) {
                executable.setNextAttempt(null);
                executable.setAttemptCount(0);
                return true;
            }
            else {
                executable.setNextAttempt(null);
                executable.setAttemptCount(executable.getAttemptCount() + 1);

                ExecutableReport lastReport = executable.getLastReport();
                if (lastReport != null && lastReport.getResolution().equals(Report.Resolution.TRY_AGAIN)) {
                    Executor executor = getExecutor();
                    if (executable.getAttemptCount() < executor.getMaxAttemptCount()) {
                        executable.setNextAttempt(DateTime.now().plus(executor.getNextAttempt()));
                    }
                }
                return false;
            }
        }

        /**
         * @param executionResult to be filled
         * @return true whether this action succeeds,
         *         false otherwise
         */
        protected abstract boolean performFinish(ExecutionResult executionResult);
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
            return PRIORITY_START;
        }

        @Override
        protected void perform(ExecutableManager executableManager)
        {
            try {
                Executor executor = getExecutor();
                Executable executable = executableManager.get(this.executable.getId());
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
                }
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.EXECUTOR, "Starting failed", exception);
            }
        }

        @Override
        protected boolean performFinish(ExecutionResult executionResult)
        {
            if (executable.getState().isStarted()) {
                executionResult.addStartedExecutable(executable);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public String toString()
        {
            return String.format("Start [exe:%d]", executable.getId());
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
            return PRIORITY_UPDATE;
        }

        @Override
        protected void perform(ExecutableManager executableManager)
        {
            try {
                Executable executable = executableManager.get(this.executable.getId());
                executable.update(getExecutor(), executableManager);
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.EXECUTOR, "Updating failed", exception);
            }
        }

        @Override
        protected boolean performFinish(ExecutionResult executionResult)
        {
            if (!executable.getState().isModified()) {
                executionResult.addUpdatedExecutable(executable);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public String toString()
        {
            return String.format("Update [exe:%d]", executable.getId());
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
            return PRIORITY_STOP;
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
        protected void perform(ExecutableManager executableManager)
        {
            try {
                Executable executable = executableManager.get(this.executable.getId());
                executable.stop(getExecutor(), executableManager);
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.EXECUTOR, "Stopping failed", exception);
            }
        }

        @Override
        protected boolean performFinish(ExecutionResult executionResult)
        {
            if (!executable.getState().isStarted()) {
                executionResult.addStoppedExecutable(executable);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public String toString()
        {
            return String.format("Stop [exe:%d]", executable.getId());
        }
    }

    /**
     * {@link ExecutionAction} for stopping {@link Executable}.
     */
    public static class MigrationAction extends ExecutionAction
    {
        /**
         * {@link Migration} which should be performed.
         */
        private Migration migration;

        /**
         * Constructor.
         *
         * @param migration
         */
        public MigrationAction(Migration migration)
        {
            this.migration = migration;
        }

        /**
         * @return {@link #migration}
         */
        public Migration getMigration()
        {
            return migration;
        }

        @Override
        public int getExecutionPriority()
        {
            return PRIORITY_MIGRATE;
        }

        @Override
        public void buildDependencies()
        {
            ExecutionAction sourceAction = executionPlan.getActionByExecutable(migration.getSourceExecutable());
            if (!(sourceAction instanceof StopExecutableAction)) {
                throw new RuntimeException("Source executable is not planned for stopping.");
            }
            ExecutionAction targetAction = executionPlan.getActionByExecutable(migration.getTargetExecutable());
            if (!(targetAction instanceof StartExecutableAction)) {
                throw new RuntimeException("Target executable is not planned for starting.");
            }
            if (migration.isReplacement()) {
                sourceAction.setSkipPerform(true);
                targetAction.setSkipPerform(true);
            }
            else {
                createDependency(sourceAction, this);
                createDependency(this, targetAction);
            }
        }

        @Override
        protected void perform(ExecutableManager executableManager)
        {
            migration.perform(getExecutor(), executableManager);
        }

        @Override
        public boolean finish(EntityManager entityManager, ExecutionResult executionResult)
        {
            return true;
        }

        @Override
        public String toString()
        {
            return String.format("Migration [from-exe:%d, to-exe:%d]",
                    migration.getSourceExecutable().getId(), migration.getTargetExecutable().getId());
        }
    }

    /**
     * Action Priority.
     */
    private static final int PRIORITY_STOP = 4;
    private static final int PRIORITY_MIGRATE = 3;
    private static final int PRIORITY_START = 2;
    private static final int PRIORITY_UPDATE = 1;
    private static final int PRIORITY_DEFAULT = 0;
}
