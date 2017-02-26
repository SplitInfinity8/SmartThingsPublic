/***
 *  Smart Lock and Open Door Alerts
 *
 *  Copywright 2017 SplitInfinity8
 *  Based on code originally Copyright 2014 Arnaud (Re-written for improved functionality and to reduce security risk.)
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
 * Notes on state storage: http://docs.smartthings.com/en/latest/smartapp-developers-guide/state.html
 *
*** Workflow layout ***
Auto-lock after __ minutes: Yes/No (Default 15, Yes)
  -(Alert on auto-lock attempt)
  -(Verify after 60 seconds, alert if failed - whether alerts are enabled or not.)
If auto-lock fails retry at 1, 5, and 60 minutes: Yes/No (Default Yes)
  -(Alert if auto-lock retry is successful)
Send auto-lock alerts: No, Push, Text, Push and text (Default Push)

Send open door alerts after __ minutes: No, Push, Text, Push and Text (Default 15, Push)
Send up to ___ additional alerts every __ minutes. (Default 4, 60)

Phone number required for optional text alerts: ______________

[Add?] Only auto-lock when motion sensor is idle?

[Add?] Auto-lock WITHOUT door sensor!?  Note, this would be a dumb timed auto-lock and could attempt to lock the door while it is open, etc. Use with caution.

[Add] Security Warning: The following setting is of dubious value and could cause your door to unlock due to a sensor malfunction.
It is left here for the rare use case scenario where someone might have a self closing door that ends up locked in the open position.
Auto-Unlock when door is left open and locked?



Schedule auto-retry attempt at locking door...
Add Followup alert if left open...
Add escalation option for door left open?  IE: Text after an hour if it's still open - vs push after 10 minutes...
Set DEFAULT	values...
***/

definition(
    name: "Smart Lock and Open Door Alerts",
    namespace: "smart-auto-lock-alert-open",
    author: "SplitInfinity8",
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
	    input "autoLock", "enum", title: "Enable auto-lock?", metadata:[values:["Yes", "No"]], required: true, defaultValue: "Yes"
            input "minutesLater", "number", title: "Delay (in minutes):", required: true, defaultValue: 15
    }
//   DISABLED - THIS IS A SECURITY VULNERABILITY IF THE SENSOR EVER MALFUNCTIONS!
//    section("Automatically unlock the door when open...") {
//        input "secondsLater", "number", title: "Delay (in seconds):", required: true
//    }
    section("Send an alert if the door is left open too long...") {
    	input "sendOpenAlert", "enum", title: "Send left open alert?", metadata:[values:["Yes", "No"]], required: true, defaultValue: "Yes"
        input "openMinutesLater", "number", title: "Delay (in minutes):", required: true, defaultValue: 15
    }
    section( "Push notification?" ) {
		input "sendPushMessage", "enum", title: "Send push notification?", metadata:[values:["Yes", "No"]], required: true, defaultValue: "Yes"
    }
    section( "Text message?" ) {
    	input "sendText", "enum", title: "Send text message notification?", metadata:[values:["Yes", "No"]], required: true, defaultValue: "No"
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
    state.autoLockAttempt = 0
    state.doorOpenAlert = 0
    //state.autoLockAttempt = state.autotLockAttempt + 1
    //log.debug "Stored state code example, counter incremented $state.autoLockAttempt times"
}

//********* Preference: sendOpenAlert (sendText)  FINISH IMPLEMENTING PREFERENCES / Enable / disable
def lockDoor()
{
	if (lock1.latestValue("lock") == "unlocked")
    	{
    		log.debug "Locking $lock1..."
    		lock1.lock()
        	log.debug ("If enabled, send push notification? $sendPushMessage, send text message? $sendText") 
    		if (sendPushMessage != "No") sendPush("Attempting to lock $lock1 after $contact1 was closed for $minutesLater minute(s)!")
		if ((sendText == "Yes") && (phoneNumber != "0")) sendSms(phoneNumber, "Attempting to lock $lock1 after $contact1 was closed for $minutesLater minute(s)!")
		//Set a timer for 1 minute to verify that the door successfully relocked.
	        def delay = (1 * 60)
	        runIn (delay, checkLock)
	}
	else if (lock1.latestValue("lock") == "locked"){log.debug "$lock1 was already locked..."}
}

//Verify that attempt to lock the door was successfull - send an alert if it fails... IE: if lock lost power etc.
def checkLock()
{
	//Door closed and unlocked, schedule delayed lock of door.
	if (lock1.latestValue("lock") == "unlocked")
    	{
		//????? Add a second attempt to re-lock if the initial attempt fails?  Option to re-try every X minutes?
    		if (sendPushMessage != "No") sendPush("Attempt at timed lock appears to have failed!!!")
    		if ((sendText == "Yes") && (phoneNumber != "0")) sendSms(phoneNumber, "Attempt at timed lock appears to have failed!!!")
	}
	else if (lock1.latestValue("lock") == "locked")
    	{
    		if (sendPushMessage != "No") sendPush("Verified that timed lock attempt succeeded.")
        }
}


def unlockDoor()
{
	log.debug "Unlock disabled, doing nothing... This is a security vulnerability if enabled, would unlock door any time a sensor malfunctions..."
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
    if ((contact1.latestValue("contact") == "open") || (lock1.latestValue("lock") == "locked") && (evt.value == "open") || (lock1.latestValue("lock") == "unlocked") && (evt.value == "open"))
    {
	unschedule (lockDoor)
	unschedule (checkLock)
	if (sendOpenAlert != "No")
	{
		log.debug "$lock1 door left open, set timer to send a warning! Using open sensor $contact1."
		def delay = (openMinutesLater * 60)
	        runIn (delay, alertOpenDoor)
	}
    }
    else if ((contact1.latestValue("contact") == "closed") && (evt.value == "locked") || (lock1.latestValue("lock") == "locked") && (evt.value == "closed"))
    {
        log.debug "$lock1 closed and already locked, do nothing... Using open sensor $contact1."
	unschedule (alertOpenDoor)
        unschedule (lockDoor)
    }
    else if ((contact1.latestValue("contact") == "closed") && (evt.value == "unlocked") || (lock1.latestValue("lock") == "unlocked") && (evt.value == "closed"))
    {
	//Door closed and unlocked, schedule delayed lock of door.
	unschedule (alertOpenDoor)
	if (autoLock != "No")
	{
		log.debug "$lock1 door closed and unlocked, scheduling delayed lock. Using open sensor $contact1."
        	def delay = (minutesLater * 60)
        	runIn (delay, lockDoor)
    	}
    }
    else
    {
        log.debug "$lock1, problem reading status, the lock or door sensor might be malfunctioning, changing states or jammed!? Using open sensor $contact1."
        //Send an alert if this happens?
        unschedule (lockDoor)
        unschedule (alertDoorOpen)
    }
}
