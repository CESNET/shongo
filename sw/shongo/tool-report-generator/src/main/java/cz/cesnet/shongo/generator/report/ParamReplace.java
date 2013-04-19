package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.GeneratorException;
import cz.cesnet.shongo.generator.PatternReplace;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
abstract class ParamReplace extends PatternReplace implements PatternReplace.Callback
{
    private Report report;

    public ParamReplace(Report report)
    {
        super("\\$\\{([^\\$]+)\\}");
        this.report = report;
    }

    public final String replace(String string)
    {
        return super.replace(string, this);
    }

    private static final Pattern FORMAT_PATTERN = Pattern.compile("(.+)\\((.+)\\)");

    @Override
    public final String callback(MatchResult matchResult)
    {
        String paramName = matchResult.group(1);

        String format = null;
        Matcher classMatcher = FORMAT_PATTERN.matcher(paramName);
        if (classMatcher.matches()) {
            format = classMatcher.group(1);
            paramName = classMatcher.group(2);
        }

        Param param = report.getParam(paramName);
        if (param == null) {
            throw new GeneratorException("Param '%s' not found in report '%s'.", paramName, report.getId());
        }
        return getReplace(param);
    }

    public abstract String getReplace(Param param);
}
