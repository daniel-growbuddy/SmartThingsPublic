/**
 *  Copyright 2018 Daniel Starbuck
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Group Light Control
 *
 *  Author: Daniel Starbuck
 */
definition(
        name: "Light Group Controller",
        namespace: "daniel-growbuddy",
        author: "Daniel Starbuck",
        description: "Manage a Group Of Lights Together with a Switch",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance@2x.png",
        singleInstance: true
)

preferences {
    page(name: "installPage")
}

def installPage(){
    dynamicPage(name: "installPage", title:"Light Group Controller", install: true, uninstall: true) {
        if(!state?.controllerInstalled){
            section("Do you want to install the Light Group Controller?"){
                paragraph "Use the install button in the top right of the screen."
            }
        }

        if(state?.controllerInstalled){
            section("Light Group Automation") {
                app(name: "lightGroupAutomation", appName: "Light Group Automation", namespace: "daniel-growbuddy", title: "Create New Light Group", multiple: true)
            }
        }

    }
}

def installed(){
    log.debug "Installed with settings: ${settings}"
    state.nextDni = 1
    state.controllerInstalled = true
    initialize()
}

def uninstalled(){
    state.controllerInstalled = false
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def appInstalled(){
    state.nextDni++
    return state.nextDni
}

def initialize(){
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }
}
