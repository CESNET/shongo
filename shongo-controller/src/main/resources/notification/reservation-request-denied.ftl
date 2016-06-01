<#--
  -- Template for rendering {@link ReservationRequestDenied}
  -->
${context.message("reservationRequestDenied.message")}

<#assign indent = 23>
<#--<#if context.reservationRequestConfirmationUrl??>-->
<#--${context.message(indent, "reservationRequest.id")}: ${target.resourceId}</a>-->
<#if notification.reservationRequestId??>
${context.message(indent, "reservationRequest.id")}: ${notification.reservationRequestId}
</#if>
<#if notification.reservationRequestUpdatedAt??>
${context.message(indent, "reservationRequest.updatedAt")}: ${context.formatDateTime(notification.reservationRequestUpdatedAt)}
${context.message(indent, "reservationRequest.updatedBy")}: ${context.formatUser(notification.reservationRequestUpdatedBy)}
</#if>
<#if notification.reservationRequestDescription??>
${context.message(indent, "reservationRequest.description")}: ${context.indentNextLines(indent + 2, notification.reservationRequestDescription)}
</#if>
${context.message(indent, 'reservationRequest.requestedSlot')}: ${context.formatInterval(notification.requestedSlot)}
<#if context.timeZone != "UTC">
${context.width(indent)}  ${context.formatInterval(notification.requestedSlot, "UTC")}
</#if>
<#include "target.ftl">
<#--<#if adminReport??>-->
<#--${context.message(indent, 'allocationFailed.userError')}: ${context.indentNextLines(indent + 2, userError)}-->
<#--${context.message(indent, 'allocationFailed.reason')}: ${context.indentNextLines(indent + 2, adminReport)}-->
<#--<#else>-->
${context.message(indent, 'allocationFailed.reason')}: ${context.indentNextLines(indent + 2, userError)}
<#--<#if (errorList??) >-->
<#--<#list errorList as error>-->
<#--${context.width(indent)}  ${error}-->
<#--</#list>-->
<#--</#if>-->
<#--</#if>-->