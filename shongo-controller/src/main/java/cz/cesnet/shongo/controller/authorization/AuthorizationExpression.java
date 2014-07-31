package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.api.UserInformation;
import org.apache.commons.jexl2.*;

import java.util.Set;

/**
 * Authorization expressions for making decisions.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationExpression
{
    /**
     * @see JexlEngine
     */
    private static final JexlEngine jexlEngine;

    /**
     * Static initialization.
     */
    static {
        jexlEngine = new JexlEngine();
        jexlEngine.setSilent(false);
        jexlEngine.setStrict(true);
    }

    /**
     * @see Authorization
     */
    private Authorization authorization;

    /**
     * Parsed {@link Exception} which can be evaluated.
     */
    private Expression expression;

    /**
     * Constructor.
     *
     * @param expression    sets the {@link #expression}
     * @param authorization sets the {@link #authorization}
     */
    public AuthorizationExpression(String expression, Authorization authorization)
    {
        if (expression == null) {
            expression = "false";
        }
        this.authorization = authorization;
        try {
            this.expression = jexlEngine.createExpression(expression);
        }
        catch (JexlException exception) {
            Throwable cause = exception.getCause();
            if (cause == null) {
                cause = exception;
            }
            throw new RuntimeException("Authorization expression [" + expression + "] cannot be parsed.", cause);
        }
    }

    /**
     * @param userInformation
     * @param userAuthorizationData
     * @return true whether user with given {@code userInformation} and {@code userAuthorizationData} meets the expression,
     *         false otherwise
     */
    public boolean evaluate(UserInformation userInformation, UserAuthorizationData userAuthorizationData)
    {
        Context context = new Context(userInformation, userAuthorizationData);
        Object result;
        try {
            result = expression.evaluate(context);
        }
        catch (JexlException exception) {
            Throwable cause = exception.getCause();
            if (cause == null) {
                cause = exception;
            }
            throw new RuntimeException("Authorization expression [" + expression + "] cannot be evaluated.", cause);
        }
        if (result != null && result instanceof Boolean) {
            return (Boolean) result;
        }
        else {
            throw new RuntimeException("Authorization expression [" + expression + "] doesn't evaluate to boolean.");
        }
    }

    /**
     * {@link JexlContext} for the {@link AuthorizationExpression}.
     */
    public class Context implements JexlContext, NamespaceResolver
    {
        /**
         * @see UserInformation
         */
        private final UserInformation userInformation;

        /**
         * @see UserAuthorizationData
         */
        private final UserAuthorizationData userAuthorizationData;

        /**
         * Constructor.
         *
         * @param userInformation sets the {@link #userInformation}
         * @param userAuthorizationData sets the {@link #userAuthorizationData}
         */
        public Context(UserInformation userInformation, UserAuthorizationData userAuthorizationData)
        {
            this.userInformation = userInformation;
            this.userAuthorizationData = userAuthorizationData;
        }

        @Override
        public Object resolveNamespace(String name)
        {
            if (name == null) {
                return this;
            }
            else {
                return null;
            }
        }

        @Override
        public Object get(String name)
        {
            try {
                switch (Variable.valueOf(name)) {
                    case id:
                        return userInformation.getUserId();
                    case firstName:
                        return userInformation.getFirstName();
                    case lastName:
                        return userInformation.getLastName();
                    case email:
                        return userInformation.getEmail();
                    case organization:
                        return userInformation.getOrganization();
                    case provider:
                        return (userAuthorizationData != null ? userAuthorizationData.getProvider() : null);
                    case loa:
                        return (userAuthorizationData != null ? userAuthorizationData.getLoa()
                                        : UserAuthorizationData.LOA_MIN);
                    default:
                        return null;
                }
            }
            catch (IllegalArgumentException exception) {
                return null;
            }
        }

        @Override
        public void set(String name, Object value)
        {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean has(String name)
        {
            try {
                Variable.valueOf(name);
                return true;
            }
            catch (IllegalArgumentException exception) {
                return false;
            }
        }

        /**
         * @param groupName
         * @return set of user-ids which are in group with given {@code groupName}
         */
        public Set<String> group(String groupName)
        {
            String groupId = authorization.getGroupIdByName(groupName);
            return authorization.listGroupUserIds(groupId).getUserIds();
        }
    }

    /**
     * Type of possible variables which can be used in {@link AuthorizationExpression}s.
     */
    private enum Variable
    {
        id,
        firstName,
        lastName,
        email,
        organization,
        provider,
        loa
    }
}
