package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.CommandSet;
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
     * @param agent
     */
    public static ContainerCommandSet createContainerAgentCommandSet(final Container container, final Agent agent)
    {
        return new ContainerCommandSet();
    }
}
