package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Capability tells that the device is able to participate in a videoconference call.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class TerminalCapability extends DeviceCapability
{
    /**
     * List of aliases that are permanently assigned to device.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * @return {@link #aliases}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param aliasId
     * @return alias with given {@code aliasId}
     * @throws cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException when the alias doesn't exist
     */
    private Alias getAliasById(Long aliasId) throws PersistentEntityNotFoundException
    {
        for (Alias alias : aliases) {
            if (alias.getId().equals(aliasId)) {
                return alias;
            }
        }
        throw new PersistentEntityNotFoundException(Alias.class, aliasId);
    }

    /**
     * @param alias alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }

    /**
     * @param alias alias to be removed from the {@link #aliases}
     */
    public void removeAlias(Alias alias)
    {
        aliases.remove(alias);
    }

    @Override
    public cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.TerminalCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.TerminalCapability terminalCapabilityApi =
                (cz.cesnet.shongo.controller.api.TerminalCapability) api;
        for (Alias alias : aliases) {
            terminalCapabilityApi.addAlias(alias.toApi());
        }
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.TerminalCapability apiTerminalCapability =
                (cz.cesnet.shongo.controller.api.TerminalCapability) api;
        // Create/modify aliases
        for (cz.cesnet.shongo.api.Alias apiAlias : apiTerminalCapability.getAliases()) {
            Alias alias;
            if (api.isPropertyItemMarkedAsNew(apiTerminalCapability.ALIASES, apiAlias)) {
                alias = new Alias();
                addAlias(alias);
            }
            else {
                alias = getAliasById(apiAlias.notNullIdAsLong());
            }
            alias.fromApi(apiAlias);
        }
        // Delete aliases
        Set<cz.cesnet.shongo.api.Alias> apiDeletedAliases = api
                .getPropertyItemsMarkedAsDeleted(apiTerminalCapability.ALIASES);
        for (cz.cesnet.shongo.api.Alias aliasApi : apiDeletedAliases) {
            Alias alias = getAliasById(aliasApi.notNullIdAsLong());
            removeAlias(alias);
        }
        super.fromApi(api, entityManager);
    }
}
