package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.fault.jade.CommandFailure;
import cz.cesnet.shongo.fault.jade.CommandTimeout;
import cz.cesnet.shongo.fault.jade.CommandUnknownFailure;
import jade.core.AID;

/**
 * {@link LocalCommand} for sending an {@link Command} to target receiver agent via JADE middle-ware.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SendLocalCommand extends LocalCommand
{
    /**
     * How long to wait for command result. Unit: milliseconds
     * <p/>
     * NOTE: some commands, e.g. dialing, may take up to 30 seconds on some devices...
     */
    public static final int COMMAND_TIMEOUT = 33000;

    /**
     * {@link AID} of the receiver agent.
     */
    private AID receiverAgentId;

    /**
     * {@link Command} which should be sent.
     */
    private Command command;

    /**
     * Current command state.
     */
    private State state;

    /**
     * {@link cz.cesnet.shongo.fault.jade.CommandFailure}.
     */
    private CommandFailure failure;

    /**
     * Result of the command.
     */
    private Object result;

    /**
     * Constructor.
     *
     * @param receiverAgentName name of the receiver agent
     * @param command           which should be performed on the receiver agent
     */
    public SendLocalCommand(String receiverAgentName, Command command)
    {
        if (receiverAgentName.contains("@")) {
            this.receiverAgentId = new AID(receiverAgentName, AID.ISGUID);
        }
        else {
            this.receiverAgentId = new AID(receiverAgentName, AID.ISLOCALNAME);
        }
        this.command = command;
        this.state = State.UNKNOWN;
    }

    /**
     * @return {@link #receiverAgentId}
     */
    public AID getReceiverAgentId()
    {
        return receiverAgentId;
    }

    /**
     * @return {@link #command}
     */
    public Command getCommand()
    {
        return command;
    }

    @Override
    public String getName()
    {
        return command.getClass().getSimpleName();
    }

    /**
     * @return {@link #state}
     */
    public State getState()
    {
        return state;
    }

    /**
     * @return true if command was already processed,
     *         false otherwise
     */
    public boolean isProcessed()
    {
        return state != State.UNKNOWN;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    /**
     * Sets the {@link #state} as {@link State#FAILED}.
     *
     * @param failure sets the {@link #failure}
     */
    public void setFailed(CommandFailure failure)
    {
        this.state = State.FAILED;
        this.failure = failure;
        if (this.failure != null) {
            this.failure.setCommand(getName());
        }
    }

    /**
     * @return {@link #failure} or {@link cz.cesnet.shongo.fault.jade.CommandUnknownFailure}
     *         when the {@link #failure} is null and the {@link #state} is {@link State#FAILED}
     */
    public CommandFailure getFailure()
    {
        if (failure == null && this.state == State.FAILED) {
            return new CommandUnknownFailure();
        }
        return failure;
    }

    /**
     * @return {@link #result}
     */
    public Object getResult()
    {
        return result;
    }

    /**
     * @param result sets the {@link #result}
     */
    public void setResult(Object result)
    {
        this.result = result;
    }

    /**
     * Wait for the command to be processed
     */
    public void waitForProcessed()
    {
        final int waitingTime = 50;

        // FIXME: use some kind of IPC instead of busy waiting
        int count = COMMAND_TIMEOUT / waitingTime;
        while (!isProcessed() && count > 0) {
            count--;
            try {
                Thread.sleep(waitingTime);
            }
            catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
        if (getState() == State.UNKNOWN) {
            setFailed(new CommandTimeout());
        }
    }

    @Override
    public void process(Agent localAgent) throws LocalCommandException
    {
        try {
            localAgent.addBehaviour(new CommandRequesterBehaviour(localAgent, this));
        }
        catch (Exception exception) {
            throw new LocalCommandException("Error in sending the command.", exception);
        }
    }

    /**
     * State of the command.
     */
    public static enum State
    {
        /**
         * Unknown state.
         */
        UNKNOWN,

        /**
         * Command was performed successfully.
         */
        SUCCESSFUL,

        /**
         * Command failed.
         */
        FAILED,
    }
}
