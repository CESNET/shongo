package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.api.jade.CommandDisabledException;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.api.jade.multipoint.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.ModifyRoom;
import cz.cesnet.shongo.connector.api.jade.recording.*;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableServiceListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.*;
import cz.cesnet.shongo.controller.executor.ExecutionResult;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.jade.Agent;
import jade.core.AID;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link cz.cesnet.shongo.controller.AbstractControllerTest} which provides a {@link cz.cesnet.shongo.controller.executor.Executor} instance to extending classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractExecutorTest extends AbstractControllerTest
{
    private static Logger logger = LoggerFactory.getLogger(AbstractExecutorTest.class);

    /**
     * @see cz.cesnet.shongo.controller.executor.Executor
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
    public ExecutableService getExecutableService()
    {
        return getControllerClient().getService(ExecutableService.class);
    }

    @Override
    protected void onInit()
    {
        super.onInit();

        Controller controller = getController();

        executor = new Executor(controller.getNotificationManager());

        RecordingsCache recordingsCache = new RecordingsCache();
        getController().addRpcService(new ResourceControlServiceImpl(recordingsCache));
        getController().addRpcService(new ExecutableServiceImpl(executor, recordingsCache));

        executor.setEntityManagerFactory(getEntityManagerFactory());
        executor.setControllerAgent(controller.getAgent());
        executor.setAuthorization(controller.getAuthorization());
        executor.init(controller.getConfiguration());
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
        try {
            return executor.execute(referenceDateTime);
        } finally {
            checkExecutableSummaryConsistency();
        }
    }

    /**
     * Checks consistency of table executable summary. For more see init.sql and entity {@link cz.cesnet.shongo.controller.booking.executable.Executable}.
     */
    protected void checkExecutableSummaryConsistency()
    {
        EntityManager entityManager = getEntityManagerFactory().createEntityManager();
        try {
            String executableQuery = NativeQuery.getNativeQuery(NativeQuery.EXECUTABLE_SUMMARY_CHECK);

            List<Object[]> executables = entityManager.createNativeQuery(executableQuery).getResultList();
            for (Object[] record : executables) {
                logger.error("Uncached executables: " + Arrays.toString(record));
            }
            if (!executables.isEmpty()) {
                List<Object[]> allCachedExecutables = entityManager.createNativeQuery("SELECT * FROM executable_summary").getResultList();
                for (Object[] record : allCachedExecutables) {
                    logger.error("Cached executables: " + Arrays.toString(record));
                }
            }
            Assert.assertTrue("Some executables has not been cached in table executable_summary.", executables.isEmpty());
        } finally {
            entityManager.close();
        }
    }

    /**
     * @return list of {@link ExecutableSummary}
     */
    public List<ExecutableSummary> listExecutable()
    {
        return getExecutableService().listExecutables(new ExecutableListRequest(SECURITY_TOKEN)).getItems();
    }

    /**
     * @param executableId
     * @param executableClass
     * @return executable for given {@code executableId}
     */
    public <T extends cz.cesnet.shongo.controller.api.Executable> T getExecutable(String executableId,
            Class<T> executableClass)
    {
        return executableClass.cast(getExecutableService().getExecutable(SECURITY_TOKEN_ROOT, executableId));
    }

    /**
     * @param roomExecutableId
     * @return {@link Room} for room executable with given {@code roomExecutableId}
     */
    public Room getRoom(String roomExecutableId)
    {
        RoomExecutable roomExecutable = getExecutable(roomExecutableId, RoomExecutable.class);
        return getResourceControlService().getRoom(SECURITY_TOKEN_ROOT,
                roomExecutable.getResourceId(), roomExecutable.getRoomId());
    }

    /**
     * @param executableId
     * @param serviceClass
     * @return {@link cz.cesnet.shongo.controller.api.ExecutableService} for {@link Executable} with given {@code roomExecutableId}
     */
    public <T extends cz.cesnet.shongo.controller.api.ExecutableService> T getExecutableService(
            String executableId, Class<T> serviceClass)
    {
        ExecutableServiceListRequest request = new ExecutableServiceListRequest(SECURITY_TOKEN_ROOT);
        request.setExecutableId(executableId);
        request.addServiceClass(serviceClass);
        request.setCount(1);
        ListResponse<cz.cesnet.shongo.controller.api.ExecutableService> response =
                getExecutableService().listExecutableServices(request);
        if (response.getCount() > 1) {
            throw new RuntimeException(
                    "Executable " + executableId + " has multiple " + serviceClass.getSimpleName() + ".");
        }
        if (response.getItemCount() > 0) {
            return serviceClass.cast(response.getItem(0));
        }
        else {
            return null;
        }
    }

    /**
     * Testing connector agent.
     */
    protected static class TestAgent extends Agent
    {
        /**
         * Specifies whether device is disabled and should throw error for every request.
         */
        protected boolean disabled = false;

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
         * @return {@link #disabled}
         */
        public boolean isDisabled()
        {
            return disabled;
        }

        /**
         * @param disabled sets the {@link #disabled}
         */
        public void setDisabled(boolean disabled)
        {
            this.disabled = disabled;
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
         * @param index of the performed command
         * @return performed {@link Command} at given {@code index}
         */
        public <T extends Command> T getPerformedCommand(int index, Class<T> commandType)
        {
            return commandType.cast(performedCommands.get(index));
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

        /**
         * @throws CommandDisabledException if {@link #disabled} is {@code true}
         */
        protected void checkDisabled()
        {
            if (disabled) {
                throw new CommandDisabledException();
            }
        }

        @Override
        protected void setup()
        {
            addOntology(ConnectorOntology.getInstance());
            super.setup();
        }

        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException
        {
            checkDisabled();
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
         * Rooms.
         */
        private Map<String, Room> rooms = new HashMap<String, Room>();

        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException
        {
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

    public class RecordableTestAgent extends TestAgent
    {
        Map<String, List<String>> recordingIdsByFolderId = new HashMap<String, List<String>>();

        Map<String, String> folderIdByRecordingId = new HashMap<String, String>();

        Map<Alias, String> activeRecordingIdByAlias = new HashMap<Alias, String>();

        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException
        {
            Object result = super.handleCommand(command, sender);
            if (command instanceof CreateRecordingFolder) {
                String folderId = "folder" + (recordingIdsByFolderId.size() + 1);
                recordingIdsByFolderId.put(folderId, new LinkedList<String>());
                return folderId;
            }
            else if (command instanceof ListRecordings) {
                ListRecordings listRecordings = (ListRecordings) command;
                String folderId = listRecordings.getRecordingFolderId();
                List<String> folderRecordingIds = recordingIdsByFolderId.get(folderId);
                if (folderRecordingIds == null) {
                    throw new IllegalArgumentException("Folder '" + folderId + "' doesn't exist.");
                }
                List<Recording> recordings = new LinkedList<Recording>();
                for (String recordingId : folderRecordingIds) {
                    Recording recording = new Recording();
                    recording.setId(recordingId);
                    recording.setRecordingFolderId(folderId);
                    recordings.add(recording);
                }
                return recordings;
            }
            else if (command instanceof GetActiveRecording) {
                GetActiveRecording getActiveRecording = (GetActiveRecording) command;
                Alias alias = getActiveRecording.getAlias();
                String recordingId = activeRecordingIdByAlias.get(alias);
                if (recordingId == null) {
                    return null;
                }
                Recording recording = new Recording();
                recording.setId(recordingId);
                return recording;
            }
            else if (command instanceof IsRecordingActive) {
                IsRecordingActive isRecordingActive = (IsRecordingActive) command;
                String recordingId = isRecordingActive.getRecordingId();
                return activeRecordingIdByAlias.containsValue(recordingId);
            }
            else if (command instanceof StartRecording) {
                StartRecording startRecording = (StartRecording) command;
                Alias alias = startRecording.getAlias();
                String folderId = startRecording.getRecordingFolderId();
                String recordingId = "recording" + (folderIdByRecordingId.size() + 1);
                activeRecordingIdByAlias.put(alias, recordingId);
                folderIdByRecordingId.put(recordingId, folderId);
                return recordingId;
            }
            else if (command instanceof StopRecording) {
                StopRecording stopRecording = (StopRecording) command;
                String recordingId = stopRecording.getRecordingId();
                for (Map.Entry<Alias, String> entry : activeRecordingIdByAlias.entrySet()) {
                    if (recordingId.equals(entry.getValue())) {
                        activeRecordingIdByAlias.remove(entry.getKey());
                        break;
                    }
                }
                String folderId = folderIdByRecordingId.get(recordingId);
                List<String> folderRecordingIds = recordingIdsByFolderId.get(folderId);
                if (folderRecordingIds == null) {
                    throw new IllegalArgumentException("Folder '" + folderId + "' doesn't exist.");
                }
                folderRecordingIds.add(recordingId);
            }
            return result;
        }
    }

    /**
     * Testing Connect agent.
     */
    public class ConnectTestAgent extends RecordableTestAgent
    {
        /**
         * Rooms.
         */
        private Map<String, Room> rooms = new HashMap<String, Room>();

        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException
        {
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
