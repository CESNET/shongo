package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.JadeReport;
import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.api.jade.Command;
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
     * @see JadeReport
     */
    private JadeReport jadeReport;

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
     * @param failure sets the {@link #jadeReport}
     */
    public void setFailed(JadeReport failure)
    {
        this.state = State.FAILED;
        this.jadeReport = failure;
    }

    /**
     * @return {@link #jadeReport} or {@link JadeReportSet.CommandUnknownErrorReport}
     *         when the {@link #jadeReport} is null and the {@link #state} is {@link State#FAILED}
     */
    public JadeReport getJadeReport()
    {
        if (jadeReport == null && this.state == State.FAILED) {
            return new JadeReportSet.CommandUnknownErrorReport(command.toString(), null);
        }
        return jadeReport;
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
            setFailed(new JadeReportSet.CommandTimeoutReport(command.toString(), receiverAgentId.getName()));
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
