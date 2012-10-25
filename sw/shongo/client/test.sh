./client.sh --connect localhost --testing-access-token --cmd "create-resource { \
  class: 'DeviceResource', \
  name: 'ahoj', \
  allocatable: 1, \
  technologies: ['H323', 'SIP'], \
  capabilities: [ \
    {class: 'StandaloneTerminalCapability'} \
  ] \
}"