package cz.cesnet.shongo.generator;

import cz.cesnet.shongo.generator.report.Report;
import cz.cesnet.shongo.generator.xml.ScopeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
        for (cz.cesnet.shongo.generator.xml.ScopeDeclaration scopeDeclaration : reports.getScopes().getScope()) {
            scopeGenerators.put(scopeDeclaration.getName(), new ScopeGenerator(scopeDeclaration));
        }

        // Add reports to scopes
        for (cz.cesnet.shongo.generator.xml.Reports.Scope scope : reports.getScope()) {
            ScopeGenerator scopeGenerator = scopeGenerators.get(scope.getName());
            cz.cesnet.shongo.generator.xml.Report template = scope.getTemplate();
            if (template != null) {
                scopeGenerator.setTemplate(template);
            }
            for (cz.cesnet.shongo.generator.xml.Report report : scope.getReport()) {
                scopeGenerator.addReport(new Report(scope.getName(), report));
            }
        }

        // Derive reports
        for (ScopeGenerator scopeGenerator : scopeGenerators.values()) {
            cz.cesnet.shongo.generator.xml.Report template = scopeGenerator.getTemplate();
            for (Report reportGenerator : scopeGenerator.getReports()) {
                String baseReportId = reportGenerator.getXml().getExtends();
                if (baseReportId == null && template != null) {
                    baseReportId = template.getExtends();
                }
                if (baseReportId != null) {
                    Report baseReportGenerator = scopeGenerator.getReport(baseReportId);
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

        // Assign API codes
        Map<Integer, ApiFaultCodeGenerator> assignedApiFaultCodeByGenerator =
                new HashMap<Integer, ApiFaultCodeGenerator>();
        for (ScopeGenerator scopeGenerator : scopeGenerators.values()) {
            cz.cesnet.shongo.generator.xml.Report template = scopeGenerator.getTemplate();
            for (Report reportGenerator : scopeGenerator.getReports()) {
                if (!reportGenerator.isApiFault()) {
                    continue;
                }
                Integer apiFaultCode = reportGenerator.getXml().getApiFaultCode();
                if (apiFaultCode == null) {
                    apiFaultCode = 0;
                }
                ApiFaultCodeGenerator apiFaultCodeGenerator = assignedApiFaultCodeByGenerator.get(apiFaultCode);
                if (apiFaultCodeGenerator == null) {
                    apiFaultCodeGenerator = new ApiFaultCodeGenerator(apiFaultCode);
                }
                else {
                    apiFaultCode = apiFaultCodeGenerator.generateNext();
                }

                if (assignedApiFaultCodeByGenerator.containsKey(apiFaultCode)) {
                    throw new RuntimeException(String.format("Fault code '%d' already assigned.", apiFaultCode));
                }
                assignedApiFaultCodeByGenerator.put(apiFaultCode, apiFaultCodeGenerator);

                reportGenerator.setApiFaultCode(apiFaultCode);
            }
        }
    }

    @Override
    public void generate() throws Exception
    {
        for (ScopeGenerator scopeGenerator : scopeGenerators.values()) {
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("scope", scopeGenerator);

            renderFile(scopeGenerator.getFileName(), "ReportSet.java.ftl", parameters);
            if (scopeGenerator.getMessagesFileName() != null) {
                renderFile(scopeGenerator.getMessagesFileName(), "ReportSetMessages.java.ftl", parameters);
            }
        }
    }

    private static class ApiFaultCodeGenerator
    {
        private int value;

        public ApiFaultCodeGenerator(int value)
        {
            this.value = value;
        }

        public int generateNext()
        {
            return ++this.value;
        }
    }

    public static class ScopeGenerator
    {
        private cz.cesnet.shongo.generator.xml.ScopeDeclaration scopeDeclaration;

        private cz.cesnet.shongo.generator.xml.Report template;

        private Collection<Report> reports = new LinkedList<Report>();

        private Map<String, Report> reportById = new HashMap<String, Report>();

        public ScopeGenerator(cz.cesnet.shongo.generator.xml.ScopeDeclaration scopeDeclaration)
        {
            this.scopeDeclaration = scopeDeclaration;
        }

        public void setTemplate(cz.cesnet.shongo.generator.xml.Report template)
        {
            this.template = template;
        }

        public cz.cesnet.shongo.generator.xml.Report getTemplate()
        {
            return template;
        }

        public void addReport(Report report)
        {
            reports.add(report);
            reportById.put(report.getXml().getId(), report);
        }

        public String getFileName()
        {
            StringBuilder fileName = new StringBuilder();
            fileName.append("shongo-" + scopeDeclaration.getModule());
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

        public String getMessagesFileName()
        {
            ScopeDeclaration.Messages messages = scopeDeclaration.getMessages();
            if (messages == null || messages.getClassName() == null) {
                return null;
            }
            String module = messages.getModule();
            if (module == null) {
                module = scopeDeclaration.getModule();
            }
            StringBuilder fileName = new StringBuilder();
            fileName.append("shongo-" + module);
            fileName.append("/src/main/java/");
            fileName.append(messages.getClassName().replace(".", "/"));
            fileName.append(".java");
            return fileName.toString();
        }

        public String getMessagesClassName()
        {
            ScopeDeclaration.Messages messages = scopeDeclaration.getMessages();
            if (messages == null || messages.getClassName() == null) {
                return null;
            }
            String className = messages.getClassName();
            int index = className.lastIndexOf(".");
            if (index == -1) {
                throw new GeneratorException("Class name '%s' doesn't contain package.", className);
            }
            return className.substring(index + 1);
        }

        public String getMessagesClassPackage()
        {
            ScopeDeclaration.Messages messages = scopeDeclaration.getMessages();
            if (messages == null || messages.getClassName() == null) {
                return null;
            }
            String className = messages.getClassName();
            int index = className.lastIndexOf(".");
            if (index == -1) {
                throw new GeneratorException("Class name '%s' doesn't contain package.", className);
            }
            return className.substring(0, index);
        }

        public Collection<Report> getReports()
        {
            return reports;
        }

        public Report getReport(String superReportId)
        {
            return reportById.get(superReportId);
        }
    }

    public static void main(String[] args) throws Exception
    {
        CodeGenerator reportGenerator = new CodeGenerator();
        reportGenerator.generate();
    }

}
