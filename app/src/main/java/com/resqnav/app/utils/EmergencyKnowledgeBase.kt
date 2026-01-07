
package com.resqnav.app.utils

/**
 * Offline Emergency Response Database
 * Provides instant responses without requiring any API
 */
object EmergencyKnowledgeBase {

    private val responses = mapOf(
        // Earthquake
        "earthquake" to """
            🔴 EARTHQUAKE SAFETY:
            
            1. DROP, COVER, HOLD ON - Get under sturdy furniture
            2. Stay away from windows and heavy objects
            3. If outdoors, move to open space away from buildings
            4. After shaking stops, check for injuries and damage
            5. Be prepared for aftershocks
            
            Emergency: Call 911 if injured or trapped
        """.trimIndent(),

        // Fire
        "fire" to """
            🔥 FIRE EMERGENCY:
            
            1. GET OUT immediately - Don't gather belongings
            2. Stay low to avoid smoke inhalation
            3. Feel doors before opening (if hot, use another exit)
            4. Once out, STAY OUT - Never go back inside
            5. Call 911 from a safe location
            
            If clothes catch fire: STOP, DROP, and ROLL
        """.trimIndent(),

        // Flood
        "flood" to """
            💧 FLOOD SAFETY:
            
            1. Move to higher ground immediately
            2. Avoid walking/driving through flood water
            3. 6 inches of water can knock you down
            4. 1 foot of water can sweep away a vehicle
            5. Never touch electrical equipment if wet
            
            Emergency: Climb to roof if trapped, call 911
        """.trimIndent(),

        // First Aid - Burns
        "burn" to """
            🩹 BURN TREATMENT:
            
            1. Cool the burn with cool (not ice-cold) water for 10-20 minutes
            2. Remove jewelry/tight items before swelling
            3. Cover with sterile, non-stick bandage
            4. Take pain reliever (ibuprofen or acetaminophen)
            5. DO NOT apply ice, butter, or ointments
            
            Seek medical help for: Large burns, face/hand burns, chemical/electrical burns
        """.trimIndent(),

        // First Aid - Bleeding
        "bleeding" to """
            🩹 SEVERE BLEEDING:
            
            1. Apply direct pressure with clean cloth
            2. Maintain pressure for at least 10 minutes
            3. Elevate the injured area above heart if possible
            4. Apply pressure bandage once bleeding slows
            5. If bleeding doesn't stop, call 911
            
            DO NOT remove objects embedded in wounds
        """.trimIndent(),

        // CPR
        "cpr" to """
            ❤️ CPR (Cardiopulmonary Resuscitation):
            
            1. Check if person is responsive - tap and shout
            2. Call 911 immediately
            3. Place hands center of chest
            4. Push hard and fast - 100-120 compressions/minute
            5. Push at least 2 inches deep
            6. Continue until help arrives
            
            If trained: Give 2 rescue breaths after every 30 compressions
        """.trimIndent(),

        // Evacuation
        "evacuation" to """
            🚨 EVACUATION PROCEDURE:
            
            1. Listen to emergency alerts and follow instructions
            2. Grab emergency kit (water, medicine, documents, phone)
            3. Wear sturdy shoes and protective clothing
            4. Follow designated evacuation routes
            5. Don't use elevators - use stairs
            6. Help elderly/disabled neighbors if safe to do so
            
            Never return until authorities say it's safe
        """.trimIndent(),

        // Hurricane
        "hurricane" to """
            🌀 HURRICANE SAFETY:
            
            1. Board up windows or close storm shutters
            2. Move to interior room away from windows
            3. Stay indoors during the storm
            4. Fill bathtub with water (for sanitation)
            5. Charge all devices and have flashlights ready
            
            After: Avoid flood water, downed power lines, and damaged buildings
        """.trimIndent(),

        // Tornado
        "tornado" to """
            🌪️ TORNADO SAFETY:
            
            1. Go to basement or interior room on lowest floor
            2. Stay away from windows
            3. Get under sturdy furniture and protect head
            4. If in mobile home, GET OUT and find sturdy shelter
            5. If outside, lie flat in ditch and cover head
            
            Do NOT try to outrun a tornado in a vehicle
        """.trimIndent(),

        // Choking
        "choking" to """
            🚨 CHOKING (Heimlich Maneuver):
            
            1. Ask "Are you choking?" - If can't speak, begin Heimlich
            2. Stand behind person, wrap arms around waist
            3. Make fist above navel, below ribcage
            4. Grasp fist with other hand
            5. Give quick, upward thrusts
            6. Repeat until object comes out
            
            If person becomes unconscious, call 911 and start CPR
        """.trimIndent(),

        // Heat Stroke
        "heat stroke" to """
            🌡️ HEAT STROKE:
            
            1. Call 911 immediately - this is life-threatening
            2. Move person to cool/shaded area
            3. Remove excess clothing
            4. Cool with water, ice packs on neck/armpits/groin
            5. Fan the person
            
            Signs: High temp (104°F+), confusion, no sweating, rapid pulse
        """.trimIndent(),

        // Default emergency
        "emergency" to """
            🚨 GENERAL EMERGENCY RESPONSE:
            
            1. Assess the situation - ensure your safety first
            2. Call 911 for life-threatening emergencies
            3. Provide first aid if trained and safe to do so
            4. Follow instructions from emergency responders
            5. Stay calm and reassure victims
            
            Available topics: earthquake, fire, flood, CPR, burns, evacuation, tornado, hurricane
        """.trimIndent()
    )

    fun getResponse(query: String): String {
        val lowercaseQuery = query.lowercase()

        // Handle conversational inputs
        when {
            // Greetings
            lowercaseQuery.matches(Regex("^(hi|hello|hey|greetings?)\\s*[!.]*$")) ->
                return "Hello! 👋 I'm your emergency response assistant. Ask me about any emergency situation like earthquakes, fires, floods, first aid, or evacuation procedures. How can I help you stay safe?"

            // Thanks/Gratitude
            lowercaseQuery.contains("thank") || lowercaseQuery.contains("thanks") ->
                return "You're welcome! 😊 Stay safe and remember: In a real emergency, always call 911 first. I'm here anytime you need emergency guidance!"

            // Help requests
            lowercaseQuery.contains("help me") && lowercaseQuery.length < 15 ->
                return "I can help you with:\n\n🔴 Earthquakes\n🔥 Fires\n💧 Floods\n🩹 First Aid (burns, bleeding, CPR)\n🚨 Evacuations\n🌀 Hurricanes\n🌪️ Tornadoes\n🚑 Choking, Heat Stroke\n\nJust ask a question like: \"What should I do during an earthquake?\""

            // Generic "ok" or short responses
            lowercaseQuery.matches(Regex("^(ok|okay|k|alright|fine|yes|no)\\s*[,!.]*$")) ->
                return "👍 Feel free to ask me any emergency-related questions. I'm here to help you prepare and respond to disasters safely!"
        }

        // Check for emergency keyword matches
        return when {
            lowercaseQuery.contains("earthquake") || lowercaseQuery.contains("quake") ->
                responses["earthquake"]!!
            lowercaseQuery.contains("fire") && !lowercaseQuery.contains("first aid") ->
                responses["fire"]!!
            lowercaseQuery.contains("flood") || (lowercaseQuery.contains("water") && lowercaseQuery.contains("emergency")) ->
                responses["flood"]!!
            lowercaseQuery.contains("burn") && (lowercaseQuery.contains("first aid") || lowercaseQuery.contains("treatment") || lowercaseQuery.contains("how")) ->
                responses["burn"]!!
            lowercaseQuery.contains("bleeding") || lowercaseQuery.contains("blood") ->
                responses["bleeding"]!!
            lowercaseQuery.contains("cpr") || lowercaseQuery.contains("cardiac") || lowercaseQuery.contains("heart attack") ->
                responses["cpr"]!!
            lowercaseQuery.contains("evacuat") ->
                responses["evacuation"]!!
            lowercaseQuery.contains("hurricane") || lowercaseQuery.contains("cyclone") ->
                responses["hurricane"]!!
            lowercaseQuery.contains("tornado") || lowercaseQuery.contains("twister") ->
                responses["tornado"]!!
            lowercaseQuery.contains("choking") || lowercaseQuery.contains("heimlich") ->
                responses["choking"]!!
            lowercaseQuery.contains("heat") && (lowercaseQuery.contains("stroke") || lowercaseQuery.contains("exhaustion")) ->
                responses["heat stroke"]!!
            else ->
                responses["emergency"]!!
        }
    }

    fun hasResponse(query: String): Boolean {
        val keywords = listOf(
            "earthquake", "fire", "flood", "burn", "bleeding", "cpr",
            "evacuation", "hurricane", "tornado", "choking", "heat", "emergency"
        )
        val lowercaseQuery = query.lowercase()
        return keywords.any { lowercaseQuery.contains(it) }
    }
}

