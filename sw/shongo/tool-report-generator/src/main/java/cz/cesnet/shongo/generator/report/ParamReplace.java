package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.GeneratorException;
import cz.cesnet.shongo.generator.PatternParser;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.NamespaceResolver;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
abstract class ParamReplace extends PatternParser implements PatternParser.Callback
{
    private static final Pattern MESSAGE_PARAM_PATTERN = Pattern.compile("\\$\\{([^\\$]+)\\}");

    private static JexlEngine jexlEngine = new JexlEngine();

    private JexlContext jexlContext = new Context();

    private Collection<String> stringParts = new LinkedList<String>();

    private final Report report;

    public ParamReplace(String text, final Report report)
    {
        super(MESSAGE_PARAM_PATTERN);
        this.report = report;
        this.stringParts = parse(text, this);
    }

    public Collection<String> getStringParts()
    {
        return stringParts;
    }

    public String getString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (String textPart : stringParts) {
            stringBuilder.append(textPart);
        }
        return stringBuilder.toString();
    }

    @Override
    public String processString(String string)
    {
        return string;
    }

    @Override
    public final String processMatch(MatchResult match)
    {
        Expression expression = jexlEngine.createExpression(match.group(1));
        Object result = expression.evaluate(jexlContext);
        if (result instanceof Param) {
            return processParam((Param) result);
        }
        else if (result instanceof String) {
            return (String) result;
        }
        else {
            throw new GeneratorException("Unknown expression result type '%s'.", result.getClass().getName());
        }
    }

    /**
     * @param param to be replaced
     * @return string which should be used for given param
     */
    protected abstract String processParam(Param param);

    /**
     * @param param
     * @param defaultValue
     * @return result for the expression
     */
    protected Object processParamIfEmpty(Param param, String defaultValue)
    {
        return param;
    }

    /**
     * @param param
     * @return result for the expression
     */
    protected Object processParamClassName(Param param)
    {
        return param;
    }

    /**
     * @param param
     * @return result for the expression
     */
    protected Object processParamUser(Param param)
    {
        return param;
    }

    public class Context implements NamespaceResolver, JexlContext
    {
        @Override
        public Object resolveNamespace(String name)
        {
            if (name == null) {
                return this;
            } else {
                return null;
            }
        }

        @Override
        public Object get(String variableName)
        {
            Param param = report.getParam(variableName);
            if (param == null) {
                throw new GeneratorException("Param '%s' not found in report '%s'.", variableName, report.getId());
            }
            return param;
        }

        @Override
        public void set(String variableName, Object value)
        {
            throw new UnsupportedOperationException(variableName);
        }

        @Override
        public boolean has(String variableName)
        {
            return report.getParam(variableName) != null;
        }

        public Object ifEmpty(Param param, String defaultValue)
        {
            return processParamIfEmpty(param, defaultValue);
        }

        public Object className(Param param)
        {
            return processParamClassName(param);
        }

        public Object user(Param param)
        {
            return processParamUser(param);
        }
    }

}
