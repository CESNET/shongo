package cz.cesnet.shongo.connector.common;

import java.util.*;

/**
 * Command for a device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Command
{
    private String command;

    /**
     * List of arguments to the command.
     * Arguments get no name, just their value and order is important.
     */
    private List<Object> arguments = new LinkedList<Object>();

    /**
     * Map of parameters to the command.
     * Parameters have a name and a value. The order of parameters is irrelevant.
     */
    private Map<String, Object> parameters = new HashMap<String, Object>();

    /**
     * Separator of parameter names and values.
     */
    private final String paramNameValueSeparator;


    public Command(String command)
    {
        this(command, ": ");
    }

    public Command(String command, String paramNameValueSeparator)
    {
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (paramNameValueSeparator == null) {
            throw new NullPointerException("paramNameValueSeparator");
        }

        this.command = command;
        this.paramNameValueSeparator = paramNameValueSeparator;
    }

    public Command addArgument(Object arg)
    {
        arguments.add(arg);
        return this;
    }

    public Command setParameter(String name, Object value)
    {
        parameters.put(name, value);
        return this;
    }

    public Command unsetParameter(String name)
    {
        parameters.remove(name);
        return this;
    }

    public Object getParameterValue(String name)
    {
        return parameters.get(name);
    }

    public String getCommand()
    {
        return command;
    }

    public List<Object> getArguments()
    {
        return Collections.unmodifiableList(arguments);
    }

    public Map<String, Object> getParameters()
    {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(command);

        for (Object arg : arguments) {
            sb.append(" ");
            sb.append(arg);
        }

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            sb.append(" ");
            sb.append(entry.getKey());
            sb.append(paramNameValueSeparator);
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

        if (!command.equals(command1.command)) {
            return false;
        }
        if (!arguments.equals(command1.arguments)) {
            return false;
        }
        if (!parameters.equals(command1.parameters)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = command.hashCode();
        result = 31 * result + arguments.hashCode();
        result = 31 * result + parameters.hashCode();
        return result;
    }


}
