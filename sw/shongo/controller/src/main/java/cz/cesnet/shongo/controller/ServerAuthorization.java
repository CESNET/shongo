package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
    private static final String AUTHENTICATION_SERVICE_PATH = "/authn/oic";

    /**
     * Authorization service path in auth-server.
     */
    private static final String AUTHORIZATION_SERVICE_PATH = "/authz/rest";

    /**
     * User web service path in auth-server.
     */
    private static final String USER_SERVICE_PATH = "/perun/resource";

    /**
     * Access token which won't be verified and can be used for testing purposes.
     */
    private String rootAccessToken;

    /**
     * URL to authorization server.
     */
    private String authorizationServer;

    /**
     * URL to authorization server.
     */
    private String authorizationServerHeader;

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
     * @param configuration to load authorization configuration from
     */
    private ServerAuthorization(Configuration configuration)
    {
        super(configuration);

        // Debug HTTP requests
        //System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        //System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        //System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
        //System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

        authorizationServer = configuration.getString(Configuration.SECURITY_SERVER);
        logger.info("Using authorization server '{}'.", authorizationServer);
        authorizationServerHeader = "id=testclient;secret=12345";
        if (authorizationServer == null) {
            throw new IllegalStateException("Authorization server is not set in the configuration.");
        }
        rootAccessToken = configuration.getString(Configuration.SECURITY_ROOT_ACCESS_TOKEN);

        // Create http client
        httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
    }

    /**
     * @param rootAccessToken sets the {@link #rootAccessToken}
     */
    public void setRootAccessToken(String rootAccessToken)
    {
        this.rootAccessToken = rootAccessToken;
    }

    /**
     * @return url to authentication service in auth-server
     */
    private String getAuthenticationUrl()
    {
        return authorizationServer + AUTHENTICATION_SERVICE_PATH;
    }

    /**
     * @return url to authorization service in auth-server
     */
    private String getAuthorizationUrl()
    {
        return authorizationServer + AUTHORIZATION_SERVICE_PATH;
    }

    /**
     * @return url to user service in auth-server
     */
    private String getUserServiceUrl()
    {
        return authorizationServer + USER_SERVICE_PATH;
    }

    @Override
    protected String onValidate(SecurityToken securityToken)
    {
        // Always allow testing access token
        if (rootAccessToken != null && securityToken.getAccessToken().equals(rootAccessToken)) {
            logger.trace("Access token '{}' is valid for testing.", securityToken.getAccessToken());
            return ROOT_USER_ID;
        }
        return super.onValidate(securityToken);
    }

    @Override
    protected UserInformation onGetUserInformationByAccessToken(String accessToken)
    {
        // Testing security token represents root user
        if (rootAccessToken != null && accessToken.equals(rootAccessToken)) {
            return ROOT_USER_INFORMATION;
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
                return createUserInformationFromData(jsonNode);
            }
            else {
                JsonNode jsonNode = readJson(response.getEntity());
                errorReason = String.format("%s, %s",
                        jsonNode.get("error").getTextValue(), jsonNode.get("error_description").getTextValue());
            }
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
    protected UserInformation onGetUserInformationByUserId(String userId)
    {
        Exception errorException = null;
        try {
            HttpGet httpGet = new HttpGet(getUserServiceUrl() + "/user/" + userId);
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JsonNode jsonNode = readJson(response.getEntity());
                return createUserInformationFromData(jsonNode);
            }
            else {
                readContent(response.getEntity());
            }
        }
        catch (Exception exception) {
            errorException = exception;
        }
        // Handle error
        throw new RuntimeException("Retrieving user information by user-id '" + userId + "' failed.", errorException);
    }

    @Override
    protected Collection<UserInformation> onListUserInformation()
    {
        Exception errorException = null;
        try {
            HttpGet httpGet = new HttpGet(getUserServiceUrl() + "/user");
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JsonNode jsonNode = readJson(response.getEntity());
                List<UserInformation> userInformationList = new LinkedList<UserInformation>();
                for (JsonNode childJsonNode : jsonNode) {
                    UserInformation userInformation = createUserInformationFromData(childJsonNode);
                    userInformationList.add(userInformation);
                }
                return userInformationList;
            }
            else {
                readContent(response.getEntity());
            }
        }
        catch (Exception exception) {
            errorException = exception;
        }
        // Handle error
        throw new RuntimeException("Retrieving user information failed.", errorException);
    }

    @Override
    protected void onPropagateAclRecordCreation(AclRecord aclRecord)
    {
        String userId = aclRecord.getUserId();
        EntityIdentifier entityId = aclRecord.getEntityId();
        Role role = aclRecord.getRole();

        StringEntity httpEntity;
        try {
            Map<String, String> data = new HashMap<String, String>();
            data.put("user_id", userId);
            data.put("resource_id", entityId.toId());
            data.put("role_id", role.getId());
            String jsonData = jsonMapper.writeValueAsString(data);
            httpEntity = new StringEntity(jsonData);
        }
        catch (IOException exception) {
            throw new RuntimeException("Propagation failed", exception);
        }
        HttpPost httpPost = new HttpPost(getAuthorizationUrl() + "/acl");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setHeader("Authorization", authorizationServerHeader);
        httpPost.setHeader("Accept", "application/hal+json");
        httpPost.setEntity(httpEntity);
        try {
            HttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                JsonNode acl = readJson(response.getEntity());

                // TODO: check returned ACL

                Controller.loggerAcl.info("Propagated ACL creation (id: {}, user: {}, entity: {}, role: {})",
                        new Object[]{aclRecord.getId(), userId, entityId, role});
            }
            else {
                handleAuthorizationRequestError(response);
            }
        }
        catch (Exception exception) {
            handleAuthorizationRequestError(exception);
        }
    }

    @Override
    protected void onPropagateAclRecordDeletion(AclRecord aclRecord)
    {
        for (String aclRecordId : listAclRecords(aclRecord.getUserId(), aclRecord.getEntityId(), aclRecord.getRole())) {
            HttpDelete httpDelete = new HttpDelete(getAuthorizationUrl() + "/acl/" + aclRecordId);
            httpDelete.setHeader("Authorization", authorizationServerHeader);
            try {
                HttpResponse response = httpClient.execute(httpDelete);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    readContent(response.getEntity());

                    Controller.loggerAcl.info("Propagated ACL deletion (id: {}, user: {}, entity: {}, role: {})",
                            new Object[]{aclRecord.getId(), aclRecord.getUserId(), aclRecord.getEntityId(),
                                    aclRecord.getRole()});
                }
                else {
                    handleAuthorizationRequestError(response);
                }
            }
            catch (Exception exception) {
                handleAuthorizationRequestError(exception);
            }
        }
    }

    /**
     * @param userId
     * @param entityId
     * @param role
     * @return identifiers of ACL records for given parameters
     */
    protected Collection<String> listAclRecords(String userId, EntityIdentifier entityId, Role role)
    {
        URI uri;
        try {
            URIBuilder uriBuilder = new URIBuilder(getAuthorizationUrl() + "/acl");
            if (userId != null) {
                uriBuilder.setParameter("user_id", userId);
            }
            if (entityId != null) {
                uriBuilder.setParameter("resource_id", entityId.toId());
            }
            else {
                uriBuilder.setParameter("resource_id", "shongo:" + Domain.getLocalDomainName() + ":*:*");
            }
            if (role != null) {
                uriBuilder.setParameter("role_id", role.getId());
            }
            uri = uriBuilder.build();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Authorization", authorizationServerHeader);
        httpGet.setHeader("Accept", "application/hal+json");
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JsonNode result = readJson(response.getEntity());
                List<String> aclRecordIds = new LinkedList<String>();
                for (JsonNode acl : result.get("_embedded").get("acls")) {
                    aclRecordIds.add(acl.get("id").getTextValue());
                }
                return aclRecordIds;
            }
            else {
                return handleAuthorizationRequestError(response);
            }
        }
        catch (Exception exception) {
            return handleAuthorizationRequestError(exception);
        }
    }

    /**
     * @param data from authorization server
     * @return {@link UserInformation}
     */
    private static UserInformation createUserInformationFromData(JsonNode data)
    {
        if (!data.has("id")) {
            throw new IllegalArgumentException("User information must contains identifier.");
        }
        if (!data.has("given_name") || !data.has("family_name")) {
            throw new IllegalArgumentException("User information must contains given and family name.");
        }
        UserInformation userInformation = new UserInformation();
        userInformation.setUserId(data.get("id").getTextValue());
        userInformation.setFirstName(data.get("given_name").getTextValue());
        userInformation.setLastName(data.get("family_name").getTextValue());

        if (data.has("original_id")) {
            userInformation.setOriginalId(data.get("original_id").getTextValue());
        }
        if (data.has("organization")) {
            userInformation.setOrganization(data.get("organization").getTextValue());
        }
        if (data.has("email")) {
            String emails = data.get("email").getTextValue();
            if (emails != null) {
                for (String email : emails.split(";")) {
                    if (!email.isEmpty()) {
                        userInformation.addEmail(email);
                    }
                }
            }
        }
        return userInformation;
    }

    /**
     * @param httpEntity to be read
     * @return {@link JsonNode} from given {@code httpEntity}
     */
    private JsonNode readJson(HttpEntity httpEntity)
    {
        try {
            InputStream inputStream = httpEntity.getContent();
            try {
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
        throw new RuntimeException(String.format("Authorization request failed: %s, %s",
                jsonNode.get("title").getTextValue(),
                jsonNode.get("detail").getTextValue()));
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
     * @return new instance of {@link ServerAuthorization}
     * @throws IllegalStateException when other {@link Authorization} already exists
     */
    public static ServerAuthorization createInstance(Configuration configuration) throws IllegalStateException
    {
        ServerAuthorization serverAuthorization = new ServerAuthorization(configuration);
        Authorization.setInstance(serverAuthorization);
        return serverAuthorization;
    }
}
