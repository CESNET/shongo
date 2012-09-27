package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;

import javax.persistence.*;

/**
 * Represents a {@link Reservation} for a {@link Alias}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasReservation extends Reservation
{
    /**
     * {@link cz.cesnet.shongo.controller.resource.AliasProviderCapability} from which the alias is allocated.
     */
    private AliasProviderCapability aliasProviderCapability;

    /**
     * Alias that is allocated.
     */
    private Alias alias;

    /**
     * @return {@link #aliasProviderCapability}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public AliasProviderCapability getAliasProviderCapability()
    {
        return aliasProviderCapability;
    }

    /**
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public void setAliasProviderCapability(AliasProviderCapability aliasProviderCapability)
    {
        this.aliasProviderCapability = aliasProviderCapability;
    }

    /**
     * @return {@link #alias}
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public Alias getAlias()
    {
        return alias;
    }

    /**
     * @param alias sets the {@link #alias}
     */
    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    @Override
    public cz.cesnet.shongo.controller.api.AliasReservation toApi(Domain domain)
    {
        return (cz.cesnet.shongo.controller.api.AliasReservation) super.toApi(domain);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.AliasReservation();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, Domain domain)
    {
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservationApi =
                (cz.cesnet.shongo.controller.api.AliasReservation) api;
        aliasReservationApi.setResourceIdentifier(
                domain.formatIdentifier(aliasProviderCapability.getResource().getId()));
        aliasReservationApi.setResourceName(aliasProviderCapability.getResource().getName());
        aliasReservationApi.setAlias(getAlias().toApi());
        super.toApi(api, domain);
    }
}
