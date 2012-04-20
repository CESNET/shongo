package cz.cesnet.shongo.common.shell;

import jline.console.ConsoleReader;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an interactive shell in which commands
 * can be executed.
 * Commands must be specified by <code>addCommand</code> methods
 * and then the shell shall be run by <code>run</code> method.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Shell
{
    private static Logger logger = LoggerFactory.getLogger(Shell.class);

    /**
     * Predefined exit command
     */
    private Command exitCommand;

    /**
     * Predefined help command
     */
    private Command helpCommand;

    /**
     * List of supported commands
     */
    private Map<String, Command> commands = new HashMap<String, Command>();

    /**
     * Command prompt string
     */
    private String prompt = "cmd";

    /**
     * Exit exception
     */
    private static class ExitException extends RuntimeException
    {
    }

    /**
     * Construct a shell
     */
    public Shell()
    {
        exitCommand = new Command("exit", "Exit the shell", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                throw new ExitException();
            }
        });
        helpCommand = new Command("help", "Show available commands for the shell", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                printHelp(args.length > 1 ? args[1] : null);
            }
        });
        addCommand(exitCommand);
        addCommand(helpCommand);
    }

    /**
     * Set shell prompt string
     *
     * @param prompt
     */
    public void setPrompt(String prompt)
    {
        this.prompt = prompt;
    }

    /**
     * Set exit command properties
     *
     * @param command
     * @param help
     */
    public void setExitCommand(String command, String help)
    {
        this.exitCommand.setCommand(command);
        this.exitCommand.setHelp(help);
    }

    /**
     * Set help command properties
     *
     * @param command
     * @param help
     */
    public void setHelpCommand(String command, String help)
    {
        this.helpCommand.setCommand(command);
        this.helpCommand.setHelp(help);
    }

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

    /**
     * Print help about provided commands.
     */
    public void printHelp(String forCommand)
    {
        if (forCommand == null) {
            for (Command command : commands.values()) {
                String help = command.getHelp();
                if (help == null) {
                    help = "No help";
                }
                System.out.printf("%-10s %s\n", command.getCommand(), help);
            }
        }
        else {
            Command command = commands.get(forCommand);
            if (command == null) {
                printError("Cannot show help for unknown command '%s'!", forCommand);
                return;
            }
            System.out.println(command.getHelp());
            if (command.getOptions().getOptions().size() > 0) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(forCommand, command.getOptions());
            }
        }
    }

    /**
     * Print error message
     *
     * @param error
     * @param objects
     */
    public static void printError(String error, java.lang.Object... objects)
    {
        System.err.printf("[ERROR] %s\n", String.format(error, objects));
    }

    /**
     * Run shell loop.
     */
    public void run()
    {
        System.setProperty("jline.shutdownhook", "true");

        try {
            ConsoleReader console = new ConsoleReader();
            console.setHistoryEnabled(true);
            console.addCompleter(new ShellCompleter(this));
            console.setCompletionHandler(new ShellCompletionHandler());
            while (true) {
                String line = console.readLine(prompt + "> ");
                if (parserCommandLine(line) == false) {
                    break;
                }
            }
        }
        catch (Exception exception) {
            logger.error("Failed to create shell console.", exception);
        }
    }

    /**
     * Parse command-line executed in the shell.
     *
     * @param line
     * @return true if shell should continue,
     *         false if shell should exit
     */
    private boolean parserCommandLine(String line)
    {
        if (line.isEmpty()) {
            return true;
        }
        // Parse command-line arguments from string
        String[] args = CommandLineParser.parse(line);

        Command command = commands.get(args[0]);
        if (command == null) {
            printError("Unknown command '%s'!", args[0]);
            return true;
        }

        CommandLine commandLine = null;
        try {
            org.apache.commons.cli.CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(command.getOptions(), args);
        }
        catch (ParseException e) {
            printError(e.getMessage());
            return true;
        }

        CommandHandler handler = command.getHandler();
        try {
            handler.perform(commandLine);
        }
        catch (ExitException exception) {
            return false;
        }
        return true;
    }

    /**
     * Main method for testing a shell.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        Shell shell = new Shell();
        shell.setPrompt("command");
        Command command = shell.addCommand("status", "Show status", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                if (commandLine.hasOption("version")) {
                    System.out.println("Show status with version=" + commandLine.getOptionValue("version"));
                }
                else {
                    System.out.println("Show status");
                }
            }
        });
        command.addOption(OptionBuilder.withLongOpt("version").hasArg().create("v"));
        command.addOption(OptionBuilder.withLongOpt("info").create("i"));
        shell.run();
    }
}
