package org.sw.marketing.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

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
import org.sw.marketing.data.form.Data.Submission;
import org.sw.marketing.data.form.Data.Form.Question;
import org.sw.marketing.data.form.Data.Form.Question.PossibleAnswer;
import org.sw.marketing.data.form.Data.Submission.Answer;
import org.sw.marketing.transformation.TransformerHelper;
import org.sw.marketing.util.ReadFile;

@WebServlet("/submission/*")
public class SubmissionServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String paramFormID = request.getPathInfo().substring(1);
		int formID = Integer.parseInt(paramFormID);
		
		FormDAO formDAO = DAOFactory.getFormDAO();
		QuestionDAO questionDAO = DAOFactory.getQuestionDAO();
		AnswerDAO answerDAO = DAOFactory.getPossibleAnswerDAO();
		SubmissionDAO submissionDAO = DAOFactory.getSubmissionDAO();
		SubmissionAnswerDAO submissionAnswerDAO = DAOFactory.getSubmissionAnswerDAO();
		
		Data data = new Data();
		Form form = formDAO.getForm(formID);
		if(form != null)
		{
			java.util.List<Question> questionList = questionDAO.getQuestions(form.getId());
			if(questionList != null)
			{
				for(Question question : questionList)
				{
					java.util.List<PossibleAnswer> possibleAnswerList = answerDAO.getPossibleAnswers(question.getId());
					if(possibleAnswerList != null)
					{
						question.getPossibleAnswer().addAll(possibleAnswerList);
					}
				}
				form.getQuestion().addAll(questionList);
			}
			data.getForm().add(form);
			
			java.util.List<Submission> submissionList = submissionDAO.getSubmissions(form.getId());
			if(submissionList != null)
			{
				for(Submission submission : submissionList)
				{
					java.util.List<Answer> submissionAnswerList = submissionAnswerDAO.getSubmissionAnswers(submission.getId());
					if(submissionAnswerList != null)
					{
						for(Answer answer : submissionAnswerList)
						{
							if(answer.isMultipleChoice())
							{
								int answerID = Integer.parseInt(answer.getAnswerValue());
								String answerLabel = answerDAO.getPossibleAnswerLabel(answerID);
								answer.setAnswerLabel(answerLabel);
							}
						}
						submission.getAnswer().addAll(submissionAnswerList);
					}
					data.getSubmission().add(submission);
				}
			}
		}
		
		/*
		 * generate output
		 */
		String xmlStr = TransformerHelper.getXmlStr("org.sw.marketing.data.form", data);		
		String xslScreen = getServletContext().getInitParameter("assetXslPath") + "submission.xsl";
		String xslStr = ReadFile.getSkin(xslScreen);
		String htmlStr = TransformerHelper.getHtmlStr(xmlStr, new ByteArrayInputStream(xslStr.getBytes()));
		
		/*
		 * display output
		 */
		System.out.println(xmlStr);
		
		response.setContentType("text/html");
		response.getWriter().println(htmlStr);
	}
}
