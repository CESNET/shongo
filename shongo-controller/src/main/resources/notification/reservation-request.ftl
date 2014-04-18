<#--
  -- Template for rendering {@link ReservationRequestNotification}
  -->
<#assign indent = 24>
<#---->
<#if context.reservationRequestUrl??>
${context.message(indent, 'reservationRequest.url')}: ${context.formatReservationRequestUrl(notification.reservationRequestId)}
<#else>
${context.message(indent, 'reservationRequest.id')}: ${notification.reservationRequestId}
</#if>
${context.message(indent, 'reservationRequest.updatedAt')}: ${context.formatDateTime(notification.reservationRequestUpdatedAt)}
<#if context.timeZoneDefault && context.userSettingsUrl??>
${context.width(indent)}  (${context.message('reservationRequest.configureTimeZone', context.userSettingsUrl)})
</#if>
${context.message(indent, 'reservationRequest.updatedBy')}: ${context.formatUser(notification.reservationRequestUpdatedBy)}
<#if notification.reservationRequestDescription??>
${context.message(indent, 'reservationRequest.description')}: ${context.indentNextLines(indent + 2, notification.reservationRequestDescription)}
</#if>