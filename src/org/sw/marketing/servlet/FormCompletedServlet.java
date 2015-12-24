package org.sw.marketing.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sw.marketing.dao.DAOFactory;
import org.sw.marketing.dao.form.FormDAO;
import org.sw.marketing.data.form.Data;
import org.sw.marketing.data.form.Data.Form;
import org.sw.marketing.transformation.TransformerHelper;

@WebServlet("/completed/*")
public class FormCompletedServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String paramFormID = request.getPathInfo().substring(1);
		int formID = Integer.parseInt(paramFormID);
		FormDAO formDAO = DAOFactory.getFormDAO();
		Data data = new Data();
		Form form = formDAO.getForm(formID);
		if(form != null)
		{
			data.getForm().add(form);
		}
		
		/*
		 * generate output
		 */
		String xmlStr = TransformerHelper.getXmlStr("org.sw.marketing.data.form", data);
		String htmlStr = TransformerHelper.getHtmlStr(xmlStr, getServletContext().getResourceAsStream("/complete.xsl"));
		
		/*
		 * display output
		 */
		System.out.println(xmlStr);
		
		response.setContentType("text/html");
		response.getWriter().println(htmlStr);
	}

}
