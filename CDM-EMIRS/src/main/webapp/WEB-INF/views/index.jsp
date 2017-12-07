<!DOCTYPE html>
<jsp:useBean id="hits" scope="request" type="org.ohnlp.ir.emirs.model.QueryResult"/>

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

    <!-- Latest compiled and minified CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
          integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">

    <!-- Optional theme -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css"
          integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">

    <link rel="shortcut icon" href="<c:url value="img/favicon.ico"/>"/>

    <!-- Latest compiled and minified JavaScript -->
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
            integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
            crossorigin="anonymous"></script>


</head>
<body class="mt-0" style="background-color: #bebebe; height:100%">
<div id="header" style="background-color:#003DA5;height: 100px; margin:0; padding:0;">
    <a href="<c:url value="/"/>"><img src="<c:url value="img/MC_stack_wht.png"/>" height="75px" alt="MC Logo"
                                      style="float: left;padding-left: 15px; padding-top: 15px; margin-top:0"/></a>
    <h2 class="text-center" style="padding-bottom: 30px;color: #ffffff; padding-top: 30px; margin:0 auto">Electronic
        Medical Information Retrieval
        System</h2>
</div>
<c:choose>
    <c:when test="${not empty hits}">
        <div id="search" class="container"
             style="float:top; height:10%; min-height:10vh; width:100%; border-bottom: 1px solid #808080">
            <div id="search-with-result" class="container"
                 style="float:top; height:10%; min-height:10vh; width:100%; border-bottom: 1px solid #808080">
                <jsp:include page="search.jsp"/>
            </div>
        </div>
        <div id="sidebar" class="container"
             style="float:left; height:90%; min-height: 90vh; width:15%; border-right: 1px solid #808080">
            <jsp:include page="sidebar.jsp"/>
        </div>
        <div id="content" class="container"
             style="float:right; height:90%; min-height: 90vh; width:85%;">
            <jsp:include page="hits.jsp"/>
        </div>
    </c:when>
    <c:otherwise>
        <div id="search" class="container"
             style="float:top; height:10%; min-height:10vh; width:100%; border-bottom: 1px solid #808080">
            <jsp:include page="search.jsp"/>
        </div>
        <div id="sidebar" class="container"
             style="float:left; height:90%; min-height: 90vh; width:15%; border-right: 1px solid #808080">
            <jsp:include page="sidebar.jsp"/>

        </div>
        <div id="content" class="container"
             style="float:right; height:90%; min-height: 90vh; width:85%;">
            <jsp:include page="hits.jsp"/>
        </div>
    </c:otherwise>
</c:choose>

</body>
</html>