package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;

import java.util.List;

/**
 * Capability tells that the resource can allocate unique values from given patterns.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ValueProviderCapability extends Capability
{
    /**
     * Pattern for aliases.
     * <p/>
     * Examples:
     * 1) "95{digit:3}"           will generate 95001, 95002, 95003, ...
     * 2) "95{digit:2}2{digit:2}" will generate 9500201, 9500202, ..., 9501200, 9501201, ...
     */
    public static final String PATTERNS = "patterns";

    /**
     * Constructor.
     */
    public ValueProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param pattern to be added to the {@link #PATTERNS}
     */
    public ValueProviderCapability(String pattern)
    {
        addPattern(pattern);
    }

    /**
     * @return {@link #PATTERNS}
     */
    @Required
    public List<String> getPatterns()
    {
        return getPropertyStorage().getCollection(PATTERNS, List.class);
    }

    /**
     * @param patterns sets the {@link #PATTERNS}
     */
    public void setPatterns(List<String> patterns)
    {
        getPropertyStorage().setValue(PATTERNS, patterns);
    }

    /**
     * @param pattern to be added to the {@link #PATTERNS}
     */
    public void addPattern(String pattern)
    {
        getPropertyStorage().addCollectionItem(PATTERNS, pattern, List.class);
    }

    /**
     * @param pattern to be removed from the {@link #PATTERNS}
     */
    public void removePattern(String pattern)
    {
        getPropertyStorage().removeCollectionItem(PATTERNS, pattern);
    }
}
