<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ page session="false"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Search page</title>
    </head>

    <body>
        <p>Search keywords in indexed web pages</p>
        <form:form modelAttribute="q" action="search" method="GET" enctype="utf8" name='searchForm'>

            <table border="1" border-collapse="collapse" style="width:50%">
                <tr>
                    <td>Enter keywords to search</td>
                    <td><form:input path="title" value="  " /></td>
                </tr>
            </table>
            <button  id="search" type="submit">
                Search
            </button>
        </form:form>
        <a href="<c:url value="/index" />">Enter web page to index</a>
    </body>
</html>
