package cz.cesnet.shongo.rpctest.common;

public class API
{
    public static class ApiException extends Exception
    {
        String dataLoad;
        
        public ApiException(String message, String dataLoad)
        {
            super(message);
            this.dataLoad = dataLoad;
        }
        
        public String toString()
        {
            String output = super.toString();
            return output + " [" + dataLoad + "]";
        }
    }
    
    public int add(int x, int y)
    {
        return x + y;
    }

    public int div(int x, int y)
    {
        return x / y;
    }

    public int addAndDiv(int x, int y, int z)
    {
        return (x + y) / z;
    }

    public String getMessage() throws ApiException
    {
        return getMessageSecond();
    }

    public String getMessageSecond() throws ApiException
    {
        return getMessageThird();
    }

    public String getMessageThird() throws ApiException
    {
        throw new ApiException("My Exception", "My data");
        //throw new XmlRpcException(25, "Hello");
        //return "Hello";
    }
}
