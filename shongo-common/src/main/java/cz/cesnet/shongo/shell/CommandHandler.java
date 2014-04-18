package cz.cesnet.shongo.shell;

import org.apache.commons.cli.CommandLine;

/**
 * Handler for Shell Command. It represents
 * action that is performed when command
 * is executed in a shell.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface CommandHandler
{
    /**
     * Method that is called when command is executed in a shell.
     *
     * @param commandLine Command-line arguments of the executed command
     */
    public abstract void perform(CommandLine commandLine);
}
