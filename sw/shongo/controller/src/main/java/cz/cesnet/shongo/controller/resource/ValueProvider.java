package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.PersistentObject;

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
public class ValueProvider extends PersistentObject
{
    /**
     * List of pattern for value generation.
     * <p/>
     * Examples:
     * 1) "95{digit:3}"     will generate 95001, 95002, 95003, ...
     * 2) "95{digit:2}2{digit:2}" will generate 9500201, 9500202, ..., 9501200, 9501201, ...
     */
    private List<String> patterns = new ArrayList<String>();

    /**
     * {@link Capability} which owns the{@link ValueProvider}.
     */
    private Capability capability;

    /**
     * Constructor.
     */
    public ValueProvider()
    {
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public ValueProvider(Capability capability)
    {
        this.capability = capability;
    }

    /**
     * Constructor.
     *
     * @param pattern to be added to the {@link #patterns}
     */
    public ValueProvider(Capability capability, String pattern)
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
    }

    /**
     * @param pattern to be removed from the {@link #patterns}
     */
    public void removePattern(String pattern)
    {
        this.patterns.remove(pattern);
    }

    /**
     * @return {@link #capability}
     */
    @OneToOne(optional = false)
    @Access(AccessType.FIELD)
    public Capability getCapability()
    {
        return capability;
    }

    /**
     * @return {@link Resource} from {@link #capability}
     */
    @Transient
    public Resource getCapabilityResource()
    {
        return capability.getResource();
    }

    /**
     * @return converted {@link ValueProvider} to API
     */
    public cz.cesnet.shongo.controller.api.ValueProvider toApi()
    {
        cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi =
                new cz.cesnet.shongo.controller.api.ValueProvider();
        valueProviderApi.setId(getId());
        for (String pattern : patterns) {
            valueProviderApi.addPattern(pattern);
        }
        return valueProviderApi;
    }

    /**
     * @param valueProviderApi from which the {@link ValueProvider} should be filled
     */
    public void fromApi(cz.cesnet.shongo.controller.api.ValueProvider valueProviderApi)
    {
        // Create patterns
        for (String pattern : valueProviderApi.getPatterns()) {
            if (valueProviderApi.isPropertyItemMarkedAsNew(valueProviderApi.PATTERNS, pattern)) {
                addPattern(pattern);
            }
        }
        // Delete patterns
        Set<String> patternsToDelete = valueProviderApi.getPropertyItemsMarkedAsDeleted(valueProviderApi.PATTERNS);
        for (String pattern : patternsToDelete) {
            removePattern(pattern);
        }
    }

    @Transient
    public ValueGenerator getValueGenerator()
    {
        return new PatternValueGenerator(getPatterns());
    }
}
