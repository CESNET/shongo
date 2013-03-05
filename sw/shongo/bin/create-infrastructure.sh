cd `dirname $0`

MODE=local
if [ "$1" ]
then
    MODE=$1
fi

case $MODE in
    shongo )
        CONTROLLER=195.113.151.172
        NAME_PREFIX=ZZ-shongo-
        DEVICE_NAME_PREFIX=
        MCU_CESNET_LICENSE_COUNT=20
        MCU_CESNET_NAME_PREFIX=
        MCU_CESNET_NUMBER_PREFIX=950087
        MCU_CESNET_NUMBER_RANGE=200:399
        ;;
    shongo-dev )
        CONTROLLER=195.113.151.181
        NAME_PREFIX=shongo-dev-
        DEVICE_NAME_PREFIX=YY-
        MCU_CESNET_LICENSE_COUNT=10
        MCU_CESNET_NUMBER_PREFIX=950087
        MCU_CESNET_NUMBER_RANGE=050:099
        ;;
    * )
        CONTROLLER=127.0.0.1
        NAME_PREFIX=shongo-dev-
        DEVICE_NAME_PREFIX=YY-
        MCU_CESNET_LICENSE_COUNT=10
        MCU_CESNET_NUMBER_PREFIX=950087
        MCU_CESNET_NUMBER_RANGE=090:099
        ;;
esac

# Print configuration
echo "Configuration:"
echo "  controller: $CONTROLLER"
echo "  MCU CESNET: $MCU_CESNET_LICENSE_COUNT licenses, $MCU_CESNET_NUMBER_PREFIX$MCU_CESNET_NUMBER_RANGE"
echo -n "Presse enter to continue..."; read line

./client-cli.sh --connect $CONTROLLER --testing-access-token --scripting <<EOF

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
            restrictedToResource: 1,
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
            restrictedToResource: 1,
        }],
        administrators: [
            { class: 'OtherPerson', name: 'Martin Srom', email: 'srom.martin@gmail.com'},
            { class: 'OtherPerson', name: 'Jan Ruzicka', email: 'janru@cesnet.cz'},
            { class: 'OtherPerson', name: 'Milos Liska', email: 'xliska@fi.muni.cz'}
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
        address: 'https://actest-w3.cesnet.cz',
        technologies: ['ADOBE_CONNECT'],
        mode: {
            connectorAgentName: 'connect-cesnet'
        },
        capabilities: [{
            class: 'RoomProviderCapability',
            licenseCount: 10,
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
            { class: 'OtherPerson', name: 'Martin Srom', email: 'srom.martin@gmail.com'},
            { class: 'OtherPerson', name: 'Jan Ruzicka', email: 'janru@cesnet.cz'},
            { class: 'OtherPerson', name: 'Milos Liska', email: 'xliska@fi.muni.cz'}
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

    create-reservation-request {
        class: 'ReservationRequest',
        purpose: 'OWNER',
        slot: '*/*',
        specification: {
            class: 'AliasSetSpecification',
            aliases: [
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Pokusna' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'MMPS' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'EUAsiaGrid' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'EInfrastruktura' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Predstavenstvo' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'TWAREN' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'eduID' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'PHW 602' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'FEE CTU' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'CC' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'RR' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'UJV REZ' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'DELLIISS' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'MZK' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'CESNET' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'ICASSP' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'CESNET2011' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Fond rozvoje' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'IPv6-wg' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'FEL-Recording' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Mneme' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Gridy' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'BBMRI-CZ' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'PSaC' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Roadmap' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Povros' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'OKaVV' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Datova uloziste' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'OOaP' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Klub reditelu' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Opticke site' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'VSB-IT4I' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Sitova infrastruktura'},
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'CESNET office' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'FROV JCU' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'ELI' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'CEITEC CSR - telekonference' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Podpora_VaVaI' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Multimedia' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Sitove aplikace' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Monitoring a konfigurace' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Sitova identita' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'ServiceDesk' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'IMETE-ICT' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'LiveSurgery2012' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'UVN' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'NEAT-FT' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'shongo-test' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'PSaC_Akce' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'ICRC' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'pokustemppstn' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'VSB-CIT' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'Skoda Power' },
                { technologies: ['H323'], aliasTypes: ['ROOM_NAME'], value: 'UPa-IC' },
            ]
        }
    }

EOF