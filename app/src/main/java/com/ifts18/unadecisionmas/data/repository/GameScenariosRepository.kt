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
                title = "La carnada inicial vs. Canchita con amigos",
                messages = listOf(
                    ChatMessage(
                        sender = "Lucas",
                        message = "¡Che $name! Me pasaron una fija imperdible para la Champions de hoy 🔥",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Franco (Curso)",
                        message = "Chicos, reservé la cancha de fútbol 5 a las 19 hs. Son $2.500 por cabeza y después tomamos algo.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Lucas",
                        message = "¡No gastes en la cancha $name! Poné $3.000 acá y en 15 minutos duplicamos todo.",
                        delayAfterMillis = 1400L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Ir al fulbito con los pibes y tomar algo",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = -2500,
                    wellbeingDelta = 15,
                    achievementId = AchievementsCatalog.ID_VIDA_REAL,
                    educationalFeedback = "Invertir en planes reales y deporte con amigos suma bienestar real sin riesgo de adicción."
                ),
                alternativeOption = DecisionOption(
                    text = "Cuidar el ahorro y rechazar la apuesta",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 8,
                    educationalFeedback = "Supiste identificar la falsa urgencia y protegiste tu bolsillo."
                ),
                riskyOption = DecisionOption(
                    text = "Apostar $3.000 a la supuesta fija",
                    isSafe = false,
                    penaltyAmount = 3000,
                    savingsDelta = -3000,
                    wellbeingDelta = -10,
                    educationalFeedback = "La promesa de duplicar plata fácil en minutos es la carnada clásica para engancharte."
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
                        sender = "Mamá ❤️",
                        message = "Hijo, ahí te pasé los $8.000 para sacar los apuntes de la fotocopiadora.",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Billetera Virtual",
                        message = "💸 Recibiste una transferencia de $80.000,00. (Tu mamá puso un cero de más por error).",
                        delayAfterMillis = 1700L
                    ),
                    ChatMessage(
                        sender = "Nico (Compañero)",
                        message = "¡Uh $name, no le digas nada todavía! Metemos las $72.000 que sobran al casino, duplicás en un toque y después le devolvés las $72 lucas sin que se entere.",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Devolverle ya mismo los $72.000 de más a mamá",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 2000,
                    wellbeingDelta = 20,
                    achievementId = AchievementsCatalog.ID_HONRADO_100,
                    educationalFeedback = "¡100% Honrado! Devolviste la plata al instante y tu mamá te regaló $2.000 extra por tu honestidad."
                ),
                alternativeOption = DecisionOption(
                    text = "Pagar los apuntes y transferirle el resto a mamá avisándole del error",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 1000,
                    wellbeingDelta = 15,
                    achievementId = AchievementsCatalog.ID_HONRADO_100,
                    educationalFeedback = "Actuaste con honestidad, devolviste la diferencia y frenaste la tentación de usar dinero familiar."
                ),
                riskyOption = DecisionOption(
                    text = "Apostar los $72.000 sobrantes para duplicarlos rápido",
                    isSafe = false,
                    penaltyAmount = 72000,
                    savingsDelta = -8000,
                    wellbeingDelta = -20,
                    educationalFeedback = "Usar plata familiar por error pensando 'la duplico y la devuelvo' es una de las causas más graves de crisis en hogares."
                ),
                timeoutPenalty = 4000,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Visibilizar el riesgo de apostar dinero familiar o destinado a estudios bajo la ilusión de 'ganar y devolver'."
            ),

            // Miércoles: La plata de la SUBE y el mito de la racha (Idea de Evelyn Gaitán)
            GameScenario(
                dayIndex = 2,
                dayName = "Miércoles",
                title = "El saldo de la SUBE y la falsa racha",
                messages = listOf(
                    ChatMessage(
                        sender = "Joaco",
                        message = "¡Recién saqué $2.000 en los slots virtuales! 🎰 El algoritmo está pagando todo hoy.",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Sistema SUBE",
                        message = "Recordatorio: tenés que separar $2.500 para cargar el transporte de toda la semana.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Joaco",
                        message = "¡Meté la plata de la SUBE antes de que se enfríe la máquina, $name! Con un tiro tenés viaje VIP todo el mes.",
                        delayAfterMillis = 1400L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Asegurar la carga de la SUBE para toda la semana",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = -2500,
                    wellbeingDelta = 12,
                    educationalFeedback = "Priorizaste tus necesidades reales de transporte sin caer en el cuento de la 'máquina caliente'."
                ),
                alternativeOption = DecisionOption(
                    text = "Explicarle a Joaco que las rachas son un engaño del algoritmo",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 15,
                    educationalFeedback = "Concientizar a un amigo ayuda a cortar el contagio grupal."
                ),
                riskyOption = DecisionOption(
                    text = "Timbear la plata de la SUBE en la supuesta racha",
                    isSafe = false,
                    penaltyAmount = 4500,
                    savingsDelta = -2500,
                    wellbeingDelta = -15,
                    educationalFeedback = "Perdiste la plata del transporte y tuviste que caminar toda la semana. Las máquinas no tienen 'rachas calientes'."
                ),
                timeoutPenalty = 3000,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Cuestionar el mito de la racha y proteger el dinero de gastos esenciales como el transporte."
            ),

            // Jueves: Préstamo inmediato e Influencer vendehumo (Idea de Lucía Pobeda + Sensor)
            GameScenario(
                dayIndex = 3,
                dayName = "Jueves",
                title = "Influencers, préstamos mágicos y spam",
                messages = listOf(
                    ChatMessage(
                        sender = "Influencer Fitness VIP (Publicidad)",
                        message = "💪 ¿Querés mi estilo de vida? Empecé con $2.000 en B-Play VIP. El primer tiro tiene ganancia asegurada.",
                        delayAfterMillis = 1500L,
                        isForwarded = true
                    ),
                    ChatMessage(
                        sender = "Notificación Pop-up",
                        message = "⚡ ¡PRÉSTAMO INMEDIATO DE $20.000 APROBADO! Sin recibo, con un solo toque de huella digital.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Lucas",
                        message = "Amigo, te dan $20.000 con un clic y ni se siente el gasto. ¿Entramos?",
                        delayAfterMillis = 1400L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Rechazar el préstamo y bloquear al influencer",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 15,
                    achievementId = AchievementsCatalog.ID_EVITANDO_TENTACION,
                    educationalFeedback = "Los influencers cobran por mostrar ganancias falsas con cuentas demo configuradas para ganar."
                ),
                alternativeOption = DecisionOption(
                    text = "Avisar en el grupo que los préstamos fáciles cobran intereses usureros",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 18,
                    educationalFeedback = "Desarmaste dos trampas juntas: la publicidad encubierta y el endeudamiento instantáneo."
                ),
                riskyOption = DecisionOption(
                    text = "Aceptar el préstamo de $20.000 y descargar la app",
                    isSafe = false,
                    penaltyAmount = 10000,
                    savingsDelta = -4000,
                    wellbeingDelta = -18,
                    educationalFeedback = "Los créditos instantáneos y billeteras sin fricción buscan que te endeudes sin sentir que estás gastando plata real."
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
                title = "Premio por tus notas vs. Recuperar pérdidas",
                messages = listOf(
                    ChatMessage(
                        sender = "Papá",
                        message = "¡Felicitaciones por el 9 en el examen $name! Te transferí $10.000 de premio para que los disfrutes o ahorres.",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Santi",
                        message = "¡Justo $name! El cajero de WhatsApp además nos fía $5.000. Si metemos tu premio recuperamos todo lo de la semana.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Santi",
                        message = "No te quedes con la espina, ¡con un pleno damos vuelta la semana!",
                        delayAfterMillis = 1400L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Sumar los $10.000 del premio a mis ahorros",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 10000,
                    wellbeingDelta = 12,
                    educationalFeedback = "¡Excelente! El premio a tu esfuerzo quedó resguardado en tus ahorros reales."
                ),
                alternativeOption = DecisionOption(
                    text = "Ir al cine con amigos y ahorrar el resto",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 6500,
                    wellbeingDelta = 18,
                    achievementId = AchievementsCatalog.ID_VIDA_REAL,
                    educationalFeedback = "Disfrutaste tu mérito en una salida real con amigos y además sumaste ahorro."
                ),
                riskyOption = DecisionOption(
                    text = "Apostar los $10.000 del premio + $5.000 fiados al cajero",
                    isSafe = false,
                    penaltyAmount = 8000,
                    savingsDelta = -5000,
                    wellbeingDelta = -15,
                    educationalFeedback = "Intentar 'recuperar lo perdido' (chasing losses) con plata fiada multiplica la deuda rápidamente."
                ),
                timeoutPenalty = 4000,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Valorar el dinero ganado con mérito propio y frenar la persecución compulsiva de pérdidas."
            ),

            // Sábado: El Superclásico del Fin de Semana (Idea de Juan Chaile + Roberto)
            GameScenario(
                dayIndex = 5,
                dayName = "Sábado",
                title = "Fiebre de Superclásico y capturas en redes",
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
                        message = "¡Hoy se juega el Superclásico! Mirá la captura que subieron al grupo, están todos haciendo combinadas de goles y tarjetas.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Mati",
                        message = "¿Vas a ser el único que lo mire sin ponerle $5.000 de picante al partido?",
                        delayAfterMillis = 1400L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Recordarle a Mati que en el grupo solo suben capturas cuando ganan",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 16,
                    educationalFeedback = "Le abriste los ojos al grupo: en redes nadie publica los cientos de tickets perdidos."
                ),
                alternativeOption = DecisionOption(
                    text = "Mirar el Superclásico tranquilo sin mezclar fútbol con plata",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 12,
                    educationalFeedback = "Disfrutar del deporte sin timba te protege de la ansiedad y el FOMO."
                ),
                riskyOption = DecisionOption(
                    text = "Meter $5.000 a la combinada del Superclásico por presión",
                    isSafe = false,
                    penaltyAmount = 5000,
                    savingsDelta = -5000,
                    wellbeingDelta = -12,
                    educationalFeedback = "Las apuestas combinadas en partidos clásicos son las que mayor margen de ganancia le dejan a la casa."
                ),
                timeoutPenalty = 3500,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Desarmar la presión social (FOMO) durante grandes eventos deportivos como el Superclásico."
            ),

            // Domingo: Extorsión del cajero ilegal y cierre de semana
            GameScenario(
                dayIndex = 6,
                dayName = "Domingo",
                title = "Amenaza del cajero ilegal y cierre",
                messages = listOf(
                    ChatMessage(
                        sender = "Cajero VIP Nocturno",
                        message = "Último aviso $name: tenés un cargo administrativo pendiente. Si transferís $4.000 ya te cierro la carpeta.",
                        delayAfterMillis = 1500L
                    ),
                    ChatMessage(
                        sender = "Cajero VIP Nocturno",
                        message = "Si no pagás antes de que termine el contador, le mandamos mensaje a tus contactos del colegio.",
                        delayAfterMillis = 1600L
                    ),
                    ChatMessage(
                        sender = "Cajero VIP Nocturno",
                        message = "Decidí ya.",
                        delayAfterMillis = 1300L
                    )
                ),
                safeOption = DecisionOption(
                    text = "No pagar, bloquear y hablar con mi familia o la Línea 141",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 20,
                    educationalFeedback = "Ante una extorsión de una red ilegal de juego, jamás hay que transferir: hablar con tu familia o el 141 corta el chantaje."
                ),
                alternativeOption = DecisionOption(
                    text = "Guardar capturas de prueba y pedir orientación en Boti o la escuela",
                    isSafe = true,
                    penaltyAmount = 0,
                    savingsDelta = 0,
                    wellbeingDelta = 16,
                    educationalFeedback = "Buscar ayuda temprana en la escuela o canales oficiales desactiva cualquier amenaza."
                ),
                riskyOption = DecisionOption(
                    text = "Transferir $4.000 en secreto por miedo",
                    isSafe = false,
                    penaltyAmount = 4000,
                    savingsDelta = -4000,
                    wellbeingDelta = -20,
                    educationalFeedback = "Pagar por miedo nunca frena a un cajero ilegal; solo alimenta nuevas exigencias."
                ),
                timeoutPenalty = 3000,
                decisionTimeSeconds = 25,
                pedagogicalFocus = "Promover el pedido de ayuda a adultos y líneas oficiales (141 / Boti) frente al hostigamiento de cajeros ilegales."
            )
        )
    }
}
