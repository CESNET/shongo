#
# Initial data for meetings.cesnet.cz on 2013-10-18.
#
#!/bin/bash

cd `dirname $0`

CONTROLLER=meetings.cesnet.cz
RUN_CLIENT_CLI="./../../bin/client_cli.sh --connect $CONTROLLER --root --scripting"

$RUN_CLIENT_CLI <<EOF

\${id} = create-reservation-request {
   "purpose" : "SCIENCE",
   "description" : "Místnost CEITEC CŘS",
   "created" : "2013-04-25T10:39:28.923Z",
   "specification" : {
      "permanentRoom" : "1",
      "aliasTypes" : [
         "ROOM_NAME"
      ],
      "technologies" : [
         "ADOBE_CONNECT"
      ],
      "value" : "mu-ceiteccrs",
      "class" : "AliasSpecification"
   },
   "class" : "ReservationRequest",
   "reusement" : "OWNED",
   "slot" : "2013-04-14T22:00:00.000Z/2014-04-14T21:59:59.000Z"
}
# Roman Badík [OWNER]
create-acl 68 \${id} OWNER
# Břetislav Regner [READER]
create-acl 56 \${id} READER

\${id} = create-reservation-request {
   "userId" : "56",
   "purpose" : "EDUCATION",
   "description" : "Obecná místnost pro záznam seminářů",
   "created" : "2013-07-04T07:20:59.125Z",
   "specification" : {
      "permanentRoom" : "1",
      "aliasTypes" : [
         "ROOM_NAME"
      ],
      "technologies" : [
         "ADOBE_CONNECT"
      ],
      "value" : "mu-uvt-seminare",
      "class" : "AliasSpecification"
   },
   "class" : "ReservationRequest",
   "reusement" : "OWNED",
   "slot" : "2013-07-03T22:00:00.000Z/2014-07-03T21:59:59.000Z"
}
# Břetislav Regner [OWNER]
create-acl 56 \${id} OWNER

\${id} = create-reservation-request {
   "userId" : "54",
   "purpose" : "SCIENCE",
   "description" : "elixir",
   "created" : "2013-09-03T05:54:57.973Z",
   "specification" : {
      "sharedExecutable" : "1",
      "aliasSpecifications" : [
         {
            "permanentRoom" : "1",
            "aliasTypes" : [
               "ROOM_NAME"
            ],
            "technologies" : [
               "H323",
               "SIP"
            ],
            "value" : "elixir_cz",
            "class" : "AliasSpecification"
         },
         {
            "permanentRoom" : "1",
            "aliasTypes" : [
               "H323_E164"
            ],
            "class" : "AliasSpecification"
         }
      ],
      "class" : "AliasSetSpecification"
   },
   "class" : "ReservationRequest",
   "reusement" : "OWNED",
   "slot" : "2013-09-02T22:00:00.000Z/2013-12-31T21:59:59.000Z"
}
# Jan Růžička [OWNER]
create-acl 54 \${id} OWNER
# Helmut Sverenyák [OWNER]
create-acl 66 \${id} OWNER

\${it4iId} = create-reservation-request {
   "userId" : "65",
   "purpose" : "SCIENCE",
   "description" : "IT4I",
   "created" : "2013-10-16T06:26:48.611Z",
   "specification" : {
      "permanentRoom" : "1",
      "aliasTypes" : [
         "ROOM_NAME"
      ],
      "technologies" : [
         "ADOBE_CONNECT"
      ],
      "value" : "IT4I",
      "class" : "AliasSpecification"
   },
   "class" : "ReservationRequest",
   "reusement" : "OWNED",
   "slot" : "2013-10-15T22:00:00.000Z/2013-10-30T21:59:59.000Z"
}
# Roman Sliva [OWNER]
create-acl 65 \${it4iId} OWNER
# Jiri Stursa [OWNER]
create-acl 67 \${it4iId} OWNER

\${id} = create-reservation-request {
   "userId" : "65",
   "reusedReservationRequestId" : "\${it4iId}",
   "purpose" : "SCIENCE",
   "description" : "IT4I VR",
   "created" : "2013-10-16T06:30:00.597Z",
   "specification" : {
      "participantCount" : "6",
      "technologies" : [
         "ADOBE_CONNECT"
      ],
      "class" : "RoomSpecification"
   },
   "class" : "ReservationRequest",
   "slot" : "2013-10-23T06:30:00.000Z/2013-10-23T10:30:00.000Z"
}

\${id} = create-reservation-request {
   "reusedReservationRequestId" : "\${it4iId}",
   "purpose" : "SCIENCE",
   "description" : "Test VR IT4I",
   "created" : "2013-10-16T15:30:43.751Z",
   "specification" : {
      "participantCount" : "6",
      "technologies" : [
         "ADOBE_CONNECT"
      ],
      "class" : "RoomSpecification"
   },
   "class" : "ReservationRequest",
   "slot" : "2013-10-21T11:40:00.000Z/2013-10-21T13:00:00.000Z"
}

EOF