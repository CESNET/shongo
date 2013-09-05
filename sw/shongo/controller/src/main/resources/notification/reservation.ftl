<#--
  -- Template for rendering standalone {@link ReservationNotification}
  -->
<#assign indent = 23>
<#---->
<#if context.administrator>
${context.message(indent, "reservation.id")}: ${notification.id}
${context.message(indent, "target.resourceId")}: ${target.resourceName} (${target.resourceId})
</#if>
<#---->
<#-- Value -->
<#if target.class.simpleName == "Value">
${context.message(indent, "target.type")}: ${context.message("target.type.value")}
<#---->
<#-- Alias -->
<#elseif target.class.simpleName == "Alias">
${context.message(indent, "target.type")}: ${context.message("target.type.alias")}
<#---->
<#-- Room -->
<#elseif target.class.simpleName == "Room">
${context.message(indent, "target.type")}: ${context.message("target.type.room")}
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
${context.message(indent, "reservation.owners")}: <#list notification.owners as owner>${context.formatUser(owner)}</#list>
${context.message(indent, "reservationRequest.url")}: ${notification.reservationRequestUrl}
${context.message(indent, "reservationRequest.updatedAt")}: ${context.formatDateTime(notification.reservationRequestUpdatedAt)}
${context.message(indent, "reservationRequest.updatedBy")}: ${context.formatUser(notification.reservationRequestUpdatedBy)}
${context.message(indent, "reservationRequest.description")}: ${notification.reservationRequestDescription}