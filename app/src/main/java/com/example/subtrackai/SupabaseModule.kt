package com.example.subtrackai

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

val supabase = createSupabaseClient(
    supabaseUrl = "https://osqgdftvkfcumkwnhgov.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9zcWdkZnR2a2ZjdW1rd25oZ292Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk1MDY4MTQsImV4cCI6MjA5NTA4MjgxNH0.74skaKWmwPahQ8OwKqVt-qD0khlXyzY2DBgJo2Cr2Sg"
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    
    defaultSerializer = KotlinXSerializer(Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = false // Important: Let DB defaults handle nulls
        isLenient = true
    })
}
