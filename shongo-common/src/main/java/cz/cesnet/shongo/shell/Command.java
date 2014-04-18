package cz.cesnet.shongo.shell;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Represents a shell command which can be executed in a shell.
 * Command consists of keyword, help string and handler that
 * is performed when the command is executed in a shell.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Command
{
    /**
     * Command keyword.
     */
    private String command;

    /**
     * Command help string.
     */
    private String help;

    /**
     * Command handler.
     */
    private CommandHandler handler;

    /**
     * Command options.
     */
    private Options options = new Options();

    /**
     * Construct a command.
     *
     * @param command
     * @param help
     * @param handler
     */
    public Command(String command, String help, CommandHandler handler)
    {
        this.command = command;
        this.help = help;
        this.handler = handler;
    }

    /**
     * Construct a command.
     *
     * @param command
     * @param handler
     */
    public Command(String command, CommandHandler handler)
    {
        this(command, null, handler);
    }

    /**
     * Get command keyword.
     *
     * @return command keyword
     */
    public String getCommand()
    {
        return command;
    }

    /**
     * Set command keyword.
     *
     * @param command
     */
    public void setCommand(String command)
    {
        this.command = command;
    }

    /**
     * Get command help string.
     *
     * @return command help string
     */
    public String getHelp()
    {
        return help;
    }

    /**
     * Set command help string.
     *
     * @param help
     */
    public void setHelp(String help)
    {
        this.help = help;
    }

    /**
     * Get command handler.
     *
     * @return command handler
     */
    public CommandHandler getHandler()
    {
        return handler;
    }

    /**
     * Get command options.
     *
     * @return command options
     */
    public Options getOptions()
    {
        return options;
    }

    /**
     * Added command line option
     *
     * @param option
     * @return this command
     */
    public Command addOption(Option option)
    {
        options.addOption(option);
        return this;
    }
}
