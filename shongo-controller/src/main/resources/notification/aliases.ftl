<#---------------------------------->
<#-- Macro for formatting aliases -->
<#--                              -->
<#-- @param aliases               -->
<#---------------------------------->
<#macro formatAliases aliases>
<#list aliases as alias>
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
${context.message(indent, "alias.ROOM_NAME")}: ${alias.value!aliasValueAny}
<#elseif alias.type == "H323_E164">
${context.message(indent, "alias.H323_GDS")}: ${('00420' + alias.value)!aliasValueAny}
${context.message(indent, "alias.H323_PSTN")}: ${('+420' + alias.value)!aliasValueAny}
<#elseif alias.type == "H323_URI">
${context.message(indent, "alias.H323_URI")}: ${alias.value!aliasValueAny}
<#elseif alias.type == "H323_IP">
${context.message(indent, "alias.H323_IP")}: ${alias.value!aliasValueAny}
<#elseif alias.type == "SIP_URI">
${context.message(indent, "alias.SIP_URI")}: ${('sip:' + alias.value)!aliasValueAny}
<#elseif alias.type == "SIP_IP">
${context.message(indent, "alias.SIP_IP")}: ${alias.value!aliasValueAny}
<#elseif alias.type == "ADOBE_CONNECT_URI">
${context.message(indent, "alias.ADOBE_CONNECT_URI")}: ${alias.value!aliasValueAny}
</#if>
</#macro>