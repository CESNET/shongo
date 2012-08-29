package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.command.ManageCommand;
import cz.cesnet.shongo.jade.command.SendCommand;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.CommandSet;
import cz.cesnet.shongo.shell.Shell;
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
                if (args.length < 3) {
                    Shell.printError("The send command requires two parameters: <AGENT> <MESSAGE>.");
                    return;
                }
                container.performCommand(agentName, SendCommand.createSendMessage(args[1], args[2]));
            }
        });

        commandSet.addCommand("manage", "Start managing a device", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (args.length < 5) {
                    Shell.printError("The manage command requires four parameters: <CONNECTOR-CLASS> <DEV-ADDRESS> <DEV-USERNAME> <DEV-PASSWORD>.");
                    return;
                }
                Command cmd = new ManageCommand(args[1], args[2], args[3], args[4]);
                container.performCommand(agentName, cmd);
            }
        });

        return commandSet;
    }
}
