package cz.cesnet.shongo.controller.executor.report;

import cz.cesnet.shongo.fault.jade.CommandFailure;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.report.Report} for {@link cz.cesnet.shongo.controller.executor.Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class CommandFailureReport extends ExecutableReport
{
    /**
     * {@link CommandFailure} which is reported.
     */
    private CommandFailure commandFailure;

    /**
     * Constructor.
     */
    public CommandFailureReport()
    {
    }

    /**
     * Constructor.
     */
    public CommandFailureReport(CommandFailure commandFailure)
    {
        super(DateTime.now());
        if (commandFailure == null) {
            throw new IllegalArgumentException("Command failure should not be null.");
        }
        setCommandFailure(commandFailure);
    }

    /**
     * @return {@link #commandFailure}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public CommandFailure getCommandFailure()
    {
        return commandFailure;
    }

    /**
     * @param commandFailure sets the {@link #commandFailure}
     */
    public void setCommandFailure(CommandFailure commandFailure)
    {
        this.commandFailure = commandFailure;
    }

    @Override
    @Transient
    public String getText()
    {
        if (commandFailure != null) {
            return String.format("Command '%s' failed: %s", commandFailure.getCommand(), commandFailure.getMessage());
        }
        else {
            return "Command failed.";
        }
    }
}
