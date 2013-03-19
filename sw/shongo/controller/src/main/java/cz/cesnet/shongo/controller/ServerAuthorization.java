package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonFaultSet;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
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
    private String testingAccessToken;

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

        authorizationServer = configuration.getString(Configuration.SECURITY_AUTHORIZATION_SERVER);
        authorizationServerHeader = "id=testclient;secret=12345";
        if (authorizationServer == null) {
            throw new IllegalStateException("Authorization server is not set in the configuration.");
        }
        testingAccessToken = configuration.getString(Configuration.SECURITY_TESTING_ACCESS_TOKEN);

        // Create http client
        httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
    }

    /**
     * @param testingAccessToken sets the {@link #testingAccessToken}
     */
    public void setTestingAccessToken(String testingAccessToken)
    {
        this.testingAccessToken = testingAccessToken;
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
    protected String onValidate(SecurityToken securityToken) throws FaultException
    {
        // Always allow testing access token
        if (testingAccessToken != null && securityToken.getAccessToken().equals(testingAccessToken)) {
            logger.debug("Access token '{}' is valid for testing.", securityToken.getAccessToken());
            return ROOT_USER_ID;
        }
        return super.onValidate(securityToken);
    }

    @Override
    protected UserInformation onGetUserInformationByAccessToken(String accessToken) throws FaultException
    {
        // Testing security token represents root user
        if (testingAccessToken != null && accessToken.equals(testingAccessToken)) {
            return ROOT_USER_INFORMATION;
        }

        Exception errorException = null;
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
                throw new FaultException(CommonFaultSet.createSecurityErrorFault(
                        String.format("Retrieving user information by access token failed: %s, %s",
                                jsonNode.get("error").getTextValue(),
                                jsonNode.get("error_description").getTextValue())));
            }
        }
        catch (Exception exception) {
            errorException = exception;
        }
        // Handle error
        throw new FaultException(errorException,
                CommonFaultSet.createSecurityErrorFault("Retrieving user information by access token failed."));
    }

    private JsonNode readJson(HttpEntity httpEntity) throws FaultException
    {
        try {
            InputStream inputStream = httpEntity.getContent();
            try {
                return jsonMapper.readTree(inputStream);
            }
            catch (Exception exception) {
                throw new FaultException("Reading JSON failed.", exception);
            }
            finally {
                inputStream.close();
            }
        }
        catch (EOFException exception) {
            throw new FaultException("JSON is empty.", exception);
        }
        catch (IOException exception) {
            throw new FaultException("Reading JSON failed.", exception);
        }
    }

    @Override
    protected UserInformation onGetUserInformationByUserId(String userId) throws FaultException
    {
        Exception errorException = null;
        try {
            HttpGet httpGet = new HttpGet(getUserServiceUrl() + "/user/" + userId);
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JsonNode jsonNode = readJson(response.getEntity());
                return createUserInformationFromData(jsonNode);
            }
        }
        catch (Exception exception) {
            errorException = exception;
        }
        // Handle error
        throw new FaultException(errorException,
                CommonFaultSet.createSecurityErrorFault("Retrieving user information by user-id failed."));
    }

    @Override
    protected Collection<UserInformation> onListUserInformation() throws FaultException
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
        }
        catch (Exception exception) {
            errorException = exception;
        }
        // Handle error
        throw new FaultException(errorException,
                CommonFaultSet.createSecurityErrorFault("Retrieving user information failed."));
    }

    @Override
    protected AclRecord onCreateAclRecord(String userId, EntityIdentifier entityId, Role role) throws FaultException
    {
        // TODO: create ACL in authorization server

        return new AclRecord(userId, entityId, role);
    }

    @Override
    protected void onDeleteAclRecord(AclRecord aclRecord) throws FaultException
    {
        // TODO: delete ACL in authorization server
    }

    @Override
    protected AclRecord onGetAclRecord(String aclRecordId) throws FaultException
    {
        // TODO: get ACL from authorization server

        return CommonFaultSet.throwSecurityErrorFault("ACL not found.");
    }

    @Override
    protected Collection<AclRecord> onListAclRecords(String userId, EntityIdentifier entityId, Role role)
            throws FaultException
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
            throw new FaultException(exception);
        }
        List<AclRecord> aclRecords = new LinkedList<AclRecord>();
        for (JsonNode acl : performGetAuthorizationRequest(uri).get("acls")) {
            AclRecord aclRecord = createAclRecordFromData(acl);
            aclRecords.add(aclRecord);
        }
        return aclRecords;
    }

    private AclRecord createAclRecordFromData(JsonNode data) throws FaultException
    {
        String id = data.get("id").getTextValue();
        String userId = data.get("user_id").getTextValue();
        EntityIdentifier entityId = EntityIdentifier.parse(data.get("resource_id").getTextValue());
        Role role = Role.forId(data.get("role_id").getTextValue());
        return new AclRecord(id, userId, entityId, role);
    }

    private JsonNode performGetAuthorizationRequest(URI uri) throws FaultException
    {
        try {
            HttpGet httpGet = new HttpGet(uri);
            httpGet.setHeader("Accept", "application/hal+json");
            httpGet.setHeader("Authorization", authorizationServerHeader);

            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return readJson(response.getEntity()).get("_embedded");
            }
            else {
                JsonNode jsonNode = readJson(response.getEntity());
                throw new FaultException(CommonFaultSet.createSecurityErrorFault(
                        String.format("Authorization request failed: %s, %s",
                                jsonNode.get("title").getTextValue(),
                                jsonNode.get("detail").getTextValue())));
            }
        }
        catch (Exception exception) {
            throw new FaultException(exception, CommonFaultSet.createSecurityErrorFault(
                    String.format("Authorization request failed. %s", exception.getMessage())));
        }
    }

    /**
     * @param data from authorization server
     * @return {@link UserInformation}
     */
    private static UserInformation createUserInformationFromData(JsonNode data)
    {
        if (!data.has("id")) {
            throw new IllegalStateException("User information must contains identifier.");
        }
        if (!data.has("given_name") || !data.has("family_name")) {
            throw new IllegalStateException("User information must contains given and family name.");
        }
        UserInformation userInformation = new UserInformation();
        userInformation.setUserId(data.get("id").getTextValue());
        userInformation.setFirstName(data.get("given_name").getTextValue());
        userInformation.setLastName(data.get("family_name").getTextValue());

        if (data.has("original_id")) {
            userInformation.setEduPersonPrincipalName(data.get("original_id").getTextValue());
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
