package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import org.codehaus.jackson.map.ObjectReader;
import org.eclipse.jetty.util.ConcurrentHashSet;

import javax.persistence.EntityManagerFactory;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cached foreign domains connector for Inter Domain Agent
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class CachedDomainsConnector extends DomainsConnector
{
    /**
     * Cache of available resources mapped by domains codes
     */
    private ConcurrentMap<String, List<DomainCapability>> availableResources = new ConcurrentHashMap<>();

    /**
     * Cache of unavailable domains, supplement to {#code availableResources} on foreign domains
     */
    private ConcurrentHashSet<String> unavailableResources = new ConcurrentHashSet<>();

    public CachedDomainsConnector(EntityManagerFactory entityManagerFactory, ControllerConfiguration configuration, EmailSender emailSender)
    {
        super(entityManagerFactory, configuration, emailSender);
        initResourceCache();
    }

    /**
     * Start periodic cache of foreign domain's resources (type of RESOURCE)
     */
    private void initResourceCache()
    {
        // Init only for inactive domains
        List<Domain> domains = new ArrayList<>();
        for (Domain domain : listForeignDomains()) {
            if (!availableResources.containsKey(domain.getName()) && !unavailableResources.contains(domain.getName())) {
                domains.add(domain);
            }
        }
        Map<String, String> parameters = new HashMap<>();
        parameters.put("type", DomainCapabilityListRequest.Type.RESOURCE.toString());
        submitCachedTypedListRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_CAPABILITY_LIST, parameters,
                domains, DomainCapability.class, availableResources, unavailableResources);
    }

    protected <T> Map<String, List<T>> submitCachedTypedListRequest(final InterDomainAction.HttpMethod method, final String action,
                                                                    final Map<String, String> parameters, final Collection<Domain> domains,
                                                                    Class<T> objectClass, ConcurrentMap cache, ConcurrentHashSet unavailableDomainsCache)
    {
        final ConcurrentHashMap<String, List<T>> resultMap = new ConcurrentHashMap<>();
        ObjectReader reader = mapper.reader(mapper.getTypeFactory().constructCollectionType(List.class, objectClass));
        submitCachedRequests(method, action, parameters, domains, reader, cache, unavailableDomainsCache, List.class);
        return resultMap;
    }

    /**
     * Submit action tasks to {#code getExecutor()} with periodic repetition (in controller configuration).
     * @param method Http method
     * @param action to call
     * @param parameters of call
     * @param domains to be used
     * @param reader for parsing JSON response
     * @param result concurrent map
     * @param unavailableDomainsCache concurrent map
     * @param returnClass class of result object(s)
     * @param <T>
     */
    protected <T> void submitCachedRequests(InterDomainAction.HttpMethod method, String action,
                                            Map<String, String> parameters, Collection<Domain> domains,
                                            ObjectReader reader, ConcurrentMap result,
                                            ConcurrentHashSet unavailableDomainsCache, Class<T> returnClass)
    {
        for (final Domain domain : domains) {
            Runnable task = new DomainTask<T>(method, action, parameters, domain, reader, returnClass, result, unavailableDomainsCache);
            getExecutor().scheduleWithFixedDelay(task, 0, getConfiguration().getInterDomainCacheRefreshRate(), TimeUnit.SECONDS);
        }
    }

    protected boolean isResourcesCachedInitialized()
    {
        boolean initialized = true;
        synchronized (availableResources) {
            if (availableResources.size() + unavailableResources.size() != listForeignDomains().size()) {
                initialized = false;
            }
        }
        if (!initialized) {
            initResourceCache();
        }
        return initialized;
    }

    public Set<DomainCapability> listAllocatableForeignResources()
    {
        DomainCapabilityListRequest request = new DomainCapabilityListRequest(DomainCapabilityListRequest.Type.RESOURCE);
        Set<DomainCapability> resources = new HashSet<>();
        for (List<DomainCapability> resourceList : listForeignCapabilities(request).values()) {
            resources.addAll(resourceList);
        }
        return resources;
    }


    /**
     * Returns cached allocatable resources for now or will perform synchronized request to all foreign domains (can be slow, depending on {@code DomainsConnector.THREAD_TIMEOUT}).
     * @param request
     * @return
     */
    @Override
    public Map<String, List<DomainCapability>> listForeignCapabilities(DomainCapabilityListRequest request)
    {
        Map<String, List<DomainCapability>> capabilities;
        if (request.getInterval() == null && request.getTechnology() == null && DomainCapabilityListRequest.Type.RESOURCE.equals(request.getType()) && isResourcesCachedInitialized()) {
            capabilities = new HashMap<>();
            // Writing to {@code availableResources} is synchronized on result map in {@link DomainTask<T>}
            synchronized (availableResources) {
                for (Map.Entry<String, List<DomainCapability>> entry : availableResources.entrySet()) {
                    if (unavailableResources.contains(entry.getKey())) {
                        capabilities.put(entry.getKey(), entry.getValue());
                    }
                    else {
                        List<DomainCapability> resources = entry.getValue();
                        for (DomainCapability resource : resources) {
                            resource.setAvailable(false);
                        }
                        capabilities.put(entry.getKey(), resources);
                    }
                }
            }
        } else {
            capabilities = super.listForeignCapabilities(request);
        }
        return capabilities;
    }
}
