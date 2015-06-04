package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import org.codehaus.jackson.map.ObjectReader;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Cached foreign domains connector for Inter Domain Agent
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class CachedDomainsConnector extends DomainsConnector
{
    private ConcurrentHashMap<String, List<DomainCapability>> foreignCapabilities = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, List<DomainCapability>> unavailableForeignCapabilities = new ConcurrentHashMap<>();

    private final int THREAD_TIMEOUT = 500;

    private final long EXECUTOR_MINUTES_DELAY = 5;

    public CachedDomainsConnector(EntityManagerFactory entityManagerFactory, ControllerConfiguration configuration, EmailSender emailSender)
    {
        super(entityManagerFactory, configuration, emailSender);
    }

    @Override
    public Map<String, List<DomainCapability>> listForeignCapabilities(DomainCapabilityListRequest request)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("type", request.getType().toString());
        if (request.getInterval() != null) {
            parameters.put("interval", request.getInterval().toString());
        }
        if (request.getTechnology() != null) {
            parameters.put("technology", request.getTechnology().toString());
        }
        Map<String, List<DomainCapability>> domainResources = performListRequest(InterDomainAction.HttpMethod.GET, InterDomainAction.DOMAIN_CAPABILITY_LIST, parameters, listForeignDomains(), DomainCapability.class);
        return domainResources;
    }

    @Override
    protected <T> Map<String, List<T>> performListRequest(final InterDomainAction.HttpMethod method, final String action,
                                                          final Map<String, String> parameters, final Collection<Domain> domains,
                                                          Class<T> objectClass)
    {
        final ConcurrentHashMap<String, List<T>> resultMap = new ConcurrentHashMap<>();
        ObjectReader reader = mapper.reader(mapper.getTypeFactory().constructCollectionType(List.class, objectClass));
        performParallelCachedRequest(method, action, parameters, domains, reader, foreignCapabilities, List.class);
        return resultMap;
    }

    protected synchronized <T> void performParallelCachedRequest(InterDomainAction.HttpMethod method, String action, Map<String, String> parameters, Collection<Domain> domains, ObjectReader reader, ConcurrentHashMap result, Class<T> returnClass)
    {
        final ConcurrentHashSet<Domain> failed = new ConcurrentHashSet<>();

        for (final Domain domain : domains) {
            Runnable task = (Runnable) new DomainTask<T>(method, action, parameters, domain, reader, returnClass, result);
            getExecutor().scheduleWithFixedDelay(task, 0, EXECUTOR_MINUTES_DELAY, TimeUnit.MINUTES);
        }

        while (result.size() + failed.size() < domains.size()) {
            try {
                Thread.sleep(THREAD_TIMEOUT);
            } catch (InterruptedException e) {
                continue;
            }
        }
    }
}
