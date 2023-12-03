package cz.cesnet.shongo.controller.booking.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Immutable
@Table(name = "arr_aux_data")
public class AbstractReservationRequestAuxData extends AbstractReservationRequest
{

    private String tagName;
    private Boolean enabled;
    private JsonNode data;

    @Type(type = "jsonb")
    @Column(columnDefinition = "text")
    public JsonNode getData() {
        return data;
    }

    @Override
    public AbstractReservationRequest clone(EntityManager entityManager) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi() {
        throw new RuntimeException("Not implemented");
    }
}
