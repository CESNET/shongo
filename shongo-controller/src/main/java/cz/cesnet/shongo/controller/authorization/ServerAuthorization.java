package cz.cesnet.shongo.controller.authorization;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.report.ReportRuntimeException;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.ws.commons.util.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.persistence.EntityManagerFactory;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

/**
 * Provides methods for performing authentication and authorization.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ServerAuthorization extends Authorization
{
    private static Logger logger = LoggerFactory.getLogger(ServerAuthorization.class);

    /**
     * Authentication service path in auth-server.
     */
    private static final String AUTHENTICATION_SERVICE_PATH = "/oidc";

    /**
     * User web service path in auth-server.
     */
    private static final String USER_SERVICE_PATH = "/perun/users";

    /**
     * User principal web service path in auth-server.
     */
    private static final String PRINCIPAL_SERVICE_PATH = "/perun/principal";

    /**
     * Groups web service path in auth-server.
     */
    private static final String GROUP_SERVICE_PATH = "/perun/groups";

    /**
     * Group einfra prefix from oidc claims.
     */
    private static final String GROUP_OIDC_PREFIX = "urn:geant:cesnet.cz:group:einfra:";

    /**
     * Group projects shongo prefix from oidc claims.
     */
    private static final String GROUP_OIDC_PROJECTS_SHONGO_PREFIX = "einfra:projects:shongo";

    /**
     * Group projects shongo regexp from perun LDAP.
     */
    private static final String GROUP_LDAP_PROJECTS_SHONGO_REGEX = "einfra:projects:shongo:(system|users):(.*)";

    /**
     * Group fragment suffix from oidc claims.
     */
    private static final String GROUP_OIDC_FRAGMENT = "#";

    private static final String PERUN_SOURCE_CESNET_IDP_PATTERN = "https://login.cesnet.cz/idp/";

    /**
     * @see cz.cesnet.shongo.controller.ControllerConfiguration
     */
    private ControllerConfiguration configuration;

    /**
     * Access token which won't be verified and can be used for testing purposes.
     */
    private String rootAccessToken;

    /**
     * URL to authorization server.
     */
    private String authorizationServer;

    /**
     * URL to LDAP authorization server.
     */
    private String ldapAuthorizationServer;

    /**
     * Authorization header for requests.
     */
    private String requestAuthorizationHeader;

    /**
     * {@link HttpClient} for performing auth-server requests.
     */
    private HttpClient httpClient;

    /**
     * @see ObjectMapper
     */
    private ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Constructor.
     *
     * @param configuration        to load authorization configuration from
     * @param entityManagerFactory
     */
    private ServerAuthorization(ControllerConfiguration configuration, EntityManagerFactory entityManagerFactory)
    {
        super(configuration, entityManagerFactory);

        // Debug HTTP requests
        //System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        //System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        //System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
        //System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

        this.configuration = configuration;

        // Authorization server
        authorizationServer = configuration.getString(ControllerConfiguration.SECURITY_SERVER);
        if (authorizationServer == null) {
            throw new IllegalStateException("Authorization server is not set in the configuration.");
        }
        logger.info("Using authorization server '{}'.", authorizationServer);

        ldapAuthorizationServer = configuration.getString(ControllerConfiguration.SECURITY_LDAP_SERVER);
        if (ldapAuthorizationServer == null) {
            throw new IllegalStateException("LDAP authorization server is not set in the configuration.");
        }

        // Authorization header

        String clientId = configuration.getString(ControllerConfiguration.SECURITY_CLIENT_ID);
        String clientSecret = configuration.getString(ControllerConfiguration.SECURITY_CLIENT_SECRET);
        String clientAuthorization = clientId + ":" + clientSecret;
        byte[] bytes = clientAuthorization.getBytes();
        requestAuthorizationHeader = "Basic " + Base64.encode(bytes, 0, bytes.length, 0, "");

        // Create http client
        httpClient = ConfiguredSSLContext.getInstance().createHttpClient();

        initialize();
    }

    /**
     * Initialize {@link #rootAccessToken}.
     */
    public void initRootAccessToken()
    {
        // Root access token
        rootAccessToken = new BigInteger(160, new SecureRandom()).toString(16);
        String rootAccessTokenFile = configuration.getString(ControllerConfiguration.SECURITY_ROOT_ACCESS_TOKEN_FILE);
        if (rootAccessTokenFile != null) {
            writeRootAccessToken(rootAccessTokenFile, rootAccessToken);
        }
        administrationModeByAccessToken.put(rootAccessToken, AdministrationMode.ADMINISTRATOR);
    }

    /**
     * @return url to authentication service in auth-server
     */
    private String getAuthenticationUrl()
    {
        return authorizationServer + AUTHENTICATION_SERVICE_PATH;
    }
    // ok
    @Override
    protected UserInformation onValidate(SecurityToken securityToken)
    {
        // Always allow testing access token
        if (rootAccessToken != null && securityToken.getAccessToken().equals(rootAccessToken)) {
            logger.trace("Access token '{}' is valid for testing.", securityToken.getAccessToken());
            return ROOT_USER_DATA.getUserInformation();
        }
        return super.onValidate(securityToken);
    }
    @Override
    protected UserData onGetUserDataByAccessToken(String accessToken)
            throws ControllerReportSet.UserNotExistsException
    {
        // Testing security token represents root user
        if (rootAccessToken != null && accessToken.equals(rootAccessToken)) {
            return ROOT_USER_DATA;
        }

        Exception errorException = null;
        String errorReason = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(getAuthenticationUrl() + "/userinfo");
            uriBuilder.setParameter("schema", "openid");
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            httpGet.setHeader("Authorization", "Bearer " + accessToken);
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JsonNode jsonNode = readJson(response.getEntity());
                if (jsonNode == null) {
                    throw new ControllerReportSet.UserNotExistsException(accessToken);
                }
                return createUserDataFromWebServiceData(jsonNode);
            }
            else {
                JsonNode jsonNode = readJson(response.getEntity());
                if (jsonNode != null) {
                    String error = jsonNode.get("error").asText();
                    String errorDescription = jsonNode.get("error_description").asText();
                    if (error.contains("invalid_token")) {
                        throw new ControllerReportSet.SecurityInvalidTokenException(accessToken);
                    }
                    errorReason = String.format("%s, %s", error, errorDescription);
                }
                else {
                    errorReason = "unknown";
                }
            }
        }
        catch (ControllerReportSet.SecurityInvalidTokenException exception) {
            throw exception;
        }
        catch (ControllerReportSet.UserNotExistsException exception) {
            throw exception;
        }
        catch (Exception exception) {
            errorException = exception;
        }
        // Handle error
        String errorMessage = String.format("Retrieving user information by access token '%s' failed.", accessToken);
        if (errorReason != null) {
            errorMessage += " " + errorReason;
        }
        throw new RuntimeException(errorMessage, errorException);
    }
    @Override
    protected UserData onGetUserDataByUserId(final String userId) throws ControllerReportSet.UserNotExistsException {
        String filter = "eduPersonPrincipalNames=" + userId;
        SearchControls ctrls = new SearchControls();
        ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        ctrls.setCountLimit(1);

        DirContext ctx = null;
        NamingEnumeration results = null;
        UserData userData;

        try {
            ctx = getLdapContext();
            results = ctx.search("", filter, ctrls);
            if (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                userData = createUserDataFromLdapData(result);
            } else {
                throw new ControllerReportSet.UserNotExistsException(userId);
            }
        } catch (NamingException e) {
            throw new ControllerReportSet.UserNotExistsException(userId);
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (ctx != null) {
                    ctx.close();
                }
            }
            catch (Exception ignored) {
                // Ignore.
            }
        }
        return userData;
    }

    @Override
    protected String onGetUserIdByPrincipalName(final String principalName)
            throws ControllerReportSet.UserNotExistsException
    {
        if (principalName.endsWith("@einfra.cesnet.cz")) {
            return principalName;
        } else {
            // Try looking in LDAP for user
            for (UserData userData : onListUserData(Collections.singleton(principalName), null)) {
                return userData.getUserId();
            }
            throw new ControllerReportSet.UserNotExistsException(principalName);
        }
    }

    @Override
    protected Collection<UserData> onListUserData(final Set<String> filterUserIds, String search)
    {
        StringBuilder filter = new StringBuilder();
        SearchControls ctrls = new SearchControls();
        ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        DirContext ctx = null;
        NamingEnumeration results = null;
        List<UserData> userDataList = new LinkedList<>();

        if (filterUserIds != null && filterUserIds.size() > 0) {
            filter.append("(|");
            for (String id : filterUserIds) {
                filter.append("(eduPersonPrincipalNames=" + id + ")");
            }
            filter.append(")");
        } else if (search != null) {
            filter.append("(&(displayName=*" + search + "*)(eduPersonPrincipalNames=*))");
        } else {
            filter.append("objectClass=*");
        }

        try {
            ctx = getLdapContext();
            results = ctx.search("", filter.toString(), ctrls);
            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                // An alumn user
                if (result.getAttributes().get("eduPersonPrincipalNames") != null) {
                    UserData userData = createUserDataFromLdapData(result);
                    if (userData != null) {
                        userDataList.add(userData);
                    }
                }
            }
        } catch (NamingException e) {
            throw new CommonReportSet.UnknownErrorException(e, "Unable to list user data from LDAP.");
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (ctx != null) {
                    ctx.close();
                }
            }
            catch (Exception ignored) {
                // Ignore.
            }
        }
        return userDataList;
    }

    @Override
    protected Group onGetGroup(final String groupId)
    {

        String filter = "perunUniqueGroupName=" + groupId;
        SearchControls ctrls = new SearchControls();
        ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        ctrls.setCountLimit(1);

        DirContext ctx = null;
        NamingEnumeration results = null;
        Group group;

        try {
            ctx = getLdapContext();
            results = ctx.search("", filter, ctrls);
            if (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                group = createGroupFromLdapData(result);
            } else {
                throw new ControllerReportSet.GroupNotExistsException(groupId);
            }
        } catch (NamingException e) {
            throw new ControllerReportSet.GroupNotExistsException(groupId);
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (ctx != null) {
                    ctx.close();
                }
            }
            catch (Exception ignored) {
                // Ignore.
            }
        }
        return group;
    }

    @Override
    public List<Group> onListGroups(Set<String> filterGroupIds, Set<Group.Type> filterGroupTypes)
    {

        StringBuilder filter = new StringBuilder();
        SearchControls ctrls = new SearchControls();
        ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        DirContext ctx = null;
        NamingEnumeration results = null;
        List<Group> groupsList = new LinkedList<>();

        filter.append("(|");
        if (filterGroupIds != null && filterGroupIds.size() > 0) {
            for (String id : filterGroupIds) {
                filter.append("(perunUniqueGroupName=" + id + ")");
            }
        } else if (filterGroupTypes != null) {
            for (Group.Type type : filterGroupTypes) {
                filter.append("(perunUniqueGroupName=" + GROUP_OIDC_PROJECTS_SHONGO_PREFIX + ":" + type + "*)");
            }
        } else {
            filter.append("(perunUniqueGroupName=" + GROUP_OIDC_PROJECTS_SHONGO_PREFIX + ":*)");
        }
        filter.append(")");

        try {
            ctx = getLdapContext();
            results = ctx.search("", filter.toString(), ctrls);
            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                Group group = createGroupFromLdapData(result);
                if (group != null) {
                    groupsList.add(group);
                }
            }
        } catch (NamingException e) {
            throw new CommonReportSet.UnknownErrorException(e, "Unable to list groups data from LDAP.");
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (ctx != null) {
                    ctx.close();
                }
            }
            catch (Exception ignored) {
                // Ignore.
            }
        }
        return groupsList;

    }

    @Override
    public Set<String> onListGroupUserIds(final String groupId)
    {

        Group group = onGetGroup(groupId);

        String filter = "memberOf=" + group.getSecondaryId();
        SearchControls ctrls = new SearchControls();
        ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        DirContext ctx = null;
        NamingEnumeration results = null;
        List<UserData> userDataList = new LinkedList<>();

        try {
            ctx = getLdapContext();
            results = ctx.search("", filter, ctrls);
            while (results.hasMore()) {
            SearchResult result = (SearchResult) results.next();
                UserData userData = createUserDataFromLdapData(result);
                if (userData != null) {
                    userDataList.add(userData);
                }
            }
        } catch (NamingException e) {
            throw new CommonReportSet.UnknownErrorException(e, "Unable to list user data from LDAP.");
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (ctx != null) {
                    ctx.close();
                }
            }
            catch (Exception ignored) {
                // Ignore.
            }
        }

        Set<String> userIds = new HashSet<String>();
        for (UserData user : userDataList) {
            userIds.add(user.getUserId());
        }
        return userIds;
    }

    @Override
    protected Set<String> onListUserGroupIds(String userId)
    {
        UserData userData = onGetUserDataByUserId(userId);
        String filter = "uniqueMember=" + userData.getSecondaryId();
        Set<String> userGroupIds = new HashSet<>();

        SearchControls ctrls = new SearchControls();
        ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        DirContext ctx = null;
        NamingEnumeration results = null;


        try {
            ctx = getLdapContext();
            results = ctx.search("", filter, ctrls);
            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                Group group = createGroupFromLdapData(result);
                if (group != null) {
                    userGroupIds.add(group.getId());
                }
            }
        } catch (NamingException e) {
            throw new CommonReportSet.UnknownErrorException(e, "Unable to list data from LDAP.");
        } finally {
            try {
                if (results != null) {
                    results.close();
                }
                if (ctx != null) {
                    ctx.close();
                }
            }
            catch (Exception ignored) {
                // Ignore.
            }
        }

        return userGroupIds;
    }

    @Override
    public String onCreateGroup(final Group group)
    {
        JsonNode groupData = createWebServiceDataFromGroup(group);
        String groupId = performPostRequest(authorizationServer + GROUP_SERVICE_PATH, groupData,
                "Creating group failed",
                new RequestHandler<String>()
                {
                    @Override
                    public String success(JsonNode data)
                    {
                        return data.get("id").asText();
                    }

                    @Override
                    public void error(StatusLine statusLine, String detail)
                    {
                        if (detail.contains("GroupExistsException")) {
                            throw new ControllerReportSet.GroupAlreadyExistsException(group.getName());
                        }
                    }
                });
        group.setId(groupId);
        modifyGroupAdministrators(group);
        return groupId;
    }

    @Override
    protected void onModifyGroup(final Group group)
    {
        JsonNode groupData = createWebServiceDataFromGroup(group);
        String groupId = group.getId();
        performPatchRequest(authorizationServer + GROUP_SERVICE_PATH + "/" + groupId, groupData,
                "Creating group failed",
                new RequestHandler<String>()
                {
                    @Override
                    public String success(JsonNode data)
                    {
                        return data.get("id").asText();
                    }

                    @Override
                    public void error(StatusLine statusLine, String detail)
                    {
                        if (detail.contains("GroupExistsException")) {
                            throw new ControllerReportSet.GroupAlreadyExistsException(group.getName());
                        }
                    }
                });
        modifyGroupAdministrators(group);
    }

    @Override
    public void onDeleteGroup(final String groupId)
    {
        performDeleteRequest(authorizationServer + GROUP_SERVICE_PATH + "/" + groupId,
                "Deleting group " + groupId + " failed",
                new RequestHandler<Object>()
                {
                    @Override
                    public void error(StatusLine statusLine, String detail)
                    {
                        if (detail.contains("GroupNotExistsException")) {
                            throw new ControllerReportSet.GroupNotExistsException(groupId);
                        }
                    }
                });
    }

    @Override
    public void onAddGroupUser(final String groupId, final String userId)
    {
        performPutRequest(authorizationServer + GROUP_SERVICE_PATH + "/" + groupId + "/users/" + userId,
                "Adding user " + userId + " to group " + groupId + " failed",
                new RequestHandler<Object>()
                {
                    @Override
                    public void error(StatusLine statusLine, String detail)
                    {
                        int statusCode = statusLine.getStatusCode();
                        if (statusCode == HttpStatus.SC_NOT_FOUND) {
                            if (detail.contains("User")) {
                                throw new ControllerReportSet.UserNotExistsException(userId);
                            }
                        }
                        if (detail.contains("AlreadyMemberException")) {
                            throw new ControllerReportSet.UserAlreadyInGroupException(groupId, userId);
                        }
                        else if (detail.contains("GroupNotExistsException")) {
                            throw new ControllerReportSet.GroupNotExistsException(groupId);
                        }
                    }
                });
    }

    @Override
    public void onRemoveGroupUser(final String groupId, final String userId)
    {
        performDeleteRequest(authorizationServer + GROUP_SERVICE_PATH + "/" + groupId + "/users/" + userId,
                "Removing user " + userId + " from group " + groupId + " failed",
                new RequestHandler<Object>()
                {
                    @Override
                    public void error(StatusLine statusLine, String detail)
                    {
                        int statusCode = statusLine.getStatusCode();
                        if (statusCode == HttpStatus.SC_NOT_FOUND) {
                            if (detail.contains("User")) {
                                throw new ControllerReportSet.UserNotExistsException(userId);
                            }
                        }
                        if (detail.contains("NotGroupMemberException")) {
                            throw new ControllerReportSet.UserNotInGroupException(groupId, userId);
                        }
                        else if (detail.contains("GroupNotExistsException")) {
                            throw new ControllerReportSet.GroupNotExistsException(groupId);
                        }
                    }
                });
    }

    /**
     * Creates context with pooled connection for LDAP.
     *
     * @return {@link DirContext}
     */
    private DirContext getLdapContext() throws NamingException {
        // LDAP client initialization
        String ldapClientDn = configuration.getString(ControllerConfiguration.SECURITY_LDAP_BINDDN );
        String ldapClientSecret = configuration.getString(ControllerConfiguration.SECURITY_LDAP_CLIENT_SECRET);
        Hashtable<String,String> env = new Hashtable <String,String>();
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.PROVIDER_URL, ldapAuthorizationServer);
        env.put(Context.SECURITY_PRINCIPAL, ldapClientDn);
        env.put(Context.SECURITY_CREDENTIALS, ldapClientSecret);
        return new InitialDirContext(env);
    }

    /**
     * Returns userData if parsing went well. If name is not set properly returns null.
     *
     * @return {@link UserData}
     */
    private UserData createUserDataFromLdapData(SearchResult result) throws NamingException {

        Attributes attributes = result.getAttributes();
        // Ignore if not set
        if (attributes.get("sn") == null || attributes.get("givenname") == null) {
            return null;
        }

        String userId = null;
        NamingEnumeration ne =  attributes.getAll();
        Set<String> principalNames = new HashSet<>();

        // Retrieve einfra id
        while (userId == null && ne.hasMore()) {
            Attribute attribute = (Attribute) ne.next();
            if (attribute.getID().equals("eduPersonPrincipalNames")) {
                NamingEnumeration values = attribute.getAll();
                while (values.hasMore()) {
                    String value = (String) values.next();
                    principalNames.add(value);
                    if (value.matches("(.*)@einfra.cesnet.cz") && userId == null) {
                        userId = value;
                    }
                }
            }
        }


        // Required fields
        if (userId == null) {
            throw new IllegalArgumentException("User data must contain valid user id.");
        }

        UserData userData = new UserData();
        UserInformation userInformation = userData.getUserInformation();
        userInformation.setUserId(userId);
        userInformation.setFirstName((String) attributes.get("givenName").get());
        userInformation.setLastName((String) attributes.get("sn").get());
        userInformation.setPrincipalNames(principalNames);

        if (attributes.get("o") != null) {
            String organization = (String) attributes.get("o").get();
            userInformation.setOrganization(organization);
        }
        if (attributes.get("mail") != null) {
            userInformation.setEmail((String) attributes.get("mail").get());
        }

        // Retrieve users dn within LDAP
        userData.setSecondaryId(result.getNameInNamespace());

        return userData;
    }

    /**
     * Returns userData if parsing went well.
     *
     * @return {@link Group}
     */
    private Group createGroupFromLdapData(SearchResult result) throws NamingException {

        Attributes attributes = result.getAttributes();
        if (attributes.get("perunUniqueGroupName") == null) {
            return null;
        }
        String perunUniqueGroupName = (String) attributes.get("perunUniqueGroupName").get();

        if (!perunUniqueGroupName.matches(GROUP_LDAP_PROJECTS_SHONGO_REGEX)) {
            return null;
        }
        String[] groupNameSplit = perunUniqueGroupName.split(":");
        String type = groupNameSplit[3];

        Group group = new Group();
        group.setId(perunUniqueGroupName);
        group.setType(Group.Type.permissiveValueOf(type.toUpperCase()));
        if (attributes.get("description") != null) {
            group.setDescription((String) attributes.get("description").get());
        }
        group.setSecondaryId(result.getNameInNamespace());
        group.setName(groupNameSplit[4]);

        return group;
    }

    /**
     * @see #performRequest
     */
    private <T> T performGetRequest(String url, String description, RequestHandler<T> requestHandler)
    {
        HttpGet httpGet = new HttpGet(url);
        return performRequest(httpGet, description, requestHandler);
    }

    /**
     * @see #performRequest
     */
    private <T> T performPostRequest(String url, JsonNode content, String description, RequestHandler<T> requestHandler)
    {
        StringEntity entity;
        try {
            String json = content.toString();
            entity = new StringEntity(json);
        }
        catch (UnsupportedEncodingException exception) {
            throw new CommonReportSet.UnknownErrorException(exception, "Entity encoding failed");
        }
        entity.setContentType("application/json");
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        httpPost.setHeader("Content-Type", "application/json");
        return performRequest(httpPost, description, requestHandler);
    }

    /**
     * @see #performRequest
     */
    private void performPutRequest(String url, String description, RequestHandler requestHandler)
    {
        HttpPut httpPut = new HttpPut(url);
        performRequest(httpPut, description, requestHandler);
    }

    /**
     * @see #performRequest
     */
    private <T> T performPatchRequest(String url, JsonNode content, String description,
                                      RequestHandler<T> requestHandler)
    {
        StringEntity entity;
        try {
            String json = content.toString();
            entity = new StringEntity(json);
        }
        catch (UnsupportedEncodingException exception) {
            throw new CommonReportSet.UnknownErrorException(exception, "Entity encoding failed");
        }
        entity.setContentType("application/json");
        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setEntity(entity);
        httpPatch.setHeader("Content-Type", "application/json");
        return performRequest(httpPatch, description, requestHandler);
    }

    /**
     * @see #performRequest
     */
    private void performDeleteRequest(String url, String description, RequestHandler requestHandler)
    {
        HttpDelete httpDelete = new HttpDelete(url);
        performRequest(httpDelete, description, requestHandler);
    }

    /**
     * Perform given {@code httpRequest}.
     *
     * @param httpRequest    to be performed
     * @param description    for error reporting
     * @param requestHandler to handle response or error
     * @return result from given {@code requestHandler}
     */
    private <T> T performRequest(HttpRequestBase httpRequest, String description, RequestHandler<T> requestHandler)
    {
        try {
            httpRequest.addHeader("Authorization", requestAuthorizationHeader);
            httpRequest.setHeader("Accept", "application/hal+json");
            httpRequest.setHeader("Cache-Control", "no-cache");
            HttpResponse response = httpClient.execute(httpRequest);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_NO_CONTENT) {
                return null;
            }
            else if (statusCode >= HttpStatus.SC_OK && statusCode <= HttpStatus.SC_ACCEPTED) {
                JsonNode data = readJson(response.getEntity());
                if (data == null) {
                    data = jsonMapper.createObjectNode();
                }
                return requestHandler.success(data);
            }
            else {
                String content = readContent(response.getEntity());
                String detail = null;
                if (content != null && !content.isEmpty()) {
                    try {
                        JsonNode jsonNode = jsonMapper.readTree(content);
                        if (jsonNode.has("detail")) {
                            JsonNode detailNode = jsonNode.get("detail");
                            if (!detailNode.isNull()) {
                                detail = detailNode.asText();
                            }
                        }
                    }
                    catch (Exception exception) {
                        logger.warn("Cannot parse json: {}", content);
                        detail = content;
                    }
                }
                requestHandler.error(statusLine, (detail != null ? detail : ""));
                String error = description + ": " + statusLine.toString();
                if (detail != null) {
                    error += ": " + detail;
                }
                throw new CommonReportSet.UnknownErrorException(error);
            }
        }
        catch (ReportRuntimeException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new CommonReportSet.UnknownErrorException(exception, description + ".");
        }
        finally {
            httpRequest.releaseConnection();
        }
    }

    /**
     * @param httpEntity to be read
     * @return {@link JsonNode} from given {@code httpEntity}
     */
    private JsonNode readJson(HttpEntity httpEntity)
    {
        if (httpEntity.getContentLength() == 0) {
            return null;
        }
        try {
            InputStream inputStream = httpEntity.getContent();
            try {
                int available = inputStream.available();
                return jsonMapper.readTree(inputStream);
            }
            finally {
                inputStream.close();
            }
        }
        catch (EOFException exception) {
            throw new RuntimeException("JSON is empty.", exception);
        }
        catch (IOException exception) {
            throw new RuntimeException("Reading JSON failed.", exception);
        }
    }

    /**
     * Read all content from given {@code httpEntity}.
     *
     * @param httpEntity to be read
     */
    private String readContent(HttpEntity httpEntity)
    {
        if (httpEntity != null) {
            try {
                return EntityUtils.toString(httpEntity);
            }
            catch (IOException exception) {
                throw new RuntimeException("Reading content failed.", exception);
            }
        }
        return null;
    }

    /**
     * @param httpResponse to be handled
     * @throws RuntimeException is always thrown
     */
    private <T> T handleAuthorizationRequestError(HttpResponse httpResponse)
    {
        JsonNode jsonNode = readJson(httpResponse.getEntity());
        return handleAuthorizationRequestError(jsonNode);
    }

    /**
     * @param jsonNode to be handled
     * @throws RuntimeException is always thrown
     */
    private <T> T handleAuthorizationRequestError(JsonNode jsonNode)
    {
        String title = "unknown";
        String detail = "none";
        if (jsonNode != null) {
            title = jsonNode.get("title").asText();
            detail = jsonNode.get("detail").asText();
        }
        throw new RuntimeException(String.format("Authorization request failed: %s, %s", title, detail));
    }

    /**
     * @param exception to be handled
     * @throws RuntimeException is always thrown
     */
    private <T> T handleAuthorizationRequestError(Exception exception)
    {
        throw new RuntimeException(String.format("Authorization request failed. %s", exception.getMessage()));
    }

    /**
     * @param data from authorization server
     * @return {@link UserData}
     */
    private static UserData createUserDataFromWebServiceData(JsonNode data)
    {
        // Required fields
        if (!data.has("id")) {
            throw new IllegalArgumentException("User data must contain identifier.");
        }
        if (!data.has("first_name") || !data.has("last_name")) {
            throw new IllegalArgumentException("User data must contain given and family name.");
        }

        UserData userData = new UserData();

        // Common user data
        UserInformation userInformation = userData.getUserInformation();
        userInformation.setUserId(data.get("id").asText());
        userInformation.setFirstName(data.get("first_name").asText());
        userInformation.setLastName(data.get("last_name").asText());
        if (data.has("organization")) {
            JsonNode organization = data.get("organization");
            if (!organization.isNull()) {
                userInformation.setOrganization(organization.asText());
            }
        }
        if (data.has("mail")) {
            JsonNode email = data.get("mail");
            if (!email.isNull()) {
                userInformation.setEmail(email.asText());
            }
        }
        if (data.has("principal_names")) {
            Iterator<JsonNode> principalNameIterator = data.get("principal_names").elements();
            while (principalNameIterator.hasNext()) {
                JsonNode principalName = principalNameIterator.next();
                userInformation.addPrincipalName(principalName.asText());
            }
        }

        // Additional user data
        if (data.has("language")) {
            JsonNode language = data.get("language");
            if (!language.isNull()) {
                Locale locale = new Locale(language.asText());
                userData.setLocale(locale);
                userInformation.setLocale(locale.toString());
            }
        }
        if (data.has("timezone")) {
            JsonNode timezone = data.get("timezone");
            if (!timezone.isNull()) {
                DateTimeZone timeZone = DateTimeZone.forID(timezone.asText());
                userData.setTimeZone(timeZone);
                userInformation.setZoneInfo(timeZone.toString());
            }
        }
        if (data.has("zoneinfo")) {
            JsonNode timezone = data.get("zoneinfo");
            if (!timezone.isNull()) {
                DateTimeZone timeZone = DateTimeZone.forID(timezone.asText());
                userData.setTimeZone(timeZone);
                userInformation.setZoneInfo(timeZone.toString());
            }
        }

        if(data.has("edu_person_entitlements")) {
            Iterator<JsonNode> eduPersonEntitlementIterator = data.get("edu_person_entitlements").elements();
            while (eduPersonEntitlementIterator.hasNext()) {
                JsonNode eduPersonEntitlement = eduPersonEntitlementIterator.next();
                userInformation.addEduPersonEntitlement(eduPersonEntitlement.asText());
            }
        }

        // for AuthN Server v0.6.4 and newer
        if (data.has("authn_provider") && data.has("authn_instant") && data.has("loa")) {
            long instant = Long.valueOf(data.get("authn_instant").asText()) * 1000;
            DateTime dateTime = new DateTime(instant);
            userData.setUserAuthorizationData(new UserAuthorizationData(
                    data.get("authn_provider").asText(),
                    dateTime,
                    data.get("loa").asInt()));
        }
        // for AuthN Server v0.6.3 and older
        if (data.has("authentication_info")) {
            JsonNode authenticationInfo = data.get("authentication_info");
            if (authenticationInfo.has("provider") && authenticationInfo.has("loa")) {
                userData.setUserAuthorizationData(new UserAuthorizationData(
                    authenticationInfo.get("provider").asText(),
                    null,
                    authenticationInfo.get("loa").asInt()));
            }
        }

//        // only for Perun purpose, gets einfra id instead of perun id
//        if (data.has("sources")) {
//            JsonNode sources = data.get("sources");
//            if (sources.isArray()) {
//                for (JsonNode source : sources) {
//                    if (source.has("name")) {
//                        if (source.get("name").getTextValue().equals(PERUN_SOURCE_CESNET_IDP_PATTERN)) {
//                            if (source.has("login")) {
//                                userInformation.setUserId(source.get("login").getTextValue());
//                            }
//                        }
//                    }
//                }
//            }
//        }

        return userData;
    }

    /**
     * @param data from authorization server
     * @return {@link Group}
     */
    private static Group createGroupFromWebServiceData(JsonNode data)
    {
        // Required fields
        if (!data.has("id")) {
            throw new IllegalArgumentException("Group data must contain identifier.");
        }
        if (!data.has("type")) {
            throw new IllegalArgumentException("Group data must contain type.");
        }

        Group group = new Group();
        group.setId(data.get("id").asText());
        if (data.has("parent_group_id")) {
            JsonNode parentId = data.get("parent_group_id");
            if (!parentId.isNull()) {
                group.setParentGroupId(parentId.asText());
            }
        }
        group.setType(Group.Type.valueOf(data.get("type").asText().toUpperCase()));
        group.setName(data.get("name").asText());
        if (data.has("description")) {
            JsonNode description = data.get("description");
            if (!description.isNull()) {
                group.setDescription(description.asText());
            }
        }
        if (data.has("admins")) {
            Iterator<JsonNode> administratorIterator = data.get("admins").elements();
            while (administratorIterator.hasNext()) {
                JsonNode administrator = administratorIterator.next();
                if (!administrator.has("id")) {
                    throw new IllegalArgumentException("Group data administrator must contain identifier.");
                }
                group.addAdministrator(administrator.get("id").asText());
            }
        }
        return group;
    }

    /**
     * @param group
     * @return {@link JsonNode} from given {@code group}
     */
    private JsonNode createWebServiceDataFromGroup(Group group)
    {
        if (group.getType() == null) {
            throw new IllegalArgumentException("Group data must type.");
        }

        ObjectNode objectNode = jsonMapper.createObjectNode();
        if (group.getId() != null) {
            objectNode.put("id", group.getId());
        }
        objectNode.put("type", group.getType().toString().toLowerCase());
        objectNode.put("name", group.getName());
        objectNode.put("description", group.getDescription());
        if (group.getParentGroupId() != null) {
            objectNode.put("parent_group_id", group.getParentGroupId());
        }
        return objectNode;
    }

    /**
     * @param group for which the {@link Group#administrators} should be modified
     */
    private void modifyGroupAdministrators(final Group group)
    {
        final String groupId = group.getId();
        Set<String> groupAdministrators = group.getAdministrators();
        Set<String> existingGroupAdministrators = getGroupAdministrators(groupId);
        // Add administrators
        for (final String administrator : groupAdministrators) {
            if (existingGroupAdministrators.contains(administrator)) {
                // Administrator already exists
                continue;
            }
            // Create administrator
            String addUrl = authorizationServer + GROUP_SERVICE_PATH + "/" + groupId + "/admins/" + administrator;
            performPutRequest(addUrl,
                    "Adding administrator " + administrator + " to group " + groupId + " failed",
                    new RequestHandler<Object>()
                    {
                        @Override
                        public void error(StatusLine statusLine, String detail)
                        {
                            int statusCode = statusLine.getStatusCode();
                            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                                if (detail.contains("User")) {
                                    throw new ControllerReportSet.UserNotExistsException(administrator);
                                }
                            }
                            if (detail.contains("GroupNotExistsException")) {
                                throw new ControllerReportSet.GroupNotExistsException(groupId);
                            }
                        }
                    });
        }

        // Delete administrators
        for (final String administrator : existingGroupAdministrators) {
            if (groupAdministrators.contains(administrator)) {
                // Administrator should exist
                continue;
            }
            // Delete administrator
            String deleteUrl = authorizationServer + GROUP_SERVICE_PATH + "/" + groupId + "/admins/" + administrator;
            performDeleteRequest(deleteUrl,
                    "Deleting administrator " + administrator + " from group " + groupId + " failed",
                    new RequestHandler<Object>()
                    {
                        @Override
                        public void error(StatusLine statusLine, String detail)
                        {
                            int statusCode = statusLine.getStatusCode();
                            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                                if (detail.contains("User")) {
                                    throw new ControllerReportSet.UserNotExistsException(administrator);
                                }
                            }
                            if (detail.contains("GroupNotExistsException")) {
                                throw new ControllerReportSet.GroupNotExistsException(groupId);
                            }
                        }
                    });
        }
    }

    /**
     * @param groupId
     * @return set of user-ids for group administrators
     */
    private Set<String> getGroupAdministrators(String groupId)
    {
        String listUrl = authorizationServer + GROUP_SERVICE_PATH + "/" + groupId + "/admins";
        return performGetRequest(listUrl, "Retrieving administrators for group " + groupId + " failed",
                new RequestHandler<Set<String>>()
                {
                    @Override
                    public Set<String> success(JsonNode data)
                    {
                        Set<String> groupIds = new HashSet<String>();
                        if (data != null) {
                            Iterator<JsonNode> userIterator = data.get("_embedded").get("admins").elements();
                            while (userIterator.hasNext()) {
                                JsonNode groupNode = userIterator.next();
                                if (!groupNode.has("id")) {
                                    throw new IllegalStateException("Group must have identifier.");
                                }
                                groupIds.add(groupNode.get("id").asText());
                            }
                        }
                        return groupIds;
                    }
                });
    }

    /**
     * @return new instance of {@link ServerAuthorization}
     * @throws IllegalStateException when other {@link Authorization} already exists
     */
    public static ServerAuthorization createInstance(ControllerConfiguration configuration,
                                                     EntityManagerFactory entityManagerFactory) throws IllegalStateException
    {
        ServerAuthorization serverAuthorization = new ServerAuthorization(configuration, entityManagerFactory);
        Authorization.setInstance(serverAuthorization);
        return serverAuthorization;
    }

    /**
     * @param fileName    where to write
     * @param accessToken to be written
     */
    private static void writeRootAccessToken(String fileName, String accessToken)
    {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                if (file.createNewFile()) {
                    chmod(fileName, 0600);
                }
            }
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            try {
                output.write(accessToken);
            }
            finally {
                output.close();
            }
        }
        catch (IOException exception) {
            logger.error("Cannot write root access token to file " + fileName, exception);
        }
    }

    /**
     * @param fileName
     * @param mode
     * @return result of chmod
     */
    private static int chmod(String fileName, int mode)
    {
        try {
            Class<?> fspClass = Class.forName("java.util.prefs.FileSystemPreferences");
            Method chmodMethod = fspClass.getDeclaredMethod("chmod", String.class, Integer.TYPE);
            chmodMethod.setAccessible(true);
            return (Integer) chmodMethod.invoke(null, fileName, mode);
        }
        catch (Throwable throwable) {
            logger.error("Cannot chmod file " + fileName + " to mode " + mode, throwable);
            return -1;
        }
    }

    /**
     * Http request handler for {@link #performRequest}
     */
    private static abstract class RequestHandler<T>
    {
        /**
         * Handle HTTP json response.
         *
         * @param data
         * @return parsed json response
         */
        public T success(JsonNode data)
        {
            return null;
        }

        /**
         * Handle HTTP error.
         *
         * @param statusLine
         * @param detail
         */
        public void error(StatusLine statusLine, String detail)
        {
        }
    }
}
