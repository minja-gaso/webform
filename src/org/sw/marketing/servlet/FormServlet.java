package org.sw.marketing.servlet;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.validator.routines.EmailValidator;
import org.sw.marketing.dao.DAOFactory;
import org.sw.marketing.dao.answer.AnswerDAO;
import org.sw.marketing.dao.form.FormDAO;
import org.sw.marketing.dao.question.QuestionDAO;
import org.sw.marketing.dao.submission.SubmissionAnswerDAO;
import org.sw.marketing.dao.submission.SubmissionDAO;
import org.sw.marketing.dao.submission.TempSubmissionAnswerDAO;
import org.sw.marketing.dao.submission.TempSubmissionDAO;
import org.sw.marketing.data.form.Data;
import org.sw.marketing.data.form.Data.Form;
import org.sw.marketing.data.form.Data.Form.Question;
import org.sw.marketing.data.form.Data.Form.Question.PossibleAnswer;
import org.sw.marketing.data.form.Data.Message;
import org.sw.marketing.data.form.Data.Submission;
import org.sw.marketing.data.form.Data.Submission.Answer;
import org.sw.marketing.transformation.TransformerHelper;
import org.sw.marketing.util.ReadFile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

@WebServlet("/public/*")
public class FormServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final boolean displayXml = true;

	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String SESSION_ID = request.getSession().getId();

		/*
		 * initialize data access objects
		 */
		FormDAO formDAO = DAOFactory.getFormDAO();
		QuestionDAO questionDAO = DAOFactory.getQuestionDAO();
		AnswerDAO answerDAO = DAOFactory.getPossibleAnswerDAO();
		SubmissionDAO submissionDAO = DAOFactory.getSubmissionDAO();
		SubmissionAnswerDAO submissionAnswerDAO = DAOFactory.getSubmissionAnswerDAO();
		TempSubmissionDAO tempSubmissionDAO = DAOFactory.getTempSubmissionDAO();
		TempSubmissionAnswerDAO tempSubmissionAnswerDAO = DAOFactory.getTempSubmissionAnswerDAO();

		/*
		 * process submitted form fields
		 */
		ListMultimap<String, String> parameterMap = getFormFields(request);
		
		/*
		 * determine action
		 */
		String paramAction = null;
		boolean paramPostForm = false;
		
		if(parameterMap != null)
		{
			if(parameterMap.get("ACTION") != null && parameterMap.get("ACTION").size() > 0)
			{
				paramAction = parameterMap.get("ACTION").get(0);
			}
			if(parameterMap.get("POST_FORM") != null && parameterMap.get("POST_FORM").size() > 0)
			{
				paramPostForm = Boolean.parseBoolean(parameterMap.get("POST_FORM").get(0));
			}
		}
		
		/*
		 * get form ID
		 */
		long formID = 0;
		try
		{
			formID = Long.parseLong(request.getPathInfo().substring(1));
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
		
		/*
		 * initialize page variable
		 */
		int lastPage = questionDAO.getLatestPage(formID);
		if(lastPage == 0)
		{
			lastPage = 1;
		}
		int currentPage = 1;
		int previousPage = 1;
		try
		{
			if(parameterMap.get("CURRENT_PAGE") != null && parameterMap.get("CURRENT_PAGE").size() > 0)
			{
				currentPage = Integer.parseInt(parameterMap.get("CURRENT_PAGE").get(0));
			}
			if(parameterMap.get("PREVIOUS_PAGE") != null && parameterMap.get("PREVIOUS_PAGE").size() > 0)
			{
				previousPage = Integer.parseInt(parameterMap.get("PREVIOUS_PAGE").get(0));
			}
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
		
		/*
		 * initialize submission
		 */
		Submission submission = tempSubmissionDAO.getSubmissionBySessionID(formID, SESSION_ID);
		if(submission == null)
		{
			tempSubmissionDAO.insert(formID, SESSION_ID);
			submission = tempSubmissionDAO.getSubmissionBySessionID(formID, SESSION_ID);
		}
		
		/*
		 * determine whether to store submitted answers or send error
		 */
		java.util.List<Question> questionList = new java.util.ArrayList<Question>();
		java.util.List<Message> messageList = null;
		if(currentPage >= previousPage)
		{
			for(String key : parameterMap.keySet())
			{
				if(key.startsWith("QUESTION_"))
				{
					Question question = questionDAO.getQuestion(Long.parseLong(key.split("QUESTION_")[1]));
					questionList.add(question);
					
					Message message = null;
					if(question.getType().contains("text"))
					{
						message = processQuestion(question, parameterMap.get(key).get(0));
					}
					else
					{
						java.util.List<String> answers = parameterMap.get(key);
						java.util.Iterator<String> answerIter = answers.iterator();
						boolean atLeastOneChecked = false;
						while(answerIter.hasNext())
						{
							String answerStr = answerIter.next();
							message = processQuestion(question, answerStr);
							if(message == null)
							{
								atLeastOneChecked = true;
							}
						}
						
						if(atLeastOneChecked)
						{
							message = null;
						}
					}
					
					if(message != null)
					{
						if(messageList == null)
						{
							messageList = new java.util.ArrayList<Message>();
						}
						messageList.add(message);
					}
				}
			}
		}
		
		/*
		 * determine current page thru action param
		 */
		if(messageList == null)
		{
			if(paramAction != null && paramAction.equals("NEXT_PAGE"))
			{
				currentPage = previousPage + 1;
			}
			else if(paramAction != null && paramAction.equals("PREVIOUS_PAGE"))
			{
				currentPage = previousPage - 1;
			}
			
			submission.setPage(previousPage);
			
			
			if(tempSubmissionAnswerDAO.getSubmissionAnswersByPage(submission) != null)
			{
				tempSubmissionAnswerDAO.deleteSubmissionAnswersByPage(submission);
			}
			
			/*
			 * insert questions into temp answers table
			 */
			for(String key : parameterMap.keySet())
			{
				if(key.startsWith("QUESTION_"))
				{
					Question question = questionDAO.getQuestion(Long.parseLong(key.split("QUESTION_")[1]));
					questionList.add(question);
					
					java.util.Iterator<String> answerIter = parameterMap.get(key).iterator();
					while(answerIter.hasNext())
					{
						String answerStr = answerIter.next();
						if(answerStr.length() > 0)
						{
							Answer answer = new Answer();
							answer.setQuestionId(question.getId());
							answer.setMultipleChoice(question.isRequired());
							answer.setAnswerValue(answerStr);
							
							tempSubmissionAnswerDAO.insert(submission, answer, question);
						}
					}
				}
			}
			
			submission.setPage(currentPage);
			if(tempSubmissionAnswerDAO.getSubmissionAnswersByPage(submission) != null)
			{
				submission.getAnswer().addAll(tempSubmissionAnswerDAO.getSubmissionAnswersByPage(submission));
			}
			
			if(paramAction != null && paramAction.equals("SUBMIT_FORM"))
			{
				tempSubmissionDAO.copyTo(SESSION_ID, formID);
				tempSubmissionAnswerDAO.copyTo(submission);
				
				/*
				 * fk constraint - delete answers first!
				 */
				tempSubmissionAnswerDAO.deleteFromTemp(submission);
				tempSubmissionDAO.deleteFromTemp(SESSION_ID, formID);
				
				response.sendRedirect(getServletContext().getContextPath() + "/completed/" + formID);
				return;
			}
		}
		else
		{
			currentPage = previousPage;
		}

		/*
		 * prepare data
		 */
		Data data = new Data();
		
		/*
		 * get form data
		 */
		Form form = formDAO.getForm(formID);		
		java.util.List<Question> currentPageQuestions = questionDAO.getQuestionsByPage(formID, currentPage);
		
		/*
		 * if errors exist add to data set
		 */
		if(messageList != null)
		{
			data.getMessage().addAll(messageList);
		}
		
		if(submission != null)
		{
			data.getSubmission().add(submission);
		}
		
		if(currentPageQuestions != null)
		{
			java.util.Iterator<Question> questionIter = currentPageQuestions.iterator();
			while(questionIter.hasNext())
			{
				Question question = questionIter.next();
				java.util.List<PossibleAnswer> answerList = answerDAO.getPossibleAnswers(question.getId());
				if(answerList != null)
				{
					question.getPossibleAnswer().addAll(answerList);
				}
			}
			form.getQuestion().addAll(currentPageQuestions);
		}

		form.setCurrentPage(currentPage);
		form.setLastPage(lastPage);
		data.getForm().add(form);

		/*
		 * generate output
		 */
		String xmlStr = TransformerHelper.getXmlStr("org.sw.marketing.data.form", data);
		String htmlStr = TransformerHelper.getHtmlStr(xmlStr, getServletContext().getResourceAsStream("/form.xsl"));

		String toolboxSkinPath = getServletContext().getInitParameter("assetPath") + "toolbox_1col.html";
		String skinHtmlStr = ReadFile.getSkin(toolboxSkinPath);
		skinHtmlStr = skinHtmlStr.replace("{NAME}", "List of Surveys");
		skinHtmlStr = skinHtmlStr.replace("{CONTENT}", htmlStr);

		if(displayXml)
		{
			System.out.println(xmlStr);
		}
		response.getWriter().print(skinHtmlStr);
	}
	
	protected ListMultimap<String, String> getFormFields(HttpServletRequest request)
	{
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
		
		return parameterMap;
	}
	
	protected Message processQuestion(Question question, String answer)
	{
		Message message = null;
		if(question.isRequired())
		{
			if(answer.length() == 0)
			{
				message = new Message();
				message.setQuestionId(question.getId());
				message.setQuestionNumber(question.getNumber());
				message.setType("error");
				message.setSubtype("required");
				message.setLabel("Question " + question.getNumber() + " is required.");
			}
		}
		
		return message;
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
