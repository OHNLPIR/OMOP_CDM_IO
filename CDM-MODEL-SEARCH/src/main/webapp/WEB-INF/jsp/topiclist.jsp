<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page import="edu.mayo.bsi.semistructuredir.cdm.model.*"%>

<html>

<head>
<title>Topic List</title>
</head>

<body>
    <font color="red">${topic_list_ex}</font>
    <c:forEach items=${topic_list} var="id">
        <b>Topic ID:</b> ${id.name} <br>
        <b>Topic Description:</b> ${id.description} <br>
    </c:forEach>
</body>

</html>