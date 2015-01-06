<#include "header.ftl">
<h1>Error ${status}
    <small>${reasonPhrase}</small>
</h1>
<#if reasonPhrase != description><p>${description}</p></#if>
<#if exception??>
<pre>${stackTrace}</pre>
</#if>
<#include "footer.ftl">
