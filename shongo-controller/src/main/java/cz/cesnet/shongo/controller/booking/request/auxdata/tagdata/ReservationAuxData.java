package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataMerged;

public class ReservationAuxData extends TagData<String>
{

    public ReservationAuxData(AuxDataMerged auxData)
    {
        super(auxData);
    }

    @Override
    public String getData()
    {
        throw new TodoImplementException("Not implemented");
    }
}
