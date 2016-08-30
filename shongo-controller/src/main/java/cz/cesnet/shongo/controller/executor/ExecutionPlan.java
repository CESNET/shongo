package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.executable.ExecutionTarget;
import cz.cesnet.shongo.report.AbstractReport;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Represents an {@link Executor} plan for collection of {@link Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutionPlan
{
    private static Logger logger = LoggerFactory.getLogger(ExecutionPlan.class);

    /**
     * @see Executor
     */
    private Executor executor;

    /**
     * Set of {@link ExecutionAction}s which haven't been popped yet.
     */
    private final Set<ExecutionAction> remainingActions = new HashSet<ExecutionAction>();

    /**
     * Set of {@link ExecutionAction}s with satisfied dependencies (with empty {@link ExecutionAction#dependencies})
     * which haven't been popped yet.
     */
    protected Set<ExecutionAction> satisfiedActions;

    /**
     * Set of popped {@link ExecutionAction}s (by {@link #popExecutionActions()}) which has been returned for starting
     * and which has not been completed yet (by {@link #removeExecutionAction(ExecutionAction)}).
     */
    private final List<ExecutionAction> poppedActions = new LinkedList<ExecutionAction>();

    /**
     * Set of completed {@link ExecutionAction}s (by {@link #popExecutionActions()}).
     */
    private final List<ExecutionAction> completedActions = new LinkedList<ExecutionAction>();

    /**
     * Map of {@link ExecutionAction} by {@link Executable} (used by {@link ExecutionAction}s for building dependencies).
     */
    final Map<Long, ExecutionAction.AbstractExecutionTargetAction> actionByExecutionTargetId =
            new HashMap<Long, ExecutionAction.AbstractExecutionTargetAction>();

    /**
     * Constructor.
     *
     * @param executor sets the {@link #executor}
     */
    public ExecutionPlan(Executor executor)
    {
        this.executor = executor;
    }

    /**
     * @return {@link #executor}
     */
    public Executor getExecutor()
    {
        return executor;
    }

    /**
     * @return {@link #poppedActions}
     */
    public Collection<ExecutionAction> getPoppedActions()
    {
        return poppedActions;
    }

    /**
     * @param executionAction to be added to the {@link ExecutionPlan}
     */
    public void addExecutionAction(ExecutionAction executionAction) throws RuntimeException
    {
        remainingActions.add(executionAction);
        executionAction.init(this);
    }

    /**
     * @param executionTarget
     * @return {@link ExecutionAction.AbstractExecutableAction} for given {@code executable}
     */
    public ExecutionAction.AbstractExecutionTargetAction getActionByExecutionTarget(ExecutionTarget executionTarget)
    {
        return actionByExecutionTargetId.get(executionTarget.getId());
    }

    /**
     * @param executionTarget
     * @param executionAction to be set from given {@code executable}
     */
    public void setActionByExecutionTarget(ExecutionTarget executionTarget,
            ExecutionAction.AbstractExecutionTargetAction executionAction)
    {
        actionByExecutionTargetId.put(executionTarget.getId(), executionAction);
    }

    /**
     * Build the {@link ExecutionPlan}.
     *
     * @throws RuntimeException when the plan cannot be constructed (because of cycle)
     */
    public void build()
    {
        satisfiedActions = new HashSet<ExecutionAction>();

        // Execution plan is empty
        if (remainingActions.size() == 0) {
            return;
        }

        // Setup dependencies and remaining actions
        for (ExecutionAction executionAction : remainingActions) {
            executionAction.buildDependencies();
        }

        // Compute initial satisfied executables
        for (ExecutionAction executionAction : remainingActions) {
            if (executionAction.dependencies.size() == 0) {
                // Action is satisfied (because it is not dependent on any other action)
                satisfiedActions.add(executionAction);
            }
            else {
                // Action is not satisfied (because it is dependent to at least one other action)
            }
        }
        remainingActions.removeAll(satisfiedActions);

        // Check for cycles
        if (satisfiedActions.size() == 0) {
            throw new RuntimeException("Execution plan cannot be constructed (contains a cycle).");
        }
    }

    /**
     * @return collection of {@link ExecutionAction}s which have satisfied dependencies and have highest priority
     *         and remove them from the current {@link ExecutionPlan} queue
     */
    public synchronized Set<ExecutionAction> popExecutionActions()
    {
        Set<ExecutionAction> executionActions = new HashSet<ExecutionAction>();

        // Determine highest priority of actions which has been popped but not completed
        int currentPriority = 0;
        for (ExecutionAction executionAction : poppedActions) {
            int actionPriority = executionAction.getExecutionPriority();
            if (actionPriority > currentPriority) {
                currentPriority = actionPriority;
            }
        }
        // Find actions for popping
        for (ExecutionAction executionAction : new LinkedList<ExecutionAction>(satisfiedActions)) {
            if (executionAction.isSkipPerform()) {
                completeExecutionAction(executionAction);
                satisfiedActions.remove(executionAction);
                continue;
            }
            int actionPriority = executionAction.getExecutionPriority();
            if (actionPriority < currentPriority) {
                // Skip actions with lower priority than actions which are already added to the set
                continue;
            }
            if (actionPriority > currentPriority) {
                // Remove actions with lower priority than new action
                executionActions.clear();
                // Set new priority
                currentPriority = actionPriority;
            }
            executionActions.add(executionAction);
        }

        // Remove actions satisfied actions
        satisfiedActions.removeAll(executionActions);

        // Added popped actions
        poppedActions.addAll(executionActions);

        for (ExecutionAction executionAction : executionActions) {
            logger.debug("{} prepared.", executionAction);
        }

        return executionActions;
    }

    /**
     * @see #popExecutionActions()
     */
    public <T extends ExecutionAction> Set<T> popExecutionActions(Class<T> type)
    {
        Set<T> executionActions = new HashSet<T>();
        for (ExecutionAction executionAction : popExecutionActions()) {
            executionActions.add(type.cast(executionAction));
        }
        return executionActions;
    }

    /**
     * @param executionAction to be removed from the {@link ExecutionPlan} (it satisfies all dependencies to given {@code executionAction})
     */
    public synchronized void removeExecutionAction(ExecutionAction<?> executionAction)
    {
        // When plan has been build
        if (satisfiedActions != null) {
            logger.debug("{} ended.", executionAction);

            if (!poppedActions.remove(executionAction)) {
                throw new IllegalArgumentException("Execution action hasn't been popped (or has already been removed).");
            }
            completeExecutionAction(executionAction);
        }
        // Otherwise remove it totally from the plan
        else {
            Object target = executionAction.getTarget();
            if (target instanceof ExecutionTarget) {
                ExecutionTarget executionTarget = (ExecutionTarget) target;
                actionByExecutionTargetId.remove(executionTarget.getId());
            }
            remainingActions.remove(executionAction);
        }
    }

    /**
     * @param executionAction to be marked as completed and to satisfy all dependents
     */
    private void completeExecutionAction(ExecutionAction<?> executionAction)
    {
        completedActions.add(executionAction);
        for (ExecutionAction parentExecutionAction : executionAction.parents) {
            parentExecutionAction.dependencies.remove(executionAction);
            if (parentExecutionAction.dependencies.size() == 0) {
                if (!remainingActions.remove(parentExecutionAction)) {
                    throw new IllegalArgumentException("Execution action isn't in the execution plan (anymore).");
                }
                satisfiedActions.add(parentExecutionAction);
            }
        }
    }

    /**
     * @return true if the {@link ExecutionPlan} doesn't have any {@link ExecutionAction}s for performing
     *         false otherwise
     */
    public synchronized boolean isEmpty()
    {
        return remainingActions.isEmpty() && satisfiedActions.isEmpty() && poppedActions.isEmpty();
    }

    /**
     * Finish execution and return result.
     *
     * @param entityManager
     * @param referenceDateTime
     * @return {@link ExecutionResult}
     */
    public ExecutionResult finish(EntityManager entityManager, DateTime referenceDateTime)
    {
        if (!poppedActions.isEmpty()) {
            throw new IllegalStateException("Some execution actions has not been removed yet.");
        }
        ExecutionResult executionResult = new ExecutionResult();
        for (ExecutionAction executionAction : completedActions) {
            Object target = executionAction.getTarget();
            if (target instanceof PersistentObject) {
                entityManager.refresh(target);
            }
            boolean result = executionAction.finish(entityManager, referenceDateTime, executionResult);
            if (target instanceof ExecutionTarget) {
                ExecutionTarget executionTarget = (ExecutionTarget) target;
                if (result) {
                    executionTarget.setNextAttempt(null);
                    executionTarget.setAttemptCount(0);
                }
                else {
                    cz.cesnet.shongo.controller.executor.ExecutionReport lastReport = executionTarget.getLastReport();
                    if (lastReport == null && executionAction.isSkipPerform()) {
                        // If no report is created for the execution target and it wasn't performing anything
                        // (e.g., the action is Stop from a failed Migration)
                        // do not increase the attempt count
                        continue;
                    }

                    // Increment attempt count
                    executionTarget.setNextAttempt(null);
                    executionTarget.setAttemptCount(executionTarget.getAttemptCount() + 1);

                    // If the last report allows for next attempt, set the date/time for the next attempt
                    if (lastReport == null || lastReport.getResolution().equals(AbstractReport.Resolution.TRY_AGAIN)) {
                        Executor executor = getExecutor();
                        if (executionTarget.getAttemptCount() < executor.getMaxAttemptCount()) {
                            executionTarget.setNextAttempt(referenceDateTime.plus(executor.getNextAttempt()));
                        }
                    }
                }
            }
        }
        return executionResult;
    }
}
