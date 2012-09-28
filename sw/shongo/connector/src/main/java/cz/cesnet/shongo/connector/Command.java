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
    private Map<String, Object> parameters = new HashMap<String, Object>();

    public Command(String command)
    {
        this.command = command;
    }

    public void setParameter(String name, Object value)
    {
        parameters.put(name, value);
    }

    public Object getParameterValue(String name)
    {
        return parameters.get(name);
    }

    public String getCommand()
    {
        return command;
    }

    public Map<String, Object> getParameters()
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
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            sb.append(' ');
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Command command1 = (Command) o;

        if (command != null ? !command.equals(command1.command) : command1.command != null) {
            return false;
        }
        if (parameters != null ? !parameters.equals(command1.parameters) : command1.parameters != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = command != null ? command.hashCode() : 0;
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }


}
