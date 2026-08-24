# Estado de implementación — Fase 2b

## Alcance

La vertical offline incorpora favoritos, recientes y comidas guardadas sobre los
módulos existentes `:app`, `:core:domain` y `:data:local`. Favoritos conservan
producto y cantidad habitual; una acción crea una entrada normal. Recientes se
deriva de `food_entries`, sin tabla ni puntuación, ordenando productos distintos
por su último consumo real. Las comidas guardadas son plantillas nombradas con
items ordenados y cantidades editables al volver a registrarlas.

## Persistencia y migración

Room pasa de versión 1 a 2 mediante `MIGRATION_1_2`, sin migración destructiva.
La migración conserva las seis tablas de Fase 2a y añade únicamente
`favorite_foods`, `saved_meals` y `saved_meal_items`. Las claves foráneas no
permiten que quitar un favorito elimine un producto, ni que eliminar una comida
afecte `food_entries`; los items de una plantilla sí se eliminan con ella.

Cada cantidad usa el entero escalado y discriminador existente (`MASS_MG`,
`VOLUME_UL`, `UNITS_THOUSANDTHS`, `PORTIONS_THOUSANDTHS`). No hay conversión
masa-volumen. Los nutrientes desconocidos permanecen `NULL` y un cero explícito
permanece cero.

## Historial y registro rápido

Al usar un favorito, reciente o comida guardada se crean `FoodEntry` nuevas. La
nutrición se calcula con la ficha vigente y queda copiada en el snapshot de cada
entrada; modificar después el producto o la plantilla no cambia el historial.
BASE_ONLY permanece intacto: `recommendedToday` sigue siendo la energía base del
plan.

## Verificación

Comandos de certificación:

```bash
gradle :core:domain:test --no-daemon
gradle :data:local:testDebugUnitTest --no-daemon
gradle :app:testDebugUnitTest --no-daemon
gradle assemble --no-daemon
gradle lint --no-daemon
```

Los tests agregados cubren favoritos, recientes distintos y deterministas,
plantillas con las cuatro unidades, preservación del historial y SQL de migración
v1→v2. Las verificaciones locales con JDK 17, Gradle 8.14.4 y Android SDK 35 finalizaron correctamente; la certificación normativa con Gradle 8.11.1 depende del resultado real de GitHub Actions.

## Postergado

No se añadieron tabla de recientes, porciones genéricas, grupos de comida,
tendencias, TDEE, evaluación de planes, Health Connect, Garmin, red, importación,
recomendaciones predictivas ni componentes de Fase 3.
