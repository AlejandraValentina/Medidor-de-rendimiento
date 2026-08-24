# Fase 3b — TDEE observacional y estabilidad

## Alcance

- `TdeeEstimator` calcula bajo demanda desde ingesta real elegible y el `WeightTrend` robusto existente.
- `NutritionQualityCalculator` mantiene separados cierre, proporción estimada, pendientes, energía desconocida, exclusiones y mezcla de planes.
- `EstimatorStabilityCalculator` evalúa únicamente revisiones vigentes, fechas civiles distintas y evidencia nueva.
- El panel muestra valor central, madurez, calidad cualitativa, evidencia, estabilidad y ventana. No propone cambios del plan.

El modelo inicial usa `k = 7700 kcal/kg` dentro de `tdee-v1`. Es una heurística de ingeniería versionada, no una constante biológica universal. El cálculo usa enteros canónicos y `BigDecimal` con redondeo explícito; el plan base nunca se usa como ingesta observada.

## Elegibilidad y contemporaneidad

Solo son elegibles días `CLOSED_CONFIRMED` o `CLOSED_WITH_ESTIMATES` bajo el umbral versionado. Los días abiertos, incompletos, excluidos y de ingesta cero confirmada no se incorporan silenciosamente. Pendientes o energías desconocidas limitan la calidad. Cuando hay más de una versión de plan, se utiliza determinísticamente el segmento homogéneo más reciente y se expone `MIXED_PLAN_VERSIONS`.

Los rangos bajo/alto permanecen ausentes porque todavía no existe una construcción defendible de incertidumbre conjunta. No se inventa un margen fijo.

## Persistencia

Room pasa de versión 2 a 3 mediante `MIGRATION_2_3`, no destructiva. La única tabla nueva es `tdee_estimates`. Conserva energía en milicalorías enteras, ratios en partes por millón, ventana, versiones, revisión de inputs, clave proporcional de evidencia y revisiones por fecha. Para estabilidad cuenta únicamente la revisión vigente de cada día.

Una edición retrospectiva puede localizar bajo demanda las estimaciones cuya ventana incluye el día corregido, generar revisiones y recalcular estabilidad desde el primer resultado afectado. No existe cola durable ni `AlgorithmRun`.

## stability-v1

La política conserva los umbrales normativos: 7 fechas mínimas, 10 fechas sobre 14 días para estabilidad, MAD relativo 0,025, amplitud 0,05, deriva 0,04, inversiones 0,035 y amplitud crítica temprana 0,06. Calidad de inputs y estabilidad siguen siendo dimensiones independientes.

## Verificación

```bash
gradle :core:domain:test --no-daemon --console=plain
ANDROID_HOME=/tmp/android-sdk gradle :data:local:testDebugUnitTest --no-daemon --console=plain
ANDROID_HOME=/tmp/android-sdk gradle :app:testDebugUnitTest --no-daemon --console=plain
ANDROID_HOME=/tmp/android-sdk gradle assemble lint --no-daemon --console=plain
```

Los tests cubren TD-01–TD-08, ES-01–ES-10, migración 2→3, revisiones vigentes y AR-05. Permanecen postergados PlanEvaluator, SHADOW, propuestas, readiness, Health Connect, Garmin y toda infraestructura de Fase 3c o posterior.
