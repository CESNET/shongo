package cz.cesnet.shongo.measurement.common;

import java.util.ArrayList;
import java.util.List;

public class CommandParser
{
    private String command;
    
    private int position;

    CommandParser(String command)
    {
        this.command = command;
        this.position = 0;
    }

    public String readToken()
    {
        StringBuilder builder = new StringBuilder();

        boolean parsingString = false;
        while ( position < command.length() ) {
            char c = command.charAt(position);

            // String
            if ( parsingString ) {
                if ( c == '"' ) {
                    break;
                }
                else if ( c == '\\' && command.charAt(position + 1) == '"' ) {
                    builder.append('"');
                    position++;
                }
                else {
                    builder.append(c);
                }
            }
            // Skip whitespaces
            else if ( c == ' ' || c == '\t' ) {
                position++;
                if ( builder.length() > 0 )
                    break;
                else
                    continue;
            }
            // String
            else if ( c == '"' ) {
                parsingString = true;
            }
            // Append
            else {
                builder.append(c);
            }
            position++;
        }
        if ( builder.length() > 0 )
            return builder.toString();
        else
            return null;
    }

    public static List<String> parse(String command)
    {
        CommandParser parser = new CommandParser(command);
        List<String> list = new ArrayList<String>();
        String token = null;
        while ( (token = parser.readToken()) != null ) {
            list.add(token);
        }
        return list;
    }
}
