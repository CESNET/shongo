package cz.cesnet.shongo.controller.executor.report;

import cz.cesnet.shongo.JadeReport;
import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.Temporal;
import org.joda.time.DateTime;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class CommandFailureReport extends ExecutableReport
{
    /**
     * {@link JadeReport} which is reported.
     */
    private JadeReport commandFailure;

    /**
     * Constructor.
     */
    public CommandFailureReport()
    {
    }

    /**
     * Constructor.
     */
    public CommandFailureReport(JadeReport commandFailure)
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
    public JadeReport getCommandFailure()
    {
        return commandFailure;
    }

    /**
     * @param commandFailure sets the {@link #commandFailure}
     */
    public void setCommandFailure(JadeReport commandFailure)
    {
        this.commandFailure = commandFailure;
    }

    @Override
    @Transient
    public State getState()
    {
        return State.ERROR;
    }

    @Override
    @Transient
    public String getText()
    {
        String dateTime = Temporal.formatDateTime(getDateTime());
        if (commandFailure != null) {
            if (commandFailure instanceof JadeReportSet.CommandAbstractErrorReport) {
                JadeReportSet.CommandAbstractErrorReport commandError =
                        (JadeReportSet.CommandAbstractErrorReport) commandFailure;
                return String.format("Command '%s' failed at %s:\n%s",
                        commandError.getCommand(), dateTime, commandError.getMessage());
            }
            return String.format("Command failed at %s:\n%s", dateTime, commandFailure.getMessage());

        }
        else {
            return String.format("Command failed at %s", dateTime);
        }
    }
}
