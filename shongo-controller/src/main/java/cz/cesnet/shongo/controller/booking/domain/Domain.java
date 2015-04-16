package cz.cesnet.shongo.controller.booking.domain;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import javax.persistence.Entity;

/**
 * Represents foreign domain with local shareable resources/capabilities.
 *
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class Domain extends SimplePersistentObject {
    /**
     * Represents an unique domain name (e.g., "cz.cesnet")
     */
    private String name;

    /**
     * Represents shorten version of {@link #name} (e.g., used in description of virtual rooms)
     */
    private String code;

    /**
     * Represents a user-visible domain organization (e.g., "CESNET, z.s.p.o.").
     */
    private String organization;

    /**
     * Status of the domain.
     */
    private cz.cesnet.shongo.controller.api.Domain.Status status;

    /**
     * @return tag converted capability to API
     */
    public final cz.cesnet.shongo.controller.api.Domain toApi()
    {
        cz.cesnet.shongo.controller.api.Domain domainApi = new cz.cesnet.shongo.controller.api.Domain();
        toApi(domainApi);
        return domainApi;
    }

    public void toApi(cz.cesnet.shongo.controller.api.Domain domainApi)
    {
        domainApi.setId(ObjectIdentifier.formatId(this));
//        domainApi.setName(name);
    }

    /**
     * @param domainApi
     * @return domain converted from API
     */
    public static Domain createFromApi(cz.cesnet.shongo.controller.api.Domain domainApi)
    {
        Domain domain = new Domain();
        domain.fromApi(domainApi);
        return domain;
    }

    public void fromApi(cz.cesnet.shongo.controller.api.Domain domainApi)
    {
//        this.setName(tagApi.getName());
    }
}
