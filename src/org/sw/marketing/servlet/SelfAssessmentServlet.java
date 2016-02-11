package org.sw.marketing.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

@WebServlet("/self-assessment/*")
public class SelfAssessmentServlet extends HttpServlet
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
		
		boolean formSubmitted = false;
		
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
		catch(NumberFormatException e)
		{
			prettyUrl = true;
			formPrettyUrl = request.getPathInfo().substring(1);
		}		

		Form form = null;
		if(prettyUrl)
		{
			form = formDAO.getFormByPrettyUrl(formPrettyUrl);	
		}
		else
		{
			form = formDAO.getForm(formID);			
		}	
		
		if(form != null)
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
			tempSubmissionDAO.insert(formID, SESSION_ID, IP_ADDRESS);
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
							if(question.getType().startsWith("TEXT"))
							{
								answer.setMultipleChoice(false);
							}
							else
							{
								answer.setMultipleChoice(true);
							}
							answer.setAnswerValue(answerStr);
							
							
							if(form.getStatus().equals("live"))
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
			if(tempSubmissionAnswerDAO.getSubmissionAnswersByPage(submission) != null)
			{
				submission.getAnswer().addAll(tempSubmissionAnswerDAO.getSubmissionAnswersByPage(submission));
			}
		if(messageList == null)
		{
			if(paramAction != null && paramAction.equals("SUBMIT_FORM"))
			{
				/*
				 * don't check if survey live so that you can still see thank you screen!
				 */
				formSubmitted = true;
				
				if(form.getStatus().equals("live"))
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
		
		if(form.getType().equals("survey"))
		{
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
		}
		else if(form.getType().equals("self_assessment"))
		{
			if(currentPageQuestions != null)
			{
				form.getQuestion().addAll(currentPageQuestions);
			}
			
			java.util.List<Data.PossibleAnswer> possibleAnswers = answerDAO.getPossibleAnswersByForm(formID);
			if(possibleAnswers != null)
			{
				data.getPossibleAnswer().addAll(possibleAnswers);
			}
		}

		form.setCurrentPage(currentPage);
		form.setLastPage(lastPage);
		data.getForm().add(form);

		/*
		 * generate output
		 */
		TransformerHelper transformerHelper = new TransformerHelper();
		transformerHelper.setUrlResolverBaseUrl(getServletContext().getInitParameter("assetXslFormsPath"));
		
		String xmlStr = transformerHelper.getXmlStr("org.sw.marketing.data.form", data);	
		
		String xslScreen = getServletContext().getInitParameter("assetXslPath");
		if(form.getType().equals("survey"))
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
		
		if(skinUrl.length() > 0 && skinCssSelector.length() > 0)
		{
			skinHtmlStr = getSkinByUrl(skinUrl, skinCssSelector);
		}
		else
		{
			skinHtmlStr = ReadFile.getSkin(toolboxSkinPath);
		}
		
		skinHtmlStr = skinHtmlStr.replace("{NAME}", form.getTitle());
		skinHtmlStr = skinHtmlStr.replace("{CONTENT}", htmlStr);

		if(displayXml)
		{
			System.out.println(xmlStr);
		}

		response.getWriter().println(skinHtmlStr);
		

		if(formSubmitted)
		{
//			try
//			{
//		    	Email email = new HtmlEmail();
//		    	email.setHostName("mailrelay.sw.org");
//		    	email.setSmtpPort(465);
//		    	email.setAuthenticator(new DefaultAuthenticator("minja.gaso@bswhealth.org", "Zaboravi90"));
//		    	email.setSSLOnConnect(true);
//		    	email.setFrom("minja.gaso@bswhealth.org");
//		    	email.setSubject(form.getTitle());
//		    	email.setMsg(emailStr);
//		    	
//		    	String[] toEmails = { "minja.gaso@outlook.com", "minja.gaso@bswhealth.org" };
//		    	email.addTo(toEmails);
//		    	email.send();
//			}
//			catch (EmailException e)
//			{
//				e.printStackTrace();
//			}
			
			int count = form.getSubmissionCount() + 1;
			formDAO.updateFormSubmissionCount(formID, count);
			
			java.util.List<Answer> answers = submissionAnswerDAO.getSubmissionAnswers(submission.getId());
			if(answers != null)
			{
				java.util.Iterator<Answer> answersIter = answers.iterator();
				int selfAssessmentScore = 0;
				while(answersIter.hasNext())
				{
					Answer answer = answersIter.next();
					selfAssessmentScore += Integer.parseInt(answer.getAnswerValue());
				}
				request.setAttribute("SELF_ASSESSMENT_SCORE", selfAssessmentScore);
				request.getRequestDispatcher("/completed/" + form.getId()).forward(request, response);
			}
			
//			response.sendRedirect(getServletContext().getContextPath() + "/completed/" + formID);
//			return;
		}		
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
	
	public String getSkinByUrl(String skinUrl, String cssSelector)
	{
		/*
		 * page to open
		 */
//		String skinUrl = "http://www.sw.org/bone-joint-institute/bone-joint-landing";
		
		/*
		 * replaces content within cssSelector with {CONTENT} variable
		 */
//		String cssSelector = ".landingDetail";
		
		InputStream urlInputStream = null;
		Document document = null;
		try
		{
			urlInputStream = new URL(skinUrl).openStream();
			document = Jsoup.parse(urlInputStream, "CP1252", skinUrl);
			String url = document.baseUri();
			boolean isHttp = false;
			boolean isHttps = false;
			if (url.substring(0, 5).equals("http:"))
			{
				isHttp = true;
			}
			else if (url.substring(0, 6).equals("https:"))
			{
				isHttps = true;
			}
			int position = -1;
			if (url.indexOf("/") > -1)
			{
				position = url.indexOf("/");
			}
			String domain = url.substring(7);
			if (domain.indexOf("/") > -1)
			{
				position = domain.indexOf("/");
				domain = domain.substring(0, position);
			}

			if (isHttp)
			{
				domain = "http://" + domain + "/";
			}
			else if (isHttps)
			{
				domain = "https://" + domain + "/";
			}

			for(Element hrefElement : document.select("a, link"))
			{
				hrefElement.attr("href", hrefElement.absUrl("href"));
			}
			for(Element srcElement : document.select("button, img, input, script"))
			{
				srcElement.attr("src", srcElement.absUrl("src"));
			}
			for(Element style : document.select("style"))
			{
				String inlineText = style.html().trim();
				inlineText = inlineText.replace("(/resources", "(" + domain + "/resources");
				style.html(inlineText);
			}
			
			document.select(cssSelector).html("{CONTENT}");
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return document.html();
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
