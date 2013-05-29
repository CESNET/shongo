package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Executor;

import java.util.*;

/**
 * Represents an {@link Executor} plan for collection of {@link Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutionPlan
{
    /**
     * @see Executor
     */
    private Executor executor;

    /**
     * @see ExecutionResult
     */
    private ExecutionResult executionResult;

    /**
     * Set of {@link ExecutionAction}s which haven't been performed yet.
     */
    private final Set<ExecutionAction> remainingActions = new HashSet<ExecutionAction>();

    /**
     * Map of {@link ExecutionAction} by {@link Executable} (used by {@link ExecutionAction}s for building dependencies).
     */
    final Map<Long, ExecutionAction.AbstractExecutableAction> actionByExecutableId =
            new HashMap<Long, ExecutionAction.AbstractExecutableAction>();

    /**
     * Set of {@link ExecutionAction}s with satisfied dependencies (with empty {@link ExecutionAction#dependencies}).
     */
    protected final Set<ExecutionAction> satisfiedActions = new HashSet<ExecutionAction>();

    /**
     * Constructor.
     *
     * @param executor sets the {@link #executor}
     */
    public ExecutionPlan(Executor executor, ExecutionResult executionResult)
    {
        this.executor = executor;
        this.executionResult = executionResult;
    }

    /**
     * @return {@link #executor}
     */
    public Executor getExecutor()
    {
        return executor;
    }

    /**
     * @return {@link #executionResult}
     */
    public ExecutionResult getExecutionResult()
    {
        return executionResult;
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
     * Build the {@link ExecutionPlan}.
     *
     * @throws RuntimeException when the plan cannot be constructed (because of cycle)
     */
    public void build()
    {
        // Execution plan is empty
        if (remainingActions.size() == 0) {
            return;
        }

        // Setup dependencies
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

        int currentPriority = 0;
        for (ExecutionAction executionAction : satisfiedActions) {
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
    public synchronized void removeExecutionAction(ExecutionAction executionAction)
    {
        if (!remainingActions.remove(executionAction)) {
            throw new IllegalArgumentException("Execution action isn't in the execution plan.");
        }
        satisfiedActions.remove(executionAction);
        for (ExecutionAction parentExecutionAction : executionAction.parents) {
            parentExecutionAction.dependencies.remove(executionAction);
            if (parentExecutionAction.dependencies.size() == 0) {
                satisfiedActions.add(parentExecutionAction);
            }
        }
    }

    /**
     * @return true if the {@link ExecutionPlan} has more {@link Executable}s with satisfied dependencies,
     *         false otherwise
     */
    public synchronized boolean isEmpty()
    {
        return remainingActions.size() == 0;
    }
}
