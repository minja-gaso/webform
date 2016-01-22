package org.sw.marketing.servlet;

import java.io.ByteArrayInputStream;
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
import org.sw.marketing.util.ReadFile;

@WebServlet("/completed/*")
public class FormCompletedServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;


	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String paramFormID = request.getPathInfo().substring(1);
		long formID = Long.parseLong(paramFormID);
		FormDAO formDAO = DAOFactory.getFormDAO();
		Data data = new Data();
		Form form = formDAO.getForm(formID);
		if(form != null)
		{
			data.getForm().add(form);
		}
		
		if(request.getAttribute("SELF_ASSESSMENT_SCORE") != null)
		{
			int score = (int) request.getAttribute("SELF_ASSESSMENT_SCORE");
			form.setScore(score);
		}
		
		/*
		 * generate output
		 */
		TransformerHelper transformerHelper = new TransformerHelper();
		transformerHelper.setUrlResolverBaseUrl(getServletContext().getInitParameter("assetXslFormsPath"));
		
		String xmlStr = transformerHelper.getXmlStr("org.sw.marketing.data.form", data);
		String xslScreen = getServletContext().getInitParameter("assetXslPath") + "complete.xsl";
		String xslStr = ReadFile.getSkin(xslScreen);
		String htmlStr = transformerHelper.getHtmlStr(xmlStr, new ByteArrayInputStream(xslStr.getBytes()));
		
		/*
		 * display output
		 */
		System.out.println(xmlStr);
		
		response.setContentType("text/html");
		response.getWriter().println(htmlStr);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		process(request, response);
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		process(request, response);
	}

}
