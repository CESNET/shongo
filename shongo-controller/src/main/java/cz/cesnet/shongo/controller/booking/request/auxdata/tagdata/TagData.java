package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataFilter;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataMerged;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class TagData<T>
{

    protected final AuxDataMerged auxData;
    protected final T data;

    protected TagData(AuxDataMerged auxData)
    {
        this.auxData = auxData;
        this.data = constructData();
    }

    protected abstract T constructData();

    public static TagData<?> create(AuxDataMerged auxData)
    {
        switch (auxData.getType()) {
            case DEFAULT:
                return new DefaultAuxData(auxData);
            case NOTIFY_EMAIL:
                return new NotifyEmailAuxData(auxData);
            case RESERVATION_DATA:
                return new ReservationAuxData(auxData);
            default:
                throw new TodoImplementException("Not implemented for tag type: " + auxData.getType());
        }
    }

    public boolean filter(AuxDataFilter filter)
    {
        if (filter.getTagName() != null) {
            if (!filter.getTagName().equals(auxData.getTagName())) {
                return false;
            }
        }
        if (filter.getTagType() != null) {
            if (!filter.getTagType().equals(auxData.getType())) {
                return false;
            }
        }
        if (filter.getEnabled() != null) {
            if (!filter.getEnabled().equals(auxData.getEnabled())) {
                return false;
            }
        }
        return true;
    }
}
