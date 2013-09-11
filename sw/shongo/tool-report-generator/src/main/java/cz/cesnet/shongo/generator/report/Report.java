package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.Formatter;
import cz.cesnet.shongo.generator.GeneratorException;
import cz.cesnet.shongo.generator.TodoException;
import cz.cesnet.shongo.generator.xml.ReportFor;
import cz.cesnet.shongo.generator.xml.ReportMessage;

import java.util.*;

/**
 * Represents a report.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Report
{
    /**
     * Scope name.
     */
    private String scopeName;

    /**
     * XML report data.
     */
    cz.cesnet.shongo.generator.xml.Report report;

    /**
     * Base report.
     */
    private Report baseReport;

    /**
     * Base class for report.
     */
    private String baseClassName;

    /**
     * Base class for exception.
     */
    private String exceptionBaseClassName;

    /**
     * Declared parameters defined by this report.
     */
    private List<ParamDeclared> declaredParams = new LinkedList<ParamDeclared>();

    /**
     * Temporary parameters defined by this report.
     */
    private List<ParamTemporary> temporaryParams = new LinkedList<ParamTemporary>();

    /**
     * All parameters by this and all parents reports ()
     */
    private List<Param> params = new LinkedList<Param>();

    /**
     * Param by original name.
     */
    private Map<String, Param> paramByOriginalName = new HashMap<String, Param>();

    /**
     * Declared parameters by this and all parents reports..
     */
    private List<ParamDeclared> allDeclaredParams = new LinkedList<ParamDeclared>();

    /**
     * Param which represents a resource-id.
     */
    private ParamDeclared resourceIdParam;

    /**
     * Api fault code.
     */
    private int apiFaultCode;

    /**
     * Constructor.
     *
     * @param report
     */
    public Report(String scopeName, cz.cesnet.shongo.generator.xml.Report report)
    {
        this.scopeName = scopeName;
        this.report = report;

        if (report.getParams() != null) {
            // Add declared params
            for (cz.cesnet.shongo.generator.xml.ReportParam param : report.getParams().getParam()) {
                ParamDeclared paramDeclared = new ParamDeclared(this, param);

                // Add param
                params.add(paramDeclared);
                paramByOriginalName.put(paramDeclared.getOriginalName(), paramDeclared);

                // Add declared param
                declaredParams.add(paramDeclared);
                allDeclaredParams.add(paramDeclared);

                // Set resource-id param
                if (param.isResourceId() != null && param.isResourceId()) {
                    if (resourceIdParam != null) {
                        throw new GeneratorException(
                                "Cannot set resource-id parameter '%s', because '%s' has already been set.",
                                paramDeclared.getName(), resourceIdParam.getName());
                    }
                    resourceIdParam = paramDeclared;
                }
            }

            // Add temporary params
            for (cz.cesnet.shongo.generator.xml.ReportParamTemporary param : report.getParams().getTemporary()) {
                ParamTemporary paramTemporary = new ParamTemporary(this, param);

                // Add param
                params.add(paramTemporary);
                paramByOriginalName.put(paramTemporary.getOriginalName(), paramTemporary);

                // Add temporary param
                temporaryParams.add(paramTemporary);
            }
        }
    }

    /**
     * @return {@link #report}
     */
    public cz.cesnet.shongo.generator.xml.Report getXml()
    {
        return report;
    }

    /**
     * @param baseReport to be set as base for this report
     */
    public void setBaseReport(Report baseReport)
    {
        // Set base report and base classes for report and exception
        this.baseReport = baseReport;
        this.baseClassName = baseReport.getClassName();
        if (baseReport.hasException()) {
            this.exceptionBaseClassName = baseReport.getExceptionClassName();
        }

        // Add all params from base report
        int paramIndex = 0;
        int declaredParamIndex = 0;
        for (Param param : baseReport.params) {
            params.add(paramIndex++, param);
            paramByOriginalName.put(param.getOriginalName(), param);
            if (param instanceof ParamDeclared) {
                ParamDeclared paramDeclared = (ParamDeclared) param;
                allDeclaredParams.add(declaredParamIndex++, paramDeclared);
            }
        }

        fillMissingFromReport(baseReport.report, false);
    }

    /**
     * @param report      from which should be filled all not-set parameters
     * @param mergeParams specifies wheter params should be merged
     */
    public void fillMissingFromReport(cz.cesnet.shongo.generator.xml.Report report, boolean mergeParams)
    {
        if (this.report.getClassification() == null) {
            this.report.setClassification(report.getClassification());
        }
        if (mergeParams && report.getParams() != null) {
            throw new TodoException("Merge params");
        }
        if (this.report.getDescription() == null) {
            this.report.setDescription(report.getDescription());
        }
        if (this.report.getExample() == null) {
            this.report.setExample(report.getExample());
        }
        if (this.report.getResolution() == null) {
            this.report.setResolution(report.getResolution());
        }
        else if (report.getResolution() != null) {
            cz.cesnet.shongo.generator.xml.ReportResolution resolution = this.report.getResolution();
            if (resolution.getParam() == null) {
                resolution.setParam(report.getResolution().getParam());
            }
            if (resolution.getValue() == null || resolution.getValue().isEmpty()) {
                resolution.setValue(report.getResolution().getValue());
            }
        }
        if (this.report.isPersistent() == null) {
            this.report.setPersistent(report.isPersistent());
        }
        if (this.report.isSerializable() == null) {
            this.report.setSerializable(report.isSerializable());
        }
        if (this.report.getException() == null) {
            this.report.setException(report.getException());
        }
        else if (report.getException() != null) {
            cz.cesnet.shongo.generator.xml.Report.Exception exception = this.report.getException();
            if (exception.getBaseClass() == null) {
                exception.setBaseClass(report.getException().getBaseClass());
            }
            if (exception.isRuntime() == null) {
                exception.setRuntime(report.getException().isRuntime());
            }
        }
        if (this.report.isApiFault() == null) {
            this.report.setApiFault(report.isApiFault());
        }
        if (this.report.getApiFaultCode() == null) {
            this.report.setApiFaultCode(report.getApiFaultCode());
        }

        List<ReportFor> thisReportVisible = this.report.getVisible();
        for (ReportFor reportFor : report.getVisible()) {
            if (!thisReportVisible.contains(reportFor)) {
                thisReportVisible.add(reportFor);
            }
        }
    }

    public void setBaseClassName(String baseClassName)
    {
        this.baseClassName = baseClassName;
    }

    public void setApiFaultCode(int apiFaultCode)
    {
        this.apiFaultCode = apiFaultCode;
    }

    public String getScopeName()
    {
        return scopeName;
    }

    public String getId()
    {
        return report.getId();
    }

    public Report getBaseReport()
    {
        return baseReport;
    }

    public String getName()
    {
        return getId() + "-report";
    }

    public boolean isAbstract()
    {
        Boolean isAbstract = report.isAbstract();
        return (isAbstract != null ? isAbstract : false);
    }

    public String getClassName()
    {
        return Formatter.formatCamelCaseFirstUpper(getName());
    }

    public String getBaseClassName()
    {
        if (baseClassName != null) {
            return baseClassName;
        }
        return "AbstractReport";
    }

    public String getConstantName()
    {
        return Formatter.formatConstant(getName());
    }

    public String getType()
    {
        cz.cesnet.shongo.generator.xml.Report.Classification classification = report.getClassification();
        if (classification == null || classification.getSeverity() == null) {
            throw new GeneratorException("Severity is not filled for report '%s'.", getId());
        }
        switch (classification.getSeverity()) {
            case ERROR:
                return "Report.Type.ERROR";
            case WARN:
                return "Report.Type.WARNING";
            case INFO:
                return "Report.Type.INFORMATION";
            case DEBUG:
                return "Report.Type.DEBUG";
            default:
                throw new TodoException(report.getClassification().getSeverity().toString());
        }
    }

    public String getDefaultMessage()
    {
        Map<ReportFor, Map<String, String>> messages = getMessages();
        if (messages != null) {
            Map<String, String> messagesForType = messages.get(null);
            if (messagesForType != null) {
                return messagesForType.get(null);
            }
        }
        return null;
    }

    /**
     * Messages.
     */
    private Map<ReportFor, Map<String, String>> messages = null;

    private Set<ReportFor> MESSAGE_TYPES = new HashSet<ReportFor>() {{
        add(ReportFor.DOMAIN_ADMIN);
        add(ReportFor.RESOURCE_ADMIN);
        add(ReportFor.USER);
        add(null);
    }};
    private Map<String, String> MESSAGE_LANGUAGES = new HashMap<String, String>() {{
        put("en", "ENGLISH");
        put("cs", "CZECH");
        put(null, null);
    }};

    /**
     * @return map of messages by type and language
     */
    public Map<ReportFor, Map<String, String>> getMessages()
    {
        if (messages == null && !isAbstract()) {
            messages = new LinkedHashMap<ReportFor, Map<String, String>>();

            // For each type
            for (ReportFor type : MESSAGE_TYPES) {
                List<ReportMessage> reportMessages = new LinkedList<ReportMessage>();
                for (ReportMessage reportMessage : report.getMessage()) {
                    if ((type == null && reportMessage.getFor().size() == 0)
                            || (type != null && reportMessage.getFor().contains(type))) {
                        reportMessages.add(reportMessage);
                    }
                }
                if (reportMessages.size() == 0) {
                    continue;
                }

                Map<String, String> languageMessages = new LinkedHashMap<String, String>();
                messages.put(type, languageMessages);

                // For each language
                for (String language : MESSAGE_LANGUAGES.keySet()) {
                    ReportMessage languageReportMessage = null;
                    for (ReportMessage reportMessage : reportMessages) {
                        if ((reportMessage.getLang() == null && language == null)
                                || (reportMessage.getLang() != null && reportMessage.getLang().equals(language))) {
                            languageReportMessage = reportMessage;
                            break;
                        }
                    }
                    if (languageReportMessage == null) {
                        continue;
                    }
                    String message = languageReportMessage.getValue();
                    message = Formatter.formatString(message);
                    languageMessages.put(MESSAGE_LANGUAGES.get(language), message);
                }
                // Default language
                if (!languageMessages.containsKey(null)) {
                    String defaultLanguage = MESSAGE_LANGUAGES.get("en");
                    if (languageMessages.containsKey(defaultLanguage)) {
                        languageMessages.put(null, languageMessages.get(defaultLanguage));
                        languageMessages.remove(defaultLanguage);
                    }
                    else {
                        throw new TodoException("Report " + report.getId() + " no message for default language for " + type + ".");
                    }
                }
            }
            // Default type
            if (!messages.containsKey(null)) {
                ReportFor defaultReportFor = ReportFor.DOMAIN_ADMIN;
                if (messages.containsKey(defaultReportFor)) {
                    messages.put(null, messages.get(defaultReportFor));
                    messages.remove(defaultReportFor);
                }
                else {
                    throw new TodoException("Report " + report.getId() + " has no message for default type.");
                }
            }
        }
        return messages;
    }

    private Collection<String> processMessage(String message)
    {
        if (message == null) {
            return Collections.emptyList();
        }
        message = Formatter.formatString(message);

        return new ParamReplace(message, this){
            @Override
            public String processString(String string)
            {
                return "\"" + string + "\"";
            }

            @Override
            public String processParam(Param param)
            {
                return "(" + param.getValue() + " == null ? \"null\" : " + param.getValueMessage() + ")";
            }

            @Override
            protected Object processParamIfNull(Param param, String defaultValue)
            {
                if (param.getType().isCollection()) {
                    return "((" + param.getValue() + " == null || " + param.getValue() + ".isEmpty()) ? \"" + defaultValue + "\" : " + param.getValueMessage() + ")";
                }
                else {
                    return "(" + param.getValue() + " == null ? \"" + defaultValue + "\" : " + param.getValueMessage() + ")";
                }
            }

            @Override
            protected Object processParamClassName(Param param)
            {
                return "(" + param.getValue() + " == null ? \"null\" : " + param.getValue() + ".getClass().getSimpleName())";
            }

            @Override
            protected Object processParamUser(Param param)
            {
                String value = param.getValue();
                if (param.getTypeClassName().equals("String")) {
                    value = "cz.cesnet.shongo.controller.authorization.Authorization.getInstance().getUserInformation(" + value +")";
                }
                return "(" + param.getValue() + " == null ? \"null\" : cz.cesnet.shongo.PersonInformation.Formatter.format(" + value + "))";
            }
        }.getStringParts();
    }

    public String getJavaDoc()
    {
        String description = getDefaultMessage();
        if (description == null) {
            return null;
        }
        description = Formatter.formatString(description);
        description = description.replace("\\n", "\n     * ");

        description = new ParamReplace(description, this){
            @Override
            public String processParam(Param param)
            {
                return "{@link #" + param.getValue() + "}";
            }

            @Override
            protected Object processParamClassName(Param param)
            {
                return "of class {@link #" + param.getValue() + "}";
            }
        }.getString();

        return description;
    }

    /**
     * @return {@link #declaredParams}
     */
    public Collection<ParamDeclared> getDeclaredParams()
    {
        return declaredParams;
    }

    /**
     * @return {@link #temporaryParams}
     */
    public Collection<ParamTemporary> getTemporaryParams()
    {
        return temporaryParams;
    }

    /**
     * @return {@link #allDeclaredParams}
     */
    public Collection<ParamDeclared> getAllDeclaredParams()
    {
        return allDeclaredParams;
    }

    /**
     * @return {@link #params}
     */
    public Collection<Param> getParams()
    {
        return params;
    }

    /**
     * @param originalName
     * @return {@link Param} with given {@code originalName}
     */
    public Param getParam(String originalName)
    {
        return paramByOriginalName.get(originalName);
    }

    /**
     * @return true if report is API fault
     */
    public boolean isApiFault()
    {
        return  report.isApiFault() != null && report.isApiFault();
    }

    /**
     * @return API fault code
     */
    public int getApiFaultCode()
    {
        return apiFaultCode;
    }

    public boolean isSerializable()
    {
        Boolean isSerializable = report.isSerializable();
        return (isSerializable != null ? isSerializable : false);
    }

    public boolean isPersistent()
    {
        Boolean isPersistent = report.isPersistent();
        return (isPersistent != null ? isPersistent : false);
    }

    public Collection<String> getPersistencePreRemove()
    {
        Collection<String> persistencePreRemove = new LinkedList<String>();
        for (ParamDeclared paramDeclared : declaredParams) {
            persistencePreRemove.addAll(
                    paramDeclared.getType().getPersistencePreRemove(paramDeclared.getVariableName()));
        }
        return persistencePreRemove;
    }

    public boolean hasException()
    {
        cz.cesnet.shongo.generator.xml.Report.Classification classification = report.getClassification();
        if (classification != null && classification.getSeverity() != null) {
            return report.getClassification().getSeverity().equals(
                    cz.cesnet.shongo.generator.xml.ReportClassificationSeverity.ERROR);
        }
        return false;
    }

    public String getExceptionClassName()
    {
        return Formatter.formatCamelCaseFirstUpper(getId() + "-exception");
    }

    public String getExceptionBaseClassName()
    {
        if (exceptionBaseClassName != null) {
            return exceptionBaseClassName;
        }
        if (report.getException() != null && report.getException().getBaseClass() != null) {
            return report.getException().getBaseClass();
        }
        if (report.getException() != null && report.getException().isRuntime()) {
            return "ReportRuntimeException";
        }
        return "ReportException";
    }

    public String getVisibleFlags()
    {
        StringBuilder visibleFlags = new StringBuilder();
        List<ReportFor> visible = report.getVisible();
        if (visible != null) {
            if (visible.contains(ReportFor.USER)) {
                visibleFlags.append("VISIBLE_TO_USER");
            }
            if (visible.contains(ReportFor.DOMAIN_ADMIN)) {
                if (visibleFlags.length() > 0) {
                    visibleFlags.append(" | ");
                }
                visibleFlags.append("VISIBLE_TO_DOMAIN_ADMIN");
            }
            if (visible.contains(ReportFor.RESOURCE_ADMIN)) {
                if (visibleFlags.length() > 0) {
                    visibleFlags.append(" | ");
                }
                visibleFlags.append("VISIBLE_TO_RESOURCE_ADMIN");
            }
        }
        return (visibleFlags.length() > 0 ? visibleFlags.toString() : null);
    }

    public String getResolution()
    {
        cz.cesnet.shongo.generator.xml.ReportResolution reportResolution = report.getResolution();
        if (reportResolution == null) {
            return null;
        }
        String paramName = reportResolution.getParam();
        if (paramName != null) {
            Param param = paramByOriginalName.get(paramName);
            Type paramType = param.getType();
            if (paramType.isReport()) {
                return param.getValue() + ".getResolution()";
            }
            else {
                throw new GeneratorException("Unknown resolution for param '%s' of type '%s'.", param, paramType);
            }
        }
        String resolution = reportResolution.getValue();
        if (resolution == null || resolution.isEmpty()) {
            return null;
        }
        else if (resolution.equals("try-again")) {
            return "Resolution.TRY_AGAIN";
        }
        else if (resolution.equals("stop")) {
            return "Resolution.STOP";
        }
        else {
            throw new GeneratorException("Unknown resolution '%s'.", resolution);
        }
    }

    public ParamDeclared getResourceIdParam()
    {
        return resourceIdParam;
    }

}
