package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * Represents a setting for a {@link RoomConfiguration}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class RoomSetting extends PersistentObject
{
    /**
     * @return {@link RoomSetting} converted to {@link cz.cesnet.shongo.api.RoomSetting}
     */
    public cz.cesnet.shongo.api.RoomSetting toApi()
    {
        cz.cesnet.shongo.api.RoomSetting api = createApi();
        toApi(api);
        return api;
    }

    /**
     * @param api from which {@link RoomSetting} should be created
     * @return new instance of {@link RoomSetting} for given {@code api}
     */
    public static RoomSetting createFromApi(cz.cesnet.shongo.api.RoomSetting api)
    {
        RoomSetting roomSetting = null;
        if (api instanceof cz.cesnet.shongo.api.RoomSetting.H323) {
            roomSetting = new H323RoomSetting();
        }
        else {
            throw new TodoImplementException(api.getClass().getCanonicalName());
        }
        roomSetting.fromApi(api);
        return roomSetting;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.api.RoomSetting}
     */
    protected abstract cz.cesnet.shongo.api.RoomSetting createApi();

    /**
     * Synchronize to {@link cz.cesnet.shongo.api.RoomSetting}.
     *
     * @param roomSettingApi which should be filled from this {@link RoomSetting}
     */
    public void toApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        roomSettingApi.setIdentifier(getId());
    }

    /**
     * Synchronize from {@link cz.cesnet.shongo.api.RoomSetting}.
     *
     * @param roomSettingApi from which this {@link RoomSetting} should be filled
     */
    public void fromApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
    }

    /**
     * @return cloned instance
     */
    public abstract RoomSetting clone();
}
