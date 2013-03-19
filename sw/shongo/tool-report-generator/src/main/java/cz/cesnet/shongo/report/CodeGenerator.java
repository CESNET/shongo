package cz.cesnet.shongo.report;

import cz.cesnet.report.xml.ApiReport;
import cz.cesnet.report.xml.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CodeGenerator extends AbstractGenerator
{
    private static Logger logger = LoggerFactory.getLogger(CodeGenerator.class);

    @Override
    public void generate() throws Exception
    {
        for (ApiReport apiReport : reports.getApi().getReport()) {
            if ( apiReport.getClassification().getSeverity() != Severity.ERROR) {
                throw new TodoException();
            }
            logger.debug("ApiReport {}", apiReport.getId());
        }
        renderFile("common-api/src/main/java/cz/cesnet/shongo/Faults.java",
                "Faults.java.ftl", new HashMap<String, Object>()
        {{
                put("reports", reports.getApi().getReport());
            }});
    }

    public static void main(String[] args) throws Exception
    {
        CodeGenerator reportGenerator = new CodeGenerator();
        reportGenerator.generate();
    }
}
