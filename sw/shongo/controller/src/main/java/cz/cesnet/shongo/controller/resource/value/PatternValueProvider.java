package cz.cesnet.shongo.controller.resource.value;

import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Object which can allocate unique values based on the specified patterns.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PatternValueProvider extends ValueProvider
{
    /**
     * List of patterns for value generation.
     * <p/>
     * Examples:
     * 1) "95{digit:3}"     will generate 95001, 95002, 95003, ...
     * 2) "95{digit:2}2{digit:2}" will generate 9500201, 9500202, ..., 9501200, 9501201, ...
     */
    private List<String> patterns = new ArrayList<String>();

    /**
     * Constructor.
     */
    public PatternValueProvider()
    {
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public PatternValueProvider(Capability capability)
    {
        super(capability);
    }

    /**
     * Constructor.
     *
     * @param pattern to be added to the {@link #patterns}
     */
    public PatternValueProvider(Capability capability, String pattern)
    {
        this(capability);

        addPattern(pattern);
    }

    /**
     * @return {@link #patterns}
     */
    @ElementCollection
    @Access(AccessType.FIELD)
    public List<String> getPatterns()
    {
        return Collections.unmodifiableList(patterns);
    }

    /**
     * @param pattern to be added to the {@link #patterns}
     */
    public void addPattern(String pattern)
    {
        this.patterns.add(pattern);
        if (this.parsedPatterns != null) {
            throw new TodoImplementException("Add parsed pattern.");
        }
    }

    /**
     * @param pattern to be removed from the {@link #patterns}
     */
    public void removePattern(String pattern)
    {
        this.patterns.remove(pattern);
        if (this.parsedPatterns != null) {
            throw new TodoImplementException("Remove parsed pattern.");
        }
    }

    @Override
    public void loadLazyCollections()
    {
        super.loadLazyCollections();

        getPatterns().size();
    }

    @Override
    protected cz.cesnet.shongo.controller.api.ValueProvider createApi()
    {
        return new cz.cesnet.shongo.controller.api.ValueProvider.Pattern();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi)
    {
        super.toApi(valueProviderApi);

        cz.cesnet.shongo.controller.api.ValueProvider.Pattern patternValueProviderApi =
                (cz.cesnet.shongo.controller.api.ValueProvider.Pattern) valueProviderApi;
        for (String pattern : patterns) {
            patternValueProviderApi.addPattern(pattern);
        }
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi, EntityManager entityManager)
            throws FaultException
    {
        super.fromApi(valueProviderApi, entityManager);

        cz.cesnet.shongo.controller.api.ValueProvider.Pattern patternValueProviderApi =
                (cz.cesnet.shongo.controller.api.ValueProvider.Pattern) valueProviderApi;
        // Create patterns
        for (String pattern : patternValueProviderApi.getPatterns()) {
            if (valueProviderApi.isPropertyItemMarkedAsNew(patternValueProviderApi.PATTERNS, pattern)) {
                addPattern(pattern);
            }
        }
        // Delete patterns
        Set<String> patternsToDelete =
                valueProviderApi.getPropertyItemsMarkedAsDeleted(patternValueProviderApi.PATTERNS);
        for (String pattern : patternsToDelete) {
            removePattern(pattern);
        }
    }

    /**
     * Patterns for the generating.
     */
    private List<Pattern> parsedPatterns;

    /**
     * @return initialized {@link #parsedPatterns}
     */
    @Transient
    private List<Pattern> getParsedPatterns()
    {
        if ( parsedPatterns == null) {
            parsedPatterns = new ArrayList<Pattern>();
            for (String pattern : patterns) {
                Pattern parsedPattern = new Pattern();
                parsedPattern.parse(pattern);
                parsedPatterns.add(parsedPattern);
            }
        }
        return parsedPatterns;
    }

    @Override
    public String generateValue(Set<String> usedValues)
    {
        String value = null;
        for (Pattern pattern : getParsedPatterns()) {
            pattern.reset();
            do {
                value = pattern.generate();
            } while (value != null && usedValues.contains(value));
            if (value != null) {
                break;
            }
        }
        if (value == null) {
            return null;
        }

        return value;
    }

    @Override
    @Transient
    public String generateValue(Set<String> usedValues, String requestedValue)
    {
        for (Pattern pattern : getParsedPatterns()) {
            if (pattern.size() == 1) {
                Pattern.PatternComponent patternComponent = pattern.get(0);
                if (patternComponent instanceof Pattern.StringPatternComponent) {
                    if (Pattern.StringPatternComponent.VALUE_PATTERN.matcher(requestedValue).matches()) {
                        if (!usedValues.contains(requestedValue)) {
                            return requestedValue;
                        }
                    }
                }
                else {
                    throw new TodoImplementException("PatternValueGenerator.isValueAvailable for %s.",
                            patternComponent.getClass().getSimpleName());
                }
            }
            else {
                throw new TodoImplementException("PatternValueGenerator.isValueAvailable for multiple components.");
            }
        }
        return null;
    }
}
