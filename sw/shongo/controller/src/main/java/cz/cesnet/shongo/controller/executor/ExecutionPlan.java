package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Executor;

import javax.persistence.EntityManager;
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
     * Set of popped {@link ExecutionAction}s (by {@link #popExecutionActions()}).
     */
    private final List<ExecutionAction> poppedActions = new LinkedList<ExecutionAction>();

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
     * @param executable
     * @return {@link ExecutionAction.AbstractExecutableAction} for given {@code executable}
     */
    public ExecutionAction.AbstractExecutableAction getActionByExecutable(Executable executable)
    {
        return actionByExecutableId.get(executable.getId());
    }

    /**
     * @param executable
     * @param executionAction to be set from given {@code executable}
     */
    public void setActionByExecutable(Executable executable, ExecutionAction.AbstractExecutableAction executionAction)
    {
        actionByExecutableId.put(executable.getId(), executionAction);
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

        // Setup dependencies and remaining actions
        for (ExecutionAction executionAction : remainingActions) {
            remainingActions.add(executionAction);
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

        // Added popped actions
        poppedActions.addAll(executionActions);

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

    /**
     * Finish execution and return result.
     *
     * @param entityManager
     * @return {@link ExecutionResult}
     */
    public ExecutionResult finish(EntityManager entityManager)
    {
        ExecutionResult executionResult = new ExecutionResult();
        for (ExecutionAction executionAction : poppedActions) {
            executionAction.finish(entityManager, executionResult);
        }
        return executionResult;
    }
}
