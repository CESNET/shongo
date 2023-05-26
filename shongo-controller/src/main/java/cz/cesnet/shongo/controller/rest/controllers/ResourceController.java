package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.RestApiPath;
import cz.cesnet.shongo.controller.rest.models.TechnologyModel;
import cz.cesnet.shongo.controller.rest.models.resource.ReservationModel;
import cz.cesnet.shongo.controller.rest.models.resource.ResourceCapacity;
import cz.cesnet.shongo.controller.rest.models.resource.ResourceCapacityUtilization;
import cz.cesnet.shongo.controller.rest.models.resource.ResourceModel;
import cz.cesnet.shongo.controller.rest.models.resource.ResourceUtilizationDetailModel;
import cz.cesnet.shongo.controller.rest.models.resource.ResourceUtilizationModel;
import cz.cesnet.shongo.controller.rest.models.resource.ResourcesUtilization;
import cz.cesnet.shongo.controller.rest.models.resource.Unit;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.config.security.AuthFilter.TOKEN;

/**
 * Rest controller for resource endpoints.
 *
 * @author Filip Karnis
 */
@Slf4j
@RestController
@RequestMapping(RestApiPath.RESOURCES)
@RequiredArgsConstructor
public class ResourceController
{

    private final Cache cache;
    private final ResourceService resourceService;

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
                    // Find out whether the resource has capacity
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
    @GetMapping(RestApiPath.CAPACITY_UTILIZATION)
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
        utilization.forEach((interval, resourceCapacityUtilization) -> {
            items.add(ResourceUtilizationModel.fromApi(interval, resourceCapacityUtilization));
        });
        return ListResponse.fromRequest(start, count, items);
    }

    /**
     * Lists {@link Resource}s.
     */
    @Operation(summary = "Gets resource utilization.")
    @GetMapping(RestApiPath.CAPACITY_UTILIZATION_DETAIL)
    ResourceUtilizationDetailModel getResourceUtilization(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable("id") String resourceId,
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
                resourceCapacityUtilization, roomCapacity, reservations);
    }
}
