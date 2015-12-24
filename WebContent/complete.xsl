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
			<h1 class="form-group"><xsl:value-of select="/data/form/title" /></h1>
			<p>The form has been completed.  Thanks for taking it!</p>
			<footer class="text-center">Provided by <em><a href="#">Interactive Marketing</a></em> at <em><a href="#">Baylor Scott &amp; White</a></em></footer>
		</form>
    	<script src="/js/form.js?v=1"></script>
	</xsl:template>
</xsl:stylesheet>