<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" />
	<xsl:template match="/">
	    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet"/>
	    <link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css" rel="stylesheet"/>
	    <link href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:400,200" rel="stylesheet"/>
    	<link rel="stylesheet" type="text/css" href="/css/main.css?v=1" />
	    <style type="text/css">
	    body { margin: 24px; }	    
	    </style>
		<form action="" method="post" name="portal_form" id="public-form">
			<input type="hidden" name="ACTION" />
			<input type="hidden" name="FORM_ID" value="{/data/form/id}" />
			<input type="hidden" name="PREVIOUS_PAGE" value="{/data/form/currentPage}" />
			<input type="hidden" name="CURRENT_PAGE" value="{/data/form/currentPage}" />
			<h1 class="form-group">Submissions</h1>
			<xsl:for-each select="/data/form/question">
				<xsl:variable name="index" select="position()" />
				<xsl:variable name="questionId" select="id" />
				<div class="form-group">
					<xsl:choose>
						<xsl:when test="type = 'text' or type = 'textarea'">
							<xsl:variable name="totalCount" select="count(/data/submission/answer[questionId = $questionId])" />
							<xsl:variable name="answeredCountStr">
								<xsl:for-each select="/data/submission/answer[questionId = $questionId]">
									<xsl:if test="string-length(answerValue) &gt; 0">
										<xsl:value-of select="'1'" />
									</xsl:if>
								</xsl:for-each>
							</xsl:variable>
							<xsl:variable name="answeredCount" select="string-length($answeredCountStr)" />
							<table class="table table-bordered table-condensed">
								<thead>
									<tr>
										<th class="col-xs-10"><xsl:value-of select="concat(number, '. ', label)" /></th>
										<th class="col-xs-2 text-center">Totals</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<th class="text-right">Answered</th>
										<td class="text-center"><xsl:value-of select="$answeredCount" /></td>
									</tr>
									<tr>
										<th class="text-right">Unanswered</th>
										<td class="text-center"><xsl:value-of select="$totalCount - $answeredCount" /></td>
									</tr>
								</tbody>
							</table>							
						</xsl:when>
						<xsl:otherwise>						
							<xsl:variable name="totalCount" select="count(/data/submission/answer[questionId = $questionId])" />
							<xsl:variable name="answeredCountStr">
								<xsl:for-each select="/data/submission/answer[questionId = $questionId]">
									<xsl:if test="string-length(answerValue) &gt; 0">
										<xsl:value-of select="'1'" />
									</xsl:if>
								</xsl:for-each>
							</xsl:variable>
							<xsl:variable name="answeredCount" select="string-length($answeredCountStr)" />
							<table class="table table-bordered table-condensed">
								<thead>
									<tr>
										<th class="col-xs-10"><xsl:value-of select="concat(number, '. ', label)" /></th>
										<th class="col-xs-2 text-center">Totals</th>
									</tr>
								</thead>
								<tbody>
									<xsl:for-each select="possibleAnswer">
										<xsl:variable name="possibleAnswerId" select="id" />
										<tr>
											<th class="text-right"><xsl:value-of select="label" /></th>
											<td class="text-center"><xsl:value-of select="count(/data/submission/answer[answerValue = $possibleAnswerId])" /></td>
										</tr>
									</xsl:for-each>
								</tbody>
							</table>
						</xsl:otherwise>
					</xsl:choose>					
				</div>
			</xsl:for-each>			
			<footer class="text-center">Provided by <em><a href="#">Interactive Marketing</a></em> at <em><a href="#">Baylor Scott &amp; White</a></em></footer>
		</form>
    	<script src="/js/form.js?v=1"></script>
	</xsl:template>
</xsl:stylesheet>