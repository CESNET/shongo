package cz.cesnet.shongo.shell;

import jline.console.ConsoleReader;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Represents an interactive shell in which commands
 * can be executed.
 * Commands must be specified by <code>addCommand</code> methods
 * and then the shell shall be run by <code>run</code> method.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Shell extends CommandSet
{
    private static Logger logger = LoggerFactory.getLogger(Shell.class);

    /**
     * Console reader.
     */
    ConsoleReader console;

    /**
     * Predefined exit command.
     */
    private Command exitCommand;

    /**
     * Predefined help command.
     */
    private Command helpCommand;

    /**
     * Command prompt string.
     */
    private String prompt = "cmd";

    /**
     * Exit exception.
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
     * Set shell prompt string.
     *
     * @param prompt
     */
    public void setPrompt(String prompt)
    {
        this.prompt = prompt;
    }

    /**
     * Set exit command properties.
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
     * Set help command properties.
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
     * Print info message with automatic newline.
     *
     * @param info
     * @param objects
     */
    public static void printInfo(String info, java.lang.Object... objects)
    {
        System.out.printf("%s\n", String.format(info, objects));

        Shell activeShell = getActive();
        if (activeShell != null) {
            activeShell.rePrompt();
        }
    }

    /**
     * Print error message with automatic newline.
     *
     * @param error
     * @param objects
     */
    public static void printError(String error, java.lang.Object... objects)
    {
        System.err.printf("[ERROR] %s\n", String.format(error, objects));

        Shell activeShell = getActive();
        if (activeShell != null) {
            activeShell.rePrompt();
        }
    }

    /**
     * Run shell loop.
     */
    public void run()
    {
        System.setProperty("jline.shutdownhook", "true");

        try {
            console = new ConsoleReader();
            console.setHistoryEnabled(true);
            console.addCompleter(new ShellCompleter(this));
            console.setCompletionHandler(new ShellCompletionHandler());

            // Run loop
            while (true) {
                active = this;
                String line = console.readLine(prompt + "> ");
                active = null;
                if (!parserCommandLine(line)) {
                    System.out.printf("\n");
                    break;
                }
            }

            console = null;
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to create shell console.", exception);
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
        if (line == null) {
            return false;
        }
        if (line.isEmpty()) {
            return true;
        }
        // Parse command-line arguments from string
        String[] args = CommandLineParser.parse(line);

        if (args.length == 0) {
            return true;
        }
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
     * Re-prompt thread.
     */
    public class RePromptThread extends Thread
    {
        long at;

        @Override
        public void run()
        {
            while (System.nanoTime() < at) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                }
            }
            try {
                console.getOutput().write(console.getPrompt() + console.getCursorBuffer().toString());
                console.getOutput().flush();
                synchronized (Shell.class) {
                    thread = null;
                }
            }
            catch (Exception exception) {
            }
        }
    }

    /**
     * Active re-prompt thread.
     */
    static RePromptThread thread;

    /**
     * Perform re-prompt. Show again the prompt on a newline.
     */
    public void rePrompt()
    {
        synchronized (Shell.class) {
            if (thread == null) {
                try {
                    console.getOutput().write("\n");
                    console.getOutput().flush();
                }
                catch (IOException e) {
                }
                try {
                    thread = new RePromptThread();
                    thread.start();
                }
                catch (Exception exception) {
                    logger.error("Failed to run re-prompt thread for shell.", exception);
                }
            }
            if (thread != null) {
                thread.at = System.nanoTime() + 1000000;
            }
        }
    }

    /**
     * Active shell instance.
     */
    private static Shell active;

    /**
     * Get active shell.
     *
     * @return active shell
     */
    public static Shell getActive()
    {
        return active;
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
