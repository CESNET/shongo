package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.value.ValueProviderCapability;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a capability that a resource can have.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Capability extends SimplePersistentObject
{
    /**
     * Resource to which the capability is applied.
     */
    private Resource resource;

    /**
     * @return {@link #resource}
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Access(AccessType.FIELD)
    public Resource getResource()
    {
        return getLazyImplementation(resource);
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        // Manage bidirectional association
        if (resource != this.resource) {
            if (this.resource != null) {
                Resource oldResource = this.resource;
                this.resource = null;
                oldResource.removeCapability(this);
            }
            if (resource != null) {
                this.resource = resource;
                this.resource.addCapability(this);
            }
        }
    }

    /**
     * @param referenceDateTime reference date/time used e.g., as base date/time for relative date/time
     * @return {@link DateTime} representing a maximum future fot the {@link Resource}
     */
    public DateTime getMaximumFutureDateTime(DateTime referenceDateTime)
    {
        return resource.getMaximumFutureDateTime(referenceDateTime);
    }

    /**
     * @param dateTime          date/time which is checked for availability
     * @param referenceDateTime reference date/time used e.g., as base date/time for relative date/time
     * @return true if the {@link Capability} (or the {@link #resource}) is available at given {@code dateTime},
     *         false otherwise
     */
    public final boolean isAvailableInFuture(DateTime dateTime, DateTime referenceDateTime)
    {
        DateTime maxDateTime = getMaximumFutureDateTime(referenceDateTime);
        return maxDateTime == null || !dateTime.isAfter(maxDateTime);
    }

    /**
     * @return converted capability to API
     */
    public final cz.cesnet.shongo.controller.api.Capability toApi()
    {
        cz.cesnet.shongo.controller.api.Capability api = createApi();
        toApi(api);
        return api;
    }

    /**
     * @param api
     * @param entityManager
     * @return new instance of {@link Capability} from API
     */
    public static Capability createFromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
    {
        Capability capability;
        if (api instanceof cz.cesnet.shongo.controller.api.RoomProviderCapability) {
            capability = new RoomProviderCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.StandaloneTerminalCapability) {
            capability = new StandaloneTerminalCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.TerminalCapability) {
            capability = new TerminalCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ValueProviderCapability) {
            capability = new ValueProviderCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.AliasProviderCapability) {
            capability = new AliasProviderCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.RecordingCapability) {
            capability = new RecordingCapability();
        }
        else {
            throw new TodoImplementException(api.getClass());
        }
        capability.fromApi(api, entityManager);
        return capability;
    }

    /**
     * @return new instance of API capability
     */
    protected abstract cz.cesnet.shongo.controller.api.Capability createApi();

    /**
     * @param api API capability to be filled
     */
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        api.setId(getId());
    }

    /**
     * Synchronize capability from API
     *
     * @param api
     * @param entityManager
     */
    protected void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
    {
    }

    /**
     * {@link Capability} class by {@link cz.cesnet.shongo.controller.api.Capability} class.
     */
    private static final Map<
            Class<? extends cz.cesnet.shongo.controller.api.Capability>,
            Class<? extends Capability>> CLASS_BY_API = new HashMap<
            Class<? extends cz.cesnet.shongo.controller.api.Capability>,
            Class<? extends Capability>>();

    /**
     * Initialization for {@link #CLASS_BY_API}.
     */
    static {
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.RoomProviderCapability.class,
                RoomProviderCapability.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.TerminalCapability.class,
                TerminalCapability.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.StandaloneTerminalCapability.class,
                StandaloneTerminalCapability.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ValueProviderCapability.class,
                ValueProviderCapability.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.AliasProviderCapability.class,
                AliasProviderCapability.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.RecordingCapability.class,
                RecordingCapability.class);
    }

    /**
     * @param capabilityApiClass
     * @return {@link Capability} for given {@code capabilityApiClass}
     */
    public static Class<? extends Capability> getClassFromApi(
            Class<? extends cz.cesnet.shongo.controller.api.Capability> capabilityApiClass)
    {
        Class<? extends Capability> capabilityClass = CLASS_BY_API.get(capabilityApiClass);
        if (capabilityClass == null) {
            throw new TodoImplementException(capabilityApiClass);
        }
        return capabilityClass;
    }
}
