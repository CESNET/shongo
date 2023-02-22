package cz.cesnet.shongo.controller.domains;

import com.fasterxml.jackson.databind.ObjectReader;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.request.CapabilitySpecificationRequest;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.joda.time.Interval;

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
     * Cache for foreign resourceReservations by resource id.
     *
     * THIS CACHE IS SHARED BETWEEN THREADS, LOCK ON {@code resourceReservations} BEFORE USE.
     */
    private Map<String, List<Reservation>> resourceReservations = new HashMap<>();

    /**
     * Cache of domains, additional for {#code resourceReservations}.
     *
     * THIS CACHE IS SHARED BETWEEN THREADS, LOCK ON {@code resourceReservations} BEFORE USE.
     */
    private Set<String> unavailableReservationsDomains = new HashSet<>();

    public CachedDomainsConnector(ControllerConfiguration configuration, DomainService domainService,
                                  DomainAdminNotifier notifier)
    {
        super(configuration, domainService, notifier);
        updateResourceCache();
        updateResourceReservationCache();
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
        MultiMap<String, String> parameters = new MultiValueMap<>();
        CapabilitySpecificationRequest capabilitySpecificationRequest = new CapabilitySpecificationRequest(DomainCapability.Type.RESOURCE);
        List<CapabilitySpecificationRequest> capabilityListRequestSpecification = Collections.singletonList(capabilitySpecificationRequest);
        submitCachedTypedListRequest(InterDomainAction.HttpMethod.POST, InterDomainAction.DOMAIN_CAPABILITY_LIST, null,
                capabilityListRequestSpecification, domains, DomainCapability.class, availableResources, unavailableResources);
    }

    /**
     * Start periodic cache of foreign domain's resourceReservations.
     * TODO
     * When cache is initialized, it will add new domains. Removing from cache and {@link ScheduledThreadPoolExecutor}
     * is handled by thread itself ({@link DomainTask}).
     */
    synchronized private void updateResourceReservationCache()
    {
        // Init only for inactive domains
        List<Domain> domains = new ArrayList<>();
        synchronized (resourceReservations) {
            for (Domain domain : listAllocatableForeignDomains()) {
                if (!resourceReservations.containsKey(domain.getName())) {
                    domains.add(domain);
                }
            }
        }
        submitCachedTypedListRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_RESOURCE_RESERVATION_LIST,
                null, null, domains, Reservation.class, resourceReservations, unavailableReservationsDomains);
    }

    private <T> void submitCachedTypedListRequest(final InterDomainAction.HttpMethod method, final String action,
                                                                    final MultiMap<String, String> parameters, Object data, final Collection<Domain> domains,
                                                                    Class<T> objectClass, Map<String, ?> cache, Set<String> unavailableDomainsCache)
    {
        ObjectReader reader = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(List.class, objectClass));
        submitCachedRequests(method, action, parameters, data, domains, reader, cache, unavailableDomainsCache, List.class);
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
     * @param <T> type of the returned result
     */
    synchronized protected <T> void submitCachedRequests(InterDomainAction.HttpMethod method, String action,
                                                         MultiMap<String, String> parameters, Object data, Collection<Domain> domains,
                                                         ObjectReader reader, Map<String, ?> result,
                                                         Set<String> unavailableDomainsCache, Class<T> returnClass)
    {
        for (final Domain domain : domains) {
            Runnable task = new DomainTask<>(method, action, parameters, data, domain, reader, returnClass, result, unavailableDomainsCache);
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
     * @return true if cache is synchronized
     */
    private boolean isCacheReady(Map cache, String domainName)
    {
        int cachedDomainsCount;
        synchronized (cache) {
            if (domainName != null && cache.get(domainName) == null) {
                return false;
            }
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
    protected boolean isResourcesCacheReady(String domainName)
    {
        boolean isReady = isCacheReady(availableResources, domainName);
        updateResourceCache();
        return isReady;
    }

    /**
     * @return true if cache contains resourceReservations for all allocatable domains
     * (if number of cached domains equals allocatable domains +-1), false otherwise.
     *
     * Submits potentially missing domains to executor (checks every time).
     */
    protected boolean isReservationCacheReady(String domainName)
    {
        boolean isReady = isCacheReady(resourceReservations, domainName);
        updateResourceReservationCache();
        return isReady;
    }

    /**
     * @return {@link Set<DomainCapability>} of resources for foreign available domains
     */
    public Set<DomainCapability> listAvailableForeignResources(DomainCapabilityListRequest request)
    {
        request.validateForResource();
        Set<DomainCapability> resources = new HashSet<>();
        for (Map.Entry<String, List<DomainCapability>> entry : listForeignCapabilities(request).entrySet()) {
            List<DomainCapability> resourceList = entry.getValue();
            resources.addAll(resourceList);
        }
        return resources;
    }


    /**
     * Returns cached allocatable resources for now or will perform synchronized request to all foreign domains (can be slow, depending on {@code DomainsConnector.THREAD_TIMEOUT}).
     * TODO: allows only capabilities from one domain, when resources is specified so must be domain
     *
     * @param request to filter foreign {@link DomainCapability}
     * @return {@link DomainCapability} by their domains
     */
    @Override
    public Map<String, List<DomainCapability>> listForeignCapabilities(DomainCapabilityListRequest request)
    {
        Map<String, List<DomainCapability>> capabilities = new HashMap<>();
        try {
            if (useResourcesCache(request)) {
                // Writing to {@code availableResources} is synchronized on result map in {@link DomainTask<T>}
                synchronized (availableResources) {
                    // Filter domains for which resources will be returned.
                    Map<String, List<DomainCapability>> resourcesByDomain = new HashMap<>();
                    if (request.getDomainName() != null) {
                        String domainName = request.getDomainName();
                        for (String requestResourceId : request.getResourceIds()) {
                            if (!domainName.equals(ObjectIdentifier.parseDomain(requestResourceId))) {
                                throw new IllegalArgumentException("Requested resource does not belong to requested domain (domain: " + domainName + ", resource: " + requestResourceId + ")");
                            }
                        }

                        resourcesByDomain.put(domainName, availableResources.get(domainName));
                    } else {
                        resourcesByDomain = availableResources;
                    }

                    // Filter requested resources and set unavailable ones
                    for (Map.Entry<String, List<DomainCapability>> entry : resourcesByDomain.entrySet()) {
                        String domainName = entry.getKey();
                        List<DomainCapability> capabilityList = entry.getValue();
                        if (capabilityList == null) {
                            // If results are not available yet, see {@link DomainTask}
                            continue;
                        }
                        List<DomainCapability> resources = new ArrayList<>();
                        for (DomainCapability resource : capabilityList) {
                            // Filter by IDs
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
                       if (domainExists(domainName)) {
                           capabilities.put(domainName, resources);
                        }
                    }
                }
            } else {
                Map<String, List<DomainCapability>> resourcesByDomain = super.listForeignCapabilities(request);
                for (Map.Entry<String, List<DomainCapability>> entry : resourcesByDomain.entrySet()) {
                    List<DomainCapability.Type> requestedTypes = request.getRequestedCapabilitiesTypes();
                    String domainName = entry.getKey();
                    List<DomainCapability> capabilityList = entry.getValue();
                    if (capabilityList == null) {
                        continue;
                    }
                    if (Boolean.FALSE.equals(request.getOnlyAllocatable()) || isDomainAllocatable(domainName)) {
                        List<DomainCapability> resources = new ArrayList<>();
                        for (DomainCapability resource : capabilityList) {
                            // Filter by IDs
                            if (request.getResourceIds().isEmpty() || request.getResourceIds().contains(resource.getId())) {
                                // Filter by user given type
                                if (request.getResourceType() == null || request.getResourceType().equals(resource.getType())) {
                                    resources.add(resource);
                                }
                            }
                            // Check if requested type is present
                            if (requestedTypes.contains(resource.getCapabilityType())) {
                                requestedTypes.remove(resource.getCapabilityType());
                            }
                        }
                        // If all requested types are present
                        if (requestedTypes.isEmpty()) {
                            capabilities.put(domainName, resources);
                        }
                        else {
                            capabilities.put(domainName, Collections.<DomainCapability>emptyList());
                        }
                    }
                }
            }
        }
        catch (IllegalArgumentException ex) {
            // Cache exception from {@code useResourcesCache()} if request is not valid
        }
        return capabilities;
    }

    /**
     * Check the {@code request} if resource cache should be used - {@code availableResources}.
     * @param request to be checked
     * @throws IllegalArgumentException when only allocatable domains are required, but given domain is not
     * @return true if resource cache has result for given request
     */
    private boolean useResourcesCache(DomainCapabilityListRequest request)
    {
        if (Boolean.FALSE.equals(request.getOnlyAllocatable())) {
            if (request.getDomain() == null || !request.getDomain().isAllocatable()) {
                return false;
            }
        }
        else if (request.getOnlyAllocatable() == null) {
            if (request.getDomain() != null && !request.getDomain().isAllocatable()) {
                return false;
            }
        }
        else {
            if (request.getDomain() != null && !request.getDomain().isAllocatable()) {
                throw new IllegalArgumentException("Domain \"" + request.getDomainName() + "\" is not allocatable, but only allocatable was required.");
            }
        }
        if (request.getCapabilitySpecificationRequests().size() != 1) {
            return false;
        } else {
            CapabilitySpecificationRequest capabilitySpecificationRequest = request.getCapabilitySpecificationRequests().get(0);
            if (!DomainCapability.Type.RESOURCE.equals(capabilitySpecificationRequest.getCapabilityType())) {
                return false;
            }
            if (capabilitySpecificationRequest.getTechnologyVariants() != null && !capabilitySpecificationRequest.getTechnologyVariants().isEmpty()) {
                return false;
            }
        }
        //TODO: better validate interval
        if (request.getSlot() != null) {
            return false;
        }
        if (!isResourcesCacheReady(request.getDomainName())) {
            return false;
        }

        return true;
    }

    /**
     * List foreign resourceReservations for given {@code resourceId}. Returns cached resourceReservations if available.
     *
     * @return {@link List<Reservation>} of resources for foreign available domains
     */
    public List<ReservationSummary> listForeignResourcesReservations(String resourceId, Interval interval) throws ForeignDomainConnectException
    {
        List<Reservation> response;
        List<ReservationSummary> result = new ArrayList<>();
        String domainName = ObjectIdentifier.parseDomain(resourceId);
        if (isReservationCacheReady(domainName)) {
            synchronized (resourceReservations) {
                response = this.resourceReservations.get(domainName);
                for (cz.cesnet.shongo.controller.api.domains.response.Reservation reservation : response) {
                    ForeignSpecification specification = reservation.getSpecification();
                    if (specification instanceof ResourceSpecification) {
                        ResourceSpecification resourceSpecification = (ResourceSpecification) specification;
                        if (resourceId.equals(resourceSpecification.getForeignResourceId())) {
                            result.add(reservation.toReservationSummary());
                        }
                    }
                    else {
                        throw new TodoImplementException("Unsupported foreign specification.");
                    }
                }
            }
        }
        else {
            Domain domain = getDomainService().findDomainByName(domainName);
            response = super.listReservations(domain, resourceId, interval);
            for (cz.cesnet.shongo.controller.api.domains.response.Reservation reservation : response) {
                result.add(reservation.toReservationSummary());
            }
        }


        return result;
    }
}
