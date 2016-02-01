package org.sw.marketing.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sw.marketing.dao.form.FormDAO;
import org.sw.marketing.dao.question.QuestionDAO;
import org.sw.marketing.data.form.Data;
import org.sw.marketing.data.form.Data.Form;
import org.sw.marketing.data.form.Data.Form.Question;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

@WebServlet("/ProcessServlet")
public class ProcessServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		FormDAO formDAO = org.sw.marketing.dao.DAOFactory.getFormDAO();
		QuestionDAO questionDAO = org.sw.marketing.dao.DAOFactory.getQuestionDAO();
		
		String paramFormId = request.getParameter("FORM_ID");
		long formId = Long.parseLong(paramFormId);
		
		String paramCurrentPage = request.getParameter("CURRENT_PAGE");
		int currentPage = Integer.parseInt(paramCurrentPage);
		
		Data data = new org.sw.marketing.data.form.Data();
		Form form = formDAO.getForm(currentPage);
		java.util.List<Question> questions = questionDAO.getQuestionsByPage(formId, currentPage);
		
		ListMultimap<String, String> parameterMap = ArrayListMultimap.create();
		java.util.Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements())
		{
			String parameterName = (String) parameterNames.nextElement();
			String[] parameterValue = request.getParameterValues(parameterName);
			for (int index = 0; index < parameterValue.length; index++)
			{
				parameterMap.put(parameterName, parameterValue[index]);
			}
		}
		
		for(String key : parameterMap.keys())
		{
			java.util.List<String> values = parameterMap.get(key);
			java.util.Iterator<String> valuesIt = values.iterator();
			while(valuesIt.hasNext())
			{
				String value = valuesIt.next();
				System.out.println(key + ":" + value);
			}
		}
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
