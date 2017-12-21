<!DOCTYPE html>
<jsp:useBean id="results" scope="request" class="org.ohnlp.ir.emirs.model.QueryResult"/>
<jsp:useBean id="query" scope="request" class="org.ohnlp.ir.emirs.model.Query"/>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:choose>
        <c:when test="${not empty param.title}">
            <title>${param.title} - EMIRS</title>
        </c:when>
        <c:otherwise>
            <title>EMIRS - Electronic Medical Information Retrieval System</title>
        </c:otherwise>
    </c:choose>

    <script src="webjars/jquery/3.2.1/jquery.min.js"></script>

    <link rel="stylesheet" href="webjars/bootstrap/3.3.7/css/bootstrap.min.css">
    <link rel="stylesheet" href="webjars/bootstrap/3.3.7/css/bootstrap-theme.min.css">
    <script src="webjars/bootstrap/3.3.7/js/bootstrap.min.js"></script>
    <script src="webjars/angularjs/1.6.6/angular.min.js"></script>
    <link rel="shortcut icon" href="<c:url value="img/favicon.ico"/>"/>

</head>
<body class="mt-0" style="background-color: #bebebe; height:100%">
<div class="row" id="header" style="background-color:#003DA5;height: 100px; margin:0; padding:0;">
    <div class="col-xs-1 " id="logo">
        <a href="<c:url value="/"/>">
            <img src="<c:url value="img/MC_stack_wht.png"/>" height="75px" alt="MC Logo"
                 style="float: left; padding-left: 15px; padding-top: 15px; margin-top:0"/>
        </a>
    </div>
    <div class="col-sm-11" id="header-text">
        <h2 class="text-center" style="padding-bottom: 30px;color: #ffffff; padding-top: 30px; margin:0 auto">Electronic
            Medical Information Retrieval
            System</h2>
    </div>
</div>
<div class="row" id="search">
    <div class="col-xs-12 text-center" style="padding-top: 20px; padding-bottom: 20px; border-bottom: 1px solid #808080">
        <jsp:include page="search.jsp"/>
    </div>
</div>
<c:if test="${not empty results.query}">
    <div class="row" id="advanced-search">
        <jsp:include page="cdm_editor.jsp"/>
    </div>
    <div class="row" id="results">
        <div id="sidebar" class="col-sm-2"
             style="float:left;">
            <jsp:include page="sidebar.jsp"/>
        </div>
        <div id="content" class="col-xs-10"
             style="float:right; padding-top: 10px; border-left: 1px solid #808080">
            <jsp:include page="results.jsp"/>
        </div>
    </div>
</c:if>

</body>
</html>