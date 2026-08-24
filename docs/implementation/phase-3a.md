# Fase 3a — Peso y tendencia

## Alcance implementado

- `WeightTrendCalculator` JVM puro, calculado bajo demanda desde `WeightMeasurement` reales.
- Ajuste robusto Theil–Sen dentro de una ventana configurable de 21 a 35 días (28 por defecto).
- Selección diaria trazable cuando existen varios pesajes, priorizando condiciones habituales y usando instante e identificador como desempate determinista.
- Detección prudente de candidatos aislados mediante mediana local y MAD; los registros originales nunca se borran ni modifican.
- Señal de posible cambio de régimen para desplazamientos persistentes.
- Cobertura, separación máxima, variabilidad robusta, razones estructuradas y confianza cualitativa.
- Panel Compose con último peso observado separado de tendencia modelada, ritmo semanal, cobertura y advertencias.

## Disponibilidad

- Menos de cinco días distintos o menos de diez días entre extremos: tendencia no disponible; el último peso real sigue visible.
- Cinco o más días y diez días de cobertura: resultado provisional de confianza baja.
- Cinco a siete días distribuidos y al menos catorce días: confianza moderada.
- Ocho o más días, al menos veintiún días, separación máxima razonable, ruido controlado y sin condiciones inhabituales: confianza alta.

El cambio mensual observado solo se produce cuando hay al menos dos observaciones cerca de cada extremo de una ventana aproximada de treinta días. No se presenta una proyección mensual como cambio observado.

## Persistencia y alcance

No hubo cambios Room: la base permanece en versión 2 y reutiliza `weight_measurements`. No existen snapshots de tendencia, mediciones interpoladas ni migración. BASE_ONLY permanece intacto y la tendencia es únicamente informativa.

Se postergan TDEE, estabilidad del estimador, evaluación de planes, ajustes, readiness, Health Connect y Garmin.

## Verificación

```bash
gradle :core:domain:test --no-daemon --console=plain
ANDROID_HOME=/tmp/android-sdk gradle :data:local:testDebugUnitTest --no-daemon --console=plain
ANDROID_HOME=/tmp/android-sdk gradle :app:testDebugUnitTest --no-daemon --console=plain
ANDROID_HOME=/tmp/android-sdk gradle assemble lint --no-daemon --console=plain
```

Los tests del dominio cubren WT-01–WT-08, AR-04, determinismo, orden de entrada, unidades semanales y ausencia de observaciones fabricadas. Las cuatro verificaciones Gradle indicadas finalizaron con `BUILD SUCCESSFUL` en el entorno de implementación.
