package cz.cesnet.shongo.client.web.auth;

import cz.cesnet.shongo.controller.SystemPermission;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;

/**
 * {@link DefaultWebSecurityExpressionHandler} which allows for
 * {@link SecurityExpressionRoot#hasPermission} for {@link SystemPermission}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class WebSecurityExpressionHandler extends DefaultWebSecurityExpressionHandler
{
    @Override
    protected SecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication,
            FilterInvocation filterInvocation)
    {
        SecurityExpressionRoot root = new SecurityExpressionRoot(authentication, filterInvocation);
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(new AuthenticationTrustResolverImpl());
        root.setRoleHierarchy(getRoleHierarchy());
        return root;
    }

    /**
     * {@link WebSecurityExpressionRoot} which allows to write following JSP code:
     *
     * <code>
     *     <security:authorize access="hasPermission(ADMINISTRATION)">
     *         ...
     *     </security:authorize>
     * </code>
     */
    public static class SecurityExpressionRoot extends WebSecurityExpressionRoot
    {
        public static final UserPermission ADMINISTRATION = UserPermission.ADMINISTRATION;
        public static final UserPermission OPERATOR = UserPermission.OPERATOR;
        public static final UserPermission RESERVATION = UserPermission.RESERVATION;
        public static final UserPermission RESOURCE_MANAGEMENT = UserPermission.RESOURCE_MANAGEMENT;

        /**
         * Constructor.
         *
         * @param authentication
         * @param filterInvocation
         */
        public SecurityExpressionRoot(Authentication authentication, FilterInvocation filterInvocation)
        {
            super(authentication, filterInvocation);
        }

        /**
         * @see #hasPermission(Object, Object)
         */
        public boolean hasPermission(Object permission)
        {
            return hasPermission(null, permission);
        }
    }
}
