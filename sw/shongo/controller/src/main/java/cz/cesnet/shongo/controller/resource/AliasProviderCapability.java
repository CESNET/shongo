package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.resource.value.PatternValueProvider;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;

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
     * Cache for provided {@link Technology}s.
     */
    private Set<Technology> cachedProvidedTechnologies;

    /**
     * Cache for provided {@link AliasType}s.
     */
    private Set<AliasType> cachedProvidedAliasTypes;

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
     * @throws EntityNotFoundException when alias doesn't exist
     */
    public Alias getAliasById(Long id) throws EntityNotFoundException
    {
        for (Alias alias : aliases) {
            if (alias.getId().equals(id)) {
                return alias;
            }
        }
        throw new EntityNotFoundException(Alias.class, id);
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
     *         for given {@code technology}, false otherwise
     */
    public boolean providesAliasTechnologies(Set<Technology> technologies)
    {
        if (cachedProvidedTechnologies == null) {
            cachedProvidedTechnologies = new HashSet<Technology>();
            for (Alias alias : aliases) {
                cachedProvidedTechnologies.add(alias.getTechnology());
            }
        }
        return cachedProvidedTechnologies.containsAll(technologies);
    }

    /**
     * @param aliasType to be checked
     * @return true if the {@link AliasProviderCapability} is able to provide an {@link Alias}
     *         of given {@code aliasType}, false otherwise
     */
    public boolean providesAliasType(AliasType aliasType)
    {
        if (cachedProvidedAliasTypes == null) {
            cachedProvidedAliasTypes = new HashSet<AliasType>();
            for (Alias alias : aliases) {
                cachedProvidedAliasTypes.add(alias.getType());
            }
        }
        return cachedProvidedAliasTypes.contains(aliasType);
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
        cz.cesnet.shongo.controller.api.AliasProviderCapability apiAliasProvider =
                (cz.cesnet.shongo.controller.api.AliasProviderCapability) api;
        for (Alias alias : aliases) {
            apiAliasProvider.addAlias(alias.toApi());
        }
        Resource valueProviderResource = valueProvider.getCapabilityResource();
        if (valueProviderResource != getResource()) {
            apiAliasProvider.setValueProvider(Domain.getLocalDomain().formatId(valueProviderResource));
        }
        else {
            apiAliasProvider.setValueProvider(valueProvider.toApi());
        }
        apiAliasProvider.setRestrictedToResource(isRestrictedToResource());
        apiAliasProvider.setPermanentRoom(isPermanentRoom());
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability capabilityApi, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderApi =
                (cz.cesnet.shongo.controller.api.AliasProviderCapability) capabilityApi;
        if (aliasProviderApi.isPropertyFilled(aliasProviderApi.RESTRICTED_TO_RESOURCE)) {
            setRestrictedToResource(aliasProviderApi.getRestrictedToResource());
        }
        if (aliasProviderApi.isPropertyFilled(aliasProviderApi.PERMANENT_ROOM)) {
            setPermanentRoom(aliasProviderApi.getPermanentRoom());
        }
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

        super.fromApi(capabilityApi, entityManager);
    }
}
