package cz.cesnet.shongo.controller.executor;

import java.util.*;

/**
 * Represents an {@link cz.cesnet.shongo.controller.Executor} for collection of {@link cz.cesnet.shongo.controller.executor.Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReverseExecutionPlan extends ExecutionPlan
{
    /**
     * Constructor.
     *
     * @param executables from which the {@link ExecutablePlan} should be constructed
     * @throws IllegalStateException when the plan cannot be constructed (because of cycle)
     */
    public ReverseExecutionPlan(Collection<Executable> executables) throws IllegalStateException
    {
        super(executables);
    }

    @Override
    protected void buildDependencies()
    {
        // Setup dependencies
        for (Executable parentExecutable : executablePlans.keySet()) {
            ExecutablePlan parentExecutablePlan = executablePlans.get(parentExecutable);
            Collection<Executable> childExecutables = parentExecutable.getChildExecutables();

            // Setup parents in parent plan and dependencies in child plans
            for (Executable childExecutable : parentExecutable.getExecutionDependencies()) {
                ExecutablePlan childExecutablePlan = executablePlans.get(childExecutable);
                if (childExecutablePlan == null) {
                    continue;
                }

                // Parent executable has child executables as parent (is required by them)
                parentExecutablePlan.parents.add(childExecutable);

                // Child executable is dependent to parent executable (requires him)
                childExecutablePlan.dependencies.add(parentExecutable);
            }
        }
    }
}
