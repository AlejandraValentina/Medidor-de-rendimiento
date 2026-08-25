# Fase 3.5 — tooling de validación SHADOW

## Alcance

Esta entrega incorpora herramientas locales para acumular, inspeccionar y reproducir evaluaciones SHADOW. No constituye validación personal: los fixtures no sustituyen 28 días personales, 14 días prospectivos ni revisión humana.

`ShadowValidationPolicy` versiona únicamente los mínimos normativos: 28 días evaluables, 14 prospectivos, 8 días de peso, 850000 ppm de cobertura nutricional elegible y 7 fechas TDEE estables. El reporte usa estados categóricos, nunca un score agregado.

## Evaluación prospectiva

El inspector exige seleccionar explícitamente `CLEAR`, `CAUTION` o `REVIEW_REQUIRED`; no existe default ni se infiere `CLEAR`. “Evaluar hoy en SHADOW” usa el plan, WeightTrend, TDEE y estabilidad vigentes, deriva `inputRevision` desde sus revisiones y persiste mediante el flujo idempotente existente. La fila queda marcada `prospectiveObserved=true` solamente por este flujo ejecutado en su fecha civil. Misma fecha/evidence/revisión no crea otra fila; una revisión retrospectiva conserva la procedencia prospectiva original.

SHADOW mantiene `operational=false`, `operationalDecision=null`, el plan intacto y BASE_ONLY. No existen aceptación, propuesta ni activación ADVISORY.

## Criterios

El analizador calcula bajo demanda desde revisiones vigentes y segmenta por `NutritionPlanVersion`. Selecciona las últimas 28 fechas evaluables y calcula peso, span, gap y nutrición desde mediciones y días reales dentro de esa misma ventana; no reutiliza los totales del último snapshot TDEE. La energía estimada usa el límite versionado en `TdeePolicy`. Los candidatos direccionales y la aprobación final permanecen `HUMAN_REVIEW_REQUIRED` porque requieren juicio humano.

Outlier, día incompleto y corrección retrospectiva solo satisfacen el criterio cuando su comportamiento fue verificado; su mera presencia no basta. Pueden verificarse con replay/fixtures, pero no cuentan como días personales prospectivos. Un cambio de plan inicia una ventana separada.

## Replay

`ShadowReplayEngine` es JVM puro, ordena cronológicamente, usa solo la última revisión de cada fecha y compara tanto outputs como el contenido semántico de `DecisionStateMemory`; su contador interno de revisión no define equivalencia. El reporte acota el replay al plan y ventana inspeccionados, por lo que una fila legacy ajena no bloquea el plan vigente. El replay no persiste ni modifica memoria productiva. Una fila legacy relevante sin `estimatorStabilityPolicyVersion` produce input incompleto.

## Persistencia

Room pasa de 4 a 5 mediante `MIGRATION_4_5`, que añade solamente las columnas nullable `plan_evaluations.estimatorStabilityPolicyVersion` y `prospectiveObserved`. Los null conservan honestamente filas legacy; no se infiere procedencia ni policy. No se crean tablas.

La corrección retrospectiva está demostrada en dominio/store mediante selección de última revisión, identificación de ventanas TDEE afectadas y reconstrucción de memoria. La recomputación completa originada por edición histórica de `FoodEntry` queda limitada hasta que exista esa vertical de edición; no se declara FR-043 cerrada por una ruta inexistente y no se crea cola de recálculo.

## Validación real pendiente

Durante las próximas semanas deben acumularse datos personales reales, revisarse candidatos hipotéticos, alternancias, outliers, días incompletos y correcciones. Aunque todos los criterios técnicos queden listos, ADVISORY requiere revisión humana y una activación explícita posterior.
