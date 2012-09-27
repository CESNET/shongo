package cz.cesnet.shongo.connector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Command for a device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Command
{
    private String command;
    private Map<String, String> parameters = new HashMap<String, String>();

    public Command(String command)
    {
        this.command = command;
    }

    public void setParameter(String name, String value)
    {
        parameters.put(name, value);
    }

    public String getParameterValue(String name)
    {
        return parameters.get(name);
    }

    public String getCommand()
    {
        return command;
    }

    public Map<String, String> getParameters()
    {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public String toString()
    {
        if (parameters.isEmpty()) {
            return command; // just a tiny optimization
        }

        StringBuilder sb = new StringBuilder(command);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            sb.append(' ');
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }
}
