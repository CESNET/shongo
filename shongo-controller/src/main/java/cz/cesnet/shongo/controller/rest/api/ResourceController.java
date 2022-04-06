package cz.cesnet.shongo.controller.rest.api;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.models.TechnologyModel;
import cz.cesnet.shongo.controller.rest.models.resource.*;
import io.swagger.v3.oas.annotations.Operation;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;
import static cz.cesnet.shongo.controller.rest.models.TimeInterval.DATETIME_FORMATTER;

/**
 * Rest controller for resource endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final Cache cache;
    private final ResourceService resourceService;

    public ResourceController(@Autowired Cache cache, @Autowired ResourceService resourceService)
    {
        this.cache = cache;
        this.resourceService = resourceService;
    }

    /**
     * Lists {@link Resource}s.
     */
    @Operation(summary = "Lists available resources.")
    @GetMapping()
    List<ResourceModel> listResources(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestParam(value = "technology", required = false) TechnologyModel technology,
            @RequestParam(value = "tag", required = false) String tag)
    {
        ResourceListRequest resourceListRequest = new ResourceListRequest();
        resourceListRequest.setSecurityToken(securityToken);
        resourceListRequest.setAllocatable(true);
        if (technology != null) {
            resourceListRequest.setTechnologies(technology.getTechnologies());
        }
        if (tag != null) {
            resourceListRequest.setTagName(tag);
        }

        // Filter only reservable resources
        resourceListRequest.setPermission(ObjectPermission.RESERVE_RESOURCE);

        ListResponse<ResourceSummary> accessibleResources = resourceService.listResources(resourceListRequest);

        return accessibleResources.getItems()
                .stream()
                .map(ResourceModel::new)
                // Filter only resources with either technology or tag
                .filter(resource -> !(resource.getTechnology() == null && resource.getTags().isEmpty()))
                .collect(Collectors.toList());
    }

    /**
     * Gets {@link ResourceCapacityUtilization}s.
     */
    @Operation(summary = "Returns resource utilization.")
    @GetMapping("/utilization")
    ResourceCapacityUtilizationModel getResourceUtilization(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestParam(value = "period") Period period,
            @RequestParam(value = "start") String startParam,
            @RequestParam(value = "end") String endParam,
            @RequestParam(value = "refresh", required = false) boolean refresh)
    {
        DateTime start = DATETIME_FORMATTER.parseDateTime(startParam);
        DateTime end = DATETIME_FORMATTER.parseDateTime(endParam);

        ResourcesUtilization resourcesUtilization = cache.getResourcesUtilization(securityToken, refresh);
        Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> utilization =
                resourcesUtilization.getUtilization(new Interval(start, end), period);

        return new ResourceCapacityUtilizationModel(resourcesUtilization.getResourceCapacities(), utilization);
    }

    /**
     * Gets {@link ResourceCapacityUtilization}s.
     */
    @Operation(summary = "Returns resource utilization.")
    @GetMapping("/utilization2")
    RModel getResourceUtilization(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
//            @RequestParam(value = "interval") Interval interval,
//            @RequestParam(value = "resourceCapacityClass") String resourceCapacityClassName,
            @RequestParam(value = "resourceId") String resourceId) throws ClassNotFoundException
    {
//        Class<? extends ResourceCapacity> resourceCapacityClass = (Class<? extends ResourceCapacity>)
//                Class.forName(ResourceCapacity.class.getCanonicalName() + "$" + resourceCapacityClassName);
        ResourcesUtilization resourcesUtilization =
                cache.getResourcesUtilization(securityToken, false);
        ResourceCapacity resourceCapacity =
//                resourcesUtilization.getResourceCapacity(resourceId, resourceCapacityClass);
                resourcesUtilization.getResourceCapacity(resourceId, ResourceCapacity.Room.class);
        ResourceCapacityUtilization resourceCapacityUtilization =
//                resourcesUtilization.getUtilization(resourceCapacity, interval);
                resourcesUtilization.getUtilization(resourceCapacity, new Interval(DateTime.parse("2020-12-09T10:10:30"), DateTime.parse("2023-12-09T10:10:30")));
//        ModelAndView modelAndView = new ModelAndView("resourceCapacityUtilizationDescription");

        Map<String, UserInformation> users = new HashMap<String, UserInformation>();
        if (resourceCapacityUtilization != null) {
            Collection<String> userIds = resourceCapacityUtilization.getReservationUserIds();
            cache.fetchUserInformation(securityToken, userIds);
            for (String userId : userIds) {
                UserInformation userInformation = cache.getUserInformation(securityToken, userId);
                users.put(userId, userInformation);
            }
        }
//        modelAndView.addObject("users", users);
//        modelAndView.addObject("interval", interval);
//        modelAndView.addObject("resourceCapacity", resourceCapacity);
//        modelAndView.addObject("resourceCapacityUtilization", resourceCapacityUtilization);
        return new RModel(resourceCapacity, resourceCapacityUtilization);
    }
}
