<#--
  -- Template for rendering {@link Target}
  -->
<#include "aliases.ftl">
<#---->
<#assign aliasValueAny = context.message('alias.value.any')>
${context.message(indent, "target.type")}: ${context.message("target.type." + target.type)}
<#-- Value -->
<#if target.class.simpleName == "Value">
<#---->
<#-- Alias -->
<#elseif target.class.simpleName == "Alias">
<#if target.technologies?has_content>
${context.message(indent, "target.technologies")}: <#list target.technologies as technology>${technology.getName()}<#if technology_has_next>, </#if></#list>
</#if>
<#if target.aliases?has_content>
<@formatAliases aliases=target.aliases/>
</#if>
<#---->
<#-- Room -->
<#elseif target.class.simpleName == "Room">
${context.message(indent, "target.technologies")}: <#list target.technologies as technology>${technology.getName()}<#if technology_has_next>, </#if></#list>
<#if target.name??>
${context.message(indent, "target.room.name")}: ${target.name}
</#if>
<#if roomRecorded??>
${context.message(indent, "target.room.recorded")}: ${context.message("general." + roomRecorded)}
</#if>
<#if (target.licenseCount > 0)>
${context.message(indent, "target.room.licenseCount")}: ${target.licenseCount}
</#if>
<#if target.pin??>
${context.message(indent, "target.room.pin")}: ${target.pin}
</#if>
<#if target.aliases?has_content>
<#assign aliasesLabel = context.message("target.room.aliases")>
${context.width(indent + (aliasesLabel?length + 1) / 2 + 1, "----- " + aliasesLabel)} -----
<@formatAliases aliases=target.aliases/>
</#if>
<#elseif target.class.simpleName == "Reused">
${context.message(indent, "target.reused.reservation")}: ${target.reusedReservationId}
</#if>