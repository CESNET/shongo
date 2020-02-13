package cz.cesnet.shongo.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.ClassHelper;


import java.io.IOException;

/**
 * Represents a XML-RPC fault string with it's parameters which can be converted to/from string.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ApiFaultString implements ReportSerializer
{
    /**
     * @see com.fasterxml.jackson.databind.ObjectMapper
     */
    private static ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Data.
     */
    private ObjectNode jsonNode;

    /**
     * Constructor.
     */
    public ApiFaultString()
    {
        jsonNode = jsonMapper.createObjectNode();
    }

    /**
     * @return message from {@link ApiFaultString}
     */
    public String getMessage()
    {
        return jsonNode.get("message").asText();
    }

    /**
     * @param message sets the message to {@link ApiFaultString}
     */
    public void setMessage(String message)
    {
        jsonNode.put("message", message);
    }

    /**
     *
     * @param name
     * @param elementTypes
     * @return value of parameter with given {@code name}
     */
    @Override
    public Object getParameter(String name, Class type, Class... elementTypes)
    {
        if (String.class.equals(type)) {
            JsonNode value = jsonNode.get(name);
            if (value == null) {
                return null;
            }
            return value.asText();
        }
        else if (Integer.class.equals(type)) {
            JsonNode value = jsonNode.get(name);
            if (value == null) {
                return null;
            }
            return value.asInt();
        }
        else if (AbstractReport.class.isAssignableFrom(type)) {
            ObjectNode value = (ObjectNode) jsonNode.get(name);
            if (value == null) {
                return null;
            }
            String reportClassName = value.get("class").asText();
            try {
                Class reportClass = ClassHelper.getClassFromShortName(reportClassName);
                @SuppressWarnings("unchecked")
                SerializableReport report = (SerializableReport) ClassHelper.createInstanceFromClass(reportClass);

                ObjectNode mainJsonNode = jsonNode;
                jsonNode = value;
                report.readParameters(this);
                jsonNode = mainJsonNode;

                return report;
            }
            catch (ClassNotFoundException exception) {
                throw new CommonReportSet.ClassUndefinedException(reportClassName);
            }
        }
        else {
            throw new TodoImplementException(type);
        }
    }

    /**
     * @param name  of parameter to be set
     * @param value of parameter to be set
     */
    @Override
    public void setParameter(String name, Object value)
    {
        if (value instanceof String) {
            jsonNode.put(name, (String) value);
        }
        else if (value instanceof Integer) {
            jsonNode.put(name, (Integer) value);
        }
        else if (value instanceof SerializableReport) {
            SerializableReport serializableReport = (SerializableReport) value;

            ObjectNode mainJsonNode = jsonNode;
            jsonNode = jsonMapper.createObjectNode();
            jsonNode.put("class", ClassHelper.getClassShortName(value.getClass()));
            serializableReport.writeParameters(this);
            mainJsonNode.replace(name, jsonNode);
            jsonNode = mainJsonNode;
        }
        else if (value != null) {
            throw new TodoImplementException(value.getClass());
        }
    }

    /**
     * Parse message and parameters from given {@code message} string.
     *
     * @param message
     */
    public void parse(String message)
    {
        try {
            jsonNode = (ObjectNode) jsonMapper.readTree(message);
        }
        catch (IOException exception) {
            throw new RuntimeException("Failed to parse JSON.", exception);
        }
    }

    @Override
    public String toString()
    {
        try {
            return jsonMapper.writeValueAsString(jsonNode);
        }
        catch (IOException exception) {
            throw new RuntimeException("Failed to format JSON.", exception);
        }
    }

    /**
     * @param message
     * @return true if given {@code message} is fault string,
     *         false otherwise
     */
    public static boolean isFaultString(String message)
    {
        return message.startsWith("{") && message.endsWith("}");
    }
}
