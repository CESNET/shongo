<#--
  -- Template for rendering {@link Target}
  -->
<#-- Value -->
${context.message(indent, "target.type")}: ${context.message("target.type." + target.type)}
<#if target.class.simpleName == "Value">
<#---->
<#-- Alias -->
<#elseif target.class.simpleName == "Alias">
<#if target.aliases?has_content>
<@formatAliases aliases=target.aliases/>
</#if>
<#---->
<#-- Room -->
<#elseif target.class.simpleName == "Room">
${context.message(indent, "target.room.licenseCount")}: ${target.licenseCount}
<#if target.name??>
${context.message(indent, "target.room.name")}: ${target.name}
</#if>
<#if target.aliases?has_content>
${context.message(indent, "target.room.aliases")}:
<@formatAliases aliases=target.aliases/>
</#if>
</#if>
<#---->
<#---------------------------------->
<#-- Macro for formatting aliases -->
<#--                              -->
<#-- @param aliases               -->
<#---------------------------------->
<#macro formatAliases aliases>
<#list aliases?sort_by(['type']) as alias>
<@formatAlias alias=alias/><#t>
</#list>
</#macro>
<#-------------------------------->
<#-- Macro for formatting alias -->
<#--                            -->
<#-- @param alias               -->
<#-------------------------------->
<#macro formatAlias alias>
<#if alias.type == "ROOM_NAME">
${context.message(indent, "alias.ROOM_NAME")}: ${alias.value}
<#elseif alias.type == "H323_E164">
${context.message(indent, "alias.H323_GDS")}: 00420${alias.value}
${context.message(indent, "alias.H323_PSTN")}: +420${alias.value}
<#elseif alias.type == "H323_URI">
${context.message(indent, "alias.H323_URI")}: ${alias.value}
<#elseif alias.type == "H323_IP">
${context.message(indent, "alias.H323_IP")}: ${alias.value}
<#elseif alias.type == "SIP_URI">
${context.message(indent, "alias.SIP_URI")}: sip:${alias.value}
<#elseif alias.type == "SIP_IP">
${context.message(indent, "alias.SIP_IP")}: ${alias.value}
<#elseif alias.type == "ADOBE_CONNECT_URI">
${context.message(indent, "alias.ADOBE_CONNECT_URI")}: ${alias.value}
</#if>
</#macro>