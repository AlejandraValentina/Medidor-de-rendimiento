# Fase 3c — PlanEvaluator y memoria de decisión

## Alcance

- `PlanEvaluator` JVM puro separa candidato, estado efectivo, estado operativo, autorización y safety.
- `PlanEvaluatorPolicy` versiona exclusivamente los umbrales LOSS de entrada (±200 g/sem), salida (±100 g/sem), dos confirmaciones, separación mínima de dos días y cooldown de 14 días desde `validFrom`.
- `DecisionStateMemory` conserva racha direccional por versión de plan y política; cambio de dirección, plan, riesgo o evidencia duplicada reinician o bloquean según corresponda.
- Safety es un gate categórico explícito. La ausencia no se convierte en `CLEAR`.
- La histéresis conserva una dirección ya confirmada dentro de la banda de salida (más de +100 g/sem para `ADJUST_DOWN`, menos de -100 g/sem para `ADJUST_UP`) y la abandona inmediatamente al alcanzar el umbral de salida.

La política direccional inicial se limita a `LOSS`, única semántica con umbrales de entrada/salida definidos en v1.1. Otros objetivos producen `OBSERVE` con razón estructurada hasta disponer de política propia validada.

## Persistencia

Room pasa de versión 3 a 4 mediante `MIGRATION_3_4`, no destructiva. Se añaden únicamente `plan_evaluations` y `decision_state_memory`. Las evaluaciones conservan revisiones por fecha y una traza embebida mínima de ventana, peso, TDEE, calidad, estabilidad, modo y versiones; la memoria activa puede reconstruirse desde la revisión vigente y calificada de cada día.

No existen `adjustment_proposals`, `AlgorithmRun`, cola de recálculo ni auditoría genérica.

## Operación

Fase 3c ejecuta exclusivamente `EvaluationMode.SHADOW`: conserva candidato y conclusión efectiva interna, pero su decisión operacional es ausente, nunca autoriza una propuesta y el panel la identifica como “EVALUACIÓN EN VALIDACIÓN”. No modifica `NutritionPlanVersion`, no calcula tamaños de ajuste y no cambia `recommendedToday`; BASE_ONLY continúa vigente.

La activación de `ADVISORY`, `AdjustmentProposal`, aceptación/rechazo y el replay/inspector de 28 días quedan postergados. Los escenarios PE que dependen de readiness, carga de entrenamiento, floors energéticos o propuestas, HY-09 y la transición SHADOW→ADVISORY quedan diferidos porque sus contratos todavía no están materializados.
