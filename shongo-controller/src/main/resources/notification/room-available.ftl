<#--
  -- Template for rendering {@link RoomParticipantNotification}
  -->
<#include "aliases.ftl">
<#---->
<#assign indent = 24>
<#---->
${context.message("room.available.description", roomName, context.formatInterval(roomEndpoint.originalSlot))}
<#-- Aliases -->
<#if aliases?? && aliases?has_content>

<#assign aliasesLabel = context.message("target.room.aliases")>
${context.width(indent + (aliasesLabel?length + 1) / 2 + 1, "----- " + aliasesLabel)} -----
<@formatAliases aliases=aliases/>
</#if>
<#if pin??>
${context.message(indent, "room.pin")}: ${pin}
</#if>
<#if userPin??>
${context.message(indent, "room.userPin")}: ${userPin}
</#if>
<#if adminPin??>
${context.message(indent, "room.adminPin")}: ${adminPin}
</#if>
<#if roomEndpoint.participants?has_content>

${context.message("room.available.participants")}:
<#list roomEndpoint.participants as participant>
 - ${context.message("room.available.participant", participant.personFullName, context.message("room.available.participant.role." + participant.role))}
</#list>
</#if>
