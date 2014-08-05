package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.api.rpc.TypeConverterFactory;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AbstractRequest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.report.*;
import cz.cesnet.shongo.util.Timer;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.XmlRpcNotAuthorizedException;
import org.apache.xmlrpc.metadata.Util;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * {@link org.apache.xmlrpc.server.ReflectiveXmlRpcHandler} with conversion of {@link cz.cesnet.shongo.report.ApiFault} to {@link org.apache.xmlrpc.XmlRpcException}.
 */
public class RpcHandler implements XmlRpcHandler
{
    private static Logger logger = LoggerFactory.getLogger(RpcServer.class);

    /**
     * Unique request identifier generator.
     */
    private static Long lastRequestId = 0l;

    /**
     * @return new generated request identifier
     */
    public synchronized static Long getNewRequestId()
    {
        return ++lastRequestId;
    }

    private static class MethodData
    {
        final Method method;
        final TypeConverter[] typeConverters;
        final boolean debug;

        MethodData(Method pMethod, org.apache.xmlrpc.common.TypeConverterFactory pTypeConverterFactory)
        {
            method = pMethod;
            debug = pMethod.getAnnotation(AbstractServiceImpl.Debug.class) != null;
            Class[] paramClasses = method.getParameterTypes();
            typeConverters = new TypeConverter[paramClasses.length];
            if (pTypeConverterFactory instanceof TypeConverterFactory) {
                TypeConverterFactory typeConverterFactory =
                        (TypeConverterFactory) pTypeConverterFactory;
                Type[] genericParamTypes = method.getGenericParameterTypes();
                for (int i = 0; i < paramClasses.length; i++) {
                    typeConverters[i] = typeConverterFactory.getTypeConverter(paramClasses[i],
                            genericParamTypes[i]);
                }
            }
            else {
                for (int i = 0; i < paramClasses.length; i++) {
                    typeConverters[i] = pTypeConverterFactory.getTypeConverter(paramClasses[i]);
                }
            }
        }
    }

    private final AbstractReflectiveHandlerMapping mapping;
    private final MethodData[] methods;
    private final Class clazz;
    private final RequestProcessorFactoryFactory.RequestProcessorFactory requestProcessorFactory;

    public RpcHandler(AbstractReflectiveHandlerMapping pMapping,
            org.apache.xmlrpc.common.TypeConverterFactory pTypeConverterFactory,
            Class pClass, RequestProcessorFactoryFactory.RequestProcessorFactory pFactory, Method[] pMethods)
    {
        mapping = pMapping;
        clazz = pClass;
        methods = new MethodData[pMethods.length];
        requestProcessorFactory = pFactory;
        for (int i = 0; i < methods.length; i++) {
            methods[i] = new MethodData(pMethods[i], pTypeConverterFactory);
        }
    }

    private Object getInstance(XmlRpcRequest pRequest) throws XmlRpcException
    {
        return requestProcessorFactory.getRequestProcessor(pRequest);
    }

    public Object execute(XmlRpcRequest pRequest) throws XmlRpcException
    {
        AbstractReflectiveHandlerMapping.AuthenticationHandler authHandler = mapping.getAuthenticationHandler();
        if (authHandler != null && !authHandler.isAuthorized(pRequest)) {
            throw new XmlRpcNotAuthorizedException("Not authorized");
        }
        Object[] args = new Object[pRequest.getParameterCount()];
        for (int j = 0; j < args.length; j++) {
            args[j] = pRequest.getParameter(j);
        }
        Object instance = getInstance(pRequest);
        for (int i = 0; i < methods.length; i++) {
            MethodData methodData = methods[i];
            TypeConverter[] converters = methodData.typeConverters;
            if (args.length == converters.length) {
                boolean matching = true;
                for (int j = 0; j < args.length; j++) {
                    if (!converters[j].isConvertable(args[j])) {
                        matching = false;
                        break;
                    }
                }
                if (matching) {
                    for (int j = 0; j < args.length; j++) {
                        args[j] = converters[j].convert(args[j]);
                    }
                    return invoke(instance, methodData.method, args, methodData.debug);
                }
            }
        }
        throw new CommonReportSet.MethodNotDefinedException(
                clazz.getSimpleName() + "." + methods[0].method.getName() + "(" + Util.getSignature(args) + ")");
    }

    private Object invoke(Object pInstance, Method pMethod, Object[] pArgs, boolean debug) throws XmlRpcException
    {
        Timer requestTimer = new Timer();
        requestTimer.start();

        // Prepare request context
        RpcRequestContext requestContext = new RpcRequestContext();
        requestContext.requestId = getNewRequestId();
        requestContext.methodName = ((Service)pInstance).getServiceName() + "." + pMethod.getName();
        requestContext.arguments = pArgs;
        requestContext.userInformation = null;

        // Get user information from the first SecurityToken argument
        if (pArgs.length > 0) {
            SecurityToken securityToken = null;
            if (pArgs[0] instanceof SecurityToken) {
                securityToken = (SecurityToken) pArgs[0];
            }
            else if (pArgs[0] instanceof AbstractRequest) {
                securityToken = ((AbstractRequest) pArgs[0]).getSecurityToken();
            }
            if (securityToken != null) {
                try {
                    requestContext.userInformation = Authorization.getInstance().getUserInformation(securityToken);
                }
                catch (Exception exception) {
                    logger.warn("Get user information by access token '{}' failed.", securityToken.getAccessToken());
                }
            }
        }

        // Log request start
        String logMessage;
        Object[] logMessageParameters;
        if (requestContext.userInformation != null) {
            logMessage = "Request:{} {} by {} (userId: {})";
            logMessageParameters = new Object[]{requestContext.requestId, requestContext.methodName,
                    requestContext.userInformation.getFullName(), requestContext.userInformation.getUserId()};
        }
        else {
            logMessage = "Request:{} {}";
            logMessageParameters = new Object[]{requestContext.requestId, requestContext.methodName};
        }
        if (debug) {
            Controller.loggerApi.debug(logMessage, logMessageParameters);
        }
        else {
            Controller.loggerApi.info(logMessage, logMessageParameters);
        }

        // Execute request
        String requestState = "OK";
        try {
            pMethod.setAccessible(true);
            Object result = pMethod.invoke(pInstance, pArgs);
            return result;
        }
        catch (IllegalAccessException exception) {
            String message = "Illegal access to method " + pMethod.getName()
                    + " in class " + clazz.getName() + ".";
            requestState = "FAILED: " + message;
            throw new XmlRpcException(message, exception);
        }
        catch (IllegalArgumentException exception) {
            String message = "Illegal argument for method " + pMethod.getName()
                    + " in class " + clazz.getName() + ".";
            requestState = "FAILED: " + message;
            throw new XmlRpcException(message, exception);
        }
        catch (InvocationTargetException exception) {
            Throwable throwable = exception.getTargetException();
            requestState = "FAILED: " + throwable.getMessage();
            if (throwable instanceof ApiFaultException) {
                ApiFaultException apiFaultException = (ApiFaultException) throwable;
                throw new RpcApiFaultXmlRpcException(requestContext, apiFaultException.getApiFault(), throwable);
            }
            else if (throwable instanceof ReportException) {
                ReportException reportException = (ReportException) throwable;
                throw new RpcReportXmlRpcException(requestContext, reportException.getReport(), throwable);
            }
            else if (throwable instanceof ReportRuntimeException) {
                ReportRuntimeException reportRuntimeException = (ReportRuntimeException) throwable;
                throw new RpcReportXmlRpcException(requestContext, reportRuntimeException.getReport(), throwable);
            }
            else {
                String message = "Failed to invoke " + ((Service) pInstance).getServiceName() + "." + pMethod.getName()
                        + ": " + throwable.getMessage();
                throw new RpcApiFaultXmlRpcException(requestContext, new CommonReportSet.UnknownErrorReport( message), throwable);
            }
        }
        finally {
            // Log request end
            long duration = requestTimer.stop();
            logMessage = "Request:{} Done in {} ms ({}).";
            logMessageParameters = new Object[]{requestContext.requestId, duration, requestState};
            if (debug) {
                Controller.loggerApi.debug(logMessage, logMessageParameters);
            }
            else {
                Controller.loggerApi.info(logMessage, logMessageParameters);
            }
        }
    }

    public static Throwable convertThrowable(Throwable throwable)
    {
        Reporter reporter = Reporter.getInstance();
        ApiFault apiFault = null;
        RpcRequestContext requestContext = null;

        // Report not API fault
        if (throwable instanceof RpcReportXmlRpcException) {
            RpcReportXmlRpcException reportXmlRpcException = (RpcReportXmlRpcException) throwable;
            AbstractReport report = reportXmlRpcException.getReport();
            apiFault = new CommonReportSet.UnknownErrorReport(report.getMessage());
            throwable = reportXmlRpcException.getCause();
            requestContext = reportXmlRpcException.getRequestContext();

            reporter.report(reportXmlRpcException.getRequestContext(), report, throwable);
        }
        // Report API fault
        else {
            if (throwable instanceof RpcApiFaultXmlRpcException) {
                RpcApiFaultXmlRpcException apiFaultXmlRpcException = (RpcApiFaultXmlRpcException) throwable;
                apiFault = apiFaultXmlRpcException.getApiFault();
                throwable = apiFaultXmlRpcException.getCause();
                requestContext = apiFaultXmlRpcException.getRequestContext();
            }
            else if (throwable instanceof ApiFaultException) {
                ApiFaultException apiFaultException = (ApiFaultException) throwable;
                apiFault = apiFaultException.getApiFault();
            }
            else if (throwable instanceof RuntimeException || throwable instanceof SAXException) {
                Throwable cause = throwable.getCause();
                if (cause instanceof RpcApiFaultXmlRpcException) {
                    RpcApiFaultXmlRpcException apiFaultXmlRpcException = (RpcApiFaultXmlRpcException) cause;
                    apiFault = apiFaultXmlRpcException.getApiFault();
                    throwable = apiFaultXmlRpcException.getCause();
                }
                else if (cause instanceof ApiFaultException) {
                    ApiFaultException apiFaultException = (ApiFaultException) cause;
                    apiFault = apiFaultException.getApiFault();
                    throwable = cause;
                }
            }
            if (apiFault == null) {
                apiFault = new CommonReportSet.UnknownErrorReport(throwable.getMessage());
            }

            reporter.reportApiFault(requestContext, apiFault, throwable);
        }

        String apiFaultMessage;
        UserInformation userInformation = (requestContext != null ? requestContext.getUserInformation() : null);
        if (apiFault instanceof AbstractReport && userInformation != null) {
            AbstractReport report = (AbstractReport) apiFault;
            Authorization authorization = Authorization.getInstance();
            Report.UserType userType = Report.UserType.USER;
            if (authorization.isAdministrator(userInformation.getUserId())) {
                userType = Report.UserType.DOMAIN_ADMIN;
            }
            apiFaultMessage = report.getMessage(userType);
        }
        else {
            apiFaultMessage = apiFault.getFaultString();
        }

        ApiFaultString apiFaultString = new ApiFaultString();
        apiFaultString.setMessage(apiFaultMessage);
        apiFault.writeParameters(apiFaultString);
        XmlRpcException xmlRpcException = new XmlRpcException(
                apiFault.getFaultCode(), apiFaultString.toString(), throwable.getCause());
        xmlRpcException.setStackTrace(throwable.getStackTrace());
        return xmlRpcException;
    }
}
