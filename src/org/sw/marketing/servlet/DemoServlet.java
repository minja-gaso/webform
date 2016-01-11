package org.sw.marketing.servlet;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.sw.marketing.data.form.Data;
import org.sw.marketing.data.form.Data.Submission;
import org.sw.marketing.data.form.Data.Submission.Answer;
import org.sw.marketing.transformation.TransformerHelper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

@WebServlet("/demo")
public class DemoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
//		Runtime.getRuntime().exec("cls");
		HttpSession session = request.getSession();
		
		ListMultimap<String, String> parameterMap = ArrayListMultimap.create();
		java.util.Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements())
		{
			String parameterName = (String) parameterNames.nextElement();
			String[] parameterValue = request.getParameterValues(parameterName);
			for(int index = 0; index < parameterValue.length; index++)
			{
				parameterMap.put(parameterName, parameterValue[index]);
			}
		}

		Submission submission = null;
		if(session.getAttribute("submission") != null)
		{
			submission = (Submission) session.getAttribute("submission");
		}
		else
		{
			submission = new Submission();
		}
		
		ListMultimap<String, Answer> existingAnswerList = ArrayListMultimap.create();
		java.util.List<Answer> submissionAnswerList = submission.getAnswer();
		for(int index = 0; index < submissionAnswerList.size(); index++)
		{
			
		}
		existingAnswerList.putAll(submissionAnswerList);
		for (String key : parameterMap.keySet())
		{
			if(key.contains("QUESTION_"))
			{
				List<String> values = parameterMap.get(key);
				
				Iterator<String> valuesIter = values.iterator();
				while(valuesIter.hasNext())
				{
					Answer answer = new Answer();
					answer.setAnswerLabel(valuesIter.next());
					answer.setQuestionId(Long.parseLong(key.split("QUESTION_")[1]));
					submission.getAnswer().add(answer);
					
				}				
			}
		}
		session.setAttribute("submission", submission);		

		Data data = new Data();
		data.getSubmission().add(submission);
		String xmlStr = TransformerHelper.getXmlStr("org.sw.marketing.data.form", data);
		System.out.println(xmlStr);
	}

}
