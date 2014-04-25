#!/usr/bin/bash

CONTROLLER=127.0.0.1:8686
NAME_PREFIX=shongo-dev-
DEVICE_NAME_PREFIX=YY-
MCU_CESNET_LICENSE_COUNT=10
MCU_CESNET_NUMBER_PREFIX=950087
MCU_CESNET_NUMBER_RANGE=090:099

shongo-deployment/bin/shongo-client-cli.sh --connect $CONTROLLER --scripting <<EOF

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
            { class: 'AnonymousPerson', name: 'Martin Srom', email: 'srom.martin@gmail.com'},
            { class: 'AnonymousPerson', name: 'Jan Ruzicka', email: 'janru@cesnet.cz'},
            { class: 'AnonymousPerson', name: 'Milos Liska', email: 'xliska@fi.muni.cz'}
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
        address: 'https://tconn.cesnet.cz',
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
                filterType: 'CONVERT_TO_URL',
                valueProvider: '1',
            },
            aliases: [
                { type: 'ROOM_NAME', value: '$DEVICE_NAME_PREFIX{requested-value}' },
                { type: 'ADOBE_CONNECT_URI', value: '{device.address}/$DEVICE_NAME_PREFIX{value}' }
            ],
            maximumFuture: 'P1Y',
        }],
        administrators: [
            { class: 'AnonymousPerson', name: 'Martin Srom', email: 'srom.martin@gmail.com'},
            { class: 'AnonymousPerson', name: 'Jan Ruzicka', email: 'janru@cesnet.cz'},
            { class: 'AnonymousPerson', name: 'Milos Liska', email: 'xliska@fi.muni.cz'}
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

shongo-deployment/bin/shongo-client-cli.sh --connect $CONTROLLER \
--cmd "list-resources" \
--cmd "get-resource 1" \
--cmd "get-resource 2" \
--cmd "get-resource 3" \
--cmd "get-resource 4" \
--cmd "get-resource 5" \
--cmd "get-resource-allocation 2" \
--cmd "get-resource-allocation 5" \
