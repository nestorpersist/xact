/*******************************************************************************
*
* Copyright (c) 2002-2010. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;    
import javax.naming.*;
import com.persist.xact.system.*;

/**
 * This class allows the XACT interpreter
 * to be called as a Java Servlet
 * It handles both GET and POST requests.
 */
public class xs extends HttpServlet {
    /** The servlet config **/
    private ServletConfig config;

    /**
     ** Called when servlet first placed into use.
     ** @param config the servlet config
     */
    public void init(ServletConfig config) throws ServletException {
        this.config = config;
    }

    /**
     ** Call when servlet removed from use.
     */
    public void destroy() {
    }

    /** Common code for handling both GET and POST requests.
     ** @param request the servlet request object
     ** @param response the servlet response object
     ** @param input POST input (or "" for GET)
     */
    private void doAll(HttpServletRequest request,
                       HttpServletResponse response,
                       String input)
            throws IOException, ServletException
    {
        XInter inter = new XInter();
        XCmd cmd = new XCmd(inter);
        XThread xt = new XThread("main",cmd);
        cmd.xtMain = xt;

        try {
            Context ctx = new InitialContext();
            String options = (String) ctx.lookup("java:comp/env/options");
            xt.xarg.doOption(options);
        } catch (NamingException e) {
        }

        String qs = request.getQueryString();
        if (qs == null) qs = "";
        xt.xarg.doQuery(qs,cmd.option.script);

        cmd.option.setOption("REMOTE_ADDR", request.getRemoteAddr(), 0);
        cmd.option.setOption("httpRequest", request, -1);
        xt.xarg.doInput(input);

        if (cmd.option.mime != "") {
            response.setContentType(cmd.option.mime);
        }
        xt.xarg.setScript();
        xt.xrun.run(xt.xarg.script,xt.xarg.scriptTree,response.getOutputStream(),false);
    }

    /**
     ** Handle a POST request
     ** @param request the servlet request object
     ** @param response the servlet response object
     */
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException
    {
        InputStream s = request.getInputStream();
        BufferedReader r = new BufferedReader(new InputStreamReader(s));
        String input = "";
        while (true) {
            String line = r.readLine();
            if (line == null) break;
            input = input + line;
        }
        doAll(request,response,input.intern());
    }
    
    /**
     ** Handle a GET request
     ** @param request the servlet request object
     ** @param response the servlet response object
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException
    {
        doAll(request,response,"");
    }
}
