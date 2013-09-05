<#--
  -- Template for rendering {@link Target}
  -->
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
</#if>