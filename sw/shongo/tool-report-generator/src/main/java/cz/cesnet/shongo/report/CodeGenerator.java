package cz.cesnet.shongo.report;

import cz.cesnet.report.xml.ApiReport;
import cz.cesnet.report.xml.ReportModule;
import cz.cesnet.report.xml.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CodeGenerator extends AbstractGenerator
{
    private static Logger logger = LoggerFactory.getLogger(CodeGenerator.class);

    private static class ModuleFaultSet
    {
        private String path;
        private String packageName;
        private String name;
        private int code = 0;
        List<ApiReport> reports = new LinkedList<ApiReport>();

        public ModuleFaultSet(String path, String packageName, String name)
        {
            this.path = path;
            this.packageName = packageName;
            this.name = name;
        }
    }

    @Override
    public void generate() throws Exception
    {
        Map<ReportModule, ModuleFaultSet> modules = new HashMap<ReportModule, ModuleFaultSet>();
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

            renderFile(fileName, "FaultSet.java.ftl", parameters);
        }
    }

    public static void main(String[] args) throws Exception
    {
        CodeGenerator reportGenerator = new CodeGenerator();
        reportGenerator.generate();
    }
}
