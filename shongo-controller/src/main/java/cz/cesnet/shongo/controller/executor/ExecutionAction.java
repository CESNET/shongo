package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.booking.executable.*;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.util.DatabaseHelper;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an {@link Executor} action.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ExecutionAction<T> extends Thread
{
    /**
     * Target for which the {@link ExecutionAction} is being executed.
     */
    protected T target;

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
     *
     * @param target sets the {@link #target}
     */
    public ExecutionAction(T target)
    {
        this.target = target;
    }

    /**
     * @return {@link #target}
     */
    public T getTarget()
    {
        return target;
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
    protected void createDependency(ExecutionAction<?> actionFrom, ExecutionAction<?> actionTo)
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
     *
     * @param entityManager
     * @param referenceDateTime
     *@param executionResult to be filled  @return true whether this action succeeds,
     *         false otherwise
     */
    public abstract boolean finish(EntityManager entityManager, DateTime referenceDateTime,
            ExecutionResult executionResult);

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
        Reporter reporter = Reporter.getInstance();
        try {
            entityManager.getTransaction().begin();

            // Perform action
            perform(executableManager);

            entityManager.getTransaction().commit();

            // Reporting
            for (cz.cesnet.shongo.controller.executor.ExecutionReport executionReport : executableManager.getExecutionReports()) {
                reporter.report(executionReport.getExecutionTarget(), executionReport);
            }
        }
        catch (Exception exception) {
            reporter.reportInternalError(Reporter.EXECUTOR, exception);
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
     * Abstract {@link ExecutionAction} for {@link ExecutableService}s.
     */
    public static abstract class AbstractExecutionTargetAction<T extends ExecutionTarget> extends ExecutionAction<T>
    {
        /**
         * Constructor.
         *
         * @param executionTarget sets the {@link #target}
         */
        public AbstractExecutionTargetAction(T executionTarget)
        {
            super(executionTarget);
        }

        @Override
        public void buildDependencies()
        {
            // Setup dependencies in this action and parents in child actions
            for (ExecutionTarget childExecutionTarget : target.getExecutionDependencies()) {
                ExecutionAction childExecutionAction = executionPlan.getActionByExecutionTarget(childExecutionTarget);

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

            if (executionPlan.getActionByExecutionTarget(target) != null) {
                throw new IllegalStateException("Execution target already exists in the execution plan.");
            }
            executionPlan.setActionByExecutionTarget(target, this);
        }

        @Override
        public boolean finish(EntityManager entityManager, DateTime referenceDateTime,
                ExecutionResult executionResult)
        {
            return performFinish(executionResult);
        }

        /**
         * @param executionResult to be filled
         * @return true whether this action succeeds,
         *         false otherwise
         */
        protected abstract boolean performFinish(ExecutionResult executionResult);
    }

    /**
     * Abstract {@link ExecutionAction} for {@link Executable}s.
     */
    public static abstract class AbstractExecutableAction extends AbstractExecutionTargetAction<Executable>
    {
        /**
         * Constructor.
         *
         * @param executable sets the {@link #target}
         */
        public AbstractExecutableAction(Executable executable)
        {
            super(executable);
        }

        @Override
        public final boolean finish(EntityManager entityManager, DateTime referenceDateTime,
                ExecutionResult executionResult)
        {
            if (target.getState().equals(Executable.State.SKIPPED)) {
                target.setState(target.getDefaultState());
            }

            boolean result = super.finish(entityManager, referenceDateTime, executionResult);

            target.updateExecutableSummary(entityManager, false);

            return result;
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
         * @param executable sets the {@link #target}
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
                Executable executable = executableManager.get(this.target.getId());
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
                Reporter.getInstance().reportInternalError(Reporter.EXECUTOR, "Starting failed", exception);
            }
        }

        @Override
        protected boolean performFinish(ExecutionResult executionResult)
        {
            if (target.getState().isStarted()) {
                executionResult.addStartedExecutable(target);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public String toString()
        {
            return String.format("Start [exe:%d]", target.getId());
        }
    }

    /**
     * {@link ExecutionAction} for updating {@link Executable}.
     */
    public static class UpdateExecutableAction extends AbstractExecutableAction
    {
        private Boolean result;

        /**
         * Constructor.
         *
         * @param executable sets the {@link #target}
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
                Executable executable = executableManager.get(this.target.getId());
                result = executable.update(getExecutor(), executableManager);
            }
            catch (Exception exception) {
                Reporter.getInstance().reportInternalError(Reporter.EXECUTOR, "Updating failed", exception);
            }
        }

        @Override
        protected boolean performFinish(ExecutionResult executionResult)
        {
            if (!target.isModified()) {
                if (Boolean.TRUE.equals(result)) {
                    executionResult.addUpdatedExecutable(target);
                }
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public String toString()
        {
            return String.format("Update [exe:%d]", target.getId());
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
         * @param executable sets the {@link #target}
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
                Executable executable = executableManager.get(this.target.getId());
                executable.stop(getExecutor(), executableManager);
            }
            catch (Exception exception) {
                Reporter.getInstance().reportInternalError(Reporter.EXECUTOR, "Stopping failed", exception);
            }
        }

        @Override
        protected boolean performFinish(ExecutionResult executionResult)
        {
            if (!target.getState().isStarted()) {
                executionResult.addStoppedExecutable(target);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public String toString()
        {
            return String.format("Stop [exe:%d]", target.getId());
        }
    }

    /**
     * {@link ExecutionAction} for finalizing {@link Executable}.
     */
    public static class FinalizeExecutableAction extends AbstractExecutableAction
    {
        /**
         * Constructor.
         *
         * @param executable sets the {@link #target}
         */
        public FinalizeExecutableAction(Executable executable)
        {
            super(executable);
        }

        @Override
        protected void perform(ExecutableManager executableManager)
        {
            try {
                Executable executable = executableManager.get(this.target.getId());
                executable.finalize(getExecutor(), executableManager);
            }
            catch (Exception exception) {
                Reporter.getInstance().reportInternalError(Reporter.EXECUTOR, "Finalizing failed", exception);
            }
        }

        @Override
        protected boolean performFinish(ExecutionResult executionResult)
        {
            if (target.getState().equals(Executable.State.FINALIZED)) {
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public String toString()
        {
            return String.format("Finalize [exe:%d]", target.getId());
        }
    }

    /**
     * {@link ExecutionAction} for stopping {@link Executable}.
     */
    public static class MigrationAction extends ExecutionAction<Migration>
    {
        /**
         * Constructor.
         *
         * @param migration
         */
        public MigrationAction(Migration migration)
        {
            super(migration);
        }

        @Override
        public int getExecutionPriority()
        {
            return PRIORITY_MIGRATE;
        }

        @Override
        public void buildDependencies()
        {
            ExecutionAction sourceAction = executionPlan.getActionByExecutionTarget(target.getSourceExecutable());
            if (!(sourceAction instanceof StopExecutableAction)) {
                throw new RuntimeException("Source executable is not planned for stopping.");
            }
            ExecutionAction targetAction = executionPlan.getActionByExecutionTarget(target.getTargetExecutable());
            if (!(targetAction instanceof StartExecutableAction)) {
                throw new RuntimeException("Target executable is not planned for starting.");
            }
            if (target.isReplacement()) {
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
            target.perform(getExecutor(), executableManager);
        }

        @Override
        public boolean finish(EntityManager entityManager, DateTime referenceDateTime, ExecutionResult executionResult)
        {
            target.getSourceExecutable().updateExecutableSummary(entityManager, false);
            target.getTargetExecutable().updateExecutableSummary(entityManager, false);
            return true;
        }

        @Override
        public String toString()
        {
            return String.format("Migration [from-exe:%d, to-exe:%d]",
                    target.getSourceExecutable().getId(), target.getTargetExecutable().getId());
        }

    }

    /**
     * {@link ExecutionAction} for activation of {@link ExecutableService}.
     */
    public static class ActivateExecutableServiceAction extends AbstractExecutionTargetAction<ExecutableService>
    {
        /**
         * Constructor.
         *
         * @param executableService sets the {@link #target}
         */
        public ActivateExecutableServiceAction(ExecutableService executableService)
        {
            super(executableService);
        }

        @Override
        public int getExecutionPriority()
        {
            return PRIORITY_ACTIVATE;
        }

        @Override
        protected void perform(ExecutableManager executableManager)
        {
            try {
                Executor executor = getExecutor();
                ExecutableService executableService = executableManager.getService(this.target.getId());
                executableService.activate(executor, executableManager);
            }
            catch (Exception exception) {
                Reporter.getInstance().reportInternalError(Reporter.EXECUTOR, "Activation executable service failed",
                        exception);
            }
        }

        @Override
        protected boolean performFinish(ExecutionResult executionResult)
        {
            if (target.getState().equals(ExecutableService.State.ACTIVE)) {
                executionResult.addActivatedExecutableService(target);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public boolean finish(EntityManager entityManager, DateTime referenceDateTime, ExecutionResult executionResult)
        {
            boolean result = super.finish(entityManager, referenceDateTime, executionResult);

            target.getExecutable().updateExecutableSummary(entityManager, false);

            return result;
        }

        @Override
        public String toString()
        {
            return String.format("Activate [srv:%d] for [exe:%d]", target.getId(), target.getExecutable().getId());
        }
    }

    /**
     * {@link ExecutionAction} for deactivation of {@link ExecutableService}.
     */
    public static class DeactivateExecutableServiceAction extends AbstractExecutionTargetAction<ExecutableService>
    {
        /**
         * Constructor.
         *
         * @param executableService sets the {@link #target}
         */
        public DeactivateExecutableServiceAction(ExecutableService executableService)
        {
            super(executableService);
        }

        @Override
        public int getExecutionPriority()
        {
            return PRIORITY_DEACTIVATE;
        }

        @Override
        protected void createDependency(ExecutionAction actionFrom, ExecutionAction actionTo)
        {
            // Deactivation must be done in reverse order than activation
            ExecutionAction deactivationActionFrom = actionTo;
            ExecutionAction deactivationActionTo = actionFrom;
            super.createDependency(deactivationActionFrom, deactivationActionTo);
        }

        @Override
        protected void perform(ExecutableManager executableManager)
        {
            try {
                Executor executor = getExecutor();
                ExecutableService executableService = executableManager.getService(this.target.getId());
                executableService.deactivate(executor, executableManager);
            }
            catch (Exception exception) {
                Reporter.getInstance().reportInternalError(Reporter.EXECUTOR, "Deactivation executable service failed",
                        exception);
            }
        }

        @Override
        protected boolean performFinish(ExecutionResult executionResult)
        {
            if (target.getState().equals(ExecutableService.State.NOT_ACTIVE)) {
                executionResult.addDeactivatedExecutableService(target);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public boolean finish(EntityManager entityManager, DateTime referenceDateTime, ExecutionResult executionResult)
        {
            boolean result = super.finish(entityManager, referenceDateTime, executionResult);

            target.getExecutable().updateExecutableSummary(entityManager, false);

            return result;
        }

        @Override
        public String toString()
        {
            return String.format("DeActivate [srv:%d] for [exe:%d]", target.getId(), target.getExecutable().getId());
        }
    }

    /**
     * {@link ExecutionAction} for checking {@link Executable}.
     */
    public static class CheckExecutableServiceAction extends ExecutionAction<ExecutableService>
    {
        /**
         * Constructor.
         *
         * @param executableService sets the {@link #target}
         */
        public CheckExecutableServiceAction(ExecutableService executableService)
        {
            super(executableService);
        }

        @Override
        public void buildDependencies()
        {
        }

        @Override
        protected void perform(ExecutableManager executableManager)
        {
            try {
                Executor executor = getExecutor();
                ExecutableService executableService = executableManager.getService(this.target.getId());
                executableService.check(executor, executableManager);
            }
            catch (Exception exception) {
                Reporter.getInstance().reportInternalError(Reporter.EXECUTOR, "Check executable service failed",
                        exception);
            }
        }

        @Override
        public boolean finish(EntityManager entityManager, DateTime referenceDateTime, ExecutionResult executionResult)
        {
            return true;
        }

        @Override
        public String toString()
        {
            return String.format("Check [srv:%d] for [exe:%d]", target.getId(), target.getExecutable().getId());
        }
    }

    /**
     * Action Priority.
     */
    private static final int PRIORITY_DEACTIVATE = 6;
    private static final int PRIORITY_STOP = 5;
    private static final int PRIORITY_MIGRATE = 4;
    private static final int PRIORITY_START = 3;
    private static final int PRIORITY_UPDATE = 2;
    private static final int PRIORITY_ACTIVATE = 1;
    private static final int PRIORITY_DEFAULT = 0;

}
