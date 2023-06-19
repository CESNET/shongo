package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxData;
import cz.cesnet.shongo.controller.booking.resource.Tag;

public class ReservationAuxData extends TagData<String>
{

    public ReservationAuxData(Tag tag, AuxData auxData)
    {
        super(tag, auxData);
    }

    @Override
    public String getData()
    {
        throw new TodoImplementException("Not implemented");
    }
}
