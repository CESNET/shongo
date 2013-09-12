package cz.cesnet.shongo.report;

import cz.cesnet.shongo.TodoImplementException;

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

    public synchronized String getMessage(String reportId, Report.UserType userType, Report.Language language, java.util.Map<String, Object> parameters)
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
        return messageFormat.format(parameters);
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
        private static final Pattern FUNCTION_PATTERN = Pattern.compile("^([\\w]+)\\(([\\w.-]+|\"[^\"]*\")(\\s*,\\s*([\\w.-]+|\"[^\"]*\")?)*\\)$");

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
                int paramCount = (functionMatcher.groupCount() - 1) / 2 + 1;
                String[] params = new String[paramCount];
                for (int param = 0; param < paramCount; param ++) {
                    params[param] = functionMatcher.group((param * 2) + 2);
                }
                return new FunctionComponent(functionMatcher.group(1), params);
            }

            throw new TodoImplementException("Illegal expression '" + expression + "'.");
        }

        public String format(Map<String, Object> parameters)
        {
            StringBuilder stringBuilder = new StringBuilder();
            for (Component component : components) {
                stringBuilder.append(component.format(parameters));
            }
            return stringBuilder.toString();
        }

        private static abstract class Component
        {
            public abstract String format(Map<String, Object> parameters);
        }

        private static class StringComponent extends Component
        {
            private String string;

            public StringComponent(String string)
            {
                this.string = string;
            }


            @Override
            public String format(Map<String, Object> parameters)
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
            public String format(Map<String, Object> parameters)
            {
                int index = 0;
                while (index < components.length) {
                    String component = components[index];
                    Object value = parameters.get(component);
                    index++;
                    if (index == components.length) {
                        if (value == null) {
                            return null;
                        }
                        else if (value instanceof Object[]) {
                            StringBuilder collectionBuilder = new StringBuilder();
                            for (Object item : (Object[]) value) {
                                if (collectionBuilder.length() > 0) {
                                    collectionBuilder.append(", ");
                                }
                                collectionBuilder.append(item);
                            }
                            return collectionBuilder.toString();
                        }
                        else if (value instanceof Collection) {
                            StringBuilder collectionBuilder = new StringBuilder();
                            for (Object item : (Collection) value) {
                                if (collectionBuilder.length() > 0) {
                                    collectionBuilder.append(", ");
                                }
                                collectionBuilder.append(item);
                            }
                            return collectionBuilder.toString();
                        }
                        return value.toString();
                    }
                    else {
                        if (value instanceof Map) {
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
                else {
                    throw new TodoImplementException("Function '" + name + "' not implemented.");
                }
                for (String param : params) {
                    if (param.startsWith("\"") && param.endsWith("\"")) {
                        this.params.add(new StringComponent(param.substring(1, param.length() - 1)));
                    }
                    else {
                        this.params.add(new ParamComponent(param));
                    }
                }
            }

            @Override
            public String format(Map<String, Object> parameters)
            {
                switch (type) {
                    case IF_EMPTY:
                        String param1 = params.get(0).format(parameters);
                        String param2 = params.get(1).format(parameters);
                        if (param1 != null && !param1.isEmpty()) {
                            return param1;
                        }
                        else {
                            return param2;
                        }
                    default:
                        throw new TodoImplementException(type);
                }
            }

            private static enum Type
            {
                IF_EMPTY
            }
        }
    }
}
