package cz.cesnet.shongo.controller.booking.request.auxdata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.auxdata.tagdata.TagData;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.resource.ResourceSpecification;
import cz.cesnet.shongo.controller.booking.resource.Tag;
import cz.cesnet.shongo.controller.booking.room.RoomSpecification;
import cz.cesnet.shongo.controller.booking.specification.Specification;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuxDataService
{

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T extends TagData<?>> List<T> getTagData(
            AbstractReservationRequest reservationRequest,
            AuxDataFilter filter, EntityManager entityManager
    ) throws AuxDataException, JsonProcessingException
    {
        List<TagData<?>> tagData = getAllTagData(reservationRequest, entityManager);

        return tagData
                .stream()
                .filter(data -> data.filter(filter))
                .map(data -> (T) data)
                .collect(Collectors.toList());
    }

    private static List<TagData<?>> getAllTagData(
            AbstractReservationRequest reservationRequest,
            EntityManager entityManager
    ) throws AuxDataException, JsonProcessingException
    {
        if (reservationRequest.getAuxData() == null) {
            throw new AuxDataException("AuxData is null");
        }

        List<AuxData> auxData = objectMapper.readValue(reservationRequest.getAuxData(), new TypeReference<>() {});

        Resource resource = getResource(reservationRequest);
        List<Tag> resourceTags = new ResourceManager(entityManager).getResourceTags(resource);

        List<TagData<?>> tagData = new ArrayList<>();
        for (Tag tag : resourceTags) {
            for (AuxData auxData1 : auxData) {
                if (auxData1.getTagName().equals(tag.getName())) {
                    tagData.add(TagData.create(tag, auxData1));
                }
            }
        }
        return tagData;
    }

    private static Resource getResource(AbstractReservationRequest reservationRequest)
    {
        Specification specification = reservationRequest.getSpecification();
        if (specification instanceof ResourceSpecification) {
            return ((ResourceSpecification) specification).getResource();
        }
        else if (specification instanceof RoomSpecification) {
            return ((RoomSpecification) specification).getDeviceResource();
        }
        else {
            throw new TodoImplementException(specification.getClass());
        }
    }
}
