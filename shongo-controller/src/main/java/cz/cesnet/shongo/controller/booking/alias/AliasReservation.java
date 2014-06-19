package cz.cesnet.shongo.controller.booking.alias;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.reservation.TargetedReservation;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.reservation.Reservation} for a {@link Alias}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasReservation extends TargetedReservation
{
    /**
     * {@link AliasProviderCapability} from which the alias is allocated.
     */
    private AliasProviderCapability aliasProviderCapability;

    /**
     * {@link cz.cesnet.shongo.controller.booking.value.ValueReservation} for value which is used for the {@link #aliasProviderCapability#getAliases()}
     */
    private ValueReservation valueReservation;

    /**
     * Constructor.
     */
    public AliasReservation()
    {
    }

    /**
     * @return {@link #aliasProviderCapability}
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
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
     * @return {@link #valueReservation}
     */
    @ManyToOne(cascade = CascadeType.PERSIST, optional = false)
    @Access(AccessType.FIELD)
    public ValueReservation getValueReservation()
    {
        return valueReservation;
    }

    /**
     * @param valueReservation sets the {@link #valueReservation}
     */
    public void setValueReservation(ValueReservation valueReservation)
    {
        this.valueReservation = valueReservation;
    }

    /**
     * @return {@link #valueReservation#getValue()}
     */
    @Transient
    public String getValue()
    {
        return valueReservation.getValue();
    }

    /**
     * @param alias to be evaluated by the {@link #valueReservation} and {@link #aliasProviderCapability}
     */
    private void evaluateAlias(Alias alias)
    {
        String aliasValue = alias.getValue();
        aliasValue = valueReservation.evaluateValue(aliasValue);
        alias.setValue(aliasValue);
        if (aliasProviderCapability.isRestrictedToResource()) {
            Resource resource = aliasProviderCapability.getResource();
            if (resource instanceof DeviceResource) {
                DeviceResource deviceResource = (DeviceResource) resource;
                deviceResource.evaluateAlias(alias);
            }
        }
    }

    /**
     * @return collection of {@link Alias}es which are allocated by the {@link #valueReservation}
     */
    @Transient
    public Collection<Alias> getAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        for (Alias aliasTemplate : aliasProviderCapability.getAliases()) {
            Alias alias = new Alias();
            alias.setType(aliasTemplate.getType());
            alias.setValue(aliasTemplate.getValue());
            evaluateAlias(alias);
            aliases.add(alias);
        }
        return aliases;
    }

    /**
     * @return collection of {@link Alias}es which are allocated by the {@link #valueReservation}
     */
    @Transient
    public Alias getAlias(AliasType aliasType)
    {
        for (Alias aliasTemplate : aliasProviderCapability.getAliases()) {
            if (aliasTemplate.getType().equals(aliasType)) {
                Alias alias = new Alias();
                alias.setType(aliasTemplate.getType());
                alias.setValue(aliasTemplate.getValue());
                evaluateAlias(alias);
                return alias;
            }
        }
        return null;
    }

    @Override
    public cz.cesnet.shongo.controller.api.AliasReservation toApi(EntityManager entityManager, boolean administrator)
    {
        return (cz.cesnet.shongo.controller.api.AliasReservation) super.toApi(entityManager, administrator);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.AliasReservation();
    }

    @Override
    protected void toApi(Reservation api, EntityManager entityManager, boolean admin)
    {
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservationApi =
                (cz.cesnet.shongo.controller.api.AliasReservation) api;
        aliasReservationApi.setResourceId(ObjectIdentifier.formatId(aliasProviderCapability.getResource()));
        aliasReservationApi.setResourceName(aliasProviderCapability.getResource().getName());
        aliasReservationApi.setValueReservation(valueReservation.toApi(entityManager, admin));
        for (Alias alias : getAliases()) {
            aliasReservationApi.addAlias(alias.toApi());
        }
        super.toApi(api, entityManager, admin);
    }

    @Override
    @Transient
    public Long getTargetId()
    {
        return aliasProviderCapability.getId();
    }

    @Override
    @Transient
    public Resource getAllocatedResource()
    {
        return aliasProviderCapability.getResource();
    }
}
