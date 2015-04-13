<#--
  -- Template for rendering standalone {@link ReservationNotification}
  -->
<#assign indent = 24>
<#---->
<#if context.administrator>
${context.message(indent, "reservation.id")}: ${notification.id}
</#if>
<#if context.administrator && target.executableId??>
${context.message(indent, "target.executable")}: ${target.executableId}
</#if>
<@formatTarget target=target/><#t>
<#---->
${context.message(indent, "reservation.slot")}: ${context.formatInterval(notification.slot)}
<#if context.timeZone != "UTC">
${context.width(indent)}  ${context.formatInterval(notification.slot, "UTC")}
</#if>
<#-- Temporarily until meeting rooms have timezone -->
<#if context.timeZone != "Europe/Prague">
${context.width(indent)}  ${context.formatInterval(notification.slot, "Europe/Prague")}
</#if>
${context.message(indent, "reservation.owners")}: <#list notification.owners as owner>${context.formatUser(owner)}<#if owner_has_next>, </#if></#list>
<#if context.reservationRequestUrl?? && notification.reservationRequestId??>
${context.message(indent, "reservationRequest.url")}: ${context.formatReservationRequestUrl(notification.reservationRequestId)}
<#elseif notification.reservationRequestId??>
${context.message(indent, "reservationRequest.id")}: ${notification.reservationRequestId}
</#if>
<#if notification.reservationRequestUpdatedAt??>
${context.message(indent, "reservationRequest.updatedAt")}: ${context.formatDateTime(notification.reservationRequestUpdatedAt)}
${context.message(indent, "reservationRequest.updatedBy")}: ${context.formatUser(notification.reservationRequestUpdatedBy)}
</#if>
<#if notification.reservationRequestDescription??>
${context.message(indent, "reservationRequest.description")}: ${context.indentNextLines(indent + 2, notification.reservationRequestDescription)}
</#if>
<#if context.administrator>
<#list notification.childTargetByReservation?keys as childReservation>

${context.message(indent, "reservation.id")}: ${childReservation}
<@formatTarget target=notification.childTargetByReservation[childReservation]/><#t>
</#list>
</#if>
<#---->
<#---------------------->
<#-- Macro for target -->
<#--                  -->
<#-- @param target    -->
<#---------------------->
<#macro formatTarget target>
<#if context.administrator && target.resourceName?? && target.resourceId??>
${context.message(indent, "target.resourceId")}: ${target.resourceName} (${target.resourceId})
</#if>
<#---->
${context.message(indent, "target.type")}: ${context.message("target.type." + target.type)}
<#-- Value -->
<#if target.class.simpleName == "Value">
<#if target.values?has_content>
${context.message(indent, "target.value.values")}: <#list target.values as value>${value}<#if value_has_next>, </#if></#list>
</#if>
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
<#if target.name??>
${context.message(indent, "target.room.name")}: ${target.name}
</#if>
<#if (target.licenseCount > 0)>
${context.message(indent, "target.room.licenseCount")}: ${target.licenseCount}
<#if context.administrator>
${context.message(indent, "target.room.availableLicenseCount")}: ${target.availableLicenseCount}
</#if>
</#if>
<#elseif target.class.simpleName == "Reused">
${context.message(indent, "target.reused.reservation")}: ${target.reusedReservationId}
<#elseif target.type == "recordingservice">
<#if context.administrator>
${context.message(indent, "target.room.availableLicenseCount")}: ${target.availableLicenseCount}
</#if>
</#if>
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
<#elseif alias.type == "CS_DIAL_STRING">
${context.message(indent, "alias.CS_DIAL_STRING")}: ${alias.value}
</#if>
</#macro>
