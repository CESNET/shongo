package cz.cesnet.shongo.connector.jade;

import cz.cesnet.shongo.connector.jade.command.ManageCommand;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.Shell;
import org.apache.commons.cli.CommandLine;

/**
 * Represents a Shell command set for a JADE connector container.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ConnectorContainerCommandSet extends ContainerCommandSet
{
    public static ContainerCommandSet createContainerAgentCommandSet(final Container container, final String agentName)
    {
        ContainerCommandSet commandSet = ContainerCommandSet.createContainerAgentCommandSet(container, agentName);

        commandSet.addCommand("manage", "Start managing a device", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (args.length < 6) {
                    Shell.printError(
                            "The manage command requires five parameters: <CONNECTOR-CLASS> <DEV-ADDRESS> <DEV-PORT> <DEV-USERNAME> <DEV-PASSWORD>.");
                    return;
                }
                Command cmd = new ManageCommand(args[1], args[2], Integer.valueOf(args[3]), args[4], args[5]);
                container.performCommand(agentName, cmd);
            }
        });

        return commandSet;
    }
}
