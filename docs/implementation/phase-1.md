# Estado de implementación — Fase 1

## Alcance implementado

- Proyecto Gradle con solo los módulos `app` y `core:domain`.
- Esqueleto Android mínimo, sin UI funcional ni librerías AndroidX. El respaldo
  automático está desactivado mientras no exista una estrategia explícita para
  datos personales.
- Dominio Kotlin/JVM puro para identidad local, magnitudes, cantidades de
  alimentos, fecha civil, reloj inyectable, plan nutricional, pesaje manual,
  producto y consumo manual.
- Tests JVM deterministas de invariantes, unidades, aritmética entera, tiempo y
  diferencia entre ausencia y cero.

## Decisiones concretas

- Kotlin 2.0.21, JVM 17, Android `minSdk 28`, `compileSdk`/`targetSdk 35` y
  namespace `com.medidorderendimiento` constituyen la base inicial.
- Masa corporal usa gramos; energía usa milicalorías; nutrientes usan
  miligramos; volumen y cantidades de volumen usan microlitros. Unidad y porción
  usan milésimas para permitir fracciones sin `Double` canónico.
- `BodyMass` exige un valor positivo. Energía, nutriente y volumen aceptan cero
  y rechazan negativos. La ausencia pertenece al campo que contiene el value
  object mediante nulabilidad; no existe un valor centinela.
- Las cantidades consumidas son variantes selladas distintas para masa,
  volumen, unidad y porción. No existe operación masa-volumen.
- El ritmo semanal es una magnitud positiva cuya dirección procede del objetivo
  `LOSS` o `GAIN`; los demás objetivos no admiten ritmo. La proteína permanece
  opcional porque la especificación no obliga a inventar una regla adicional.
- Toda versión de plan incluye vigencia y aceptación explícita. Ninguna regla
  modifica automáticamente el plan.
- `CivilDay` representa exclusivamente una fecha de calendario. La política de
  zona/offset se añadirá con las observaciones persistidas que realmente la
  necesiten, sin confundirla con un instante UTC.

## Decisiones postergadas

Se postergan para Fase 2 o posteriores: persistencia Room, repositorios y casos
de uso, revisiones persistidas, cierres de diario, porciones vinculadas a bases
nutricionales, densidad, agregación, Compose y panel funcional. También quedan
fuera motores, tendencias, TDEE, evaluación de planes, propuestas, readiness,
Health Connect, Garmin/FIT, WorkManager, red, fotografía y auditoría avanzada.
No se crearon módulos, interfaces vacías ni `TODO` para esas capacidades.

## Compilación y tests

Comandos previstos:

```bash
./gradlew assemble
./gradlew :core:domain:test
```

El conjunto exportable desde Codex web contiene únicamente archivos de texto.
Por ello, `gradle/wrapper/gradle-wrapper.jar` se excluye deliberadamente del
repositorio. Antes de certificar el build, debe regenerarse en un entorno local
con Gradle o Android Studio y, a continuación, deben ejecutarse los comandos
anteriores. Los scripts textuales `gradlew` y `gradlew.bat`, junto con
`gradle-wrapper.properties`, se conservan como configuración del wrapper.

En el entorno de implementación, la resolución inicial de plugins Gradle y la
instalación del Android SDK quedaron impedidas por el proxy de red (`HTTP 403`),
por lo que esos dos comandos no pudieron completarse allí. Como comprobación
local independiente de Android, todo el código principal de `core:domain` se
compiló con el compilador Kotlin 2.0.21 incluido en Gradle 8.14.4. Los tests
quedan configurados para ejecutarse con Kotlin Test/JUnit Platform en cuanto
Gradle pueda resolver sus artefactos.

## Desviaciones

La especificación fue suministrada en la raíz como
`Medidor_de_rendimiento_Especificacion_v1.1.md`, no en la ruta `docs/` indicada
en el encargo. No hay desviaciones funcionales deliberadas respecto de la Fase
1. La imposibilidad ambiental de resolver plugins/SDK impide certificar en este
entorno el ensamblado Android y la ejecución Gradle de los tests.
