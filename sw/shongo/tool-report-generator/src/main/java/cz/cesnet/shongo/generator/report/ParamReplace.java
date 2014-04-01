package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.GeneratorException;
import cz.cesnet.shongo.util.PatternParser;
import org.apache.commons.jexl2.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ParamReplace extends PatternParser implements PatternParser.Callback
{
    private static final Pattern MESSAGE_PARAM_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

    private static JexlEngine jexlEngine = new JexlEngine();

    private Context context;

    private Collection<String> stringParts = new LinkedList<String>();

    private final Report report;

    public ParamReplace(String text, final Report report, Context context)
    {
        super(MESSAGE_PARAM_PATTERN);
        this.report = report;
        this.context = context;
        this.context.paramReplace = this;
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
        String code = match.group(1);
        Expression expression = jexlEngine.createExpression(code);
        Object result = expression.evaluate(context);
        if (result == null) {
            throw new GeneratorException("Expression can't be evaluated '" + match.group(1) + "'.");
        }
        else if (result instanceof Param) {
            return context.processParam((Param) result);
        }
        else if (result instanceof String) {
            return (String) result;
        }
        else {
            throw new GeneratorException("Unknown expression result type '%s'.", result.getClass().getName());
        }
    }

    public static abstract class Context implements NamespaceResolver, JexlContext
    {
        private ParamReplace paramReplace;

        @Override
        public Object resolveNamespace(String name)
        {
            if (name == null) {
                return this;
            }
            else {
                return null;
            }
        }

        @Override
        public Object get(String variableName)
        {
            Param param = paramReplace.report.getParam(variableName.split("\\.")[0]);
            if (param == null) {
                throw new GeneratorException("Param '%s' not found in report '%s'.",
                        variableName, paramReplace.report.getId());
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
            return paramReplace.report.getParam(variableName) != null;
        }

        /**
         * @param param to be replaced
         * @return string which should be used for given param
         */
        public abstract String processParam(Param param);

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
    }

}
