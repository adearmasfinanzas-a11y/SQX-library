---
paths: SQX_Library/**
cargar_en: Paso 6 del protocolo (Diseño)
naturaleza: ESTABLE — marco conceptual de diseño, no una estructura que StrategyQuant valide de forma nativa
---

# Arquitectura de 5 capas

## Qué es esto, y qué no es

**Esto es un marco de razonamiento para diseñar con coherencia, no una estructura que el Builder de StrategyQuant reconozca o exija.** Verificado directamente sobre archivos reales de la instalación (`Build-Task1.xml`, `wizard.xml`): el motor genético de SQX construye árboles lógicos combinando bloques (Indicadores, Price, Operadores, Barras/Tiempo) mediante un conteo plano de condiciones (`RulesComplexity minConditions/maxConditions`) — no existe ningún concepto nativo de "Contexto", "Oportunidad" o "Evidencia" dentro del programa. Ni AlgoWizard ni el Full Builder distinguen capas.

Por tanto, las 5 capas sirven para que **nosotros** (Ariel + Claude) razonemos con disciplina qué bloques habilitar y por qué durante el Paso 6 — no son un requisito que deba verse reflejado como una etiqueta o restricción literal dentro del `.cfx`/`.sqx`. Intentar forzar una traducción 1:1 (ej. "este bloque de Building Blocks ES la Capa 1") no tiene sentido estructural y puede llevar a restricciones artificiales que reduzcan el espacio de búsqueda sin necesidad real.

**Importante — esto no es la causa de fallos de generación:** si un proyecto genera 0 estrategias aceptadas, la causa más probable es un error de configuración concreto (bloques sobrantes activos, rangos de Shift/Period mal calibrados, criterios de aceptación contradictorios — ver `_registro-cambios.md` del 2026-07-05 para un caso real diagnosticado). No es un síntoma de que "las 5 capas no funcionan en SQX" — son dos problemas independientes y no deben confundirse.

## Las familias arquitectónicas ya viven en `matriz-activo-hipotesis.md`

Cada hipótesis registrada en la matriz (Reversión a la media, Breakout, Momentum, Liquidez de sesión, etc.) **es** la familia arquitectónica de esa plantilla. Las 5 capas son el checklist que usamos para diseñar esa familia con coherencia interna — no una capa adicional de nomenclatura que haya que mantener por separado.

## El checklist de diseño (guía, no obligación estructural)

| Capa | Pregunta guía | Cómo se usa en el diseño |
|---|---|---|
| 1 — Contexto | ¿Existe el contexto adecuado para que la hipótesis tenga sentido? | Si aplica, se traduce a un bloque/condición adicional dentro del mismo conjunto plano de `EntryRules` del Builder — no a una sección separada del archivo. Puede marcarse "neutral" si la hipótesis no lo requiere, con justificación escrita. |
| 2 — Oportunidad | ¿Existe una situación compatible con la hipótesis? | Detecta el fenómeno (exceso, ruptura, impulso, compresión, expansión, desequilibrio, agotamiento). Guía qué indicador habilitar en Building Blocks. |
| 3 — Evidencia | ¿Hay evidencia suficiente para ejecutar? | Confirma el inicio del comportamiento. Se busca que sea de la misma familia lógica que la Capa 2, salvo justificación — pero si el genético combina esto en una sola condición plana junto con la Capa 2, es un resultado válido, no un incumplimiento. |
| 4 — Ejecución | ¿Cómo se ejecuta? | Guía qué Order Types habilitar (Market/Stop/Limit) y máximo de entradas. |
| 5 — Gestión | ¿Cómo se gestiona la posición? | Guía la configuración de SL/PT/Trailing/salida temporal en la pantalla correspondiente del Builder. |

**Regla de coherencia que sí se mantiene:** ningún indicador se habilita en Building Blocks sin que exista una línea en la ficha que responda "¿qué mide esto y para qué propósito de diseño (capa) se eligió?". Esto sigue siendo obligatorio — lo que cambia es que ya no se exige que el Builder represente esa separación estructuralmente, solo que nuestra documentación (la ficha) explique la elección.

**Prioridad si hay conflicto:** ante cualquier tensión entre "mantener la separación de capas visible" y "no reducir artificialmente el espacio de búsqueda genético", gana la amplitud del espacio de búsqueda. Las capas son una herramienta de diseño al servicio de la robustez estadística — no un fin en sí mismas.

**Evidencia real que refuerza esto:** ver `@.claude/rules/patrones-validados-por-retest.md` — estrategias EURUSD reales que pasaron Retest usaron una sola condición de entrada, no las 5 capas apiladas simultáneamente. No es excusa para no pensar en capas al diseñar, pero sí es una señal real de que forzar varias condiciones simultáneas en el Build reduce la probabilidad de encontrar algo viable.
