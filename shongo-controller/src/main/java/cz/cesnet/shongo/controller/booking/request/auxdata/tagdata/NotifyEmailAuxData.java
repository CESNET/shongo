package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxData;
import cz.cesnet.shongo.controller.booking.resource.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NotifyEmailAuxData extends TagData<List<String>>
{

    public NotifyEmailAuxData(Tag tag, AuxData auxData)
    {
        super(tag, auxData);
    }

    @Override
    public List<String> getData()
    {
        List<String> tagDataEmails;
        List<String> auxDataEmails;

        try {
            tagDataEmails = objectMapper.readValue(tag.getData(), new TypeReference<>() {
            });
        } catch (JsonProcessingException | NullPointerException e) {
            logger.warn("Error while parsing tag data: {}", e.getMessage());
            tagDataEmails = new ArrayList<>();
        }
        try {
            auxDataEmails = objectMapper.readValue(aux.getData().toString(), new TypeReference<>() {
            });
        } catch (JsonProcessingException | NullPointerException e) {
            logger.warn("Error while parsing aux data: {}", e.getMessage());
            auxDataEmails = new ArrayList<>();
        }

        return Stream
                .concat(tagDataEmails.stream(), auxDataEmails.stream())
                .collect(Collectors.toList());
    }
}
