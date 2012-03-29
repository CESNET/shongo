package cz.cesnet.shongo.rpctest.soapaxis;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    {
        try {
            response.getWriter().write("Called");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
