# Codex task — Phase 3.5 Shadow validation

Lee este archivo completo y luego lee **completo**:

`continuacion_prompt_fase_3_5.txt`

Ese archivo contiene las secciones 4–42 del contrato de implementación y forma parte de este encargo. No lo resumas ni lo sustituyas por inferencias.

## 0. Fuente normativa

Fuente normativa exclusiva:

`Medidor_de_rendimiento_Especificacion_v1.1.md`

Lee especialmente FR-029, FR-034, FR-035, FR-036, FR-043, SH-01–SH-12 y Fase 3.5. Si hay contradicción entre el encargo y la especificación, prevalece la especificación.

No modifiques la especificación.

## 1. Precondición Git

Haz `git fetch origin main --prune` y toma como base **el `origin/main` actual que contiene este archivo**.

No uses como requisito un SHA histórico anterior, porque los archivos de encargo se transportan temporalmente dentro de `main`.

Debes:

1. registrar el SHA exacto de `origin/main` al comenzar;
2. confirmar árbol limpio;
3. crear `phase-3-5-shadow-validation` directamente desde ese SHA;
4. usar ese SHA como parent esperado del trabajo de Fase 3.5.

Los archivos:

- `CODEX_TASK.md`
- `continuacion_prompt_fase_3_5.txt`

son **documentación temporal de transporte del encargo**. No cuentan como implementación funcional de la fase y no deben ser modificados por Codex.

## 2. Estado funcional de partida

La Fase 3c ya está certificada y debe conservarse. Confirma que el `main` actual contiene, como mínimo:

- `PlanEvaluator`;
- `PlanEvaluation`;
- `DecisionStateMemory`;
- `lastProcessedDay`;
- `firstQualifiedDay`;
- `lastQualifiedDay`;
- `EvaluationMode`;
- SHADOW no operativo;
- cooldown de 14 días;
- entrada ±200 g/sem;
- salida ±100 g/sem;
- Room v4;
- `plan_evaluations`;
- `decision_state_memory`;
- `MIGRATION_3_4`;
- banner `EVALUACIÓN EN VALIDACIÓN`;
- BASE_ONLY.

Si alguno de estos elementos funcionales falta, detente con `PHASE_3_5_BLOCKED`.

## 3. Regla crítica de cierre

Fase 3.5 tiene dos partes distintas:

### A. Tooling de validación
Se implementa ahora con código y tests.

### B. Validación personal real
Requiere datos personales acumulados en el tiempo.

Un test verde o fixtures sintéticos **NO** significan que la validación personal haya sido aprobada.

El resultado técnico máximo de este encargo es:

`PHASE_3_5_TOOLING_READY`

No uses:

- `PHASE_3_5_COMPLETE`
- `ADVISORY_READY`
- `ADVISORY_ENABLED`

salvo que un encargo futuro lo autorice expresamente después de la validación real.

## 4. Contrato restante

Ahora lee y ejecuta íntegramente, en orden, todas las secciones 4–42 de:

`continuacion_prompt_fase_3_5.txt`

No omitas secciones.

No avances a fases posteriores.

No hagas push manual.

Cuando termines, reporta exactamente uno de estos estados:

- `PHASE_3_5_TOOLING_READY`
- `PHASE_3_5_BLOCKED`
