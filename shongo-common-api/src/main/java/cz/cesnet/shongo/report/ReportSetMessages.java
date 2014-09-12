package cz.cesnet.shongo.report;

import cz.cesnet.shongo.JadeReport;
import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.AbstractObjectReport;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a set/group of {@link cz.cesnet.shongo.report.AbstractReport}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReportSetMessages
{
    private Map<String, Map<Report.UserType, Map<Report.Language, String>>> messages =
            new HashMap<String, Map<Report.UserType, Map<Report.Language, String>>>();

    private Map<String, MessageFormat> messageFormatById = new HashMap<String, MessageFormat>();

    public synchronized void addMessage(String reportId, Report.UserType[] userTypes, Report.Language language, String message)
    {
        Map<Report.UserType, Map<Report.Language, String>> reportMessages = messages.get(reportId);
        if (reportMessages == null) {
            reportMessages = new HashMap<Report.UserType, Map<Report.Language, String>>();
            messages.put(reportId, reportMessages);
        }

        if (userTypes.length == 0) {
            addMessage(reportMessages, null, language, message);
        }
        else {
            for (Report.UserType userType : userTypes) {
                addMessage(reportMessages, userType, language, message);
            }
        }
    }

    private void addMessage(Map<Report.UserType, Map<Report.Language, String>> reportMessages,
            Report.UserType userType, Report.Language language, String message)
    {
        Map<Report.Language, String> userTypeMessages = reportMessages.get(userType);
        if (userTypeMessages == null) {
            userTypeMessages = new HashMap<Report.Language, String>();
            reportMessages.put(userType, userTypeMessages);
        }
        userTypeMessages.put(language, message);
    }

    public synchronized String getMessage(String reportId, Report.UserType userType, Report.Language language,
            DateTimeZone timeZone, java.util.Map<String, Object> parameters)
    {
        StringBuilder messageIdBuilder = new StringBuilder();
        messageIdBuilder.append(reportId);
        messageIdBuilder.append(":");
        messageIdBuilder.append(userType);
        messageIdBuilder.append(":");
        messageIdBuilder.append(language);
        String messageId = messageIdBuilder.toString();
        MessageFormat messageFormat = messageFormatById.get(messageId);
        if (messageFormat == null) {
            messageFormat = getMessageFormat(reportId, userType, language);
            messageFormatById.put(messageId, messageFormat);
        }
        return messageFormat.format(userType, language, timeZone, parameters);
    }

    private MessageFormat getMessageFormat(String reportId, Report.UserType userType, Report.Language language)
    {
        Map<Report.UserType, Map<Report.Language, String>> reportMessages = messages.get(reportId);
        if (reportMessages == null) {
            throw new IllegalArgumentException("Message for report '" + reportId + "' doesn't exists.");
        }
        Map<Report.Language, String> userTypeMessages = reportMessages.get(userType);
        if (userTypeMessages == null) {
            userTypeMessages = reportMessages.get(null);
            if (userTypeMessages == null) {
                userTypeMessages = reportMessages.get(Report.UserType.DOMAIN_ADMIN);
                if (userTypeMessages == null) {
                    throw new IllegalArgumentException("Message for report '" + reportId +
                            "' and type '" + userType + "' doesn't exists.");
                }
            }
        }
        String message = userTypeMessages.get(language);
        if (message == null) {
            message = userTypeMessages.get(null);
            if (message == null) {
                message = userTypeMessages.get(Report.Language.ENGLISH);
                if (message == null) {
                    throw new IllegalArgumentException("Message for report '" + reportId +
                            "' and type '" + userType + "' and language '" + language + "' doesn't exists.");
                }
            }
        }
        return new MessageFormat(message);
    }

    private static class MessageFormat
    {
        private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
        private static final Pattern PARAM_PATTERN = Pattern.compile("^[\\w.-]+$");
        private static final Pattern FUNCTION_PATTERN = Pattern.compile("^([\\w]+)\\(([\\w.-]+|\"[^\"]*\")?(\\s*,\\s*([\\w.-]+|\"[^\"]*\"))?(\\s*,\\s*([\\w.-]+|\"[^\"]*\"))?(\\s*,\\s*([\\w.-]+|\"[^\"]*\"))?(\\s*,\\s*([\\w.-]+|\"[^\"]*\"))?\\)$");

        private List<Component> components = new LinkedList<Component>();

        public MessageFormat(String message)
        {
            Matcher matcher = EXPRESSION_PATTERN.matcher(message);
            int start = 0;
            while (matcher.find()) {
                if (matcher.start() != start) {
                    components.add(new StringComponent(message.substring(start, matcher.start())));
                }
                components.add(getComponentForExpression(matcher.group(1)));
                start = matcher.end();
            }
            if (message.length() > start) {
                components.add(new StringComponent(message.substring(start, message.length())));
            }
        }

        private Component getComponentForExpression(String expression)
        {
            Matcher paramMatcher = PARAM_PATTERN.matcher(expression);
            if (paramMatcher.matches()) {
                return new ParamComponent(expression);
            }

            Matcher functionMatcher = FUNCTION_PATTERN.matcher(expression);
            if (functionMatcher.matches()) {
                int groupCount = 0;
                for (int groupIndex = 0; groupIndex <= functionMatcher.groupCount(); groupIndex++) {
                    if ( functionMatcher.group(groupIndex) == null) {
                        break;
                    }
                    groupCount++;
                }
                int paramCount = (groupCount - 1) / 2 + 1;
                String[] params = new String[paramCount];
                for (int param = 0; param < paramCount; param ++) {
                    params[param] = functionMatcher.group((param * 2) + 2);
                }
                return new FunctionComponent(functionMatcher.group(1), params);
            }

            throw new TodoImplementException("Illegal expression '" + expression + "'.");
        }

        public String format(Report.UserType userType, Report.Language language, DateTimeZone timeZone, Map<String, Object> parameters)
        {
            StringBuilder stringBuilder = new StringBuilder();
            for (Component component : components) {
                stringBuilder.append(component.format(userType, language, timeZone, parameters));
            }
            return stringBuilder.toString();
        }

        private static abstract class Component
        {
            private static final DateTimeFormatter DATE_TIME_FORMATTER =
                    DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG);

            public abstract Object getValue(Map<String, Object> parameters);

            public String format(Report.UserType userType, Report.Language language, DateTimeZone timeZone,
                    Map<String, Object> parameters)
            {
                Object value = getValue(parameters);
                return formatValue(value, language, timeZone, false);
            }

            protected static String formatValue(Object value, Report.Language language, DateTimeZone timeZone, boolean nested)
            {
                if (value == null) {
                    return null;
                }
                else if (value instanceof String) {
                    return (String) value;
                }
                else if (value instanceof DateTime) {
                    DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(language.toLocale(), timeZone);
                    return dateTimeFormatter.formatDateTime((DateTime) value);
                }
                else if (value instanceof Interval) {
                    DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(language.toLocale(), timeZone);
                    return dateTimeFormatter.formatInterval((Interval) value);
                }
                else if (value instanceof Object[]) {
                    StringBuilder collectionBuilder = new StringBuilder();
                    for (Object item : (Object[]) value) {
                        if (collectionBuilder.length() > 0) {
                            collectionBuilder.append(", ");
                        }
                        collectionBuilder.append(formatValue(item, language, timeZone, true));
                    }
                    if (nested) {
                        collectionBuilder.insert(0, "[");
                        collectionBuilder.append("]");
                    }
                    return collectionBuilder.toString();
                }
                else if (value instanceof Collection) {
                    StringBuilder collectionBuilder = new StringBuilder();
                    for (Object item : (Collection) value) {
                        if (collectionBuilder.length() > 0) {
                            collectionBuilder.append(", ");
                        }
                        collectionBuilder.append(formatValue(item, language, timeZone, true));
                    }
                    if (nested) {
                        collectionBuilder.insert(0, "[");
                        collectionBuilder.append("]");
                    }
                    return collectionBuilder.toString();
                }
                return value.toString();
            }
        }

        private static class StringComponent extends Component
        {
            private String string;

            public StringComponent(String string)
            {
                this.string = string;
            }

            @Override
            public Object getValue(Map<String, Object> parameters)
            {
                return string;
            }
        }

        private static class ParamComponent extends Component
        {
            private String[] components;

            public ParamComponent(String param)
            {
                components = param.split("\\.");
            }

            @Override
            public Object getValue(Map<String, Object> parameters)
            {
                int index = 0;
                while (index < components.length) {
                    String component = components[index];
                    Object value = parameters != null ? parameters.get(component) : null;
                    index++;
                    if (index == components.length) {
                        return value;
                    }
                    else {
                        if (value == null) {
                            return null;
                        }
                        else if (value instanceof Map) {
                            parameters = (Map) value;
                        }
                        else {
                            throw new IllegalArgumentException("Param '" + component + "' is not Map but " + value + ".");
                        }
                    }
                }
                throw new IllegalStateException("Never should get here.");
            }
        }

        private static class FunctionComponent extends Component
        {
            private Type type;
            private List<Component> params = new LinkedList<Component>();

            public FunctionComponent(String name, String... params)
            {
                if (name.equals("ifEmpty")) {
                    type = Type.IF_EMPTY;
                }
                else if (name.equals("format")) {
                    type = Type.FORMAT;
                }
                else if (name.equals("jadeReportMessage")) {
                    type = Type.JADE_REPORT;
                }
                else {
                    throw new TodoImplementException("Function '" + name + "' not implemented.");
                }
                for (String param : params) {
                    if (param == null) {
                        continue;
                    }
                    if (param.startsWith("\"") && param.endsWith("\"")) {
                        this.params.add(new StringComponent(param.substring(1, param.length() - 1)));
                    }
                    else {
                        this.params.add(new ParamComponent(param));
                    }
                }
            }

            @Override
            public Object getValue(Map<String, Object> parameters)
            {
                switch (type) {
                    case IF_EMPTY:
                    case FORMAT:
                    case JADE_REPORT:
                        return params.get(0).getValue(parameters);
                    default:
                        throw new TodoImplementException(type);
                }
            }

            @Override
            public String format(Report.UserType userType, Report.Language language, DateTimeZone timeZone,
                    Map<String, Object> parameters)
            {
                switch (type) {
                    case IF_EMPTY:
                    {
                        String param1 = params.get(0).format(userType, language, timeZone, parameters);
                        String param2 = params.get(1).format(userType, language, timeZone, parameters);
                        if (param1 != null && !param1.isEmpty()) {
                            return param1;
                        }
                        else {
                            return param2;
                        }
                    }
                    case FORMAT:
                    {
                        Object value = params.get(0).getValue(parameters);
                        String format = params.get(1).format(userType, language, timeZone, null);
                        String separator = params.get(2).format(userType, language, timeZone, null);
                        StringBuilder output = new StringBuilder();
                        if (value instanceof Object[]) {
                            for (Object item : (Object[]) value) {
                                if (output.length() > 0) {
                                    output.append(separator);
                                }
                                String itemValue = formatValue(item, language, timeZone, true);
                                output.append(format.replace("$value", itemValue));
                            }
                        }
                        else if (value instanceof Collection) {
                            for (Object item : (Collection) value) {
                                if (output.length() > 0) {
                                    output.append(separator);
                                }
                                String itemValue = formatValue(item, language, timeZone, true);
                                output.append(format.replace("$value", itemValue));
                            }
                        }
                        else if (value instanceof Map) {
                            Map map = (Map) value;
                            for (Object key : new TreeSet(map.keySet())) {
                                if (output.length() > 0) {
                                    output.append(separator);
                                }
                                String itemKey = formatValue(key, language, timeZone, true);
                                String itemValue = formatValue(map.get(key), language, timeZone, true);
                                output.append(format.replace("$key", itemKey).replace("$value", itemValue));
                            }
                        }
                        else {
                            output.append(formatValue(value, language, timeZone, true));
                        }
                        return output.toString();
                    }
                    case JADE_REPORT:
                    {
                        Object param1 = params.get(0).getValue(parameters);
                        if (param1 instanceof JadeReport) {
                            JadeReport jadeReport = (JadeReport) param1;
                            return jadeReport.getMessage(userType, language);
                        }
                        else if (param1 instanceof Map){
                            @SuppressWarnings("unchecked")
                            Map<String, Object> jadeReport = (Map<String, Object>) param1;
                            String jadeReportId = (String) jadeReport.get(AbstractObjectReport.ID);
                            return JadeReportSet.getMessage(jadeReportId, userType, language, timeZone, jadeReport);
                        }
                        else {
                            throw new TodoImplementException(param1.getClass());
                        }
                    }
                    default:
                        throw new TodoImplementException(type);
                }
            }

            private static enum Type
            {
                IF_EMPTY,
                FORMAT,
                JADE_REPORT
            }
        }
    }
}
