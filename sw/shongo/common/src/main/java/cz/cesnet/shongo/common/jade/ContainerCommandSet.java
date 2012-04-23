package cz.cesnet.shongo.common.jade;

import cz.cesnet.shongo.common.jade.command.SendCommand;
import cz.cesnet.shongo.common.shell.CommandHandler;
import cz.cesnet.shongo.common.shell.CommandSet;
import cz.cesnet.shongo.common.shell.Shell;
import org.apache.commons.cli.CommandLine;

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

        commandSet.addCommand("send", "Send a message to another JADE agent", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (commandLine.getArgs().length < 3) {
                    Shell.printError("The send command requires two parameters: <AGENT> <MESSAGE>.");
                    return;
                }
                container.performCommand(agentName, SendCommand.createSendMessage(args[1], args[2]));
            }
        });
        return commandSet;
    }
}
