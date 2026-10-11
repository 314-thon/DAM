package com.ifts18.unadecisionmas.data.repository

import com.ifts18.unadecisionmas.data.model.ChatMessage
import com.ifts18.unadecisionmas.data.model.DecisionOption
import com.ifts18.unadecisionmas.data.model.GameScenario

object GameScenariosRepository {

    fun getScenarios(playerName: String): List<GameScenario> {
        val name = playerName.ifBlank { "Mateo" }
        return listOf(
            // Lunes
            GameScenario(
                dayIndex = 0,
                dayName = "Lunes",
                title = "La carnada inicial",
                messages = listOf(
                    ChatMessage(
                        sender = "Lucas",
                        message = "¡Che $name! Me pasaron una fija imperdible para el partido de hoy 🔥",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Lucas",
                        message = "¿Ponés $1.000 ahora y nos llevamos $2.000 cada uno al toque?",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Lucas",
                        message = "¡Confirmame ya que en 10 segundos cierran la apuesta y se cae la cuota!",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "No transferir",
                    isSafe = true,
                    penaltyAmount = 0,
                    educationalFeedback = "Supiste identificar la urgencia ficticia y protegiste tu dinero."
                ),
                riskyOption = DecisionOption(
                    text = "Apostar $1.000",
                    isSafe = false,
                    penaltyAmount = 1000,
                    educationalFeedback = "La promesa de duplicar el dinero en minutos es el gancho inicial más común."
                ),
                timeoutPenalty = 1000,
                pedagogicalFocus = "Desactivar el atractivo de recompensas inmediatas y la falsa urgencia."
            ),

            // Martes
            GameScenario(
                dayIndex = 1,
                dayName = "Martes",
                title = "La ilusión de control",
                messages = listOf(
                    ChatMessage(
                        sender = "Nico (Compañero)",
                        message = "¡$name, juega el puntero invicto contra los suplentes del último!",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Nico (Compañero)",
                        message = "Es victoria segura. Te presto $2.000 de mi billetera y después me devolvés.",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Nico (Compañero)",
                        message = "Sabemos de fútbol, ¡es imposible que perdamos esta!",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Rechazar apuesta",
                    isSafe = true,
                    penaltyAmount = 0,
                    educationalFeedback = "Entendiste que saber de fútbol no elimina el azar ni la ventaja matemática de la casa."
                ),
                riskyOption = DecisionOption(
                    text = "Apostar $2.000 prestados",
                    isSafe = false,
                    penaltyAmount = 2000,
                    educationalFeedback = "Apostar dinero prestado genera deudas con amigos e incrementa la presión."
                ),
                timeoutPenalty = 2000,
                pedagogicalFocus = "Desmitificar la creencia de que el conocimiento deportivo asegura ganar apuestas."
            ),

            // Miércoles
            GameScenario(
                dayIndex = 2,
                dayName = "Miércoles",
                title = "La falacia de la racha",
                messages = listOf(
                    ChatMessage(
                        sender = "Joaco",
                        message = "¡Recién ganamos $1.000 de pura suerte en la ruleta virtual! 🎰",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Joaco",
                        message = "El algoritmo está pagando hoy. Si le metemos $3.000 más nos salvamos la semana.",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Joaco",
                        message = "¿Le ponemos fichas antes de que se enfríe?",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Retirarse y frenar",
                    isSafe = true,
                    penaltyAmount = 0,
                    educationalFeedback = "Excelente: una victoria casual no garantiza futuros aciertos."
                ),
                riskyOption = DecisionOption(
                    text = "Cargar $3.000 más",
                    isSafe = false,
                    penaltyAmount = 3000,
                    educationalFeedback = "Creer en 'la buena racha' hace que los jugadores pierdan todo lo ganado y más."
                ),
                timeoutPenalty = 3000,
                pedagogicalFocus = "Cuestionar la falacia del jugador y el sesgo de confirmación."
            ),

            // Jueves
            GameScenario(
                dayIndex = 3,
                dayName = "Jueves",
                title = "Fricción del dinero digital",
                messages = listOf(
                    ChatMessage(
                        sender = "Grupo del Curso",
                        message = "Chicos, en la app parece que no gastás nada, son solo numeritos en pantalla...",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Lucas",
                        message = "Total son unos toques con la huella digital. Pasame $5.000 al casino ya.",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Lucas",
                        message = "¡Ni se siente el gasto, dale $name!",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Dinero virtual es real",
                    isSafe = true,
                    penaltyAmount = 0,
                    educationalFeedback = "Reconociste que el saldo digital representa plata y esfuerzo real."
                ),
                riskyOption = DecisionOption(
                    text = "Transferir $5.000 sin pensar",
                    isSafe = false,
                    penaltyAmount = 5000,
                    educationalFeedback = "Las plataformas diseñan interfaces rápidas para invisibilizar el valor real del dinero."
                ),
                timeoutPenalty = 5000,
                pedagogicalFocus = "Evidenciar la pérdida de noción del gasto provocada por billeteras virtuales."
            ),

            // Viernes
            GameScenario(
                dayIndex = 4,
                dayName = "Viernes",
                title = "Perseguir las pérdidas",
                messages = listOf(
                    ChatMessage(
                        sender = "Santi",
                        message = "Uh $name, perdiste plata esta semana... pero tengo la solución.",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Santi",
                        message = "Un cajero nos presta $4.000 al instante. Recuperamos lo perdido y lo devolvemos.",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Santi",
                        message = "No te quedes con la espina clavada, ¡hay que recuperar ya!",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Asumir pérdida y parar",
                    isSafe = true,
                    penaltyAmount = 0,
                    educationalFeedback = "Frenar a tiempo es la mejor decisión para evitar una espiral de endeudamiento."
                ),
                riskyOption = DecisionOption(
                    text = "Pedir $4.000 de crédito",
                    isSafe = false,
                    penaltyAmount = 4000,
                    educationalFeedback = "Intentar 'recuperar lo perdido' (chasing losses) es la trampa que genera deudas graves."
                ),
                timeoutPenalty = 4000,
                pedagogicalFocus = "Desarticular la conducta compulsiva de perseguir pérdidas acumuladas."
            ),

            // Sábado
            GameScenario(
                dayIndex = 5,
                dayName = "Sábado",
                title = "Presión de redes y desinformación",
                messages = listOf(
                    ChatMessage(
                        sender = "Historias / Estados",
                        message = "Captura de pantalla: '¡Gané $10.000 acostado desde mi cama! 🤑💸'",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Mati",
                        message = "Todos los pibes están subiendo capturas ganando. ¿Vas a ser el único que se quede afuera?",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Mati",
                        message = "Cargale $3.000 al cajero de WhatsApp y subite a la movida.",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Solo muestran cuando ganan",
                    isSafe = true,
                    penaltyAmount = 0,
                    educationalFeedback = "Identificaste que las redes muestran una ilusión y ocultan las pérdidas masivas."
                ),
                riskyOption = DecisionOption(
                    text = "Cargar $3.000 por presión",
                    isSafe = false,
                    penaltyAmount = 3000,
                    educationalFeedback = "Apostar por presión social o FOMO conduce a decisiones impulsivas y costosas."
                ),
                timeoutPenalty = 3000,
                pedagogicalFocus = "Combatir la presión de pares y la narrativa engañosa en redes sociales."
            ),

            // Domingo
            GameScenario(
                dayIndex = 6,
                dayName = "Domingo",
                title = "Extorsión y cierre de semana",
                messages = listOf(
                    ChatMessage(
                        sender = "Cajero Virtual",
                        message = "Último aviso: si transferís $2.000 ahora, te congelo los recargos e intereses.",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Cajero Virtual",
                        message = "Si no pagás en 10 segundos, la deuda pasa a cobro externo.",
                        delayAfterMillis = 2000L
                    ),
                    ChatMessage(
                        sender = "Cajero Virtual",
                        message = "Respondé ya o se complica.",
                        delayAfterMillis = 1500L
                    )
                ),
                safeOption = DecisionOption(
                    text = "Silenciar y pedir ayuda",
                    isSafe = true,
                    penaltyAmount = 0,
                    educationalFeedback = "Frente a amenazas o extorsiones, lo fundamental es no pagar y hablar con un adulto o llamar al 141."
                ),
                riskyOption = DecisionOption(
                    text = "Transferir $2.000 por miedo",
                    isSafe = false,
                    penaltyAmount = 2000,
                    educationalFeedback = "Pagar ante la extorsión no soluciona el problema; pedir ayuda profesional sí."
                ),
                timeoutPenalty = 2000,
                pedagogicalFocus = "Fomentar la búsqueda de ayuda y el corte de contacto ante extorsiones digitales."
            )
        )
    }
}
