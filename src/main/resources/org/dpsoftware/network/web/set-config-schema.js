// Declarative schema of the settings page: the sections and their fields (id, type, numeric constraints) used to build the form. Values and
// labels are merged at runtime from the server-provided config/field options.
export const sections = [
    {
        id: 'leds', fields: [
            {id: 'topLed', type: 'number', numeric: true, digitsOnly: true, min: 0},
            {id: 'leftLed', type: 'number', numeric: true, digitsOnly: true, min: 0},
            {id: 'rightLed', type: 'number', numeric: true, digitsOnly: true, min: 0},
            {id: 'bottomLeftLed', type: 'number', numeric: true, digitsOnly: true, min: 0},
            {id: 'bottomRightLed', type: 'number', numeric: true, digitsOnly: true, min: 0},
            {id: 'bottomRowLed', type: 'number', numeric: true, digitsOnly: true, min: 0},
            {id: 'ledStartOffset', type: 'number', numeric: true, min: 0},
            {id: 'orientation', type: 'select'},
            {id: 'groupBy', type: 'select', numeric: true},
            {
                id: 'grabberAreaTopBottom',
                type: 'select',
                options: Array.from({length: 41}, (_, i) => i + '%'),
                direction: 'vertical'
            },
            {
                id: 'grabberSide',
                type: 'select',
                options: Array.from({length: 41}, (_, i) => i + '%'),
                direction: 'horizontal'
            },
            {
                id: 'gapTypeTopBottom',
                type: 'select',
                options: Array.from({length: 41}, (_, i) => i + '%'),
                direction: 'vertical'
            },
            {
                id: 'gapTypeSide',
                type: 'select',
                options: Array.from({length: 41}, (_, i) => i + '%'),
                direction: 'horizontal'
            },
            {id: 'splitBottomMargin', type: 'select', options: Array.from({length: 96}, (_, i) => i + '%')}
        ]
    },
    {
        id: 'mode', fields: [
            {id: 'screenResX', type: 'number', numeric: true, digitsOnly: true, min: 0},
            {id: 'screenResY', type: 'number', numeric: true, digitsOnly: true, min: 0},
            {id: 'monitorNumber', type: 'select'},
            {id: 'osScaling', type: 'select', numeric: true},
            {id: 'cubeLut', type: 'select'},
            {id: 'resamplingFactor', type: 'select', numeric: true},
            {id: 'defaultLedMatrix', type: 'select'},
            {id: 'language', type: 'select'},
            {id: 'captureMethod', type: 'select'},
            {id: 'algo', type: 'select'},
            {id: 'simdAvx', type: 'select', numeric: true},
            {id: 'syncCheck', type: 'checkbox', numeric: false},
            {id: 'checkForUpdates', type: 'checkbox', numeric: false},
            {id: 'startWithSystem', type: 'checkbox', numeric: false},
            {id: 'webMcpServerEnabled', type: 'checkbox', numeric: false}
        ]
    },
    {
        id: 'network', fields: [
            {id: 'wirelessStream', type: 'checkbox', numeric: false},
            {id: 'mqttEnable', type: 'checkbox', numeric: false},
            {id: 'mqttHost', type: 'text', numeric: false},
            {id: 'mqttPort', type: 'text', numeric: true, digitsOnly: true, min: 1, max: 65535},
            {id: 'mqttTopic', type: 'text', numeric: false},
            {id: 'mqttUser', type: 'text', numeric: false},
            {id: 'mqttPwd', type: 'password', numeric: false},
            {id: 'mqttDiscoveryTopic', type: 'text', numeric: false},
            {id: 'mqttDiscoveryActions', type: 'actions'}
        ],
        subAccordions: [{
            id: 'provisioning', fields: [
                {id: 'improvContext', type: 'note', noteKey: 'improvContext', provisioning: true},
                {id: 'improvSsid', type: 'combo', provisioning: true},
                {id: 'improvWifiPwd', type: 'password', provisioning: true},
                {id: 'improvDeviceName', type: 'text', provisioning: true},
                {id: 'improvEthernetMode', type: 'select', provisioning: true},
                {id: 'improvEthernetBoard', type: 'select', provisioning: true},
                {id: 'improvMi', type: 'text', numeric: true, digitsOnly: true, provisioning: true},
                {id: 'improvMo', type: 'text', numeric: true, digitsOnly: true, provisioning: true},
                {id: 'improvSck', type: 'text', numeric: true, digitsOnly: true, provisioning: true},
                {id: 'improvCs', type: 'text', numeric: true, digitsOnly: true, provisioning: true},
                {id: 'improvComPort', type: 'combo', provisioning: true},
                {id: 'improvBaudrate', type: 'select', provisioning: true},
                {id: 'improvAction', type: 'action', provisioning: true}
            ]
        }]
    },
    {
        id: 'misc', fields: [
            {id: 'toggleLed', type: 'toggleButton', numeric: false},
            {id: 'effect', type: 'select'},
            {id: 'audioDevice', type: 'select'},
            {id: 'audioChannels', type: 'select'},
            {id: 'colorMode', type: 'select'},
            {id: 'gamma', type: 'select', numeric: true},
            {id: 'audioLoopbackGain', type: 'range', numeric: true, min: -5, max: 5, step: '0.1'},
            {id: 'whiteTemperature', type: 'range', numeric: true, min: 2000, max: 11000, step: '50'},
            {id: 'brightness', type: 'range', numeric: true, min: 0, max: 100, step: '1'},
            {id: 'desiredFramerate', type: 'combo'},
        ],
        subAccordions: [
            {
                id: 'gamma', fields: [
                    {id: 'enableAutomaticGamma', type: 'checkbox', numeric: false},
                    {id: 'gammaLevel', type: 'select'}
                ]
            },
            {
                id: 'smoothing', fields: [
                    {id: 'smoothingType', type: 'select'},
                    {id: 'emaAlpha', type: 'select', numeric: true},
                    {id: 'smoothingCaptureFramerate', type: 'readonly'},
                    {id: 'frameInsertionTarget', type: 'select', numeric: true},
                    {id: 'smoothingTargetFramerate', type: 'select', numeric: true}
                ]
            },
            {
                id: 'advancedEyeCare', fields: [
                    {id: 'luminosityThreshold', type: 'select', numeric: true},
                    // Keep the dialog's numeric choices available if an older server omits field options.
                    {
                        id: 'brightnessLimiter', type: 'select', numeric: true,
                        options: [1, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3].map(function (value) {
                            return {value: String(value), label: Math.round(value * 100) + '%'};
                        })
                    },
                    {id: 'enableLDR', type: 'checkbox', numeric: false},
                    {id: 'ldrLabel', type: 'info'},
                    {
                        id: 'ldrInterval', type: 'select', numeric: true,
                        options: [0, 10, 20, 30, 40, 50, 60, 120].map(function (value) {
                            return {value: String(value), label: value === 0 ? '0' : value + ' min'};
                        })
                    },
                    {id: 'ldrTurnOff', type: 'checkbox', numeric: false},
                    {
                        id: 'minimumBrightness', type: 'select', numeric: true,
                        options: Array.from({length: 10}, function (_, index) {
                            var value = (index + 1) * 10;
                            return {value: String(value), label: value + '%'};
                        })
                    },
                    {id: 'ldrCalibration', type: 'actions'}
                ]
            },
            {
                id: 'profiles', fields: [{id: 'profilesControl', type: 'profiles'}]
            }
        ]
    },
    {
        id: 'devices', fields: [
            {id: 'softwareVersion', type: 'info'},
            {id: 'powerSaving', type: 'select'},
            {id: 'multiMonitor', type: 'select'},
            {id: 'multiScreenSingleDevice', type: 'checkbox', numeric: false},
            {id: 'serialPort', type: 'combo'},
            {id: 'baudRate', type: 'select'}
        ],
        subAccordions: [
            {
                id: 'connectedDevices', fields: [
                    {id: 'devicesContent', type: 'note', note: 'Loading devices…'}
                ]
            },
            {
                id: 'satellites', fields: [{id: 'satelliteManager', type: 'satelliteManager'}]
            }
        ]
    }
];
