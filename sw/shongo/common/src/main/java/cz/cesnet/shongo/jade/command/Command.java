package cz.cesnet.shongo.jade.command;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.jade.Agent;

import java.util.UUID;

/**
 * Represents a command for a JADE agent.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Command
{
    /**
     * How long to wait for command result. Unit: milliseconds
     * <p/>
     * NOTE: some commands, e.g. dialing, may take up to 30 seconds on some devices...
     */
    public static final int COMMAND_TIMEOUT = 33000;

    /**
     * Command unique identifier.
     */
    private String identifier;

    /**
     * Current command state.
     */
    private State state;

    /**
     * State description.
     */
    private String stateDescription;

    /**
     * Result of the command.
     */
    private Object result;

    /**
     * Constructor.
     */
    public Command()
    {
        identifier = UUID.randomUUID().toString();
        state = State.UNKNOWN;
    }

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
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
     * @param state            sets the {@link #state}
     * @param stateDescription sets the {@link #stateDescription}
     */
    public void setState(State state, String stateDescription)
    {
        this.state = state;
        this.stateDescription = stateDescription;
    }

    /**
     * @return {@link #stateDescription}
     */
    public String getStateDescription()
    {
        return stateDescription;
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
     * Process this command on an agent.
     *
     * @param agent
     */
    public abstract void process(Agent agent) throws CommandException, CommandUnsupportedException;

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
        if (getState() == Command.State.UNKNOWN) {
            setState(Command.State.FAILED, "Timeout");
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
