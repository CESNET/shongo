# This folder contains designs for shongo-client-web

Each design should be stored in own folder with the design name.

## Requirements

Each folder must contain:

* <code>/layout.html</code> - design HTML layout file

* <code>/css/style.css</code> - design CSS file

Each folder can contain:

* <code>/img/icon.ico</code> - icon for web browsers (@see favicon)
* <code>/img/apple-touch-icon.png</code> - icon for apple touch (@see apple-touch-icon)

In HTML layout file you may use the following syntax:

Variables:


<pre>

head
title
content

app.version

url.resources
url.home
url.changelog
url.reservationRequest
url.roomList
url.report
url.language.cs
url.language.en
url.user.login
url.user.logout
url.user.settings
url.user.settingsAdvancedMode
url.user.settingsAdministratorMode

links[reservationRequest|roomList|user.settings|help]
links[n].title
links[n].url

timezone.title
timezone.help

session.locale.title
session.locale.language
session.timezone.title
session.timezone.help

user.id
user.name
user.advancedMode
user.administratorMode
user.administratorModeAvailable

breadcrumbs[n]
breadcrumbs[n].title
breadcrumbs[n].url

</pre>

Messages:

<pre>
app.name
menu
report
user.login
user.logout
user.administrator
user.settings
user.settingsAdvancedMode
user.settingsAdministratorMode
</pre>



## Example

<pre>
cesnet/
cesnet/layout.html
cesnet/css/style.css
cesnet/img/favicon.ico
cesnet/img/favicon.png
cesnet/js/*.js
</pre>