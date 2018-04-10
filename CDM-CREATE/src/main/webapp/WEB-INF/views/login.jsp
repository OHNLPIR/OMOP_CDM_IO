<!DOCTYPE html>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
    <title>CREATe - Login</title>

    <script src="webjars/jquery/3.2.1/jquery.min.js"></script>

    <link rel="stylesheet" href="webjars/bootstrap/3.3.7/css/bootstrap.min.css">
    <link rel="stylesheet" href="webjars/bootstrap/3.3.7/css/bootstrap-theme.min.css">
    <script src="webjars/bootstrap/3.3.7/js/bootstrap.min.js"></script>
    <script src="webjars/angularjs/1.6.6/angular.min.js"></script>
    <link rel="shortcut icon" href="<c:url value="img/favicon.ico"/>"/>
    <link href="css/search.css" rel="stylesheet">
</head>
<body class="mt-0"
      style="background-color: rgb(226,226,226); height:100%">
<div class="row" id="page-header">
    <div class="col-xs-1 " id="logo" style="z-index: 100">
        <a href="<c:url value="/"/>">
            <img src="<c:url value="img/MC_stack_wht.png"/>" height="100px" alt="MC Logo"
                 style="float: left; padding: 15px; margin-top:0"/>
        </a>
    </div>
    <div class="col-sm-12" id="header-text">
        <h2 class="text-center"
            style="line-height:40px; height: 100px; padding-bottom: 30px;color: #ffffff; padding-top: 30px; margin:0 auto">
            Cohort Retrieval Enhanced by the Analysis of Text</h2>
    </div>
</div>
<!-- Navbar -->
<div class="navbar navbar-default"></div>
<div id="main-login-form" class="row">
    <div class="col-xs-4"></div>
    <div class="col-xs-4">
        <div class="panel panel-default">
            <div class="panel-heading clearfix text-center"><h3>Log In</h3></div>
            <div class="panel-body">
                <form name='loginForm'
                      action="<c:url value="/login"/>" method='POST' class="form-group form-horizontal">
                    <div class="row">
                        <div class="col-xs-1"></div>
                        <div class="col-xs-10">
                            <div class="input-group">
                                <span class="input-group-addon"><i class="glyphicon glyphicon-user"></i></span>
                                <input id="user" type="text" class="form-control" name="username" placeholder="LAN ID">
                            </div>
                            <div class="input-group">
                                <span class="input-group-addon"><i class="glyphicon glyphicon-lock"></i></span>
                                <input id="password" type="password" class="form-control" name="password"
                                       placeholder="Password">
                            </div>
                        <br/>
                            <input class="btn btn-block btn-primary" name="submit" type="submit"
                                   value="Sign In"/>
                        </div>
                        <div class="col-xs-1"></div>
                    </div>
                    <input type="hidden" name="${_csrf.parameterName}"
                           value="${_csrf.token}"/>
                </form>
            </div>
        </div>
    </div>
</div>
<div class="col-xs-4"></div>
</div>
</body>
</html>