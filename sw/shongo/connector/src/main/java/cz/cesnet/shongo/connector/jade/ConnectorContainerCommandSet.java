package cz.cesnet.shongo.connector.jade;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.jade.action.GetUserInformation;
import cz.cesnet.shongo.fault.jade.CommandFailure;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.jade.LocalCommand;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
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
                SendLocalCommand sendLocalCommand = container.sendAgentCommand(agentName, "Controller",
                        new GetUserInformation(args[1]));
                if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
                    UserInformation userInformation = (UserInformation) sendLocalCommand.getResult();
                    Shell.printInfo("User: %s", userInformation.toString());
                }
                else {
                    CommandFailure commandFailure = sendLocalCommand.getFailure();
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
