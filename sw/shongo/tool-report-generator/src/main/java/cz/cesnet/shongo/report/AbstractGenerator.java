package cz.cesnet.shongo.report;

import cz.cesnet.report.xml.ReportParam;
import cz.cesnet.report.xml.ReportParamType;
import cz.cesnet.report.xml.Reports;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractGenerator
{
    private static Logger logger = LoggerFactory.getLogger(AbstractGenerator.class);

    private static final String FILE_NAME = "doc/reports.xml";

    protected Reports reports;

    protected AbstractGenerator()
    {
        try {
            JAXBContext ctx = JAXBContext.newInstance(Reports.class);
            Unmarshaller um = ctx.createUnmarshaller();
            reports = (Reports) um.unmarshal(new File(FILE_NAME));
        }
        catch (JAXBException exception) {
            throw new RuntimeException(exception);
        }
    }

    public abstract void generate() throws Exception;

    /**
     * Render given {@code templateFileName} with specified {@code parameters}.
     *
     * @param targetFileName   whether the result should be written
     * @param templateFileName to be rendered
     * @param parameters       to be rendered
     */
    public void renderFile(String targetFileName, String templateFileName, Map<String, Object> parameters)
            throws Exception
    {
        Template template = getConfiguration().getTemplate("file/" + templateFileName);
        StringWriter stringWriter = new StringWriter();
        parameters.put("this", this);
        template.process(parameters, stringWriter);
        writeFile(targetFileName, stringWriter.toString());
        logger.debug("Writing '{}'...\n{}", targetFileName, stringWriter);
    }

    /**
     * Write file content.
     *
     * @param fileName
     * @param content
     * @throws IOException
     */
    public void writeFile(String fileName, String content) throws IOException
    {
        new File(fileName).delete();
        java.io.FileWriter fileWriter = new java.io.FileWriter(fileName);
        java.io.BufferedWriter out = new java.io.BufferedWriter(fileWriter);
        out.write(content);
        out.close();
    }

    public String formatConstant(String text)
    {
        text = text.replaceAll("[ -]", "_");
        return text.toUpperCase();
    }

    public String formatIdentifier(String text)
    {
        if (text.equals("class")) {
            text = "className";
        }
        String[] parts = text.split("[ -]");
        StringBuilder camelCase = new StringBuilder();
        for (String part : parts) {
            if (camelCase.length() > 0 ) {
                camelCase.append(formatFirstUpperCase(part));
            }
            else {
                camelCase.append(formatFirstLowerCase(part));
            }
        }
        return camelCase.toString();
    }

    public String formatCamelCase(String text)
    {
        if (text.equals("class")) {
            text = "className";
        }
        String[] parts = text.split("[ -]");
        StringBuilder camelCase = new StringBuilder();
        for (String part : parts) {
            camelCase.append(formatFirstUpperCase(part));
        }
        return camelCase.toString();
    }

    public String formatFirstUpperCase(String text)
    {
        StringBuilder camelCase = new StringBuilder();
        camelCase.append(text.substring(0, 1).toUpperCase());
        camelCase.append(text.substring(1));
        return camelCase.toString();
    }

    public String formatFirstLowerCase(String text)
    {
        StringBuilder camelCase = new StringBuilder();
        camelCase.append(text.substring(0, 1).toLowerCase());
        camelCase.append(text.substring(1));
        return camelCase.toString();
    }

    public String formatParamType(ReportParamType type)
    {
        switch (type) {
            case STRING:
                return "String";
            case JADE_REPORT:
                return "CommandFailure";
            default:
                throw new TodoException(type.toString());
        }
    }

    public String formatParamToString(ReportParam param)
    {
        String paramIdentifier = formatIdentifier(param.getName());
        switch (param.getType()) {
            case STRING:
                return paramIdentifier;
            case JADE_REPORT:
                return paramIdentifier + ".toString()";
            default:
                throw new TodoException(param.getType().toString());
        }
    }

    public String formatString(String description)
    {
        description = description.trim();
        description = description.replaceAll("\\s+", " ");
        return description;
    }

    public String formatDescriptionAsJavaDoc(String description)
    {
        description = description.trim();
        description = description.replaceAll("\\s+", " ");
        description = description.replaceAll("\\{(.+)\\}", "{@link #$1}");
        description = description.replace("{@link #class}", "{@link #className}");
        return description;
    }

    /**
     * Single instance of {@link Configuration}.
     */
    private static Configuration configuration;

    /**
     * @return {@link #configuration}
     */
    private static Configuration getConfiguration()
    {
        if (configuration == null) {
            configuration = new Configuration();
            configuration.setObjectWrapper(new DefaultObjectWrapper());
            configuration.setClassForTemplateLoading(AbstractGenerator.class, "/");
        }
        return configuration;
    }
}
