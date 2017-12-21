<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<jsp:useBean id="requestQuery" scope="request" class="org.ohnlp.ir.emirs.model.Query"/>

<div class="container">
    <form:form method="POST" action="/_query" modelAttribute="requestQuery">
        <form:label path="unstructured">Query</form:label>
        <form:input path="unstructured"/>
        <input type = "submit" value = "Submit" />
    </form:form>
</div>

