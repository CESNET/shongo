package cz.cesnet.shongo.shell;

import java.util.ArrayList;
import java.util.List;

/**
 * Help class for parsing commandLine-line into array of string.
 * This class evaluates quotes to be able to pass spaces
 * inside argument.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public final class CommandLineParser
{
    /**
     * Command-line that is parsed
     */
    private final String commandLine;

    /**
     * Position in command line
     */
    private int position = 0;

    /**
     * Construct command-line parser
     *
     * @param commandLine
     */
    private CommandLineParser(String commandLine)
    {
        this.commandLine = commandLine;
    }

    /**
     * Read next token from command-line
     *
     * @return token
     */
    private String readToken()
    {
        StringBuilder builder = new StringBuilder();

        boolean parsingString = false;
        while (position < commandLine.length()) {
            char c = commandLine.charAt(position);

            // String
            if (parsingString) {
                if (c == '"') {
                    break;
                }
                else if (c == '\\' && commandLine.charAt(position + 1) == '"') {
                    builder.append('"');
                    position++;
                }
                else {
                    builder.append(c);
                }
            }
            // Skip whitespaces
            else if (c == ' ' || c == '\t') {
                position++;
                if (builder.length() > 0) {
                    break;
                }
                else {
                    continue;
                }
            }
            // String
            else if (c == '"') {
                parsingString = true;
            }
            // Append
            else {
                builder.append(c);
            }
            position++;
        }
        if (builder.length() > 0) {
            return builder.toString();
        }
        else {
            return null;
        }
    }

    /**
     * Parse command-line to array of arguments
     *
     * @param commandLine
     * @return array of arguments
     */
    public static String[] parse(String commandLine)
    {
        CommandLineParser parser = new CommandLineParser(commandLine);
        List<String> list = new ArrayList<String>();
        String token = null;
        while ((token = parser.readToken()) != null) {
            list.add(token);
        }
        return list.toArray(new String[list.size()]);
    }
}
