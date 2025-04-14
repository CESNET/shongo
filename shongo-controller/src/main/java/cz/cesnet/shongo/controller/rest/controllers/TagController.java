package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.Tag;
import cz.cesnet.shongo.controller.api.request.TagListRequest;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.controller.rest.RestApiPath;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cz.cesnet.shongo.controller.rest.config.security.AuthFilter.TOKEN;

@RestController
@RequestMapping(RestApiPath.TAGS)
@RequiredArgsConstructor
public class TagController {

    private final ResourceService resourceService;

    /**
     * Lists {@link Tag}s.
     */
    @Operation(summary = "Lists available tags.")
    @GetMapping
    List<Tag> listTags(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestAttribute(value = "resourceId", required = false) String resourceId
    ) {
        TagListRequest request = new TagListRequest(securityToken);
        request.setResourceId(resourceId);

        return resourceService.listTags(request);
    }
}
