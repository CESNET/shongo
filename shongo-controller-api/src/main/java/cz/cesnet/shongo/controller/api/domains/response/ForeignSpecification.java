package cz.cesnet.shongo.controller.api.domains.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Represents a specification for foreign {@link Reservation}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ResourceSpecification.class, name = "ResourceSpecification"),
        @JsonSubTypes.Type(value = RoomSpecification.class, name = "RoomSpecification") })
public abstract class ForeignSpecification
{
}
