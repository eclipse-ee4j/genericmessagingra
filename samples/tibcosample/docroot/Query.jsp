<%--

    Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Distribution License v. 1.0, which is available at
    http://www.eclipse.org/org/documents/edl-v10.php.

    SPDX-License-Identifier: BSD-3-Clause

--%>

<%@ page session="false" %>
<%@ page import="javax.ejb.EJBHome"%>
<%@ page import="javax.naming.*"%>;
<%@ page import="javax.rmi.PortableRemoteObject"%>
<%@ page import="java.awt.*"%>
<%@ page import="java.awt.event.*"%>
<%@ page import="javax.swing.*"%>
<%@ page import="javax.ejb.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="javax.jms.*"%>
<%@ page import="java.util.logging.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.sql.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.math.*"%>

<!--
	Queries and displays the rows of the
	database
-->


<HTML>
<HEAD>
<TITLE>Display All Orders</TITLE>
</HEAD>
<BODY>
<CENTER>
<BR>
<TABLE border="1">
<TR>
<TH>Product Name</TH>
<TH>Quantity</TH>
<TH>Date</TH>
</TR>
<%! java.sql.Connection con; %>
<%! String dbName = "java:comp/env/jdbc/PublisherDB"; %>
<%! 
void makeConnection(JspWriter out)
{
   try
   {
        Context initCtx = new InitialContext();
	DataSource ds = (DataSource)initCtx.lookup(dbName);
	con = ds.getConnection();
   }
   catch (Exception ex) {
        ex.printStackTrace();
	System.out.println("Unable to connect to database. " +
   ex);
   }
}
%>

<!--
	Displays the rows of the database
	to the user
-->

<%!
public void listRows(JspWriter out) throws SQLException
{
  ResultSet rs;
  int rows=0,columns=3,cnt=0;

  String listStatement="select * from publisher";
  PreparedStatement prepStmt = con.prepareStatement(listStatement);
  rs = prepStmt.executeQuery();
  rows = rs.getFetchSize();
  String tableValues[][] = new String[rows+1][columns];
  while(rs.next()) 
  {
      	try{ 
        out.println("<TR>");
      	out.println("<TD>" + rs.getString(1) + "</TD>");
      	out.println("<TD>" + rs.getString(2) + "</TD>");
        out.println("<TD>" + rs.getString(3) + "</TD>");
 	out.println("</TR>");}
        catch(Exception e){e.printStackTrace();}
  }
  rs.close();
}
%>
<%!
private void releaseConnection()
{
    try
    {
	con.close();
    }
    catch (SQLException ex)
    {
       	throw new EJBException("releaseConnection: " + ex.getMessage());
    }
}
%>
<%
	makeConnection(out);
	try
        {
      	listRows(out);
	}
        catch(Exception e)
	{
      	System.out.println(""+e);
	}
        releaseConnection();
%>

</TABLE>
</CENTER>
</BODY>
</HTML>

