package cz.cesnet.shongo.controller.booking.participant;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.executable.EndpointProvider;
import cz.cesnet.shongo.controller.booking.executable.ExternalEndpointSet;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an external (not existing) {@link EndpointParticipant}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpointSetParticipant extends AbstractParticipant implements EndpointProvider
{
    /**
     * Set of {@link Technology}s which are required/supported by this {@link ExternalEndpointSetParticipant}.
     */
    protected Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Number of external endpoints of the same type.
     */
    private int count = 1;

    /**
     * Constructor.
     */
    public ExternalEndpointSetParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param technology
     */
    public ExternalEndpointSetParticipant(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param technology
     * @param count
     */
    public ExternalEndpointSetParticipant(Technology technology, int count)
    {
        addTechnology(technology);
        setCount(count);
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies.clear();
        this.technologies.addAll(technologies);
    }

    /**
     * Clear the {@link #technologies}
     */
    public void clearTechnologies()
    {
        technologies.clear();
    }

    /**
     * @param technologies to be added to the {@link #technologies}
     */
    public void addTechnologies(Set<Technology> technologies)
    {
        this.technologies.addAll(technologies);
    }

    /**
     * @param technology technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @return {@link #count}
     */
    @Column(name = "same_count")
    public int getCount()
    {
        return count;
    }

    /**
     * @param count sets the {@link #count}
     */
    public void setCount(int count)
    {
        this.count = count;
    }

    @Override
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        ExternalEndpointSetParticipant externalEndpointSetParticipant = (ExternalEndpointSetParticipant) participant;

        boolean modified = super.synchronizeFrom(participant);
        modified |= !ObjectHelper.isSameIgnoreOrder(getTechnologies(), externalEndpointSetParticipant.getTechnologies());
        modified |= !ObjectHelper.isSame(getCount(), externalEndpointSetParticipant.getCount());

        setTechnologies(externalEndpointSetParticipant.getTechnologies());
        setCount(externalEndpointSetParticipant.getCount());

        return modified;
    }

    @Override
    protected void cloneReset()
    {
        super.cloneReset();
        technologies = new HashSet<Technology>();
    }

    @Override
    @Transient
    public Endpoint getEndpoint()
    {
        return new ExternalEndpointSet(this);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractParticipant createApi()
    {
        return new cz.cesnet.shongo.controller.api.ExternalEndpointSetParticipant();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi)
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSetParticipant externalEndpointSetParticipantApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSetParticipant) participantApi;
        externalEndpointSetParticipantApi.setCount(getCount());
        for (Technology technology : getTechnologies()) {
            externalEndpointSetParticipantApi.addTechnology(technology);
        }
        super.toApi(participantApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSetParticipant externalEndpointSetParticipantApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSetParticipant) participantApi;

        setCount(externalEndpointSetParticipantApi.getCount());

        clearTechnologies();
        addTechnologies(externalEndpointSetParticipantApi.getTechnologies());

        super.fromApi(participantApi, entityManager);
    }
}
