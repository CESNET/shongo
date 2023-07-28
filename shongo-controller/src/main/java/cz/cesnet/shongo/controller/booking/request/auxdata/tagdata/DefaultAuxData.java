package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataMerged;

public class DefaultAuxData extends TagData<Void>
{

    public DefaultAuxData(AuxDataMerged auxData)
    {
        super(auxData);
    }

    @Override
    public Void getData()
    {
        return null;
    }
}
