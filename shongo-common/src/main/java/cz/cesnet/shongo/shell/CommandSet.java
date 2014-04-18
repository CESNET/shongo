package cz.cesnet.shongo.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a set of commands that a shell can perform.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandSet
{
    /**
     * List of supported commands
     */
    protected Map<String, Command> commands = new HashMap<String, Command>();

    /**
     * Add command that shell can perform.
     *
     * @param command
     * @return new command
     */
    public Command addCommand(Command command)
    {
        commands.put(command.getCommand(), command);
        return command;
    }

    /**
     * Add command that shell can perform.
     *
     * @param command
     * @param help
     * @param handler
     * @return new command
     */
    public Command addCommand(String command, String help, CommandHandler handler)
    {
        return addCommand(new Command(command, help, handler));
    }

    /**
     * Add command that shell can perform.
     *
     * @param command
     * @param handler
     * @return new command
     */
    public Command addCommand(String command, CommandHandler handler)
    {
        return addCommand(new Command(command, handler));
    }

    /**
     * Add commands that shell can perform.
     *
     * @param commandGroup
     */
    public void addCommands(CommandSet commandGroup)
    {
        for (Command command : commandGroup.commands.values()) {
            commands.put(command.getCommand(), command);
        }
    }

    /**
     * Remove commands by set of commands.
     *
     * @param commandGroup
     */
    public void removeCommands(CommandSet commandGroup)
    {
        for (Command command : commandGroup.commands.values()) {
            commands.remove(command.getCommand());
        }
    }

    /**
     * Get list of commands that shell can perform
     *
     * @return list of commands
     */
    public List<Command> getCommands()
    {
        List<Command> commands = new ArrayList<Command>();
        for (Command command : this.commands.values()) {
            commands.add(command);
        }
        return commands;
    }
}
