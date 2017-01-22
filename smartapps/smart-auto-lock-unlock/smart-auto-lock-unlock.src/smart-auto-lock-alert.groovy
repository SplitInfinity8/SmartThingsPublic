/**
 *  Smart Lock / Alert Left Open
 *
 *  Copyright 2014 Arnaud
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
 */
definition(
    name: "Smart Auto Lock - Alert Left Open",
    namespace: "smart-auto-lock-alert-open",
    author: "Arnaud - modified by SplitInfinity8",
    description: "Automatically lock door X minutes after being closed, and alert X minutes after being left open.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences
{
    section("Select the door lock:") {
        input "lock1", "capability.lock", required: true
    }
    section("Select the door contact sensor:") {
    	input "contact1", "capability.contactSensor", required: true
    }
    section("Automatically lock the door when closed...") {
	    input "autoLock", "enum", title: "Enable auto-lock?", metadata:[values:["Yes", "No"]], required: false, defaultValue: "Yes"
            input "minutesLater", "number", title: "Delay (in minutes):", required: true, defaultValue: 15
    }
//   DISABLED - THIS IS A SECURITY VULNERABILITY IF THE SENSOR EVER MALFUNCTIONS!
//    section("Automatically unlock the door when open...") {
//        input "secondsLater", "number", title: "Delay (in seconds):", required: true
//    }
    section("Send an alert if the door is left open too long...") {
    	input "sendOpenAlert", "enum", title: "Send left open alert?", metadata:[values:["Yes", "No"]], required: false, defaultValue: "Yes"
        input "openMinutesLater", "number", title: "Delay (in minutes):", required: true, defaultValue: 15
    }
    section( "Push notification?" ) {
		input "sendPushMessage", "enum", title: "Send push notification?", metadata:[values:["Yes", "No"]], required: false, defaultValue: "Yes"
    }
    section( "Text message?" ) {
    	input "sendText", "enum", title: "Send text message notification?", metadata:[values:["Yes", "No"]], required: false, defaultValue: "No"
       	input "phoneNumber", "phone", title: "Enter phone number:", required: false
    }
}

def installed()
{
    initialize()
}

def updated()
{
    unsubscribe()
    unschedule()
    initialize()
}

def initialize()
{
    log.debug "Settings: ${settings}"
    subscribe(lock1, "lock", doorHandler, [filterEvents: false])
    subscribe(lock1, "unlock", doorHandler, [filterEvents: false])  
    subscribe(contact1, "contact.open", doorHandler)
    subscribe(contact1, "contact.closed", doorHandler)
}

//autoLock, sendOpenAlert (sendText)  FINISH IMPLEMENTING PREFERENCES / Enable / disable
def lockDoor()
{
	if (lock1.latestValue("lock") == "unlocked")
    	{
    		log.debug "Locking $lock1..."
    		lock1.lock()
        	log.debug ("Sending Push Notification...") 
    		if (sendPushMessage != "No") sendPush("$lock1 locked after $contact1 was closed for $minutesLater minute(s)!")
    		log.debug("Sending text message...")
		if ((sendText == "Yes") && (phoneNumber != "0")) sendSms(phoneNumber, "$lock1 locked after $contact1 was closed for $minutesLater minute(s)!")
        }
	else if (lock1.latestValue("lock") == "locked")
    	{
        log.debug "$lock1 was already locked..."
        }
}

def unlockDoor()
{
	log.debug "Unlock disabled, doing nothing..."
	/*
	//Automated door unlock is a possible security issue if sensor ever malfunctions and usefulness is dubious, DISABLED!
	if (lock1.latestValue("lock") == "locked")
    	{
    		log.debug "Unlocking $lock1..."
    		lock1.unlock()
        	log.debug ("Sending Push Notification...") 
    		if (sendPushMessage != "No") sendPush("$lock1 unlocked after $contact1 was open for $secondsLater seconds(s)!")
    		log.debug("Sending text message...")
		if ((sendText == "Yes") && (phoneNumber != "0")) sendSms(phoneNumber, "$lock1 unlocked after $contact1 was open for $secondsLater seconds(s)!")        
        }
	else if (lock1.latestValue("lock") == "unlocked")
    	{
        log.debug "$lock1 was already unlocked..."
        }
	*/
}

def alertOpenDoor()
{
	if (contact1.latestValue("contact") == "open")
    	{
    		log.debug "Sending alert, door left open!"
    		if (sendPushMessage != "No") sendPush("WARNING: $lock1 left open for $openMinutesLater minute(s)!")
    		if ((sendText == "Yes") && (phoneNumber != "0")) sendSms(phoneNumber, "WARNING: $lock1 left open for $openMinutesLater minute(s)!")
        }
	else if (contact1.latestValue("contact") == "closed")
    	{
        	log.debug "$lock1 door was already closed..."
        }
}

def doorHandler(evt)
{
    if ((contact1.latestValue("contact") == "open"))
    {
    //if ((contact1.latestValue("contact") == "open") && (evt.value == "locked"))
    //else if ((contact1.latestValue("contact") == "open") && (evt.value == "unlocked"))
        unschedule (lockDoor)
    	log.debug "Door left open, set timer to send a warning!"
        def delay = (openMinutesLater * 60)
        runIn (delay, alertOpenDoor)
    }
    else if ((contact1.latestValue("contact") == "closed") && (evt.value == "locked"))
    {
	//Door already locked, unschedule pending lock.
	unschedule (alertOpenDoor)
        unschedule (lockDoor)
    }   
    else if ((contact1.latestValue("contact") == "closed") && (evt.value == "unlocked"))
    {
	//Door closed and unlocked, schedule delayed lock of door.
	unschedule (alertOpenDoor)
        log.debug "Scheduling lock of $lock1..."
        def delay = (minutesLater * 60)
        runIn (delay, lockDoor)
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "open"))
    {
        unschedule (lockDoor)
    	log.debug "Door left open, set timer to send a warning!"
        def delay = (openMinutesLater * 60)
        runIn (delay, alertOpenDoor)
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "closed"))
    {
    	//lock1.unlock()
	unschedule (alertOpenDoor)
        log.debug "Scheduling lock of $lock1..."
        def delay = (minutesLater * 60)
        runIn (delay, lockDoor)
    }
    else if ((lock1.latestValue("lock") == "locked") && (evt.value == "open"))
    {
        //lock1.unlock()
        unschedule (lockDoor)
    	log.debug "Door left open and locked?, set timer to send a warning!"
        def delay = (openMinutesLater * 60)
        runIn (delay, alertOpenDoor)
    }
    else if ((lock1.latestValue("lock") == "locked") && (evt.value == "closed"))
    {
        log.debug "$lock1 closed and locked, do nothing..."
        unschedule (lockDoor)
        unschedule (alertDoorOpen)
    	//lock1.unlock()
    }
    else
    {
        log.debug "Problem with $lock1, the lock might be jammed!"
        unschedule (lockDoor)
        unschedule (alertDoorOpen)
    }
}
