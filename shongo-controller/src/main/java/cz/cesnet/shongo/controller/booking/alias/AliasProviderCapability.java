package cz.cesnet.shongo.controller.booking.alias;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.datetime.DateTimeSpecification;
import cz.cesnet.shongo.controller.booking.resource.Capability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.value.provider.PatternValueProvider;
import cz.cesnet.shongo.controller.booking.value.provider.ValueProvider;
import org.joda.time.DateTime;

import jakarta.persistence.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Capability tells that the resource acts as alias provider which can allocate aliases for itself and/or
 * other resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasProviderCapability extends Capability
{
    /**
     * {@link cz.cesnet.shongo.controller.booking.value.provider.PatternValueProvider} which is used
     */
    private ValueProvider valueProvider;

    /**
     * Type of aliases.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * Defines a maximum future to which the {@link AliasProviderCapability} is allocatable (e.g., can be set
     * as relative date/time which means that it can be always scheduled only e.g., to four month ahead).
     */
    private DateTimeSpecification maximumFuture;

    /**
     * Specifies whether the {@link AliasProviderCapability} can be allocated as {@link Alias}es only for
     * the owner {@link #resource} or for all {@link cz.cesnet.shongo.controller.booking.resource.Resource}s in the resource database.
     */
    private boolean restrictedToResource;

    /**
     * Cache for provided {@link Technology}s.
     */
    private Set<Technology> cachedProvidedTechnologies;

    /**
     * Cache for provided {@link AliasType}s.
     */
    private Set<AliasType> cachedProvidedAliasTypes;

    /**
     * Cache for value patterns.
     */
    private List<Pattern> cachedValuePatterns;

    /**
     * Constructor.
     */
    public AliasProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param pattern for construction of {@link cz.cesnet.shongo.controller.booking.value.provider.PatternValueProvider}
     * @param type    to be added as {@link Alias} to {@link #aliases}
     */
    public AliasProviderCapability(String pattern, AliasType type)
    {
        setValueProvider(new PatternValueProvider(this, pattern));
        addAlias(new Alias(type, "{value}"));
    }

    /**
     * Constructor.
     *
     * @param pattern              for construction of {@link cz.cesnet.shongo.controller.booking.value.provider.PatternValueProvider}
     * @param type                 to be added as {@link Alias} to {@link #aliases}
     * @param restrictedToResource sets the {@link #restrictedToResource}
     */
    public AliasProviderCapability(String pattern, AliasType type, boolean restrictedToResource)
    {
        setValueProvider(new PatternValueProvider(this, pattern));
        addAlias(new Alias(type, "{value}"));
        this.restrictedToResource = restrictedToResource;
    }

    /**
     * @return {@link #valueProvider}
     */
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Access(AccessType.FIELD)
    public ValueProvider getValueProvider()
    {
        return valueProvider;
    }

    /**
     * @param valueProvider sets the {@link #valueProvider}
     */
    public void setValueProvider(ValueProvider valueProvider)
    {
        this.valueProvider = valueProvider;
    }

    /**
     * @return {@link #aliases}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<Alias> getAliases()
    {
        return Collections.unmodifiableList(aliases);
    }

    /**
     * @param alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        this.aliases.add(alias);

        // Reset caches
        this.cachedProvidedAliasTypes = null;
        this.cachedProvidedTechnologies = null;
        this.cachedValuePatterns = null;
    }

    /**
     * @param alias to be removed from the {@link #aliases}
     */
    public void removeAlias(Alias alias)
    {
        this.aliases.remove(alias);

        // Reset caches
        this.cachedProvidedAliasTypes = null;
        this.cachedProvidedTechnologies = null;
        this.cachedValuePatterns = null;
    }

    /**
     * @return {@link #maximumFuture}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public DateTimeSpecification getMaximumFuture()
    {
        return maximumFuture;
    }

    @Override
    public DateTime getMaximumFutureDateTime(DateTime referenceDateTime)
    {
        if (maximumFuture == null) {
            return super.getMaximumFutureDateTime(referenceDateTime);
        }
        return maximumFuture.getEarliest(referenceDateTime);
    }

    /**
     * @param maximumFuture sets the {@link #maximumFuture}
     */
    public void setMaximumFuture(DateTimeSpecification maximumFuture)
    {
        this.maximumFuture = maximumFuture;
    }

    /**
     * @return {@link #restrictedToResource}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isRestrictedToResource()
    {
        return restrictedToResource;
    }

    /**
     * @param restrictedToResource sets the {@link #restrictedToResource}
     */
    public void setRestrictedToResource(boolean restrictedToResource)
    {
        this.restrictedToResource = restrictedToResource;
    }

    /**
     * @param technologies to be checked
     * @return true if the {@link AliasProviderCapability} is able to provide an {@link Alias}
     *         for any of given {@code technologies},
     *         false otherwise
     */
    public boolean providesAliasTechnology(Set<Technology> technologies)
    {
        if (cachedProvidedTechnologies == null) {
            cachedProvidedTechnologies = new HashSet<Technology>();
            for (Alias alias : aliases) {
                cachedProvidedTechnologies.add(alias.getTechnology());
            }
        }
        if (!Collections.disjoint(cachedProvidedTechnologies, technologies)) {
            return true;
        }
        if (technologies.size() > 0) {
            if (cachedProvidedTechnologies.contains(Technology.ALL)) {
                Resource resource = getResource();
                if (resource instanceof DeviceResource) {
                    DeviceResource deviceResource = (DeviceResource) resource;
                    return !Collections.disjoint(deviceResource.getTechnologies(), technologies);
                }
            }
        }
        return false;
    }

    /**
     * @param aliasTypes to be checked
     * @return true if the {@link AliasProviderCapability} is able to provide an {@link Alias}
     *         for any of given {@code aliasTypes},
     *         false otherwise
     */
    public boolean providesAliasType(Set<AliasType> aliasTypes)
    {
        if (cachedProvidedAliasTypes == null) {
            cachedProvidedAliasTypes = new HashSet<AliasType>();
            for (Alias alias : aliases) {
                cachedProvidedAliasTypes.add(alias.getType());
            }
        }
        return !Collections.disjoint(cachedProvidedAliasTypes, aliasTypes);
    }

    /**
     * @see PreUpdate
     */
    @PreUpdate
    @PrePersist
    protected void onUpdate()
    {
        if (isRestrictedToResource() && !(getResource() instanceof DeviceResource)) {
            throw new RuntimeException("Restricted to resource option can be enabled only in a device resource.");
        }
    }

    @Override
    public void loadLazyProperties()
    {
        super.loadLazyProperties();

        getAliases().size();
        getValueProvider().loadLazyProperties();
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.AliasProviderCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderApi =
                (cz.cesnet.shongo.controller.api.AliasProviderCapability) api;

        for (Alias alias : aliases) {
            aliasProviderApi.addAlias(alias.toApi());
        }

        Resource valueProviderResource = valueProvider.getCapabilityResource();
        if (valueProviderResource != getResource()) {
            aliasProviderApi.setValueProvider(ObjectIdentifier.formatId(valueProviderResource));
        }
        else {
            aliasProviderApi.setValueProvider(valueProvider.toApi());
        }

        DateTimeSpecification maximumFuture = getMaximumFuture();
        if (maximumFuture != null) {
            aliasProviderApi.setMaximumFuture(maximumFuture.toApi());
        }

        aliasProviderApi.setRestrictedToResource(isRestrictedToResource());
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability capabilityApi, EntityManager entityManager)
    {
        super.fromApi(capabilityApi, entityManager);

        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderApi =
                (cz.cesnet.shongo.controller.api.AliasProviderCapability) capabilityApi;

        Object valueProviderApi = aliasProviderApi.getValueProvider();
        setValueProvider(ValueProvider.modifyFromApi(valueProviderApi, this.valueProvider, this, entityManager));

        Object maximumFuture = aliasProviderApi.getMaximumFuture();
        if (maximumFuture == null) {
            setMaximumFuture(null);
        }
        else {
            setMaximumFuture(DateTimeSpecification.fromApi(maximumFuture, getMaximumFuture()));
        }

        setRestrictedToResource(aliasProviderApi.getRestrictedToResource());

        Synchronization.synchronizeCollection(aliases, aliasProviderApi.getAliases(),
                new Synchronization.Handler<Alias, cz.cesnet.shongo.api.Alias>(Alias.class)
                {
                    @Override
                    public Alias createFromApi(cz.cesnet.shongo.api.Alias objectApi)
                    {
                        Alias alias = new Alias();
                        alias.fromApi(objectApi);
                        return alias;
                    }

                    @Override
                    public void updateFromApi(Alias object, cz.cesnet.shongo.api.Alias objectApi)
                    {
                        object.fromApi(objectApi);
                    }
                });
    }

    public String parseValue(String value)
    {
        if (cachedValuePatterns == null) {
            cachedValuePatterns = new LinkedList<Pattern>();
            for (Alias alias : aliases) {
                String aliasValue = alias.getValue();
                aliasValue = aliasValue.replaceAll("\\{.+\\}", "(.+)");
                cachedValuePatterns.add(Pattern.compile(aliasValue));
            }
        }
        for (Pattern pattern : cachedValuePatterns) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
                if (matcher.groupCount() > 0) {
                    return matcher.group(1);
                }
            }
        }
        return value;
    }
}
