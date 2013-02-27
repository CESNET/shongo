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
        commandSet.addCommand("get-user", "Get user information based on user-id", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (args.length < 2) {
                    Shell.printError("Get user requires user-id as a first argument.");
                    return;
                }
                Command command = container.performCommand(agentName, new AgentActionCommand("Controller",
                        new GetUserInformation(args[1])));
                command.waitForProcessed();
                if (command.getState() == Command.State.SUCCESSFUL) {
                    UserInformation userInformation = (UserInformation) command.getResult();
                    Shell.printInfo("User: %s", userInformation.toString());
                }
                else {
                    CommandFailure commandFailure = command.getFailure();
                    Shell.printError("Get user failed: %s", commandFailure.getMessage());
                    if (commandFailure.getCause() != null) {
                        commandFailure.getCause().printStackTrace();
                    }
                }
            }
        });

        return commandSet;
    }
}
