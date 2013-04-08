package cz.cesnet.shongo.report;

import cz.cesnet.report.xml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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

        for (ScopeDeclaration scopeDeclaration : reports.getScopes().getScope()) {
            scopeGenerators.put(scopeDeclaration.getName(), new ScopeGenerator(scopeDeclaration));
        }
        for (Reports.Scope scope : reports.getScope()) {
            ScopeGenerator scopeGenerator = scopeGenerators.get(scope.getName());
            for (Report report : scope.getReport()) {
                scopeGenerator.addReport(new ReportGenerator(report));
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

        private Collection<ReportGenerator> reports = new LinkedList<ReportGenerator>();

        public ScopeGenerator(ScopeDeclaration scopeDeclaration)
        {
            this.scopeDeclaration = scopeDeclaration;
        }

        public void addReport(ReportGenerator reportGenerator)
        {
            reports.add(reportGenerator);
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
                throw new RuntimeException("Class name doesn't contain package.");
            }
            return className.substring(index + 1);
        }

        public String getClassPackage()
        {
            String className = scopeDeclaration.getClassName();
            int index = className.lastIndexOf(".");
            if (index == -1) {
                throw new RuntimeException("Class name doesn't contain package.");
            }
            return className.substring(0, index);
        }

        public Collection<ReportGenerator> getReports()
        {
            return reports;
        }
    }

    public static class ReportGenerator
    {
        private Report report;

        private Collection<ParamGenerator> params = new LinkedList<ParamGenerator>();

        public ReportGenerator(Report report)
        {
            this.report = report;

            if (report.getParams() != null) {
                for (ReportParam reportParam : report.getParams().getParam()) {
                    params.add(new ParamGenerator(reportParam));
                }
            }
        }

        public String getName()
        {
            return report.getId() + "-report";
        }

        public String getClassName()
        {
            return formatCamelCaseFirstUpper(getName());
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
            switch (report.getClassification().getSeverity()) {
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
            return formatJavaDoc(getDescription());
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

        public boolean hasException()
        {
            return report.getClassification().getSeverity().equals(ReportClassificationSeverity.ERROR);
        }

        public String getExceptionClassName()
        {
            return formatCamelCaseFirstUpper(report.getId() + "-exception");
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
                name = "className";
            }
            else if (name.equals("type")) {
                name = "typeName";
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
            switch (reportParam.getType()) {
                case STRING:
                    return "String";
                case JADE_REPORT:
                    return "CommandFailure";
                default:
                    throw new TodoException(reportParam.getType().toString());
            }
        }

        public String getVariableStringValue()
        {
            switch (reportParam.getType()) {
                case STRING:
                    return getVariableName();
                default:
                    return getVariableName() + ".toString()";
            }

        }
    }

    public static void main(String[] args) throws Exception
    {
        CodeGenerator reportGenerator = new CodeGenerator();
        reportGenerator.generate();
    }
}
