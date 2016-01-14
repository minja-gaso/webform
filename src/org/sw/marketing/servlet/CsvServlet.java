package org.sw.marketing.servlet;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

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
import org.sw.marketing.dao.user.UserDAO;
import org.sw.marketing.data.form.Data;
import org.sw.marketing.data.form.Data.Form;
import org.sw.marketing.data.form.Data.Submission;
import org.sw.marketing.data.form.Data.Form.Question;
import org.sw.marketing.data.form.Data.Form.Question.PossibleAnswer;
import org.sw.marketing.data.form.Data.Submission.Answer;
import org.sw.marketing.transformation.TransformerHelper;

import com.opencsv.CSVWriter;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

@WebServlet("/csv")
public class CsvServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		/*
		 * DAO Initialization
		 */
		UserDAO userDAO = DAOFactory.getUserDAO();
		FormDAO formDAO = DAOFactory.getFormDAO();
		QuestionDAO questionDAO = DAOFactory.getQuestionDAO();
		AnswerDAO answerDAO = DAOFactory.getPossibleAnswerDAO();
		SubmissionDAO submissionDAO = DAOFactory.getSubmissionDAO();
		SubmissionAnswerDAO submissionAnswerDAO = DAOFactory.getSubmissionAnswerDAO();
		
		Data data = new Data();
		long formID = Long.parseLong("1161948810985866242");
		Form form = formDAO.getForm(formID);
		
		java.util.List<Submission> submissions = submissionDAO.getSubmissions(formID);	
		java.util.List<String> headers = new java.util.ArrayList<String>();
		java.util.List<Long> answeredIds = new java.util.ArrayList<>();
		java.util.List<Question> questions = questionDAO.getQuestions(formID);

		/*
		 * add questions to xml
		 */
		if(questions != null)
		{
			for(Question question : questions)
			{
				long questionID = question.getId();
				java.util.List<PossibleAnswer> answerList = answerDAO.getPossibleAnswers(questionID);
				if(answerList != null)
				{
					java.util.Iterator<PossibleAnswer> answerListIter = answerList.iterator();
					while(answerListIter.hasNext())
					{
						headers.add(question.getNumber() + ". " + answerListIter.next().getLabel());
					}
				}
				else
				{
					headers.add(question.getNumber() + ". " + question.getLabel());
				}
				form.getQuestion().add(question);
			}
		}


		/*
		 * add submissions to xml
		 */
		if(submissions != null)
		{
			java.util.List<String> row = new java.util.ArrayList<String>();
			for(Submission submission : submissions)
			{
				java.util.List<Answer> submissionAnswers = submissionAnswerDAO.getSubmissionAnswers(submission.getId());
				if(submissionAnswers != null)
				{
					for(Answer answer : submissionAnswers)
					{
						if(answer.isMultipleChoice())
						{
							long answerID = Long.parseLong(answer.getAnswerValue());
							String answerLabel = answerDAO.getPossibleAnswerLabel(answerID);
							answer.setAnswerLabel(answerLabel);
							System.err.println("Answer:" + answerLabel + ":::" + answer.getAnswerValue());
							answeredIds.add(Long.parseLong(answer.getAnswerValue()));
						}
					}
					submission.getAnswer().addAll(submissionAnswers);
				}
				data.getSubmission().add(submission);
			}
		}
		data.getForm().add(form);

		String xmlStr = TransformerHelper.getXmlStr("org.sw.marketing.data.form", data);	
//		System.out.println(xmlStr);
		
		try
		{
			CSVWriter writer = new CSVWriter(new FileWriter("E:\\sample2.csv"), ',');
			writer.writeNext(headers.toArray(new String[headers.size()]));

			java.util.List<String> columnCells = new java.util.ArrayList<String>();
			
			
			for(Submission submission : submissions)
			{
				java.util.List<String> cellsList = new java.util.ArrayList<String>();
				java.util.List<Answer> submissionAnswers = submissionAnswerDAO.getSubmissionAnswers(submission.getId());
				if(submissionAnswers != null)
				{
					java.util.Map<Long, Long> questionIdMap = new java.util.LinkedHashMap<Long, Long>();
					for(Answer answer : submissionAnswers)
					{
						questionIdMap.put(answer.getQuestionId(), answer.getQuestionId());
					}
					
					for(Answer answer : submissionAnswers)
					{
						if(answer.isMultipleChoice())
						{
							long answerID = Long.parseLong(answer.getAnswerValue());
							
							if(questionIdMap.get(answer.getQuestionId()) != (long) 0)
							{
								String answerLabel = answerDAO.getPossibleAnswerLabel(answerID);
								List<PossibleAnswer> possibleAnswers = answerDAO.getPossibleAnswers(answer.getQuestionId());
								java.util.Iterator<PossibleAnswer> possibleAnswersIter = possibleAnswers.iterator();
								while(possibleAnswersIter.hasNext())
								{
									PossibleAnswer possibleAnswer = possibleAnswersIter.next();
//									if(answerID == possibleAnswer.getId())
									if(answeredIds.contains(answerID))
									{
										cellsList.add("1");
									}
									else
									{
										cellsList.add("0");
									}
									
									System.out.println("Possible Answer: " + possibleAnswer.getLabel());
								}
							}
							
							questionIdMap.put(answer.getQuestionId(), (long) 0);
						}
						else
						{
							cellsList.add(answer.getAnswerValue());
						}
					}
					submission.getAnswer().addAll(submissionAnswers);
					writer.writeNext(cellsList.toArray(new String[cellsList.size()]));
				}
				data.getSubmission().add(submission);
			}
			
			writer.writeNext(columnCells.toArray(new String[columnCells.size()]));
			
			writer.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		for()

		for(Submission submission : submissions)
		{
			java.util.List<Answer> submissionAnswerList = submissionAnswerDAO.getSubmissionAnswers(submission.getId());
			if(submissionAnswerList != null)
			{
				for(Answer answer : submissionAnswerList)
				{
					if(answer.isMultipleChoice())
					{
						long answerID = Long.parseLong(answer.getAnswerValue());
						String answerLabel = answerDAO.getPossibleAnswerLabel(answerID);
						answer.setAnswerLabel(answerLabel);
//						System.out.println(answerLabel);
					}
					else
					{
//						System.out.println(answer.getAnswerValue());
					}
				}
				submission.getAnswer().addAll(submissionAnswerList);
			}
			data.getSubmission().add(submission);
		}
	}
}
