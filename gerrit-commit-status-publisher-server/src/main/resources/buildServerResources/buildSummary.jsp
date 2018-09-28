<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--@elvariable id="gerrit_url" type="java.lang.String"--%>
<%--@elvariable id="sonar_url" type="java.lang.String"--%>
<c:if test="${not empty gerrit_url}">
    <tr><td></td>
        <td class="st">
            View in <a href="${gerrit_url}">gerrit</a>
            <c:if test="${not empty sonar_url}">/ <a href="${sonar_url}">sonar</a></c:if>
        </td>
    </tr>
</c:if>