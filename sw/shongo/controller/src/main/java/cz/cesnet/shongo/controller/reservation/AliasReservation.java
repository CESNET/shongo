package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.scheduler.report.DurationLongerThanMaximumReport;
import cz.cesnet.shongo.fault.TodoImplementException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
     * Alias value that is allocated.
     */
    private String aliasValue;

    /**
     * Constructor.
     */
    public AliasReservation()
    {
    }

    /**
     * @return {@link #aliasProviderCapability}
     */
    @ManyToOne(optional = false)
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
     * @return {@link #aliasValue}
     */
    @Column(nullable = false)
    public String getAliasValue()
    {
        return aliasValue;
    }

    /**
     * @param aliasValue sets the {@link #aliasValue}
     */
    public void setAliasValue(String aliasValue)
    {
        this.aliasValue = aliasValue;
    }

    /**
     * @return collection of {@link Alias}es which are allocated by the {@link #aliasValue}
     */
    @Transient
    public Collection<Alias> getAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        for ( Alias aliasTemplate : aliasProviderCapability.getAliases() ) {
            Alias alias = new Alias();
            alias.setType(aliasTemplate.getType());
            alias.setValue(aliasTemplate.getValue().replace("{value}", aliasValue));
            aliases.add(alias);
        }
        return aliases;
    }

    @Override
    public void validate(cz.cesnet.shongo.controller.Cache cache) throws ReportException
    {
        Period duration = getSlot().toPeriod();
        Period maxDuration = cache.getAliasReservationMaximumDuration();
        if (TemporalHelper.isPeriodLongerThan(duration, maxDuration)) {
            throw new DurationLongerThanMaximumReport(duration, maxDuration).exception();
        }
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
        aliasReservationApi.setAliasValue(getAliasValue());
        for (Alias alias : getAliases()) {
            aliasReservationApi.addAlias(alias.toApi());
        }
        super.toApi(api, domain);
    }
}
