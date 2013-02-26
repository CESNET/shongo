package cz.cesnet.shongo.connector.jade;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.jade.action.GetUserInformation;
import cz.cesnet.shongo.fault.jade.CommandFailure;
import cz.cesnet.shongo.jade.AgentActionCommand;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
import cz.cesnet.shongo.jade.Command;
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
                // NOTE: passing options for the connector is not implemented, use the XML configuration file
                Command cmd = new ManageCommand(args[1], args[2], Integer.valueOf(args[3]), args[4], args[5], null);
                container.performCommand(agentName, cmd);
            }
        });

        return commandSet;
    }
}
