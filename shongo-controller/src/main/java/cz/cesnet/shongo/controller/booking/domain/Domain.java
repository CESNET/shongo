package cz.cesnet.shongo.controller.booking.domain;

import com.google.common.base.Strings;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.resource.Capability;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Set;

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
    private String shortName;

    /**
     * Represents a user-visible domain organization (e.g., "CESNET, z.s.p.o.").
     */
    private String organization;

    private String url;

    private int port;

    private String certificatePath;

    private String passwordHash;

    /**
     * If foreign domain uses the same authorization server or shares user consolidator.
     *
     * Use when all users have the same ids in both domains.
     */
    private boolean shareAuthorizationServer = false;

    /**
     * Is this domain used for local allocations.
     * Allocatable domains are also cached. See {@link cz.cesnet.shongo.controller.domains.CachedDomainsConnector}
     */
    private boolean allocatable;

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH, unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH, unique = true)
    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
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

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getCertificatePath() {
        return certificatePath;
    }

    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }

    @Column(nullable = false)
    public boolean isAllocatable() {
        return allocatable;
    }

    public void setAllocatable(boolean allocatable) {
        this.allocatable = allocatable;
    }

    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isShareAuthorizationServer()
    {
        return shareAuthorizationServer;
    }

    public void setShareAuthorizationServer(boolean shareAuthorizationServer)
    {
        this.shareAuthorizationServer = shareAuthorizationServer;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * @return domain converted capability to API
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
        domainApi.setShortName(shortName);
        domainApi.setOrganization(organization);
        domainApi.setDomainAddress(new DeviceAddress(url, port));
        domainApi.setCertificatePath(certificatePath);
        domainApi.setAllocatable(allocatable);
        domainApi.setPasswordHash(passwordHash);
        domainApi.setShareAuthorizationServer(shareAuthorizationServer);
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
        this.setShortName(domainApi.getShortName());
        this.setUrl(domainApi.getDomainAddress().getUrl());
        this.setPort(domainApi.getDomainAddress().getPort());
        this.setCertificatePath(domainApi.getCertificatePath());
        this.setAllocatable(domainApi.isAllocatable());
        this.setPasswordHash(domainApi.getPasswordHash());
        this.setShareAuthorizationServer(domainApi.isShareAuthorizationServer());
    }

    /**
     * Validate resource.
     *
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectInvalidException
     *
     */
    public void validate() throws CommonReportSet.ObjectInvalidException
    {
        if (Strings.isNullOrEmpty(name)) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Domain cannot have empty name.");
        }
        if (Strings.isNullOrEmpty(url)) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Domain cannot have empty url.");
        }
        if (port == 0) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Domain cannot have empty port 0.");
        }
        if (Strings.isNullOrEmpty(certificatePath) && Strings.isNullOrEmpty(passwordHash)) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Domain cannot have empty certificate path and password hash.");
        }

    }
}
