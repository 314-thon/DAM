package com.ifts18.unadecisionmas.data.repository

import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.data.model.AchievementsCatalog
import com.ifts18.unadecisionmas.data.model.ChatMessage
import com.ifts18.unadecisionmas.data.model.DecisionOption
import com.ifts18.unadecisionmas.data.model.GameScenario

object GameScenariosRepository {

    fun getScenarios(playerName: String): List<GameScenario> {
        val name = playerName.ifBlank { "Mateo" }
        return listOf(
            // Lunes: Carnada inicial vs Fulbito con amigos (Ideas de Lucas + Roberto Araujo)
            GameScenario(
                dayIndex = 0,
                dayName = "Lunes",
                title = "La fija del día o la canchita",
                messages = listOf(
                    ChatMessage(
                        sender = "Lucas",
                        message = "Che $name, me pasaron una fija para la Champions de hoy.",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Franco (Curso)",
                        message = "Chicos, reservé fútbol 5 a las 19 hs. Son $2.500 cada uno y después tomamos algo.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Lucas",
                        message = "No gastes en la cancha $name. Poné $3.000 acá y en 15 minutos duplicamos todo.",
                        delayAfterMillis = 1400L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Ir a jugar al fútbol",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = -2500,
                    wellbeingDelta = 15,
                    achievementId = AchievementsCatalog.ID_VIDA_REAL,
                    educationalFeedback = "Invertir en planes reales con amigos suma bienestar sin riesgo de adicción."
                ),
                alternativeOption = DecisionOption(
                    text = "Rechazar la apuesta",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 8,
                    educationalFeedback = "Identificaste la falsa urgencia y cuidaste tu bolsillo."
                ),
                riskyOption = DecisionOption(
                    text = "Apostar $3.000 a la fija",
                    isSafe = false,
                    penaltyAmount = 3000,
                    savingsDelta = -3000,
                    wellbeingDelta = -10,
                    educationalFeedback = "Prometer plata rápida en minutos es la carnada clásica para engancharte."
                ),
                timeoutPenalty = 2000,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Contrastar el gasto saludable en vínculos reales frente a la promesa engañosa del dinero rápido."
            ),

            // Martes: El Error de Transferencia (Idea de Evelyn Gaitán)
            GameScenario(
                dayIndex = 1,
                dayName = "Martes",
                title = "El error de transferencia ($80.000)",
                messages = listOf(
                    ChatMessage(
                        sender = "Mamá",
                        message = "Hijo, ahí te pasé los $8.000 para sacar los apuntes de la fotocopiadora.",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Billetera Virtual",
                        message = "Recibiste una transferencia de $80.000,00. Mamá puso un cero de más por error.",
                        delayAfterMillis = 1700L
                    ),
                    ChatMessage(
                        sender = "Nico (Compañero)",
                        message = "No le digas nada todavía, $name. Metemos los $72.000 que sobran al casino, duplicás en un toque y se los devolvés sin que se entere.",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Devolver los $72.000 ahora",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 2000,
                    wellbeingDelta = 20,
                    achievementId = AchievementsCatalog.ID_HONRADO_100,
                    educationalFeedback = "Devolviste la plata al instante y tu mamá te dejó $2.000 por tu honestidad."
                ),
                alternativeOption = DecisionOption(
                    text = "Pagar copias y avisarle",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 1000,
                    wellbeingDelta = 15,
                    achievementId = AchievementsCatalog.ID_HONRADO_100,
                    educationalFeedback = "Actuaste con honestidad y frenaste la tentación de usar dinero familiar."
                ),
                riskyOption = DecisionOption(
                    text = "Apostar los $72.000 sobrantes",
                    isSafe = false,
                    penaltyAmount = 72000,
                    savingsDelta = -8000,
                    wellbeingDelta = -20,
                    educationalFeedback = "Apostar plata familiar pensando en devolverla después es una de las causas más graves de deuda."
                ),
                timeoutPenalty = 4000,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Visibilizar el riesgo de apostar dinero familiar o destinado a estudios bajo la ilusión de ganar y devolver."
            ),

            // Miércoles: La plata de la SUBE y el mito de la racha (Idea de Evelyn Gaitán)
            GameScenario(
                dayIndex = 2,
                dayName = "Miércoles",
                title = "El saldo de la SUBE",
                messages = listOf(
                    ChatMessage(
                        sender = "Joaco",
                        message = "Recién saqué $2.000 en los slots. El algoritmo está pagando todo hoy.",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Sistema SUBE",
                        message = "Recordatorio: tenés que separar $2.500 para cargar el transporte de la semana.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Joaco",
                        message = "Meté lo de la SUBE antes de que se enfríe la máquina, $name. Con un tiro viajás todo el mes.",
                        delayAfterMillis = 1400L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Cargar la SUBE primero",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = -2500,
                    wellbeingDelta = 12,
                    educationalFeedback = "Priorizaste tus gastos reales de transporte sin caer en el mito de la máquina caliente."
                ),
                alternativeOption = DecisionOption(
                    text = "Frenar a Joaco con datos",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 15,
                    educationalFeedback = "Desarmar el mito de la racha con un amigo ayuda a cortar el contagio grupal."
                ),
                riskyOption = DecisionOption(
                    text = "Timbear el saldo de la SUBE",
                    isSafe = false,
                    penaltyAmount = 4500,
                    savingsDelta = -2500,
                    wellbeingDelta = -15,
                    educationalFeedback = "Perdiste la plata del transporte. Las máquinas no tienen rachas calientes."
                ),
                timeoutPenalty = 3000,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Cuestionar el mito de la racha y proteger el dinero de gastos esenciales como el transporte."
            ),

            // Jueves: Préstamo inmediato e Influencer vendehumo (Idea de Lucía Pobeda + Sensor)
            GameScenario(
                dayIndex = 3,
                dayName = "Jueves",
                title = "Influencers y préstamos fáciles",
                messages = listOf(
                    ChatMessage(
                        sender = "Tomi Fitness VIP",
                        message = "¿Querés este nivel de vida? Empecé con $2.000 en B-Play VIP. El primer tiro tiene premio seguro.",
                        delayAfterMillis = 1500L,
                        isForwarded = true
                    ),
                    ChatMessage(
                        sender = "Lucas",
                        message = "Amigo, con un clic te aprueban $20.000 al instante y ni se siente el gasto. ¿Entramos?",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Rechazar crédito y bloquear",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 15,
                    achievementId = AchievementsCatalog.ID_EVITANDO_TENTACION,
                    educationalFeedback = "Los influencers cobran por mostrar ganancias falsas con cuentas demo preparadas para ganar."
                ),
                alternativeOption = DecisionOption(
                    text = "Advertir al grupo del engaño",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 18,
                    educationalFeedback = "Desarmaste dos trampas juntas: publicidad encubierta y deuda instantánea."
                ),
                riskyOption = DecisionOption(
                    text = "Pedir $20.000 e instalar app",
                    isSafe = false,
                    penaltyAmount = 10000,
                    savingsDelta = -4000,
                    wellbeingDelta = -18,
                    educationalFeedback = "Los créditos fáciles sin fricción buscan que te endeudes sin notar que gastás plata real."
                ),
                timeoutPenalty = 4500,
                decisionTimeSeconds = 25,
                triggersSpamShakeAlert = true,
                pedagogicalFocus = "Desenmascarar la publicidad engañosa de influencers y el peligro de los micropréstamos instantáneos."
            ),

            // Viernes: Recompensa por Mérito vs Perseguir pérdidas (Ideas de Evelyn + Roberto)
            GameScenario(
                dayIndex = 4,
                dayName = "Viernes",
                title = "Premio por tus notas",
                messages = listOf(
                    ChatMessage(
                        sender = "Papá",
                        message = "Felicitaciones por el 9 en el examen, $name. Te transferí $10.000 para que los disfrutes o ahorres.",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Santi",
                        message = "Justo, $name. El cajero nos fía $5.000 más. Si metemos tu premio recuperamos todo lo de la semana.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Santi",
                        message = "No te quedes con la espina, con un pleno damos vuelta todo.",
                        delayAfterMillis = 1400L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Guardar los $10.000",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 10000,
                    wellbeingDelta = 12,
                    educationalFeedback = "El premio a tu esfuerzo quedó resguardado en tus ahorros reales."
                ),
                alternativeOption = DecisionOption(
                    text = "Ir al cine con amigos",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 6500,
                    wellbeingDelta = 18,
                    achievementId = AchievementsCatalog.ID_VIDA_REAL,
                    educationalFeedback = "Disfrutaste tu mérito en una salida real y además sumaste ahorro."
                ),
                riskyOption = DecisionOption(
                    text = "Apostar premio + $5.000 fiados",
                    isSafe = false,
                    penaltyAmount = 8000,
                    savingsDelta = -5000,
                    wellbeingDelta = -15,
                    educationalFeedback = "Intentar recuperar lo perdido con plata fiada multiplica la deuda muy rápido."
                ),
                timeoutPenalty = 4000,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Valorar el dinero ganado con mérito propio y frenar la persecución compulsiva de pérdidas."
            ),

            // Sábado: El Superclásico del Fin de Semana (Idea de Juan Chaile + Roberto)
            GameScenario(
                dayIndex = 5,
                dayName = "Sábado",
                title = "Fiebre de Superclásico",
                messages = listOf(
                    ChatMessage(
                        sender = "Mati",
                        message = "",
                        delayAfterMillis = 1500L,
                        imageResId = R.drawable.captura,
                        isForwarded = true
                    ),
                    ChatMessage(
                        sender = "Mati",
                        message = "Hoy se juega el Superclásico. Mirá la captura que subieron al grupo, están todos haciendo combinadas.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Mati",
                        message = "¿Vas a ser el único que lo mire sin ponerle $5.000 al partido?",
                        delayAfterMillis = 1400L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Desmentir la captura falsa",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 16,
                    educationalFeedback = "En redes y grupos nadie publica los cientos de tickets perdidos."
                ),
                alternativeOption = DecisionOption(
                    text = "Mirar el partido sin apostar",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 12,
                    educationalFeedback = "Disfrutar del deporte sin apostar te protege de la ansiedad y la presión grupal."
                ),
                riskyOption = DecisionOption(
                    text = "Meter $5.000 a combinada",
                    isSafe = false,
                    penaltyAmount = 5000,
                    savingsDelta = -5000,
                    wellbeingDelta = -12,
                    educationalFeedback = "Las apuestas combinadas en clásicos son las que mayor margen de ganancia le dejan a la casa."
                ),
                timeoutPenalty = 3500,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Desarmar la presión social durante grandes eventos deportivos como el Superclásico."
            ),

            // Domingo: Extorsión del cajero ilegal y cierre de semana
            GameScenario(
                dayIndex = 6,
                dayName = "Domingo",
                title = "Amenaza del cajero",
                messages = listOf(
                    ChatMessage(
                        sender = "Cajero VIP Nocturno",
                        message = "Último aviso, $name: tenés un cargo pendiente. Si transferís $4.000 ya cierro tu carpeta.",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Cajero VIP Nocturno",
                        message = "Si no pagás antes de que termine el contador, le escribimos a tus contactos del colegio.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Cajero VIP Nocturno",
                        message = "Decidí ya.",
                        delayAfterMillis = 1300L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Bloquear y pedir ayuda",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 20,
                    educationalFeedback = "Ante una extorsión ilegal jamás hay que transferir: hablar con tu familia o llamar al 141 corta el chantaje."
                ),
                alternativeOption = DecisionOption(
                    text = "Guardar capturas y consultar",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 16,
                    educationalFeedback = "Pedir orientación en la escuela o canales oficiales desactiva cualquier amenaza."
                ),
                riskyOption = DecisionOption(
                    text = "Transferir $4.000 por miedo",
                    isSafe = false,
                    penaltyAmount = 4000,
                    savingsDelta = -4000,
                    wellbeingDelta = -20,
                    educationalFeedback = "Pagar por miedo nunca frena a un cajero ilegal; solo abre nuevos pedidos de plata."
                ),
                timeoutPenalty = 3000,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Promover el pedido de ayuda a adultos y líneas oficiales (141 / Boti) frente al hostigamiento de cajeros ilegales."
            )
        )
    }
}
