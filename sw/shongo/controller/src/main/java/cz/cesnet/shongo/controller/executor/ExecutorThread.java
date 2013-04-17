package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;

/**
 * Represent a {@link Thread} which is responsible to perform one action of specified {@link #type} for
 * specified {@link #executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutorThread extends Thread
{
    /**
     * @see Type
     */
    private Type type;

    /**
     * For which the action will be performed.
     */
    private Executable executable;

    /**
     * Reference to {@link Executor}.
     */
    private Executor executor;

    /**
     * {@link ExecutionPlan} which planned this action.
     */
    private ExecutionPlan executionPlan;

    /**
     * Constructor.
     *
     * @param type          sets the {@link #type}
     * @param executable    sets the {@link #executable}
     * @param executor      sets the {@link #executor}
     * @param executionPlan sets the {@link #executionPlan}
     */
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
        EntityManager entityManager = executor.getEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        try {
            entityManager.getTransaction().begin();

            // Perform proper action for executable
            Executable executable = null;
            boolean success = true;
            switch (type) {
                case START:
                    try {
                        executable = executableManager.get(this.executable.getId());
                        executable.start(executor, executableManager);
                        success = executable.getState().isStarted();
                        if (success) {
                            if (executable instanceof RoomEndpoint && executionPlan.hasParents(executable)) {
                                executor.getLogger().info("Waiting for room '{}' to be created...", executable.getId());
                                try {
                                    Thread.sleep(executor.getStartingDurationRoom().getMillis());
                                }
                                catch (InterruptedException exception) {
                                    executor.getLogger().error("Waiting for room was interrupted...");
                                    exception.printStackTrace();
                                }
                            }
                        }
                    }
                    catch (Exception exception) {
                        Reporter.reportInternalError(Reporter.InternalErrorType.EXECUTOR, "Starting failed", exception);
                    }
                    break;
                case UPDATE:
                    try {
                        executable = executableManager.get(this.executable.getId());
                        executable.update(executor, executableManager);
                        success = !executable.getState().isModified();
                    }
                    catch (Exception exception) {
                        Reporter.reportInternalError(Reporter.InternalErrorType.EXECUTOR, "Updating failed", exception);
                    }
                    break;
                case STOP:
                    try {
                        executable = executableManager.get(this.executable.getId());
                        executable.stop(executor, executableManager);
                        success = !executable.getState().isStarted();
                    }
                    catch (Exception exception) {
                        Reporter.reportInternalError(Reporter.InternalErrorType.EXECUTOR, "Stopping failed", exception);
                    }
                    break;
                default:
                    throw new TodoImplementException(type.toString());
            }

            if (success) {
                executable.setNextAttempt(null);
                executable.setAttemptCount(0);
            }
            else {
                ExecutableReport lastReport = executable.getLastReport();
                if ((executable.getAttemptCount() + 1) < executor.getMaxAttemptCount() &&
                        lastReport.getResolution().equals(Report.Resolution.TRY_AGAIN)) {
                    executable.setNextAttempt(DateTime.now().plus(executor.getNextAttempt()));
                    executable.setAttemptCount(executable.getAttemptCount() + 1);
                }
                else {
                    executable.setNextAttempt(null);
                }
            }

            // Remove executable from plan
            executionPlan.removeExecutable(this.executable);

            entityManager.getTransaction().commit();

            // Reporting
            for (ExecutableReport executableReport : executableManager.getExecutableReports()) {
                Reporter.report(executableReport);
            }
        }
        catch (Exception exception) {
            Reporter.reportInternalError(Reporter.InternalErrorType.EXECUTOR, exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    /**
     * Type of {@link ExecutorThread} action.
     */
    public static enum Type
    {
        /**
         * Call {@link Executable#start}.
         */
        START,

        /**
         * Call {@link Executable#update}.
         */
        UPDATE,

        /**
         * Call {@link Executable#stop}.
         */
        STOP
    }
}
