package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.api.jade.CommandDisabledException;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.GetRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableServiceImpl;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlServiceImpl;
import cz.cesnet.shongo.jade.Agent;
import jade.core.AID;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link cz.cesnet.shongo.controller.AbstractControllerTest} which provides a {@link Executor} instance to extending classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractExecutorTest extends AbstractControllerTest
{
    private static Logger logger = LoggerFactory.getLogger(AbstractExecutorTest.class);

    /**
     * @see Executor
     */
    private Executor executor;

    /**
     * Constructor.
     */
    public AbstractExecutorTest()
    {
        // Executor configuration
        System.setProperty(ControllerConfiguration.EXECUTOR_EXECUTABLE_START, "PT0S");
        System.setProperty(ControllerConfiguration.EXECUTOR_EXECUTABLE_END, "PT0S");
        System.setProperty(ControllerConfiguration.EXECUTOR_STARTING_DURATION_ROOM, "PT0S");
        System.setProperty(ControllerConfiguration.EXECUTOR_EXECUTABLE_NEXT_ATTEMPT, "PT0S");
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

    /**
     * Testing connector agent.
     */
    protected static class TestAgent extends Agent
    {
        /**
         * List of performed actions on connector.
         */
        private List<Command> performedCommands = new ArrayList<Command>();

        /**
         * Constructor.
         */
        public TestAgent()
        {
        }

        /**
         * @return {@link Class}es for {@link #performedCommands}
         */
        public List<Class<? extends Command>> getPerformedCommandClasses()
        {
            List<Class<? extends Command>> performedActionClasses = new ArrayList<Class<? extends Command>>();
            for (Command command : performedCommands) {
                performedActionClasses.add(command.getClass());
            }
            return performedActionClasses;
        }

        /**
         * Clear the {@link #performedCommands}.
         */
        public void clearPerformedCommands()
        {
            performedCommands.clear();
        }

        /**
         * @param type
         * @return {@link Command} of given {@code type}
         */
        public <T> T getPerformedCommandByClass(Class<T> type)
        {
            for (Command command : performedCommands) {
                if (type.isAssignableFrom(command.getClass())) {
                    return type.cast(command);
                }
            }
            throw new RuntimeException("Command of type '" + type.getSimpleName() + "' was not found.");
        }

        @Override
        protected void setup()
        {
            addOntology(ConnectorOntology.getInstance());
            super.setup();
        }

        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException, CommandUnsupportedException
        {
            performedCommands.add(command);
            logger.debug("ConnectorAgent '{}' receives command '{}'.", getName(), command.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Testing MCU agent.
     */
    public class McuTestAgent extends TestAgent
    {
        /**
         * Specifies whether device is disabled and should throw error for every request.
         */
        private boolean disabled = false;

        /**
         * Rooms.
         */
        private Map<String, Room> rooms = new HashMap<String, Room>();

        /**
         * @param disabled sets the {@link #disabled}
         */
        public void setDisabled(boolean disabled)
        {
            this.disabled = disabled;
        }

        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException, CommandUnsupportedException
        {
            if (disabled) {
                throw new CommandDisabledException();
            }
            Object result = super.handleCommand(command, sender);
            if (command instanceof CreateRoom) {
                CreateRoom createRoom = (CreateRoom) command;
                String roomId = String.valueOf(rooms.size() + 1);
                rooms.put(roomId, createRoom.getRoom());
                return roomId;
            }
            else if (command instanceof ModifyRoom) {
                ModifyRoom modifyRoom = (ModifyRoom) command;
                Room room = modifyRoom.getRoom();
                String roomId = room.getId();
                if (!rooms.containsKey(roomId)) {
                    throw new RuntimeException("Room not found.");
                }
                rooms.put(roomId, room);
                return roomId;
            }
            else if (command instanceof GetRoom) {
                GetRoom getRoom = (GetRoom) command;
                String roomId = getRoom.getRoomId();
                return rooms.get(roomId);
            }
            return result;
        }
    }
}
