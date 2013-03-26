cd `dirname $0`

MODE=local
if [ "$1" ]
then
    MODE=$1
fi

case $MODE in
    shongo )
        CONTROLLER=195.113.151.174
        NAME_PREFIX=ZZ-shongo-
        DEVICE_NAME_PREFIX=
        MCU_CESNET_LICENSE_COUNT=15
        MCU_CESNET_NAME_PREFIX=
        MCU_CESNET_NUMBER_PREFIX=950087
        MCU_CESNET_NUMBER_RANGE=200:399
        CONNECT_CESNET=https://connect.cesnet.cz
        CONNECT_CESNET_LICENSE_COUNT=20
        ;;
    shongo-dev )
        CONTROLLER=195.113.151.181
        NAME_PREFIX=shongo-dev-
        DEVICE_NAME_PREFIX=YY-
        MCU_CESNET_LICENSE_COUNT=10
        MCU_CESNET_NUMBER_PREFIX=950087
        MCU_CESNET_NUMBER_RANGE=050:099
        CONNECT_CESNET=https://actest-w3.cesnet.cz
        CONNECT_CESNET_LICENSE_COUNT=20
        ;;
    * )
        CONTROLLER=127.0.0.1
        NAME_PREFIX=shongo-dev-
        DEVICE_NAME_PREFIX=YY-
        MCU_CESNET_LICENSE_COUNT=10
        MCU_CESNET_NUMBER_PREFIX=950087
        MCU_CESNET_NUMBER_RANGE=090:099
        CONNECT_CESNET=https://actest-w3.cesnet.cz
        CONNECT_CESNET_LICENSE_COUNT=20
        ;;
esac

# Print configuration
echo "Configuration:"
echo "  controller: $CONTROLLER"
echo "  MCU CESNET: $MCU_CESNET_LICENSE_COUNT licenses, $MCU_CESNET_NUMBER_PREFIX$MCU_CESNET_NUMBER_RANGE"
echo "  Connect:    $CONNECT_CESNET, $CONNECT_CESNET_LICENSE_COUNT licenses"
echo -n "Presse enter to continue..."; read line

RUN_CLIENT_CLI="cat"
RUN_CLIENT_CLI="./client-cli.sh --connect $CONTROLLER --testing-access-token --scripting"

################################################################################
#
# CREATE RESOURCES
#
$RUN_CLIENT_CLI <<EOF

    create-resource {
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
        maximumFuture: 'P4M',
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
            valueProvider: '1',
            aliases: [
                { type: 'ROOM_NAME', value: '$DEVICE_NAME_PREFIX{value}' }
            ],
            maximumFuture: 'P1Y',
            permanentRoom: 1,
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
                { type: 'SIP_IP', value: '195.113.222.60 {value}#' },
                { type: 'SIP_URI', value: '$MCU_CESNET_NUMBER_PREFIX{value}@cesnet.cz' }
            ],
            maximumFuture: 'P1Y',
            permanentRoom: 1,
        }],
        administrators: [
            { class: 'OtherPerson', name: 'Admins', email: 'vidcon@cesnet.cz'}
        ]
    }

    create-resource {
        class: 'DeviceResource',
        name: 'mcu-muni',
        description: 'H.323/SIP MCU at MUNI',
        allocatable: 1,
        maximumFuture: 'P4M',
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
        maximumFuture: 'P4M',
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
            class: 'AliasProviderCapability',
            valueProvider: {
                class: 'ValueProvider.Filtered',
                type: 'CONVERT_TO_URL',
                valueProvider: '1',
            },
            aliases: [
                { type: 'ROOM_NAME', value: '$DEVICE_NAME_PREFIX{requested-value}' },
                { type: 'ADOBE_CONNECT_URI', value: '{device.address}/$DEVICE_NAME_PREFIX{value}' }
            ],
            maximumFuture: 'P1Y',
            permanentRoom: 1,
        }],
        administrators: [
            { class: 'OtherPerson', name: 'Admins', email: 'vidcon@cesnet.cz'}
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
            aliases: [{
                type: 'H323_E164',
                value: '950081038'
            }]
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

EOF

################################################################################
#
# CREATE RESERVATIONS
#

# Pattern for each line from room files
PATTERN="[ \t]*\(.\+\)[ \t]*"

# Alias specification from matched line
REPLACE=$(cat <<EOF
                { technologies: ['<TECHNOLOGY>'], aliasTypes: ['ROOM_NAME'], value: '\1' },
EOF
)

$RUN_CLIENT_CLI <<EOF

    create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '*/*',
        specification: {
            class: 'AliasSetSpecification',
            aliases: [
`cat rooms/mcu-cesnet.txt | sed -e "s/$PATTERN/$REPLACE/g" | sed "s/<TECHNOLOGY>/H323/g"`
`cat rooms/connect-cesnet.txt | sed "s/$PATTERN/$REPLACE/g" | sed "s/<TECHNOLOGY>/ADOBE_CONNECT/g"`
            ]
        }
    }

EOF
