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
${context.message(indent, "alias.H323_SIP_PHONE")}: ${('(00420)' + alias.value)!aliasValueAny}
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
<#elseif alias.type == "CS_DIAL_STRING">
${context.message(indent, "alias.CS_DIAL_STRING")}: ${alias.value}
<#elseif alias.type == "WEB_CLIENT_URI">
${context.message(indent, "alias.WEB_CLIENT_URI")}: ${alias.value!aliasValueAny}
<#elseif alias.type == "SKYPE_URI">
<#if !alias.value?contains("@cesnet.cz")>
${context.message(indent, "alias.SKYPE_URI")}: ${alias.value!aliasValueAny}
</#if>
<#elseif alias.type == "FREEPBX_CONFERENCE_NUMBER">
<#if alias.value?length == 9>
    <#assign str="${'+420 ' + alias.value[0..2] + ' ' + alias.value[3..4] + ' ' + alias.value[5..8]}">
<#else>
    <#assign str="${'+420 ' + alias.value}">
</#if>
${context.message(indent, "alias.FREEPBX_CONFERENCE_NUMBER")}: ${str}


</#if>
</#macro>
