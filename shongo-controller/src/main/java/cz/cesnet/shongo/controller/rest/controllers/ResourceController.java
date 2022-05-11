package cz.cesnet.shongo.controller.rest.controllers;

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
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;

/**
 * Rest controller for resource endpoints.
 *
 * @author Filip Karnis
 */
@Slf4j
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
    @GetMapping
    List<ResourceModel> listResources(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestParam(value = "technology", required = false) TechnologyModel technology,
            @RequestParam(value = "tag", required = false) String tag)
    {
        ResourceListRequest resourceListRequest = new ResourceListRequest(securityToken);
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
                .map(resourceSummary -> {
                    // TODO hasCapacity
                    Resource resource = resourceService.getResource(securityToken, resourceSummary.getId());
                    return new ResourceModel(resourceSummary, !resource.getCapabilities().isEmpty());
                })
                // Filter only resources with either technology or tag
                .filter(resource -> !(resource.getTechnology() == null && resource.getTags().isEmpty()))
                .collect(Collectors.toList());
    }

    /**
     * Gets {@link ResourceCapacityUtilization}s.
     */
    @Operation(summary = "Returns resource utilization.")
    @GetMapping("/capacity_utilizations")
    ListResponse<ResourceUtilizationModel> listResourcesUtilization(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestParam(value = "interval_from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            DateTime intervalFrom,
            @RequestParam(value = "interval_to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            DateTime intervalTo,
            @RequestParam Unit unit,
            @RequestParam(required = false) int start,
            @RequestParam(required = false) int count,
            @RequestParam(required = false) boolean refresh)
    {
        Period period = unit.getPeriod();
        ResourcesUtilization resourcesUtilization = cache.getResourcesUtilization(securityToken, refresh);
        Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> utilization =
                resourcesUtilization.getUtilization(new Interval(intervalFrom, intervalTo), period);
        List<ResourceUtilizationModel> items = new ArrayList<>();
        utilization.forEach((interval, resourceCapacityUtilizations) -> {
            items.add(ResourceUtilizationModel.fromApi(interval, resourceCapacityUtilizations));
        });
        return ListResponse.fromRequest(start, count, items);
    }

    /**
     * Lists {@link Resource}s.
     */
    @Operation(summary = "Gets resource utilization.")
    @GetMapping("/{id}/capacity_utilizations")
    ResourceUtilizationDetailModel getResourceUtilization(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable("id") String resourceId,
            @RequestParam String resource,
            @RequestParam(value = "interval_from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime intervalFrom,
            @RequestParam(value = "interval_to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime intervalTo)
            throws ClassNotFoundException
    {
        @SuppressWarnings("unchecked")
        Class<? extends ResourceCapacity> resourceCapacityClass = (Class<? extends ResourceCapacity>)
                Class.forName(ResourceCapacity.class.getCanonicalName() + "$" + "Room");
        ResourcesUtilization resourcesUtilization =
                cache.getResourcesUtilization(securityToken, false);
        ResourceCapacity resourceCapacity =
                resourcesUtilization.getResourceCapacity(resourceId, resourceCapacityClass);
        ResourceCapacityUtilization resourceCapacityUtilization =
                resourcesUtilization.getUtilization(resourceCapacity, new Interval(intervalFrom, intervalTo));

        ResourceCapacity.Room roomCapacity = (ResourceCapacity.Room) resourceCapacity;
        List<ReservationModel> reservations = (resourceCapacityUtilization != null)
                ? resourceCapacityUtilization.getReservations()
                .stream().map(res ->
                        ReservationModel.fromApi(res, cache.getUserInformation(securityToken, res.getUserId()))
                ).collect(Collectors.toList())
                : Collections.emptyList();
        return ResourceUtilizationDetailModel.fromApi(
                resourceCapacityUtilization, roomCapacity, new Interval(intervalFrom, intervalTo), reservations);
    }
}
