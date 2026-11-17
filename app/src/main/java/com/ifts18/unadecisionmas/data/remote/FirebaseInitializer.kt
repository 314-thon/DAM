package com.ifts18.unadecisionmas.data.remote

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ifts18.unadecisionmas.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Inicialización centralizada de Firebase SDK usando credenciales inyectadas
 * desde local.properties hacia BuildConfig (sin secretos en el repositorio).
 */
object FirebaseInitializer {

    @Synchronized
    fun ensureInitialized(context: Context): Boolean {
        return try {
            val appContext = context.applicationContext
            if (FirebaseApp.getApps(appContext).isEmpty() &&
                BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
                BuildConfig.FIREBASE_APP_ID.isNotBlank()
            ) {
                val optionsBuilder = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)

                if (BuildConfig.FIREBASE_DB_URL.isNotBlank()) {
                    optionsBuilder.setDatabaseUrl(BuildConfig.FIREBASE_DB_URL)
                }
                if (BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()) {
                    optionsBuilder.setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                }
                FirebaseApp.initializeApp(appContext, optionsBuilder.build())
            }
            FirebaseApp.getApps(appContext).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun getAuthOrNull(context: Context): FirebaseAuth? {
        return if (ensureInitialized(context)) {
            runCatching { FirebaseAuth.getInstance() }.getOrNull()
        } else null
    }

    fun getDatabaseOrNull(context: Context): FirebaseDatabase? {
        if (!ensureInitialized(context)) return null
        return runCatching {
            if (BuildConfig.FIREBASE_DB_URL.isNotBlank()) {
                FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DB_URL)
            } else {
                FirebaseDatabase.getInstance()
            }
        }.getOrNull()
    }
}

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            cont.resume(task.result as T)
        } else {
            cont.resumeWithException(
                task.exception ?: IllegalStateException("Error desconocido en tarea de Firebase")
            )
        }
    }
}
