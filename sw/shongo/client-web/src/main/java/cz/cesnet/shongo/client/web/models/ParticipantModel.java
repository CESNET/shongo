package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.controller.api.*;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model for {@link AbstractParticipant}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ParticipantModel implements ReportModel.ContextSerializable
{

    private CacheProvider cacheProvider;

    private String id;

    private Type type;

    private UserInformation user;

    private String name;

    private String email;

    private ParticipantRole role;

    public ParticipantModel(CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        this.type = Type.USER;
    }

    public ParticipantModel(AbstractParticipant participant, CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        this.id = participant.getId();
        if (participant instanceof PersonParticipant) {
            PersonParticipant personParticipant = (PersonParticipant) participant;
            this.role = personParticipant.getRole();
            AbstractPerson person = personParticipant.getPerson();
            if (person instanceof UserPerson) {
                UserPerson userPerson = (UserPerson) person;
                setType(Type.USER);
                setUserId(userPerson.getUserId());
            }
            else if (person instanceof AnonymousPerson) {
                AnonymousPerson anonymousPerson = (AnonymousPerson) person;
                type = Type.USER;
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
            case USER:
            {
                UserPerson userPerson = new UserPerson();
                if (user == null) {
                    throw new IllegalStateException("User must not be null.");
                }
                userPerson.setUserId(user.getUserId());
                personParticipant.setPerson(userPerson);
                return personParticipant;
            }
            case ANONYMOUS:
            {
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

    public void validate(BindingResult bindingResult)
    {
        ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "type", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "role", "validation.field.required");
        if (type != null) {
            switch (type) {
                case USER:
                    ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "userId", "validation.field.required");
                    break;
                case ANONYMOUS:
                    ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "name", "validation.field.required");
                    CommonModel.validateEmail(bindingResult, "email", "validation.field.invalidEmail");
                    break;
                default:
                    throw new TodoImplementException(type);
            }
        }
    }

    public boolean isNew()
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
            throw new IllegalStateException("UserInformationProvider isn't set.");
        }
        setUser(cacheProvider.getUserInformation(userId));
    }

    public UserInformation getUser()
    {
        return user;
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

    public ParticipantRole getRole()
    {
        return role;
    }

    public void setRole(ParticipantRole role)
    {
        this.role = role;
    }

    @Override
    public String toContextString()
    {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("Type", type);
        attributes.put("User ID", user != null ? user.getUserId() : null);
        attributes.put("Name", name);
        attributes.put("Email", email);
        attributes.put("Role", role);
        return ReportModel.formatAttributes(attributes);
    }

    public static enum Type
    {
        USER,
        ANONYMOUS
    }

    /**
     * @param participantContainer
     * @param participantId
     * @return {@link ParticipantModel} with given {@code participantId} from given {@code participantContainer}
     */
    public static ParticipantModel getParticipant(ParticipantContainer participantContainer, String participantId)
    {
        ParticipantModel participant = null;
        for (ParticipantModel possibleParticipant : participantContainer.getParticipants()) {
            if (possibleParticipant.getId().equals(participantId)) {
                participant = possibleParticipant;
            }
        }
        if (participant == null) {
            throw new IllegalArgumentException("Participant " + participantId + " doesn't exist.");
        }
        return participant;
    }

    /**
     * Add new participant.
     *
     * @param participantContainer
     * @param participant
     * @param participantBindingResult
     */
    public static boolean createParticipant(ParticipantContainer participantContainer, ParticipantModel participant,
            BindingResult participantBindingResult)
    {
        participant.validate(participantBindingResult);
        if (participantBindingResult.hasErrors()) {
            return false;
        }
        participant.setNewId();
        participantContainer.addParticipant(participant);
        return true;
    }

    /**
     * Modify existing participant
     *
     * @param participantContainer
     * @param participantId
     * @param participant
     * @param participantBindingResult
     */
    public static boolean modifyParticipant(ParticipantContainer participantContainer, String participantId,
            ParticipantModel participant, BindingResult participantBindingResult)
    {
        participant.validate(participantBindingResult);
        if (participantBindingResult.hasErrors()) {
            return false;
        }
        ParticipantModel oldParticipant = getParticipant(participantContainer, participantId);
        participantContainer.removeParticipant(oldParticipant);
        participantContainer.addParticipant(participant);
        return true;
    }

    /**
     * Delete existing participant.
     *
     * @param participantContainer
     * @param participantId
     */
    public static void deleteParticipant(ParticipantContainer participantContainer, String participantId)
    {
        ParticipantModel participant = getParticipant(participantContainer, participantId);
        participantContainer.removeParticipant(participant);
    }
}
