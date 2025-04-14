package cz.cesnet.shongo.controller.booking.request;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.controller.booking.specification.Specification;
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
public class AbstractReservationRequestAuxData
{

    @Id
    private Long id;

    private String tagName;
    private Boolean enabled;
    @Type(type = "jsonb")
    @Column(columnDefinition = "text")
    private JsonNode data;
    @ManyToOne(cascade = CascadeType.ALL, optional = false, fetch = FetchType.LAZY)
    private Specification specification;
}
