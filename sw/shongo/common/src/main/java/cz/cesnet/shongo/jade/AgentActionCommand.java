package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.jade.AgentAction;
import jade.core.AID;

/**
 * {@link Command} to send an {@link AgentAction} to an agent via JADE middle-ware.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class AgentActionCommand extends Command
{
    /**
     * {@link AgentAction} which should be sent.
     */
    private AgentAction agentAction;

    /**
     * {@link AID} of the receiver agent.
     */
    private AID agentReceiverId;

    /**
     * Construct command that sends a action to another agent.
     *
     * @param agentReceiverName name of the receiver agent
     * @param agentAction       agent action which should be performed on the receiver agent
     */
    public AgentActionCommand(String agentReceiverName, AgentAction agentAction)
    {
        if (agentReceiverName.contains("@")) {
            this.agentReceiverId = new AID(agentReceiverName, AID.ISGUID);
        }
        else {
            this.agentReceiverId = new AID(agentReceiverName, AID.ISLOCALNAME);
        }
        this.agentAction = agentAction;
    }

    /**
     * @return {@link #agentAction}
     */
    public AgentAction getAgentAction()
    {
        return agentAction;
    }

    /**
     * @return {@link #agentReceiverId}
     */
    public AID getAgentReceiverId()
    {
        return agentReceiverId;
    }

    @Override
    public String getName()
    {
        return agentAction.getClass().getSimpleName();
    }

    @Override
    public void process(Agent agent) throws CommandException
    {
        try {
            agent.addBehaviour(new AgentActionRequesterBehaviour(agent, this));
        }
        catch (Exception exception) {
            throw new CommandException("Error in requesting agent action.", exception);
        }
    }
}
