<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="query" scope="request" class="org.ohnlp.ir.emirs.model.Query"/>

<div class="container">
    <form action = "<c:url value="/_query"/>" method = "POST">
        <label>
            Query: <input type="text" name="query" <c:if test="${not empty query.unstructured}">value="${query.unstructured}"</c:if>/>
        </label>
        <input type = "submit" value = "Submit" />
    </form>
</div>

