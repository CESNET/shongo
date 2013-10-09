package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;

/**
 * Represents a {@link AbstractParticipant} defined by a {@link AbstractPerson}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PersonParticipant extends AbstractParticipant
{
    /**
     * @see AbstractPerson
     */
    private AbstractPerson person;

    /**
     * Each {@link AbstractParticipant} acts in a meeting in a {@link cz.cesnet.shongo.ParticipantRole}.
     */
    private ParticipantRole participantRole;

    /**
     * @return {@link #person}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public AbstractPerson getPerson()
    {
        return person;
    }

    /**
     * @param person sets the {@link #person}
     */
    public void setPerson(AbstractPerson person)
    {
        this.person = person;
    }

    /**
     * @return {@link #participantRole}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public ParticipantRole getParticipantRole()
    {
        return participantRole;
    }

    /**
     * @param participantRole sets the {@link #participantRole}
     */
    public void setParticipantRole(ParticipantRole participantRole)
    {
        this.participantRole = participantRole;
    }

    /**
     * @return {@link AbstractPerson#getInformation()}
     */
    @Transient
    public PersonInformation getPersonInformation()
    {
        if (person == null) {
            return null;
        }
        return person.getInformation();
    }

    @Override
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        PersonParticipant personParticipant = (PersonParticipant) participant;

        boolean modified = super.synchronizeFrom(participant);
        modified |= !ObjectHelper.isSame(getPerson(), personParticipant.getPerson());

        setPerson(personParticipant.getPerson().clone());

        return modified;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractParticipant createApi()
    {
        return new cz.cesnet.shongo.controller.api.PersonParticipant();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi)
    {
        cz.cesnet.shongo.controller.api.PersonParticipant personParticipant =
                (cz.cesnet.shongo.controller.api.PersonParticipant) participantApi;
        personParticipant.setPerson(getPerson().toApi());
        super.toApi(participantApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.PersonParticipant personParticipantApi =
                (cz.cesnet.shongo.controller.api.PersonParticipant) participantApi;

        cz.cesnet.shongo.controller.api.AbstractPerson personApi = personParticipantApi.getPerson();
        if (personApi == null) {
            throw new IllegalArgumentException("Person must not be null.");
        }
        else if (getPerson() != null && getPerson().getId().equals(personApi.notNullIdAsLong())) {
            getPerson().fromApi(personApi);
        }
        else {
            AbstractPerson person = AbstractPerson.createFromApi(personApi);
            setPerson(person);
        }

        super.fromApi(participantApi, entityManager);
    }
}
