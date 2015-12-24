package org.sw.marketing.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.code.linkedinapi.schema.Person;

@WebServlet("/signin")
public class LinkedInAuthentication extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final String consumerKey = "78u27r4jbd4ohs";
	private static final String consumerSecret = "uRdsiqrrItp4aCim";

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		HttpSession httpSession = request.getSession();
		
		LinkedInOAuthService oauthService = LinkedInOAuthServiceFactory.getInstance().createLinkedInOAuthService(consumerKey, consumerSecret);
		LinkedInRequestToken requestToken = oauthService.getOAuthRequestToken("http://localhost:8080/toolbox/callback");
		String authorizationUrl = requestToken.getAuthorizationUrl();
		httpSession.setAttribute("requestToken", requestToken);
		response.sendRedirect(authorizationUrl);
	}
}
