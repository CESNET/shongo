package cz.cesnet.shongo.report;

import cz.cesnet.report.xml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.MatchResult;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CodeGenerator extends AbstractGenerator
{
    private static Logger logger = LoggerFactory.getLogger(CodeGenerator.class);

    private Map<String, ScopeGenerator> scopeGenerators = new HashMap<String, ScopeGenerator>();

    public CodeGenerator()
    {
        super();

        // Add scopes
        for (ScopeDeclaration scopeDeclaration : reports.getScopes().getScope()) {
            scopeGenerators.put(scopeDeclaration.getName(), new ScopeGenerator(scopeDeclaration));
        }

        // Add reports to scopes
        for (Reports.Scope scope : reports.getScope()) {
            ScopeGenerator scopeGenerator = scopeGenerators.get(scope.getName());
            Report template = scope.getTemplate();
            if (template != null) {
                scopeGenerator.setTemplate(template);
            }
            for (Report report : scope.getReport()) {
                scopeGenerator.addReport(new ReportGenerator(report));
            }
        }

        // Derive reports
        for (ScopeGenerator scopeGenerator : scopeGenerators.values()) {
            Report template = scopeGenerator.getTemplate();
            for (ReportGenerator reportGenerator : scopeGenerator.getReports()) {
                String baseReportId = reportGenerator.report.getExtends();
                if (baseReportId == null && template != null) {
                    baseReportId = template.getExtends();
                }
                if (baseReportId != null) {
                    ReportGenerator baseReportGenerator = scopeGenerator.getReport(baseReportId);
                    if (baseReportGenerator != null) {
                        reportGenerator.setBaseReport(baseReportGenerator);
                    }
                    else {
                        reportGenerator.setBaseClassName(baseReportId);
                    }
                }
                if (template != null) {
                    reportGenerator.fillMissingFromReport(template, true);
                }
            }
        }
    }

    @Override
    public void generate() throws Exception
    {
        // TODO: Assign super reports

        // TODO: API codes

        for (ScopeGenerator scopeGenerator : scopeGenerators.values()) {
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("scope", scopeGenerator);

            renderFile(scopeGenerator.getFileName(), "ReportSet.java.ftl", parameters);
        }

        /*Map<ReportModule, ModuleFaultSet> modules = new HashMap<ReportModule, ModuleFaultSet>();
        modules.put(ReportModule.COMMON, new ModuleFaultSet(
                "common-api/src/main/java", "cz.cesnet.shongo.api", "FaultSet"));
        modules.put(ReportModule.CONTROLLER, new ModuleFaultSet(
                "controller-api/src/main/java", "cz.cesnet.shongo.controller.api", "FaultSet"));
        ReportModule[] modulesOrder = new ReportModule[]{
                ReportModule.COMMON,
                ReportModule.CONTROLLER
        };

        Map<String, Integer> reportCodes = new HashMap<String, Integer>();
        for (ApiReport apiReport : reports.getApi().getReport()) {
            if (apiReport.getParams() == null) {
                apiReport.setParams(new ApiReport.Params());
            }
            if (apiReport.getClassification().getSeverity() != Severity.ERROR) {
                throw new TodoException();
            }
            ReportModule reportModule = apiReport.getModule();
            if (reportModule == null) {
                reportModule = ReportModule.COMMON;
            }
            modules.get(reportModule).reports.add(apiReport);
            reportCodes.put(apiReport.getId(), reportCodes.size());
        }

        for (ReportModule key : modulesOrder) {
            ModuleFaultSet module = modules.get(key);
            String className = module.name;
            String fileName = module.path + "/" + module.packageName.replace(".", "/") + "/" + className + ".java";

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", className);
            parameters.put("package", module.packageName);
            if (key.equals(ReportModule.COMMON)) {
                parameters.put("base_name", "cz.cesnet.shongo.fault.AbstractFaultSet");
            }
            else {
                parameters.put("base_name", "cz.cesnet.shongo.api.FaultSet");
            }
            parameters.put("reports", module.reports);
            parameters.put("reportCodes", reportCodes);

            renderFile(fileName, "ReportSet.java.ftl", parameters);
        }*/
    }

    public static class ScopeGenerator
    {
        private ScopeDeclaration scopeDeclaration;

        private Report template;

        private Collection<ReportGenerator> reports = new LinkedList<ReportGenerator>();

        private Map<String, ReportGenerator> reportById = new HashMap<String, ReportGenerator>();

        public ScopeGenerator(ScopeDeclaration scopeDeclaration)
        {
            this.scopeDeclaration = scopeDeclaration;
        }

        public void setTemplate(Report template)
        {
            this.template = template;
        }

        public Report getTemplate()
        {
            return template;
        }

        public void addReport(ReportGenerator reportGenerator)
        {
            reports.add(reportGenerator);
            reportById.put(reportGenerator.report.getId(), reportGenerator);
        }

        public String getFileName()
        {
            StringBuilder fileName = new StringBuilder();
            fileName.append(scopeDeclaration.getModule());
            fileName.append("/src/main/java/");
            fileName.append(scopeDeclaration.getClassName().replace(".", "/"));
            fileName.append(".java");
            return fileName.toString();
        }

        public String getClassName()
        {
            String className = scopeDeclaration.getClassName();
            int index = className.lastIndexOf(".");
            if (index == -1) {
                throw new GeneratorException("Class name '%s' doesn't contain package.", className);
            }
            return className.substring(index + 1);
        }

        public String getClassPackage()
        {
            String className = scopeDeclaration.getClassName();
            int index = className.lastIndexOf(".");
            if (index == -1) {
                throw new GeneratorException("Class name '%s' doesn't contain package.", className);
            }
            return className.substring(0, index);
        }

        public Collection<ReportGenerator> getReports()
        {
            return reports;
        }

        public ReportGenerator getReport(String superReportId)
        {
            return reportById.get(superReportId);
        }
    }

    public static class ReportGenerator
    {
        private Report report;

        private Collection<ParamGenerator> declaredParams = new LinkedList<ParamGenerator>();

        private List<ParamGenerator> params = new LinkedList<ParamGenerator>();

        private Map<String, ParamGenerator> paramByName = new HashMap<String, ParamGenerator>();

        private ReportGenerator baseReportGenerator;

        private String baseClassName;

        private String exceptionBaseClassName;

        public ReportGenerator(Report report)
        {
            this.report = report;

            if (report.getParams() != null) {
                for (ReportParam reportParam : report.getParams().getParam()) {
                    ParamGenerator paramGenerator = new ParamGenerator(reportParam);
                    declaredParams.add(paramGenerator);
                    params.add(paramGenerator);
                    paramByName.put(paramGenerator.reportParam.getName(), paramGenerator);
                }
            }
        }

        public void setBaseReport(ReportGenerator baseReportGenerator)
        {
            this.baseReportGenerator = baseReportGenerator;
            this.baseClassName = baseReportGenerator.getClassName();
            if (baseReportGenerator.hasException()) {
                this.exceptionBaseClassName = baseReportGenerator.getExceptionClassName();
            }

            int paramIndex = 0;
            for (ParamGenerator paramGenerator : baseReportGenerator.getParams()) {
                params.add(paramIndex++, paramGenerator);
                paramByName.put(paramGenerator.reportParam.getName(), paramGenerator);
            }

            fillMissingFromReport(baseReportGenerator.report, false);
        }

        public void setBaseClassName(String baseClassName)
        {
            this.baseClassName = baseClassName;
        }

        public void fillMissingFromReport(Report report, boolean mergeParams)
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
            if (this.report.isPersistent() == null) {
                this.report.setPersistent(report.isPersistent());
            }
            if (this.report.getException() == null) {
                this.report.setException(report.getException());
            }
            else if (report.getException() != null) {
                Report.Exception exception = this.report.getException();
                if (exception.getBaseClass() == null) {
                    exception.setBaseClass(report.getException().getBaseClass());
                }
                if (exception.isRuntime() == null) {
                    exception.setRuntime(report.getException().isRuntime());
                }
            }

            if (this.report.getUser() == null) {
                this.report.setUser(report.getUser());
            }
            else if (report.getUser() != null){
                throw new TodoException("Merge user");
            }

            if (this.report.getDomainAdmin() == null) {
                this.report.setDomainAdmin(report.getDomainAdmin());
            }
            else if (report.getDomainAdmin() != null){
                throw new TodoException("Merge domain admin");
            }

            if (this.report.getResourceAdmin() == null) {
                this.report.setResourceAdmin(report.getResourceAdmin());
            }
            else if (report.getResourceAdmin() != null){
                throw new TodoException("Merge resource admin");
            }
        }

        public String getId()
        {
            return report.getId();
        }

        public ReportGenerator getBaseReport()
        {
            return baseReportGenerator;
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
            return formatCamelCaseFirstUpper(getName());
        }

        public String getBaseClassName()
        {
            if (baseClassName != null) {
                return baseClassName;
            }
            return "Report";
        }

        public String getConstantName()
        {
            return formatConstant(getName());
        }

        public int getCode()
        {
            return 0;
        }

        public String getType()
        {
            Report.Classification classification = report.getClassification();
            if (classification == null || classification.getSeverity() == null) {
                throw new GeneratorException("Severity is not filled for report '%s'.", getId());
            }
            switch (classification.getSeverity()) {
                case ERROR:
                    return "Report.Type.ERROR";
                case WARN:
                    return "Report.Type.WARN";
                case INFO:
                    return "Report.Type.INFO";
                case DEBUG:
                    return "Report.Type.DEBUG";
                default:
                    throw new TodoException(report.getClassification().getSeverity().toString());
            }
        }

        public String getDescription()
        {
            return formatString(report.getDescription());
        }

        public String getJavaDoc()
        {
            String description = getDescription();
            if (description == null) {
                return null;
            }

            PatternReplace patternReplace = new PatternReplace("\\$\\{([^\\$]+)\\}");
            description = patternReplace.replace(description, new PatternReplace.Callback()
            {
                @Override
                public String callback(MatchResult matchResult)
                {
                    String paramName = matchResult.group(1);
                    ParamGenerator paramGenerator = paramByName.get(paramName);
                    if (paramGenerator == null) {
                        throw new GeneratorException("Param '%s' not found in report '%s'.", paramName, getId());
                    }
                    return "{@link #" + paramGenerator.getVariableName() + "}";
                }
            });

            return description;
        }

        public Collection<ParamGenerator> getDeclaredParams()
        {
            return declaredParams;
        }

        public Collection<ParamGenerator> getParams()
        {
            return params;
        }

        public boolean isApiFault()
        {
            ReportUser reportUser = report.getUser();
            if (reportUser != null) {
                return reportUser.isVisible() && reportUser.getVia().equals(ReportUserVia.UI);
            }
            return false;
        }

        public boolean isPersistent()
        {
            Boolean isPersistent = report.isPersistent();
            return (isPersistent != null ? isPersistent : false);
        }

        public boolean hasException()
        {
            Report.Classification classification = report.getClassification();
            if (classification != null && classification.getSeverity() != null) {
                return report.getClassification().getSeverity().equals(ReportClassificationSeverity.ERROR);
            }
            return false;
        }

        public String getExceptionClassName()
        {
            return formatCamelCaseFirstUpper(getId() + "-exception");
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
    }

    public static class ParamGenerator
    {
        private ReportParam reportParam;

        public ParamGenerator(ReportParam reportParam)
        {
            this.reportParam = reportParam;
        }

        public String getName()
        {
            String name = reportParam.getName();
            if (name.equals("class")) {
                name = "class-name";
            }
            else if (name.equals("type")) {
                name = "type-name";
            }
            return name;
        }

        public String getVariableName()
        {
            return formatCamelCaseFirstLower(getName());
        }

        public String getMethodName()
        {
            return formatCamelCaseFirstUpper(getName());
        }

        public String getVariableType()
        {
            return reportParam.getType();
        }

        public String getVariableStringValue()
        {
            if (reportParam.getType().equals("String")) {
                return getVariableName();
            }
            else {
                return getVariableName() + ".toString()";
            }
        }

        private static final Map<String, String> persistenceAnnotationByType = new HashMap<String, String>(){{
            put("String", "@javax.persistence.Column");
            put("cz.cesnet.shongo.JadeReport", "@javax.persistence.OneToOne(orphanRemoval = true)");
        }};

        public String getPersistenceAnnotation()
        {
            String variableType = getVariableType();
            String persistenceAnnotation = persistenceAnnotationByType.get(variableType);
            if (persistenceAnnotation == null) {
                throw new GeneratorException("Unknown persistence mapping of type '%s'.", variableType);
            }
            return persistenceAnnotation;
        }
    }

    public static void main(String[] args) throws Exception
    {
        CodeGenerator reportGenerator = new CodeGenerator();
        reportGenerator.generate();
    }

}
