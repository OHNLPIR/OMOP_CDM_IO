<%--suppress XmlDuplicatedId --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:useBean id="results" scope="request" type="org.ohnlp.ir.emirs.model.QueryResult"/>
<div class="panel-group">
    <c:forEach var="hit" items="${results.hits}">
        <div class="panel panel-default">
            <div class="panel-heading">
                <div class="panel-title pull-left h4">
                    <a data-toggle="collapse"
                       href="#${hit.doc.docLinkId}">${hit.doc.sectionName}
                        - MRN: ${hit.patient.id}
                        - Id: ${hit.doc.docLinkId}v${hit.doc.revision}
                        - Type: ${hit.doc.docType}
                    </a>
                </div>
                <div class="panel-title pull-right">Score: ${hit.score}</div>
                <div class="clearfix"></div>
            </div>
            <div id="${hit.doc.docLinkId}" class="panel-collapse collapse">
                <div class="panel-body">${fn:replace(hit.doc.text, '\\n', "<br/>")}</div>
            </div>
        </div>
    </c:forEach>
</div>

