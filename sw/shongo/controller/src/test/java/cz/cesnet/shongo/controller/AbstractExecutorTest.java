package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableServiceImpl;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlServiceImpl;
import cz.cesnet.shongo.controller.executor.ExecutionResult;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractControllerTest} which provides a {@link Executor} instance to extending classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractExecutorTest extends AbstractControllerTest
{
    private static Logger logger = LoggerFactory.getLogger(AbstractExecutorTest.class);

    /**
     * @see cz.cesnet.shongo.controller.Executor
     */
    private Executor executor;

    /**
     * Constructor.
     */
    public AbstractExecutorTest()
    {
        // Executor configuration
        System.setProperty(Configuration.EXECUTOR_EXECUTABLE_START, "PT0S");
        System.setProperty(Configuration.EXECUTOR_EXECUTABLE_END, "PT0S");
        System.setProperty(Configuration.EXECUTOR_STARTING_DURATION_ROOM, "PT0S");
    }

    /**
     * @return {@link #executor}
     */
    public Executor getExecutor()
    {
        return executor;
    }

    /**
     * @return {@link cz.cesnet.shongo.controller.api.rpc.ResourceControlService} from the {@link #controllerClient}
     */
    public ResourceControlService getResourceControlService()
    {
        return getControllerClient().getService(ResourceControlService.class);
    }

    /**
     * @return {@link cz.cesnet.shongo.controller.api.rpc.ExecutableService} from the {@link #controllerClient}
     */
    public ExecutableService getExecutorService()
    {
        return getControllerClient().getService(ExecutableService.class);
    }

    @Override
    protected void onInit()
    {
        super.onInit();

        Controller controller = getController();
        getController().addRpcService(new ResourceControlServiceImpl());
        getController().addRpcService(new ExecutableServiceImpl());

        executor = new Executor();
        executor.setEntityManagerFactory(getEntityManagerFactory());
        executor.init(controller.getConfiguration());
        executor.setControllerAgent(controller.getAgent());
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        getController().startJade();
    }

    /**
     * Run {@link Executor#execute} for given {@code referenceDateTime}.
     *
     * @param referenceDateTime
     */
    protected ExecutionResult runExecutor(DateTime referenceDateTime)
    {
        return executor.execute(referenceDateTime);
    }
}
