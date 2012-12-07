package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Represents a {@link Thread} for executing single {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutorThread extends Thread
{
    private static Logger logger = LoggerFactory.getLogger(ExecutorThread.class);

    /**
     * @see cz.cesnet.shongo.controller.Executor
     */
    private Executor executor;

    /**
     * @see cz.cesnet.shongo.controller.ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * Identifier of {@link Executable} to be executed.
     */
    private Long executableId;

    /**
     * {@link javax.persistence.EntityManagerFactory} used for loading {@link Executable}s for execution.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Constructor.
     *
     * @param executable sets the {@link #executableId} by {@link Executable#getId()}
     */
    public ExecutorThread(Executable executable)
    {
        executable.checkPersisted();
        this.executableId = executable.getId();
    }

    /**
     * @return {@link #executor}
     */
    public Executor getExecutor()
    {
        return executor;
    }

    /**
     * @return {@link #controllerAgent}
     */
    public ControllerAgent getControllerAgent()
    {
        return controllerAgent;
    }

    /**
     * @return {@link #executableId}
     */
    public Long getExecutableId()
    {
        return executableId;
    }

    /**
     * @return logger
     */
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * @param entityManager
     * @return loaded compartment from the given {@code entityManager}
     */
    public Executable getExecutable(EntityManager entityManager)
    {
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        Executable executable = null;
        try {
            executable = executableManager.get(executableId);
        }
        catch (EntityNotFoundException exception) {
            throw new IllegalStateException(String.format("{} with identifier '{}' was not found!",
                    Executable.class.getSimpleName(), executableId));
        }

        return executable;
    }

    /**
     * Start {@link ExecutorThread}.
     *
     * @param executor             sets the {@link #executor}
     * @param controllerAgent      sets the {@link #controllerAgent}
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public void start(Executor executor, ControllerAgent controllerAgent, EntityManagerFactory entityManagerFactory)
    {
        if (controllerAgent == null) {
            throw new IllegalArgumentException(ControllerAgent.class.getSimpleName() + " must not be null.");
        }
        this.executor = executor;
        this.controllerAgent = controllerAgent;
        this.entityManagerFactory = entityManagerFactory;
        setName(String.format("Executor-%d", executableId));

        // Start thread
        start();
    }

    @Override
    public void run()
    {
        // Wait for start
        while (true) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            Executable executable = getExecutable(entityManager);
            entityManager.close();
            DateTime start = executable.getSlot().getStart();
            start = start.plus(executor.getExecutableStart());
            if (!DateTime.now().isBefore(start)) {
                break;
            }
            try {
                logger.debug("Waiting for {} to start...", executable.getName());
                Thread.sleep(executor.getExecutableWaitingStart().getMillis());
            }
            catch (InterruptedException exception) {
            }
        }

        // Start
        {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            Executable executable = getExecutable(entityManager);

            if (executable.getState() == Compartment.State.NOT_STARTED) {
                logger.info("Starting {}...", executable.getName());
                executable.start(this, entityManager);
            }
            else {
                logger.info("Resuming compartment '{}'...", executable.getId());
                executable.resume(this, entityManager);
            }

            entityManager.close();
        }

        // Wait for end
        while (true) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            Executable executable = getExecutable(entityManager);
            entityManager.close();
            DateTime end = executable.getSlot().getEnd();
            end = end.plus(executor.getExecutableEnd());
            if (!DateTime.now().isBefore(end)) {
                break;
            }
            try {
                logger.debug("Waiting for {} to end...", executable.getName());
                Thread.sleep(executor.getExecutableWaitingEnd().getMillis());
            }
            catch (InterruptedException exception) {
            }
        }

        // End
        {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            Executable executable = getExecutable(entityManager);

            if (executable.getState() == Compartment.State.STARTED) {
                logger.info("Stopping {}...", executable.getName());
                executable.stop(this, entityManager);
            }

            entityManager.close();
        }
    }
}
