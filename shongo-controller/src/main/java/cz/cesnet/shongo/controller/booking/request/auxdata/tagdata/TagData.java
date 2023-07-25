package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxData;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataFilter;
import cz.cesnet.shongo.controller.booking.resource.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@RequiredArgsConstructor
public abstract class TagData<T>
{

    protected final Tag tag;
    protected final AuxData aux;

    public abstract T getData();

    public static TagData<?> create(Tag tag, AuxData auxData)
    {
        switch (tag.getType()) {
            case NOTIFY_EMAIL:
                return new NotifyEmailAuxData(tag, auxData);
            case RESERVATION_DATA:
                return new ReservationAuxData(tag, auxData);
            default:
                throw new TodoImplementException("Not implemented for tag type: " + tag.getType());
        }
    }

    public boolean filter(AuxDataFilter filter)
    {
        if (filter.getTagName() != null) {
            if (!filter.getTagName().equals(tag.getName())) {
                return false;
            }
        }
        if (filter.getTagType() != null) {
            if (!filter.getTagType().equals(tag.getType())) {
                return false;
            }
        }
        if (filter.getEnabled() != null) {
            if (!filter.getEnabled().equals(aux.isEnabled())) {
                return false;
            }
        }
        return true;
    }
}
