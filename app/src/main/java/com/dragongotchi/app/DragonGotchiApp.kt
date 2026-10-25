package com.dragongotchi.app

import android.app.Application
import com.dragongotchi.app.data.local.AppDatabase
import com.dragongotchi.app.data.preferences.UserPreferences
import com.dragongotchi.app.data.repository.PetRepository
import com.dragongotchi.app.data.repository.RewardsRepository
import com.dragongotchi.app.data.repository.SessionRepository
import com.dragongotchi.app.location.TrackingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Contenedor de dependencias manual (sin Hilt: proyecto mas simple de mantener). */
class AppContainer(application: Application) {
    private val database = AppDatabase.create(application)
    val sessionRepository = SessionRepository(database.sessionDao())
    val petRepository = PetRepository(database.petDao())
    val userPreferences = UserPreferences(application)
    val rewardsRepository = RewardsRepository(userPreferences)
    val trackingManager = TrackingManager()
}

class DragonGotchiApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // El SDK de anuncios NO se inicializa aqui: en Europa (GDPR) hay
        // que recoger antes el consentimiento UMP. Lo hace MainActivity
        // al arrancar y solo entonces inicializa Mobile Ads.
        container = AppContainer(this)
        appScope.launch { container.petRepository.ensurePetExists() }
    }
}
