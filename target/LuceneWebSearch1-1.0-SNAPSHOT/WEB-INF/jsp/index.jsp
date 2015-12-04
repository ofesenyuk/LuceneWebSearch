<%@page contentType="text/html" pageEncoding="UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ page session="false"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Create index page</title>
    </head>

    <body>
        <p>Create index for web page</p>
        <form:form modelAttribute="q" action="indexing"  method="POST" enctype="utf8" name='indexForm'>

            <table border="0" border-collapse="collapse" style="width:50%">
                <tr>
                    <td>Enter web-page address for indexing</td>
                    <td><form:input path="url"/></td>
                    <td><form:errors path="url" element="div"/></td>
                </tr>
                <tr>
                    <td>Enter number web-pages reviewed from current</td>
                    <td><form:input path="title"/></td>
                    <td><form:errors path="title" element="div"/></td>
                </tr>
            </table>
            <button type="submit">
                Index
            </button>
        </form:form>
    </body>
</html>
