package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Executor;

import java.util.*;

/**
 * Represents an {@link Executor} for collection of {@link Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutionPlan
{
    /**
     * Map of {@link ExecutablePlan}s by {@link Executable}s.
     */
    private final Map<Executable, ExecutablePlan> executablePlans = new HashMap<Executable, ExecutablePlan>();

    /**
     * Set of {@link Executable}s with satisfied dependencies (with empty {@link ExecutablePlan#dependencies}).
     */
    private final Set<Executable> satisfiedExecutables = new HashSet<Executable>();

    /**
     * Constructor.
     *
     * @param executables from which the {@link ExecutablePlan} should be constructed
     * @throws IllegalStateException when the plan cannot be constructed (because of cycle)
     */
    public ExecutionPlan(Collection<Executable> executables) throws IllegalStateException
    {
        // Initialize executable plans and set of satisfied
        for (Executable executable : executables) {
            executablePlans.put(executable, new ExecutablePlan(executable));
            satisfiedExecutables.add(executable);
        }
        // Setup dependencies
        for (Executable parentExecutable : executables) {
            ExecutablePlan parentExecutablePlan = executablePlans.get(parentExecutable);
            Collection<Executable> childExecutables = parentExecutable.getChildExecutables();

            // Setup dependencies in parent plan and parents in child plans
            for (Executable childExecutable : parentExecutable.getChildExecutables()) {
                ExecutablePlan childExecutablePlan = executablePlans.get(childExecutable);

                // Child executable doesn't exists in the plan, so it is automatically satisfied
                if (childExecutablePlan == null) {
                    continue;
                }

                // Parent executable is dependent to all child executables (requires them)
                parentExecutablePlan.dependencies.add(childExecutable);

                // Child executable has new parent (is required by him)
                childExecutablePlan.parents.add(parentExecutable);
            }

            if (parentExecutablePlan.dependencies.size() > 0) {
                // Parent executable is not satisfied (because it is dependent to at least one child executable)
                satisfiedExecutables.remove(parentExecutable);
            }
            else {
                // Parent executable remains satisfied (because it is not dependent on any child executable)
            }
        }
        if (satisfiedExecutables.size() == 0) {
            throw new IllegalStateException("Execution plan cannot be constructed (contains a cycle).");
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
        return satisfiedExecutables;
    }

    /**
     * @param executable to be removed from the {@link ExecutionPlan}
     *                   (it satisfies all dependencies to given {@code executable})
     */
    public synchronized void removeExecutable(Executable executable)
    {
        ExecutablePlan executablePlan = executablePlans.get(executable);
        if (executablePlan == null) {
            throw new IllegalArgumentException("Given executable isn't in the plan.");
        }
        executablePlans.remove(executable);
        satisfiedExecutables.remove(executable);
        for (Executable parentExecutable : executablePlan.parents) {
            ExecutablePlan parentExecutablePlan = executablePlans.get(parentExecutable);
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
        return executablePlans.size() == 0;
    }

    /**
     * Represents an {@link Executor} plan for a single {@link Executable}.
     */
    private static class ExecutablePlan
    {
        /**
         * {@link Executable} to which the plan belongs.
         */
        private Executable executable;

        /**
         * Set of {@link Executable}s which are required by this {@link #executable}
         * (e.g., must be started before this {@link #executable} is started).
         */
        private Set<Executable> dependencies = new HashSet<Executable>();

        /**
         * Set of {@link Executable}s which requires this {@link #executable}
         * (e.g., must be started after this {@link #executable} is started).
         */
        private Set<Executable> parents = new HashSet<Executable>();

        /**
         * Constructor.
         *
         * @param executable
         */
        public ExecutablePlan(Executable executable)
        {
            this.executable = executable;
        }
    }
}
