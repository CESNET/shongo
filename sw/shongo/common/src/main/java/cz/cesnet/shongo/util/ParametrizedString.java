package cz.cesnet.shongo.util;

import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ParametrizedString
{
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{([^\\$]+)\\}");

    private static final PatternParser PATTERN_PARSER = new PatternParser(PARAM_PATTERN);

    private String content;

    public ParametrizedString(final String content, final Map<String, String> parameters)
    {
        this.content = PATTERN_PARSER.parseAndJoin(content, new PatternParser.Callback()
        {
            @Override
            public String processString(String string)
            {
                return string;
            }

            @Override
            public String processMatch(MatchResult match)
            {
                String name = match.group(1);
                String value = parameters.get(name);
                if (value == null) {
                    throw new IllegalArgumentException("Parameter " + name + " not defined.");
                }
                return value;
            }
        });
    }

    @Override
    public String toString()
    {
        return content;
    }
}

