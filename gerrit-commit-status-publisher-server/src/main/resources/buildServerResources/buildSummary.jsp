<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--@elvariable id="gerrit_url" type="java.lang.String"--%>
<c:if test="${not empty gerrit_url}">
    <tr><td></td>
        <td class="st">
            <a href="${gerrit_url}">View in gerrit</a>
        </td>
    </tr>
</c:if>