<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page import="edu.mayo.bsi.semistructuredir.cdm.model.*"%>

<html>

<head>
<title>Topic Search Results</title>
</head>

<body>
    <font color="red">${topic_result_ex}</font>
    <c:forEach items=request.getParameter("topic_id") var="id">
        <b>Topic ID:</b> ${id} <br>
        <b>Topic Description:</b> ${"${id}_result"}.topicDesc <br>
        <b>Topic Query:</b> ${"${id}_result"}.query <br>
        <tr>
            <td>Result ID</td>
            <td>Score</td>
            <td>Document Text</td>
        </tr>
        <c:forEach items=${"${id}_result"} var="result">
            <tr>
                <td>${result.result}</td>
                <td>${result.score}</td>
                <td>${result.documentText}</td>
            </tr>
        </c:forEach>
    </c:forEach>
</body>

</html>