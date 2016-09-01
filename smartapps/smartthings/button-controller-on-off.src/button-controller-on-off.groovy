/**
 *  Copyright 2015 SmartThings
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
 *	Button Controller
 *
 *	Author: SmartThings
 *	Date: 2014-5-21
 */
definition(
    name: "Button Controller ON-OFF",
    namespace: "smartthings",
    author: "Carlos Lasso",
    description: "Turn on and off devices such as light and locks",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
	page(name: "selectButton")
	page(name: "configureButton1")
	page(name: "configureButton2")
	page(name: "configureButton3")
	page(name: "configureButton4")

	}


def selectButton() {
	dynamicPage(name: "selectButton", title: "First, select your button device", nextPage: "configureButton1", uninstall: configured()) {
		section {
			input "buttonDevice", "capability.button", title: "Button", multiple: false, required: true
		}

		section(title: "More options", hidden: hideOptionsSection(), hideable: true) {

			def timeLabel = timeIntervalLabel()


		}
	}
}

def configureButton1() {
	dynamicPage(name: "configureButton1", title: "Now let's decide how to use the first button",
		nextPage: "configureButton2", uninstall: configured(), getButtonSections(1))
}
def configureButton2() {
	dynamicPage(name: "configureButton2", title: "If you have a second button, set it up here",
		nextPage: "configureButton3", uninstall: configured(), getButtonSections(2))
}

def configureButton3() {
	dynamicPage(name: "configureButton3", title: "If you have a third button, you can do even more here",
		nextPage: "configureButton4", uninstall: configured(), getButtonSections(3))
}
def configureButton4() {
	dynamicPage(name: "configureButton4", title: "If you have a fourth button, you rule, and can set it up here",
		install: true, uninstall: true, getButtonSections(4))
}

def getButtonSections(buttonNumber) {
	return {
		section("Lights ON") {
			input "lightson_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lightson_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
		}
        section("Lights OFF") {
			input "lightsoff_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lightsoff_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
		}
		section("Locks ON") {
			input "lockson_${buttonNumber}_pushed", "capability.lock", title: "Pushed", multiple: true, required: false
			input "lockson_${buttonNumber}_held", "capability.lock", title: "Held", multiple: true, required: false
        }
        section("Locks OFF") {
			input "locksoff_${buttonNumber}_pushed", "capability.lock", title: "Pushed", multiple: true, required: false
			input "locksoff_${buttonNumber}_held", "capability.lock", title: "Held", multiple: true, required: false
        }
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(buttonDevice, "button", buttonEvent)
}

def configured() {
	return buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4)
}

def buttonConfigured(idx) {
	return settings["lightson_$idx_pushed"] ||
    	settings["lightsoff_$idx_pushed"] ||
		settings["lockson_$idx_pushed"] ||
        settings["locksoff_$idx_pushed"]
}

def buttonEvent(evt){
	if(allOk) {
		def buttonNumber = evt.data // why doesn't jsonData work? always returning [:]
		def value = evt.value
		log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
		log.debug "button: $buttonNumber, value: $value"

		def recentEvents = buttonDevice.eventsSince(new Date(now() - 3000)).findAll{it.value == evt.value && it.data == evt.data}
		log.debug "Found ${recentEvents.size()?:0} events in past 3 seconds"

		if(recentEvents.size <= 1){
			switch(buttonNumber) {
				case ~/.*1.*/:
					executeHandlers(1, value)
					break
				case ~/.*2.*/:
					executeHandlers(2, value)
					break
				case ~/.*3.*/:
					executeHandlers(3, value)
					break
				case ~/.*4.*/:
					executeHandlers(4, value)
					break
			}
		} else {
			log.debug "Found recent button press events for $buttonNumber with value $value"
		}
	}
}

def executeHandlers(buttonNumber, value) {
	log.debug "executeHandlers: $buttonNumber - $value"

	def lightson = find('lightson', buttonNumber, value)
	if (lightson != null) lightson.on()
    
    def lightsoff = find('lightsoff', buttonNumber, value)
	if (lightsoff != null) lightsoff.off()

	def lockson = find('lockson', buttonNumber, value)
	if (lockson != null) lockson.lock()
    
    def locksoff = find('locksoff', buttonNumber, value)
	if (locksoff != null) locksoff.unlock()
}

def find(type, buttonNumber, value) {
	def preferenceName = type + "_" + buttonNumber + "_" + value
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def findMsg(type, buttonNumber) {
	def preferenceName = type + "_" + buttonNumber
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def changeMode(mode) {
	log.debug "changeMode: $mode, location.mode = $location.mode, location.modes = $location.modes"

	if (location.mode != mode && location.modes?.find { it.name == mode }) {
		setLocationMode(mode)
	}
}

// execution filter methods
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting, location.timeZone).time
		def stop = timeToday(ending, location.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private hideOptionsSection() {
	(starting || ending || days || modes) ? false : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}