<#--
  -- Template for rendering standalone {@link ReservationNotification}
  -->
<#assign indent = 24>
<#---->
<#if context.administrator>
${context.message(indent, "reservation.id")}: ${notification.id}
<#if target.resourceName?? && target.resourceId??>
${context.message(indent, "target.resourceId")}: ${target.resourceName} (${target.resourceId})
</#if>
</#if>
<#---->
${context.message(indent, "target.type")}: ${context.message("target.type." + target.type)}
<#-- Value -->
<#if target.class.simpleName == "Value">
<#---->
<#-- Alias -->
<#elseif target.class.simpleName == "Alias">
<#if target.technologies?has_content>
${context.message(indent, "target.technologies")}: <#list target.technologies as technology>${technology.getName()}<#if technology_has_next>, </#if></#list>
</#if>
<#list target.aliases?sort_by(['type']) as alias>
<@formatAlias alias=alias/><#t>
</#list>
<#---->
<#-- Room -->
<#elseif target.class.simpleName == "Room">
${context.message(indent, "target.technologies")}: <#list target.technologies as technology>${technology.getName()}<#if technology_has_next>, </#if></#list>
${context.message(indent, "target.room.licenseCount")}: ${target.licenseCount}
<#if context.administrator>
${context.message(indent, "target.room.availableLicenseCount")}: ${target.availableLicenseCount}
</#if>
</#if>
<#---->
${context.message(indent, "reservation.slot")}: ${context.formatInterval(notification.slot)}
<#if context.timeZone != "UTC">
${context.width(indent)}  ${context.formatInterval(notification.slot, "UTC")}
</#if>
${context.message(indent, "reservation.owners")}: <#list notification.owners as owner>${context.formatUser(owner)}<#if owner_has_next>, </#if></#list>
<#if notification.reservationRequestUrl??>
${context.message(indent, "reservationRequest.url")}: ${notification.reservationRequestUrl}
<#elseif notification.reservationRequestId??>
${context.message(indent, "reservationRequest.id")}: ${notification.reservationRequestId}
</#if>
<#if notification.reservationRequestUpdatedAt??>
${context.message(indent, "reservationRequest.updatedAt")}: ${context.formatDateTime(notification.reservationRequestUpdatedAt)}
${context.message(indent, "reservationRequest.updatedBy")}: ${context.formatUser(notification.reservationRequestUpdatedBy)}
</#if>
<#if notification.reservationRequestDescription??>
${context.message(indent, "reservationRequest.description")}: ${notification.reservationRequestDescription}
</#if>
<#---->
<#-------------------------------->
<#-- Macro for formatting alias -->
<#--                            -->
<#-- @param alias               -->
<#-------------------------------->
<#macro formatAlias alias>
<#if alias.type == "ROOM_NAME">
${context.message(indent, "alias.ROOM_NAME")}: ${alias.value}
<#elseif alias.type == "H323_E164">
${context.message(indent, "alias.H323_E164")}: ${alias.value}
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