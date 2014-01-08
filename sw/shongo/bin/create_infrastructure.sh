cd `dirname $0`
#
# Create testing infrastructure in running Shongo controller.
#
#   "create_infrastructure.sh (localhost)"  Create infrastructure on localhost
#   "create_infrastructure.sh meetings"     Create infrastructure on meetings.cesnet.cz
#   "create_infrastructure.sh shongo-dev"   Create infrastructure on shongo-dev.cesnet.cz
#

MODE=local
if [ "$1" ]
then
    MODE=$1
fi

case $MODE in
    meetings )
        CONTROLLER=meetings.cesnet.cz
        NAME_PREFIX=ZZ-shongo-
        DEVICE_NAME_PREFIX=
        MCU_CESNET_LICENSE_COUNT=15
        MCU_CESNET_NUMBER_PREFIX=950087
        MCU_CESNET_NUMBER_RANGE=200:399
        CONNECT_CESNET=https://connect.cesnet.cz
        CONNECT_CESNET_LICENSE_COUNT=100
        TCS_LICENSE_COUNT=5
        RESOURCE_ADMIN_EMAIL=meetings-announce@cesnet.cz
        ;;
    shongo-dev )
        CONTROLLER=shongo-dev.cesnet.cz
        NAME_PREFIX=shongo-dev-
        DEVICE_NAME_PREFIX=YY-
        MCU_CESNET_LICENSE_COUNT=10
        MCU_CESNET_NUMBER_PREFIX=950087
        MCU_CESNET_NUMBER_RANGE=050:099
        CONNECT_CESNET=https://actest-w3.cesnet.cz
        CONNECT_CESNET_LICENSE_COUNT=20
        TCS_LICENSE_COUNT=3
        RESOURCE_ADMIN_EMAIL=srom.martin@gmail.cz
        ;;
    * )
        CONTROLLER=127.0.0.1
        NAME_PREFIX=shongo-local-
        DEVICE_NAME_PREFIX=YY-
        MCU_CESNET_LICENSE_COUNT=10
        MCU_CESNET_NUMBER_PREFIX=950087
        MCU_CESNET_NUMBER_RANGE=090:099
        CONNECT_CESNET=https://actest-w3.cesnet.cz
        CONNECT_CESNET_LICENSE_COUNT=20
        TCS_LICENSE_COUNT=2
        RESOURCE_ADMIN_EMAIL=srom.martin@gmail.com
        ;;
esac

# Print configuration
echo "Configuration:"
echo "  Controller:     $CONTROLLER"
echo "  MCU CESNET:     $MCU_CESNET_LICENSE_COUNT licenses, $MCU_CESNET_NUMBER_PREFIX$MCU_CESNET_NUMBER_RANGE"
echo "  Adobe Connect:  $CONNECT_CESNET, $CONNECT_CESNET_LICENSE_COUNT licenses"
echo "  TCS:            $TCS_LICENSE_COUNT licenses"
echo "  Resource admin: $RESOURCE_ADMIN_EMAIL"
echo -n "Presse enter to continue..."; read line

RUN_CLIENT_CLI="cat"
RUN_CLIENT_CLI="./client_cli.sh --connect $CONTROLLER --root --scripting"

################################################################################
#
# CREATE RESOURCES
#
$RUN_CLIENT_CLI <<EOF

    \${id} = create-resource {
        class: 'Resource',
        name: 'namingService',
        description: 'Naming service for all technologies',
        allocatable: 1,
        capabilities: [{
            class: 'ValueProviderCapability',
            valueProvider: {
                class: 'ValueProvider.Pattern',
                patterns: ['$NAME_PREFIX{hash}'],
                allowAnyRequestedValue: 1,
            },
        }]
    }

    create-resource {
        class: 'DeviceResource',
        name: 'mcu-cesnet',
        description: 'H.323/SIP MCU at CESNET',
        allocatable: 1,
        maximumFuture: 'P1Y',
        technologies: ['H323','SIP'],
        address: '195.113.222.60',
        mode: {
            connectorAgentName: 'mcu-cesnet'
        },
        capabilities: [{
            class: 'RoomProviderCapability',
            licenseCount: $MCU_CESNET_LICENSE_COUNT,
            requiredAliasTypes: ['ROOM_NAME', 'H323_E164'],
        },{
            class: 'AliasProviderCapability',
            valueProvider: '\${id}',
            aliases: [
                { type: 'ROOM_NAME', value: '$DEVICE_NAME_PREFIX{value}' }
            ],
            maximumFuture: 'P4Y',
            restrictedToResource: 1
        },{
            class: 'AliasProviderCapability',
            valueProvider: {
                class: 'ValueProvider.Pattern',
                patterns: ['{number:$MCU_CESNET_NUMBER_RANGE}'],
            },
            aliases: [
                { type: 'H323_E164', value: '$MCU_CESNET_NUMBER_PREFIX{value}' },
                { type: 'H323_URI', value: '$MCU_CESNET_NUMBER_PREFIX{value}@{device.address}' },
                { type: 'H323_IP', value: '195.113.222.60 {value}#' },
                { type: 'SIP_URI', value: '$MCU_CESNET_NUMBER_PREFIX{value}@cesnet.cz' },
                { type: 'SIP_IP', value: '195.113.222.60 {value}#' }
            ],
            maximumFuture: 'P4Y',
            restrictedToResource: 1
        }],
        administrators: [
            { class: 'AnonymousPerson', name: 'Admins', email: '$RESOURCE_ADMIN_EMAIL'}
        ]
    }

    create-resource {
        class: 'DeviceResource',
        name: 'mcu-muni',
        description: 'H.323/SIP MCU at MUNI',
        allocatable: 0,
        maximumFuture: 'P1Y',
        technologies: ['H323','SIP'],
        address: '147.251.15.253',
        mode: {
            connectorAgentName: 'mcu-muni'
        },
        capabilities: [{
            class: 'RoomProviderCapability',
            licenseCount: 10,
            requiredAliasTypes: ['ROOM_NAME', 'H323_E164'],
        }]
    }

    create-resource {
        class: 'DeviceResource',
        name: 'connect-cesnet',
        description: 'Adobe Connect server at CESNET',
        allocatable: 1,
        maximumFuture: 'P1Y',
        address: '$CONNECT_CESNET',
        technologies: ['ADOBE_CONNECT'],
        mode: {
            connectorAgentName: 'connect-cesnet'
        },
        capabilities: [{
            class: 'RoomProviderCapability',
            licenseCount: $CONNECT_CESNET_LICENSE_COUNT,
            requiredAliasTypes: ['ROOM_NAME'],
        },{
            class: 'RecordingCapability'
        },{
            class: 'AliasProviderCapability',
            valueProvider: {
                class: 'ValueProvider.Filtered',
                filterType: 'CONVERT_TO_URL',
                valueProvider: '\${id}',
            },
            aliases: [
                { type: 'ROOM_NAME', value: '$DEVICE_NAME_PREFIX{requested-value}' },
                { type: 'ADOBE_CONNECT_URI', value: '{device.address}/$DEVICE_NAME_PREFIX{value}' }
            ],
            maximumFuture: 'P4Y',
            restrictedToResource: 1
        }],
        administrators: [
            { class: 'AnonymousPerson', name: 'Admins', email: '$RESOURCE_ADMIN_EMAIL'}
        ]
    }

    create-resource {
        class: 'DeviceResource',
        name: 'c90-sitola',
        description: 'Tandberg endpoint in SITOLA at FI MUNI',
        allocatable: 1,
        technologies: ['H323'],
        mode: {
            connectorAgentName: 'c90-sitola'
        },
        capabilities: [{
            class: 'StandaloneTerminalCapability',
            aliases: [
                { type: 'H323_E164', value: '950081038' }
            ]
        }]
    }

    create-resource {
        class: 'DeviceResource',
        name: 'lifesize-sitola',
        description: 'LifeSize endpoint in SITOLA at FI MUNI',
        allocatable: 1,
        technologies: ['H323'],
        mode: {
            connectorAgentName: 'lifesize-sitola'
        },
        capabilities: [{
            class: 'StandaloneTerminalCapability'
        }]
    }

    create-resource {
        class : "DeviceResource",
        technologies: [
            "SIP",
            "H323"
        ],
        mode: {
            "class" : "ManagedMode",
            "connectorAgentName" : "tcs"
        },
        name: "tcs",
        allocatable: "1",
        capabilities: [{
            class: "RecordingCapability",
            licenseCount: $TCS_LICENSE_COUNT
        }],
        administrators: [
            { class: 'AnonymousPerson', name: 'Admins', email: '$RESOURCE_ADMIN_EMAIL'}
        ]
    }

EOF

################################################################################
#
# CREATE RESERVATIONS
#

# Command for listing not-shongo MCU rooms
<<COMMENT
bin/client_cli.sh --connect meetings.cesnet.cz --root --cmd "control-resource 4 list-rooms" \
    | grep "^|.\+|.\+|" \
    | grep -v "exe:\|-+-\|| Description" \
    | sed -r -e "s/^\| *([^\|]+) *\|.*/\1/g" \
    | sed -r -e "s/[ \t]*$//g" \
    | sort
COMMENT

# Command for listing not-shongo ADOBE CONNECT rooms
<<COMMENT
bin/client_cli.sh --connect meetings.cesnet.cz --root --cmd "control-resource 24 list-rooms" \
    | grep "^|.\+|.\+|" \
    | grep -v "exe:\|-+-\|| Description" \
    | sed -r -e "s/^\|[^\|]+\|[^\|]+\|[^\|]+\| *\/([^\|]+)\/ *\|.*/\1/g" \
    | grep -v "^|" \
    | sed -r -e "s/[ \t]*$//g" \
    | sort
COMMENT

# Pattern for each line from room files
PATTERN="[ \t]*\(.\+\)[ \t]*"

# Value from matched line
REPLACE=$(cat <<EOF
                '\1',
EOF
)

$RUN_CLIENT_CLI <<EOF

    create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '*/*',
        specification: {
            class: 'ValueSpecification',
            resourceId: '1',
            values: [
`cat rooms/mcu-cesnet.txt | sed -e "s/$PATTERN/$REPLACE/g"`
`cat rooms/connect-cesnet.txt | sed "s/$PATTERN/$REPLACE/g"`
            ]
        }
    }

EOF
