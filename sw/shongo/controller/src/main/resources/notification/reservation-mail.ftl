${type.getName()} reservation by
  ${template.formatUser(reservation.userId)}

<#----------------------------------------------------------------------------->
<#-- Reservation name                                                        -->
<#----------------------------------------------------------------------------->
<#if reservation.class.simpleName == "RoomReservation">
ROOM RESERVATION:
-----------------
<#elseif aliasReservations?has_content>
ALIAS RESERVATION:
------------------
<#else>
RESERVATION:
------------
</#if>
<#----------------------------------------------------------------------------->
<#-- Common reservation and request attributes                               -->
<#----------------------------------------------------------------------------->
<#if reservationRequest??>

  Requested at:     ${template.formatDateTime(reservationRequest.created)}
</#if>

  Start date/time:  ${template.formatDateTime(reservation.slot)} (local time)
                    ${template.formatDateTimeUTC(reservation.slot)} (UTC)

  Duration:         ${template.formatDuration(reservation.slot)}
<#if reservationRequest??>

  Description:      ${reservationRequest.description}
</#if>
<#----------------------------------------------------------------------------->
<#-- Room reservation attributes                                             -->
<#----------------------------------------------------------------------------->
<#if reservation.class.simpleName == "RoomReservation">
    <#if reservationRequest?? && reservationRequest.specification.class.simpleName == "RoomSpecification">

  Conference type:  <#rt>
        <#list reservationRequest.specification.technologies as technology>
            ${technology.getName()}<#if technology_has_next>, </#if><#t>
        </#list> <#lt>

  Participants:     ${reservationRequest.specification.participantCount}
    </#if>
    <#if reservation.executable.aliases?has_content>
        <#-- Find room name -->
        <#list reservation.executable.aliases as alias>
            <#if alias.type == "ROOM_NAME">
                <#assign roomName = alias.value>
            </#if>
        </#list>
        <#-- Print room name if it was found -->
        <#if roomName??>

  Room name:        ${roomName}
        </#if>

  How to reach:
        <#list reservation.executable.aliases as alias>
            <@formatAlias alias=alias/>
        </#list>
    </#if>
</#if>
<#----------------------------------------------------------------------------->
<#-- Alias reservation attributes                                            -->
<#----------------------------------------------------------------------------->
<#if aliasReservations?has_content>
    <#-- Find room name -->
    <#list aliasReservations as aliasReservation>
        <#list aliasReservation.aliases as alias>
            <#if alias.type == "ROOM_NAME">
                <#assign roomName = alias.value>
            </#if>
        </#list>
    </#list>
    <#-- Print room name if it was found -->
    <#if roomName??>

  Room name:        ${roomName}
    </#if>

  Aliases:
    <#list aliasReservations as aliasReservation>
        <#list aliasReservation.aliases as alias>
            <@formatAlias alias=alias/>
        </#list>
    </#list>
</#if>


<#----------------------------------------------------------------------------->
<#-- Details for administrators                                              -->
<#----------------------------------------------------------------------------->
DETAILS (for administrators):
-----------------------------
<#if reservationRequest??>
  Request-id:         ${reservationRequest.id}
</#if>
  Reservation-id:     ${reservation.id}
<#if reservation.class.simpleName == "RoomReservation">
  Room Provider:      ${reservation.resourceName} (id: ${reservation.resourceId}, licenses: ${reservation.licenseCount})
<#elseif aliasReservations?has_content>
  Alias Provider(s):  <#rt>
        <#list aliasReservations as aliasReservation>
            ${aliasReservation.resourceName} (id: ${aliasReservation.resourceId}, value: ${aliasReservation.valueReservation.value})<#lt>
                      ${""}<#rt>
        </#list>
</#if>
<#----------------------------------------------------------------------------->
<#-- Macro for formatting alias                                              -->
<#--                                                                         -->
<#-- @param alias                                                            -->
<#----------------------------------------------------------------------------->
<#macro formatAlias alias>
    <#if alias.type == "H323_E164">

   * H.323 GDS number: 00420${alias.value}

   * PSTN dial in: +420${alias.value}
    <#elseif alias.type == "H323_URI">

   * H323 IP: ${alias.value}
    <#elseif alias.type == "SIP_URI">

   * SIP: sip:${alias.value}
    <#elseif alias.type == "ADOBE_CONNECT_URI">

   * URL: ${alias.value}
    </#if>
</#macro>
