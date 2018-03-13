<#--
  -- Template for rendering {@link RoomGroupNotification}
  -->
<#include "aliases.ftl">
<#---->
<#assign indent = 24>
<#---->
<#if description?? && (description?length > 0)>
${context.message(indent, "room.description")}: ${context.indentNextLines(indent + 2, description)}

</#if>
<#-- Aliases -->
<#if aliases?? && aliases?has_content>
<#assign aliasesLabel = context.message("target.room.aliases")>
${context.width(indent + (aliasesLabel?length + 1) / 2 + 1, "----- " + aliasesLabel)} -----
<@formatAliases aliases=aliases/>
</#if>
<#if pin??>
${context.message(indent, "room.pin")}: ${pin}
</#if>
<#if adminPin??>
${context.message(indent, "room.adminPin")}: ${adminPin}
</#if>

<#-- Notifications -->
${notifications}

