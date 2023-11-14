package cz.cesnet.shongo.controller.booking.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.request.TagListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class TagTest extends AbstractControllerTest {

    @Test
    public void testCreateTag() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();

        final String tagName1 = "testTag1";
        final String tagName2 = "testTag2";
        final TagType tagType2 = TagType.NOTIFY_EMAIL;
        final JsonNode tagData2 = objectMapper.readTree("[\"karnis@cesnet.cz\",\"filip.karnis@cesnet.cz\"]");

        ResourceService resourceService = getResourceService();

        // tag1 init
        cz.cesnet.shongo.controller.api.Tag tag1 = new cz.cesnet.shongo.controller.api.Tag();
        tag1.setName(tagName1);

        // tag2 init
        cz.cesnet.shongo.controller.api.Tag tag2 = new cz.cesnet.shongo.controller.api.Tag();
        tag2.setName(tagName2);
        tag2.setType(tagType2);
        tag2.setData(tagData2);

        String tagId1 = resourceService.createTag(SECURITY_TOKEN_ROOT, tag1);
        String tagId2 = resourceService.createTag(SECURITY_TOKEN_ROOT, tag2);

        cz.cesnet.shongo.controller.api.Tag getResult1 = resourceService.getTag(SECURITY_TOKEN_ROOT, tagId1);
        cz.cesnet.shongo.controller.api.Tag getResult2 = resourceService.getTag(SECURITY_TOKEN_ROOT, tagId2);

        Assert.assertNotNull(getResult1);
        Assert.assertNotNull(getResult2);

        Assert.assertEquals(tagId1, getResult1.getId());
        Assert.assertEquals(tagName1, getResult1.getName());
        Assert.assertEquals(TagType.DEFAULT, getResult1.getType());
        Assert.assertNull(getResult1.getData());

        Assert.assertEquals(tagId2, getResult2.getId());
        Assert.assertEquals(tagName2, getResult2.getName());
        Assert.assertEquals(tagType2, getResult2.getType());
        Assert.assertEquals(tagData2, getResult2.getData());
    }

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

        // set another owner of the tag
        AclEntry aclEntry1 = new AclEntry();
        aclEntry1.setIdentityPrincipalId(getUserId(SECURITY_TOKEN_USER3));
        aclEntry1.setIdentityType(AclIdentityType.USER);
        aclEntry1.setObjectId(tagId);
        aclEntry1.setRole(ObjectRole.OWNER);
        authorizationService.createAclEntry(SECURITY_TOKEN_ROOT,aclEntry1);

        // set read permissions for everyone
        AclEntry aclEntry2 = new AclEntry();
        aclEntry2.setIdentityPrincipalId(Authorization.EVERYONE_GROUP_ID);
        aclEntry2.setIdentityType(AclIdentityType.GROUP);
        aclEntry2.setObjectId(tagId);
        aclEntry2.setRole(ObjectRole.READER);
        String aclEntryId = authorizationService.createAclEntry(SECURITY_TOKEN_ROOT,aclEntry2);

        // test if everyone has read permission for tag
        Assert.assertTrue(resourceService.listTags(new TagListRequest(SECURITY_TOKEN_ROOT)).size() > 0);
        Assert.assertTrue(resourceService.listTags(new TagListRequest(SECURITY_TOKEN_USER1)).size() > 0);
        Assert.assertTrue(resourceService.listTags(new TagListRequest(SECURITY_TOKEN_USER2)).size() > 0);

        // role owner for user3 should not be inherited
        Assert.assertNull(getAclEntry(getUserId(SECURITY_TOKEN_USER3),resourceId,ObjectRole.OWNER));

        // test if everyone has read permission for resource
        Assert.assertTrue(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_ROOT)).getItemCount() > 0);
        Assert.assertTrue(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_USER1)).getItemCount() > 0);
        Assert.assertTrue(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_USER2)).getItemCount() > 0);

        authorizationService.deleteAclEntry(SECURITY_TOKEN_ROOT,aclEntryId);

        // test if inherited permissions has been deleted
        Assert.assertTrue(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_ROOT)).getItemCount() > 0);
        Assert.assertFalse(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_USER1)).getItemCount() > 0);
        Assert.assertFalse(resourceService.listResources(new ResourceListRequest(SECURITY_TOKEN_USER2)).getItemCount() > 0);
    }

    @Test
    public void testRemoveTagsChildAcls() throws Exception
    {
        ResourceService resourceService = getResourceService();
        AuthorizationService authorizationService = getAuthorizationService();

        // tag1 init
        cz.cesnet.shongo.controller.api.Tag tag1 = new cz.cesnet.shongo.controller.api.Tag();
        tag1.setName("testTag1");
        String tagId1 = resourceService.createTag(SECURITY_TOKEN_ROOT, tag1);

        AclEntry aclEntry1 = new AclEntry();
        aclEntry1.setIdentityPrincipalId(Authorization.EVERYONE_GROUP_ID);
        aclEntry1.setIdentityType(AclIdentityType.GROUP);
        aclEntry1.setObjectId(tagId1);
        aclEntry1.setRole(ObjectRole.READER);
        String aclEntryId1 = authorizationService.createAclEntry(SECURITY_TOKEN_ROOT,aclEntry1);

        // tag2 init
        cz.cesnet.shongo.controller.api.Tag tag2 = new cz.cesnet.shongo.controller.api.Tag();
        tag2.setName("testTag2");
        String tagId2 = resourceService.createTag(SECURITY_TOKEN_ROOT, tag2);

        AclEntry aclEntry2 = new AclEntry();
        aclEntry2.setIdentityPrincipalId(Authorization.EVERYONE_GROUP_ID);
        aclEntry2.setIdentityType(AclIdentityType.GROUP);
        aclEntry2.setObjectId(tagId2);
        aclEntry2.setRole(ObjectRole.READER);
        String aclEntryId2 = authorizationService.createAclEntry(SECURITY_TOKEN_ROOT,aclEntry2);

        // resource1 init
        cz.cesnet.shongo.controller.api.Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = createResource(resource);

        // assign tags to resource
        resourceService.assignResourceTag(SECURITY_TOKEN_ROOT, resourceId, tagId1);
        resourceService.assignResourceTag(SECURITY_TOKEN_ROOT, resourceId, tagId2);

        // test correct assigned acls
        Assert.assertNotNull(getAclEntryForGroup(Authorization.EVERYONE_GROUP_ID, resourceId, ObjectRole.READER));

        // remove resource-tag1
        resourceService.removeResourceTag(SECURITY_TOKEN_ROOT, resourceId, tagId1);

        // test if resource still have inherited acl from tag2
        Assert.assertNotNull(getAclEntryForGroup(Authorization.EVERYONE_GROUP_ID, resourceId, ObjectRole.READER));

        // remove resource-tag2
        resourceService.removeResourceTag(SECURITY_TOKEN_ROOT, resourceId, tagId2);

        // test if all inherited acls has been deleted
        Assert.assertNull(getAclEntryForGroup(Authorization.EVERYONE_GROUP_ID, resourceId, ObjectRole.READER));
    }

    @Test
    public void testFindTag() throws Exception
    {
        ResourceService resourceService = getResourceService();

        // tag1 init
        cz.cesnet.shongo.controller.api.Tag tag1 = new cz.cesnet.shongo.controller.api.Tag();
        tag1.setName("testTag1");
        String tagId1 = resourceService.createTag(SECURITY_TOKEN_ROOT, tag1);

        cz.cesnet.shongo.controller.api.Tag findResult = resourceService.findTag(SECURITY_TOKEN_ROOT,tag1.getName());
        cz.cesnet.shongo.controller.api.Tag getResult = resourceService.getTag(SECURITY_TOKEN_ROOT, tagId1);

        Assert.assertNotNull(getResult);
        Assert.assertNotNull(findResult);

        Assert.assertEquals(getResult.getName(),findResult.getName());
        Assert.assertEquals(getResult.getId(),findResult.getId());
    }

    @Test
    public void testTagForForeignResource()
    {
        ResourceService resourceService = getResourceService();
        Domain domain = new Domain();
        domain.setName(LocalDomain.getLocalDomainName() + ".notLocal");
        domain.setOrganization("CESNET z.s.p.o.");
        DeviceAddress deviceAddress = new DeviceAddress("127.0.0.1", 8443);
        domain.setDomainAddress(deviceAddress);
        domain.setPasswordHash("hashedpassword");
        resourceService.createDomain(SECURITY_TOKEN_ROOT, domain);

        cz.cesnet.shongo.controller.api.Tag tag = new cz.cesnet.shongo.controller.api.Tag();
        tag.setName("testTag1");
        String tagId = resourceService.createTag(SECURITY_TOKEN_ROOT, tag);

        String resourceId = ObjectIdentifier.formatId(domain.getName(), ObjectType.RESOURCE, 1L);
        resourceService.assignResourceTag(SECURITY_TOKEN_ROOT, resourceId, tagId);


        TagListRequest request = new TagListRequest(SECURITY_TOKEN_ROOT);
        request.setResourceId(resourceId);
        List<cz.cesnet.shongo.controller.api.Tag> response = resourceService.listTags(request);
        Assert.assertEquals(1, response.size());
    }
}
