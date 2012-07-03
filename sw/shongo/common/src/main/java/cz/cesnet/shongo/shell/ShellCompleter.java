package cz.cesnet.shongo.shell;

import jline.console.completer.Completer;
import org.apache.commons.cli.Option;

import java.util.Collection;
import java.util.List;

/**
 * Shell completer
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ShellCompleter implements Completer
{
    /**
     * Shell instance.
     */
    private Shell shell;

    /**
     * Construct shell completer.
     *
     * @param shell
     */
    public ShellCompleter(Shell shell)
    {
        this.shell = shell;
    }

    /**
     * Get candidates for completion.
     *
     * @param buffer
     * @param cursor
     * @param candidates
     * @return true if at least one candidate was found.
     */
    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates)
    {
        List<Command> commands = shell.getCommands();

        // For empty prompt enumerate all command
        if (buffer == null || buffer.length() == 0) {
            for (Command command : commands) {
                candidates.add(command.getCommand());
            }
        }
        // Otherwise filter commands by prompt content
        else {
            for (Command command : commands) {
                String keyword = command.getCommand();
                Collection options = command.getOptions().getOptions();
                // Candidates for commands
                if (keyword.startsWith(buffer)) {
                    candidates.add(keyword);
                }
                // Candidates for commands options
                else if (options.size() > 0) {
                    String bufferPrefix = buffer.substring(0, Math.min(buffer.length(), keyword.length()));
                    if (keyword.startsWith(bufferPrefix)) {
                        String bufferSuffix = "";
                        int position = buffer.lastIndexOf(" ");
                        if (position != -1) {
                            bufferSuffix = buffer.substring(position + 1, buffer.length());
                        }
                        for (Object item : options) {
                            Option option = (Option) item;
                            String optionKeyword = "--" + option.getLongOpt();
                            if (optionKeyword.startsWith(bufferSuffix)) {
                                candidates.add(optionKeyword);
                            }
                        }
                    }
                }
            }
        }

        if (candidates.size() == 1) {
            candidates.set(0, candidates.get(0) + " ");
        }

        return candidates.isEmpty() ? -1 : 0;
    }
}
