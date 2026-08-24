# Fase 3c — PlanEvaluator y memoria de decisión

## Alcance

- `PlanEvaluator` JVM puro separa candidato, estado efectivo, estado operativo, autorización y safety.
- `PlanEvaluatorPolicy` versiona exclusivamente los umbrales LOSS de entrada (±200 g/sem), salida (±100 g/sem), dos confirmaciones y separación mínima de dos días.
- `DecisionStateMemory` conserva racha direccional por versión de plan y política; cambio de dirección, plan, riesgo o evidencia duplicada reinician o bloquean según corresponda.
- Safety es un gate categórico explícito. La ausencia no se convierte en `CLEAR`.

La política direccional inicial se limita a `LOSS`, única semántica con umbrales de entrada/salida definidos en v1.1. Otros objetivos producen `OBSERVE` con razón estructurada hasta disponer de política propia validada.

## Persistencia

Room pasa de versión 3 a 4 mediante `MIGRATION_3_4`, no destructiva. Se añaden únicamente `plan_evaluations` y `decision_state_memory`. Las evaluaciones conservan revisiones por fecha; la memoria activa puede reconstruirse desde la revisión vigente de cada día.

No existen `adjustment_proposals`, `AlgorithmRun`, cola de recálculo ni auditoría genérica.

## Operación

Fase 3c clasifica evidencia, pero no modifica `NutritionPlanVersion`, no calcula tamaños de ajuste y no cambia `recommendedToday`. BASE_ONLY continúa vigente. SHADOW operativo, ADVISORY, propuestas y aceptación quedan postergados.
