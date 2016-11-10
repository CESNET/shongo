package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.ExpirationSet;
import cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.recording.RecordableEndpoint;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ManagedMode;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Component of a domain controller which executes actions according to allocation plan which was created
 * by the {@link cz.cesnet.shongo.controller.scheduler.Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Executor extends SwitchableComponent
        implements Component.WithThread, Component.EntityManagerFactoryAware, Component.ControllerAgentAware,
                   Component.AuthorizationAware, Runnable
{
    /**
     * {@link Logger} for {@link Executor}
     */
    private static Logger logger = LoggerFactory.getLogger(Executor.class);

    /**
     * @see NotificationManager
     */
    private NotificationManager notificationManager;

    /**
     * {@link EntityManagerFactory} used for loading {@link Executable}s for execution.
     */
    public EntityManagerFactory entityManagerFactory;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    /**
     * @see ControllerConfiguration#EXECUTOR_PERIOD
     */
    private Duration period;

    /**
     * @see ControllerConfiguration#EXECUTOR_EXECUTABLE_START
     */
    private Duration executableStart;

    /**
     * @see ControllerConfiguration#EXECUTOR_EXECUTABLE_END
     */
    private Duration executableEnd;

    /**
     * @see ControllerConfiguration#EXECUTOR_STARTING_DURATION_ROOM
     */
    private Duration startingDurationRoom;

    /**
     * @see ControllerConfiguration#EXECUTOR_EXECUTABLE_NEXT_ATTEMPT
     */
    private Duration nextAttempt;

    /**
     * @see ControllerConfiguration#EXECUTOR_EXECUTABLE_MAX_ATTEMPT_COUNT
     */
    private int maxAttemptCount;

    /**
     * Map of (maps of recording folders by recording capabilities) by recordable endpoint ids.
     */
    private final Map<Long, Map<Long, String>> recordingFolders = new HashMap<Long, Map<Long, String>>();

    /**
     * Set of identifiers of {@link cz.cesnet.shongo.controller.api.ExecutableService}s which should not be checked again.
     */
    private ExpirationSet<Long> checkedExecutableServiceIds = new ExpirationSet<Long>();

    /**
     * List of {@link AbstractNotification}s to be executed.
     */
    private final List<AbstractNotification> notifications = new LinkedList<AbstractNotification>();

    /**
     * Constructor.
     * @param notificationManager
     */
    public Executor(NotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
        this.checkedExecutableServiceIds.setExpiration(Duration.standardSeconds(10));
    }

    /**
     * @return {@link #logger}
     */
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * @return {@link #notificationManager}
     */
    public NotificationManager getNotificationManager()
    {
        return notificationManager;
    }

    /**
     * @return {@link #startingDurationRoom}
     */
    public Duration getStartingDurationRoom()
    {
        return startingDurationRoom;
    }

    /**
     * @return {@link #nextAttempt}
     */
    public Duration getNextAttempt()
    {
        return nextAttempt;
    }

    /**
     * @return {@link #maxAttemptCount}
     */
    public int getMaxAttemptCount()
    {
        return maxAttemptCount;
    }

    @Override
    public Thread getThread()
    {
        Thread thread = new Thread(this);
        thread.setName("executor");
        return thread;
    }

    /**
     * @return new {@link EntityManager}
     */
    public EntityManager getEntityManager()
    {
        return entityManagerFactory.createEntityManager();
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * @return {@link #controllerAgent}
     */
    public ControllerAgent getControllerAgent()
    {
        return controllerAgent;
    }

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    /**
     * @return {@link #authorization}
     */
    public Authorization getAuthorization()
    {
        return authorization;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(ControllerConfiguration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(controllerAgent, ControllerAgent.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);

        period = configuration.getDuration(ControllerConfiguration.EXECUTOR_PERIOD);
        executableStart = configuration.getDuration(ControllerConfiguration.EXECUTOR_EXECUTABLE_START);
        executableEnd = configuration.getDuration(ControllerConfiguration.EXECUTOR_EXECUTABLE_END);
        nextAttempt = configuration.getDuration(ControllerConfiguration.EXECUTOR_EXECUTABLE_NEXT_ATTEMPT);
        startingDurationRoom = configuration.getDuration(ControllerConfiguration.EXECUTOR_STARTING_DURATION_ROOM);
        maxAttemptCount = configuration.getInt(ControllerConfiguration.EXECUTOR_EXECUTABLE_MAX_ATTEMPT_COUNT);
    }

    @Override
    public void run()
    {
        logger.debug("Executor started!");

        while (!Thread.interrupted()) {
            try {
                Thread.sleep(period.getMillis());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
            execute(DateTime.now());
        }

        logger.debug("Executor stopped!");
    }

    /**
     * Execute {@link Reservation}s which should be executed for given {@code interval}.
     *
     * @param dateTime specifies date/time which should be used as "now" executing {@link Reservation}s
     * @return {@link cz.cesnet.shongo.controller.executor.ExecutionResult}
     */
    public synchronized ExecutionResult execute(DateTime dateTime)
    {
        if (!isEnabled()) {
            logger.warn("Skipping executor because it is disabled...");
            return new ExecutionResult();
        }

        // Globally synchronized (see ThreadLock documentation)
        //logger.info("Executor waiting for lock...............................");
        synchronized (ThreadLock.class) {
            //logger.info("Executor lock acquired...     (((((");

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG);
            logger.debug("Checking executables for execution at '{}'...", dateTimeFormatter.formatDateTime(dateTime));

            EntityManager entityManager = entityManagerFactory.createEntityManager();
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            try {
                // Create execution plan
                DateTime start = dateTime.minus(executableStart);
                DateTime stop = dateTime.minus(executableEnd);
                ExecutionPlan executionPlan = new ExecutionPlan(this);
                for (Executable executable : executableManager.listExecutablesForStart(start, maxAttemptCount)) {
                    executionPlan.addExecutionAction(new ExecutionAction.StartExecutableAction(executable));
                    Executable migrateFromExecutable = executable.getMigrateFromExecutable();
                    if (migrateFromExecutable != null) {
                        if (migrateFromExecutable.getState().isStarted()) {
                            Migration migration = new Migration(migrateFromExecutable, executable);
                            executionPlan.addExecutionAction(new ExecutionAction.MigrationAction(migration));
                        }
                        else {
                            logger.warn("Cannot migrate executable {} ({}) from {} ({}, {}).", new Object[]{
                                    executable.getId(), executable.getSlot(), migrateFromExecutable.getId(),
                                    migrateFromExecutable.getSlot(), migrateFromExecutable.getState()});
                        }
                    }
                }
                for (Executable executable : executableManager.listExecutablesForStop(stop, maxAttemptCount)) {
                    executionPlan.addExecutionAction(new ExecutionAction.StopExecutableAction(executable));
                }
                for (Executable executable : executableManager.listExecutablesForFinalization(dateTime, maxAttemptCount)) {
                    executionPlan.addExecutionAction(new ExecutionAction.FinalizeExecutableAction(executable));
                }
                for (Executable executable : executableManager.listExecutablesForUpdate(dateTime, maxAttemptCount)) {
                    ExecutionAction executionAction = executionPlan.getActionByExecutionTarget(executable);
                    if (executionAction != null) {
                        // Skip updating executable when other action is planned (i.e., start or stop)
                        continue;
                    }
                    executionPlan.addExecutionAction(new ExecutionAction.UpdateExecutableAction(executable));
                }
                for (ExecutableService service : executableManager.listServicesForActivation(start, maxAttemptCount)) {
                    executionPlan.addExecutionAction(new ExecutionAction.ActivateExecutableServiceAction(service));
                }
                for (ExecutableService service : executableManager.listServicesForDeactivation(stop, maxAttemptCount)) {
                    executionPlan.addExecutionAction(new ExecutionAction.DeactivateExecutableServiceAction(service));
                }
                executionPlan.build();

                // Perform execution plan
                while (!executionPlan.isEmpty()) {
                    Collection<ExecutionAction> executionActions = executionPlan.popExecutionActions();
                    for (ExecutionAction executionAction : executionActions) {
                        executionAction.start();
                    }
                    for (ExecutionAction executionAction : executionActions) {
                        executionAction.join();
                    }
                }

                // Finish execution plan
                entityManager.getTransaction().begin();
                ExecutionResult executionResult = executionPlan.finish(entityManager, dateTime);
                entityManager.getTransaction().commit();

                // Set all activated and deactivated services as checked
                for (ExecutableService executableService : executionResult.getActivatedExecutableServices()) {
                    addCheckedExecutableService(executableService);
                }
                for (ExecutableService executableService : executionResult.getDeactivatedExecutableServices()) {
                    addCheckedExecutableService(executableService);
                }

                // Add notifications
                entityManager.getTransaction().begin();
                synchronized (notifications) {
                    notificationManager.addNotifications(notifications, entityManager);
                    notifications.clear();
                }
                entityManager.getTransaction().commit();

                return executionResult;
            }
            catch (Exception exception) {
                Reporter.getInstance().reportInternalError(Reporter.EXECUTOR, exception);
                return null;
            }
            finally {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }

                entityManager.close();
            }

            //logger.info("Executor releasing lock...    )))))");
        }
        //logger.info("Executor lock released...");
    }

    /**
     * @param recordableEndpoint
     * @param recordingCapability
     * @return identifier of recording folder for given {@code recordingCapability} which can be used for given {@code recordableEndpoint}
     * @throws ExecutionReportSet.CommandFailedException
     *          when the retrieving of the recording folder fails
     */
    public String getRecordingFolderId(RecordableEndpoint recordableEndpoint, RecordingCapability recordingCapability)
            throws ExecutionReportSet.CommandFailedException
    {
        Map<Long, String> recordingFolders;
        synchronized (this.recordingFolders) {
            recordingFolders = this.recordingFolders.get(recordableEndpoint.getId());
            if (recordingFolders == null) {
                recordingFolders = new HashMap<Long, String>();
                this.recordingFolders.put(recordableEndpoint.getId(), recordingFolders);
            }
        }
        synchronized (recordingFolders) {
            String recordingFolderId = recordingFolders.get(recordingCapability.getId());
            if (recordingFolderId == null) {
                recordingFolderId = recordableEndpoint.getRecordingFolderId(recordingCapability);
                if (recordingFolderId == null) {
                    DeviceResource deviceResource = recordingCapability.getDeviceResource();
                    ManagedMode managedMode = deviceResource.requireManaged();
                    String agentName = managedMode.getConnectorAgentName();
                    SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName,
                            new CreateRecordingFolder(recordableEndpoint.getRecordingFolderApi()));
                    if (!SendLocalCommand.State.SUCCESSFUL.equals(sendLocalCommand.getState())) {
                        throw new ExecutionReportSet.CommandFailedException(
                                sendLocalCommand.getName(), sendLocalCommand.getJadeReport());
                    }
                    recordingFolderId = (String) sendLocalCommand.getResult();
                    if (recordingFolderId == null || recordingFolderId.isEmpty()) {
                        throw new RuntimeException(CreateRecordingFolder.class.getSimpleName() +
                                " should return identifier of the recording folder.");
                    }
                    recordableEndpoint.putRecordingFolderId(recordingCapability, recordingFolderId);
                }
                recordingFolders.put(recordingCapability.getId(), recordingFolderId);
            }
            return recordingFolderId;
        }
    }

    /**
     * @param executableService which has been just {@link ExecutableService#check}ed
     */
    public void addCheckedExecutableService(ExecutableService executableService)
    {
        checkedExecutableServiceIds.add(executableService.getId());
    }

    /**
     * @param executableService
     * @return true whether given {@code executableService} can be {@link ExecutableService#check}ed,
     *         false otherwise
     */
    public boolean isExecutableServiceCheckable(ExecutableService executableService)
    {
        return !checkedExecutableServiceIds.contains(executableService.getId());
    }

    /**
     * @param notification to be added to the {@link #notifications}
     */
    public void addNotification(AbstractNotification notification)
    {
        synchronized (notifications) {
            notifications.add(notification);
        }
    }
}
