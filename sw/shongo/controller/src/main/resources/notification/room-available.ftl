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

${context.message("room.aliases")}:
<#if pin??>
${context.message(indent, "room.pin")}: ${pin}
</#if>
<@formatAliases aliases=aliases/>
</#if>
<#if roomEndpoint.participants?has_content>

${context.message("room.available.participants")}:
<#list roomEndpoint.participants as participant>
 - ${context.message("room.available.participant", participant.personFullName, context.message("room.available.participant.role." + participant.role))}
</#list>
</#if>
