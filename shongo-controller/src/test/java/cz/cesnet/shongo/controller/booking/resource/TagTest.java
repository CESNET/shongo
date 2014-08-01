package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceReservation;
import cz.cesnet.shongo.controller.api.request.AclEntryListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.request.TagListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.controller.authorization.Authorization;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class TagTest extends AbstractControllerTest {
    @Test
    public void testCreateTagsAcl() throws Exception
    {
        ResourceService resourceService = getResourceService();
        AuthorizationService authorizationService = getAuthorizationService();

        cz.cesnet.shongo.controller.api.Tag tag = new cz.cesnet.shongo.controller.api.Tag();
        tag.setName("testTag");
        String tagId = resourceService.createTag(SECURITY_TOKEN_ROOT, tag);

        cz.cesnet.shongo.controller.api.Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = createResource(resource);
        resourceService.assignResourceTag(SECURITY_TOKEN_ROOT, resourceId, tagId);

        AclEntry aclEntry = new AclEntry();
        aclEntry.setIdentityPrincipalId(Authorization.EVERYONE_GROUP_ID);
        aclEntry.setIdentityType(AclIdentityType.GROUP);
        aclEntry.setObjectId(tagId);
        aclEntry.setRole(ObjectRole.READER);
        String aclEntryId = authorizationService.createAclEntry(SECURITY_TOKEN_ROOT,aclEntry);

        Assert.assertTrue(resourceService.listTags(new TagListRequest(SECURITY_TOKEN_ROOT)).size() > 0);
        Assert.assertTrue(resourceService.listTags(new TagListRequest(SECURITY_TOKEN_USER1)).size() > 0);

        ListResponse<AclEntry> response = authorizationService.listAclEntries(new AclEntryListRequest(SECURITY_TOKEN_ROOT));

        Assert.assertTrue(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_ROOT)).getItemCount() > 0);
        Assert.assertTrue(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_USER1)).getItemCount() > 0);
        Assert.assertTrue(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_USER2)).getItemCount() > 0);

        authorizationService.deleteAclEntry(SECURITY_TOKEN_ROOT,aclEntryId);

        Assert.assertTrue(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_ROOT)).getItemCount() > 0);
        Assert.assertFalse(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_USER1)).getItemCount() > 0);
        Assert.assertFalse(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_USER2)).getItemCount() > 0);
    }
}
