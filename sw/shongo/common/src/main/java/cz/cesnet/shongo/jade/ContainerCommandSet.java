package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.jade.PingCommand;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.CommandSet;
import cz.cesnet.shongo.shell.Shell;
import org.apache.commons.cli.CommandLine;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Represents a Shell command set for a JADE container.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ContainerCommandSet extends CommandSet
{
    /**
     * Create command set for the JADE container.
     *
     * @param container
     */
    public static ContainerCommandSet createContainerCommandSet(final Container container)
    {
        ContainerCommandSet commandSet = new ContainerCommandSet();
        commandSet.addCommand("status", "Print a status of the JADE container", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                container.printStatus();
            }
        });
        commandSet.addCommand("gui", "Show/hide JADE Management GUI", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                if (container.hasManagementGui()) {
                    container.removeManagementGui();
                }
                else {
                    container.addManagementGui();
                }
            }
        });
        return commandSet;
    }

    /**
     * Create command set for the JADE agent in the given JADE container.
     *
     * @param container
     */
    public static ContainerCommandSet createContainerAgentCommandSet(final Container container, final String agentName)
    {
        ContainerCommandSet commandSet = new ContainerCommandSet();
        commandSet.addCommand("ping", "Ping to another agent", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (args.length < 2) {
                    Shell.printError("Ping requires agent name as a first argument.");
                    return;
                }
                DateTime start = DateTime.now();
                SendLocalCommand sendLocalCommand = container.sendAgentCommand(agentName, args[1],
                        new PingCommand());
                if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
                    DateTime middle = (DateTime) sendLocalCommand.getResult();
                    DateTime end = DateTime.now();
                    long duration = new Interval(start, end).toDurationMillis();
                    DateTimeFormatter formatter = ISODateTimeFormat.hourMinuteSecondMillis();
                    Shell.printInfo("Ping Succeeded: %d ms (%s -> %s -> %s)", duration,
                            formatter.print(start), formatter.print(middle), formatter.print(end));
                }
                else {
                    Shell.printError("Ping failed: %s", sendLocalCommand.getJadeReport().getMessage());
                }
            }
        });
        return commandSet;
    }
}
