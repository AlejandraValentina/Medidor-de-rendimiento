# Medidor de rendimiento

## Especificación de producto, dominio y arquitectura Android — v1.1

**Fecha de referencia:** 23 de agosto de 2026.  
**Estado:** revisión contractual para implementación vertical; no constituye implementación ni validación clínica.  
**Versión anterior preservada:** especificación v1.0 del 23 de agosto de 2026.  
**Plataforma:** Android.  
**Código de aplicación:** Kotlin exclusivamente.  
**Dispositivo prioritario:** Garmin vívoactive 6.  
**Propósito:** conservar la arquitectura objetivo y precisar qué se materializa en cada fase antes de escribir código de producción.

---

## 0. Control de revisión v1.0 → v1.1

### 0.1 Alcance y continuidad

Esta versión **no redefine el producto**. Ratifica la tesis central, el dominio Kotlin puro, la separación por capas, Room como fuente local de verdad, la abstracción de proveedores, la distinción entre ausencia y cero, los cinco estados del evaluador, la privacidad local-first, los ajustes conservadores y la aceptación manual de cualquier cambio operativo.

La revisión diferencia de manera normativa dos planos:

1. **Arquitectura objetivo:** capacidad conceptual completa que el sistema debe admitir sin rediseño estructural.
2. **Alcance materializado:** componentes necesarios para cerrar la vertical de implementación actualmente autorizada.

Que una entidad, puerto, módulo o tabla figure en la arquitectura objetivo no autoriza implementarlo anticipadamente. La regla rectora pasa a ser:

> Arquitectura objetivo ≠ todo debe implementarse desde el primer commit.

### 0.2 Evaluación crítica de los diez cambios

| Revisión | Resolución | Contradicción detectada y resolución | Secciones v1.0 afectadas |
| --- | --- | --- | --- |
| RV-01. Evitar sobreingeniería inicial. | **ACEPTADA CON PRECISIÓN.** | La trazabilidad no se elimina: comienza con versiones, ventana, motivos y resumen de entradas; `AlgorithmRun`, `AlgorithmInput`, hashes exhaustivos, cola durable y auditoría avanzada se postergan. La estabilidad e histéresis sí requieren persistencia mínima propia. | 8.2–8.6, 9.9, 10.1–10.8, 16, 17 y 18. |
| RV-02. Readiness categórico inicial. | **ACEPTADA.** | La v1.0 ya admitía puntuación opcional, pero su tabla y panel inducían a implementarla. La categoría `GOOD`, `MODERATE`, `LOW` o `UNKNOWN` constituye ahora el contrato completo de la primera versión; el número queda diferido. | FR-025; 9.7; 11.8; 13.1; RD-01–RD-08; 17.5. |
| RV-03. Confianza visible cualitativa. | **ACEPTADA.** | El índice interno sigue siendo necesario para políticas y tests, pero deja de presentarse como porcentaje o probabilidad en la experiencia ordinaria. Deben mostrarse cobertura y factores limitantes. | FR-012 y FR-019; 9.1; 11.5; 13; 16.2. |
| RV-04. Estabilidad explícita del TDEE. | **ACEPTADA CON PRECISIÓN.** | Buena calidad de entrada no garantiza estabilidad de salida. Para evaluar oscilación se necesita un historial mínimo de estimaciones; esta tabla acotada se materializa cuando se implementa el estimador, sin anticipar un sistema completo de snapshots. | FR-014 y FR-015; 9.8; 10.7; 11.5–11.6; 11.10; 12; 15.4. |
| RV-05. Histéresis del evaluador. | **ACEPTADA CON PRECISIÓN.** | Se requiere memoria persistente mínima por versión de plan. La histéresis nunca mantiene una propuesta cuando aparecen riesgo, baja confianza, estimador inestable, datos corregidos o cambio de plan; las salidas de seguridad son inmediatas. | FR-015 y FR-016; 9.8–9.9; 10.7; 11.10; 12.1–12.3; 15.6. |
| RV-06. Sin compensación intradiaria inicial. | **ACEPTADA.** | La v1.0 situaba compensación moderada en la fase 4. Se conserva como arquitectura objetivo, pero `BASE_ONLY` es el modo inicial obligatorio incluso si hay entrenamiento; `ADAPTIVE` requiere compuertas independientes y activación explícita. | FR-009 y FR-026; 9.8; 11.9; 12.1; 13.1; 17.5; 18.8. |
| RV-07. Shadow Mode formal. | **ACEPTADA CON PRECISIÓN.** | El algoritmo puede producir un candidato `ADJUST_*`, pero durante `SHADOW` no existe propuesta operativa ni acción aceptable. El panel indica evaluación en validación. La fase 3.5 valida el núcleo sin Garmin; nuevas señales incorporadas después requieren validación acotada adicional. | FR-015, FR-016 y FR-019; 9.8; 10.7; 11.10; 12; 15; 16; 17; 18. |
| RV-08. Spikes empíricos de Garmin/Health Connect. | **ACEPTADA CON PRECISIÓN.** | Las verificaciones son criterios obligatorios antes de construir su adaptador o parser específico, pero no bloquean las fases 1–3, que son locales e independientes del reloj. Falta de teléfono o exportación bloquea únicamente la integración correspondiente. | 3; FR-021, FR-022 y FR-027; 14.1; 17.1, 17.5 y 17.6. |
| RV-09. Readiness no define dirección nutricional. | **ACEPTADA.** | Se formaliza una restricción que estaba implícita: recuperación puede contextualizar, bloquear o exigir observación; la dirección `ADJUST_UP`/`ADJUST_DOWN` requiere evidencia longitudinal nutricional y de peso independiente. | 8.6; 9.8; 11.8; 11.10; 12.2; PE-02, PE-03 y PE-05. |
| RV-10. UI cotidiana simple. | **ACEPTADA CON PRECISIÓN.** | Simplificar el panel no elimina explicabilidad: categorías, resumen y estado aparecen en la pantalla diaria; procedencia, ventanas, cobertura, versiones y motivos continúan disponibles en detalles. Durante validación no se muestra una recomendación direccional como operativa. | FR-009, FR-015 y FR-019; 13.1–13.4; 16.2. |

**Resultado:** diez cambios compatibles; ninguno exige replantear producto ni arquitectura. Seis requieren precisiones de transición, persistencia mínima o presentación operativa que se resuelven explícitamente en esta versión.

### 0.3 Matriz de trazabilidad contractual

| Revisión | Requisitos v1.0 preservados o ajustados | Nuevos requisitos v1.1 | Contratos o pruebas añadidas |
| --- | --- | --- | --- |
| RV-01 | FR-001–FR-020; FR-019 cambia su materialización, no su propósito. | FR-029 y FR-035. | Matriz de materialización; AR-01–AR-07. |
| RV-02 | FR-024 y FR-025. | FR-038. | RD-01–RD-08 revisados; RC-01–RC-06. |
| RV-03 | FR-012 y FR-019. | FR-037. | CV-01–CV-05. |
| RV-04 | FR-014 y FR-015. | FR-031 y FR-032. | ES-01–ES-10. |
| RV-05 | FR-015 y FR-016. | FR-033. | HY-01–HY-10. |
| RV-06 | FR-009 y FR-026. | FR-030 y FR-042. | DR-01–DR-08. |
| RV-07 | FR-015, FR-016 y FR-019. | FR-034–FR-036 y FR-043. | SH-01–SH-12. |
| RV-08 | FR-021, FR-022 y FR-027. | FR-040 y FR-041. | SP-01–SP-07. |
| RV-09 | FR-015, FR-024 y FR-025. | FR-039. | PR-01–PR-07. |
| RV-10 | FR-009, FR-012, FR-015 y FR-019. | FR-044. | UX-01–UX-07. |

Las referencias a secciones v1.0 permiten reconstruir por qué cambió cada contrato. Todos los casos WT, NU, TD, RD, PE e IN preexistentes se conservan; solo se corrigen expectativas incompatibles con `SHADOW`, readiness categórico o recomendación `BASE_ONLY`.

### 0.4 Invariantes nuevos de v1.1

1. Una capacidad futura no se materializa hasta que una vertical actual la necesita.
2. `SHADOW` calcula y registra observaciones, pero nunca crea propuestas operativas ni modifica el plan.
3. `ADVISORY` requiere validación previa y activación explícita; ninguna recomendación se aplica automáticamente.
4. Calidad de datos y estabilidad del estimador son dimensiones independientes; ambas deben autorizar un ajuste.
5. La histéresis amortigua ruido, pero jamás retiene una acción cuando una compuerta de seguridad se cierra.
6. `BASE_ONLY` implica `recomendado_hoy = plan_base`; la actividad no activa compensación por sí misma.
7. Readiness contextualiza, bloquea o exige observación; nunca establece por sí solo la dirección del cambio nutricional.
8. La confianza cotidiana se expresa cualitativamente y se justifica con cobertura observada.
9. La recuperación inicial se expresa categóricamente, sin score 0–100.
10. Cada integración específica requiere evidencia empírica del dispositivo o archivo antes de asumir dependencias.

---

## 1. Tesis y delimitación del sistema

El producto es un sistema personal, local-first y explicable que evalúa si un plan nutricional continúa siendo apropiado según la respuesta observada de una persona físicamente activa.

Su pregunta rectora es:

> Dado el plan vigente, la ingesta registrada, la evolución del peso, la actividad y las señales de recuperación disponibles, ¿existe evidencia suficiente para mantenerlo, observarlo o proponer una corrección conservadora?

El producto no promete diagnosticar enfermedades, medir con exactitud el gasto energético, atribuir causalidad a cambios fisiológicos ni reemplazar supervisión profesional cuando existan señales de riesgo.

~~~mermaid
flowchart TD
    P[Plan vigente] --> O[Observaciones]
    O --> B[Baselines personales]
    B --> E[Evaluación de evidencia]
    E --> D{Decisión permitida}
    D --> M[Mantener]
    D --> V[Observar]
    D --> C[Proponer corrección]
    D --> I[Datos insuficientes]
    M --> O
    V --> O
    C --> A[Aceptación explícita]
    A --> P
~~~

La recomendación propuesta y el plan aplicado son estados diferentes. Durante `SHADOW` no existe propuesta operativa; la rama de corrección del diagrama pertenece al modo `ADVISORY` validado. Durante el MVP ninguna corrección modifica automáticamente el objetivo nutricional.

---

## 2. Análisis crítico de la idea original

| Hallazgo | Riesgo si se ignora | Decisión de arquitectura |
| --- | --- | --- |
| Health Connect admite más tipos de datos que los efectivamente publicados por Garmin. | Diseñar readiness alrededor de HRV o recuperación que nunca llegan. | Separar capacidad del esquema, capacidad declarada del proveedor y disponibilidad observada en ejecución. |
| La API oficial de Garmin Connect está orientada a uso empresarial y requiere aprobación. | Construir un MVP personal sobre una integración inaccesible o dependiente de servidor. | Utilizar Health Connect y exportaciones autorizadas; la API empresarial queda fuera del camino crítico. |
| El indicador de nutrición del vívoactive 6 requiere Garmin Connect+ según el manual vigente. | Depender involuntariamente de una suscripción adicional para registrar alimentos. | Mantener el diario nutricional como función propia, local y sin suscripción Garmin. |
| El historial de Garmin no garantiza historial nutricional. | Prometer TDEE adaptativo completo desde el primer inicio. | Importar baselines fisiológicos y actividad; estimar TDEE observacional únicamente cuando existan ingesta y peso contemporáneos. |
| Importado, medido, estimado y derivado no son categorías mutuamente excluyentes. | Perder procedencia o presentar estimaciones del reloj como mediciones directas. | Modelar adquisición, naturaleza, confirmación y calidad como ejes independientes. |
| Un diario vacío no equivale a cero calorías. | Generar déficits falsos y recomendaciones inseguras. | Exigir estados explícitos de completitud; distinguir ausencia, cero real y día no cerrado. |
| Una comida confirmada puede seguir siendo nutricionalmente estimada. | Confundir confirmación del usuario con exactitud de su composición. | Separar estado de confirmación de incertidumbre de alimento, cantidad y nutrientes. |
| FIT de actividad, FIT de bienestar y CSV no contienen necesariamente las mismas métricas. | Comprometer importación histórica sin conocer archivos reales. | Incorporar inspección de exportaciones, registro de parsers y matriz de cobertura por archivo. |
| Las calorías del reloj son estimaciones, y el pole o el ballet pueden estar mal representados. | Sobrecompensar ejercicio o interpretar una sesión exigente como esfuerzo bajo. | Tratar energía wearable como señal contextual; incorporar modalidad y esfuerzo percibido opcional. |
| Rendimiento estable no puede inferirse solamente de pasos, VO₂ max o calorías. | Afirmar estabilidad sin evidencia relevante para pole y ballet. | Representar rendimiento como desconocido salvo observación específica de sesión o disciplina. |
| Health Connect puede sumar pasos del teléfono y del reloj. | Duplicar actividad y amplificar recomendaciones energéticas. | Utilizar agregación y conciliación de fuentes, con trazabilidad y sin sumar registros solapados. |
| TDEE adaptativo y compensación diaria pueden contabilizar dos veces actividad habitual. | Crear un ciclo de realimentación que eleve progresivamente el objetivo. | Reservar la compensación diaria para actividad excepcional respecto del baseline ya incorporado al TDEE. |
| Cambiar el plan altera la señal que el propio evaluador intenta interpretar. | Encadenar cambios antes de observar su resultado. | Versionar planes, segmentar evidencia por vigencia y aplicar períodos de enfriamiento. |
| WorkManager no garantiza ejecución exacta ni frecuencia continua. | Diseñar readiness supuestamente en vivo. | Recalcular por eventos, apertura de la aplicación y sincronizaciones periódicas oportunistas. |
| Las copias automáticas de Android pueden incluir la base Room. | Enviar datos de salud a la nube contradiciendo local-first. | Configurar exclusiones explícitas y respaldos voluntarios cifrados. |
| Una cifra de confianza como 0,82 no es automáticamente una probabilidad estadística. | Ofrecer falsa precisión y decisiones aparentemente científicas. | Mostrar confianza cualitativa, cobertura observada y rangos operativos; reservar el índice no calibrado a políticas y depuración avanzada. |
| Reducción calórica con carga deportiva alta puede empeorar disponibilidad energética. | Favorecer deterioro de recuperación y rendimiento. | Bloquear recortes cuando existan señales de riesgo, alta carga mal observada o contexto insuficiente. |
| HRV baja y déficit simultáneos no establecen causalidad. | Convertir coincidencias en conclusiones médicas. | Explicar asociaciones y considerar sueño, entrenamiento, estrés, enfermedad y viajes. |
| Historial importado antiguo puede mezclar etapas de entrenamiento o planes diferentes. | Construir baselines personales contaminados o desactualizados. | Calcular baselines móviles, excluir eventos contextualizados y registrar ventanas y versiones. |

### 2.1 Decisiones críticas cerradas

1. El evaluador del plan es el centro del producto; readiness, peso y TDEE son insumos o resultados auxiliares.
2. Ninguna métrica de Garmin específica es obligatoria para que la aplicación funcione.
3. El MVP debe aportar valor aun sin HRV, Body Battery, estrés, Recovery Time o internet.
4. El sistema propone; la persona acepta, rechaza o pospone.
5. La ausencia de rendimiento observado nunca se presenta como rendimiento estable.
6. La aplicación no recorta automáticamente objetivos nutricionales.
7. Todo umbral inicial es una política de producto versionada, no una afirmación clínica validada.

---

## 3. Restricciones externas verificadas

### 3.1 Capacidades reales del vívoactive 6

Garmin documenta para vívoactive 6 sueño, fases y puntuación de sueño, HRV nocturna, Body Battery, estrés, frecuencia cardíaca, pasos, calorías, VO₂ max y Recovery Time. La presencia de una función en el reloj no implica que esté disponible para otra aplicación Android. [Presentación oficial de vívoactive 6](https://www.garmin.com/en-US/newsroom/press-release/wearables-health/meet-vivoactive-6-the-latest-health-and-fitness-smartwatch-from-garmin/) y [manual oficial de indicadores del dispositivo](https://www8.garmin.com/manuals/webhelp/GUID-8C2C402F-55AC-431F-9CF2-1442B89CE149/EN-US/GUID-97EA1540-A780-480F-BA4D-9A9E147FB225.html).

El manual lista Recovery Time, HRV y Body Battery, pero no incluye Training Readiness entre sus indicadores; por tanto, la preparación de esta aplicación será un cálculo propio y no una lectura de una puntuación Garmin inexistente en el modelo. El mismo manual indica que el indicador de nutrición del reloj requiere una suscripción activa Garmin Connect+. La arquitectura no debe depender de esa función de pago: el registro nutricional propio es local y gratuito desde el MVP. [Manual oficial de indicadores del vívoactive 6](https://www8.garmin.com/manuals/webhelp/GUID-8C2C402F-55AC-431F-9CF2-1442B89CE149/EN-US/GUID-97EA1540-A780-480F-BA4D-9A9E147FB225.html).

Garmin indica que su propio estado de HRV necesita aproximadamente tres semanas de registros nocturnos consistentes. El producto debe aplicar criterios propios de madurez del baseline, no asumir que una sola noche basta. [Manual oficial: Heart Rate Variability Status](https://www8.garmin.com/manuals/webhelp/GUID-8C2C402F-55AC-431F-9CF2-1442B89CE149/EN-US/GUID-9282196F-D969-404D-B678-F48A13D8D0CB.html).

### 3.2 Health Connect: esquema disponible frente a publicación efectiva

Android define registros para sueño, frecuencia cardíaca, frecuencia cardíaca en reposo, HRV RMSSD, pasos, actividades, calorías activas, calorías totales, peso, nutrición y VO₂ max. Sin embargo, el proveedor emisor determina qué tipos escribe efectivamente. [Tipos y permisos oficiales de Health Connect](https://developer.android.com/health-and-fitness/health-connect/data-types).

La lista publicada por Garmin para Health Connect incluye datos de actividad —calorías activas y totales, cadencia ciclista, distancia, elevación, frecuencia cardíaca, velocidad, pasos y brazadas— y bienestar —grasa corporal, calorías totales, pisos, frecuencia cardíaca, fases de sueño, pasos/distancia y peso—. HRV, Body Battery, estrés, Recovery Time, puntuación de sueño y VO₂ max no aparecen en esa lista pública consultada. Su disponibilidad debe considerarse no garantizada y comprobarse en el dispositivo real. [Garmin: compartir datos con Health Connect](https://support.garmin.com/en-US/?faq=JToBEy0jfe6pIygark2Ui5).

| Métrica | Dispositivo | Tipo en Health Connect | Garmin la enumera para Health Connect | Exportación Garmin | Contrato del producto |
| --- | --- | --- | --- | --- | --- |
| Sueño y fases | Sí | Sí | Sí | FIT de bienestar, sujeto al archivo | Insumo prioritario del MVP integrado en fase 4; no bloquea el núcleo local. |
| Puntuación de sueño | Sí | No existe un tipo estándar equivalente directo | No | Posible, no garantizada | Opcional; no depender de ella. |
| Frecuencia cardíaca | Sí | Sí | Sí | FIT de bienestar o actividad | Utilizar con origen y calidad. |
| Frecuencia cardíaca en reposo | Sí | Sí | No figura explícitamente | Posible; verificar | Opcional; una derivación propia debe identificarse como tal. |
| HRV RMSSD | Sí | Sí | No figura | Garmin menciona HRV en FIT de bienestar | Priorizar validación de exportación; nunca asumir flujo continuo. |
| Estrés | Sí | Sin tipo estándar Garmin equivalente | No figura | Garmin menciona estrés en FIT de bienestar | Opcional y dependiente de parser. |
| Body Battery | Sí | Sin tipo estándar | No figura | No garantizada | Integración futura; no tratar como indispensable. |
| Pasos | Sí | Sí | Sí | Sí | Conciliar reloj y teléfono; evitar duplicación. |
| Calorías activas | Sí | Sí | Sí, para actividad | Sí | Estimación del proveedor, no medición exacta. |
| Calorías totales | Sí | Sí | Sí | Sí | No sumarlas nuevamente con calorías activas. |
| Sesiones y entrenamientos | Sí | Sí | Cobertura concreta a verificar en ejecución | FIT de actividad y CSV de actividades | Soportar sesiones reales si existen y carga manual opcional. |
| Recovery Time | Sí | Sin tipo estándar equivalente | No figura | No garantizada | Opcional; nunca bloquear readiness. |
| VO₂ max | Sí | Sí | No figura | Posible; verificar | Futuro; no usar como proxy de rendimiento en pole o ballet. |
| Peso | Registro manual y posible integración Garmin | Sí | Sí, cuando exista | CSV u otros formatos, sujeto a exportación | El registro manual propio es suficiente y prioritario. |
| Nutrición | No constituye una fuente garantizada de Garmin | Sí | No figura | No garantizada | La aplicación es fuente primaria de su propio diario. |

Estados de capacidad obligatorios: NO_SOPORTADO_POR_ESQUEMA, SOPORTADO_POR_ESQUEMA, DECLARADO_POR_PROVEEDOR, OBSERVADO_EN_EJECUCIÓN, PERMISO_DENEGADO, SIN_DATOS y FORMATO_NO_VERIFICADO. Estos estados no son intercambiables.

### 3.3 Historial, permisos y sincronización

Health Connect requiere Android 9/API 28 o superior para su uso efectivo. En Android 14 o superior forma parte del sistema; en versiones previas puede requerir instalación independiente. Se propone minSdk 28. [Disponibilidad oficial de Health Connect](https://developer.android.com/health-and-fitness/health-connect/availability).

Por defecto, la lectura histórica está restringida a aproximadamente 30 días anteriores al otorgamiento de permisos; leer más historia requiere permiso específico. La lectura en segundo plano requiere otro permiso específico y depende de disponibilidad de la característica. No se debe prometer importación histórica ilimitada ni sincronización automática sin esos permisos. [Lectura de datos e historial](https://developer.android.com/health-and-fitness/health-connect/read-data).

Health Connect recomienda tokens de cambios separados por tipo de registro, gestión de inserciones, modificaciones y eliminaciones, y recuperación ante expiración mediante relectura y deduplicación. Los tokens pueden expirar si no se utilizan durante 30 días. [Sincronización oficial de Health Connect](https://developer.android.com/health-and-fitness/health-connect/sync-data).

Desde cambios documentados en 2026, Health Connect puede aportar pasos del propio teléfono con orígenes sintéticos dependientes del dispositivo. No se deben fijar esos identificadores como constantes ni sumar sin conciliación pasos del teléfono y del Garmin. [Lectura de datos y atribución de pasos](https://developer.android.com/health-and-fitness/health-connect/read-data).

### 3.4 Exportaciones Garmin y acceso oficial

Garmin documenta exportación de actividades en CSV y formatos originales, y exportación de archivos FIT de bienestar con datos que pueden incluir pasos, sueño, estrés y HRV. El contenido exacto depende del dispositivo, período, modalidad de exportación y archivos presentes. [Garmin: exportación de datos](https://support.garmin.com/en-US/?faq=W1TvTPW8JZ6LfJSfK512Q8).

La misma documentación diferencia la exportación puntual de bienestar por día de una solicitud de exportación completa mediante Account Management Center. La aplicación debe contemplar ambas rutas, pero no asumir que el paquete completo contiene idénticos formatos, que está disponible inmediatamente ni que cada métrica propietaria se exporta en forma reutilizable.

El FIT SDK oficial dispone de implementación Java. Su uso como dependencia JVM es compatible con escribir todo el código propio en Kotlin; no obliga a introducir Python ni clases Java propias. Deben verificarse licencia, compatibilidad Android y formato real antes de incluirlo. [FIT SDK oficial](https://developer.garmin.com/fit/get-the-sdk/) y [decodificación de archivos de actividad](https://developer.garmin.com/fit/cookbook/decoding-activity-files/).

El Garmin Connect Developer Program se define como programa de uso empresarial y requiere aprobación. Por tanto, no es una base apropiada para un proyecto Android personal sin backend. [Preguntas frecuentes oficiales de Garmin Developers](https://developer.garmin.com/gc-developer-program/program-faq/) y [Garmin Health API](https://developer.garmin.com/gc-developer-program/health-api/).

Quedan excluidos scraping de credenciales, automatización de inicio de sesión no autorizada y dependencia de endpoints privados inestables.

### 3.5 Dependencias estables de referencia

Con fecha de verificación 23/08/2026, las referencias estables publicadas son Room 2.8.4, Health Connect 1.1.0 y WorkManager 2.11.2. Existe documentación de transición a Room 3, pero la página oficial de versiones sigue señalando Room 2.8.4 como estable; no se adoptará una versión nueva solamente por aparecer documentada. Las versiones se verificarán nuevamente al iniciar la implementación. [Versiones de Room](https://developer.android.com/jetpack/androidx/releases/room), [versiones de Health Connect](https://developer.android.com/jetpack/androidx/releases/health-connect) y [versiones de WorkManager](https://developer.android.com/jetpack/androidx/releases/work).

---

## 4. A. Definición de producto

### 4.1 Problema

Las aplicaciones tradicionales suelen fragmentar información nutricional, peso, actividad y recuperación, presentar calorías wearable con precisión excesiva o cambiar objetivos según señales aisladas. Para una persona con alta carga de pole, ballet y otras disciplinas, esos errores pueden hacer que un plan aparentemente correcto deje de acompañar el rendimiento y la recuperación.

### 4.2 Objetivo

Ofrecer una evaluación longitudinal, prudente y auditable de la relación entre plan nutricional, ejecución real y respuesta personal.

### 4.3 Propuesta de valor

1. Integra señales heterogéneas sin tratarlas como igualmente exactas.
2. Aprende baselines personales cuando existe historia suficiente.
3. Estima el gasto energético con incertidumbre explícita.
4. Detecta discrepancias persistentes entre objetivo y respuesta observada.
5. Bloquea ajustes cuando falta evidencia o aparecen señales de riesgo.
6. Explica cada decisión mediante razones estructuradas y referencias verificables.
7. Mantiene datos, cálculos y funcionamiento ordinario en el dispositivo.

### 4.4 Usuario inicial

Una persona adulta físicamente activa que utiliza Android y Garmin vívoactive 6; combina entrenamientos de pole, ballet, flexibilidad u otras sesiones; registra alimentación y peso de forma no necesariamente diaria; y quiere preservar rendimiento, recuperación y autonomía de decisión.

La primera versión es monousuario. No infiere sexo, composición corporal, estado menstrual, edad ni antecedentes sensibles. Cualquier dato opcional de ese tipo requiere introducción y consentimiento específicos.

### 4.5 Casos de uso principales

1. Definir un plan energético y proteico con objetivo de mantenimiento, pérdida, ganancia o prioridad de rendimiento.
2. Registrar peso esporádicamente y observar una tendencia robusta.
3. Registrar alimentos manualmente, repetir favoritos y recientes, o cargar comidas guardadas.
4. Identificar qué parte de la ingesta está confirmada, estimada o pendiente.
5. Cerrar un día nutricional y corregirlo posteriormente con historial de revisiones.
6. Sincronizar sueño, actividad y otras métricas efectivamente disponibles.
7. Importar archivos históricos compatibles sin mezclar observaciones duplicadas.
8. Consultar preparación matinal cuando existan señales suficientes.
9. Obtener TDEE provisional o adaptativo con rango e índice de confianza.
10. Consultar estado del plan y causas de mantener, observar, corregir o abstenerse.
11. Aceptar, descartar o posponer una corrección propuesta.
12. Inspeccionar calidad de datos y razones de cualquier bloqueo.

### 4.6 No objetivos

Quedan fuera del MVP y de la tesis central: red social, gamificación, coach conversacional, servidor obligatorio, autenticación obligatoria, varios wearables, Connect IQ, IA fotográfica paga, recetas complejas, planificación de menús, micronutrientes exhaustivos, diagnósticos, predicción clínica, ajuste autónomo de calorías y telemetría comercial de datos personales.

### 4.7 Indicadores de éxito

- La aplicación permite registrar una ingesta recurrente en pocos gestos.
- Un día sin pesaje no genera un peso ficticio.
- Un día nutricional no cerrado no se incorpora al cálculo adaptativo como si estuviera completo.
- Cada recomendación explica los datos que la autorizan o bloquean.
- El núcleo funciona sin reloj, internet, cuenta ni permisos de Health Connect.
- La existencia de Garmin aumenta información disponible, pero nunca define por sí sola una decisión.

---

## 5. B. Requisitos funcionales

### 5.1 MVP imprescindible: núcleo útil sin integraciones

| ID | Requisito | Criterio mínimo |
| --- | --- | --- |
| FR-001 | Perfil local único y configuración inicial. | Crear perfil sin cuenta ni conexión. |
| FR-002 | Plan nutricional versionado. | Registrar calorías base, proteína objetivo, tipo de objetivo y ritmo deseado cuando corresponda. |
| FR-003 | Registro manual de peso. | Crear, editar mediante revisión y eliminar sin exigir frecuencia diaria. |
| FR-004 | Tendencia de peso robusta. | Procesar fechas irregulares y señalar datos insuficientes sin inventar mediciones. |
| FR-005 | Registro manual rápido de alimentos. | Nombre, cantidad, unidad y nutrientes conocidos; permitir campos desconocidos. |
| FR-006 | Favoritos. | Guardar producto y porción habitual; registrar en una acción principal. |
| FR-007 | Recientes. | Repetir productos o entradas utilizadas anteriormente. |
| FR-008 | Comidas guardadas. | Guardar componentes, clonar la comida y editar cantidades sin alterar el modelo original. |
| FR-009 | Nutrición diaria. | Distinguir plan base, recomendado, confirmado, estimado, pendiente y restante calculable; en el modo inicial el recomendado coincide exactamente con el plan base. |
| FR-010 | Macros parciales. | Mostrar proteína, carbohidratos y grasas cuando existan; desconocido no equivale a cero. |
| FR-011 | Cierre explícito del diario. | Clasificar cada día como abierto, completo confirmado, incompleto o excluido justificadamente. |
| FR-012 | Calidad por dimensión. | Calcular cobertura y factores limitantes para peso y nutrición. |
| FR-013 | TDEE inicial transparente. | Aceptar valor manual o estimación poblacional opcional sin presentarla como aprendizaje personal. |
| FR-014 | TDEE adaptativo básico. | Utilizar exclusivamente ventanas contemporáneas de ingesta suficientemente completa y tendencia de peso; informar por separado calidad de entradas y estabilidad del resultado. |
| FR-015 | Evaluador básico del plan. | Calcular MAINTAIN, OBSERVE, ADJUST_UP, ADJUST_DOWN o INSUFFICIENT_DATA con razones; distinguir candidato interno, estado efectivo y autorización operativa. |
| FR-016 | Corrección manualmente aceptada. | Solo en ADVISORY validado, mostrar propuesta conservadora, límites, evidencia y resultado de aceptación o rechazo; en SHADOW no crear propuestas. |
| FR-017 | Registro contextual mínimo. | Marcar enfermedad, viaje, sesión excepcional, lesión u otras incidencias relevantes de forma opcional. |
| FR-018 | Esfuerzo y rendimiento subjetivos opcionales. | Registrar modalidad, duración, esfuerzo 1–10 y percepción de rendimiento sin convertirlos en requisito. |
| FR-019 | Trazabilidad. | Persistir proporcionalmente a la fase versión de algoritmo/política, ventana, resumen de entradas, razones e índice interno; posponer corridas normalizadas, hashes exhaustivos y auditoría avanzada hasta que sean necesarias. |
| FR-020 | Uso offline. | Ejecutar todas las funciones anteriores sin internet. |

### 5.1.1 Incrementos contractuales introducidos en v1.1

| ID | Requisito | Criterio mínimo | Primera fase aplicable |
| --- | --- | --- | --- |
| FR-029 | Implementación vertical progresiva. | Crear únicamente módulos, puertos, entidades y tablas utilizados por la vertical actual. | 1–7. |
| FR-030 | Recomendación inicial BASE_ONLY. | Para todo día y toda actividad, `recomendado_hoy = plan_base`; Garmin no altera la igualdad. | 2. |
| FR-031 | Estabilidad explícita del estimador. | Calcular `INSUFFICIENT_HISTORY`, `UNSTABLE`, `STABILIZING` o `STABLE` independientemente de la confianza de inputs. | 3b. |
| FR-032 | Historial mínimo de TDEE. | Conservar estimaciones cronológicas necesarias para estabilidad, política, revisión y depuración básica; no exigir auditoría normalizada completa. | 3b. |
| FR-033 | Histéresis versionada. | Aplicar umbrales de entrada y salida diferentes, memoria por plan y salida inmediata ante bloqueo o riesgo. | 3c. |
| FR-034 | Modo de validación SHADOW. | Calcular y registrar candidatos sin crear `AdjustmentProposal`, cambiar el plan ni permitir aceptación. | 3c–3.5. |
| FR-035 | Observabilidad mínima proporcional. | Guardar fecha, versiones, ventana, plan, coberturas, estabilidad, estado, motivos y carácter no operativo sin tablas avanzadas. | 3c. |
| FR-036 | Transición explícita SHADOW → ADVISORY. | Exigir criterios de salida, revisión humana y activación consciente; mantener la aceptación manual de cada propuesta. | 3.5. |
| FR-037 | Confianza cualitativa visible. | Mostrar ALTA, MODERADA, BAJA o INSUFICIENTE con pesajes, días completos e ingesta estimada; reservar el índice numérico a inspección avanzada. | 3. |
| FR-038 | Readiness categórico. | Mostrar GOOD, MODERATE, LOW o UNKNOWN y factores; no requerir, calcular ni persistir score 0–100 en la primera implementación. | 4. |
| FR-039 | Separación readiness/evaluación. | Ninguna observación de readiness puede determinar la dirección de `ADJUST_UP` o `ADJUST_DOWN`; sí puede bloquear o contextualizar. | 4. |
| FR-040 | Spike empírico Health Connect. | Documentar permisos, orígenes y tipos efectivamente observados en el teléfono antes de crear el adaptador específico. | 0; compuerta 4. |
| FR-041 | Spike empírico exportación Garmin. | Inventariar archivos, formatos, granularidad, IDs, duplicados y limitaciones antes de seleccionar parser o SDK. | 0; compuerta 5. |
| FR-042 | Activación condicionada de compensación. | Habilitar `ADAPTIVE` solo después de validar TDEE, baseline de actividad, nutrición, tendencia, sombra y reglas anti-duplicación. | V2 o fase posterior habilitada. |
| FR-043 | Invalidación por corrección retrospectiva. | Marcar evaluaciones y estabilidad afectadas como revisables; bloquear propuestas anteriores si cambia evidencia relevante. | 3c–3.5. |
| FR-044 | Panel cotidiano simple. | Priorizar recuperación categórica, energía/proteína, ritmo de peso y estado del plan; conservar explicación completa en detalle. | 2–4. |

### 5.2 MVP ampliado: integración realista

| ID | Requisito | Criterio mínimo |
| --- | --- | --- |
| FR-021 | Diagnóstico de Health Connect. | Mostrar disponibilidad del servicio, permisos y tipos efectivamente encontrados. |
| FR-022 | Sincronización por tipos. | Leer sueño, pasos, frecuencia cardíaca, energía y sesiones únicamente cuando existan. |
| FR-023 | Deduplicación. | Reconciliar orígenes, intervalos y exportaciones repetidas. |
| FR-024 | Baseline fisiológico. | Calcular sueño y carga personales; añadir HRV o FC en reposo solo si existe evidencia real. |
| FR-025 | Morning Readiness básico. | Mostrar exclusivamente categoría GOOD, MODERATE, LOW o UNKNOWN, factores y confianza cualitativa; score numérico diferido. |
| FR-026 | Recomendación diaria conservadora. | Mantener BASE_ONLY como predeterminado; una compensación excepcional solo existe tras activar ADAPTIVE mediante compuertas independientes, nunca 1:1. |
| FR-027 | Importación histórica mínima. | Inspeccionar una exportación real e importar al menos el formato prioritario que se valide. |
| FR-028 | Actualización oportunista. | Sincronizar al abrir, manualmente y en segundo plano solo cuando esté autorizado. |

FR-021 y FR-022 quedan sujetos a FR-040; FR-027 queda sujeto a FR-041. No se promete compatibilidad con métricas, FIT de bienestar, CSV o ZIP específicos antes de disponer de evidencia empírica. La ausencia de estos spikes no bloquea las fases locales 1–3.5.

### 5.3 V2

- Parser FIT de bienestar para HRV, estrés y demás campos realmente verificados.
- Parser FIT de actividad con mayor detalle e importación masiva controlada.
- Current Readiness intradiario guiado por sesiones, sueño y señales disponibles.
- Compensación diaria `ADAPTIVE` únicamente después de aprobar su validación específica.
- Score readiness 0–100 únicamente si demuestra utilidad incremental frente a las categorías.
- Integración de código de barras con base externa, caché y tolerancia offline.
- Historial y comparación de baselines por modalidad de entrenamiento.
- Edición y revisión avanzada del diario, detección de solapamientos y conciliación de orígenes.
- Copia y restauración cifradas, si no llegaron a incorporarse al MVP ampliado.
- Contexto opcional de ciclo menstrual, síntomas o viajes, con consentimiento granular.
- Herramientas de inspección de algoritmos, simulación y comparación entre versiones.
- Exportación personal portable de observaciones y decisiones.

### 5.4 Futuro

- Fotografía con proveedor reemplazable y confirmación de alimento y cantidad.
- Modelo local de reconocimiento; proveedor remoto exclusivamente opcional.
- Envases abiertos y cantidades restantes aproximadas.
- Integraciones autorizadas adicionales y eventuales API empresariales si el proyecto cambia de naturaleza.
- Estimadores personalizados más sofisticados después de validación empírica.
- Ajuste automático estrictamente opcional y sujeto a un expediente de seguridad independiente.
- Intercambio opcional de nutrición o peso propio con Health Connect, sin realimentar registros previamente leídos.

---

## 6. C. Requisitos no funcionales

### 6.1 Privacidad y seguridad

1. No exigir cuenta, backend ni identificador publicitario.
2. Persistir datos personales principalmente en almacenamiento privado de la aplicación.
3. Solicitar permisos de salud por tipo y únicamente cuando la función correspondiente se activa.
4. No enviar peso, sueño, HRV, entrenamientos ni historial a Open Food Facts o proveedores fotográficos para consultas de productos.
5. Excluir explícitamente base de datos, archivos originales y registros sensibles de copias automáticas en la nube salvo decisión informada y protección adecuada.
6. Aplicar cifrado adicional según modelo de amenaza; si se cifran claves, utilizar Android Keystore. Room sin integración adicional no equivale por sí mismo a cifrado de base de datos.
7. No imprimir observaciones de salud, alimentos, rutas GPS ni credenciales en logs de producción.
8. No registrar ni importar ubicación si no es necesaria para la función.
9. Permitir borrar datos de aplicación y revocar integraciones sin destruir archivos externos de terceros.
10. Diferenciar exportación voluntaria cifrada de sincronización obligatoria.

Android incluye bases SQLite en Auto Backup de forma predeterminada y recomienda configurar exclusiones para datos sensibles. Android Keystore permite proteger claves con material no exportable. [Auto Backup de Android](https://developer.android.com/identity/data/autobackup) y [Android Keystore](https://developer.android.com/privacy-and-security/keystore).

### 6.2 Offline y tolerancia a fallos

- Room es la fuente de verdad de la interfaz.
- Alimentación, peso, cálculos, planes y consultas deben permanecer disponibles sin red.
- Health Connect no disponible, permisos revocados, archivo corrupto o API externa inaccesible no bloquean funciones manuales.
- Cada integración informa disponibilidad, última sincronización y causas de degradación.
- Importaciones y sincronizaciones son idempotentes, reanudables y resistentes a resultados parciales.
- Las tareas que requieren internet se limitan a funcionalidades que realmente lo necesitan.

### 6.3 Mantenibilidad

- Núcleo de dominio y algoritmos compilable y testeable en JVM sin Android.
- Kotlin de aplicación, Gradle Kotlin DSL y dependencias estables.
- Módulos y contratos pequeños con dependencias dirigidas hacia el dominio.
- Configuración de umbrales y versiones de algoritmos explícita.
- Migraciones Room probadas; prohibir migraciones destructivas sobre datos de producción.
- Separar objetos Room, modelos externos y entidades de dominio.

### 6.4 Rendimiento y capacidad

- Registro de alimento o peso reflejado inmediatamente desde persistencia local.
- Consultas del panel principal por fecha e índices; evitar recorrer series completas por recomposición.
- Recalcular únicamente áreas invalidadas por una modificación.
- Procesar importaciones grandes por lotes, con progreso y cancelación.
- Establecer políticas de retención y agregación para series de frecuencia cardíaca de alta granularidad.
- Priorizar consumo moderado de batería y evitar polling permanente.

Presupuestos iniciales medibles en un teléfono Android de referencia, sujetos a ajuste después del primer benchmark: actualización reactiva de un registro local en menos de 300 ms en p95; lectura del panel diario ya indexado en menos de 500 ms en p95; recálculo de una ventana personal de 90 días en menos de 1 segundo en condiciones normales; ausencia de consultas Room, importaciones o operaciones criptográficas bloqueantes en el hilo principal. Las importaciones extensas muestran progreso y cancelación en vez de exigir un tiempo universal independiente del hardware.

### 6.5 Auditabilidad, exactitud y calidad

- Un cálculo importante debe reconstruirse desde datos, revisiones, configuración y versión de algoritmo.
- La ausencia es explícita y nunca se sustituye silenciosamente por cero.
- La incertidumbre y las advertencias acompañan resultados derivados.
- Las correcciones nutricionales están subordinadas a reglas de autorización verificables.
- Mismos datos, versión, configuración, zona horaria y reloj de referencia deben producir el mismo resultado.
- Cualquier evaluación debe distinguir evidencia observada de interpretación del algoritmo.

### 6.6 Límites sanitarios

El producto es de bienestar y seguimiento, no dispositivo médico. En personas con carga deportiva significativa, una disponibilidad energética insuficiente puede afectar salud y rendimiento; el software no debe diagnosticar REDs ni utilizar sus indicadores como certeza causal. Debe bloquear recortes ante señales relevantes y remitir a revisión cuando corresponda. [Consenso 2023 del Comité Olímpico Internacional sobre REDs](https://bjsm.bmj.com/content/57/17/1073).

---

## 7. Contratos transversales de datos y tiempo

### 7.1 Ejes independientes de significado

Cada observación importante conserva, cuando corresponda:

| Eje | Valores representativos | Pregunta que responde |
| --- | --- | --- |
| Presencia | PRESENTE, AUSENTE, NO_APLICA, NO_DISPONIBLE, SIN_PERMISO | ¿Existe un valor utilizable? |
| Adquisición | MANUAL, HEALTH_CONNECT, ARCHIVO_IMPORTADO, API_ALIMENTOS, MODELO_LOCAL, MODELO_REMOTO | ¿Cómo ingresó al sistema? |
| Origen | USUARIO, GARMIN, OTRA_APLICACIÓN, BASE_ALIMENTOS, APLICACIÓN_PROPIA | ¿Quién originó la información? |
| Naturaleza | AUTORREPORTADO, SENSOR, ESTIMADO_POR_PROVEEDOR, ESTIMADO_POR_APP, DERIVADO, ETIQUETA_DECLARADA | ¿Qué representa epistemológicamente? |
| Confirmación | BORRADOR, PENDIENTE, CONFIRMADO, RECHAZADO | ¿Fue aceptado como registro? |
| Calidad | VÁLIDO, INCOMPLETO, POSIBLE_OUTLIER, DUPLICADO, CONFLICTIVO, DESACTUALIZADO | ¿Qué limitaciones presenta? |
| Confianza | Índice 0–1 y motivos | ¿Qué decisiones puede respaldar? |
| Temporalidad | instante UTC, día civil, zona, intervalo, revisión | ¿Cuándo ocurrió y cómo se agrupa? |

Ejemplo: calorías de un FIT de Garmin corresponden simultáneamente a ARCHIVO_IMPORTADO, GARMIN, ESTIMADO_POR_PROVEEDOR y eventualmente CONFIRMADO. No son una medición directa solo por estar presentes en un archivo original.

### 7.2 Ausencia y ceros legítimos

- 0 kcal en una bebida declarada sin calorías puede ser un valor legítimo.
- 0 alimentos registrados en un día abierto significa ingesta desconocida, no ayuno confirmado.
- Un nutriente ausente en la etiqueta significa desconocido, no 0 g.
- Una consulta agregada sin registros significa ausencia, aunque ejemplos genéricos de SDK reemplacen resultados nulos por cero.
- Ausencia de HRV significa que no debe utilizarse esa señal; no significa HRV igual a cero ni readiness mínimo.
- Actividad no sincronizada significa actividad desconocida; no equivale a descanso.
- Un día con ingesta cero explícitamente confirmado requiere revisión especial y nunca habilita recortes automáticos.

### 7.3 Metadatos mínimos

Campos conceptuales comunes: identificador local, persona, instante de ocurrencia UTC, inicio y fin cuando corresponda, fecha civil efectiva, zona horaria, offset, origen, mecanismo de adquisición, identificador externo, revisión externa, método, naturaleza, calidad, rango o nivel de incertidumbre, lote de importación, instante de incorporación y estado de vigencia.

Para derivados: identificador de ejecución, nombre y versión de algoritmo, versión de política, ventana, referencias a entradas y revisiones, huella de contenido, configuración, zona de cálculo, confianza y razones.

### 7.4 Días civiles, viajes y horario de verano

1. Los instantes se almacenan en UTC; la fecha civil conserva la zona y offset originales o la política de agrupación elegida.
2. Una noche de sueño se atribuye principalmente al día de despertar.
3. Comidas y peso se asignan según fecha civil efectiva del evento.
4. Sesiones que cruzan medianoche conservan intervalo completo; cualquier distribución diaria derivada se identifica como tal.
5. Días de 23 o 25 horas por cambio horario no se modelan como ventanas universales de 24 horas.
6. Si falta offset de la fuente, se registra que fue inferido.
7. Cambio de zona, viaje o datos nocturnos incompletos pueden introducir una bandera contextual y reducir confianza.

### 7.5 Revisión e inmutabilidad lógica

- La corrección de un peso o alimento crea una revisión identificable.
- Una versión de producto alimentario no modifica consumos históricos ya confirmados.
- Los planes son versiones inmutables con vigencia y evento de aceptación.
- Los resultados derivados pueden recalcularse y coexistir por versión de algoritmo.
- La eliminación genera invalidación de derivados afectados; se conserva solo la información de auditoría permitida por la política de privacidad.

---

## 8. D. Arquitectura propuesta

### 8.1 Capas

~~~mermaid
flowchart TD
    UI[Compose y ViewModels] --> APP[Casos de uso]
    APP --> DOMAIN[Dominio y políticas]
    APP --> ENGINES[Motores deterministas]
    ENGINES --> DOMAIN
    APP --> PORTS[Puertos de repositorio]
    LOCAL[Adaptador Room] --> PORTS
    HC[Adaptador Health Connect] --> PORTS
    IMPORT[Adaptadores de importación] --> PORTS
    FOOD[Adaptador de alimentos externo] --> PORTS
    LOCAL --> DB[(SQLite)]
~~~

La flecha representa dependencia: los puertos pertenecen al núcleo y los adaptadores dependen de ellos, nunca al revés. El dominio no conoce Room, Android, Garmin, FIT, JSON, HTTP, Compose ni WorkManager.

### 8.2 Módulos objetivo y materialización progresiva

| Módulo | Tipo | Responsabilidad | Dependencias permitidas |
| --- | --- | --- | --- |
| app | Android | Compose, navegación, ViewModels, permisos, composición e integración del sistema. | application, domain, engine, local y únicamente los adaptadores ya autorizados para la fase. |
| core:domain | Kotlin/JVM | Entidades, value objects, invariantes, estados, puertos y contratos. | Kotlin y librerías JVM/Kotlin estrictamente necesarias. |
| core:engine | Kotlin/JVM | Tendencia, baseline, calidad, TDEE, readiness, recomendación y evaluación. | core:domain. |
| core:application | Kotlin/JVM | Casos de uso, coordinación de repositorios y reglas transaccionales abstractas. | core:domain y core:engine. |
| data:local | Android Library | Room, DAO, mappers, revisiones y repositorios locales. | core:domain y Room. |
| data:healthconnect | Android Library | Permisos, capacidades, normalización y sincronización Health Connect. | core:domain y cliente Health Connect. |
| data:import | Kotlin/JVM | Inspección y parsers sobre streams o bytes; Android SAF se resuelve en app mediante un adaptador delgado. | core:domain y, opcionalmente, FIT SDK JVM. |

Módulos futuros: data:foodremote, data:photo-local, data:photo-remote y sync. Esta tabla describe la arquitectura **objetivo**, no el árbol Gradle exigible desde la fase 1.

| Elemento | Materializar por primera vez | No materializar anticipadamente |
| --- | --- | --- |
| `app` y `core:domain`. | Fase 1. | Pantallas de Garmin, importación o reconocimiento. |
| `core:application` y `data:local`. | Fase 2, cuando la primera vertical necesite casos de uso y Room. | Repositorios de auditoría, fuentes, FIT o derivados no utilizados. |
| `core:engine` y motores concretos. | Fase 3a; cada motor se añade cuando su vertical tiene inputs reales. | Baselines fisiológicos, readiness o recomendación adaptativa vacíos. |
| `data:healthconnect`. | Fase 4, después del Spike A. | SDK, permisos y supuestos de métricas antes de la compuerta empírica. |
| `data:import`. | Fase 5, después del Spike B y selección de un formato. | FIT SDK, parsers genéricos y tablas masivas sin muestra confirmada. |
| `data:foodremote` y fotografía. | Fase 7a/7b o posterior. | Dependencias de red, IA, proveedores pagos o permisos de cámara. |

Se admite temporalmente que `core:application` o `core:engine` compartan un módulo Kotlin si la primera vertical es más simple y sus fronteras permanecen inequívocas. Separarlos se vuelve obligatorio cuando la complejidad o las dependencias efectivamente lo justifican; no se crearán módulos vacíos para satisfacer un diagrama.

### 8.3 Restricciones de dependencia

1. core:domain no importa android, androidx, Room, Compose, SDK Garmin ni networking.
2. core:engine contiene funciones deterministas y no consulta directamente repositorios, reloj del sistema, red o archivos.
3. core:application recibe reloj, zona, configuración y repositorios mediante interfaces.
4. data:local traduce entre filas Room y dominio; entidades Room no atraviesan la capa de aplicación.
5. data:healthconnect traduce registros externos; sus clases no llegan a los motores.
6. Los parsers producen modelos normalizados, nunca puntuaciones de negocio.
7. WorkManager coordina casos de uso; no contiene algoritmos nutricionales.
8. La inyección puede comenzar con constructores y un contenedor simple; cualquier framework queda limitado a la periferia Android.

### 8.4 Puertos de dominio

- WeightRepository: consulta y revisión de mediciones por período y origen.
- NutritionDiaryRepository: productos, consumos, cierre diario y revisiones.
- MealTemplateRepository: favoritos, recientes y comidas guardadas.
- NutritionPlanRepository: versiones, vigencias, propuestas y aceptación.
- HealthMetricsRepository: sueño, frecuencia cardíaca, HRV y métricas normalizadas disponibles.
- ActivityRepository: sesiones, carga, modalidad y reflexiones subjetivas.
- PersonalBaselineRepository: baselines calculados y versiones.
- DerivedResultsRepository: tendencias, TDEE, readiness, recomendaciones y evaluaciones.
- EstimatorHistoryRepository: historial acotado de estimaciones necesario para determinar estabilidad.
- EvaluationObservationRepository: registro mínimo de candidatos y resultados en validación o asesoramiento.
- DecisionStateRepository: memoria de histéresis, contador de evidencia y vigencia por plan.
- EvaluationModeRepository: modo `SHADOW` o `ADVISORY` y condiciones de activación.
- DataQualityRepository: calidad por ventana y dimensión.
- SourceCapabilityRepository: características declaradas y observadas.
- ImportRepository: lotes, archivos y resultados de normalización.
- AlgorithmAuditRepository: ejecuciones, entradas, razones y configuración.
- ClockProvider y TimeZoneProvider: tiempo determinista para uso y pruebas.
- FoodCatalogGateway: proveedor externo futuro de productos por código de barras.
- PhotoRecognitionGateway: proveedor futuro local o remoto, siempre reemplazable.

Los puertos anteriores representan capacidades objetivo. Fase 2 materializa únicamente `WeightRepository`, `NutritionDiaryRepository`, `NutritionPlanRepository`, `ClockProvider` y, si sus funciones están incluidas en la vertical, `MealTemplateRepository`. Fase 3 introduce los tres puertos mínimos de estimador, observaciones y memoria de decisión. `AlgorithmAuditRepository`, `ImportRepository`, repositorios fisiológicos y gateways remotos se difieren hasta su fase correspondiente.

### 8.5 Casos de uso principales

RegistrarPeso; CorregirPeso; ObtenerTendenciaPeso; RegistrarAlimento; RepetirFavorito; RepetirReciente; CargarComidaGuardada; CorregirConsumo; CerrarDiario; ConfigurarPlan; ObtenerResumenDiario; RegistrarSesiónManual; RegistrarEsfuerzoPercibido; SincronizarHealthConnect; InspeccionarExportación; ImportarHistorial; RecalcularBaseline; CalcularTdee; CalcularMorningReadiness; CalcularCurrentReadiness; ObtenerRecomendaciónDiaria; EvaluarPlan; AceptarPropuesta; RechazarPropuesta; ConsultarCalidad; InspeccionarEjecución.

### 8.6 Grafo de invalidación

~~~mermaid
flowchart TD
    W[Pesos] --> WT[Tendencia]
    N[Alimentos y cierre] --> DN[Totales diarios]
    A[Actividad] --> L[Carga]
    S[Sueño y fisiología] --> B[Baselines]
    WT --> T[TDEE]
    DN --> T
    B --> R[Readiness]
    L --> R
    L --> DR[Recomendación diaria]
    P[Plan vigente] --> DR
    P --> E[Evaluación del plan]
    WT --> E
    T --> ST[Estabilidad TDEE]
    ST --> E
    R --> E
    DN --> E
~~~

Una comida recalcula nutrición diaria y, cuando se cierra el día o cambia una ventana consolidada, TDEE y evaluación. Una comida aislada no recalcula artificialmente readiness. Un cambio de plan invalida recomendaciones y evaluaciones futuras, pero no reescribe observaciones históricas.

La arista `carga → recomendación diaria` está **inactiva** en modo `BASE_ONLY`: actividad y entrenamiento nunca cambian el objetivo diario inicial. La arista `readiness → evaluación` es contextual o bloqueante; no produce dirección energética. En fases 2 y 3 las ramas sin proveedor real simplemente no existen en la implementación. La invalidación puede comenzar mediante recomputación síncrona o por caso de uso; una cola durable solo se introduce cuando el volumen, la latencia o la recuperación de fallos la requieren.

---

## 9. E. Modelo de dominio

### 9.1 Value objects fundamentales

| Value object | Semántica e invariantes |
| --- | --- |
| PersonId, RecordId y SourceId | Identificadores estables; no dependen de IDs externos. |
| RecordedAt | Instante UTC y metadatos de zona/offset cuando sean necesarios. |
| CivilDay | Fecha civil más política de zona horaria. |
| TimeWindow | Inicio inclusivo, fin exclusivo, zona y finalidad analítica. |
| BodyMass | Masa almacenada en unidad entera consistente; nunca negativa ni nula por ausencia. |
| EnergyAmount | Energía en unidad interna entera y reproducible; ausencia distinta de cero. |
| NutrientAmount | Masa de nutriente con unidad explícita y posibilidad de valor desconocido. |
| VolumeAmount | Volumen explícito; no se convierte a masa sin densidad o porción verificable. |
| Quantity | Número, unidad, base de servicio y contexto de empaque. |
| EstimateRange | Extremos ordenados y método de construcción; opcional cuando no existe modelo defendible. |
| ConfidenceIndex | Valor entre 0 y 1, etiqueta y razones; no se interpreta como probabilidad calibrada. |
| DataOrigin | Proveedor, aplicación de origen, dispositivo y metadatos disponibles. |
| AcquisitionPath | Canal de ingreso; independiente de la naturaleza del valor. |
| ObservationNature | Autorreporte, sensor, etiqueta, estimación de proveedor, estimación propia o derivado. |
| QualityFlags | Conjunto de limitaciones o advertencias, sin reemplazar el valor observado. |
| AlgorithmIdentity | Nombre, versión semántica y versión de política. |
| InputReference | Identificador, revisión, origen y huella opcional de cada entrada. |
| EvidenceReason | Motivo estructurado con código, dirección, evidencia, contexto e incertidumbre. |

La representación interna de energía, masa y macros debe evitar acumulaciones no deterministas de números de punto flotante. Se recomienda almacenar energía en milicalorías, peso corporal en gramos enteros y masas nutricionales en miligramos, y redondear únicamente al presentar o mediante una política centralizada.

### 9.2 Observaciones y resultados

Una observación representa un evento registrado. Un resultado derivado representa una interpretación calculada. Un estado ausente representa que no existe información suficiente; no se inserta como observación numérica artificial.

Propiedades comunes de ObservationMetadata:

- recordId y personId.
- occurredAt, intervalo opcional y civilDay.
- sourceId, dataOrigin y acquisitionPath.
- observationNature y measurementMethod.
- confirmationStatus y qualityFlags.
- externalRecordId, externalRevision y payloadDigest cuando existan.
- importBatchId y localRevision.

Propiedades comunes de CalculationMetadata:

- calculationRunId.
- algorithmIdentity y policyVersion.
- computedAt y civilDay.
- analysisWindow y timezonePolicy.
- inputReferences o snapshot consistente de entradas.
- configurationDigest.
- confidenceIndex.
- evidenceReasons.

### 9.3 Peso

WeightMeasurement:

- masa observada.
- momento de pesaje.
- método: báscula manual declarada, peso importado o fuente externa.
- condiciones opcionales: mañana, ayunas, después de entrenar, ropa, báscula habitual.
- estado de revisión y banderas de posible outlier.

WeightTrend:

- tendencia estimada para fecha de referencia.
- pendiente en kg por semana.
- cambio equivalente semanal.
- cambio mensual observado solo si existe ventana suficientemente cubierta.
- ritmo mensual extrapolado, claramente etiquetado, cuando corresponda.
- intervalo operativo opcional.
- mediciones incluidas, excluidas y motivo de exclusión.
- distribución temporal, ruido, confianza y versión de algoritmo.

WeightObservationSelection representa la medición realmente elegida cuando existen varios pesajes en un día. WeightTrendPoint nunca se almacena como WeightMeasurement.

### 9.4 Productos, porciones y diario nutricional

FoodProduct:

- identificador, nombre, marca opcional y código de barras opcional.
- origen local, etiqueta ingresada o base externa.
- revisiones independientes de consumos históricos.

FoodProductVersion:

- base nutricional: por 100 g, por 100 ml o por porción.
- energía y macros conocidos.
- incertidumbre o calidad de etiqueta.
- densidad opcional y verificable.
- fecha de vigencia y procedencia.

FoodServing:

- unidad en g, ml, unidad o porción.
- relación explícita con base nutricional.
- contenido del envase opcional.
- fracción comestible opcional.

FoodEntry:

- momento y día civil.
- versión concreta del producto o alimento manual libre.
- cantidad consumida, no tamaño total del envase.
- nutrientes calculables para esa cantidad.
- naturaleza de cantidad: medida, declarada o estimada.
- naturaleza nutricional: etiqueta, base externa o estimación.
- estado: borrador, pendiente, confirmado o rechazado.
- grupo de comida opcional y referencias a correcciones.

Ejemplo contractual: un envase de Coca Zero de 1,5 L con ingesta de 500 ml se registra con packageSize = 1,5 L y consumedQuantity = 500 ml. La energía se calcula solamente para 500 ml. Si una ficha declara nutrientes por 100 g y el consumo se registra en ml, no se convierte sin densidad conocida.

SavedMeal y SavedMealItem representan una plantilla. Al registrar una comida guardada se crean entradas nuevas e independientes; modificar la plantilla no modifica registros previos.

FavoriteFood conserva producto, versión de referencia y porción habitual. RecentFood se construye a partir de consumo histórico o se materializa como caché local.

### 9.5 Completitud del diario

NutritionDiaryDay:

- fecha y zona.
- estado de cierre.
- cantidad de consumos confirmados, pendientes y estimados.
- energía confirmada calculable.
- energía estimada calculable.
- porcentaje estimado.
- macros conocidos, parciales o desconocidos.
- motivo de exclusión o revisión cuando corresponda.
- usuario/acción y momento de cierre.

Estados:

| Estado | Interpretación | ¿Puede alimentar TDEE adaptativo? |
| --- | --- | --- |
| OPEN | Día aún en registro. | No. |
| CLOSED_CONFIRMED | La persona declaró el día suficientemente completo. | Sí, sujeto a otras validaciones. |
| CLOSED_WITH_ESTIMATES | Día cerrado con estimaciones explícitas. | Sí, si la incertidumbre está bajo umbral. |
| CLOSED_INCOMPLETE | Falta información relevante. | No. |
| EXCLUDED_CONTEXT | Día excluido por enfermedad, viaje u otro evento justificado. | No; reduce cobertura de la ventana. |
| ZERO_INTAKE_CONFIRMED | Ingesta cero declarada explícitamente. | Revisión especial; no habilita recortes. |

Cerrar no convierte nutrientes estimados en medidos. Una edición posterior reabre o crea una revisión del cierre y vuelve a evaluar cálculos dependientes.

### 9.6 Actividad y fisiología

ActivitySession:

- instante inicial y final.
- modalidad: pole, ballet, fuerza, flexibilidad, caminata, carrera, otra o desconocida.
- origen y método de detección.
- calorías del proveedor, si existen, marcadas como estimadas.
- frecuencia cardíaca resumida, cuando tenga calidad suficiente.
- duplicidad o solapamientos conciliados.

SessionReflection:

- esfuerzo percibido opcional en escala 1–10.
- rendimiento percibido: mejor, esperado, peor o desconocido.
- dificultad técnica, fatiga o molestias opcionales.
- confirmación y calidad.

TrainingLoad:

- carga por sesión y día.
- método: duración × esfuerzo percibido, frecuencia cardíaca contextual u otra aproximación documentada.
- baseline por modalidad cuando exista.
- disponibilidad y confianza.

SleepSession y SleepStage:

- inicio, final, duración, fases opcionales, fuente y calidad.
- atribución al día de despertar.
- puntuación del proveedor únicamente si realmente está disponible.

PhysiologicalObservation:

- tipo: frecuencia cardíaca, FC en reposo, HRV RMSSD, estrés, Body Battery, VO₂ max u otro.
- valor, unidad, instante o intervalo.
- método, fuente, naturaleza y calidad.
- capacidad para representar métricas futuras sin convertirlas en dependencias obligatorias del dominio.

DailyActivitySummary agrega pasos, actividad y energía manteniendo separado total, activo y basal; ninguno se considera automáticamente medición exacta.

### 9.7 Recuperación

PersonalBaseline:

- métrica o grupo.
- ventana histórica.
- mediana, dispersión robusta y tamaño muestral.
- exclusiones y estado de madurez.
- referencia por modalidad, día de semana u otro contexto solo cuando la evidencia lo justifique.

MorningReadiness:

- día y hora ancla.
- categoría: GOOD, MODERATE, LOW o UNKNOWN.
- puntuación 0–100 excluida de la primera implementación; puede incorporarse posteriormente mediante migración y validación de utilidad incremental.
- dominios observados, ausentes y factores limitantes.
- baseline utilizado, confianza y razones.
- revisión posterior si una sincronización tardía incorpora datos de sueño; nunca modificar silenciosamente una lectura previa.

CurrentReadiness:

- referencia a MorningReadiness.
- eventos intradiarios realmente observados.
- impacto limitado de sesiones, estrés agregado o siestas, si existen datos fiables.
- momento del cálculo, confianza y causas.

ReadinessSnapshot es una capacidad objetivo de persistencia para cualquiera de las dos variantes. Fase 4 puede guardar únicamente el último resultado categórico con sus factores o calcularlo bajo demanda; múltiples snapshots intradiarios aparecen solo cuando Current Readiness los necesita. Body Battery, estrés y HRV no se agregan como evidencia independiente sin corregir correlación entre variables derivadas por el mismo proveedor.

### 9.8 Plan, gasto y evaluación

NutritionPlanVersion:

- objetivo: MAINTENANCE, LOSS, GAIN, RECOMPOSITION o PERFORMANCE_PRIORITY.
- energía base diaria.
- objetivo proteico y otros macros opcionales.
- ritmo de cambio deseado únicamente cuando sea compatible con el objetivo.
- límite inferior personal explícito y otros guardrails configurados.
- vigencia, versión, motivo y forma de aceptación.

RECOMPOSITION no afirma cambios de masa magra sin mediciones apropiadas. PERFORMANCE_PRIORITY impide recortes automáticos y prioriza recuperación y suficiencia.

TdeeEstimate:

- tipo: USER_PROVIDED, POPULATION_PRIOR, WEARABLE_CONTEXT, OBSERVATIONAL o BLENDED.
- valor central redondeable.
- rango operativo y método de construcción, si existe.
- ventana, días nutricionales elegibles, tendencia de peso usada y coeficiente energético considerado.
- índice de confianza y limitaciones.
- estabilidad del resultado como dimensión independiente de la calidad de datos.
- revisión de inputs y referencia al historial mínimo usado para comprobar consistencia temporal.

EstimatorStability:

- estado: `INSUFFICIENT_HISTORY`, `UNSTABLE`, `STABILIZING` o `STABLE`.
- intervalo cronológico y número de estimaciones realmente obtenidas en días distintos.
- centro robusto, dispersión relativa, amplitud pico a pico, deriva entre períodos y cambios direccionales relevantes.
- cobertura temporal efectiva; las ventanas deslizantes muy solapadas no se cuentan como observaciones independientes.
- motivo estructurado, política aplicada, revisión de entradas y autorización derivada para asesoramiento.

TdeeEstimate y EstimatorStability nunca se sustituyen entre sí: `confianza_inputs = ALTA` junto con `estabilidad = UNSTABLE` sigue bloqueando ajustes nutricionales.

EnergyBalance:

- ingesta observada o estimada.
- TDEE estimado.
- diferencia esperada con su incertidumbre.
- relación con objetivo, sin presentarse como medición directa.

DailyRecommendation:

- plan base aplicable.
- modo: `BASE_ONLY` o `ADAPTIVE`; la primera versión admite exclusivamente `BASE_ONLY`.
- ajustes de actividad excepcional, si están autorizados.
- recomendado diario y límites aplicados.
- total confirmado, estimado y pendiente.
- restante respecto de confirmado y restante respecto de total probable, cuando proceda.
- razones y confianza.

PlanEvaluation:

- modo de ejecución: `SHADOW` o `ADVISORY`.
- estado candidato producido por la evidencia.
- estado efectivo después de histéresis y compuertas de seguridad.
- estado operativo visible y autorización, si el modo permite asesoramiento.
- plan evaluado y ventana.
- objetivo, evolución observada, TDEE disponible, recuperación, carga y rendimiento observados.
- calidad por dimensión.
- estabilidad del estimador y memoria de transición por versión de plan.
- razones a favor, motivos de bloqueo y factores confusores.
- estado de seguridad y autorización de propuesta.

EvaluationMode:

- `SHADOW`: evaluación hipotética, no operativa y sin creación de propuestas.
- `ADVISORY`: evaluación validada que puede generar una propuesta conservadora y manualmente aceptable.
- transición registrada con fecha, revisión de política, criterios de salida y acción explícita del usuario.

EvaluationObservation:

- fecha de observación, versión de plan y modo.
- algoritmo, política y ventana analítica.
- estado candidato, estado efectivo y carácter operativo.
- TDEE redondeado, estado de estabilidad, cobertura de peso y nutrición, e incertidumbre agregada.
- códigos de razones, bloqueos y revisión lógica de inputs.
- sin identificación individual exhaustiva ni hashes obligatorios durante las primeras fases.

DecisionStateMemory:

- versión de plan, estado efectivo anterior y fecha de entrada.
- conteo de evaluaciones direccionales calificadas en días diferentes.
- última transición, banda vigente y evidencia que mantiene la persistencia.
- invalidación inmediata ante cambio de plan, política incompatible, riesgo o corrección retrospectiva relevante.

AdjustmentProposal:

- plan anterior y energía propuesta.
- delta, límite aplicado y razones estructuradas.
- estado: PENDING, ACCEPTED, REJECTED, DISMISSED o EXPIRED.
- evidencia y evaluación que originaron la propuesta.
- fecha de expiración o invalidez si cambian datos relevantes.

### 9.9 Seguridad, contexto y calidad

ContextEvent: enfermedad, lesión, viaje, cambio de horario, entrenamiento excepcional, estrés elevado declarado, alteración menstrual voluntariamente registrada, síntomas de baja disponibilidad energética u otro evento significativo.

SafetyStatus: CLEAR, CAUTION o REVIEW_REQUIRED. Es ortogonal al estado de evaluación; no implica diagnóstico.

DecisionAuthorization: BLOCKED, MAINTAIN_ONLY, OBSERVE_ONLY o PROPOSAL_ALLOWED.

DataQualityAssessment: dimensión, ventana, cobertura, completitud, frescura, incertidumbre, consistencia, origen, limitantes y confianza.

AlgorithmRun y AlgorithmInput: arquitectura objetivo de ejecución reproducible con versión, configuración, orden de entradas y resultado. No forman parte del esquema ni de los puertos iniciales; `EvaluationObservation` y los metadatos embebidos satisfacen proporcionalmente la trazabilidad hasta que una necesidad concreta justifique normalización avanzada.

### 9.10 Relaciones fundamentales

- Una persona tiene múltiples mediciones, diarios, sesiones y versiones de plan.
- Una versión de producto puede respaldar múltiples entradas; cada entrada conserva su versión efectiva.
- Un diario agrupa entradas por día civil.
- Una actividad puede tener cero o una reflexión subjetiva vigente.
- Un baseline se calcula a partir de múltiples observaciones auditables.
- Un TDEE requiere una tendencia de peso y varios diarios elegibles, o declara que es únicamente un prior.
- Una evaluación referencia una versión de plan y un conjunto de resultados/observaciones con revisiones explícitas.
- Una propuesta referencia exactamente una evaluación y, si se acepta, crea una nueva versión de plan.

---

## 10. F. Modelo de persistencia Room

### 10.1 Criterios generales

1. Room es la fuente de verdad de la interfaz y del historial local.
2. Observaciones, derivados y auditoría pueden separarse en la arquitectura objetivo; en cada fase solo se crean las tablas efectivamente necesarias y una traza mínima puede permanecer embebida.
3. Las magnitudes numéricas se guardan preferentemente como enteros escalados.
4. Los instantes se guardan como epoch UTC; la zona y fecha civil se conservan separadamente.
5. Los campos ausentes permanecen nulos o se modelan con estado explícito; no se sustituyen por cero.
6. Los índices cubren persona, día, instante, estado, origen e IDs externos.
7. Las revisiones se conservan de forma trazable; el mecanismo exacto se decide antes de implementar DAO.
8. No se utiliza migración destructiva para una base con datos personales reales.
9. Una huella de payload facilita idempotencia, pero no sustituye IDs de fuente cuando estos existen.

### 10.1.1 Esquema mínimo frente al esquema objetivo

| Fase | Tablas efectivamente necesarias | Tablas explícitamente diferidas | Justificación |
| --- | --- | --- | --- |
| 1. Dominio mínimo. | Ninguna tabla de producto es obligatoria antes de necesitar persistencia. | Todo el esquema Room. | El dominio puro y sus invariantes no dependen de Android. |
| 2a. Primera vertical útil. | `user_profiles`, `nutrition_plan_versions`, `weight_measurements`, `food_products`, `food_entries`, `nutrition_diary_days`. | Fuentes, sincronización, derivados, importación y auditoría. | Persistir plan, peso y alimentación; producir panel offline. |
| 2b. Reducción de fricción. | `favorite_foods`, `saved_meals`, `saved_meal_items` si esas funciones se incluyen. | Tabla de recientes, snapshots diarios y agrupadores complejos. | Recientes se obtiene desde `food_entries`; porciones simples pueden permanecer embebidas. |
| 3a. Tendencia. | Ninguna tabla derivada adicional si se recalcula bajo demanda. | `weight_trend_snapshots` hasta que su historial o rendimiento lo requieran. | Una tendencia se reconstruye a partir de observaciones reales. |
| 3b. TDEE y estabilidad. | `tdee_estimates`, con una observación por fecha y revisiones necesarias. | `algorithm_runs`, `algorithm_run_inputs`, baselines generales y cola durable. | No es posible medir estabilidad temporal sin conservar un historial mínimo de estimaciones. |
| 3c–3.5. Evaluación en sombra. | `plan_evaluations` con modo y resumen embebido; `decision_state_memory` por plan y política. | `adjustment_proposals`, `decision_reasons` normalizada y auditoría avanzada. | Shadow necesita historial no operativo; histéresis necesita memoria; ninguna propuesta existe todavía. |
| Asesoramiento validado. | `adjustment_proposals` o equivalente solo al habilitar `ADVISORY`. | Automatización y aplicación automática. | La aceptación manual aparece únicamente cuando existen propuestas reales. |
| 4. Health Connect y recuperación. | Fuentes, capacidades, sesiones, sueño o fisiología solo para tipos empíricamente observados. | Series, tablas y snapshots de métricas inexistentes; score de readiness. | La muestra del teléfono define la granularidad necesaria. |
| 5. Importación Garmin. | Lotes, artefactos y vínculos mínimos del formato elegido. | Parsers y tablas de formatos todavía no verificados. | Spike B determina el alcance real. |
| 6–7. Intradía y proveedores externos. | Snapshots intradiarios, caché remota u otros recursos solo cuando se implemente la funcionalidad. | Auditoría exhaustiva o fotografía hasta justificarla. | Cada nueva capacidad llega con una migración probada y proporcional. |

**Inmutabilidad histórica sin sobreingeniería:** fase 2 puede guardar un snapshot nutricional inmutable directamente dentro de cada `food_entry`; así una edición posterior del producto no altera consumos anteriores. `food_product_versions` se introduce cuando edición avanzada, reutilización o auditoría lo hagan necesario. Lo obligatorio desde el primer día es la semántica histórica, no una tabla concreta.

Los registros manuales iniciales pueden conservar `source_kind = MANUAL` y metadatos básicos dentro de su propia fila; `data_sources` y claves externas aparecen al incorporar una segunda fuente real. De igual manera, versiones de algoritmo/política, ventana y motivos pueden residir en columnas o JSON estructurado local mientras no exista justificación para tablas normalizadas.

### 10.2 Perfil, fuentes y sincronización

| Tabla | Clave principal | Campos relevantes | Índices y restricciones |
| --- | --- | --- | --- |
| user_profiles | profile_id | display_name opcional, height_mm opcional, birth_year opcional, timezone_policy, created_at. | Perfil activo único en MVP. |
| data_sources | source_id | provider_kind, channel_kind, source_package, device_label, availability_state. | Índice por provider_kind y source_package. |
| source_capabilities | capability_id | source_id, metric_type, schema_status, declared_status, observed_status, checked_at. | UNIQUE source_id + metric_type. |
| sync_cursors | cursor_id | source_id, record_type, changes_token, last_success_at, token_state. | UNIQUE source_id + record_type. |
| sync_runs | sync_run_id | source_id, started_at, ended_at, status, inserted_count, updated_count, deleted_count, error_code. | Índice source_id + started_at. |
| external_record_links | link_id | source_id, external_record_id, local_record_type, local_record_id, external_revision, digest. | UNIQUE source_id + external_record_id + local_record_type. |

### 10.3 Importaciones

| Tabla | Clave principal | Campos relevantes | Índices y restricciones |
| --- | --- | --- | --- |
| import_batches | batch_id | started_at, finished_at, source_label, status, files_seen, records_seen, records_imported. | Índice status + started_at. |
| import_artifacts | artifact_id | batch_id, original_filename, mime_type, format_type, sha256, size_bytes, parser_version, status. | Índice sha256; FK batch_id. |
| import_record_links | import_link_id | artifact_id, record_offset, logical_record_key, normalized_type, normalized_record_id, status. | UNIQUE artifact_id + logical_record_key. |
| import_issues | issue_id | batch_id, artifact_id opcional, severity, issue_code, record_reference, redacted_message. | Índice batch_id + severity. |

Guardar archivos originales dentro de la aplicación debe ser opcional por privacidad y tamaño. Si se descartan, mantener metadatos, huella e información suficiente para explicar cobertura, sin afirmar reproducibilidad byte a byte cuando ya no exista el archivo.

### 10.4 Peso y contexto

| Tabla | Clave principal | Campos relevantes | Índices y restricciones |
| --- | --- | --- | --- |
| weight_measurements | weight_id | profile_id, logical_weight_id, occurred_at, recorded_at, civil_day, zone_id, body_mass_g, source_id nullable, source_kind, method, condition_flags, revision, supersedes_id nullable, record_state. | Índices profile_id + occurred_at y profile_id + civil_day; UNIQUE logical_weight_id + revision; deduplicación por origen externo cuando exista. |
| context_events | context_event_id | profile_id, event_type, start_at, end_at, severity, user_confirmed, notes_sensitive opcionales. | Índices profile_id + start_at y event_type + start_at. |
| performance_observations | performance_id | activity_id opcional, civil_day, modality, perceived_performance, effort_rating, user_confirmed. | Índices activity_id y profile_id + civil_day. |

### 10.5 Productos y diario nutricional

| Tabla | Clave principal | Campos relevantes | Índices y restricciones |
| --- | --- | --- | --- |
| food_products | product_id | display_name, normalized_name, barcode opcional, brand, source_kind, nutrient_basis, energy_mkcal nullable, protein_mg nullable, carbs_mg nullable, fat_mg nullable, revision, current_version_id nullable. | Índices barcode, normalized_name y source_kind; versionado externo diferido si el snapshot de cada entrada preserva historia. |
| food_product_versions | product_version_id | product_id, revision, nutrient_basis, energy_mkcal, protein_mg, carbs_mg, fat_mg, density opcional, uncertainty_kind, created_at. | UNIQUE product_id + revision; FK product_id. |
| food_servings | serving_id | product_id, label, quantity_scaled, unit, basis_conversion, package_size opcional. | Índice product_id. |
| favorite_foods | favorite_id | profile_id, product_id, serving_id opcional, preferred_quantity, preferred_unit, last_used_at. | UNIQUE profile_id + product_id + serving_id cuando corresponda; porción simple embebida sin exigir tabla adicional. |
| saved_meals | saved_meal_id | profile_id, name, created_at, updated_at, archived_at. | Índice profile_id + updated_at. |
| saved_meal_items | saved_meal_item_id | saved_meal_id, product_id, product_version_id opcional, serving_id opcional, quantity_scaled, unit, ordering. | Índice saved_meal_id + ordering; no requiere tabla de porciones si la unidad está embebida. |
| meal_groups | meal_group_id | profile_id, civil_day, label, saved_meal_id opcional, consumed_at. | Índice profile_id + civil_day. |
| food_entries | food_entry_id | profile_id, logical_entry_id, meal_group_id nullable, product_version_id nullable, product_id, occurred_at, recorded_at, civil_day, quantity, unit, energy_mkcal nullable, macros nullable, nutrient_snapshot, confirmation_status, quantity_nature, nutrient_nature, revision, supersedes_id nullable. | Índices profile_id + civil_day, product_id, product_version_id y confirmation_status; UNIQUE logical_entry_id + revision. |
| nutrition_diary_days | diary_day_id | profile_id, civil_day, closure_state, closed_at, updated_at, closure_revision, closure_history embebido, exclusion_reason. | UNIQUE profile_id + civil_day; historial mínimo de cierres sin tabla de auditoría anticipada. |
| daily_nutrition_snapshots | nutrition_snapshot_id | profile_id, civil_day, confirmed_energy, estimated_energy, pending_energy nullable, macro_totals, completeness_score, algorithm_version, policy_version, algorithm_run_id nullable. | Índice profile_id + civil_day + calculated_at; tabla diferida mientras el agregado pueda recalcularse bajo demanda. |

Recientes puede comenzar como consulta indexada sobre food_entries; no requiere tabla adicional hasta demostrar una necesidad real de rendimiento.

### 10.6 Actividad, sueño y fisiología

| Tabla | Clave principal | Campos relevantes | Índices y restricciones |
| --- | --- | --- | --- |
| activity_sessions | activity_id | profile_id, start_at, end_at, civil_day, modality, source_id, external_id, active_energy nullable, quality_flags, revision. | Índices profile_id + start_at, profile_id + civil_day y source_id + external_id. |
| session_reflections | reflection_id | activity_id, effort_rating nullable, perceived_performance, soreness_flag, created_at, revision. | Índice activity_id; una reflexión vigente por sesión. |
| daily_activity_summaries | activity_summary_id | profile_id, civil_day, steps nullable, active_energy nullable, total_energy nullable, basal_energy nullable, sources_digest. | Índice profile_id + civil_day. |
| sleep_sessions | sleep_id | profile_id, start_at, end_at, wake_civil_day, source_id, duration_seconds, sleep_score nullable, quality_flags, revision. | Índices profile_id + wake_civil_day y source_id + external_id. |
| sleep_stages | stage_id | sleep_id, start_at, end_at, stage_kind, quality_flags. | Índice sleep_id + start_at. |
| physiological_observations | observation_id | profile_id, metric_type, occurred_at, civil_day, numeric_value_scaled, unit, source_id, observation_nature, quality_flags, revision. | Índices profile_id + metric_type + occurred_at y source_id + external_id. |
| heart_rate_series | series_id | profile_id, start_at, end_at, source_id, sample_count, quality_flags. | Índice profile_id + start_at. |
| heart_rate_samples | sample_id | series_id, occurred_at, bpm, quality_flags. | Índice series_id + occurred_at; política de retención explícita. |

La tabla genérica physiological_observations se limita a métricas cuantitativas extensibles; las estructuras complejas y consultadas intensivamente, como sueño y sesiones, conservan tablas propias. Evitar un único modelo EAV para toda la aplicación.

### 10.7 Planes, derivados y decisiones

| Tabla | Clave principal | Campos relevantes | Índices y restricciones |
| --- | --- | --- | --- |
| nutrition_plan_versions | plan_version_id | profile_id, objective_kind, base_energy, protein_target, rate_target, personal_floor nullable, starts_at, ends_at nullable, accepted_by_user. | Índice profile_id + starts_at; una versión activa por perfil. |
| personal_baselines | baseline_id | profile_id, metric_type, window_start, window_end, center_scaled, spread_scaled, sample_count, confidence, algorithm_version, policy_version, algorithm_run_id nullable. | Índice profile_id + metric_type + window_end. |
| weight_trend_snapshots | trend_id | profile_id, reference_day, trend_mass, weekly_rate, monthly_observed_change nullable, confidence, algorithm_version, policy_version, algorithm_run_id nullable. | Índice profile_id + reference_day + algorithm_version; diferida mientras la tendencia se calcule bajo demanda. |
| tdee_estimates | tdee_id | profile_id, reference_day, estimate_kind, central_energy, low_energy nullable, high_energy nullable, confidence, stability_status, relative_dispersion nullable, peak_to_peak_ratio nullable, window_start, window_end, algorithm_version, policy_version, input_revision, algorithm_run_id nullable. | Índices profile_id + reference_day y profile_id + reference_day + input_revision; orden cronológico determinista. |
| training_load_snapshots | load_id | profile_id, reference_day, acute_load nullable, chronic_load nullable, modality, confidence, algorithm_version, policy_version, algorithm_run_id nullable. | Índice profile_id + reference_day + modality. |
| readiness_snapshots | readiness_id | profile_id, readiness_kind, reference_day, readiness_category, confidence, factor_summary, algorithm_version, algorithm_run_id nullable, supersedes_id nullable; score solo mediante migración futura justificada. | Índices profile_id + reference_day + readiness_kind y calculated_at. |
| daily_recommendations | recommendation_id | profile_id, reference_day, plan_version_id, recommendation_mode, base_energy, daily_energy, activity_adjustment nullable, confidence, algorithm_run_id nullable. | Índice profile_id + reference_day + plan_version_id; tabla innecesaria mientras BASE_ONLY se derive del plan. |
| data_quality_assessments | quality_id | profile_id, dimension, window_start, window_end, confidence, coverage, completeness, limiting_factor, algorithm_version, policy_version, algorithm_run_id nullable. | Índice profile_id + dimension + window_end; diferida si el resultado se incorpora en una evaluación existente. |
| plan_evaluations | evaluation_id | profile_id, plan_version_id, reference_day, execution_mode, candidate_state, effective_state, operational, hypothetical_delta nullable, stability_status, safety_status, authorization, confidence, algorithm_version, policy_version, input_revision, coverage_summary, reason_codes, algorithm_run_id nullable. | Índices profile_id + reference_day, plan_version_id + reference_day y execution_mode + reference_day. |
| decision_state_memory | plan_version_id | profile_id, policy_version, effective_state, entered_at, qualified_evaluation_count, last_transition_at, evidence_revision, blocked_reason nullable. | PK plan_version_id; índice profile_id + last_transition_at; reinicio ante cambio de plan. |
| adjustment_proposals | proposal_id | evaluation_id, current_energy, proposed_energy, delta_energy, proposal_state, created_at, expires_at, resolved_at. | Índice evaluation_id y proposal_state + created_at. |
| plan_change_events | change_event_id | profile_id, previous_plan_id, next_plan_id, proposal_id nullable, accepted_at, reason_code. | Índice profile_id + accepted_at. |

### 10.8 Auditoría y recalculación: arquitectura objetivo diferida

| Tabla | Clave principal | Campos relevantes | Índices y restricciones |
| --- | --- | --- | --- |
| algorithm_runs | run_id | algorithm_name, algorithm_version, policy_version, computed_at, window_start, window_end, config_digest, timezone_policy, input_digest, result_digest. | Índices algorithm_name + computed_at y algorithm_name + algorithm_version. |
| algorithm_run_inputs | run_input_id | run_id, input_type, input_id, input_revision, source_id, ordering, digest. | UNIQUE run_id + ordering; índice input_type + input_id. |
| decision_reasons | reason_id | run_id, reason_code, direction, metric_type, observed_value, baseline_value, unit, severity, evidence_confidence, contribution, ordering. | Índice run_id + ordering. |
| recalculation_queue | queue_id | profile_id, affected_domain, from_day, reason_code, enqueued_at, processing_state. | Índice processing_state + enqueued_at; coalescencia por dominio y ventana. |
| audit_events | audit_event_id | profile_id, event_type, occurred_at, entity_type, entity_id, redacted_payload, privacy_class. | Índice profile_id + occurred_at. |

`algorithm_runs`, `algorithm_run_inputs`, `decision_reasons`, `recalculation_queue` y `audit_events` no se incluyen en fases 1–3.5 por existir en este catálogo. Solo se añaden cuando una funcionalidad activa no puede satisfacerse de forma segura mediante trazas embebidas, observaciones mínimas y recomputación controlada.

No es necesario crear todas las tablas futuras en la primera migración. El orden de implantación sigue el roadmap y conserva la capacidad de expansión mediante migraciones probadas. Ningún DAO inicial puede depender de tablas que todavía no existen. [Migraciones y pruebas oficiales de Room](https://developer.android.com/training/data-storage/room/migrating-db-versions).

---

## 11. G. Motores de cálculo

### 11.1 Reglas comunes

Cada motor:

1. Recibe un conjunto explícito de entradas normalizadas y una configuración versionada.
2. No accede a Android, Room, Garmin, red o reloj global.
3. Declara ventana temporal, supuestos, resultado, incertidumbre y razones.
4. Trata ausencia como ausencia y conserva exclusiones identificables.
5. Produce exactamente el mismo resultado con las mismas entradas ordenadas.
6. Puede devolver NOT_AVAILABLE o INSUFFICIENT_DATA sin lanzar un error de negocio.
7. No emite diagnósticos ni inferencias causales.

### 11.2 BaselineCalculator

**Propósito:** describir el comportamiento reciente y habitual de cada señal sin confundirlo con una referencia poblacional.

**Entradas:** observaciones de una misma métrica, ventanas, banderas contextuales, calidad, modalidad opcional y configuración.

**Estrategia inicial:**

- Utilizar mediana como centro y desviación absoluta mediana como dispersión robusta.
- Ventana preferida de 21–42 días según señal y disponibilidad.
- Excluir, cuando esté justificado, registros corruptos, duplicados y días con contexto incompatible.
- Separar señales nocturnas de diurnas y modalidades de entrenamiento distintas.
- Evitar incorporar el mismo día evaluado a su baseline de comparación cuando eso oculte una desviación reciente.
- Reducir confianza si existen demasiados días consecutivos sin información o cambios importantes de contexto.

**Umbrales iniciales versionados:**

| Señal | Ventana inicial | Mínimo para resultado provisional | Mínimo preferido para resultado alto |
| --- | --- | --- | --- |
| Sueño | 21 días | 7 noches distribuidas | 14 o más noches. |
| HRV nocturna | 28 días | 10 noches distribuidas | 18 o más noches y al menos 21 días de cobertura. |
| FC en reposo | 21 días | 7 observaciones | 14 observaciones distribuidas. |
| Carga total | 28 días | 10 días contextualizados | 21 días y modalidades identificables. |
| Actividad habitual | 28 días | 10 días | 21 días, evitando duplicación de fuentes. |

Estos mínimos son políticas iniciales de software y deben calibrarse contra datos reales; no son umbrales clínicos universales.

### 11.3 WeightTrendCalculator

**Propósito:** estimar tendencia y ritmo de cambio a partir de pesajes reales e irregulares.

**Entradas:** mediciones vigentes, condiciones de pesaje, fecha de referencia, ventana, eventos de contexto y configuración.

**Preprocesamiento:**

1. Ordenar por instante y revisar duplicados externos.
2. Si hay varios registros el mismo día, conservar todos y elegir una observación analítica representativa mediante prioridad de condiciones habituales.
3. Detectar candidatos a outlier con mediana local y dispersión robusta, conservando siempre el registro original.
4. No excluir automáticamente un desplazamiento persistente observado en varias mediciones; marcar posible cambio de régimen.
5. Asignar menor confianza a mediciones realizadas en condiciones inhabituales sin declararlas automáticamente falsas.

**Método inicial recomendado:** pendiente robusta tipo Theil–Sen sobre observaciones reales dentro de una ventana de 21–35 días, acompañada de intercepto robusto. La complejidad cuadrática es aceptable porque se limita la cantidad de pesajes por ventana.

Definición conceptual:

~~~text
pendiente_diaria = mediana de las pendientes válidas entre pares de pesajes
ritmo_semanal = pendiente_diaria × 7
tendencia_en_fecha = ajuste robusto evaluado en la fecha de referencia
~~~

**Disponibilidad:**

- Con menos de cinco días de pesaje distintos o menos de diez días entre extremos: tendencia provisional o no disponible para decisiones.
- Con cinco a siete pesajes distribuidos y al menos 14 días: tendencia moderada.
- Con ocho o más pesajes, al menos 21 días, distribución temporal razonable y ruido controlado: elegible para confianza alta.
- Último peso observado siempre puede mostrarse aunque la tendencia no sea confiable.

**Salidas:** último peso real, tendencia suavizada, ritmo semanal, variabilidad, mediciones incluidas/excluidas, confianza y razones.

**Cambio mensual:** solo se presenta como cambio observado si hay información suficiente en ambos extremos de una ventana aproximada de 30 días. Si se muestra una proyección derivada del ritmo, debe identificarse como ritmo mensual equivalente y no como cambio realmente medido.

**Prohibiciones:**

- No crear pesajes en días faltantes.
- No convertir interpolaciones en observaciones.
- No utilizar un único peso extremo para justificar corrección del plan.
- No inferir cambios de composición corporal desde peso únicamente.

### 11.4 NutritionAggregationEngine

**Propósito:** construir un resumen diario sin ocultar incertidumbre ni confundir estados de confirmación.

**Entradas:** versiones vigentes de FoodEntry, estado del diario, cantidades, fichas nutricionales y zona civil.

**Resultados separados:**

- Energía confirmada calculable.
- Energía estimada adicional calculable.
- Energía pendiente de confirmación, cuando sea cuantificable.
- Total probable: confirmado más estimado aceptado.
- Nutrientes confirmados, estimados y desconocidos.
- Proporción estimada y motivos de incertidumbre.
- Estado de completitud del día.

Los grupos energéticos utilizados en sumas son mutuamente excluyentes. Una entrada que el usuario confirmó pero cuya cantidad o composición sigue estimada pertenece al grupo estimado aceptado; no se suma simultáneamente al grupo confirmado de menor incertidumbre. Confirmación del registro y clasificación epistemológica siguen siendo ejes separados aunque la interfaz agrupe montos sin duplicarlos.

**Reglas:**

1. Una cantidad confirmada no vuelve exacta una etiqueta incompleta.
2. Una energía desconocida no se agrega como cero.
3. Una comida pendiente no se incluye automáticamente en el total confirmado.
4. Un rango energético solo se informa si existe un método explícito para construirlo.
5. La edición de una entrada invalida el agregado previo y puede reabrir el diario.
6. Favoritos, recientes y plantillas generan entradas normales, no vías especiales de cálculo.

### 11.5 DataQualityEvaluator

**Propósito:** determinar qué inferencias están justificadas y qué acciones se encuentran autorizadas.

**Dimensiones mínimas:**

- Cobertura temporal.
- Completitud de nutrición.
- Distribución de mediciones de peso.
- Frescura.
- Incertidumbre de cantidades y alimentos.
- Coherencia y deduplicación de actividad.
- Madurez de baselines fisiológicos.
- Contexto de rendimiento y seguridad.
- Estabilidad del plan vigente.
- Estabilidad del resultado TDEE, registrada por separado y sin confundirla con cobertura o completitud de sus entradas.

**Política inicial de confianza:** utilizar factores explícitos y límites por eslabón débil; una excelente cobertura Garmin no puede compensar un diario nutricional incompleto.

Ejemplo conceptual para peso:

~~~text
factor_pesajes = limitar(días_con_pesaje / pesajes_objetivo_del_perfil, 0, 1)
factor_ventana = limitar(días_entre_extremos / ventana_objetivo_del_perfil, 0, 1)
factor_frescura = degradación por antigüedad del último peso
factor_distribución = cobertura de primera y segunda mitad de la ventana
factor_ruido = penalización documentada por dispersión y condiciones variables

confianza_peso = mínimo de los factores aplicables
~~~

Los perfiles de decisión son explícitos: observación o aumento prudente pueden emplear una referencia preliminar de aproximadamente 14 días y seis pesajes distribuidos; un recorte exige la referencia estricta de aproximadamente 21 días y ocho pesajes. Así, el umbral mínimo de 14 días para evaluar una posible insuficiencia no entra en contradicción con la exigencia más alta impuesta a una reducción.

Ejemplo conceptual para nutrición:

~~~text
factor_cierre = días_completos_elegibles / días_requeridos
factor_estimación = 1 - 0,5 × proporción_energética_estimada
factor_pendientes = penalización por entradas pendientes o macros críticos desconocidos
factor_consistencia = reducción ante días anómalos no explicados

confianza_nutrición = mínimo de los factores aplicables
~~~

La fórmula exacta y sus constantes pertenecen a una PolicyVersion; deben verse y modificarse sin alterar silenciosamente evaluaciones históricas.

**Etiquetas iniciales visibles:**

| Índice | Etiqueta de producto |
| --- | --- |
| Menor que 0,40 | Insuficiente. |
| Desde 0,40 hasta 0,59 | Baja. |
| Desde 0,60 hasta 0,74 | Moderada. |
| Desde 0,75 | Alta. |

El índice es un indicador operacional de calidad y consistencia; no representa probabilidad estadística, intervalo de credibilidad ni certeza fisiológica. En la pantalla ordinaria se muestra la etiqueta cualitativa junto con evidencia concreta: por ejemplo, `9 pesajes / 28 días`, `25 días nutricionales completos` y `18 % de ingesta estimada`. El valor interno `0,82` se reserva para políticas, tests e inspector avanzado; no se transforma en `82 % de confianza`.

`confianza_datos = ALTA` y `estabilidad_estimador = UNSTABLE` son resultados perfectamente compatibles. La primera describe insumos; la segunda describe el comportamiento del modelo. Ninguna dimensión compensa automáticamente una deficiencia de la otra.

### 11.6 TdeeEstimator

**Propósito:** estimar gasto energético diario personal con progresiva dependencia de evidencia observada.

**Fuentes posibles:**

1. Valor manual configurado por la persona.
2. Prior poblacional opcional, calculado únicamente si se proporcionan voluntariamente los datos requeridos.
3. Calorías del wearable como contexto auxiliar de baja confianza.
4. Ingesta real suficientemente registrada y pendiente robusta de peso en una ventana comparable.

Si no existe perfil suficiente para una ecuación poblacional, no se inventan edad, sexo, composición corporal ni TDEE. El plan base todavía puede funcionar porque es una configuración independiente.

**Modelo observacional inicial:**

~~~text
ritmo_peso_diario = cambio_tendencial_kg / días_de_ventana
tdee_observacional = ingesta_media_elegible - k × ritmo_peso_diario
~~~

Si la ingesta media es 2100 kcal/día y el ritmo observado es -0,35 kg/semana, una aproximación simple con k = 7700 kcal/kg produce aproximadamente 2485 kcal/día. Ese coeficiente es una heurística inicial, no una constante biológica exacta ni un modelo dinámico validado para cada persona. Los modelos dinámicos de balance energético muestran limitaciones de reglas estáticas. [Quantification of the effect of energy imbalance on bodyweight](https://pubmed.ncbi.nlm.nih.gov/21872751/).

**Elegibilidad de días:**

- Día cerrado y confirmado, o cerrado con estimaciones dentro del umbral.
- Ausencia de eventos que invaliden su comparabilidad.
- Correspondencia con la ventana de tendencia de peso.
- Sin confundir plan objetivo con ingesta realmente consumida.
- Sin incorporar días abiertos, no registrados o explicitados como incompletos.

**Madurez inicial:**

| Estado | Condición orientativa | Presentación |
| --- | --- | --- |
| UNAVAILABLE | No existe prior confiable ni datos contemporáneos. | No disponible. |
| PRIOR_ONLY | Solo hay valor manual, ecuación o wearable contextual. | Estimación inicial, confianza limitada. |
| PROVISIONAL | Aproximadamente 14 días y suficiente calidad mínima. | Estimación provisional con rango amplio. |
| ADAPTIVE | Aproximadamente 21–28 días comparables. | Estimación personal con rango operativo. |
| HIGH_QUALITY | 28 o más días, buena nutrición y tendencia distribuida. | Mayor confianza, sin falsa precisión. |

**Construcción del rango operativo:** combinar escenarios de incertidumbre de ingesta, pendiente de peso, dispersión y coeficiente energético. Si no hay una base defendible para calcular extremos, mostrar solamente confianza cualitativa. El rango no debe denominarse intervalo de confianza estadístico.

**Protecciones:**

- Limitar la velocidad de actualización del estimador.
- Reducir influencia de semanas anómalas, cambios agudos de agua y comidas ampliamente estimadas.
- Mezclar prior y observación con una ponderación explícita que aumente solo cuando mejora la calidad.
- No usar energía activa Garmin como gasto absoluto definitivo.
- No sumar gasto basal y total cuando el total ya incluye componentes activos y basales.
- No retroalimentar el objetivo recomendado como si fuera ingesta observada.

### 11.6.1 EstimatorStabilityCalculator

**Propósito:** impedir que un estimador todavía oscilante autorice cambios del plan aunque las observaciones de peso y nutrición tengan buena cobertura.

**Entradas mínimas:** estimaciones cronológicas por días civiles distintos, versión del algoritmo, versión de política, revisión de inputs, ventana utilizada, calidad de cada cálculo y eventos de cambio de régimen.

**Métricas independientes:**

1. Mediana robusta del TDEE durante el período evaluado.
2. Dispersión relativa robusta: `MAD / mediana`.
3. Amplitud pico a pico relativa: `(máximo - mínimo) / mediana`.
4. Deriva relativa entre dos períodos cronológicos consecutivos de aproximadamente siete días.
5. Tamaño y alternancia de cambios diarios clínicamente no interpretados, utilizados solo como señal de inestabilidad algorítmica.
6. Cantidad de fechas distintas, amplitud temporal efectiva y datos nuevos incorporados.
7. Solapamiento de ventanas y revisión retrospectiva de inputs.

Varias ejecuciones durante el mismo día, o recalcular repetidamente la misma ventana, no crean observaciones independientes. Dos ventanas deslizantes consecutivas que comparten la mayoría de sus días no se interpretan como confirmaciones estadísticas mutuamente independientes.

| Estado | Criterio inicial de ingeniería, versionado y no clínico | ¿Habilita propuesta? |
| --- | --- | --- |
| `INSUFFICIENT_HISTORY` | Menos de siete fechas válidas, horizonte insuficiente, solo prior poblacional/manual o datos revisados sin reconstrucción. | No. |
| `UNSTABLE` | Oscilación relevante, alternancia persistente, amplitud excesiva, deriva entre períodos o cambio de régimen no explicado. | No. |
| `STABILIZING` | Historia parcial o mejora progresiva sin cumplir todavía todas las compuertas estrictas. | No. |
| `STABLE` | Al menos 10 fechas útiles distribuidas en una ventana inclusiva de 14 días, dos períodos cronológicos comparables, cobertura suficiente y todas las métricas dentro de política. | Sí, únicamente si el resto de las compuertas también autoriza. |

Política inicial orientativa `stability-v1`:

- `minimum_observation_days = 7` para diferenciar historia insuficiente de estabilización, salvo oscilación crítica visible.
- `stable_horizon_days = 14` y `stable_distinct_estimate_days = 10`.
- `maximum_relative_mad = 0,025`.
- `maximum_peak_to_peak_ratio = 0,05`.
- `maximum_consecutive_period_drift = 0,04`.
- Dos o más inversiones alternantes mayores que `0,035` entre estimaciones contiguas constituyen señal de inestabilidad.
- Una amplitud superior a `0,06` permite clasificar `UNSTABLE` antes de completar el horizonte mínimo; nunca convierte una serie corta en estable.

Ejemplo de aceptación: `2450 → 2310 → 2420 → 2300` tiene mediana aproximada `2365` y amplitud relativa `150 / 2365 ≈ 6,3 %`; produce `UNSTABLE`, aunque los días nutricionales estén completos. No se presenta ese `6,3 %` como certeza biológica ni como confianza del TDEE.

Cuando una edición retrospectiva altera significativamente una ventana, el historial posterior se recalcula desde el primer día afectado y el estado pasa a `INSUFFICIENT_HISTORY`, `UNSTABLE` o `STABILIZING` según evidencia restante. Una corrección menor puede preservar estabilidad solo si una política documentada demuestra que el cambio no es material.

**Regla operativa:** si `EstimatorStability != STABLE`, el evaluador puede informar `MAINTAIN`, `OBSERVE` o `INSUFFICIENT_DATA`, pero no generar una propuesta energética. Si existe señal preocupante, informa revisión contextual sin atribuir causalidad ni recomendar automáticamente una cifra.

### 11.7 TrainingLoadEstimator

**Propósito:** contextualizar recuperación y gasto sin depender de métricas deportivas exclusivas de carrera.

**Estrategia inicial:**

- Si existe esfuerzo percibido, utilizar duración de sesión multiplicada por esfuerzo reportado.
- Si no existe, estimar contexto a partir de duración, modalidad y frecuencia cardíaca de calidad suficiente.
- Clasificar pole, ballet, fuerza y flexibilidad de forma diferenciada.
- Tratar agarres, contracciones isométricas, pausas técnicas o uso irregular del reloj como causas de menor confianza.
- No interpretar VO₂ max, pasos o ritmo de carrera como rendimiento de pole o ballet.
- Mantener estado UNKNOWN cuando la sesión no dispone de evidencia suficiente.

Una percepción subjetiva opcional mejora el contexto; no debe convertirse en una obligación diaria de alto esfuerzo.

### 11.8 ReadinessCalculator

**Propósito:** describir preparación relativa frente al comportamiento habitual, no reproducir el algoritmo propietario de Garmin ni diagnosticar recuperación.

**Diseño:** modelo de factores limitantes, no promedio opaco de porcentajes.

**Dominios de evidencia:**

1. Sueño reciente frente a duración y patrón personal.
2. Señales fisiológicas nocturnas disponibles: HRV y/o FC en reposo, si existen.
3. Carga reciente y recuperación entre sesiones.
4. Estrés agregado u otras métricas, únicamente si se reciben realmente.
5. Contexto acumulado de déficit, ritmo de pérdida, ingesta proteica o baja disponibilidad energética; nunca efecto instantáneo de una comida aislada.

**Agrupación de variables correlacionadas:** HRV, estrés y Body Battery pueden reflejar procesos relacionados. El algoritmo no suma tres penalizaciones máximas por la misma alteración; agrega por dominio y registra evidencia corroborante.

**Morning Readiness:** primero se clasifica la gravedad de cada dominio frente a su baseline robusto: NORMAL, LEVE, MODERADA, ALTA o DESCONOCIDA. Después se determina una categoría mediante una tabla de evidencia corroborante, sin sumar porcentajes fisiológicos arbitrarios.

| Evidencia observada | Categoría inicial | Explicación esperada |
| --- | --- | --- |
| Dominios suficientes normales o una desviación leve aislada. | `GOOD`. | Sueño o carga dentro de lo habitual; limitaciones explicitadas. |
| Una desviación moderada o varias desviaciones leves consistentes. | `MODERATE`. | Principal factor limitante y persistencia observada. |
| Deterioro alto corroborado por otro dominio, o múltiples dominios moderadamente deteriorados. | `LOW`. | Limitantes corroborados; sin diagnóstico ni causalidad nutricional automática. |
| Cobertura insuficiente o ausencia de baseline interpretable. | `UNKNOWN`. | Señales ausentes o línea base todavía inmadura. |

La primera versión **no calcula, persiste ni muestra puntuación 0–100**. Clasifica factores y explica categoría, cobertura y limitantes. Una puntuación ordinal futura requerirá evidencia de que distingue decisiones o situaciones que las categorías no distinguen; no se añade por simetría con Garmin ni para producir diferencias artificiales entre 71 y 74. La ausencia reduce confianza, pero no empeora por sí sola la categoría.

Política inicial:

- Requerir al menos dos dominios independientes utilizables, o un dominio dominante con evidencia excepcionalmente clara y confianza explícita reducida.
- Si HRV no está disponible, sueño y carga pueden producir un readiness de cobertura limitada.
- Una noche levemente alterada no debe producir un descenso extremo o injustificado de categoría.
- Varias señales deterioradas y coherentes sí pueden reducirlo.
- Si falta baseline personal, informar provisional o UNKNOWN.
- Una comida registrada no cambia el readiness instantáneamente.
- No presentar recuperación del reloj como disponible si solo se encuentra visible en Garmin Connect.

**Current Readiness, posterior al MVP básico y correspondiente a la fase 6:**

- Parte de Morning Readiness y conserva referencia a su versión.
- Se actualiza por sesión real, siesta reconocida, estrés acumulado disponible o corrección relevante de datos.
- No introduce caída artificial por simple paso del tiempo salvo política validada.
- Aplica transiciones categóricas limitadas y saturadas.
- No cambia artificialmente de categoría por un alimento individual.

### 11.9 DailyRecommendationEngine

**Propósito:** distinguir plan base, recomendado diario e ingesta efectiva sin duplicar compensaciones.

**Definiciones:**

- Plan base: objetivo relativamente estable de la versión de plan vigente.
- Recomendado hoy: plan base sin correcciones en `BASE_ONLY`; solo en `ADAPTIVE`, plan base más una corrección diaria moderada y autorizada.
- Consumido: entradas confirmadas y, por separado, estimaciones aceptadas.

**Modo inicial obligatorio: `BASE_ONLY`.**

~~~text
modo = BASE_ONLY
recomendado_hoy = plan_base
ajuste_actividad = 0 por política, no porque la actividad observada sea cero
~~~

La igualdad se mantiene aunque haya entrenamiento intenso, pasos elevados, calorías Garmin disponibles o readiness alto. El sistema sigue registrando actividad y construyendo baselines cuando corresponda, pero no aprende simultáneamente TDEE habitual y compensación de ejercicio.

No se necesita implementar `DailyRecommendationEngine` como clase independiente mientras una función o caso de uso sencillo pueda expresar esta identidad sin perder la separación conceptual entre plan, recomendado y consumido.

**Modo futuro `ADAPTIVE`: regla de actividad únicamente tras habilitación.**

~~~text
actividad_excepcional = actividad_observada - actividad_habitual_comparable
compensación = factor_conservador × máximo(0, actividad_excepcional)
recomendado_hoy = plan_base + compensación_limitada
~~~

Valores de partida sujetos a calibración: factor inicial de 0,10–0,25 y tope absoluto aproximado de 100–150 kcal por día. Son guardrails de producto, no equivalencias fisiológicas. Si el gasto de sesión es incierto, priorizar duración, modalidad y contexto antes que una cifra exacta de calorías.

**Compuertas acumulativas antes de habilitar `ADAPTIVE`:**

1. Shadow Mode del núcleo superado y versión de política revisada.
2. TDEE observacional en estado `STABLE` durante un período suficiente definido por política.
3. Tendencia de peso y nutrición contemporánea de calidad adecuada.
4. Baseline de actividad construido con al menos una ventana personal comparable; referencia inicial orientativa: 28 días.
5. Sesiones y fuentes deduplicadas; energía activa y total no se suman dos veces.
6. Capacidad para distinguir actividad realmente excepcional de actividad habitual incorporada al TDEE.
7. Validación específica en sombra de la política de compensación y revisión de sus falsos positivos.
8. Ausencia de riesgo, eventos confusores relevantes y enfriamiento incompatible.
9. Activación explícita de la función; ante cualquier degradación se vuelve a `BASE_ONLY`.

La llegada de Health Connect en fase 4 no satisface por sí sola estas compuertas. La compensación diaria puede permanecer desactivada durante toda la primera versión integrada.

**Restricciones:**

1. No sumar 700 kcal por una sesión que el reloj estima en 700 kcal.
2. No volver a compensar actividad habitual ya absorbida por el TDEE adaptativo.
3. No reducir el plan base porque un día tenga pocos pasos o readiness bajo.
4. No convertir cada entrenamiento en una modificación de la versión del plan.
5. Controlar el total semanal de compensaciones para evitar realimentación.
6. En `BASE_ONLY`, recomendado hoy siempre es igual al plan base; en `ADAPTIVE`, también lo es cuando no existe evidencia de actividad excepcional.
7. Mantener proteína objetivo estable salvo una decisión de plan separada y justificada.

**Restantes:**

~~~text
restante_confirmado = recomendado_hoy - energía_confirmada
restante_probable = recomendado_hoy - energía_confirmada - energía_estimada_aceptada
~~~

Las entradas pendientes se muestran aparte. Un valor negativo se identifica como diferencia respecto del objetivo, no como deuda nutricional ni obligación de compensación posterior.

### 11.10 PlanEvaluator

**Propósito:** responder si el plan vigente sigue siendo apropiado según evidencia longitudinal y salvaguardas.

**Entradas:**

- NutritionPlanVersion vigente y antigüedad.
- WeightTrend.
- NutritionDiaryDay y DailyNutrition.
- TdeeEstimate, si está disponible.
- EstimatorStability y su historial efectivo, como compuerta independiente.
- MorningReadiness o tendencia de recuperación, cuando exista.
- TrainingLoad y observaciones de rendimiento, si existen.
- ContextEvent, SafetyStatus y evaluaciones de calidad.
- Historial de cambios y período de enfriamiento.
- DecisionStateMemory, versión de política y EvaluationMode.

**Estados:** MAINTAIN, OBSERVE, ADJUST_UP, ADJUST_DOWN e INSUFFICIENT_DATA.

**Secuencia obligatoria:**

1. Verificar elegibilidad del plan, vigencia, objetivo y revisión de inputs.
2. Verificar calidad mínima e independiente de peso e ingesta.
3. Comparar objetivo y respuesta longitudinal para obtener un **candidato direccional**; readiness no participa en esa dirección.
4. Detectar contexto de seguridad, datos faltantes, factores confusores y necesidad de revisión.
5. Verificar `EstimatorStability`; cualquier estado distinto de `STABLE` bloquea propuestas energéticas.
6. Comprobar antigüedad del plan, período de enfriamiento y propuestas previamente rechazadas.
7. Analizar recuperación y rendimiento observados únicamente como contexto, corroboración o bloqueo.
8. Aplicar histéresis y memoria por versión de plan solamente después de las compuertas duras.
9. Obtener estado efectivo, razones ordenadas, confianza cualitativa y autorización.
10. Aplicar el modo: `SHADOW` registra evaluación hipotética; `ADVISORY` permite asesoramiento si todas las compuertas siguen abiertas.
11. Construir una propuesta limitada **solo** en `ADVISORY`, con evidencia suficiente y aceptación manual pendiente.

**Separación de estados:**

| Capa | Pregunta respondida | Ejemplo | ¿Puede cambiar el plan? |
| --- | --- | --- | --- |
| Candidato de evidencia. | ¿Qué sugiere la discrepancia longitudinal entre objetivo y respuesta? | `ADJUST_DOWN`. | No. |
| Estado efectivo. | ¿Qué permanece después de calidad, estabilidad, seguridad e histéresis? | `OBSERVE` por TDEE inestable. | No por sí solo. |
| Proyección operativa `SHADOW`. | ¿Se trata únicamente de validación? | `EN VALIDACIÓN`; candidato guardado localmente. | Nunca. |
| Proyección operativa `ADVISORY`. | ¿Existe una propuesta segura, habilitada y explicable? | Propuesta `-75 kcal`, pendiente. | Solo después de aceptación explícita. |

**Banda de tolerancia inicial:** el máximo entre aproximadamente 0,10–0,15 kg/semana y una fracción configurable del objetivo. Esta banda absorbe incertidumbre; no debe presentarse como norma clínica universal.

**Dirección conceptual de corrección energética:**

~~~text
delta_teórico = k × (ritmo_objetivo - ritmo_observado) / 7
delta_propuesto = limitar_y_redondear(delta_teórico, guardrails)
~~~

Ejemplo: objetivo -0,35 kg/semana y observado -0,12 kg/semana generan una corrección teórica negativa. El producto no aplica toda esa magnitud: puede proponer -50 o -75 kcal solamente si supera todas las compuertas de evidencia y seguridad.

Una asociación entre déficit y caída de HRV se comunica como coincidencia o patrón compatible, nunca como causalidad demostrada.

### 11.10.1 Histéresis del PlanEvaluator

**Propósito:** impedir alternancias repetidas entre observar y ajustar cuando la evidencia fluctúa alrededor de un único umbral.

Para un objetivo de pérdida, utilizar una discrepancia orientativa:

~~~text
desviación = ritmo_observado - ritmo_objetivo

objetivo -0,35 kg/sem; observado -0,08 kg/sem
desviación = +0,27 kg/sem
~~~

Política inicial de ingeniería para `LOSS`, sujeta a revisión:

| Transición | Compuerta propuesta | Persistencia necesaria |
| --- | --- | --- |
| Entrar en `ADJUST_DOWN`. | Desviación positiva mayor o igual que `0,20 kg/sem` y evidencia segura de pérdida insuficiente. | Dos evaluaciones calificadas en fechas distintas, separadas al menos 48 horas y con evidencia nueva verificable. |
| Permanecer en `ADJUST_DOWN`. | Desviación todavía superior a `0,10 kg/sem` y todas las compuertas de seguridad abiertas. | Memoria válida para la misma versión de plan y política. |
| Salir de `ADJUST_DOWN`. | Desviación inferior o igual a `0,10 kg/sem`, pérdida de corroboración o cualquier bloqueo duro. | Salida inmediata; no necesita persistencia si existe riesgo. |
| Entrar en `ADJUST_UP`. | Desviación negativa menor o igual que `-0,20 kg/sem` y evidencia segura de pérdida excesiva. | Dos evaluaciones calificadas en fechas distintas, con nueva evidencia y sin contexto confusor invalidante. |
| Permanecer en `ADJUST_UP`. | Desviación todavía inferior a `-0,10 kg/sem` y condiciones de seguridad vigentes. | Memoria válida; no transforma readiness aislado en causalidad. |
| Salir de `ADJUST_UP`. | Desviación mayor o igual que `-0,10 kg/sem` o aparición de bloqueo duro. | Salida inmediata ante riesgo, revisión material o baja calidad. |

Los valores son `PolicyVersion`, no recomendaciones clínicas. Objetivos `GAIN`, `MAINTENANCE`, `RECOMPOSITION` y `PERFORMANCE_PRIORITY` requieren políticas propias; no se reutilizan signos o umbrales sin revisar su semántica.

**Prioridad normativa:**

1. Riesgo, baja confianza, estimador inestable, cambio de plan, corrección retrospectiva o evidencia contradictoria anulan inmediatamente la persistencia direccional.
2. Cooldown e histéresis son mecanismos distintos: el primero limita frecuencia de cambios; la segunda limita transiciones por ruido.
3. Recalcular dos veces la misma ventana sin datos nuevos no satisface persistencia de evidencia.
4. Una propuesta pendiente no se duplica mientras el estado persiste.
5. Una propuesta aceptada reinicia memoria, abre una nueva versión de plan y activa cooldown.
6. Una propuesta rechazada no se vuelve a emitir cada día; requiere evidencia nueva y una política de reoferta.
7. La transición `SHADOW → ADVISORY` nunca arrastra automáticamente un candidato anterior hacia una propuesta real: exige nueva evaluación autorizada.

### 11.10.2 Shadow Mode y separación operativa

`SHADOW` es el modo obligatorio al materializar por primera vez el evaluador y se mantiene durante la fase 3.5. Para cada ejecución conserva una `EvaluationObservation` mínima:

- día, plan vigente, algoritmo y política;
- ventana analítica y revisión lógica de inputs;
- cobertura de pesajes, cierres nutricionales y proporción estimada;
- TDEE aproximado, rango si corresponde y estado de estabilidad;
- estado candidato, estado efectivo, códigos de bloqueo e histéresis;
- cambio hipotético, solo en inspector de validación claramente identificado;
- `operational = false`, sin entidad `AdjustmentProposal` y sin acción de aceptación.

El panel puede mostrar `PLAN EN VALIDACIÓN`, junto con observaciones descriptivas o mantenimiento factual del plan actual. No muestra `BAJAR CALORÍAS`, `SUBIR CALORÍAS` ni un objetivo hipotético como recomendación vigente.

`ADVISORY` es una capacidad posterior a la validación. Su activación requiere criterios documentados, consentimiento explícito y una ejecución nueva; cada propuesta posterior continúa necesitando aceptación manual. Cambiar de algoritmo, importar historia que altere materialmente el baseline o incorporar una nueva señal direccional obliga a una validación acotada adicional antes de permitir efectos operativos.

### 11.10.3 Regla arquitectónica de readiness

> Readiness puede contextualizar o bloquear una decisión nutricional, pero por sí solo no determina la dirección de una modificación del plan.

Comportamientos prohibidos:

- `readiness = LOW → ADJUST_UP` sin discrepancia longitudinal del plan.
- `readiness = GOOD → ADJUST_DOWN` sin pérdida persistentemente inferior al objetivo.
- `readiness = GOOD` utilizado para ignorar TDEE inestable, nutrición incompleta o riesgo.
- `readiness = LOW` interpretado como prueba de que el déficit causó el deterioro.

Comportamientos permitidos:

- Pérdida demasiado rápida **ya demostrada** + recuperación deteriorada: corroborar preocupación o bloquear automatización; la dirección sigue proveniendo de la respuesta longitudinal.
- Pérdida lenta + recuperación baja: bloquear un recorte potencialmente inseguro.
- Recuperación desconocida + carga alta: elevar cautela y exigir evidencia adicional.
- Readiness aislado anormal: informar contexto, mantener u observar según el resto de señales.

---

## 12. H. Políticas de decisión y seguridad

### 12.1 Parámetros iniciales versionables

| Parámetro | Valor inicial de diseño | Justificación y limitación |
| --- | --- | --- |
| Ventana mínima de evaluación preliminar | 14 días. | Reduce reacción a eventos aislados; no garantiza estabilidad fisiológica. |
| Ventana preferida para recorte | 21–28 días. | Exige evidencia más sostenida para una decisión potencialmente restrictiva. |
| Días de peso mínimos | 5 distribuidos. | Evita decisiones basadas en dos mediciones extremas. |
| Días de peso preferidos para ajuste | 8 en aproximadamente 21 días. | Mejora distribución temporal y confiabilidad de pendiente. |
| Cobertura nutricional mínima preliminar | Aproximadamente 10–12 días cerrados en 14. | Un diario abierto no se convierte en cero. |
| Cobertura nutricional para ADJUST_DOWN | Al menos 85 % de la ventana. | Un recorte requiere especial confianza en la ingesta. |
| Proporción máxima estimada para recorte | Aproximadamente 35 % de energía. | Limita influencia de alimentos o cantidades inciertas. |
| Confianza mínima para recorte | 0,80. | Compuerta operacional; no probabilidad clínica. |
| Confianza mínima para propuesta de aumento | 0,70–0,75 con señales consistentes. | Asimetría prudente; riesgo y contexto pueden igualmente bloquear. |
| Modo inicial de evaluación | `SHADOW`. | Evalúa y registra sin producir propuestas operativas. |
| Modo inicial de recomendación diaria | `BASE_ONLY`. | Mantiene recomendado y plan base idénticos mientras se aprende el TDEE habitual. |
| Horizonte mínimo de estabilidad TDEE | 14 días civiles; al menos 10 fechas útiles. | Describe comportamiento observado; ventanas solapadas no generan independencia estadística. |
| Dispersión robusta máxima provisional | `MAD / mediana ≤ 0,025`. | Compuerta operativa revisable, no certeza fisiológica. |
| Amplitud máxima provisional del TDEE | `(máximo - mínimo) / mediana ≤ 0,05`. | Evita habilitar ajustes con oscilaciones visibles. |
| Umbral de entrada direccional en LOSS | Desviación de `±0,20 kg/sem`, orientativa. | Requiere discrepancia más fuerte para entrar en un estado de ajuste. |
| Umbral de salida direccional en LOSS | Desviación de `±0,10 kg/sem`, orientativa. | Introduce banda de histéresis sin cancelar guardrails. |
| Persistencia direccional mínima | Dos evaluaciones elegibles separadas al menos 48 horas y con evidencia nueva. | Evita contar ejecuciones repetidas del mismo conjunto de datos. |
| Duración inicial de Shadow Mode | Referencia de 28 días; posible extensión si faltan criterios. | Valida comportamiento personal antes de asesoramiento operativo. |
| Corrección descendente inicial | -50 a -75 kcal/día. | Evita cambios bruscos y realimentación. |
| Corrección ascendente inicial | +75 a +150 kcal/día. | Permite respuestas conservadoras sin presentarlas como tratamiento. |
| Período de enfriamiento tras aceptar cambio | 14 días como valor inicial. | Permite observar la nueva versión del plan antes de reconsiderar. |
| Compensación excepcional diaria | Desactivada en `BASE_ONLY`; en `ADAPTIVE` validado, 0,10–0,25 del exceso contextual. | Evita aprendizaje simultáneo y reposición automática de calorías wearable. |
| Tope diario de compensación habilitada | Aproximadamente 100–150 kcal, únicamente en `ADAPTIVE`. | Mantiene estabilidad del plan diario cuando la capacidad esté validada. |

Todos los valores anteriores son propuestas de ingeniería iniciales. Deben estar centralizados, versionados, testeados y revisados con datos reales. Un límite inferior energético universal no es aceptable: debe existir un piso individual explícito o, en su ausencia, un bloqueo de ajustes restrictivos cuando no se pueda determinar seguridad suficiente.

### 12.2 Matriz de autorización

Las filas que admiten propuestas presuponen siempre modo `ADVISORY`, estabilidad `STABLE`, histéresis satisfecha, política vigente y ausencia de bloqueo. `SHADOW` tiene precedencia sobre cualquier candidato direccional.

| Situación | Estado permitido | ¿Puede proponer bajar? | ¿Puede proponer subir? |
| --- | --- | --- | --- |
| Menos de cinco pesajes útiles o ventana demasiado corta. | INSUFFICIENT_DATA. | No. | No; solo señal de revisión contextual. |
| Nutrición abierta o incompleta en demasiados días. | INSUFFICIENT_DATA u OBSERVE. | No. | No, salvo recomendación de revisión sin objetivo numérico. |
| Datos completos, pero TDEE `INSUFFICIENT_HISTORY`, `UNSTABLE` o `STABILIZING`. | OBSERVE o INSUFFICIENT_DATA. | No. | No; revisión contextual sin cifra si corresponde. |
| Modo `SHADOW`, aunque el candidato efectivo sea `ADJUST_UP` o `ADJUST_DOWN`. | EN VALIDACIÓN; estado interno no operativo. | No. | No. |
| Discrepancia en la banda de histéresis, sin compuerta de entrada cumplida. | OBSERVE o estado previamente seguro. | No nueva propuesta. | No nueva propuesta. |
| Ritmo dentro de tolerancia y señales conocidas estables. | MAINTAIN. | No. | No. |
| Discrepancia aislada o ventanas contradictorias. | OBSERVE. | No. | No. |
| Pérdida sostenida demasiado rápida y recuperación deteriorada, con evidencia suficiente. | ADJUST_UP. | No. | Sí, dentro del tope y con aceptación. |
| Pérdida sostenida demasiado lenta, ingesta fiable, recuperación adecuada y sin riesgos. | ADJUST_DOWN. | Sí, dentro del tope y con aceptación. | No. |
| Carga alta y recuperación desconocida o rendimiento desconocido. | OBSERVE o MAINTAIN según objetivo. | No. | Solo si la evidencia autoriza una propuesta independiente. |
| Enfermedad, lesión relevante, embarazo, lactancia, minoría de edad o antecedentes sensibles declarados. | OBSERVE con REVIEW_REQUIRED o INSUFFICIENT_DATA. | No. | No automáticamente; revisión apropiada. |
| Período de enfriamiento vigente. | OBSERVE o MAINTAIN. | No. | No, salvo contexto que requiera revisión sin ajuste automático. |
| Propuesta colocaría el plan por debajo del piso personal. | OBSERVE con bloqueo. | No. | No por ese único motivo. |
| Rendimiento observado en deterioro. | OBSERVE o ADJUST_UP si existe evidencia concordante. | No. | Solo cuando corresponda y con evidencia suficiente. |
| Readiness LOW aislado, sin discrepancia longitudinal. | MAINTAIN u OBSERVE, con contexto. | No. | No. |
| Readiness GOOD aislado, sin evidencia sostenida de pérdida insuficiente. | MAINTAIN u OBSERVE. | No. | No. |
| Corrección retrospectiva material o cambio de versión de política. | OBSERVE, INSUFFICIENT_DATA o retorno a SHADOW según impacto. | No hasta revalidación. | No hasta revalidación. |

La persona puede editar manualmente su plan porque conserva autonomía, pero el sistema no debe presentar esa acción como recomendación validada si no supera sus guardrails.

### 12.3 Anti-realimentación

1. El TDEE se calcula desde ingesta observada, no desde recomendado diario.
2. La actividad habitual ya reflejada en el TDEE no vuelve a compensarse.
3. Una propuesta aceptada crea un nuevo período analítico.
4. Se evita mezclar sin distinción días correspondientes a planes diferentes.
5. No se generan nuevas propuestas durante enfriamiento, salvo cambiar a observación o revisión.
6. Una propuesta pendiente expira si una edición relevante modifica evidencia o seguridad.
7. No se compensa al día siguiente por haber superado el recomendado anterior.
8. El sistema puede concluir explícitamente que no corresponde cambiar nada.
9. El modo `SHADOW` no alimenta propuestas, cambios de plan ni recomendaciones diarias adaptativas.
10. La transición a `ADVISORY` exige una evaluación nueva; no reutiliza automáticamente sugerencias hipotéticas previas.
11. Un cambio de plan reinicia memoria de histéresis y separa la evidencia del período anterior.
12. `BASE_ONLY` impide que actividad habitual y TDEE aprendido se compensen simultáneamente.

### 12.4 Evaluación por modo de objetivo

| Objetivo | Qué se compara | Precaución principal |
| --- | --- | --- |
| LOSS | Ritmo observado frente al ritmo de pérdida establecido. | Evitar restricciones cuando recuperación, rendimiento o disponibilidad energética son inciertos. |
| GAIN | Ritmo observado frente a ganancia deseada. | Evitar atribuir cambios de agua a ganancia real. |
| MAINTENANCE | Tendencia compatible con estabilidad dentro de tolerancia. | No reaccionar a oscilaciones diarias. |
| RECOMPOSITION | Estabilidad o cambios compatibles con el plan y entrenamiento observado. | No afirmar ganancia muscular sin evidencia apropiada. |
| PERFORMANCE_PRIORITY | Recuperación, suficiencia y desempeño observado. | No priorizar recorte energético por una variación de peso. |

### 12.5 Catálogo inicial de razones

Motivos favorables:

- WEIGHT_RATE_WITHIN_TARGET.
- WEIGHT_TREND_STABLE.
- NUTRITION_COVERAGE_SUFFICIENT.
- RECOVERY_BASELINE_STABLE.
- PERFORMANCE_OBSERVED_STABLE.
- PLAN_EPOCH_MATURE.
- TDEE_ESTIMATOR_STABLE.
- HYSTERESIS_EVIDENCE_PERSISTENT.
- SHADOW_VALIDATION_CRITERIA_MET.

Motivos de observación o bloqueo:

- WEIGHT_OBSERVATIONS_INSUFFICIENT.
- WEIGHT_WINDOW_TOO_SHORT.
- WEIGHT_NOISE_ELEVATED.
- NUTRITION_DAYS_INCOMPLETE.
- NUTRITION_ESTIMATION_SHARE_HIGH.
- NUTRITION_PENDING_CONFIRMATION.
- TDEE_UNCERTAINTY_HIGH.
- TDEE_ESTIMATOR_HISTORY_INSUFFICIENT.
- TDEE_ESTIMATOR_UNSTABLE.
- TDEE_ESTIMATOR_STABILIZING.
- TDEE_ESTIMATOR_OSCILLATION_HIGH.
- TDEE_ESTIMATOR_PERIOD_DRIFT_HIGH.
- SHADOW_MODE_ACTIVE.
- SHADOW_VALIDATION_INCOMPLETE.
- HYSTERESIS_ENTRY_NOT_CONFIRMED.
- HYSTERESIS_SAFETY_EXIT.
- EVALUATION_REVISED_RETROSPECTIVELY.
- ACTIVITY_BASELINE_IMMATURE.
- DAILY_RECOMMENDATION_BASE_ONLY.
- RECOVERY_SIGNAL_UNAVAILABLE.
- RECOVERY_SIGNALS_DETERIORATING.
- PERFORMANCE_UNKNOWN.
- PERFORMANCE_DECLINING.
- TRAINING_LOAD_HIGH.
- CONFOUNDING_EVENT_PRESENT.
- PLAN_EPOCH_TOO_SHORT.
- COOLDOWN_ACTIVE.
- PERSONAL_ENERGY_FLOOR_UNDEFINED.
- PERSONAL_ENERGY_FLOOR_REACHED.
- SAFETY_CONTEXT_REQUIRES_REVIEW.
- SOURCE_PERMISSION_REVOKED.
- DUPLICATE_SOURCE_PREVENTED.

Motivos direccionales:

- WEIGHT_LOSS_FASTER_THAN_TARGET.
- WEIGHT_LOSS_SLOWER_THAN_TARGET.
- WEIGHT_GAIN_FASTER_THAN_TARGET.
- WEIGHT_GAIN_SLOWER_THAN_TARGET.
- MULTI_SIGNAL_UNDER_RECOVERY_PATTERN.
- EXCEPTIONAL_ACTIVITY_ABOVE_BASELINE.
- ADJUSTMENT_CAPPED_BY_POLICY.

Cada motivo debe conservar dirección, severidad, métrica, valor observado, referencia, unidad, ventana, confianza y contribución; su texto visible se genera a partir del código y no reemplaza la evidencia estructurada.

### 12.6 Contextos sensibles

Ante antecedente de trastorno alimentario, embarazo, lactancia, minoría de edad, lesión, enfermedad, alteraciones menstruales u otras señales relevantes informadas voluntariamente:

- No diagnosticar ni inferir la condición.
- Restringir recomendaciones restrictivas.
- Evitar mensajes centrados en compensación, deuda o culpa.
- Separar una alerta de revisión de una sugerencia automática de calorías.
- Respetar permisos y permitir que esos datos permanezcan no registrados.
- No convertir ausencia de datos sensibles en afirmación de ausencia de riesgo.

---

## 13. I. Experiencia de usuario y pantallas

### 13.1 Panel principal

| Área | Resumen cotidiano |
| --- | --- |
| Recuperación. | **BUENA**; o NO DISPONIBLE si todavía no existe integración útil. |
| Nutrición. | **1470 / 2050 kcal**; usar `~` si el total contiene estimaciones relevantes. |
| Proteína. | **103 / 145 g**; desconocida si faltan datos críticos. |
| Peso. | **-0,31 kg/sem** cuando existe tendencia suficiente; sin pesajes ficticios. |
| Plan operativo. | **MANTENER**, **OBSERVAR**, **DATOS INSUFICIENTES** o **EN VALIDACIÓN**, según fase y autorización. |

En fase 2 la sección recuperación puede no existir y el plan puede limitarse a mostrar el objetivo configurado; no se inventa una evaluación todavía no implementada. Durante `SHADOW`, el estado cotidiano es **EN VALIDACIÓN**; una sugerencia hipotética de ajuste no se presenta como consejo real. Una propuesta direccional solo aparece en el flujo específico de plan cuando `ADVISORY` se encuentra habilitado y existe una propuesta válida.

Al abrir nutrición se pueden consultar plan base, recomendado hoy, confirmado, estimado, pendiente y restante. En `BASE_ONLY`, plan base y recomendado coinciden; no hace falta repetir dos cifras idénticas permanentemente en el panel. Al abrir recuperación aparecen factores como `Sueño: normal`, `Carga: alta`, `HRV: no disponible`. Al abrir plan aparecen motivos, cobertura, estabilidad y versión.

La pantalla debe ocultar o etiquetar como desconocidas las secciones sin soporte; no llenar con ceros, no inventar readiness y no mostrar versiones o índices técnicos como contenido principal.

### 13.2 Pantallas mínimas

1. Inicio/panel del día.
2. Registro rápido de alimento y selector de favoritos/recientes.
3. Comidas guardadas y edición de cantidades.
4. Diario y cierre del día.
5. Registro e historial de peso.
6. Plan vigente, objetivo y propuestas.
7. Detalle de tendencia, TDEE y calidad.
8. Detalle de recuperación y factores limitantes, a partir de fase 4.
9. Integraciones, permisos y capacidades reales, únicamente al incorporar Health Connect.
10. Importación histórica y resultado por archivo, únicamente después de elegir un formato real.
11. Configuración de privacidad y exportación propia, según capacidades efectivamente implementadas.

El listado constituye navegación objetivo, no once pantallas obligatorias en la primera vertical. Cada fase expone exclusivamente superficies con funcionalidad real y acciones comprensibles.

### 13.3 Explicación de decisión

~~~text
PLAN ACTUAL                  2050 kcal/día

Objetivo                     -0,35 kg/sem
Ritmo observado              -0,12 kg/sem
Ventana                      28 días
Peso                         9 registros distribuidos
Nutrición                    25 de 28 días completos
Ingesta estimada             18 % del total registrado
Estabilidad del TDEE         estable
Recuperación                 estable en señales disponibles
Rendimiento                  estable; 4 sesiones informadas
Confianza                    alta

Corrección propuesta         1975 kcal/día
Cambio                       -75 kcal/día
Límite aplicado              corrección máxima conservadora
Aplicación                   pendiente de aceptación
~~~

Este ejemplo solo es válido en `ADVISORY` tras superar Shadow Mode. En `SHADOW` se muestra `EVALUACIÓN EN VALIDACIÓN`; los resultados hipotéticos, si se inspeccionan, se etiquetan claramente como no operativos y no ofrecen botón de aceptar.

Si rendimiento no se registró, la interfaz debe decir RENDIMIENTO DESCONOCIDO y aplicar la política de seguridad correspondiente; no reemplazarlo por estable.

### 13.4 Lenguaje visual y precisión

- Redondear TDEE y recomendaciones a incrementos razonables.
- Mostrar símbolo aproximado cuando la naturaleza del dato lo requiera.
- No mostrar una comida fotográfica como exacta.
- Diferenciar último peso real de tendencia modelada.
- Utilizar confianza cualitativa acompañada de motivos; reservar el índice numérico exclusivamente a inspección avanzada o depuración, nunca al panel cotidiano ni al detalle estándar.
- Indicar antigüedad de la última sincronización.
- Evitar mensajes punitivos, compensaciones y alertas repetitivas de peso.

### 13.5 Baja fricción para disciplinas reales

- Registrar Pepsi Zero o Coca Zero con cantidad y favorito, sin fotografía.
- Permitir repetir café, yogur y desayunos habituales.
- Clasificar pole y ballet aun cuando Health Connect use categorías genéricas.
- Permitir esfuerzo percibido en un solo gesto después de una sesión, opcionalmente.
- No exigir pesaje diario, registro de síntomas ni seguimiento de ciclo.

---

## 14. J. Integración, importación y sincronización

### 14.1 Compuertas empíricas obligatorias

La documentación oficial identifica posibilidades; **no sustituye la observación del teléfono ni de la exportación concreta**. Fase 0 contiene dos líneas de descubrimiento independientes que pueden avanzar en paralelo con las fases locales 1–3.5.

**Spike A — Garmin vívoactive 6 → Health Connect en el teléfono real.**

Registrar, sin conservar muestras sensibles innecesarias:

1. Modelo de teléfono, versión Android, disponibilidad de Health Connect y versión relevante de Garmin Connect.
2. Permisos pedidos, concedidos, denegados o no disponibles por tipo.
3. Origen o paquete realmente observado; distinguir Garmin, teléfono y otros proveedores.
4. Presencia efectiva de sueño, fases, pasos, actividades, frecuencia cardíaca, calorías y peso.
5. Presencia o ausencia específica de HRV, FC en reposo, estrés, Body Battery, Recovery Time y VO₂ max.
6. Granularidad, unidades, intervalo histórico accesible, frescura y latencia de publicación.
7. Duplicados o solapamientos entre reloj y teléfono; limitaciones de agregación.
8. Capacidad real de historial extendido, cambios y lectura en segundo plano, cuando corresponda.
9. Matriz final por métrica: soportada por esquema, permiso, proveedor observado, cobertura y limitación.

**Cierre del Spike A:** informe empírico con datos observados o ausencia inequívoca. Es compuerta obligatoria para crear `data:healthconnect`, seleccionar tipos y agregar permisos específicos; no bloquea plan, peso o nutrición locales.

El spike puede utilizar herramientas oficiales, inspección de permisos o un diagnóstico descartable; realizarlo no obliga a incorporar un SDK, parser o adaptador permanente al proyecto productivo.

**Spike B — inspección de exportación Garmin real y autorizada.**

Documentar:

1. Mecanismo de exportación, período solicitado y fecha efectiva de obtención.
2. Archivos y directorios encontrados, extensiones, formatos, tamaños y artefactos inaccesibles.
3. En CSV: encoding, encabezados, delimitador, unidades, campos, identificadores y filas anómalas.
4. En FIT u otros formatos: clase de archivo, mensajes realmente presentes, unidades, SDK necesario y compatibilidad Android/JVM.
5. Métricas presentes y ausentes; distinguir actividad, bienestar y resúmenes.
6. Granularidad, intervalos, timestamps, zonas, vacíos y cambios de formato.
7. Identificadores utilizables, duplicados internos y solapamientos potenciales con Health Connect.
8. Restricciones de privacidad, retención, archivos corruptos, muestras incompletas y alcance histórico.
9. Formato priorizado, valor real, riesgo técnico y razón para descartar otros.

**Cierre del Spike B:** inventario verificable y decisión explícita sobre un formato viable o conclusión de que no existe uno útil. Es compuerta obligatoria para elegir parser, añadir FIT SDK o diseñar tablas específicas de importación.

Estados de seguimiento de ambos spikes: `NOT_STARTED`, `BLOCKED_BY_DEVICE`, `BLOCKED_BY_SAMPLE`, `COMPLETE_WITH_LIMITATIONS` y `COMPLETE`. La fase 4 admite `COMPLETE` o `COMPLETE_WITH_LIMITATIONS` solo si el Spike A verificó alguna capacidad utilizable; la fase 5 admite esos estados solo si el Spike B verificó un formato viable. Si una compuerta permanece bloqueada, las funciones locales y Shadow Mode continúan sin degradarse.

Queda prohibido diseñar dependencias de HRV, FIT de bienestar, hashes de exportación o identificadores Garmin basándose exclusivamente en documentación general, capturas promocionales o funciones visibles en el reloj.

### 14.2 Pipeline de importación

~~~mermaid
flowchart TD
    F[Archivo elegido] --> S[Inspección segura]
    S --> T{Formato reconocido}
    T --> C[Parser CSV]
    T --> A[Parser FIT actividad]
    T --> W[Parser FIT bienestar]
    C --> N[Normalización]
    A --> N
    W --> N
    N --> D[Deduplicación]
    D --> R[(Room)]
    R --> B[Recalcular baselines]
~~~

### 14.3 Parsers previstos

| Parser | Prioridad | Valor aportado | Condición de activación |
| --- | --- | --- | --- |
| CSV de actividades | Inicial si la exportación real lo confirma. | Sesiones, fechas, disciplinas y resúmenes. | Encabezados y tipos verificables. |
| CSV de peso | Inicial si existe en la exportación. | Historial para tendencia y baseline de peso. | Fechas, unidades y duplicados verificables. |
| FIT de actividad | Alta posterior. | Sesiones detalladas y series disponibles. | FIT SDK compatible y muestras válidas. |
| FIT de bienestar | Alta para HRV y estrés. | Posible baseline fisiológico realmente diferenciador. | Campos específicos presentes y decodificables en archivos reales. |
| JSON u otros formatos de exportación | Dependiente de evidencia. | Métricas que no lleguen por otros canales. | Formato documentado o estructura verificable y estable. |

Un archivo CSV de actividades no se presenta como importación completa de sueño, HRV o estrés. Un FIT de bienestar no se presenta como fuente de Body Battery o Recovery Time hasta verificar esos campos.

### 14.4 Seguridad e idempotencia de importación

- Utilizar selección de documentos del sistema y acceso explícitamente otorgado.
- Validar tamaño, formato y límites de descompresión.
- Prevenir path traversal y archivos comprimidos maliciosos.
- Procesar en lotes con transacciones coherentes y progreso.
- Registrar archivo, parser, versión, cantidad de registros, errores y advertencias.
- Reimportar el mismo archivo sin duplicar datos.
- Conciliar datos importados con observaciones ya obtenidas de Health Connect.
- Permitir fallos parciales por archivo, sin marcar toda la historia como completa.

### 14.5 Conciliación de fuentes

Orden orientativo:

1. Coincidencia exacta por ID externo estable.
2. Coincidencia por origen, métrica, intervalo, dispositivo y valor normalizado.
3. Coincidencia aproximada limitada, con umbral y motivo registrados.
4. Conservación de ambos registros como conflicto cuando no existe suficiente certeza.

Para pasos y otras métricas acumulativas, utilizar agregaciones apropiadas o una política de origen preferido. Nunca sumar sin revisión totales diarios del teléfono, Garmin y exportación de la misma actividad. [Lectura agregada recomendada por Android](https://developer.android.com/health-and-fitness/health-connect/read-data).

### 14.6 Sincronización operativa

- Activación al abrir la aplicación.
- Sincronización manual explícita.
- Sincronización periódica en segundo plano solamente si existe permiso y soporte.
- Tokens de cambios separados por tipo cuando las señales se consumen de forma independiente.
- Upsert por ID externo y revisión.
- Tratamiento de eliminaciones externas mediante tombstone e invalidación de derivados.
- Recuperación ante token expirado por relectura acotada y deduplicación.
- Reintentos con backoff y estados visibles de error.
- Ausencia de internet no invalida lectura local de Health Connect ya disponible en el teléfono.

WorkManager permite periodicidad mínima de 15 minutos, pero no garantiza ejecución exacta; una cadencia más moderada suele ser suficiente para este producto. [Definición oficial de trabajo periódico](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).

### 14.7 Open Food Facts en V2

La integración se limita inicialmente a consulta de productos por código de barras; no se envían peso, HRV, entrenamientos ni información nutricional personal.

- Implementar FoodCatalogGateway reemplazable.
- Consultar por código completo, no búsqueda en cada pulsación.
- Usar caché local con fecha, procedencia, versión y calidad.
- Permitir edición o creación manual cuando el producto no exista.
- Aplicar User-Agent identificado y límites de uso.
- Distinguir ficha colaborativa de etiqueta verificada.
- Mantener funcionalidad manual offline.

La documentación actual recomienda API v3 para nuevas integraciones, indica que los datos no tienen garantía de exactitud, solicita User-Agent personalizado y establece límites aproximados de 15 consultas de producto y 10 búsquedas por minuto e IP. [Documentación oficial de Open Food Facts](https://openfoodfacts.github.io/openfoodfacts-server/api/).

### 14.8 Fotografía futura

El flujo tiene etapas separadas:

~~~text
imagen → detección de producto → estimación de cantidad → nutrientes → confirmación
~~~

Contratos obligatorios:

- Reconocer Coca Zero no determina que se consumió todo el envase.
- La cantidad consumida requiere confirmación independiente.
- Las estimaciones conservan naturaleza, intervalo defendible y estado pendiente.
- El proveedor puede ser local, remoto opcional o reemplazarse completamente.
- El MVP no requiere una API paga ni subida de fotos.

---

## 15. K. Estrategia de testing y validación

### 15.1 Pirámide de pruebas

1. Pruebas unitarias JVM del dominio y motores: prioridad principal.
2. Pruebas de contratos de repositorios y mappers.
3. Pruebas Room de DAO, restricciones, relaciones y migraciones.
4. Pruebas de integración de Health Connect con fixtures y herramientas oficiales.
5. Pruebas de importación con muestras reales anonimizadas y archivos sintéticos.
6. Pruebas Compose para flujos críticos.
7. Pruebas end-to-end de registrar, sincronizar, evaluar y aceptar una propuesta.

Toda prueba del motor utiliza reloj fijo, zona explícita, política versionada, datos ordenados y ausencia de aleatoriedad implícita.

### 15.2 Casos de tendencia de peso

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| WT-01 | Pesajes lunes, miércoles, jueves y sábado durante varias semanas. | Tendencia disponible; días faltantes sin mediciones ficticias. |
| WT-02 | Serie alrededor de 92 kg con un registro aislado cercano a 95 kg. | Outlier señalado; pendiente sin cambio extremo. |
| WT-03 | Solo dos pesajes separados por pocos días. | Último peso visible; tendencia no habilita ajuste. |
| WT-04 | Tres mediciones el mismo día. | Se conservan todas; selección analítica explícita y trazable. |
| WT-05 | Subida persistente durante varios pesajes consecutivos. | Posible cambio de régimen; no descartar toda la secuencia como outlier. |
| WT-06 | Pesaje en horario o condiciones diferentes. | Menor confianza o advertencia; observación no se elimina automáticamente. |
| WT-07 | Cambio horario o viaje. | Fecha civil y ventana coherentes, sin duplicación. |
| WT-08 | Ventana menor que 30 días. | No presentar extrapolación como cambio mensual efectivamente observado. |

### 15.3 Casos de nutrición

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| NU-01 | Día sin entradas y no cerrado. | Ingesta desconocida, no 0 kcal. |
| NU-02 | Bebida de etiqueta 0 kcal confirmada. | Cero legítimo preservado. |
| NU-03 | Proteína ausente en ficha nutricional. | Proteína desconocida, no 0 g. |
| NU-04 | Envase de 1,5 L y consumo de 500 ml. | Cálculo limitado a 500 ml. |
| NU-05 | Producto por 100 g y porción en ml sin densidad. | Conversión no disponible; solicitar corrección del dato dentro del flujo de la app. |
| NU-06 | Entrada confirmada basada en etiqueta estimada. | Confirmación y naturaleza nutricional permanecen independientes. |
| NU-07 | Día cerrado con 30 % de energía estimada. | Estado CLOSED_WITH_ESTIMATES y confianza ajustada. |
| NU-08 | Edición posterior al cierre. | Revisión o reapertura y recálculo de derivados. |
| NU-09 | Modificación de un producto después de consumirlo. | La entrada histórica conserva versión y nutrientes previos. |
| NU-10 | Edición de una comida guardada después de registrarla. | Consumos históricos permanecen inalterados. |
| NU-11 | Día cero confirmado. | Revisión especial y bloqueo de recortes. |

### 15.4 Casos de TDEE

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| TD-01 | Ingesta cerrada y estable de 2100 kcal; peso estable durante 28 días. | TDEE converge aproximadamente hacia 2100 kcal dentro de un margen configurado. |
| TD-02 | Ingesta estable de 2100 kcal y pérdida robusta de 0,35 kg/sem. | Estimación alrededor de 2485 kcal usando el coeficiente inicial, con rango e incertidumbre. |
| TD-03 | Faltan tres o más días relevantes de nutrición. | Desciende confianza y puede bloquear recortes. |
| TD-04 | Subida transitoria de agua tras viaje o sesión intensa. | No se produce salto abrupto permanente ni cambio agresivo. |
| TD-05 | No existe perfil suficiente ni historial contemporáneo. | TDEE no disponible o prior manual; no se inventan datos personales. |
| TD-06 | Garmin informa energía total alta un día. | Se trata como contexto; no reemplaza estimación observacional. |
| TD-07 | Plan objetivo de 2050 pero ingesta confirmada de 2200. | El cálculo utiliza 2200, no el objetivo. |
| TD-08 | Cambio de plan en mitad de la ventana. | Segmentación o explicación explícita de la mezcla; no aprendizaje silencioso. |

### 15.5 Casos de readiness

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| RD-01 | HRV moderadamente baja una noche y otras señales estables. | Categoría estable o cambio prudente; sin puntuación numérica ni colapso artificial. |
| RD-02 | Sueño pobre, HRV baja, FC en reposo alta y carga elevada varios días. | Categoría y factores reflejan múltiples limitantes coherentes, sin score 0–100. |
| RD-03 | Garmin no publica HRV en Health Connect. | Función degradada o UNKNOWN; sin excepción ni HRV cero. |
| RD-04 | Registro de almuerzo. | Morning y Current Readiness no aumentan artificialmente. |
| RD-05 | Sesión intensa confirmada después de la lectura matinal. | En fase 6, Current Readiness puede cambiar de categoría con razón estructurada. |
| RD-06 | HRV, estrés y Body Battery provienen de procesos correlacionados. | Penalización agrupada; no triple conteo. |
| RD-07 | El sueño llega tarde tras sincronización. | Nueva revisión trazable proporcional a la fase; múltiples snapshots solo son obligatorios si Current Readiness los necesita. |
| RD-08 | Ausencia de baseline. | Resultado provisional o UNKNOWN con confianza reducida. |

### 15.6 Casos de evaluación del plan

Los escenarios `ADJUST_UP` y `ADJUST_DOWN` presuponen explícitamente modo `ADVISORY`, estimador `STABLE`, evidencia longitudinal persistente, histéresis satisfecha y ausencia de bloqueos. Con los mismos datos en `SHADOW`, el estado puede existir internamente, pero nunca se crea una propuesta operativa.

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| PE-01 | Objetivo -0,35; observado -0,34; recuperación y rendimiento observados estables. | MAINTAIN. |
| PE-02 | Objetivo -0,35; observado -0,75; deterioro fisiológico sostenido; datos buenos. | ADJUST_UP o revisión según guardrails, sin causalidad afirmada. |
| PE-03 | Objetivo -0,35; observado -0,08; datos completos; recuperación y rendimiento adecuados. | ADJUST_DOWN limitado a magnitud conservadora. |
| PE-04 | Dos pesos y varios días nutricionales faltantes. | INSUFFICIENT_DATA. |
| PE-05 | Recuperación baja un único día. | OBSERVE o MAINTAIN; sin cambio de plan. |
| PE-06 | Confianza 0,74 con indicio de pérdida lenta. | No autorizar ADJUST_DOWN. |
| PE-07 | Entrenamiento alto de pole y ballet; rendimiento y recuperación desconocidos. | No autorizar recorte. |
| PE-08 | Corrección aceptada hace menos de 14 días. | COOLDOWN_ACTIVE y no nueva propuesta. |
| PE-09 | Reducción colocaría el plan bajo el piso individual. | Bloqueo y motivo estructurado. |
| PE-10 | Cambio de plan mientras existe propuesta pendiente. | Propuesta expira. |
| PE-11 | Objetivo PERFORMANCE_PRIORITY y peso ligeramente superior. | No priorizar recorte sobre recuperación. |
| PE-12 | Día de enfermedad o lesión relevante. | Revisión contextual y bloqueo de recomendaciones restrictivas. |
| PE-13 | Rendimiento no informado. | Mostrar UNKNOWN; jamás declarar estable. |

### 15.7 Casos de integración y persistencia

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| IN-01 | La misma sesión entra por Health Connect y FIT. | Una actividad canónica y referencias a ambas procedencias. |
| IN-02 | Pasos del teléfono y Garmin en intervalos solapados. | Sin suma duplicada. |
| IN-03 | HRV soportada por esquema pero no escrita por Garmin. | Capacidad separada y estado SIN_DATOS. |
| IN-04 | Permiso revocado. | Integración degradada; peso y nutrición manual permanecen disponibles. |
| IN-05 | Token de cambios expirado. | Relectura acotada e idempotente. |
| IN-06 | Health Connect elimina registro externo. | Tombstone y recálculo de derivados afectados. |
| IN-07 | ZIP corrupto, sobredimensionado o con path traversal. | Rechazo seguro y error explicable. |
| IN-08 | Importación del mismo archivo dos veces. | Sin duplicación de observaciones. |
| IN-09 | Archivo con formato parcialmente desconocido. | Importación parcial segura y reporte honesto de cobertura. |
| IN-10 | Migración de versiones sucesivas de Room. | Datos y restricciones conservados; prueba completa de migraciones. |
| IN-11 | Restauración de respaldo propio. | Identidades, revisiones y relaciones conservadas; permisos se vuelven a gestionar. |

### 15.7.1 Materialización progresiva

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| AR-01 | Fase 1 sin persistencia ni adaptadores externos. | Dominio compila y se prueba sin Android, Room o Garmin. |
| AR-02 | Primera vertical de plan, peso y nutrición. | Panel útil con tablas mínimas; no existen corridas de auditoría ni cola durable. |
| AR-03 | Producto modificado después de un consumo con snapshot embebido. | La entrada histórica conserva nutrientes anteriores sin exigir `food_product_versions`. |
| AR-04 | Tendencia calculada desde mediciones reales. | No se exige tabla de snapshots si no aporta funcionalidad activa. |
| AR-05 | Estabilidad de TDEE habilitada. | Existe historial acotado suficiente; no se implementa auditoría exhaustiva. |
| AR-06 | Evaluador con histéresis después de reiniciar la app. | Se conserva memoria mínima por versión de plan. |
| AR-07 | Fase 3.5 completada sin Garmin. | Núcleo y validación funcionan completamente offline. |

### 15.7.2 Estabilidad del estimador TDEE

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| ES-01 | Estimaciones cercanas a 2400 durante 14 días con cobertura suficiente. | `STABLE` y métricas de dispersión explicables. |
| ES-02 | Serie `2450, 2310, 2420, 2300` con buena nutrición. | `UNSTABLE`; sin autorización de ajuste. |
| ES-03 | Inputs completos, pero solo dos fechas con estimación. | `INSUFFICIENT_HISTORY`, no estabilidad implícita. |
| ES-04 | Siete estimaciones mejorando sin horizonte completo. | `STABILIZING`; todavía sin propuesta. |
| ES-05 | Doce ejecuciones sobre los mismos inputs en un solo día. | Cuenta una fecha; no fabrica historia ni independencia. |
| ES-06 | Ventanas deslizantes altamente solapadas sin información nueva. | No se consideran confirmaciones independientes. |
| ES-07 | Deriva relevante entre períodos cronológicos consecutivos. | `UNSTABLE` aunque la dispersión diaria interna sea pequeña. |
| ES-08 | MAD aparentemente bajo, pero amplitud superior al límite. | `UNSTABLE`; una métrica favorable no compensa otra bloqueante. |
| ES-09 | Edición retrospectiva material de nutrición. | Revisión y recomputación desde el primer día afectado; estabilidad revalidada. |
| ES-10 | Confianza de inputs alta y estimador inestable. | Evaluación no operativa de ajuste; motivo `TDEE_ESTIMATOR_UNSTABLE`. |

### 15.7.3 Histéresis y memoria del evaluador

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| HY-01 | Desviación fluctúa cerca del umbral de entrada. | Permanece `OBSERVE` sin alternancia repetitiva. |
| HY-02 | Dos evaluaciones calificadas, separadas y con datos nuevos. | Entrada direccional permitida si el resto de compuertas autoriza. |
| HY-03 | Estado `ADJUST_DOWN` y desviación intermedia entre entrada y salida. | Mantiene estado seguro sin crear otra propuesta. |
| HY-04 | Desviación cruza umbral de salida. | Retorna a observación o mantenimiento según evidencia. |
| HY-05 | Aparece enfermedad, riesgo o confianza insuficiente. | Salida y bloqueo inmediatos, aunque histéresis indicara persistencia. |
| HY-06 | TDEE pasa de `STABLE` a `UNSTABLE`. | Cancela autorización y deja sin vigencia una propuesta pendiente. |
| HY-07 | Dos recalculaciones sobre idéntica revisión de inputs. | No satisfacen persistencia direccional. |
| HY-08 | Se acepta un cambio de plan. | Nueva versión, memoria reiniciada y cooldown activo. |
| HY-09 | Se rechaza una propuesta y no existe evidencia nueva. | No se vuelve a presentar diariamente. |
| HY-10 | Reinicio de la aplicación entre dos evaluaciones. | Se recupera memoria mínima sin introducir auditoría avanzada. |

### 15.7.4 Shadow Mode y activación operativa

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| SH-01 | Primer arranque del evaluador. | Modo `SHADOW`, nunca `ADVISORY` implícito. |
| SH-02 | Candidato interno `ADJUST_DOWN` plenamente calificado. | Observación hipotética guardada; cero propuestas operativas. |
| SH-03 | Candidato interno `ADJUST_UP` plenamente calificado. | Observación hipotética guardada; plan vigente sin cambios. |
| SH-04 | Usuario abre el panel durante validación. | Muestra `EN VALIDACIÓN`; sin botón de aceptar un objetivo hipotético. |
| SH-05 | Historial de cuatro semanas con entradas incompletas. | Shadow se prolonga; no se habilita asesoramiento. |
| SH-06 | TDEE oscila y aparecen candidatos alternantes. | Métricas de estabilidad y falsos cambios registrados; transición bloqueada. |
| SH-07 | Todos los criterios técnicos se cumplen sin acción del usuario. | Continúa `SHADOW`; activación requiere decisión explícita. |
| SH-08 | Usuario habilita `ADVISORY` tras validación. | Se realiza evaluación nueva; no se transforma retrospectivamente un candidato anterior. |
| SH-09 | Cambio significativo de algoritmo o política. | Validación acotada adicional antes de efectos operativos. |
| SH-10 | Corrección retrospectiva material durante sombra. | Historial afectado marcado y recompuesto; motivos consistentes. |
| SH-11 | Se incorpora recuperación después de validar el núcleo. | Readiness puede bloquear; su efecto nuevo se revisa en sombra específica. |
| SH-12 | Importación histórica altera sustancialmente tendencia o baselines. | Suspende propuestas y revalida estabilidad antes de reactivar asesoramiento. |

### 15.7.5 Recomendación diaria por modo

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| DR-01 | Plan de 2050 kcal sin actividad registrada. | Recomendado 2050; consumo independiente. |
| DR-02 | Plan de 2050 y Garmin informa 700 kcal activas. | En `BASE_ONLY`, recomendado sigue en 2050. |
| DR-03 | Entrenamiento intenso con TDEE inmaduro. | En `BASE_ONLY`, ninguna compensación diaria. |
| DR-04 | Health Connect comienza a sincronizar correctamente. | Sin activación explícita, continúa `BASE_ONLY`. |
| DR-05 | Actividad habitual incorporada al TDEE estable. | No se vuelve a compensar por duplicado. |
| DR-06 | Evento excepcional y `ADAPTIVE` validado. | Corrección limitada, explicable y nunca 1:1. |
| DR-07 | Se degrada baseline o aparecen duplicados. | Retorno seguro a `BASE_ONLY`. |
| DR-08 | Readiness alto o bajo sin actividad excepcional validada. | No altera el recomendado diario. |

### 15.7.6 Readiness categórico

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| RC-01 | Sueño y carga dentro del baseline. | `GOOD` con factores y confianza cualitativa. |
| RC-02 | Un factor moderadamente deteriorado. | `MODERATE` sin crear un score numérico. |
| RC-03 | Varios factores independientes deteriorados. | `LOW` y limitantes estructurados. |
| RC-04 | HRV no publicada, pero sueño y carga suficientes. | Categoría posible de cobertura limitada; HRV marcada no disponible. |
| RC-05 | Sin baseline ni señales suficientes. | `UNKNOWN`, no cero ni recuperación normal. |
| RC-06 | Esquema inicial de persistencia de recuperación. | Ninguna columna de score obligatoria antes de una migración futura justificada. |

### 15.7.7 Confianza cualitativa

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| CV-01 | Índice operacional interno `0,82`. | UI ordinaria muestra `ALTA`, no `82 %`. |
| CV-02 | Cobertura de nueve pesajes y 25 diarios completos. | Muestra cantidades y ventana reales como justificación. |
| CV-03 | Ingesta estimada equivalente al 18 % del total. | Muestra `18 % de ingesta estimada`, no probabilidad de acierto. |
| CV-04 | Excelente Garmin pero nutrición incompleta. | El factor limitante bloquea la decisión; cobertura wearable no lo compensa. |
| CV-05 | Inspector técnico avanzado habilitado. | Índice 0–1 visible con advertencia de que no es probabilidad. |

### 15.7.8 Spikes y dependencias empíricas

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| SP-01 | Spike A pendiente y fases locales activas. | Plan, peso, nutrición y sombra siguen disponibles. |
| SP-02 | Intento de crear adaptador específico sin Spike A. | Fase 4 no autorizada. |
| SP-03 | HRV existe en esquema, pero no se observa en Garmin. | Se clasifica como no disponible; no se diseña dependencia sobre ella. |
| SP-04 | Spike B sin exportación real. | Fase 5 bloqueada; no se selecciona FIT SDK ni parser especulativo. |
| SP-05 | Exportación contiene CSV útil, pero FIT no verificable. | Se prioriza CSV real y se difiere FIT. |
| SP-06 | Exportación carece de formato aprovechable. | Informe `COMPLETE_WITH_LIMITATIONS`; núcleo permanece funcional. |
| SP-07 | Teléfono y Garmin registran pasos simultáneos. | Spike documenta solapamiento antes de diseñar agregación. |

### 15.7.9 Separación entre readiness y dirección del plan

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| PR-01 | Readiness `LOW` aislado y peso dentro de objetivo. | `MAINTAIN` u `OBSERVE`; nunca `ADJUST_UP` por esa sola causa. |
| PR-02 | Readiness `GOOD` y peso dentro del objetivo. | No se autoriza `ADJUST_DOWN`. |
| PR-03 | Pérdida insuficiente sostenida, pero readiness bajo. | Readiness puede bloquear recorte; no cambia automáticamente la dirección a aumento. |
| PR-04 | Pérdida excesiva sostenida y recuperación deteriorada. | La dirección ascendente procede de la evolución longitudinal; recuperación contextualiza. |
| PR-05 | Readiness bueno y TDEE inestable. | Estabilidad sigue bloqueando cualquier propuesta. |
| PR-06 | Recuperación ausente y carga elevada. | Precaución o bloqueo según política; ausencia no se transforma en buena recuperación. |
| PR-07 | Almuerzo registrado y readiness previamente bajo. | La comida no altera categoría ni genera una corrección del plan. |

### 15.7.10 Simplicidad de la experiencia cotidiana

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| UX-01 | Panel normal con datos suficientes. | Muestra recuperación, energía, proteína, ritmo y estado sin índices técnicos. |
| UX-02 | `BASE_ONLY` y plan de 2050 kcal. | El resumen muestra `1470 / 2050 kcal`, sin repetir objetivos idénticos innecesariamente. |
| UX-03 | Readiness disponible solo como categoría. | Muestra `BUENA`, `MODERADA`, `BAJA` o no disponible, nunca 71/74. |
| UX-04 | Evaluación durante Shadow Mode. | Muestra `EN VALIDACIÓN`; sin propuestas accionables. |
| UX-05 | Usuario abre el detalle de la evaluación. | Accede a ventana, cobertura, estabilidad, motivos, algoritmo y política. |
| UX-06 | Nutrición o peso inexistentes. | Muestra desconocido o datos insuficientes; no rellena ceros ficticios. |
| UX-07 | Pantallas de Garmin o importación antes de sus fases. | No aparecen acciones vacías ni promesas de capacidades inexistentes. |

### 15.8 Pruebas de propiedades e invariantes

- Agregar una observación ausente no modifica totales numéricos ni equivale a cero.
- El orden de entrada no cambia resultados una vez aplicada la ordenación canónica.
- Ninguna propuesta descendente atraviesa su piso individual.
- Ninguna corrección excede los topes de política.
- Un plan nunca cambia sin evento explícito de aceptación o edición del usuario.
- Una actividad nunca incrementa dos veces el recomendado por duplicación de fuentes.
- Una comida individual no cambia readiness salvo corrección de una política acumulada legítima.
- Un resultado histórico se puede recalcular con su versión de algoritmo y snapshot.
- Ninguna serie de recalculaciones sobre los mismos datos fabrica estabilidad o persistencia direccional.
- Ninguna evaluación `SHADOW` crea una propuesta operativa ni permite aceptar una cifra hipotética.
- Ningún readiness por sí solo determina la dirección de una corrección nutricional.
- Ninguna actividad altera el recomendado diario mientras el modo sea `BASE_ONLY`.
- Ningún bloqueo de seguridad queda anulado por histéresis o una categoría positiva de recuperación.

### 15.9 Herramientas de prueba

- Tests JVM con fixtures sintéticos declarativos y reloj fijo.
- Pruebas Room con esquemas exportados y MigrationTestHelper.
- Pruebas Android de Health Connect con Health Connect Toolbox cuando corresponda.
- Datos reales anonimizados para FIT y CSV, nunca incluidos públicamente sin revisión.
- Golden files de razones estructuradas, no solamente capturas visuales.

[Pruebas oficiales de migraciones Room](https://developer.android.com/training/data-storage/room/migrating-db-versions) y [Health Connect Toolbox](https://developer.android.com/health-and-fitness/health-connect/test/health-connect-toolbox).

---

## 16. L. Observabilidad y reconstrucción

### 16.1 Trazabilidad proporcional a la fase

**Nivel obligatorio inicial — fases 2–3.5:**

1. Observaciones originales locales y revisiones lógicas conservadas dentro de sus propias tablas.
2. Identidad del algoritmo, versión y política utilizada.
3. Fecha de cálculo, ventana, plan vigente y revisión o corte temporal de inputs.
4. Resultado, categoría de calidad, estabilidad del TDEE y factores limitantes.
5. Cantidad de pesajes elegibles, días nutricionales completos y proporción estimada.
6. Razones estructuradas y principales exclusiones dentro de un resumen embebido.
7. Modo `SHADOW` o `ADVISORY`, autorización y carácter operativo.

Conservar revisiones, ventanas, política y corte temporal permite reconstruir resultados mediante consulta determinista a las observaciones originales sin exigir una tabla por cada input. No se presentan resultados como reproducibles byte a byte si se descartó información necesaria.

**Nivel ampliado — solo cuando exista una necesidad activa:**

- IDs individuales exhaustivos y orden completo de inputs por corrida.
- `AlgorithmRun`, `AlgorithmInput` y razones normalizadas.
- Huellas de configuración, observaciones y resultado.
- Snapshots múltiples, comparación automática de versiones y cola durable.
- Auditoría avanzada, exportaciones técnicas y simulación de políticas.

El paso al nivel ampliado se justifica por volumen, debugging real, importación compleja, múltiples políticas activas o requisitos de recuperación; no por anticipación arquitectónica.

### 16.2 Inspector de resultado

Cada área debe ofrecer un detalle equivalente a:

~~~text
Algoritmo                    plan-evaluator
Versión                      1.1.0
Política                     conservative-2026-08
Fecha de cálculo             23/08/2026 09:15
Ventana                      28 días
Plan evaluado                versión 3
Pesajes incluidos            9
Pesajes excluidos            1; outlier aislado
Días nutricionales válidos   25
Ingesta estimada             18 %
Estabilidad del TDEE         estable
HRV                          no disponible por Health Connect
Sueño                        23 noches
Rendimiento                  4 observaciones
Confianza                    alta
Modo                         advisory; o shadow claramente no operativo
Resultado                    mantener
~~~

El inspector técnico avanzado puede mostrar `índice interno = 0,84` con una advertencia explícita: indicador operacional no probabilístico. El detalle ordinario no necesita mostrar IDs, hashes, motivos normalizados ni puntuaciones fisiológicas inexistentes.

### 16.3 Recomposición histórica

- Un algoritmo nuevo puede ejecutarse sobre observaciones originales.
- Los resultados nuevos se almacenan como otra versión o corrida solo cuando la funcionalidad activa requiere conservar ambos; inicialmente basta una revisión trazable o el historial mínimo previsto.
- Las decisiones efectivamente aceptadas permanecen históricas y no se alteran retroactivamente.
- Si ya no existen archivos originales, se informa el nivel real de reproducibilidad.
- El sistema distingue resultado histórico original de resultado recalculado posteriormente.

La edición retrospectiva de un peso o consumo no altera silenciosamente una evaluación anterior: se registra una nueva revisión, se identifica la ventana afectada y se vuelve a calcular el estado operativo antes de permitir una propuesta.

### 16.4 Logs y privacidad

- Logs técnicos de producción: identificadores locales mínimos, tipos y códigos de error.
- Inspector local: valores sensibles visibles únicamente dentro de la aplicación.
- Exportación de diagnóstico: redactada por defecto y voluntaria.
- No incluir nombres completos de alimentos, pesos o HRV en telemetría externa.

### 16.5 Observabilidad de Shadow Mode

El historial local de validación debe permitir revisar, como mínimo:

1. Estados candidatos y efectivos por fecha, sin presentar recomendaciones hipotéticas como acciones reales.
2. Evolución del TDEE, estabilidad, amplitud y deriva entre períodos.
3. Sensibilidad a pesos atípicos, días nutricionales incompletos y proporción estimada.
4. Cantidad y motivo de cambios de estado, entradas/salidas por histéresis y bloqueos inmediatos.
5. Candidatos `ADJUST_UP` o `ADJUST_DOWN` considerados falsos, inseguros o insuficientemente explicados.
6. Efecto de enfermedad, viajes, semanas atípicas y correcciones retrospectivas.
7. Coherencia de motivos con el plan y con las observaciones realmente disponibles.
8. Diferencia entre una limitación del algoritmo y falta de evidencia personal.

La revisión es local, explícitamente no clínica y proporcional: una fila de `plan_evaluations` con resumen estructurado por ejecución es suficiente hasta demostrar necesidad de auditoría exhaustiva.

---

## 17. M. Roadmap y orden de implementación

### 17.1 Fase 0: descubrimiento y contrato

**Objetivo:** aprobar esta especificación y abrir dos líneas empíricas separadas antes de asumir capacidades específicas.

Entregables:

- Especificación v1.1, umbrales provisionales, políticas versionadas y casos sintéticos.
- Spike A: matriz efectiva Garmin/Health Connect del teléfono, cuando el dispositivo esté disponible.
- Spike B: inventario de una exportación Garmin autorizada, cuando exista una muestra real.
- Estado formal de cada compuerta y limitaciones conocidas.

**Salida:** los spikes son obligatorios para sus integraciones, no una barrera para fases 1–3.5. No se diseña parser ni dependencia Garmin sobre supuestos documentales.

### 17.2 Fase 1: dominio mínimo

**Alcance:**

- Proyecto Android/Kotlin básico y módulo de dominio Kotlin puro.
- Entidades mínimas de plan, peso, alimento, entrada y ausencia explícita.
- Unidades, fechas civiles, fuente manual, revisión y reloj inyectable.
- Tests JVM de invariantes, ausencias, unidades y separación de capas.
- Configuración inicial de privacidad y respaldo apropiada al momento en que aparezcan datos personales.

**No incluye:** todas las entidades objetivo, Room completo, auditoría, Garmin, tendencias, TDEE, readiness, integraciones ni clases vacías.

**Salida:** dominio mínimo compilable y testeable sin Android; base preparada para la primera vertical útil.

### 17.3 Fase 2: primera vertical de plan, peso y nutrición

**Vertical 2a — panel útil:**

1. Definir un plan nutricional versionado.
2. Registrar peso manual sin frecuencia diaria obligatoria.
3. Registrar productos y consumos manuales con nutrientes conocidos o ausentes.
4. Persistir observaciones y revisiones mínimas en Room.
5. Gestionar apertura/cierre explícito del diario.
6. Mostrar energía, proteína y peso observado en un panel Compose sencillo.
7. Aplicar `BASE_ONLY`: recomendado diario idéntico al plan base.

**Vertical 2b — baja fricción:**

- Favoritos y repetición de recientes mediante consulta de consumos.
- Comidas guardadas con cantidades editables.
- Distinción entre confirmado, estimado y pendiente.
- Snapshot nutricional inmutable dentro de cada entrada.

**Salida:** aplicación realmente útil, offline, sin Health Connect, estimador adaptativo, evaluator operativo ni tablas de funcionalidades futuras.

### 17.4 Fase 3: núcleo diferenciador en tres verticales

**Fase 3a — `Peso → WeightTrend`.**

- Tendencia robusta sobre observaciones reales.
- Cobertura, distribución, outliers y confianza cualitativa.
- Recomputación bajo demanda; sin snapshots obligatorios.
- Casos WT-01–WT-08 y AR-04.

**Fase 3b — `Peso + nutrición → TDEE`.**

- Prior explícito y TDEE observacional con ventanas contemporáneas.
- Calidad de peso/nutrición por separado.
- Historial cronológico mínimo de estimaciones.
- `EstimatorStability` y bloqueo ante oscilación.
- Casos TD-01–TD-08 y ES-01–ES-10.

**Fase 3c — `Plan + TDEE + tendencia → PlanEvaluator`.**

- Cinco estados de evaluación interna.
- Razones estructuradas, guardrails, cooldown e histéresis versionada.
- Memoria mínima de transición y registro de observaciones.
- Modo inicial `SHADOW`; sin tabla ni flujo de propuestas aceptables.
- Contexto manual mínimo y rendimiento opcional cuando realmente aporte evidencia.

**Salida:** el sistema calcula su diferencial y lo explica, pero todavía no ofrece ajustes operativos. La validación personal constituye una fase independiente obligatoria.

### 17.5 Fase 3.5: Shadow Mode y validación real

**Objetivo:** verificar que un algoritmo correcto en fixtures también se comporta sensatamente con datos personales reales antes de habilitar asesoramiento.

Durante esta fase:

- El plan real permanece estable y elegido por la persona.
- Una edición manual voluntaria continúa permitida, pero reinicia la ventana de validación correspondiente a la nueva versión del plan.
- El evaluador calcula candidatos y estados efectivos conforme a políticas vigentes.
- Se persisten observaciones hipotéticas mínimas sin `AdjustmentProposal`.
- El panel indica `EN VALIDACIÓN` y conserva objetivo, alimentación y tendencia normales.
- Se revisan sensibilidad a outliers, ingesta incompleta, semanas anómalas, revisiones retrospectivas, estabilidad del TDEE y consistencia de razones.
- Una ejecución sintética complementa la revisión real; no sustituye la observación personal.

**Criterios formales de salida iniciales:**

1. Ventana objetivo de 28 días personales realmente evaluables, con al menos 14 días de operación prospectiva; extender si falta evidencia.
2. Al menos ocho días de pesaje distribuidos y cobertura nutricional elegible de aproximadamente 85 % o más.
3. Proporción energética estimada dentro de la política vigente para el tipo de decisión.
4. TDEE `STABLE` en al menos siete fechas evaluadas distintas, sin oscilaciones críticas ni deriva persistente.
5. Ninguna propuesta hipotética manifiestamente insegura; revisar cada candidato direccional y su evidencia.
6. Sin alternancias direccionales injustificadas; toda transición debe poder explicarse por datos nuevos, salida de banda o bloqueo.
7. Motivos, calidad, ventana y estado de seguridad consistentes con las observaciones reales.
8. Reproducción determinista de los casos inspeccionados con su política y revisión de inputs.
9. Validación explícita de al menos un outlier, un día incompleto y una corrección retrospectiva mediante escenarios reales o replay local.
10. Revisión humana y acción explícita de activación de `ADVISORY`.

Si no aparece naturalmente un caso real de `ADJUST_UP` o `ADJUST_DOWN`, se complementa con replay local y fixtures; no se fuerza a la persona a modificar su dieta para producir un escenario.

**Salida:** asesoramiento opcional autorizado; cualquier propuesta futura sigue siendo conservadora, explicable y sujeta a aceptación manual. La persona puede mantener `SHADOW` indefinidamente.

### 17.6 Fase 4: Health Connect y recuperación categórica

**Compuerta previa:** Spike A documentado y con al menos una capacidad útil realmente observada.

**Alcance:**

- Permisos mínimos, diagnóstico por tipo y origen efectivo.
- Sueño, pasos, frecuencia cardíaca, energía o sesiones solamente cuando existan.
- Baselines personales realmente viables.
- Morning Readiness `GOOD`, `MODERATE`, `LOW` o `UNKNOWN`, sin score 0–100.
- Factores limitantes y ausencia explícita de HRV, estrés u otras señales.
- Readiness como contexto o bloqueo del evaluador; nunca como origen de dirección energética.
- Sincronización al abrir y trabajo de fondo únicamente si está autorizado.
- Recomendación diaria todavía `BASE_ONLY`.

**Validación complementaria:** las nuevas señales de recuperación se observan inicialmente en una sombra acotada respecto del evaluador ya validado. Una señal de riesgo puede bloquear una propuesta inmediatamente; ninguna nueva métrica habilita por sí sola una recomendación adicional.

**Salida:** integración útil que enriquece contexto sin reabrir aprendizaje energético simultáneo ni invalidar el núcleo local.

### 17.7 Fase 5: importación histórica Garmin enfocada

**Compuerta previa:** Spike B documentado, muestra autorizada y un formato realmente útil.

**Alcance:**

- Primer parser CSV, FIT u otro seleccionado exclusivamente por valor observado.
- Normalización, procedencia, idempotencia y deduplicación frente a Health Connect.
- Historial útil para baselines personales y métricas verdaderamente presentes.
- Reporte exacto de cobertura, granularidad, límites y datos no importables.
- Revisión del TDEE y del evaluador si la importación cambia materialmente una ventana activa.

**Salida:** no se promete nutrición histórica ni TDEE adaptativo retrospectivo sin ingesta contemporánea. Un cambio sustancial de baseline suspende propuestas hasta recuperar estabilidad y validación apropiadas.

### 17.8 Fase 6: readiness intradiario

**Alcance:**

- Current Readiness categórico vinculado a la lectura matinal.
- Cambios explicables por entrenamiento, carga o señales realmente recibidas.
- Snapshots intradiarios únicamente cuando la funcionalidad los necesita.
- Recomputación oportunista, no continua ni basada en cada alimento.
- Evaluación longitudinal todavía separada de la preparación del momento.

**Salida:** contexto intradiario útil sin score artificial obligatorio y sin alterar automáticamente el plan nutricional.

### 17.9 Fase 7: código de barras y fotografía modular

**Fase 7a — código de barras:** proveedor abierto reemplazable, caché Room, tolerancia offline y confirmación de cantidades.

**Fase 7b — fotografía posterior:** reconocimiento local o externo opcional, rangos, confirmación del alimento y de la cantidad efectivamente consumida; ningún proveedor pago es obligatorio.

Los envases abiertos, reconocimiento avanzado y funciones adicionales se evalúan después de que el registro manual y las comidas guardadas demuestren baja fricción.

### 17.10 Activaciones independientes y alcance de MVP

| Capacidad | Primera fase posible | Compuerta real | Estado predeterminado |
| --- | --- | --- | --- |
| Panel offline de nutrición, peso y plan. | 2. | Primera vertical completa. | Activo. |
| Tendencia y TDEE observacional. | 3a–3b. | Datos y cobertura suficientes. | Informativo. |
| Evaluación del plan. | 3c. | Motores, estabilidad e histéresis implementados. | `SHADOW`. |
| Propuestas energéticas manualmente aceptables. | Después de 3.5. | Shadow aprobado, TDEE estable y activación explícita. | Desactivadas hasta `ADVISORY`. |
| Health Connect y readiness matinal. | 4. | Spike A, permisos y métricas observadas. | Categórico y contextual. |
| Importación histórica. | 5. | Spike B y un formato real viable. | Desactivada hasta tener parser válido. |
| Readiness intradiario. | 6. | Señales y eventos reales. | No disponible antes de implementarse. |
| Compensación diaria adaptativa. | Fase posterior a 4 o V2. | TDEE y actividad estables, validación específica y activación explícita. | `BASE_ONLY`. |
| Código de barras y fotografía. | 7a y 7b. | Proveedor reemplazable, permisos y privacidad. | Opcionales. |

**Límites de MVP:** fase 2 entrega una aplicación local útil; fase 3 entrega el diferenciador calculado pero en validación; fase 3.5 cierra el **MVP diferenciador validado** y permite activar asesoramiento. Fase 4 entrega el MVP integrado con Garmin mediante Health Connect; fase 5 añade historial únicamente cuando existe evidencia empírica. Ninguna integración bloquea el valor local.

---

## 18. N. Contrato de trabajo posterior con Codex

### 18.1 Regla general

Cada etapa se implementa por separado y entrega una vertical verificable. No se solicita construir toda la aplicación en una sola instrucción ni materializar anticipadamente todos los módulos, puertos, entidades o tablas de este documento. Cada encargo declara fase, objetivo, límites, invariantes, pruebas y criterio de cierre.

### 18.2 Encargo 1: dominio mínimo y proyecto Android

~~~text
Implementar exclusivamente el esqueleto Android/Kotlin y el dominio mínimo de
plan, peso, alimento, ausencia, unidades, fecha y reloj inyectable.

No crear Room completo, core:engine vacío, integraciones Garmin, tablas futuras,
auditoría, reconocimiento fotográfico ni parsers.

Cierre: dominio Kotlin puro compilable sin Android, tests de invariantes verdes
y ninguna observación ausente representada como cero.
~~~

### 18.3 Encargo 2: primera vertical completa y útil

~~~text
Implementar Plan + Peso + Nutrición manual → persistencia Room mínima → panel
Compose útil. Añadir solo las tablas user_profiles, nutrition_plan_versions,
weight_measurements, food_products, food_entries y nutrition_diary_days, o una
estructura equivalente que preserve sus invariantes.

Conservar revisiones locales y snapshot nutricional inmutable dentro de cada
entrada. El modo diario es BASE_ONLY: recomendado_hoy = plan_base.

No crear AlgorithmRun, AlgorithmInput, recalculation_queue, tablas de importación,
fuentes Garmin, snapshots derivados ni propuestas.

Cierre: plan, peso, consumo, proteína y restante funcionan offline; ausencia no
equivale a cero; correcciones no modifican consumos históricos.
~~~

### 18.4 Encargo 3: reducción de fricción nutricional

~~~text
Añadir favoritos, recientes y comidas guardadas sobre la vertical ya funcional.
Recientes debe comenzar como consulta sobre food_entries; crear tablas adicionales
solo si una función concreta las necesita.

Cubrir NU-01 a NU-11, distinguiendo envase de consumo, estimado de confirmado y
día abierto de diario completo.

Cierre: repetir alimentos y desayunos en pocos pasos sin alterar el historial.
~~~

### 18.5 Encargo 4: peso y WeightTrend

~~~text
Implementar WeightTrendCalculator y calidad específica de peso sobre observaciones
reales. Resolver WT-01 a WT-08 y AR-04 sin inventar pesajes ni exigir snapshots
persistentes anticipados.

Cierre: tendencia robusta, outliers tratados prudentemente y confianza cualitativa
explicada mediante cobertura y distribución.
~~~

### 18.6 Encargo 5: TDEE y estabilidad explícita

~~~text
Implementar TdeeEstimator, calidad nutricional y EstimatorStability. Materializar
únicamente el historial cronológico mínimo de tdee_estimates necesario para
calcular estabilidad. Cubrir TD-01 a TD-08 y ES-01 a ES-10.

Diferenciar inputs de buena calidad y estimador inestable. No usar objetivo del
plan como ingesta real ni tratar recalculaciones repetidas como datos nuevos.

Cierre: TDEE provisional o adaptativo explicable; estados INSUFFICIENT_HISTORY,
UNSTABLE, STABILIZING y STABLE reproducibles.
~~~

### 18.7 Encargo 6: PlanEvaluator, histéresis y sombra

~~~text
Implementar PlanEvaluator con razones estructuradas, compuertas de calidad,
estabilidad, seguridad, cooldown e histéresis versionada. Persistir únicamente
plan_evaluations y decision_state_memory, o estructuras mínimas equivalentes.

El modo inicial es SHADOW. No crear AdjustmentProposal ni acciones de aceptación.
Cubrir PE-01 a PE-13, HY-01 a HY-10, SH-01 a SH-04 y PR-01 a PR-07 aplicables.

Cierre: los cinco estados existen internamente; el panel muestra EN VALIDACIÓN;
ningún cálculo modifica el plan ni se presenta como propuesta operativa.
~~~

### 18.8 Encargo 7: validación personal de Shadow Mode

~~~text
Implementar inspector local proporcional, replay determinista y revisión de
estabilidad, outliers, días incompletos, transiciones e inputs corregidos.

Evaluar datos personales reales contra los diez criterios de salida de fase 3.5.
No habilitar ADVISORY automáticamente ni alterar el plan para fabricar fixtures.

Cierre: informe de validación local; criterios cumplidos o bloqueos explícitos;
casos SH-05 a SH-12 y ES/HY relevantes verificados.
~~~

### 18.9 Encargo 8: asesoramiento manual opcional

~~~text
Solo después de aprobar Shadow Mode y recibir activación explícita, implementar
la transición a ADVISORY y el flujo mínimo de AdjustmentProposal.

Cada propuesta debe provenir de una evaluación nueva, aplicar guardrails, mostrar
razones y esperar aceptación humana. Cancelar propuestas ante nuevas revisiones,
riesgo, TDEE inestable o cambio de plan.

Cierre: el plan nunca cambia automáticamente y los candidatos previos de SHADOW
jamás se vuelven propuestas retroactivas.
~~~

### 18.10 Encargo 9: Health Connect y readiness categórico

~~~text
Verificar primero el informe completo del Spike A. Implementar únicamente los
tipos, permisos y orígenes realmente observados en el teléfono usado.

Añadir baselines viables y Morning Readiness GOOD/MODERATE/LOW/UNKNOWN. No crear
score 0–100; no asumir HRV, Body Battery, estrés, Recovery Time ni VO₂ max.
Mantener BASE_ONLY aunque aparezcan calorías o entrenamientos Garmin.

Cierre: casos IN-01 a IN-06, RD-01 a RD-08 aplicables, RC-01 a RC-06, PR-01 a
PR-07 y DR-01 a DR-04; readiness solo contextualiza o bloquea.
~~~

### 18.11 Encargo 10: importación Garmin empíricamente verificada

~~~text
Verificar primero el informe completo del Spike B y una muestra autorizada.
Implementar exclusivamente el parser prioritario que los archivos reales confirmen.

Normalizar, deduplicar, registrar procedencia y recalcular baselines compatibles;
suspender propuestas si una importación altera materialmente evidencia activa.

Cierre: importación idempotente y reporte honesto; ninguna afirmación sobre HRV,
FIT o TDEE histórico sin archivos e ingesta contemporánea.
~~~

### 18.12 Encargos posteriores

Current Readiness intradiario, compensación ADAPTIVE, código de barras, fotografía, snapshots avanzados y auditoría normalizada se solicitan individualmente cuando superan sus compuertas específicas. La compensación requiere una sombra propia y nunca se activa como efecto secundario de sincronizar Garmin.

### 18.13 Condiciones generales de aceptación para Codex

- Presentar el alcance exacto de la vertical antes de modificar archivos significativos.
- Mantener dependencias dirigidas al dominio y crear solo módulos efectivamente necesarios.
- Agregar tests deterministas junto con cada motor, transición y política.
- No incorporar Python, backend obligatorio, scraping Garmin ni APIs pagas.
- No introducir clases gigantes, auditoría anticipada, tablas vacías o dependencias especulativas.
- No inventar métricas ausentes, confianza probabilística ni score de recuperación.
- Conservar observaciones, revisiones, esquema exportado y migraciones no destructivas.
- Mantener SHADOW y BASE_ONLY hasta que su activación independiente esté expresamente autorizada.
- Documentar algoritmo, política, riesgos y razones de cada entrega proporcionalmente a la fase.

---

## 19. O. Riesgos, supuestos y decisiones abiertas acotadas

### 19.1 Registro de riesgos

| ID | Riesgo | Impacto | Mitigación |
| --- | --- | --- | --- |
| RK-01 | Garmin no publica HRV ni FC en reposo a Health Connect. | Readiness con menos señales. | Degradación explícita y exploración de FIT de bienestar real. |
| RK-02 | Exportación FIT no incluye métricas propietarias deseadas. | Baseline histórico incompleto. | Mostrar cobertura por archivo; no bloquear producto principal. |
| RK-03 | Falta nutrición histórica para aprender TDEE desde el inicio. | Arranque lento del estimador adaptativo. | Prior manual/poblacional opcional y transición honesta a modo observacional. |
| RK-04 | Registro alimentario incompleto. | Estimación y ajuste inseguros. | Cierre de diario, confianza y bloqueo de propuestas. |
| RK-05 | Calorías wearable poco representativas para pole/ballet. | Compensación equivocada. | Modalidad, esfuerzo percibido opcional y topes conservadores. |
| RK-06 | Duplicación entre teléfono, reloj y archivos. | Sobreestimación de carga y gasto. | Agregación, ID externo y conciliación. |
| RK-07 | Variaciones de agua durante viajes o entrenamiento. | Tendencia aparente incorrecta. | Estimador robusto, contexto y ventanas suficientes. |
| RK-08 | Copia automática de Room a la nube. | Violación de privacidad esperada. | Exclusiones de backup y exportación cifrada voluntaria. |
| RK-09 | Cambios demasiado frecuentes de plan. | Realimentación y ausencia de evidencia comparable. | Planes versionados, cooldown y expiración de propuestas. |
| RK-10 | Interpretación médica excesiva. | Riesgo sanitario y expectativas falsas. | Lenguaje no causal, alertas no diagnósticas y restricciones de seguridad. |
| RK-11 | Dependencias alpha o cambios de proveedor. | Inestabilidad de build o capacidades. | Preferencia por versiones estables y puertos reemplazables. |
| RK-12 | Series fisiológicas demasiado grandes. | Consumo excesivo de almacenamiento o batería. | Retención, agregación, lotes e índices. |
| RK-13 | Materializar arquitectura objetivo completa en el primer commit. | Complejidad accidental, migraciones innecesarias y menor velocidad de validación. | Verticales completas, esquema mínimo y materialización condicionada por funcionalidad activa. |
| RK-14 | TDEE oscilante con inputs aparentemente excelentes. | Ajustes incorrectos pese a cobertura nutricional alta. | `EstimatorStability` independiente, historial mínimo y bloqueo hasta `STABLE`. |
| RK-15 | Convertir una simulación de Shadow Mode en propuesta real. | Cambio de plan sin validación adecuada o engaño de interfaz. | Separar candidato, estado efectivo y autorización; `operational = false`; ninguna propuesta en `SHADOW`. |
| RK-16 | Histéresis mantiene una decisión cuando aparece riesgo. | Persistencia de una sugerencia insegura. | Compuertas duras y salida inmediata tienen precedencia absoluta. |
| RK-17 | Documentación de Garmin confundida con disponibilidad real. | Dependencias sobre métricas o formatos inexistentes. | Spike A y Spike B como compuertas específicas; núcleo local independiente. |
| RK-18 | Activar compensación de ejercicio antes de estabilizar TDEE habitual. | Doble conteo y realimentación energética. | `BASE_ONLY` por defecto; activación `ADAPTIVE` independiente y validada. |
| RK-19 | Presentar índice 0–1 o readiness categórico como precisión fisiológica. | Falsa interpretación estadística o confianza excesiva. | Confianza cualitativa, evidencia de cobertura y ausencia de score 0–100 inicial. |
| RK-20 | Importar historia o corregir diarios después de aprobar una propuesta. | Evidencia operativa desactualizada. | Revisiones, invalidación material, expiración de propuesta y revalidación de estabilidad. |

### 19.2 Supuestos provisionales

1. La primera versión se utiliza por una sola persona y en un teléfono Android.
2. El teléfono soporta Health Connect si se encuentra en Android 9 o superior y tiene configuración compatible.
3. El registro nutricional inicial se realiza dentro de la propia aplicación.
4. El objetivo y los límites individuales son configuraciones explícitas, no inferencias automáticas.
5. Las métricas del reloj se tratan como observaciones indirectas o estimaciones según su naturaleza.
6. La importación histórica depende de archivos autorizados y realmente disponibles.
7. El índice de confianza no constituye una probabilidad médica ni estadística.
8. La estabilidad del estimador debe medirse sobre estimaciones reales cronológicamente distribuidas.
9. Las fases locales no necesitan que los spikes de proveedores ya estén completos.
10. Las activaciones de asesoramiento y compensación diaria son independientes y explícitas.

### 19.3 Decisiones que requieren datos, no bloqueo anticipado

- Qué métricas aparecen realmente en Health Connect del teléfono concreto.
- Qué estructura contiene la exportación Garmin concreta.
- Si FIT de bienestar expone HRV, estrés y otros campos aprovechables.
- Qué duración y cadencia de sincronización resultan apropiadas para el dispositivo.
- Qué umbrales reducen falsos positivos sobre series personales reales.
- Qué amplitud, deriva y duración describen estabilidad suficiente en las series personales observadas.
- Cuándo un cambio de algoritmo, importación o corrección retrospectiva amerita volver a sombra.
- Si un score 0–100 o compensación diaria aporta información útil adicional después de validar alternativas más simples.
- Si el modelo de amenaza exige cifrado de la base además del aislamiento y cifrado del dispositivo.
- Si el piso energético debe configurarse manualmente o derivarse de una política profesional externa.

Ninguna de estas decisiones autoriza a inventar datos; todas se resuelven mediante spike, configuración explícita o degradación segura.

### 19.4 Decisiones de arquitectura registradas

- ADR-001: Kotlin exclusivo en el código propio; dependencias JVM interoperables permitidas.
- ADR-002: Room local como fuente de verdad.
- ADR-003: Health Connect como integración principal autorizada, sin asumir paridad con Garmin Connect.
- ADR-004: API Garmin empresarial excluida del camino crítico.
- ADR-005: Procedencia, naturaleza, presencia y confirmación como ejes independientes.
- ADR-006: Plan base, recomendado diario e ingesta como conceptos separados.
- ADR-007: Tendencias y motores separados por escala temporal.
- ADR-008: Días nutricionales con cierre explícito.
- ADR-009: Propuestas conservadoras sujetas a aceptación manual.
- ADR-010: Métricas desconocidas no se convierten en cero ni se presentan como normales.
- ADR-011: Rendimiento específico de disciplina requiere evidencia específica.
- ADR-012: Umbrales y algoritmos versionados, auditables y recalculables.
- ADR-013: MVP funcional antes de integraciones propietarias complejas.
- ADR-014: Contextos sensibles bloquean restricciones y nunca constituyen diagnóstico.
- ADR-015: Arquitectura objetivo y materialización inicial son planos distintos; implementación estrictamente vertical.
- ADR-016: Readiness inicial categórico; score numérico diferido hasta demostrar utilidad incremental.
- ADR-017: Confianza visible cualitativa; índice interno versionado y no probabilístico.
- ADR-018: Estabilidad del TDEE independiente de la calidad de sus inputs y necesaria para propuestas.
- ADR-019: Histéresis por política y versión de plan, subordinada a salidas inmediatas de seguridad.
- ADR-020: `SHADOW` predeterminado; `ADVISORY` solo después de validación y activación explícita.
- ADR-021: `BASE_ONLY` predeterminado; compensación `ADAPTIVE` condicionada y separada del estimador habitual.
- ADR-022: Spikes empíricos por proveedor bloquean integraciones específicas, nunca el núcleo local.
- ADR-023: Readiness contextualiza o bloquea; la dirección energética exige evidencia longitudinal independiente.
- ADR-024: Trazabilidad mínima embebida inicialmente; auditoría normalizada solo cuando una funcionalidad activa la requiere.
- ADR-025: Dashboard cotidiano reducido; explicación, ventanas y procedencia disponibles en detalle.

---

## 20. P. Criterios de aceptación global

El proyecto cumple esta especificación cuando:

1. La aplicación puede instalarse y utilizarse sin backend, cuenta o Python.
2. Todo el código propio de aplicación y dominio está escrito en Kotlin.
3. El núcleo de dominio y cálculo se prueba sin Android.
4. Un plan nutricional versionado existe independientemente del reloj.
5. El registro nutricional manual, favoritos, recientes y comidas guardadas funcionan offline.
6. Una bebida de 0 kcal se distingue de un día nutricional desconocido.
7. Pesajes irregulares generan tendencia solamente cuando la evidencia lo permite.
8. Un outlier aislado no cambia abruptamente tendencia, TDEE ni plan.
9. El TDEE adaptativo utiliza ingesta real y peso en ventanas comparables.
10. Sin datos suficientes, la evaluación informa INSUFFICIENT_DATA u OBSERVE.
11. La ausencia de HRV, estrés, Body Battery o Recovery Time no provoca fallos ni valores inventados.
12. El readiness inicial utiliza exclusivamente categoría, factores limitantes, cobertura y confianza cualitativa.
13. Actividad del Garmin y del teléfono no se duplica.
14. Plan base, recomendado hoy y consumido se presentan por separado.
15. Mientras rige `BASE_ONLY`, ninguna sesión modifica el recomendado diario; en un eventual modo adaptativo, nunca se compensa automáticamente el 100 % de calorías wearable.
16. Ningún cambio de plan se aplica sin aceptación explícita.
17. Una propuesta restrictiva se bloquea ante baja confianza, rendimiento desconocido bajo alta carga, riesgo o piso individual indefinido.
18. Cada cálculo relevante conserva versiones, ventana, revisiones de entradas, resultado, calidad y razones con una traza proporcional a la fase.
19. Las migraciones Room preservan información personal.
20. Las copias automáticas y llamadas externas respetan la política local-first.
21. Una importación Garmin informa exactamente qué encontró y qué no encontró.
22. El sistema puede decidir no cambiar nada y justificar esa decisión.
23. El dominio, módulos, puertos y tablas crecen únicamente cuando una vertical funcional los necesita.
24. Un historial mínimo permite distinguir TDEE `INSUFFICIENT_HISTORY`, `UNSTABLE`, `STABILIZING` y `STABLE`.
25. Alta cobertura de datos jamás autoriza una propuesta si el estimador sigue inestable.
26. La histéresis evita oscilaciones alrededor de un umbral sin retrasar salidas ante riesgo o revisión material.
27. Shadow Mode conserva observaciones hipotéticas, pero no crea propuestas ni altera el plan.
28. La transición a `ADVISORY` exige validación personal, activación explícita y evaluación nueva.
29. Readiness aislado nunca determina la dirección de un cambio nutricional.
30. El panel cotidiano muestra categorías y resúmenes; no presenta score de readiness ni porcentaje ficticio de confianza.
31. Spike A precede la integración Health Connect específica; Spike B precede la selección del parser Garmin.
32. La falta de teléfono, permiso o exportación no bloquea plan, peso, nutrición ni validación local.
33. Activar compensación `ADAPTIVE` requiere una validación independiente y puede permanecer desactivada en todo el MVP integrado.
34. Editar inputs o importar historia relevante invalida propuestas afectadas y reevalúa estabilidad.
35. La versión v1.1 preserva los requisitos sólidos de v1.0 y documenta la trazabilidad de cada cambio.

---

## 21. Fuentes primarias y documentación oficial

1. [Garmin: presentación oficial de vívoactive 6](https://www.garmin.com/en-US/newsroom/press-release/wearables-health/meet-vivoactive-6-the-latest-health-and-fitness-smartwatch-from-garmin/).
2. [Garmin: manual de indicadores del vívoactive 6](https://www8.garmin.com/manuals/webhelp/GUID-8C2C402F-55AC-431F-9CF2-1442B89CE149/EN-US/GUID-97EA1540-A780-480F-BA4D-9A9E147FB225.html).
3. [Garmin: estado de variabilidad de frecuencia cardíaca](https://www8.garmin.com/manuals/webhelp/GUID-8C2C402F-55AC-431F-9CF2-1442B89CE149/EN-US/GUID-9282196F-D969-404D-B678-F48A13D8D0CB.html).
4. [Garmin: compartir datos con Health Connect](https://support.garmin.com/en-US/?faq=JToBEy0jfe6pIygark2Ui5).
5. [Garmin: exportar datos desde Garmin Connect](https://support.garmin.com/en-US/?faq=W1TvTPW8JZ6LfJSfK512Q8).
6. [Garmin Developers: programa y preguntas frecuentes](https://developer.garmin.com/gc-developer-program/program-faq/).
7. [Garmin Developers: Health API](https://developer.garmin.com/gc-developer-program/health-api/).
8. [Garmin Developers: FIT SDK](https://developer.garmin.com/fit/get-the-sdk/).
9. [Android Developers: tipos de datos y permisos de Health Connect](https://developer.android.com/health-and-fitness/health-connect/data-types).
10. [Android Developers: disponibilidad de Health Connect](https://developer.android.com/health-and-fitness/health-connect/availability).
11. [Android Developers: lectura de datos, historial y pasos](https://developer.android.com/health-and-fitness/health-connect/read-data).
12. [Android Developers: sincronización y tokens](https://developer.android.com/health-and-fitness/health-connect/sync-data).
13. [Android Developers: publicación de versiones Room](https://developer.android.com/jetpack/androidx/releases/room).
14. [Android Developers: versiones Health Connect](https://developer.android.com/jetpack/androidx/releases/health-connect).
15. [Android Developers: versiones WorkManager](https://developer.android.com/jetpack/androidx/releases/work).
16. [Android Developers: programación periódica WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
17. [Android Developers: migraciones Room](https://developer.android.com/training/data-storage/room/migrating-db-versions).
18. [Android Developers: Auto Backup](https://developer.android.com/identity/data/autobackup).
19. [Android Developers: Android Keystore](https://developer.android.com/privacy-and-security/keystore).
20. [Open Food Facts: documentación oficial de API](https://openfoodfacts.github.io/openfoodfacts-server/api/).
21. [Comité Olímpico Internacional: consenso REDs 2023](https://bjsm.bmj.com/content/57/17/1073).
22. [Hall y colaboradores: dinámica de balance energético y peso corporal](https://pubmed.ncbi.nlm.nih.gov/21872751/).

---

**Conclusión de arquitectura:** el producto no es una suma de pantallas de nutrición y Garmin. Es un controlador prudente de evidencia personal cuyo resultado principal consiste en determinar si el plan actual debe mantenerse, observarse o corregirse, y si la calidad de datos autoriza realmente esa decisión.
