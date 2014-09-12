package cz.cesnet.shongo.controller.booking.participant;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.ClassHelper;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a participant in a meeting.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractParticipant extends SimplePersistentObject implements Cloneable
{
    /**
     * Synchronize properties from given {@code specification}.
     *
     * @param participant from which will be copied all properties values to this {@link AbstractParticipant}
     * @return true if some modification was made, false otherwise
     */
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        return false;
    }

    /**
     * @return cloned instance of {@link AbstractParticipant}.
     */
    public AbstractParticipant clone() throws CloneNotSupportedException
    {
        AbstractParticipant specification = (AbstractParticipant) super.clone();
        specification.cloneReset();
        specification.synchronizeFrom(this);
        return specification;
    }

    /**
     * Perform reset for clone operation().
     */
    protected void cloneReset()
    {
        setIdNull();
    }

    /**
     * @return {@link AbstractParticipant} converted to {@link cz.cesnet.shongo.controller.api.AbstractParticipant}
     */
    public cz.cesnet.shongo.controller.api.AbstractParticipant toApi()
    {
        cz.cesnet.shongo.controller.api.AbstractParticipant api = createApi();
        toApi(api);
        return api;
    }

    /**
     * @param participantApi from which {@link AbstractParticipant} should be created
     * @return new instance of {@link AbstractParticipant} for given {@code api}
     */
    public static AbstractParticipant createFromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi,
            EntityManager entityManager)
    {
        Class<? extends AbstractParticipant> participantClass = getClassFromApi(participantApi.getClass());
        AbstractParticipant participant = ClassHelper.createInstanceFromClass(participantClass);
        participant.fromApi(participantApi, entityManager);
        return participant;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.AbstractParticipant}
     */
    protected abstract cz.cesnet.shongo.controller.api.AbstractParticipant createApi();

    /**
     * Synchronize to {@link cz.cesnet.shongo.controller.api.AbstractParticipant}.
     *
     * @param participantApi which should be filled from this {@link AbstractParticipant}
     */
    public void toApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi)
    {
        participantApi.setId(getId());
    }

    /**
     * Synchronize from {@link cz.cesnet.shongo.controller.api.AbstractParticipant}.
     *
     * @param participantApi from which this {@link AbstractParticipant} should be filled
     * @param entityManager
     */
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi, EntityManager entityManager)
    {
    }

    /**
     * {@link AbstractParticipant} class by {@link cz.cesnet.shongo.controller.api.AbstractParticipant} class.
     */
    private static final Map<
            Class<? extends cz.cesnet.shongo.controller.api.AbstractParticipant>,
            Class<? extends AbstractParticipant>> CLASS_BY_API = new HashMap<
            Class<? extends cz.cesnet.shongo.controller.api.AbstractParticipant>,
            Class<? extends AbstractParticipant>>();

    /**
     * Initialization for {@link #CLASS_BY_API}.
     */
    static {
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.PersonParticipant.class,
                PersonParticipant.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.InvitedPersonParticipant.class,
                InvitedPersonParticipant.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ExistingEndpointParticipant.class,
                ExistingEndpointParticipant.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ExternalEndpointParticipant.class,
                ExternalEndpointParticipant.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ExternalEndpointSetParticipant.class,
                ExternalEndpointSetParticipant.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.LookupEndpointParticipant.class,
                LookupEndpointParticipant.class);
    }

    /**
     * @param participantApiClass
     * @return {@link AbstractParticipant} for given {@code participantApiClass}
     */
    public static Class<? extends AbstractParticipant> getClassFromApi(
            Class<? extends cz.cesnet.shongo.controller.api.AbstractParticipant> participantApiClass)
    {
        Class<? extends AbstractParticipant> participantClass = CLASS_BY_API.get(participantApiClass);
        if (participantClass == null) {
            throw new TodoImplementException(participantApiClass);
        }
        return participantClass;
    }
}
