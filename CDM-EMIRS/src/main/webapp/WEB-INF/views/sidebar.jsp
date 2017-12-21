<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="results" scope="request" type="org.ohnlp.ir.emirs.model.QueryResult"/>
<div class="panel-group">
    <div class="panel panel-default">
        <div class="panel-heading">
            <h3 class="panel-title">
                <a data-toggle="collapse" href="#sidebar-patients">Matched Patients</a>
            </h3>
        </div>
        <div id="sidebar-patients" class="panel-collapse collapse">
            <div class="panel-body">
                <div class="panel-group">
                    <c:forEach var="patient" items="${results.patients}">
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h4 class="panel-title">
                                    <a data-toggle="collapse" href="#sidebar-${patient.id}">${patient.id}</a>
                                </h4>
                            </div>
                            <div id="sidebar-${patient.id}" class="panel-collapse collapse">
                                <div class="panel-body">MRN: ${patient.id}</div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>
    </div>
</div>
