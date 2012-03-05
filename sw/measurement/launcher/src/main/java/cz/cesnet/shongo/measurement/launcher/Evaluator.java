package cz.cesnet.shongo.measurement.launcher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expression evaluator for test cases
 *
 * @author Martin Srom
 */
public class Evaluator {

    private Map<String, String> variables = new HashMap<String, String>();

    public Evaluator()
    {
    }

    public Evaluator(Evaluator parentEvaluator)
    {
        for ( String name : parentEvaluator.variables.keySet() ) {
            variables.put(name, parentEvaluator.variables.get(name));
        }
    }
    
    public void setVariable(String name, String value)
    {
        variables.put(name, value);
    }
    
    public boolean hasVariable(String name)
    {
        return variables.containsKey(name);
    }
    
    public String getVariable(String name)
    {
        return variables.get(name);
    }
    
    public String evaluate(String expression)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(expression);
        while ( evaluateString(builder) ) {
            continue;
        }
        return builder.toString();
    }

    public boolean evaluateString(StringBuilder builder)
    {
        boolean result = false;
        Pattern regex = Pattern.compile("\\{([^\\}]*)\\}");
        Matcher matcher = regex.matcher(builder.toString());
        StringBuffer buffer = new StringBuffer();
        while ( matcher.find() ) {
            String name = matcher.group(1);
            String value = "{" + name + "}";
            if ( variables.containsKey(name)) {
                value = variables.get(name);
                result = true;
            }
            matcher.appendReplacement(buffer, value);
        }
        matcher.appendTail(buffer);
        builder.delete(0, builder.length());
        builder.append(buffer.toString());
        return result;
    }

}
