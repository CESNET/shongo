package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.api.jade.PingCommand;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.api.jade.endpoint.Mute;
import cz.cesnet.shongo.connector.api.jade.endpoint.SetMicrophoneLevel;
import cz.cesnet.shongo.connector.api.jade.endpoint.Unmute;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.AbstractExecutorTest;
import cz.cesnet.shongo.controller.api.jade.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.notification.Notification;
import cz.cesnet.shongo.controller.notification.manager.NotificationExecutor;
import cz.cesnet.shongo.controller.notification.NotificationMessage;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.SendLocalCommand;
import jade.core.AID;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for serializing API classes for JADE.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class JadeServiceTest extends AbstractExecutorTest
{
    private final static Room ROOM_API;

    static {
        ROOM_API = new Room();
        ROOM_API.setId("roomId");
        ROOM_API.addAlias(AliasType.ROOM_NAME, "roomName");
        ROOM_API.setLicenseCount(99);
    }

    /**
     * @see TestingNotificationExecutor
     */
    private TestingNotificationExecutor notificationExecutor = new TestingNotificationExecutor();

    @Override
    protected void onStart()
    {
        super.onStart();

        cz.cesnet.shongo.controller.Controller controller = getController();
        controller.addNotificationExecutor(notificationExecutor);
        controller.setJadeService(new ServiceImpl(getEntityManagerFactory(),
                controller.getConfiguration(), controller.getNotificationManager(), getExecutor(),
                controller.getAuthorization())
        {
            @Override
            public Room getRoom(String agentName, String roomId) throws CommandException
            {
                Assert.assertEquals(ROOM_API.getId(), roomId);
                return ROOM_API;
            }
        });
        controller.startJade();
    }

    /**
     * @return name of {@link cz.cesnet.shongo.controller.ControllerAgent}
     */
    private String getControllerAgentName()
    {
        return getController().getAgent().getLocalName();
    }

    @Test
    public void test() throws Exception
    {
        cz.cesnet.shongo.controller.Controller controller = getController();
        TestAgent testAgent = controller.addJadeAgent("agent", new TestAgent());
        controller.waitForJadeAgentsToStart();

        cz.cesnet.shongo.controller.ControllerAgent controllerAgent = controller.getAgent();
        SendLocalCommand sendLocalCommand;

        // Test getUserInformation
        sendLocalCommand = controllerAgent.sendCommand(testAgent.getLocalName(), new PingCommand());
        Assert.assertEquals(SendLocalCommand.State.SUCCESSFUL, sendLocalCommand.getState());

        // Test getRoom
        sendLocalCommand = controllerAgent.sendCommand(testAgent.getLocalName(), new Mute());
        Assert.assertEquals(SendLocalCommand.State.SUCCESSFUL, sendLocalCommand.getState());

        // Test notifyTarget single-lingual
        sendLocalCommand = controllerAgent.sendCommand(testAgent.getLocalName(), new Unmute());
        Assert.assertEquals(SendLocalCommand.State.SUCCESSFUL, sendLocalCommand.getState());
        Assert.assertEquals(1, notificationExecutor.getNotificationCount());

        // Test notifyTarget multi-lingual
        sendLocalCommand = controllerAgent.sendCommand(testAgent.getLocalName(), new SetMicrophoneLevel());
        Assert.assertEquals(SendLocalCommand.State.SUCCESSFUL, sendLocalCommand.getState());
        Assert.assertEquals(2, notificationExecutor.getNotificationCount());
    }

    /**
     * Testing {@link Agent}.
     */
    public class TestAgent extends Agent
    {
        @Override
        protected void setup()
        {
            addOntology(ConnectorOntology.getInstance());
            addOntology(ControllerOntology.getInstance());
            super.setup();
        }

        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException, CommandUnsupportedException
        {
            if (command instanceof PingCommand) {
                SendLocalCommand sendLocalCommand = sendCommand(getControllerAgentName(),
                        GetUserInformation.byUserId(Authorization.ROOT_USER_ID));
                Assert.assertEquals(SendLocalCommand.State.SUCCESSFUL, sendLocalCommand.getState());
                UserInformation userInformation = (UserInformation) sendLocalCommand.getResult();
                Assert.assertEquals(Authorization.ROOT_USER_DATA.getFullName(), userInformation.getFullName());
                return userInformation;
            }
            else if (command instanceof Mute) {
                SendLocalCommand sendLocalCommand = sendCommand(getControllerAgentName(),
                        new GetRoom(ROOM_API.getId()));
                Assert.assertEquals(SendLocalCommand.State.SUCCESSFUL, sendLocalCommand.getState());
                Room roomApi = (Room) sendLocalCommand.getResult();
                Assert.assertEquals(ROOM_API.getId(), roomApi.getId());
                Assert.assertEquals(ROOM_API.getName(), roomApi.getName());
                Assert.assertEquals(ROOM_API.getLicenseCount(), roomApi.getLicenseCount());
                return roomApi;
            }
            else if (command instanceof Unmute) {
                NotifyTarget notifyTarget = new NotifyTarget(
                        Service.NotifyTargetType.USER, Authorization.ROOT_USER_ID, "title", "message");
                SendLocalCommand sendLocalCommand = sendCommand(getControllerAgentName(), notifyTarget);
                Assert.assertEquals(SendLocalCommand.State.SUCCESSFUL, sendLocalCommand.getState());
                return null;
            }
            else if (command instanceof SetMicrophoneLevel) {
                NotifyTarget notifyTarget = new NotifyTarget(Service.NotifyTargetType.USER, Authorization.ROOT_USER_ID);
                notifyTarget.addMessage("en", "title", "message");
                notifyTarget.addMessage("cs", "titulek", "zpráva");
                SendLocalCommand sendLocalCommand = sendCommand(getControllerAgentName(), notifyTarget);
                Assert.assertEquals(SendLocalCommand.State.SUCCESSFUL, sendLocalCommand.getState());
                return null;
            }
            return super.handleCommand(command, sender);
        }
    }

    /**
     * {@link cz.cesnet.shongo.controller.notification.manager.NotificationExecutor} for testing.
     */
    private static class TestingNotificationExecutor extends NotificationExecutor
    {
        /**
         * Number of executed notifications.
         */
        private int notificationCount = 0;

        /**
         * @return {@link #notificationCount}
         */
        public int getNotificationCount()
        {
            return notificationCount;
        }

        @Override
        public void executeNotification(Notification notification)
        {
            StringBuilder recipientString = new StringBuilder();
            for (PersonInformation recipient : notification.getRecipients()) {
                if (recipientString.length() > 0) {
                    recipientString.append(", ");
                }
                recipientString.append(String.format("%s (%s)", recipient.getFullName(),
                        recipient.getPrimaryEmail()));
            }
            PersonInformation firstRecipient = notification.getRecipients().iterator().next();
            NotificationMessage message = notification.getRecipientMessage(firstRecipient);
            logger.debug("Notification '{}' for {}...\n{}", new Object[]{message.getTitle(),
                    recipientString.toString(), message.getContent()
            });
            notificationCount++;
        }
    }
}
