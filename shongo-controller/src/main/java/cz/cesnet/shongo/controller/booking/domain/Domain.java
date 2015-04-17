package cz.cesnet.shongo.controller.booking.domain;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import javax.persistence.Column;
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

    private String url;

    private int port;

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH, unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH, unique = true)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

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
        domainApi.setName(name);
        domainApi.setCode(code);
        domainApi.setOrganization(organization);
        domainApi.setDeviceAddress(new DeviceAddress(url, port));
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
        this.setOrganization(domainApi.getOrganization());
        this.setName(domainApi.getName());
        this.setCode(domainApi.getCode());
        this.setUrl(domainApi.getDeviceAddress().getUrl());
        this.setPort(domainApi.getDeviceAddress().getPort());
    }
}
