package cz.cesnet.shongo.rpctest.common;

public class API
{
    public static class Resource
    {
        public String name;
        public String description;
        //public Resource resource;
        
        public String getName() {
            return name;
        }
        public String getDescription() {
            return description;
        }
        //public Resource getResource() {
        //    return resource;
        //}
    }

    public static class Date
    {
        public String date;
        
        public Date(String date)
        {
            this.date = date;
        }
        
        public String toString()
        {
            return "Date [" + date + "]";
        }
    }
    
    public static class PeriodicDate extends Date
    {
        public String period;

        public PeriodicDate(String date, String period)
        {
            super(date);
            this.period = period;
        }

        public String toString()
        {
            return "PeriodicDate [" + date + ", " + period + "]";
        }
    }
    
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
    
    public String formatDate(Date date)
    {
        return date.toString();
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
    }

    public Resource getResource()
    {
        Resource resource = new Resource();
        resource.name = "Name";
        resource.description = "Long description";
        return resource;
    }
}
