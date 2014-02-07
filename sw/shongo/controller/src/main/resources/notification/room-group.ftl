<#--
  -- Template for rendering {@link RoomGroupNotification}
  -->
<#include "aliases.ftl">
<#---->
<#assign indent = 23>
<#---->
<#if description??>
${context.message(23, "room.description")}: ${context.indentNextLines(indent + 2, description)}
</#if>
<#if pin??>
${context.message(23, "room.pin")}: ${pin}
</#if>
<#-- Aliases -->
<#if aliases?? && aliases?has_content>
${context.message(23, "room.aliases")}:
<@formatAliases aliases=aliases/>

</#if>
<#-- Notifications -->
${notifications}

