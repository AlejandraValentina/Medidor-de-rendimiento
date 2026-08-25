# Fase 3.5 — tooling de validación SHADOW

## Alcance

Esta entrega incorpora herramientas locales para acumular, inspeccionar y reproducir evaluaciones SHADOW. No constituye validación personal: los fixtures no sustituyen 28 días personales, 14 días prospectivos ni revisión humana.

`ShadowValidationPolicy` versiona únicamente los mínimos normativos: 28 días evaluables, 14 prospectivos, 8 días de peso, 850000 ppm de cobertura nutricional elegible y 7 fechas TDEE estables. El reporte usa estados categóricos, nunca un score agregado.

## Evaluación prospectiva

El inspector exige seleccionar explícitamente `CLEAR`, `CAUTION` o `REVIEW_REQUIRED`; no existe default ni se infiere `CLEAR`. “Evaluar hoy en SHADOW” usa el plan, WeightTrend, TDEE y estabilidad vigentes, deriva `inputRevision` desde el TDEE y persiste mediante el flujo idempotente existente. Misma fecha/evidence/revisión no crea otra fila; evidencia nueva crea una revisión del día.

SHADOW mantiene `operational=false`, `operationalDecision=null`, el plan intacto y BASE_ONLY. No existen aceptación, propuesta ni activación ADVISORY.

## Criterios

El analizador calcula bajo demanda desde revisiones vigentes y segmenta por `NutritionPlanVersion`: ventana personal, operación prospectiva, peso/cobertura nutricional, energía estimada según el límite ya versionado en `TdeePolicy`, TDEE estable, alternancias, consistencia, replay y escenarios obligatorios. Los candidatos direccionales y la aprobación final permanecen `HUMAN_REVIEW_REQUIRED` porque requieren juicio humano.

Outlier, día incompleto y corrección retrospectiva pueden verificarse con replay/fixtures, pero no cuentan como días personales prospectivos. Un cambio de plan inicia una ventana separada.

## Replay

`ShadowReplayEngine` es JVM puro, ordena cronológicamente, usa solo la última revisión de cada fecha y no persiste ni modifica la memoria productiva. Una fila legacy sin `estimatorStabilityPolicyVersion` produce input incompleto: nunca se inventa ni se extrae desde `evidenceKey`.

## Persistencia

Room pasa de 4 a 5 mediante `MIGRATION_4_5`, que añade solamente la columna nullable `plan_evaluations.estimatorStabilityPolicyVersion`. El null conserva honestamente filas legacy; las evaluaciones nuevas guardan la policy real. No se crean tablas.

## Validación real pendiente

Durante las próximas semanas deben acumularse datos personales reales, revisarse candidatos hipotéticos, alternancias, outliers, días incompletos y correcciones. Aunque todos los criterios técnicos queden listos, ADVISORY requiere revisión humana y una activación explícita posterior.
