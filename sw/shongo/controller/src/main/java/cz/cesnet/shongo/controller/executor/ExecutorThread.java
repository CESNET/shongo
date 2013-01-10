package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.EntityManager;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutorThread extends Thread
{
    private Type type;

    private Executable executable;

    private Executor executor;

    private ExecutionPlan executionPlan;

    private ExecutionResult executionResult;

    private EntityManager entityManager;

    public ExecutorThread(Type type, Executable executable, Executor executor, ExecutionPlan executionPlan,
            ExecutionResult executionResult, EntityManager entityManager)
    {
        this.type = type;
        this.executable = executable;
        this.executor = executor;
        this.executionPlan = executionPlan;
        this.executionResult = executionResult;
        this.entityManager = entityManager;
    }

    @Override
    public void run()
    {
        switch (type) {
            case START:
                executable.start(executor, entityManager);
                if (executable.getState().equals(Executable.State.STARTED)) {
                    executionResult.addStartedExecutable(executable);
                }
                executionPlan.removeExecutable(executable);
                break;
            case STOP:
                executable.stop(executor, entityManager);
                if (executable.getState().equals(Executable.State.STOPPED)) {
                    executionResult.addStoppedExecutable(executable);
                }
                executionPlan.removeExecutable(executable);
                break;
            default:
                throw new TodoImplementException(type.toString());
        }
    }

    /**
     * Type of {@link ExecutorThread} action.
     */
    public static enum Type
    {
        /**
         * Call {@link Executable#start(cz.cesnet.shongo.controller.Executor, javax.persistence.EntityManager)}.
         */
        START,

        /**
         * Call {@link Executable#stop(cz.cesnet.shongo.controller.Executor, javax.persistence.EntityManager)}.
         */
        STOP
    }
}
