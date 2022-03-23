package cz.cesnet.shongo.controller.rest.models.detail;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import cz.cesnet.shongo.controller.rest.models.CommonModel;
import cz.cesnet.shongo.controller.rest.models.UnsupportedApiException;

/**
 * Model for {@link AbstractParticipant}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Filip Karnis
 */
public class ParticipantModel //implements ReportModel.ContextSerializable
{
    private final CacheProvider cacheProvider;

    protected String id;

    private Type type;

    private UserInformation user;

    private String name;

    private String email;

    private ParticipantRole role;

    private String organization;

    public ParticipantModel(CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        this.type = Type.USER;
    }

    public ParticipantModel(UserInformation userInformation, CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        this.type = Type.USER;
        setUser(userInformation);
    }

    public ParticipantModel(AbstractParticipant participant, CacheProvider cacheProvider)
    {
        this.id = participant.getId();
        this.cacheProvider = cacheProvider;
        if (participant instanceof PersonParticipant) {
            PersonParticipant personParticipant = (PersonParticipant) participant;
            this.role = personParticipant.getRole();
            AbstractPerson person = personParticipant.getPerson();
            if (person instanceof UserPerson) {
                UserPerson userPerson = (UserPerson) person;
                setType(Type.USER);
                setUserId(userPerson.getUserId());
/*                setName(userPerson.getName());
                setEmail(userPerson.getEmail());
                setOrganization(userPerson.getOrganization());*/
            }
            else if (person instanceof AnonymousPerson) {
                AnonymousPerson anonymousPerson = (AnonymousPerson) person;
                type = Type.ANONYMOUS;
                name = anonymousPerson.getName();
                email = anonymousPerson.getEmail();
            }
            else {
                throw new UnsupportedApiException(person.getClass());
            }
        }
        else {
            throw new UnsupportedApiException(participant.getClass());
        }
    }

    public AbstractParticipant toApi()
    {
        PersonParticipant personParticipant = new PersonParticipant();
        if (!isNew()) {
            personParticipant.setId(id);
        }
        personParticipant.setRole(role);
        switch (type) {
            case USER: {
                UserPerson userPerson = new UserPerson();
                if (user == null) {
                    throw new IllegalStateException("User must not be null.");
                }
                userPerson.setUserId(user.getUserId());
/*                userPerson.setName(name);
                userPerson.setEmail(email);
                userPerson.setOrganization(organization);*/
                personParticipant.setPerson(userPerson);
                return personParticipant;
            }
            case ANONYMOUS: {
                AnonymousPerson anonymousPerson = new AnonymousPerson();
                anonymousPerson.setName(name);
                anonymousPerson.setEmail(email);
                personParticipant.setPerson(anonymousPerson);
                return personParticipant;
            }
            default:
                throw new TodoImplementException(type);
        }
    }

    private boolean isNew()
    {
        return id == null || CommonModel.isNewId(id);
    }

    public String getId()
    {
        return id;
    }

    public void setNewId()
    {
        this.id = CommonModel.getNewId();
    }

    public void setNullId()
    {
        this.id = null;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public String getUserId()
    {
        if (user == null) {
            return null;
        }
        return user.getUserId();
    }

    public void setUserId(String userId)
    {
        if (userId == null || userId.isEmpty()) {
            setUser(null);
            return;
        }
        if (cacheProvider == null) {
            throw new IllegalStateException(CacheProvider.class + " isn't set.");
        }
        setUser(cacheProvider.getUserInformation(userId));
    }

    public void setUser(UserInformation user)
    {
        this.user = user;
    }

    public String getName()
    {
        if (user != null) {
            return user.getFullName();
        }
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getEmail()
    {
        if (user != null) {
            return user.getPrimaryEmail();
        }
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getOrganization() {
        if (user != null) {
            return user.getOrganization();
        }
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public ParticipantRole getRole()
    {
        return role;
    }

    public void setRole(ParticipantRole role)
    {
        this.role = role;
    }

//    @Override
//    public String toContextString()
//    {
//        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
//        attributes.put("ID", id);
//        attributes.put("Type", type);
//        attributes.put("User ID", user != null ? user.getUserId() : null);
//        attributes.put("Name", name);
//        attributes.put("Email", email);
//        attributes.put("Role", role);
//        attributes.put("Organization", organization);
//        return ReportModel.formatAttributes(attributes);
//    }

//    @Override
//    public String toString()
//    {
//        switch (type) {
//            case USER:
//                return String.format("UserParticipant(userId: %s, role: %s)", getUserId(), role);
//            default:
//                return String.format("Participant(type: %s, role: %s)", type, role);
//        }
//    }

    public enum Type
    {
        USER,
        ANONYMOUS
    }
}
