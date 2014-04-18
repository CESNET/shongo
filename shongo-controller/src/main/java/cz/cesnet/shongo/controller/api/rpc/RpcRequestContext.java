package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Reporter;

/**
 * Represents attributes of XML-RPC request.
 */
class RpcRequestContext implements Reporter.ReportContext
{
    /**
     * Unique identifier of the request.
     */
    Long requestId;

    /**
     * Name of the method which is invoked by the request (e.g., "service.method")
     */
    String methodName;

    /**
     * Method arguments.
     */
    Object[] arguments;

    /**
     * {@link cz.cesnet.shongo.api.UserInformation} of user which requested the method.
     */
    UserInformation userInformation;

    /**
     * @return {@link #userInformation}
     */
    public UserInformation getUserInformation()
    {
        return userInformation;
    }

    @Override
    public String getReportContextName()
    {
        if (userInformation != null) {
            return String.format("%s by %s",
                    methodName, userInformation.getFullName());
        }
        else {
            return String.format("%s", methodName);
        }
    }

    @Override
    public String getReportContextDetail()
    {
        StringBuilder reportDetail = new StringBuilder();
        reportDetail.append("REQUEST\n\n");

        reportDetail.append("      ID: ");
        reportDetail.append(requestId);
        reportDetail.append("\n");

        if (userInformation != null) {
            reportDetail.append("    User: ");
            reportDetail.append(userInformation.getFullName());
            reportDetail.append("(id: ");
            reportDetail.append(userInformation.getUserId());
            reportDetail.append(")");
            reportDetail.append("\n");
        }

        reportDetail.append("  Method: ");
        reportDetail.append(methodName);
        reportDetail.append("\n");

        reportDetail.append("  Arguments:\n");
        for (Object argument : arguments) {
            reportDetail.append("   * "); reportDetail.append(argument.toString()); reportDetail.append("\n");
        }
        return reportDetail.toString();
    }
}
