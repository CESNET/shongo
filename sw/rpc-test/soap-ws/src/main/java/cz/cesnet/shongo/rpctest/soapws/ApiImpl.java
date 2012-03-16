package cz.cesnet.shongo.rpctest.soapws;

import javax.jws.WebService;

@WebService(endpointInterface = "cz.cesnet.shongo.rpctest.soapws.Api")
public class ApiImpl implements Api
{
    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public int div(int a, int b) {
        return a / b;
    }

    @Override
    public String getMessage() throws ApiException {
        return getMessageSecond();
    }

    public String getMessageSecond() throws ApiException
    {
        return getMessageThird();
    }

    public String getMessageThird() throws ApiException
    {
        throw new ApiException("My Exception", "My data");
    }

    @Override
    public Resource getResource()
    {
        Resource resource = new Resource();
        resource.name = "Name";
        resource.description = "Long description";
        return resource;
    }
}