# Estado de implementación — Fase 2a

## Alcance implementado

Se añadió `:data:local` como adaptador Android Room utilizado por `:app`. La base
versión 1 contiene exclusivamente `user_profiles`, `nutrition_plan_versions`,
`weight_measurements`, `food_products`, `food_entries` y
`nutrition_diary_days`. No existen tablas ni componentes de fases posteriores.

## Representación y relaciones

Los planes, pesos, entradas y días de diario referencian el perfil local; las
entradas también referencian su producto. Las relaciones tienen claves foráneas
y solo se indexan las consultas básicas actuales. Planes y observaciones usan
inserción estricta; perfil, producto y estado diario admiten guardado explícito.

`CivilDay` se almacena como el entero `epochDay` de `LocalDate`, separado de los
instantes UTC en milisegundos epoch. Esto permite un round-trip determinista sin
convertir el día civil en un instante. Las cantidades se guardan como un entero
escalado y un discriminador cerrado (`MASS_MG`, `VOLUME_UL`,
`UNITS_THOUSANDTHS` o `PORTIONS_THOUSANDTHS`); no existe conversión entre masa y
volumen.

Las magnitudes opcionales usan columnas `NULL`. Un valor desconocido permanece
`NULL`, mientras que energía o nutrientes explícitamente iguales a cero se
guardan como `0`. Los snapshots nutricionales de cada entrada están en columnas
enteras propias y no se reconstruyen desde cambios posteriores del producto.

## Decisiones concretas

Room 2.7.2 y KSP procesan entidades separadas del dominio. La base inicial tiene
versión 1 y no inventa migraciones históricas. Los seis estados de cierre se
persisten por nombre y un día sin fila continúa siendo ausencia, no
`ZERO_INTAKE_CONFIRMED`. Los DAOs son específicos y solo exponen inserción,
lectura/listado básico y las modificaciones admitidas por la vertical actual.

## Decisiones postergadas

Se postergan Fase 2b, favoritos, comidas guardadas, fuentes externas, tendencias,
TDEE, evaluación de planes, readiness, sincronización, red, auditoría avanzada,
migraciones posteriores y UI funcional. No se añadieron placeholders para esas
capacidades.

## Verificación

Comandos de certificación:

```bash
gradle :core:domain:test --no-daemon
gradle :data:local:testDebugUnitTest --no-daemon
gradle assemble --no-daemon
gradle lint --no-daemon
```

La ejecución local conjunta quedó limitada porque este contenedor no tiene
Android SDK (`SDK location not found`). La certificación final depende del
resultado real de GitHub Actions; no se declaran verdes anticipadamente los
tests Room, `assemble` ni `lint`. No hay desviaciones funcionales deliberadas de
v1.1. La especificación disponible está en la raíz del repositorio, aunque el
encargo indique una ruta bajo `docs/`.
