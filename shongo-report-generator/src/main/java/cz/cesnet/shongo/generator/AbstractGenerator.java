package cz.cesnet.shongo.generator;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractGenerator
{
    private static Logger logger = LoggerFactory.getLogger(AbstractGenerator.class);

    private static final String FILE_NAME = "shongo-report-generator/reports.xml";
    private static final String SCHEMA = "shongo-report-generator/reports.xsd";

    protected cz.cesnet.shongo.generator.xml.Reports reports;

    protected AbstractGenerator()
    {
        try {
            JAXBContext ctx = JAXBContext.newInstance(cz.cesnet.shongo.generator.xml.Reports.class);
            Unmarshaller um = ctx.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new File(SCHEMA));
            um.setSchema(schema);
            reports = (cz.cesnet.shongo.generator.xml.Reports) um.unmarshal(new File(FILE_NAME));
        }
        catch (JAXBException exception) {
            throw new RuntimeException(exception);
        }
        catch (SAXException exception) {
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
        if (new File(fileName).delete()) {
            logger.debug("Deleting old file {}...", fileName);
        }
        java.io.FileWriter fileWriter = new java.io.FileWriter(fileName);
        try {
            java.io.BufferedWriter out = new java.io.BufferedWriter(fileWriter);
            try {
                out.write(content);
            }
            finally {
                out.close();
            }
        }
        finally {
            fileWriter.close();
        }
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
            configuration.setObjectWrapper(new BeansWrapper());
            configuration.setClassForTemplateLoading(AbstractGenerator.class, "/");
        }
        return configuration;
    }
}
