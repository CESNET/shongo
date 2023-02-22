package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.DummyAuthorization;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link AuthorizationExpression}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationExpressionTest extends AbstractControllerTest
{
    @Test
    public void test() throws Exception
    {
        DummyAuthorization authorization = getAuthorization();
        AuthorizationService authorizationService = getAuthorizationService();

        Assert.assertTrue(evaluate("true", SECURITY_TOKEN_USER1));
        Assert.assertFalse(evaluate("true && false", SECURITY_TOKEN_USER1));

        authorization.setUserAuthorizationData(SECURITY_TOKEN_USER1, new UserAuthorizationData(0));
        Assert.assertFalse(evaluate("loa == 2", SECURITY_TOKEN_USER1));
        authorization.setUserAuthorizationData(SECURITY_TOKEN_USER1, new UserAuthorizationData(2));
        Assert.assertTrue(evaluate("loa == 2", SECURITY_TOKEN_USER1));

        String groupId = authorizationService.createGroup(SECURITY_TOKEN_ROOT, new Group("test", Group.Type.USER));
        Assert.assertFalse(evaluate("group('test').contains(id)", SECURITY_TOKEN_USER2));
        authorizationService.addGroupUser(SECURITY_TOKEN_ROOT, groupId, getUserId(SECURITY_TOKEN_USER2));
        Assert.assertTrue(evaluate("group('test').contains(id)", SECURITY_TOKEN_USER2));

        Assert.assertTrue(evaluate("loa == 2 || group('test').contains(id)", SECURITY_TOKEN_USER1));
        Assert.assertTrue(evaluate("loa == 2 || group('test').contains(id)", SECURITY_TOKEN_USER2));
    }

    public boolean evaluate(String expression, SecurityToken securityToken)
    {
        Authorization authorization = getAuthorization();
        UserInformation user = getUserInformation(securityToken);
        UserAuthorizationData userAuthorizationData = authorization.getUserAuthorizationData(securityToken);
        AuthorizationExpression authorizationExpression = new AuthorizationExpression(expression, authorization);
        return authorizationExpression.evaluate(user, userAuthorizationData);
    }
}
