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

    public ExecutorThread(Type type, Executable executable, Executor executor, ExecutionPlan executionPlan)
    {
        this.type = type;
        this.executable = executable;
        this.executor = executor;
        this.executionPlan = executionPlan;
    }

    @Override
    public void run()
    {
        switch (type) {
            case START:
                try {
                    EntityManager entityManager = executor.getEntityManager();
                    ExecutableManager executableManager = new ExecutableManager(entityManager);
                    Executable executable = executableManager.get(this.executable.getId());
                    entityManager.getTransaction().begin();
                    executable.start(executor, entityManager);
                    entityManager.getTransaction().commit();
                    entityManager.close();
                } catch (FaultException exception) {
                    executor.getLogger().error("Failed to load executable", exception);
                }
                executionPlan.removeExecutable(executable);
                break;
            case STOP:
                try {
                    EntityManager entityManager = executor.getEntityManager();
                    ExecutableManager executableManager = new ExecutableManager(entityManager);
                    Executable executable = executableManager.get(this.executable.getId());
                    entityManager.getTransaction().begin();
                    executable.stop(executor, entityManager);
                    entityManager.getTransaction().commit();
                    entityManager.close();
                } catch (FaultException exception) {
                    executor.getLogger().error("Failed to load executable", exception);
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
