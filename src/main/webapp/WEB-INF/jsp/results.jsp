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
        <p>Search results</p>
        <c:choose>
            <c:when test="${not empty found}">
                <table>
                    <tr>
                        <td><form:form action="sorting" method="GET" enctype="utf8" name='sortingForm'>
                            <button  id="sorting" type="submit">
                                ${sorting}
                            </button>
                        </form:form></td>
                        <!--<td><input type="submit" class="button" name="sorting" value="по релевантности" ></td>-->
                    </tr>
                    <tr>
                        <td><form:form action="next" method="GET" enctype="utf8" name='sortingForm'>
                                <button  id="next" type="submit">
                                    next 10 results
                                </button>
                        </form:form></td>
                    </tr>
                    <c:forEach items="${found}" var="page">
                        <tr>
                            <td>
                                <a href="<c:url value="${page.plainUrl}" />">
                                    <b>${page.title}</b><br>
                                    <p style='color: green'>${page.plainUrl}</p>
                                    <p style='color: black'>${page.content}</p>
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                </table>
            </c:when>
            <c:otherwise>
                There are no results
            </c:otherwise>
        </c:choose>

    </body>
</html>
