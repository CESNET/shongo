package cz.cesnet.shongo.controller.booking.value.provider;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.resource.Capability;
import cz.cesnet.shongo.TodoImplementException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

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
     * Option specifying whether any requested values are allowed event those which doesn't
     * match the {@link #patterns}, i.e., {@link #getRequestedValuePattern()}.
     */
    private boolean allowAnyRequestedValue;

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
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
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

    /**
     * @return {@link #allowAnyRequestedValue}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isAllowAnyRequestedValue()
    {
        return allowAnyRequestedValue;
    }

    /**
     * @param allowAnyRequestedValue sets the {@link #allowAnyRequestedValue}
     */
    public void setAllowAnyRequestedValue(boolean allowAnyRequestedValue)
    {
        this.allowAnyRequestedValue = allowAnyRequestedValue;
    }

    @Override
    public void loadLazyProperties()
    {
        super.loadLazyProperties();

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
        patternValueProviderApi.setAllowAnyRequestedValue(isAllowAnyRequestedValue());
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi, EntityManager entityManager)
    {
        super.fromApi(valueProviderApi, entityManager);

        cz.cesnet.shongo.controller.api.ValueProvider.Pattern patternValueProviderApi =
                (cz.cesnet.shongo.controller.api.ValueProvider.Pattern) valueProviderApi;

        setAllowAnyRequestedValue(patternValueProviderApi.getAllowAnyRequestedValue());

        Synchronization.synchronizeCollection(patterns, patternValueProviderApi.getPatterns());
    }

    /**
     * Patterns for the generating.
     */
    private List<Pattern> parsedPatterns;

    /**
     * {@link java.util.regex.Pattern} for matching requested values.
     */
    private java.util.regex.Pattern requestedValuePattern;

    /**
     * @return initialized {@link #parsedPatterns}
     */
    @Transient
    private List<Pattern> getParsedPatterns()
    {
        if (parsedPatterns == null) {
            parsedPatterns = new ArrayList<Pattern>();
            for (String pattern : patterns) {
                Pattern parsedPattern = new Pattern();
                parsedPattern.parse(pattern);
                parsedPatterns.add(parsedPattern);
            }
        }
        return parsedPatterns;
    }

    @Transient
    private java.util.regex.Pattern getRequestedValuePattern()
    {
        if (requestedValuePattern == null) {
            StringBuilder patternBuilder = new StringBuilder();
            for (Pattern pattern : getParsedPatterns()) {
                if (patternBuilder.length() > 0) {
                    patternBuilder.append("|");
                }
                patternBuilder.append("(");
                for (Pattern.PatternComponent patternComponent : pattern) {
                    patternBuilder.append("(");
                    patternBuilder.append(patternComponent.getRegexPattern());
                    patternBuilder.append(")");
                }
                patternBuilder.append(")");
            }
            requestedValuePattern = java.util.regex.Pattern.compile(patternBuilder.toString());
        }
        return requestedValuePattern;
    }


    @Override
    public String generateValue(Set<String> usedValues) throws NoAvailableValueException
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
            throw new NoAvailableValueException();
        }

        return value;
    }

    @Override
    @Transient
    public String generateValue(Set<String> usedValues, String requestedValue)
            throws ValueAlreadyAllocatedException, InvalidValueException
    {
        if (!isAllowAnyRequestedValue()) {
            Matcher matcher = getRequestedValuePattern().matcher(requestedValue);
            if (!matcher.matches()) {
                throw new InvalidValueException();
            }
            int groupIndex = 1;
            for (int patternIndex = 0; patternIndex < parsedPatterns.size(); patternIndex++) {
                groupIndex++;
                Pattern pattern = parsedPatterns.get(patternIndex);
                for (int patternComponentIndex = 0; patternComponentIndex < pattern.size(); patternComponentIndex++) {
                    String groupValue = matcher.group(groupIndex);
                    if (groupValue != null) {
                        Pattern.PatternComponent patternComponent = pattern.get(patternComponentIndex);
                        if (!patternComponent.isValueValid(groupValue)) {
                            throw new InvalidValueException();
                        }
                    }
                    groupIndex++;
                }
            }
        }
        for (String usedValue : usedValues) {
            if (usedValue.equalsIgnoreCase(requestedValue)) {
                throw new ValueAlreadyAllocatedException();
            }
        }

        return requestedValue;
    }
}
