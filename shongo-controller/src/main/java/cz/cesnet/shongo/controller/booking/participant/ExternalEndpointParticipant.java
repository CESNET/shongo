package cz.cesnet.shongo.controller.booking.participant;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.executable.EndpointProvider;
import cz.cesnet.shongo.controller.booking.executable.ExternalEndpoint;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an external (not existing in resource database) {@link EndpointParticipant}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpointParticipant extends EndpointParticipant implements EndpointProvider
{
    /**
     * List of aliases that can be used to reference the external endpoint.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * Constructor.
     */
    public ExternalEndpointParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param technology
     */
    public ExternalEndpointParticipant(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param technology
     * @param alias
     */
    public ExternalEndpointParticipant(Technology technology, Alias alias)
    {
        if (technology == null) {
            throw new IllegalArgumentException("Technology cannot be null!");
        }
        else if (!technology.equals(alias.getTechnology())) {
            throw new IllegalArgumentException("Cannot use alias for technology '" + alias.getTechnology().getName()
                    + "' for an external endpoint with technology '" + technology.getName() + "!");
        }
        addTechnology(technology);

        addAlias(alias);
    }

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
     * @param aliases sets the {@link #aliases}
     */
    public void setAliases(List<Alias> aliases)
    {
        this.aliases.clear();
        for (Alias alias : aliases) {
            this.aliases.add(alias);
        }
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
    public AbstractParticipant clone() throws CloneNotSupportedException
    {
        ExternalEndpointParticipant participant = (ExternalEndpointParticipant) super.clone();
        updateTechnologies();
        return participant;
    }

    @Override
    protected void cloneReset()
    {
        super.cloneReset();
        aliases = new ArrayList<Alias>();
    }

    @Override
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        ExternalEndpointParticipant externalEndpointParticipant = (ExternalEndpointParticipant) participant;

        boolean modified = super.synchronizeFrom(participant);

        if (!ObjectHelper.isSameIgnoreOrder(getAliases(), externalEndpointParticipant.getAliases())) {
            this.aliases.clear();
            for (Alias alias : externalEndpointParticipant.getAliases()) {
                try {
                    addAlias(alias.clone());
                }
                catch (CloneNotSupportedException exception) {
                    throw new RuntimeException(exception);
                }
            }
            modified = true;
        }

        return modified;
    }

    @Override
    @Transient
    public Endpoint getEndpoint()
    {
        return new ExternalEndpoint(this);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractParticipant createApi()
    {
        return new cz.cesnet.shongo.controller.api.ExternalEndpointParticipant();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi)
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointParticipant externalEndpointParticipantApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointParticipant) participantApi;
        for (Technology technology : getTechnologies()) {
            externalEndpointParticipantApi.addTechnology(technology);
        }
        if (aliases.size() > 0) {
            if (aliases.size() == 1) {
                externalEndpointParticipantApi.setAlias(aliases.iterator().next().toApi());
            }
            else {
                throw new TodoImplementException("Allow multiple aliases in external endpoint participant in API.");
            }
        }
        super.toApi(participantApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointParticipant externalEndpointParticipantApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointParticipant) participantApi;

        Synchronization.synchronizeCollection(technologies, externalEndpointParticipantApi.getTechnologies());

        aliases.clear();
        if (externalEndpointParticipantApi.getAlias() != null) {
            Alias alias = new Alias();
            alias.fromApi(externalEndpointParticipantApi.getAlias());
            addAlias(alias);
        }

        super.fromApi(participantApi, entityManager);
    }
}
