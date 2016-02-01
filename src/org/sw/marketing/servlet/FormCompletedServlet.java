package org.sw.marketing.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.sw.marketing.dao.DAOFactory;
import org.sw.marketing.dao.form.FormDAO;
import org.sw.marketing.dao.question.QuestionDAO;
import org.sw.marketing.dao.score.ScoreDAO;
import org.sw.marketing.dao.submission.SubmissionAnswerDAO;
import org.sw.marketing.dao.submission.SubmissionDAO;
import org.sw.marketing.data.form.Data;
import org.sw.marketing.data.form.Data.Form;
import org.sw.marketing.data.form.Data.Score;
import org.sw.marketing.data.form.Data.Submission;
import org.sw.marketing.transformation.TransformerHelper;
import org.sw.marketing.util.DateToXmlGregorianCalendar;
import org.sw.marketing.util.ReadFile;
import org.sw.marketing.util.SkinReader;
import org.sw.marketing.util.SurveyHelper;

@WebServlet("/completed/*")
public class FormCompletedServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;


	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String SESSION_ID = request.getSession().getId();
		
		FormDAO formDAO = DAOFactory.getFormDAO();
		QuestionDAO questionDAO = DAOFactory.getQuestionDAO();
		SubmissionDAO submissionDAO = DAOFactory.getSubmissionDAO();
		SubmissionAnswerDAO submissionAnswerDAO = DAOFactory.getSubmissionAnswerDAO();

		Data data = new Data();
		/*
		 * get form
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
		
		/*
		 * initialize submission
		 */
		Submission submission = null;
		if(request.getSession().getAttribute("SUBMISSION") != null)
		{
			submission = (Submission) request.getSession().getAttribute("SUBMISSION");
			data.getSubmission().add(submission);
		}
		
		if(request.getAttribute("SELF_ASSESSMENT_SCORE") != null)
		{
			int score = (int) request.getAttribute("SELF_ASSESSMENT_SCORE");
			form.setScore(score);
		}
		
		form = SurveyHelper.verifyStartAndEndDate(form);

		if(form != null)
		{
			data.getForm().add(form);
		}
		
		ScoreDAO scoreDAO = DAOFactory.getScoreDAO();
		java.util.List<Score> scores = scoreDAO.getScores(formID);
		if(scores != null)
		{
			data.getScore().addAll(scores);
		}
		
		/*
		 * generate output
		 */
		TransformerHelper transformerHelper = new TransformerHelper();
		transformerHelper.setUrlResolverBaseUrl(getServletContext().getInitParameter("assetXslFormsPath"));
		
		String xmlStr = transformerHelper.getXmlStr("org.sw.marketing.data.form", data);
		String xslScreen = getServletContext().getInitParameter("assetXslPath") + "form_message.xsl";
		String xslStr = ReadFile.getSkin(xslScreen);
		String htmlStr = transformerHelper.getHtmlStr(xmlStr, new ByteArrayInputStream(xslStr.getBytes()));
		String toolboxSkinPath = getServletContext().getInitParameter("assetPath") + "toolbox_1col.html";
		String skinHtmlStr = null;
		
		String skinUrl = form.getSkinUrl();
		String skinCssSelector = form.getSkinSelector();
		
		if(skinUrl.length() > 0 && skinCssSelector.length() > 0)
		{
			skinHtmlStr = SkinReader.getSkinByUrl(form);
		}
		else
		{
			skinHtmlStr = ReadFile.getSkin(toolboxSkinPath);
		}
		
		skinHtmlStr = skinHtmlStr.replace("{TITLE}", form.getTitle());
		skinHtmlStr = skinHtmlStr.replace("{CONTENT}", htmlStr);

		/*
		 * display output
		 */
		System.out.println(xmlStr);
		
		response.setContentType("text/html");
		response.getWriter().println(skinHtmlStr);
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
