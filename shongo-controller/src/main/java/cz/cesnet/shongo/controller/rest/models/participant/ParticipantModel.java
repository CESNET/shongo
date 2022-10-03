package cz.cesnet.shongo.controller.rest.models.participant;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import cz.cesnet.shongo.controller.rest.error.UnsupportedApiException;
import cz.cesnet.shongo.controller.rest.models.CommonModel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for {@link AbstractParticipant}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Filip Karnis
 */
@Data
@NoArgsConstructor
public class ParticipantModel
{

    protected String id;

    private Type type;

    private String userId;

    private String name;

    private String email;

    private ParticipantRole role;

    private String organization;

    public ParticipantModel(UserInformation userInformation)
    {
        this.type = Type.USER;
        setUserId(userInformation.getUserId());
    }

    public ParticipantModel(AbstractParticipant participant, CacheProvider cacheProvider)
    {
        this.id = participant.getId();
        if (participant instanceof PersonParticipant) {
            PersonParticipant personParticipant = (PersonParticipant) participant;
            this.role = personParticipant.getRole();
            AbstractPerson person = personParticipant.getPerson();
            if (person instanceof UserPerson) {
                UserPerson userPerson = (UserPerson) person;
                setType(Type.USER);
                setUserId(userPerson.getUserId());
                UserInformation userInformation = cacheProvider.getUserInformation(userPerson.getUserId());
                setName(userInformation.getFullName());
                setEmail(userInformation.getEmail());
                setOrganization(userInformation.getOrganization());
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
                if (userId == null) {
                    throw new IllegalStateException("User must not be null.");
                }
                userPerson.setUserId(userId);
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

    public void setNewId()
    {
        this.id = CommonModel.getNewId();
    }

    public void setNullId()
    {
        this.id = null;
    }

    public enum Type
    {
        USER,
        ANONYMOUS
    }
}
