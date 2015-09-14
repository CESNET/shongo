package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import cz.cesnet.shongo.controller.api.domains.response.Reservation;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;

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
     * Cache of available resources mapped by domains codes.
     *
     * THIS CACHE IS SHARED BETWEEN THREADS, LOCK ON {@code availableResources} BEFORE USE.
     */
    private Map<String, List<DomainCapability>> availableResources = new HashMap<>();

    /**
     * Cache of unavailable domains, additional for {#code availableResources}.
     *
     * THIS CACHE IS SHARED BETWEEN THREADS, LOCK ON {@code availableResources} BEFORE USE.
     */
    private Set<String> unavailableResources = new HashSet<>();

    /**
     * Cache for foreign reservations by resource id.
     *
     * THIS CACHE IS SHARED BETWEEN THREADS, LOCK ON {@code reservations} BEFORE USE.
     */
    private Map<String, List<Reservation>> reservations = new HashMap<>();

    /**
     * Cache of domains, additional for {#code reservations}.
     *
     * THIS CACHE IS SHARED BETWEEN THREADS, LOCK ON {@code reservations} BEFORE USE.
     */
    private Set<String> unavailableReservationsDomains = new HashSet<>();

    public CachedDomainsConnector(EntityManagerFactory entityManagerFactory, ControllerConfiguration configuration, EmailSender emailSender)
    {
        super(entityManagerFactory, configuration, emailSender);
        updateResourceCache();
        updateReservationCache();
    }

    /**
     * Start periodic cache of foreign domain's resources (type of {@link cz.cesnet.shongo.controller.ObjectType#RESOURCE}).
     * When cache is initialized, it will add new domains. Removing from cache and {@link ScheduledThreadPoolExecutor}
     * is handled by thread itself ({@link DomainTask}).
     */
    synchronized private void updateResourceCache()
    {
        // Init only for inactive domains
        List<Domain> domains = new ArrayList<>();
        synchronized (availableResources) {
            for (Domain domain : listAllocatableForeignDomains()) {
                if (!availableResources.containsKey(domain.getName())) {
                    domains.add(domain);
                    // Make sure no other thread start task before result is stored
//                    availableResources.put(domain.getName(), null);
                }
            }
        }
        Map<String, String> parameters = new HashMap<>();
        parameters.put("type", DomainCapabilityListRequest.Type.RESOURCE.toString());
        submitCachedTypedListRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_CAPABILITY_LIST, parameters,
                domains, DomainCapability.class, availableResources, unavailableResources);
    }

    /**
     * Start periodic cache of foreign domain's reservations.
     * TODO
     * When cache is initialized, it will add new domains. Removing from cache and {@link ScheduledThreadPoolExecutor}
     * is handled by thread itself ({@link DomainTask}).
     */
    synchronized private void updateReservationCache()
    {
        // Init only for inactive domains
        List<Domain> domains = new ArrayList<>();
        synchronized (reservations) {
            for (Domain domain : listAllocatableForeignDomains()) {
                if (!reservations.containsKey(domain.getName())) {
                    domains.add(domain);
                    // Make sure no other thread start task before result is stored
//                    reservations.put(domain.getName(), null);
                }
            }
        }
        submitCachedTypedListRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_RESOURCE_RESERVATION_LIST, null,
                domains, Reservation.class, reservations, unavailableReservationsDomains);
    }

    private <T> void submitCachedTypedListRequest(final InterDomainAction.HttpMethod method, final String action,
                                                                    final Map<String, String> parameters, final Collection<Domain> domains,
                                                                    Class<T> objectClass, Map<String, ?> cache, Set<String> unavailableDomainsCache)
    {
        ObjectReader reader = mapper.reader(mapper.getTypeFactory().constructCollectionType(List.class, objectClass));
        submitCachedRequests(method, action, parameters, domains, reader, cache, unavailableDomainsCache, List.class);
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
    synchronized protected <T> void submitCachedRequests(InterDomainAction.HttpMethod method, String action,
                                            Map<String, String> parameters, Collection<Domain> domains,
                                            ObjectReader reader, Map<String, ?> result,
                                            Set<String> unavailableDomainsCache, Class<T> returnClass)
    {
        for (final Domain domain : domains) {
            Runnable task = new DomainTask<>(method, action, parameters, domain, reader, returnClass, result, unavailableDomainsCache);
            getExecutor().scheduleWithFixedDelay(task, 0, getConfiguration().getInterDomainCacheRefreshRate(), TimeUnit.SECONDS);
        }
    }

    /**
     * @return true if cache contains resources for all allocatable domains
     * (if number of cached domains equals allocatable domains), false otherwise.
     *
     * Submits potentially missing domains to executor (checks every time).
     *
     * NOTE: used only in tests
     */
    protected boolean checkResourcesCacheInitialized()
    {
        boolean initialized = true;
        int cachedDomainsCount;
        synchronized (availableResources) {
            cachedDomainsCount = availableResources.size();
        }
        int actualDomainsCount = listAllocatableForeignDomains().size();
        if (cachedDomainsCount != actualDomainsCount) {
            initialized = false;
        }

        updateResourceCache();
        return initialized;
    }

    /**
     * Check if cache is ready to be used. Does not update the cache.
     *
     * True if
     * - size of the cache exact to allocatable domains
     * - cache is bigger than number of allocatable domains
     * - cache smaller only by 1 (except it is empty)
     * False otherwise
     *
     * @param cache to be checked
     * @return
     */
    private boolean isCacheReady(Map cache)
    {
        int cachedDomainsCount;
        synchronized (cache) {
            cachedDomainsCount = cache.size();
        }
        int actualDomainsCount = listAllocatableForeignDomains().size();

        boolean isReady = false;
        if (actualDomainsCount == cachedDomainsCount) {
            isReady = true;
        }
        else if (actualDomainsCount > cachedDomainsCount) {
            if (actualDomainsCount - cachedDomainsCount == 1 && actualDomainsCount != 1) {
                isReady = true;
            }
        }
        else if (cachedDomainsCount > actualDomainsCount) {
            isReady = true;
        }

        return isReady;
    }

    /**
     * @return true if cache contains resources for all allocatable domains
     * (if number of cached domains equals allocatable domains +-1), false otherwise.
     *
     * Submits potentially missing domains to executor (checks every time).
     */
    protected boolean isResourcesCacheReady()
    {
        boolean isReady = isCacheReady(availableResources);
        updateResourceCache();
        return isReady;
    }

    /**
     * @return true if cache contains reservations for all allocatable domains
     * (if number of cached domains equals allocatable domains +-1), false otherwise.
     *
     * Submits potentially missing domains to executor (checks every time).
     */
    protected boolean isReservationCacheReady()
    {
        boolean isReady = isCacheReady(reservations);
        updateReservationCache();
        return isReady;
    }

    /**
     * @return {@link Set<DomainCapability>} of resources for foreign available domains
     */
    public Set<DomainCapability> listAvailableForeignResources(DomainCapabilityListRequest request)
    {
        if (!DomainCapabilityListRequest.Type.RESOURCE.equals(request.getCapabilityType())) {
            throw new IllegalArgumentException("Type has to be RESOURCE.");
        }
        Set<DomainCapability> resources = new HashSet<>();
        for (Map.Entry<String, List<DomainCapability>> entry : listForeignCapabilities(request).entrySet()) {
            List<DomainCapability> resourceList = entry.getValue();
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
        if (request.getInterval() == null && request.getTechnology() == null && DomainCapabilityListRequest.Type.RESOURCE.equals(request.getCapabilityType()) && isResourcesCacheReady()) {
            capabilities = new HashMap<>();
            // Writing to {@code availableResources} is synchronized on result map in {@link DomainTask<T>}
            synchronized (availableResources) {
                // Filter domains for which resources will be returned.
                Map<String, List<DomainCapability>> requestedResources = new HashMap<>();
                if (!request.getResourceIds().isEmpty()) {
                    for (String requestResourceId : request.getResourceIds()) {
                        String domainName = ObjectIdentifier.parseDomain(requestResourceId);
                        requestedResources.put(domainName, availableResources.get(domainName));
                    }
                }
                else if (request.getDomainName() != null) {
                    String domainName = request.getDomainName();
                    requestedResources.put(domainName, availableResources.get(domainName));
                }
                else {
                    requestedResources = availableResources;
                }

                // Filter requested resources and set unavailable resources
                for (Map.Entry<String, List<DomainCapability>> entry : requestedResources.entrySet()) {
                    String domainName = entry.getKey();
                    List<DomainCapability> capabilityList = entry.getValue();
                    if (capabilityList == null) {
                        continue;
                    }
                    // If domain exists and is allocatable
                    if (isDomainAllocatable(domainName)) {
                        List<DomainCapability> resources = new ArrayList<>();
                        for (DomainCapability resource : capabilityList) {
                            // Filter by ids
                            if (request.getResourceIds().isEmpty() || request.getResourceIds().contains(resource.getId())) {
                                // Filter by type
                                if (request.getResourceType() == null || request.getResourceType().equals(resource.getType())) {
                                    if (unavailableResources.contains(domainName)) {
                                        resource.setAvailable(false);
                                    }
                                    resources.add(resource);
                                }
                            }
                        }
                        if (!resources.isEmpty()) {
                            capabilities.put(entry.getKey(), resources);
                        }
                    }
                }
            }
        } else {
            capabilities = super.listForeignCapabilities(request);
        }
        return capabilities;
    }

    /**
     * @return {@link List<Reservation>} of resources for foreign available domains
     */
    public List<ReservationSummary> listForeignDomainReservations(String resourceId)
    {
        List<Reservation> response;
        List<ReservationSummary> result = new ArrayList<>();
        String domainName = ObjectIdentifier.parseDomain(resourceId);
        if (isReservationCacheReady()) {
            synchronized (reservations) {
                response = this.reservations.get(domainName);
                for (cz.cesnet.shongo.controller.api.domains.response.Reservation reservation : response) {
                    if (resourceId.equals(reservation.getForeignResourceId())) {
                        result.add(reservation.toReservationSummary());
                    }
                }
            }
        }
        else {
            Domain domain = getDomainService().findDomainByName(domainName);
            response = super.listReservations(domain, resourceId);
            for (cz.cesnet.shongo.controller.api.domains.response.Reservation reservation : response) {
                result.add(reservation.toReservationSummary());
            }
        }


        return result;
    }
}
