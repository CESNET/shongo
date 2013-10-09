package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.*;
import net.tanesha.recaptcha.ReCaptchaResponse;
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
    private String id;

    private Type type;

    private String userId;

    private String name;

    private String email;

    private ParticipantRole role;

    public ParticipantModel()
    {
    }
    public ParticipantModel(AbstractParticipant participant)
    {
        this.id = participant.getId();
        if (participant instanceof PersonParticipant) {
            PersonParticipant personParticipant = (PersonParticipant) participant;
            AbstractPerson person = personParticipant.getPerson();
            if (person instanceof UserPerson) {
                UserPerson userPerson = (UserPerson) person;
                type = Type.USER;
                userId = userPerson.getUserId();
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
        switch (type) {
            case USER:
            {
                UserPerson userPerson = new UserPerson();
                userPerson.setUserId(userId);
                PersonParticipant personParticipant = new PersonParticipant();;
                personParticipant.setPerson(userPerson);
                personParticipant.setRole(role);
                return personParticipant;
            }
            case ANONYMOUS:
            {
                AnonymousPerson anonymousPerson = new AnonymousPerson();
                anonymousPerson.setName(name);
                anonymousPerson.setEmail(email);
                PersonParticipant personParticipant = new PersonParticipant();;
                personParticipant.setPerson(anonymousPerson);
                personParticipant.setRole(role);
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
                    ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "email", "validation.field.required");
                    break;
                default:
                    throw new TodoImplementException(type);
            }
        }
    }

    public String getId()
    {
        return id;
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
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getEmail()
    {
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
        attributes.put("User ID", userId);
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
}
