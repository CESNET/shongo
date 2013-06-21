package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.api.jade.PingCommand;
import jade.core.AID;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link SendLocalCommand}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SendLocalCommandTest
{
    private static Logger logger = LoggerFactory.getLogger(SendLocalCommandTest.class);

    private Container jadeContainer;

    @Before
    public void setUp() throws Exception
    {
        jadeContainer = Container.createMainContainer("localhost", 8585, "Shongo");
        if (!jadeContainer.start()) {
            throw new RuntimeException("Failed to start JADE container.");
        }
    }

    @After
    public void tearDown() throws Exception
    {
        jadeContainer.stop();
    }

    /**
     * Test recursive jade communication working (A -> B, B -> A, B <- A, A <- B).
     *
     * @throws Exception
     */
    @Test
    public void testRecursiveMessage() throws Exception
    {
        Agent agent1 = new Agent();
        Agent agent2 = new Agent()
        {
            @Override
            public Object handleCommand(Command command, AID sender)
                    throws CommandException, CommandUnsupportedException
            {
                if (command instanceof PingCommand) {
                    logger.info("Received Ping by {} from {}.", getLocalName(), sender.getLocalName());
                    logger.info("Sending Ping from {} to {}.", getLocalName(), sender.getLocalName());
                    SendLocalCommand sendLocalCommand = sendCommand("agent1", new PingCommand());
                    logger.info("Received Ping Result by {}: {}.", getLocalName(), sendLocalCommand.getResult());
                }
                return super.handleCommand(command, sender);
            }
        };

        jadeContainer.addAgent("agent1", agent1, null);
        jadeContainer.addAgent("agent2", agent2, null);
        jadeContainer.waitForJadeAgentsToStart();

        logger.info("Sending Ping from {} to {}.", agent1.getLocalName(), agent2.getLocalName());
        SendLocalCommand sendLocalCommand = agent1.sendCommand(agent2.getLocalName(), new PingCommand());
        Assert.assertEquals(SendLocalCommand.State.SUCCESSFUL, sendLocalCommand.getState());
        logger.info("Received Ping Result by {}: {}.", agent1.getLocalName(), sendLocalCommand.getResult());
    }
}
