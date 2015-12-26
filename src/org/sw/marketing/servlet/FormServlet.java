package org.sw.marketing.servlet;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sw.marketing.dao.DAOFactory;
import org.sw.marketing.dao.answer.AnswerDAO;
import org.sw.marketing.dao.form.FormDAO;
import org.sw.marketing.dao.question.QuestionDAO;
import org.sw.marketing.dao.submission.SubmissionAnswerDAO;
import org.sw.marketing.dao.submission.SubmissionDAO;
import org.sw.marketing.data.form.Data;
import org.sw.marketing.data.form.Data.Form;
import org.sw.marketing.data.form.Data.Form.Question;
import org.sw.marketing.data.form.Data.Form.Question.PossibleAnswer;
import org.sw.marketing.data.form.Data.Message;
import org.sw.marketing.data.form.Data.Submission;
import org.sw.marketing.data.form.Data.Submission.Answer;
import org.sw.marketing.transformation.TransformerHelper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

@WebServlet("/public/*")
public class FormServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{	
		// dao		
		FormDAO formDAO = DAOFactory.getFormDAO();
		QuestionDAO questionDAO = DAOFactory.getQuestionDAO();
		AnswerDAO answerDAO = DAOFactory.getPossibleAnswerDAO();
		SubmissionDAO submissionDAO = DAOFactory.getSubmissionDAO();
		SubmissionAnswerDAO submissionAnswerDAO = DAOFactory.getSubmissionAnswerDAO();
		
		// params
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
		//
		List<String> paramActionList = null;
		String paramAction = null;
		if(parameterMap.get("ACTION") != null)
		{
			paramActionList = parameterMap.get("ACTION");
			if(paramActionList.size() > 0)
			{
				paramAction = paramActionList.get(0);
			}
		}
		//
		long formID = 0;
		try
		{
			if(parameterMap.get("FORM_ID") != null && parameterMap.get("FORM_ID").size() > 0)
			{
				formID = Long.parseLong(parameterMap.get("FORM_ID").get(0));
			}
			else
			{
				formID = Long.parseLong(request.getPathInfo().substring(1));
			}
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
		//
		int currentPage = 1;
		try
		{
			if(parameterMap.get("CURRENT_PAGE") != null && parameterMap.get("CURRENT_PAGE").size() > 0)
			{
				currentPage = Integer.parseInt(parameterMap.get("CURRENT_PAGE").get(0));
			}
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
		//
		int priorPage = 1;
		try
		{
			if(parameterMap.get("PRIOR_PAGE") != null && parameterMap.get("PRIOR_PAGE").size() > 0)
			{
				priorPage = Integer.parseInt(parameterMap.get("PRIOR_PAGE").get(0));
			}
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
		int lastPage = questionDAO.getLatestPage(formID);
		String formSubmitRedirect = getServletContext().getContextPath() + "/completed/" + formID;

		// initialize
		Form form = formDAO.getForm(formID);
		java.util.List<Question> questionListCurrentPage = questionDAO.getQuestionsByPage(formID, currentPage);
		java.util.List<Question> questionListPriorPage = questionDAO.getQuestionsByPage(formID, priorPage);
		java.util.List<Message> messageList = null;
		
		Submission submission = null;
		if(request.getSession().getAttribute("sessionSubmission") != null)
		{
			submission = (Submission) request.getSession().getAttribute("sessionSubmission");
		}
		
		// store submitted answers
		ListMultimap<Long, String> answerMap = ArrayListMultimap.create();
		Answer answer = null;
		for (String questionKey : parameterMap.keySet()) 
		{
			if(submission == null)
			{
				submission = new Submission();
			}
			if(questionKey.contains("QUESTION_"))
			{
				String questionKeyIdStr = questionKey.split("QUESTION_")[1];
				long questionKeyID = Long.parseLong(questionKeyIdStr);
				List<String> questionAnswerList = parameterMap.get(questionKey);
				Iterator<String> questionAnswerIter = questionAnswerList.iterator();
				while(questionAnswerIter.hasNext())
				{
					String questionAnswer = questionAnswerIter.next();
					answer = new Answer();
					answer.setQuestionId(questionKeyID);
					answer.setAnswerValue(questionAnswer);
					submission.getAnswer().add(answer);
					answerMap.put(questionKeyID, questionAnswer);
				}
//				System.out.println(questionKey + ": " + questionAnswerList);
			}
		}
		
		if(submission != null)
		{
			request.getSession().setAttribute("sessionSubmission", submission);
		}
		
//		questionListPriorPage = questionDAO.getQuestionsByPage(formID, priorPage);
		Message message = null;
		boolean formSubmitted = false;
		if(paramAction != null && paramAction.equals("SUBMIT_FORM"))
		{
			if(questionListPriorPage != null && questionListCurrentPage != null)
			{
				java.util.Iterator<Question> questionIter = questionListPriorPage.iterator();
				while(questionIter.hasNext())
				{
					Question question = questionIter.next();
					
					boolean answerExists = false;
					List<String> answerList = answerMap.get(question.getId());
					if(answerList != null && answerList.size() > 0)
					{
						if(answerList.get(0).length() > 0)
						{
							answerExists = true;
						}
					}
					
					if(question.isRequired() && !answerExists)
					{
						if(messageList == null)
						{
							messageList = new java.util.ArrayList<Message>();
						}
						message = new Message();
						message.setQuestionId(question.getId());
						message.setQuestionNumber(question.getNumber());
						message.setLabel(question.getLabel());
						message.setType("error");
						messageList.add(message);
					}
				}
				
				if(messageList == null)
				{
					long submissionID = submissionDAO.insert(formID);
					for(Answer answerElement : submission.getAnswer())
					{
						Question question = questionDAO.getQuestion(answerElement.getQuestionId());
						submission.setId(submissionID);
						submissionAnswerDAO.insert(submission, answerElement, question);
						request.getSession().removeAttribute("sessionSubmission");
						formSubmitted = true;		
					}
					response.sendRedirect(formSubmitRedirect);
					
					return;
				}
			}
		}
		
		
		/*
		 * prepare data
		 */
		
		if(messageList == null)
		{
			if(questionListCurrentPage != null)
			{
				java.util.Iterator<Question> questionIter = questionListCurrentPage.iterator();
				Question question = null;
				while(questionIter.hasNext())
				{
					question = questionIter.next();
					java.util.List<PossibleAnswer> answerList = answerDAO.getPossibleAnswers(question.getId());
					if(answerList != null)
					{
						question.getPossibleAnswer().addAll(answerList);
					}
				}
				form.getQuestion().addAll(questionListCurrentPage);
			}
		}
		else
		{
			if(questionListCurrentPage != null)
			{
				form.getQuestion().addAll(questionListCurrentPage);
			}
		}
		
//		System.out.println(answerMap);
//		System.out.println(questionMap);
		
		Data data = new Data();
		if(submission != null && !formSubmitted)
		{
			data.getSubmission().add(submission);
			request.getSession().setAttribute("sessionSubmission", submission);
		}		
		if(messageList != null)
		{
			currentPage = priorPage;
			data.getMessage().addAll(messageList);
		}
		form.setCurrentPage(currentPage);
		
		if(lastPage == 0)
		{
			lastPage = 1;
		}
		form.setLastPage(lastPage);
		data.getForm().add(form);
		
		/*
		 * generate output
		 */
		String xmlStr = TransformerHelper.getXmlStr("org.sw.marketing.data.form", data);
		String htmlStr = TransformerHelper.getHtmlStr(xmlStr, getServletContext().getResourceAsStream("/form.xsl"));
		
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
