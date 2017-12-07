<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="hits" scope="request" type="org.ohnlp.ir.emirs.model.QueryResult"/>

<c:forEach var="result" items="${hits.}"
<p>Results Content</p>