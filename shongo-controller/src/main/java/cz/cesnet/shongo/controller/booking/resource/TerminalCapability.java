package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.alias.Alias;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Capability tells that the device is able to participate in a conference call.
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
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException when the alias doesn't exist
     */
    private Alias getAliasById(Long aliasId) throws CommonReportSet.ObjectNotExistsException
    {
        for (Alias alias : aliases) {
            if (alias.getId().equals(aliasId)) {
                return alias;
            }
        }
        return ControllerReportSetHelper.throwObjectNotExistFault(Alias.class, aliasId);
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
    {
        super.fromApi(api, entityManager);

        cz.cesnet.shongo.controller.api.TerminalCapability apiTerminalCapability =
                (cz.cesnet.shongo.controller.api.TerminalCapability) api;

        Synchronization.synchronizeCollection(aliases, apiTerminalCapability.getAliases(),
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
}
