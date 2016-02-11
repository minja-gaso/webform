package org.sw.marketing.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.validator.routines.EmailValidator;
import org.sw.marketing.dao.DAOFactory;
import org.sw.marketing.dao.form.FormDAO;
import org.sw.marketing.dao.form.answer.AnswerDAO;
import org.sw.marketing.dao.form.question.QuestionDAO;
import org.sw.marketing.dao.form.submission.SubmissionAnswerDAO;
import org.sw.marketing.dao.form.submission.SubmissionDAO;
import org.sw.marketing.dao.form.submission.TempSubmissionAnswerDAO;
import org.sw.marketing.dao.form.submission.TempSubmissionDAO;
import org.sw.marketing.data.form.Data;
import org.sw.marketing.data.form.Data.Form;
import org.sw.marketing.data.form.Data.Form.Question;
import org.sw.marketing.data.form.Data.Form.Question.PossibleAnswer;
import org.sw.marketing.data.form.Data.Message;
import org.sw.marketing.data.form.Data.Submission;
import org.sw.marketing.data.form.Data.Submission.Answer;
import org.sw.marketing.transformation.TransformerHelper;
import org.sw.marketing.util.ReadFile;
import org.sw.marketing.util.SkinReader;
import org.sw.marketing.util.SurveyHelper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

@WebServlet("/public/*")
public class FormServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final boolean displayXml = true;

	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String SESSION_ID = request.getSession().getId();
		String IP_ADDRESS = InetAddress.getLocalHost().getHostAddress();

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
		if (parameterMap != null)
		{
			if (parameterMap.get("ACTION") != null && parameterMap.get("ACTION").size() > 0)
			{
				paramAction = parameterMap.get("ACTION").get(0);
			}
		}

		boolean submitted = false;
		if (paramAction != null && paramAction.equals("SUBMIT_FORM"))
		{
			submitted = true;
		}		
		boolean isSubmissionValid = false;

		/*
		 * get form ID
		 */
		boolean prettyUrl = false;
		long formID = 0;
		String formPrettyUrl = null;
		try
		{
			formID = Long.parseLong(request.getPathInfo().substring(1));
		}
		catch (NumberFormatException e)
		{
			prettyUrl = true;
			formPrettyUrl = request.getPathInfo().substring(1);
		}

		Form form = null;
		if (prettyUrl)
		{
			form = formDAO.getFormByPrettyUrl(formPrettyUrl);
		}
		else
		{
			form = formDAO.getForm(formID);
		}

		if (form != null)
		{
			formID = form.getId();
		}
		else
		{
			response.getWriter().println("The form you are looking for could not be found.");
			return;
		}

		/*
		 * initialize page variable
		 */
		int lastPage = questionDAO.getLatestPage(formID);
		if (lastPage == 0)
		{
			lastPage = 1;
		}
		int currentPage = 1;
		int previousPage = 1;
		try
		{
			if (parameterMap.get("CURRENT_PAGE") != null && parameterMap.get("CURRENT_PAGE").size() > 0)
			{
				currentPage = Integer.parseInt(parameterMap.get("CURRENT_PAGE").get(0));
			}
			if (parameterMap.get("PREVIOUS_PAGE") != null && parameterMap.get("PREVIOUS_PAGE").size() > 0)
			{
				previousPage = Integer.parseInt(parameterMap.get("PREVIOUS_PAGE").get(0));
			}
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
		}
		
		java.util.List<Question> questionList = new java.util.ArrayList<Question>();
		java.util.List<Message> messageList = null;

		/*
		 * determine current page thru action param
		 */
		boolean goPreviousPage = false;
		boolean goNextPage = false;
		if (messageList == null)
		{
			if (paramAction != null && paramAction.equals("NEXT_PAGE"))
			{
				currentPage = previousPage + 1;
				goNextPage = true;
			}
			else if (paramAction != null && paramAction.equals("PREVIOUS_PAGE"))
			{
				currentPage = previousPage - 1;
				goPreviousPage = true;
			}
		}

		/*
		 * initialize submission
		 */
		Submission submission = tempSubmissionDAO.getSubmissionBySessionID(formID, SESSION_ID);
		if (submission == null)
		{
			tempSubmissionDAO.insert(formID, SESSION_ID, IP_ADDRESS);
			submission = tempSubmissionDAO.getSubmissionBySessionID(formID, SESSION_ID);
		}

		/*
		 * determine whether to store submitted answers or send error
		 */
		if (goNextPage || submitted)
		{
			for (String key : parameterMap.keySet())
			{
				if (key.startsWith("QUESTION_"))
				{
					Question question = questionDAO.getQuestion(Long.parseLong(key.split("QUESTION_")[1]));
					questionList.add(question);

					Message message = null;
					if (question.getType().contains("text"))
					{
						message = processQuestion(question, parameterMap.get(key).get(0));
					}
					else
					{
						java.util.List<String> answers = parameterMap.get(key);
						java.util.Iterator<String> answerIter = answers.iterator();
						boolean atLeastOneChecked = false;
						while (answerIter.hasNext() )
						{
							String answerStr = answerIter.next();
							message = processQuestion(question, answerStr);
							if (message == null)
							{
								atLeastOneChecked = true;
							}
						}

						if (atLeastOneChecked)
						{
							message = null;
						}
					}

					if (message != null)
					{
						if (messageList == null)
						{
							messageList = new java.util.ArrayList<Message>();
						}
						messageList.add(message);
					}
				}
			}
		}
		
		if(messageList == null && submitted)
		{
			isSubmissionValid = true;
		}

		submission.setPage(previousPage);

		/*
		 * insert questions into temp answers table
		 */
		for (String key : parameterMap.keySet())
		{
			if (key.startsWith("QUESTION_"))
			{
				Question question = questionDAO.getQuestion(Long.parseLong(key.split("QUESTION_")[1]));
				questionList.add(question);

				java.util.Iterator<String> answerIter = parameterMap.get(key).iterator();
				while (answerIter.hasNext())
				{
					String answerStr = answerIter.next();
					if (answerStr.length() > 0)
					{
						Answer answer = new Answer();
						answer.setQuestionId(question.getId());
						if (question.getType().startsWith("text"))
						{
							answer.setMultipleChoice(false);
						}
						else
						{
							answer.setMultipleChoice(true);
						}
						answer.setAnswerValue(answerStr);

						if (form.getStatus().equals("live"))
						{
							tempSubmissionAnswerDAO.insert(submission, answer, question);
						}
						else
						{
							System.out.println("Not live!");
						}
					}
				}
			}
		}

		submission.setPage(currentPage);
		java.util.List<Answer> submissionAnswersByPage = tempSubmissionAnswerDAO.getSubmissionAnswersByPage(submission);
		if (submissionAnswersByPage != null)
		{
			submission.getAnswer().addAll(submissionAnswersByPage);
		}
		if (isSubmissionValid)
		{
			/*
			 * don't check if survey live so that you can still see thank
			 * you screen!
			 */
			if (form.getStatus().equals("live"))
			{
				tempSubmissionDAO.copyTo(SESSION_ID, formID);
				tempSubmissionAnswerDAO.copyTo(submission);

				/*
				 * fk constraint - delete answers first!
				 */
				tempSubmissionAnswerDAO.deleteFromTemp(submission);
				tempSubmissionDAO.deleteFromTemp(SESSION_ID, formID);
			}
		}
		else
		{
			currentPage = previousPage;
		}
		
		if(submissionAnswersByPage != null)
		{
			tempSubmissionAnswerDAO.deleteSubmissionAnswersByPage(submission);
		}

		/*
		 * prepare data
		 */
		Data data = new Data();

		/*
		 * get form data
		 */
		java.util.List<Question> currentPageQuestions = questionDAO.getQuestionsByPage(formID, currentPage);

		/*
		 * if errors exist add to data set
		 */
		if (messageList != null)
		{
			data.getMessage().addAll(messageList);
		}

		if (submission != null)
		{
			data.getSubmission().add(submission);
		}

		if (form.getType().equals("survey"))
		{
			if (currentPageQuestions != null)
			{
				java.util.Iterator<Question> questionIter = currentPageQuestions.iterator();
				while (questionIter.hasNext())
				{
					Question question = questionIter.next();
					java.util.List<PossibleAnswer> answerList = answerDAO.getPossibleAnswers(question.getId());
					if (answerList != null)
					{
						question.getPossibleAnswer().addAll(answerList);
					}
				}
				form.getQuestion().addAll(currentPageQuestions);
			}
		}
		else if (form.getType().equals("self_assessment"))
		{
			if (currentPageQuestions != null)
			{
				form.getQuestion().addAll(currentPageQuestions);
			}

			java.util.List<Data.PossibleAnswer> possibleAnswers = answerDAO.getPossibleAnswersByForm(formID);
			if (possibleAnswers != null)
			{
				data.getPossibleAnswer().addAll(possibleAnswers);
			}
		}

		form.setCurrentPage(currentPage);
		form.setLastPage(lastPage);

		form = SurveyHelper.verifyStartAndEndDate(form);
		
		data.getForm().add(form);

		/*
		 * generate output
		 */
		TransformerHelper transformerHelper = new TransformerHelper();
		transformerHelper.setUrlResolverBaseUrl(getServletContext().getInitParameter("assetXslFormsPath"));

		String xmlStr = transformerHelper.getXmlStr("org.sw.marketing.data.form", data);

		String xslScreen = getServletContext().getInitParameter("assetXslPath");
		if (form.getType().equals("survey"))
		{
			xslScreen = xslScreen + "form.xsl";
		}
		else
		{
			xslScreen = xslScreen + "self_assessment_form.xsl";
		}
		String xslStr = ReadFile.getSkin(xslScreen);
		String htmlStr = transformerHelper.getHtmlStr(xmlStr, new ByteArrayInputStream(xslStr.getBytes()));

		String emailScreen = getServletContext().getInitParameter("assetXslPath") + "email_submission.xsl";
		String emailTemplateStr = ReadFile.getSkin(emailScreen);
		String emailStr = transformerHelper.getHtmlStr(xmlStr, new ByteArrayInputStream(emailTemplateStr.getBytes()));

		String toolboxSkinPath = getServletContext().getInitParameter("assetPath") + "toolbox_1col.html";
		String skinHtmlStr = null;

		String skinUrl = form.getSkinUrl();
		String skinCssSelector = form.getSkinSelector();

		if (skinUrl.length() > 0 && skinCssSelector.length() > 0)
		{
			skinHtmlStr = SkinReader.getSkinByUrl(form);
		}
		else
		{
			skinHtmlStr = ReadFile.getSkin(toolboxSkinPath);
		}

		skinHtmlStr = skinHtmlStr.replace("{TITLE}", form.getTitle());
		skinHtmlStr = skinHtmlStr.replace("{CONTENT}", htmlStr);

		if (displayXml)
		{
			System.out.println(xmlStr);
		}
		
		if(request.getParameter("XSL") != null && request.getParameter("XSL").equals("NONE"))
		{
			response.setContentType("text/xml");
			response.getWriter().println(xmlStr);
			return;
		}

		if (isSubmissionValid)
		{
			if (form.getStatus().equals("live"))
			{
				int count = form.getSubmissionCount() + 1;
				formDAO.updateFormSubmissionCount(formID, count);
			}

			String redirectId = "" + formID;
			if (prettyUrl)
			{
				redirectId = form.getPrettyUrl();
			}

			response.sendRedirect(getServletContext().getContextPath() + "/completed/" + redirectId);
			return;
		}

		response.getWriter().println(skinHtmlStr);
	}

	protected ListMultimap<String, String> getFormFields(HttpServletRequest request)
	{
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

		return parameterMap;
	}

	protected Message processQuestion(Question question, String answer)
	{
		Message message = null;
		if (question.isRequired() && answer.length() == 0)
		{
			message = new Message();
			message.setQuestionId(question.getId());
			message.setQuestionNumber(question.getNumber());
			message.setType("error");
			message.setSubtype("required");
			message.setLabel("Question " + question.getNumber() + " is required.");
		}
		else if (question.getFilter().equals("email"))
		{
			EmailValidator emailValidator = EmailValidator.getInstance();
			/*
			 * if question is required and the answer is not a valid email
			 */
			if (!emailValidator.isValid(answer) && question.isRequired())
			{
				message = new Message();
				message.setQuestionId(question.getId());
				message.setQuestionNumber(question.getNumber());
				message.setType("error");
				message.setSubtype("email");
				message.setLabel("Question " + question.getNumber() + " requires a valid email address.");
			}
			/*
			 * if question is not required but is answered with an invalid email
			 */
			else if (!emailValidator.isValid(answer) && !question.isRequired() && answer.length() > 0)
			{
				message = new Message();
				message.setQuestionId(question.getId());
				message.setQuestionNumber(question.getNumber());
				message.setType("error");
				message.setSubtype("email");
				message.setLabel("Question " + question.getNumber() + " requires a valid email address.");
			}
		}
		else if (question.getFilter().equals("date"))
		{
			Pattern pattern = Pattern.compile("(0?[1-9]|1[012])/(0?[1-9]|[12][0-9]|3[01])/((19|20)\\d\\d)");
			Matcher matcher = pattern.matcher(answer);
			/*
			 * if question is required and answer doesn't match desired format
			 */
			if (question.isRequired())
			{
				if (!matcher.matches())
				{
					message = new Message();
					message.setQuestionId(question.getId());
					message.setQuestionNumber(question.getNumber());
					message.setType("error");
					message.setSubtype("date");
					message.setLabel("Question " + question.getNumber() + " requires a valid date.");
				}
			}
			else if (!question.isRequired() && answer.length() > 0)
			{
				if (!matcher.matches())
				{
					message = new Message();
					message.setQuestionId(question.getId());
					message.setQuestionNumber(question.getNumber());
					message.setType("error");
					message.setSubtype("date");
					message.setLabel("Question " + question.getNumber() + " requires a valid date.");
				}
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
