package cz.cesnet.shongo.controller.booking.participant;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import cz.cesnet.shongo.controller.booking.person.AbstractPerson;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.specification.Specification} for an endpoint which can participate in a conference.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class EndpointParticipant extends AbstractParticipant
{
    /**
     * Set of {@link cz.cesnet.shongo.Technology}s which are required/supported by this {@link cz.cesnet.shongo.controller.booking.specification.Specification}.
     */
    protected Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Persons that use the endpoint to participate in a conference.
     */
    private List<AbstractPerson> persons = new ArrayList<AbstractPerson>();

    /**
     * Defines who should initiate the call to this endpoint ({@code null} means that the {@link Scheduler}
     * can decide it).
     */
    private CallInitiation callInitiation;

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
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    /**
     * Update {@link #technologies} for this {@link cz.cesnet.shongo.controller.booking.specification.Specification}.
     */
    public void updateTechnologies()
    {
    }

    /**
     * @return {@link #persons}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<AbstractPerson> getPersons()
    {
        return persons;
    }

    /**
     * @param person to be added to the {@link #persons}
     */
    public void addPerson(AbstractPerson person)
    {
        persons.add(person);
    }

    /**
     * @param person to be removed from the {@link #persons}
     */
    public void removePerson(AbstractPerson person)
    {
        persons.remove(person);
    }

    /**
     * @return {@link #callInitiation}
     */
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public CallInitiation getCallInitiation()
    {
        return callInitiation;
    }

    /**
     * @param callInitiation sets the {@link #callInitiation}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
    }

    @Override
    public AbstractParticipant clone() throws CloneNotSupportedException
    {
        EndpointParticipant participant = (EndpointParticipant) super.clone();
        updateTechnologies();
        return participant;
    }

    @Override
    protected void cloneReset()
    {
        super.cloneReset();
        technologies = new HashSet<Technology>();
        persons = new ArrayList<AbstractPerson>();
    }

    @Override
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        EndpointParticipant endpointParticipant = (EndpointParticipant) participant;

        boolean modified = super.synchronizeFrom(participant);
        modified |= !ObjectHelper.isSameIgnoreOrder(getTechnologies(), endpointParticipant.getTechnologies());
        modified |= !ObjectHelper.isSame(getCallInitiation(), endpointParticipant.getCallInitiation());

        setTechnologies(endpointParticipant.getTechnologies());
        setCallInitiation(endpointParticipant.getCallInitiation());

        if (!ObjectHelper.isSameIgnoreOrder(getPersons(), endpointParticipant.getPersons())) {
            this.persons.clear();
            for (AbstractPerson person : endpointParticipant.getPersons()) {
                try {
                    addPerson(person.clone());
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
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi, EntityManager entityManager)
    {
        super.fromApi(participantApi, entityManager);

        // Update current technologies
        updateTechnologies();
    }
}
