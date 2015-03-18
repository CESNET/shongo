<%@ tag import="cz.cesnet.shongo.TodoImplementException" %>
<%@ tag import="org.joda.time.*" %>
<%@ tag import="cz.cesnet.shongo.util.DateTimeFormatter" %>
<%@ tag import="cz.cesnet.shongo.client.web.models.UserSession" %>
<%@ tag import="org.joda.time.format.PeriodFormat" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%@ attribute name="style" required="false" type="java.lang.String" %>
<%@ attribute name="styleShort" required="false" type="java.lang.Boolean" %>
<%@ attribute name="multiline" required="false" type="java.lang.Boolean" %>
<%@ attribute name="pre" required="false" type="org.joda.time.Period" %>
<%@ attribute name="post" required="false" type="org.joda.time.Period" %>

<%
    if (styleShort == null) {
        styleShort = Boolean.FALSE;
    }
    if (multiline == null) {
        multiline = Boolean.FALSE;
    }
    DateTimeFormatter.Type formatType = DateTimeFormatter.Type.LONG;
    if (styleShort) {
        formatType = DateTimeFormatter.Type.SHORT;
    }
    DateTimeZone timeZone = UserSession.getInstance(request).getTimeZone();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(formatType, response.getLocale(), timeZone);
    if (value instanceof ReadablePartial) {
        value = dateTimeFormatter.formatDate((ReadablePartial) value);
    }
    else if (value instanceof DateTime) {
        if (style != null && style.equals("time")) {
            value = dateTimeFormatter.formatTime((DateTime) value);
        }
        else if (style != null && style.equals("date")) {
            value = dateTimeFormatter.formatDate((DateTime) value);
        }
        else {
            value = dateTimeFormatter.formatDateTime((DateTime) value);
        }
    }
    else if (value instanceof Interval) {
        if (style != null && style.equals("date")) {
            value = dateTimeFormatter.formatIntervalDate((Interval) value);
        }
        else if (multiline) {
            value = dateTimeFormatter.formatIntervalMultiLine((Interval) value, pre, post);
        }
        else {
            value = dateTimeFormatter.formatInterval((Interval) value);
        }
    }
    else if (value instanceof Period) {
        value = dateTimeFormatter.formatDuration((Period) value);
    }
    else if (value instanceof Duration) {
        if (style != null && style.equals("time")) {
            value = dateTimeFormatter.formatDurationTime((Duration) value);
        }
        else {
            value = dateTimeFormatter.formatDuration((Duration) value);
        }
    }
    else if (value != null) {
        throw new TodoImplementException(value.getClass());
    }
    jspContext.setAttribute("value", value);
%>
${value}
