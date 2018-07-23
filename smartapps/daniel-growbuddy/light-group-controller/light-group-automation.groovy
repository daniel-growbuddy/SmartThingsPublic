definition(
        name: "Light Group Automation",
        namespace: "daniel-growbuddy",
        author: "Daniel Starbuck",
        description: "A simple app to control light group creation and basic lighting automations.",
        category: "My Apps",

        // the parent option allows you to specify the parent app in the form <namespace>/<app name>
        parent: "daniel-growbuddy:Light Group Controller",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "mainPage", title: "Group Lights & Switches for Automation", install: false, uninstall: true, nextPage: "lightPage"
    page name: "lightPage", title: "Select the lights you wish to group control", install: false, uninstall: false, nextPage: "switchPage"
    page name: "switchPage", title: "Select the switch you wish to use for controlling the lights", install: false, uninstall: false, nextPage: "namePage"
    page name: "namePage", title: "Group Lights & Switches for Automation", install: true, uninstall: true
}

def currentInstallNumber = 1

def setCurrentInstallNumber(installNumber) {
    currentInstallNumber = installNumber
}

// main page to select lights, the action, and turn on/off times
def mainPage() {
    dynamicPage(name: "mainPage") {
        section("The Hub") {
            input "theHub", "hub", title: "Select the hub (required for local execution) (Optional)", multiple: false, required: false
        }
        section("Light Type"){
            input "virtualDeviceType", "enum", title: "Which type of group/virtual device do you want to create?", multiple: false, required: true, options: ["Simulated RGBW Bulb"]
        }
        section("Light Control Device Name") {
            input "deviceName", title: "Enter custom name for the control device", defaultValue: "Light Group ${currentInstallNumber}", required: true

        }
    }
}

def lightPage() {
    dynamicPage(name: "lightPage") {
        section("Lights to Control"){
            input "slaveLights", "capability.colorControl", title: "Which lights do you want to control?", multiple: true, submitOnChange: true
        }
    }
}

def switchPage() {
    dynamicPage(name: "lightPage") {
        section("Light Switch for Control"){
            input "lightSwitch", "capability.switch", title: "Which switch do you use to control the lights manually?", multiple: false, submitOnChange: true
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
    initialize()
    parent.appInstalled()
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
    if (virtualDeviceType) {
        def d = addChildDevice("smartthings", virtualDeviceType, "virtual-$currentInstallNumber", theHub?.id, [completedSetup: true, label: deviceName])
        state.masterLight = d
    } else {
        log.error "Failed creating Virtual Device because the device type was missing"
    }

    subscribe(state.masterLight, "switch.on", turnLightsOn)
    subscribe(state.masterLight, "switch.off", turnLightsOff)
    subscribe(state.masterLight, "hue", colorHandler)
    subscribe(state.masterLight, "saturation", colorHandler)
    subscribe(state.masterLight, "level", colorHandler)
    subscribe(state.masterLight, "colorTemperature", tempHandler)

    subscribe(lightSwitch, "switch.on", turnLightsOn)
    subscribe(lightSwitch, "switch.off", turnLightsOff)

}


def turnLightsOn(evt) {
    if(lightSwitch.off){
        lightSwitch.on()
    }

    if(state.virtualLight.off){
        state.virtualLight.on()
    }

    slaveLights?.on()
}

def turnLightsOff(evt) {
    if(lightSwitch.on){
        lightSwitch.off()
    }

    if(state.virtualLight.on){
        state.virtualLight.off()
    }

    slaveLights?.off()
}

def colorHandler(evt) {
    if(state.masterLight?.currentValue("switch") == "on"){
        log.debug "Changing Slave units H,S,L"
        def dimLevel = state.masterLight?.currentValue("level")
        def hueLevel = state.masterLight?.currentValue("hue")
        def saturationLevel = state.masterLight.currentValue("saturation")
        def newValue = [hue: hueLevel, saturation: saturationLevel, level: dimLevel as Integer]
        slaveLights?.setColor(newValue)
    }
}

def tempHandler(evt){
    if(state.masterLight?.currentValue("switch") == "on"){
        if(evt.value != "--"){
            log.debug "Changing Slave color temp based on Master change"
            def tempLevel = state.masterLight?.currentValue("colorTemperature")
            slaveLights?.setColorTemperature(tempLevel)
        }
    }
}