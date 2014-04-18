package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.api.DataMap;

/**
 * {@link AbstractParticipant} for {@link AbstractPerson}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersonParticipant extends AbstractParticipant
{
    /**
     * The requested person.
     */
    private AbstractPerson person;

    /**
     * Each {@link AbstractParticipant} acts in a meeting in a {@link cz.cesnet.shongo.ParticipantRole}.
     */
    private ParticipantRole role;

    /**
     * Constructor.
     */
    public PersonParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param person sets the {@link #person}
     */
    public PersonParticipant(AbstractPerson person)
    {
        setPerson(person);
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link AnonymousPerson#name} for the {@link #person}
     * @param email sets the {@link AnonymousPerson#email} for the {@link #person}
     */
    public PersonParticipant(String name, String email)
    {
        setPerson(new AnonymousPerson(name, email));
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link AnonymousPerson#name} for the {@link #person}
     * @param email sets the {@link AnonymousPerson#email} for the {@link #person}
     * @param role sets the {@link #role}
     */
    public PersonParticipant(String name, String email, ParticipantRole role)
    {
        setPerson(new AnonymousPerson(name, email));
        setRole(role);
    }

    /**
     * @return {@link #PERSON}
     */
    public AbstractPerson getPerson()
    {
        return person;
    }

    /**
     * @param person sets the {@link #PERSON}
     */
    public void setPerson(AbstractPerson person)
    {
        this.person = person;
    }

    /**
     * @return {@link #role}
     */
    public ParticipantRole getRole()
    {
        return role;
    }

    /**
     * @param role sets the {@link #role}
     */
    public void setRole(ParticipantRole role)
    {
        this.role = role;
    }

    public static final String PERSON = "person";
    public static final String ROLE = "role";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PERSON, person);
        dataMap.set(ROLE, role);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        person = dataMap.getComplexTypeRequired(PERSON, AbstractPerson.class);
        role = dataMap.getEnum(ROLE, ParticipantRole.class);
    }
}
