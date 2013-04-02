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
     * Map of {@link ExecutablePlan}s by {@link Executable}s.
     */
    protected final Map<Long, ExecutablePlan> executablePlans = new HashMap<Long, ExecutablePlan>();

    /**
     * Set of {@link Executable}s which still are in the plan (and should be processed).
     */
    protected final Set<Long> remainingExecutableIds = new HashSet<Long>();

    /**
     * Set of {@link Executable}s with satisfied dependencies (with empty {@link ExecutablePlan#dependencies}).
     */
    protected final Set<Executable> satisfiedExecutables = new HashSet<Executable>();

    /**
     * Constructor.
     *
     * @param executables from which the {@link ExecutablePlan} should be constructed
     * @throws RuntimeException when the plan cannot be constructed (because of cycle)
     */
    public ExecutionPlan(Collection<Executable> executables) throws RuntimeException
    {
        // Execution plan is empty
        if (executables.size() == 0) {
            return;
        }

        // Initialize executable plans and set of satisfied
        for (Executable executable : executables) {
            executablePlans.put(executable.getId(), new ExecutablePlan(executable));
            remainingExecutableIds.add(executable.getId());
        }

        // Setup dependencies
        buildDependencies();

        // Compute initial satisfied executables
        for (Executable executable : executables) {
            ExecutablePlan executablePlan = executablePlans.get(executable.getId());
            if (executablePlan.dependencies.size() == 0) {
                // Executable is satisfied (because it is not dependent on any other executable)
                satisfiedExecutables.add(executable);
            }
            else {
                // Executable is not satisfied (because it is dependent to at least one other executable)
            }
        }

        // Check for cycles
        if (satisfiedExecutables.size() == 0) {
            throw new RuntimeException("Execution plan cannot be constructed (contains a cycle).");
        }
    }

    /**
     * Setup dependencies.
     */
    protected void buildDependencies()
    {
        for (Long parentExecutableId : remainingExecutableIds) {
            ExecutablePlan parentExecutablePlan = executablePlans.get(parentExecutableId);
            Executable parentExecutable = parentExecutablePlan.getExecutable();
            Collection<Executable> childExecutables = parentExecutable.getChildExecutables();

            // Setup dependencies in parent plan and parents in child plans
            for (Executable childExecutable : parentExecutable.getExecutionDependencies()) {
                ExecutablePlan childExecutablePlan = executablePlans.get(childExecutable.getId());

                // Child executable doesn't exists in the plan, so it is automatically satisfied
                if (childExecutablePlan == null) {
                    continue;
                }

                // Parent executable is dependent to all child executables (requires them)
                parentExecutablePlan.dependencies.add(childExecutable);

                // Child executable has new parent (is required by him)
                childExecutablePlan.parents.add(parentExecutable);
            }
        }
    }

    /**
     * @return collection of {@link Executable}s which have satisfied dependencies
     *         and remove them from the current {@link ExecutionPlan} queue
     */
    public synchronized Collection<Executable> popExecutables()
    {
        Set<Executable> currentExecutables = new HashSet<Executable>();
        currentExecutables.addAll(satisfiedExecutables);
        satisfiedExecutables.clear();
        return currentExecutables;
    }

    /**
     * @param executable to be removed from the {@link ExecutionPlan}
     *                   (it satisfies all dependencies to given {@code executable})
     */
    public synchronized void removeExecutable(Executable executable)
    {
        Long executableId = executable.getId();
        ExecutablePlan executablePlan = executablePlans.get(executableId);
        if (executablePlan == null) {
            throw new IllegalArgumentException("Given executable isn't in the plan.");
        }
        remainingExecutableIds.remove(executableId);
        satisfiedExecutables.remove(executable);
        for (Executable parentExecutable : executablePlan.parents) {
            ExecutablePlan parentExecutablePlan = executablePlans.get(parentExecutable.getId());
            parentExecutablePlan.dependencies.remove(executable);
            if (parentExecutablePlan.dependencies.size() == 0) {
                satisfiedExecutables.add(parentExecutable);
            }
        }
    }

    /**
     * @return true if the {@link ExecutionPlan} has more {@link Executable}s with satisfied dependencies,
     *         false otherwise
     */
    public synchronized boolean isEmpty()
    {
        return remainingExecutableIds.size() == 0;
    }

    /**
     * @param executable to be checked for parents
     * @return true if some executables are dependent to given {@code executable} (should be started after it),
     *         false otherwise
     */
    public boolean hasParents(Executable executable)
    {
        ExecutablePlan executablePlan = executablePlans.get(executable.getId());
        if (executablePlan == null) {
            throw new IllegalArgumentException("Given executable isn't in the plan.");
        }
        return executablePlan.parents.size() > 0;
    }

    /**
     * Represents an {@link Executor} plan for a single {@link Executable}.
     */
    protected static class ExecutablePlan
    {
        /**
         * {@link Executable} to which the plan belongs.
         */
        private Executable executable;

        /**
         * Set of {@link Executable}s which are required by this {@link #executable}
         * (e.g., must be started before this {@link #executable} is started).
         */
        protected Set<Executable> dependencies = new HashSet<Executable>();

        /**
         * Set of {@link Executable}s which requires this {@link #executable}
         * (e.g., must be started after this {@link #executable} is started).
         */
        protected Set<Executable> parents = new HashSet<Executable>();

        /**
         * Constructor.
         *
         * @param executable
         */
        public ExecutablePlan(Executable executable)
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
    }
}
