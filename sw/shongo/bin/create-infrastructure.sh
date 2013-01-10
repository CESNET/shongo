CONTROLLER=127.0.0.1
cd `dirname $0`
./client-cli.sh --connect $CONTROLLER --testing-access-token --scripting \
--cmd "\
    create-resource { \
        class: 'Resource', \
        name: 'gatekeeper', \
        description: 'Alias provider for H.323/SIP MCUs', \
        allocatable: 1, \
        maximumFuture: 'P4M', \
        capabilities: [{ \
            class: 'AliasProviderCapability', \
            aliases: [ \
                { type: 'H323_E164', value: '9500872{value}' }, \
                { type: 'H323_URI', value: '{resource.address}#9500872{value}' }, \
                { type: 'SIP_URI', value: '9500872{value}@cesnet.cz' } \
            ], \
            patterns: ['{digit:2}'], \
        }] \
    }" \
--cmd "\
    create-resource { \
        class: 'DeviceResource', \
        name: 'mcu-cesnet', \
        description: 'H.323/SIP MCU at CESNET', \
        allocatable: 1, \
        maximumFuture: 'P4M', \
        technologies: ['H323','SIP'], \
        address: '195.113.222.60', \
        mode: { \
            connectorAgentName: 'mcu-cesnet' \
        }, \
        capabilities: [{ \
            class: 'RoomProviderCapability', \
            licenseCount: 20 \
        }], \
        administrators: [ \
            { class: 'OtherPerson', name: 'Martin Srom', email: 'srom.martin@gmail.com'}, \
            { class: 'OtherPerson', name: 'Jan Ruzicka', email: 'janru@cesnet.cz'} \
        ] \
    }" \
--cmd "\
    create-resource { \
        class: 'DeviceResource', \
        name: 'mcu-muni', \
        description: 'H.323/SIP MCU at MUNI', \
        allocatable: 1, \
        maximumFuture: 'P4M', \
        technologies: ['H323','SIP'], \
        address: '147.251.15.253', \
        mode: { \
            connectorAgentName: 'mcu-muni' \
        }, \
        capabilities: [{ \
            class: 'RoomProviderCapability', \
            licenseCount: 10 \
        }], \
        administrators: [ \
            { class: 'OtherPerson', name: 'Martin Srom', email: 'srom.martin@gmail.com'}, \
            { class: 'OtherPerson', name: 'Jan Ruzicka', email: 'janru@cesnet.cz'} \
        ] \
    }" \
--cmd "\
    create-resource { \
        class: 'DeviceResource', \
        name: 'connect-cesnet', \
        description: 'Adobe Connect server at CESNET', \
        allocatable: 1, \
        address: 'https://connect.cesnet.cz', \
        technologies: ['ADOBE_CONNECT'], \
        mode: { \
            connectorAgentName: 'connect-cesnet' \
        }, \
        capabilities: [{ \
            class: 'RoomProviderCapability', \
            licenseCount: 10 \
        }, { \
            class: 'AliasProviderCapability', \
            aliases: [ \
                { type: 'ADOBE_CONNECT_NAME', value: '{value}' }, \
                { type: 'ADOBE_CONNECT_URI', value: '{resource.address}/{value}' }, \
            ], \
            patterns: ['{string}'], \
            permanentRoom: 1 \
        }], \
        administrators: [ \
            { class: 'OtherPerson', name: 'Martin Srom', email: 'srom.martin@gmail.com'}, \
            { class: 'OtherPerson', name: 'Jan Ruzicka', email: 'janru@cesnet.cz'} \
        ] \
    }" \
--cmd "\
    create-resource { \
        class: 'DeviceResource', \
        name: 'c90-sitola', \
        description: 'H.323/SIP endpoint in SITOLA at FI MUNI', \
        allocatable: 1, \
        technologies: ['H323'], \
        mode: { \
            connectorAgentName: 'c90-sitola' \
        }, \
        capabilities: [{ \
            class: 'StandaloneTerminalCapability', \
            aliases: [{ \
                type: 'H323_E164', \
                value: '950081038' \
            }] \
        }] \
    }"