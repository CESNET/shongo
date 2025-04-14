package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cesnet.shongo.controller.api.TagType;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataMerged;

import java.util.Map;

public class ReservationAuxData extends TagData<Map<String, Object>>
{

    public ReservationAuxData(AuxDataMerged auxData)
    {
        super(auxData);
        if (!TagType.RESERVATION_DATA.equals(auxData.getType())) {
            throw new IllegalArgumentException("AuxData is not of type RESERVATION_DATA");
        }
    }

    @Override
    protected Map<String, Object> constructData()
    {
        Map<String, Object> tagMap = new ObjectMapper().convertValue(auxData.getData(), new TypeReference<>() {});
        Map<String, Object> auxMap = new ObjectMapper().convertValue(auxData.getAuxData(), new TypeReference<>() {});
        tagMap.putAll(auxMap);
        return tagMap;
    }
}
