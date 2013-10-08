package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.common.AbstractParticipant;
import cz.cesnet.shongo.controller.common.AbstractPerson;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link Specification} for an endpoint which can participate in a conference.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class EndpointParticipant extends AbstractParticipant
{
    /**
     * Set of {@link cz.cesnet.shongo.Technology}s which are required/supported by this {@link Specification}.
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
     * Update {@link #technologies} for this {@link Specification}.
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
     * @param id of the {@link cz.cesnet.shongo.controller.common.AbstractPerson}
     * @return {@link cz.cesnet.shongo.controller.common.AbstractPerson} with given {@code id}
     * @throws CommonReportSet.EntityNotFoundException
     *          when the {@link cz.cesnet.shongo.controller.common.AbstractPerson} doesn't exist
     */
    @Transient
    private AbstractPerson getPersonById(Long id) throws CommonReportSet.EntityNotFoundException
    {
        for (AbstractPerson person : persons) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return ControllerReportSetHelper.throwEntityNotFoundFault(AbstractPerson.class, id);
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
    @Column
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
    public AbstractParticipant clone()
    {
        AbstractParticipant participant = super.clone();
        updateTechnologies();
        return participant;
    }

    @Override
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        EndpointParticipant endpointParticipant = (EndpointParticipant) participant;

        boolean modified = super.synchronizeFrom(participant);
        modified |= !ObjectHelper.isSame(getTechnologies(), endpointParticipant.getTechnologies());
        modified |= !ObjectHelper.isSame(getCallInitiation(), endpointParticipant.getCallInitiation());

        setTechnologies(endpointParticipant.getTechnologies());
        setCallInitiation(endpointParticipant.getCallInitiation());

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
