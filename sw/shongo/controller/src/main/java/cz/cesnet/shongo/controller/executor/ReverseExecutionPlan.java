package cz.cesnet.shongo.controller.executor;

import java.util.Collection;

/**
 * Represents an reverse {@link cz.cesnet.shongo.controller.executor.ExecutionPlan}.
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
        for (Long parentExecutableId : remainingExecutableIds) {
            ExecutablePlan parentExecutablePlan = executablePlans.get(parentExecutableId);
            Executable parentExecutable = parentExecutablePlan.getExecutable();
            Collection<Executable> childExecutables = parentExecutable.getChildExecutables();

            // Setup parents in parent plan and dependencies in child plans
            for (Executable childExecutable : parentExecutable.getExecutionDependencies()) {
                ExecutablePlan childExecutablePlan = executablePlans.get(childExecutable.getId());
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
