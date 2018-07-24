definition(
        name: "Light Group Automation",
        namespace: "daniel-growbuddy",
        author: "Daniel Starbuck",
        description: "A simple app to control light group creation and basic lighting automations.",
        category: "My Apps",

        // the parent option allows you to specify the parent app in the form <namespace>/<app name> asdf
        parent: "daniel-growbuddy:Light Group Controller",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

def setCurrentInstallNumber(installNumber) {
    state.automationInstallNumber = installNumber
}

preferences {
    page name: "mainPage", title: "Group Lights & Switches for Automation", install: false, uninstall: true, nextPage: "lightPage"
    page name: "lightPage", title: "Select the lights you wish to group control", install: false, uninstall: false, nextPage: "switchPage"
    page name: "switchPage", title: "Select the switch you wish to use for controlling the lights", install: false, uninstall: false, nextPage: "namePage"
    page name: "namePage", title: "Group Lights & Switches for Automation", install: true, uninstall: true
}


// main page to select lights, the action, and turn on/off times
def mainPage() {
    dynamicPage(name: "mainPage") {
        section("The Hub") {
            input "theHub", "hub", title: "Select the hub (required for local execution) (Optional)", multiple: false, required: false
        }
        section("Light Type"){
            input "virtualDeviceType", "enum", title: "Which type of group/virtual device do you want to create?", multiple: false, required: true, options: ["Virtual RGBW Bulb"]
        }
        section("Light Control Device Name") {
            input "deviceName", title: "Enter custom name for the control device", defaultValue: "My Light Bulb", required: true

        }
    }
}

def lightPage() {
    dynamicPage(name: "lightPage") {
        section("Lights to Control"){
            input "slaveLights", "capability.colorControl", title: "Which lights do you want to control?", multiple: true, required: true
        }
    }
}

def switchPage() {
    dynamicPage(name: "switchPage") {
        section("Light Switch for Control"){
            input "lightSwitch", "capability.switch", title: "Which switch do you use to control the lights manually?", multiple: false, required: true
        }
    }
}

// page for allowing the user to give the automation a custom name
def namePage() {
    dynamicPage(name: "namePage") {
        section("Light Group Automation name") {
            label title: "Enter name for light group automation", defaultValue: app.label, required: true
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    state.automationInstallNumber = parent.appInstalled()
    initialize()
}

def uninstalled() {
    getAllChildDevices().each {
        deleteChildDevice(it.deviceNetworkId, true)
    }
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    uninstalled()
    initialize()
}



def initialize() {
    def newDevice
    if (virtualDeviceType) {
        log.debug "Install Number ${state.automationInstallNumber ?: 1}"
        newDevice = addChildDevice("daniel-growbuddy", virtualDeviceType, "virtual-$state.automationInstallNumber", theHub?.id, [completedSetup: true, label: deviceName])
    } else {
        log.error "Failed creating Virtual Device because the device type was missing"
    }

    log.debug "Device Type: $newDevice.deviceType"

    if(newDevice){
        log.debug "added subscriptions"
        subscribe(newDevice, "switch.on", turnLightsOn)
        subscribe(newDevice, "switch.off", turnLightsOff)
        subscribe(newDevice, "hue", colorHandler)
        subscribe(newDevice, "saturation", colorHandler)
        subscribe(newDevice, "level", colorHandler)
        subscribe(newDevice, "colorTemperature", tempHandler)

        subscribe(lightSwitch, "switch.on", turnLightsOn)
        subscribe(lightSwitch, "switch.off", turnLightsOff)
    }

}


def turnLightsOn(evt) {
    log.debug "Turn Lights On"

    if(lightSwitch.off){
        lightSwitch.on()
    }

    getAllChildDevices().each{
        if(it.off){
            it.on()
        }
    }

    slaveLights?.on()
}

def turnLightsOff(evt) {
    log.debug "Turn Lights Off"

    if(lightSwitch.on){
        lightSwitch.off()
    }

    getAllChildDevices().each{
        if(it.on){
            it.off()
        }
    }

    slaveLights?.off()
}

def colorHandler(evt) {
    log.debug "Color Lights"
    def masterLight = getAllChildDevices.first()

    if(masterLight?.currentValue("switch") == "on"){
        log.debug "Changing Slave units H,S,L"
        def dimLevel = masterLight.currentValue("level")
        def hueLevel = masterLight.currentValue("hue")
        def saturationLevel = masterLight.currentValue("saturation")
        def newValue = [hue: hueLevel, saturation: saturationLevel, level: dimLevel as Integer]
        slaveLights?.setColor(newValue)
    }
}

def tempHandler(evt){
    def masterLight = getAllChildDevices.first()

    if(masterLight?.currentValue("switch") == "on"){
        if(evt.value != "--"){
            log.debug "Changing Slave color temp based on Master change"
            def tempLevel = masterLight.currentValue("colorTemperature")
            slaveLights?.setColorTemperature(tempLevel)
        }
    }
}