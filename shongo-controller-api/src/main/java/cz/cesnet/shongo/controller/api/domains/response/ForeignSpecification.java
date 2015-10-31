package cz.cesnet.shongo.controller.api.domains.response;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;

/**
 * Represents a specification for foreign {@link Reservation}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
@JsonSubTypes({
        @Type(value = ResourceSpecification.class, name = "resourceSpecification"),
        @Type(value = RoomSpecification.class, name = "roomSpecification") })
public abstract class ForeignSpecification
{
}
