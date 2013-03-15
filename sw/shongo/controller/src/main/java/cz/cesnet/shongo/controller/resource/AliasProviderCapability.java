package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ControllerImplFaultSet;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.DateTimeSpecification;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.resource.value.PatternValueProvider;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.old.OldFaultException;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.*;

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
     * {@link cz.cesnet.shongo.controller.resource.value.PatternValueProvider} which is used
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
     * the owner {@link #resource} or for all {@link Resource}s in the resource database.
     */
    private boolean restrictedToResource;

    /**
     * Specifies whether the {@link Alias}es allocated for the {@link AliasProviderCapability}
     * should represent permanent rooms (should get allocated {@link RoomEndpoint}).
     */
    private boolean permanentRoom;

    /**
     * Cache for provided {@link Technology}s.
     */
    private Set<Technology> cachedProvidedTechnologies;

    /**
     * Cache for provided {@link AliasType}s.
     */
    private Set<AliasType> cachedProvidedAliasTypes;

    /**
     * Constructor.
     */
    public AliasProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param pattern for construction of {@link cz.cesnet.shongo.controller.resource.value.PatternValueProvider}
     * @param type    to be added as {@link cz.cesnet.shongo.controller.resource.Alias} to {@link #aliases}
     */
    public AliasProviderCapability(String pattern, AliasType type)
    {
        setValueProvider(new PatternValueProvider(this, pattern));
        addAlias(new Alias(type, "{value}"));
    }

    /**
     * Constructor.
     *
     * @param pattern              for construction of {@link cz.cesnet.shongo.controller.resource.value.PatternValueProvider}
     * @param type                 to be added as {@link cz.cesnet.shongo.controller.resource.Alias} to {@link #aliases}
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
    @ManyToOne(cascade = CascadeType.ALL)
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
     * @param id
     * @return alias with given {@code id}
     * @throws FaultException when alias doesn't exist
     */
    public Alias getAliasById(Long id) throws FaultException
    {
        for (Alias alias : aliases) {
            if (alias.getId().equals(id)) {
                return alias;
            }
        }
        return ControllerImplFaultSet.throwEntityNotFoundFault(Alias.class, id);
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
    public final boolean isAvailableInFuture(DateTime dateTime, DateTime referenceDateTime)
    {
        if (maximumFuture != null) {
            DateTime earliestDateTime = maximumFuture.getEarliest(referenceDateTime);
            return !dateTime.isAfter(earliestDateTime);
        }
        return super.isAvailableInFuture(dateTime, referenceDateTime);
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
     * @return {@link #permanentRoom}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isPermanentRoom()
    {
        return permanentRoom;
    }

    /**
     * @param permanentRoom sets the {@link #permanentRoom}
     */
    public void setPermanentRoom(boolean permanentRoom)
    {
        this.permanentRoom = permanentRoom;
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
        // When for alias should be allocated permanent room, it should be also restricted to the owner resource
        // (room must be allocated on a concrete resource)
        if (isPermanentRoom() && !isRestrictedToResource()) {
            setRestrictedToResource(true);
        }
        if (isRestrictedToResource() && !(getResource() instanceof DeviceResource)) {
            throw new IllegalStateException("Restricted to resource option can be enabled only in a device resource.");
        }
        if (isPermanentRoom()) {
            Resource resource = getResource();
            if (!(resource instanceof DeviceResource)) {
                throw new IllegalStateException("Permanent room option can be enabled only in a device resource.");
            }
            if (!resource.hasCapability(RoomProviderCapability.class)) {
                throw new IllegalStateException("Permanent room option can be enabled only in a device resource"
                        + " with alias provider capability.");
            }
        }
    }

    @Override
    public void loadLazyCollections()
    {
        super.loadLazyCollections();

        getAliases().size();
        getValueProvider().loadLazyCollections();
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
            aliasProviderApi.setValueProvider(EntityIdentifier.formatId(valueProviderResource));
        }
        else {
            aliasProviderApi.setValueProvider(valueProvider.toApi());
        }

        DateTimeSpecification maximumFuture = getMaximumFuture();
        if (maximumFuture != null) {
            aliasProviderApi.setMaximumFuture(maximumFuture.toApi());
        }

        aliasProviderApi.setRestrictedToResource(isRestrictedToResource());
        aliasProviderApi.setPermanentRoom(isPermanentRoom());
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability capabilityApi, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderApi =
                (cz.cesnet.shongo.controller.api.AliasProviderCapability) capabilityApi;

        if (aliasProviderApi.isPropertyFilled(aliasProviderApi.VALUE_PROVIDER)) {
            Object valueProviderApi = aliasProviderApi.getValueProvider();
            setValueProvider(ValueProvider.modifyFromApi(valueProviderApi, this.valueProvider, this, entityManager));
        }

        // Create/modify aliases
        for (cz.cesnet.shongo.api.Alias apiAlias : aliasProviderApi.getAliases()) {
            if (capabilityApi.isPropertyItemMarkedAsNew(aliasProviderApi.ALIASES, apiAlias)) {
                Alias alias = new Alias();
                alias.fromApi(apiAlias);
                addAlias(alias);
            }
            else {
                Alias alias = getAliasById(apiAlias.notNullIdAsLong());
                alias.fromApi(apiAlias);
            }
        }
        // Delete aliases
        Set<cz.cesnet.shongo.api.Alias> apiDeletedAliases =
                capabilityApi.getPropertyItemsMarkedAsDeleted(aliasProviderApi.ALIASES);
        for (cz.cesnet.shongo.api.Alias apiAlias : apiDeletedAliases) {
            removeAlias(getAliasById(apiAlias.notNullIdAsLong()));
        }

        if (aliasProviderApi.isPropertyFilled(aliasProviderApi.MAXIMUM_FUTURE)) {
            Object maximumFuture = aliasProviderApi.getMaximumFuture();
            if (maximumFuture == null) {
                setMaximumFuture(null);
            }
            else {
                setMaximumFuture(DateTimeSpecification.fromApi(maximumFuture, getMaximumFuture()));
            }
        }
        if (aliasProviderApi.isPropertyFilled(aliasProviderApi.RESTRICTED_TO_RESOURCE)) {
            setRestrictedToResource(aliasProviderApi.getRestrictedToResource());
        }
        if (aliasProviderApi.isPropertyFilled(aliasProviderApi.PERMANENT_ROOM)) {
            setPermanentRoom(aliasProviderApi.getPermanentRoom());
        }

        super.fromApi(capabilityApi, entityManager);
    }
}
