---
paths: SQX_Library/**
cargar_en: Al configurar cualquier condición de filtrado/aceptación en Build, Retest o Filtering — de cualquier plantilla, no solo la actual
naturaleza: ESTABLE — mecanismo nativo del motor, no específico de ninguna plantilla
---

# Mecanismo nativo de condiciones (columna + muestra + comparador) — Filtering y afines

Investigado el 2026-07-12 a raíz de una pregunta del usuario que expuso una sobre-ingeniería propia (ver `_registro-cambios.md`, entrada 2026-07-12: se había propuesto escribir un `CustomAnalysis` en Java para filtrar por Profit Factor Out-of-Sample, cuando el motor nativo ya lo soporta desde la interfaz). Documentado con evidencia real: se decompilaron las clases Java reales del motor con `javap` (incluido en la instalación, `j64/bin/javap.exe`) — no se infiere ni se supone nada de esta ficha sin haberlo verificado contra el bytecode real o un archivo XML real de la instalación.

## Idea central

**Build (Rankings), Retest (Rankings) y Filtering comparten el mismo framework de condiciones** (`com.strategyquant.tradinglib.conditions.*`). Aprender a armar una condición en cualquiera de los tres sirve para los tres. Una condición (`<Column-Value>` en XML, `Condition` en Java) siempre tiene:

- **Lado izquierdo:** normalmente una columna de estadística (`column`), pero también puede ser un valor numérico fijo, una propiedad del databank, o una propiedad del proyecto (`ConditionValueTypes`: `numeric`, `column`, `databankProperty`, `projectProperty`).
- **Comparador:** string libre (no un enum cerrado) — `">="`, `">"`, `"<"`, `"<="`, `"="`, etc.
- **Lado derecho:** mismo abanico de tipos que el izquierdo (normalmente un valor numérico fijo).

## Catálogo completo de columnas disponibles (`StatsKey`, decompilado)

Cualquiera de estas ~70 columnas puede usarse en una condición. Las más relevantes para esta biblioteca, en negrita:

**Rentabilidad:** NetProfit, GrossProfit, GrossLoss, **ProfitFactor**, AvgProfitPerDay/Month/Year, AvgPctProfitPerYear, **CAGR**, NetProfitInPips.

**Operaciones:** **NumberOfTrades**, NumberOfLosses, NumberOfProfits, NumberOfCanceled, WinLossRatio, AvgTrade, AvgAbsTrade, AvgWin, AvgLoss, PayoutRatio, AvgTradesPerDay/Month/Year, **WinningPct**.

**Rachas:** MaxConsecWins, MaxConsecLosses, AvgConsecWins, AvgConsecLosses, MaxProfit, MaxLoss.

**Riesgo/Drawdown:** **Drawdown**, DrawdownPct, DrawdownPips, **ReturnDDRatio**, ReturnOpenDDRatio, AnnualPctReturnDDRatio (AAR/DD), **CalmarRatio**.

**Estadísticos:** StandardDev, ZScore, ZProbability, Expectancy, RExpectancy, RExpectancyScore, Symmetry, Stability, DegreesOfFreedom, AHPR, **SharpeRatio**, SQN, SQNScore.

**Duración/estancamiento:** MaxNewHighDuration(From/To/Period/Pct), Stagnation(From/To/Period/PeriodPct), Exposure, ExposurePosition.

**Tiempo:** TotalTradingDays/Months/Years, ProfitableMonths.

**Costos:** Commission, SlippageInMoney, CommSwapInMoney.

**Otros:** OptimizationParameters(Array) — específico de tareas de Optimize.

## Tipos de muestra (`SampleTypes`, decompilado — valores numéricos reales)

Este es el hallazgo que corrigió la sobre-ingeniería original. El atributo XML `sampleType="N"` (o el equivalente en la UI, un selector de "sample") acepta:

| Valor | Constante | Significado |
|---|---|---|
| 127 | `FullSample` | Todos los datos (lo que veíamos como "Main data" en el Build) |
| 10 | `InSample` | Solo el período de ajuste (IS) |
| 11 | `InSampleTraining` | Sub-tipo de IS (training) |
| **20** | **`OutOfSample`** | **Solo el período fuera de muestra (OOS) — esto es lo que necesitábamos, nativo, sin código** |
| 21-30 | `OutOfSample1`...`OutOfSample10` | Corridas individuales de Walk-Forward (cada ciclo del Walk-Forward Matrix/Optimization) |
| 40 | `InSampleValidation` | Validación dentro de IS |
| 41-50 | `InSampleValidation1`...`InSampleValidation10` | Corridas individuales de validación IS |
| 66 | `InSampleValidationEvery` | Agregado de todas las validaciones IS |
| 77 | `OutOfSampleEvery` | Agregado de todas las corridas OOS del Walk-Forward |
| 99 | `NoTrade` | Sin operaciones |

**Confirmado en uso real:** `sampleType="20"` ya aparece en `internal/plugins/TaskRetest/task.xml` (condiciones de Walk-Forward Matrix/Optimization) y en nuestro propio `Build-Task1.xml`. No es un valor teórico — es el mismo que usa el motor internamente en templates reales de la instalación.

## Otros enums de apoyo (decompilados)

- **`Directions`:** `Both=0`, `Long=1`, `Short=-1` — filtra por dirección de operación si hace falta.
- **`PlTypes`:** `Money=10`, `Percent=20`, `Pips/Ticks=30`, `OpenMoney=40`, `OpenPercent=50` — en qué unidad se expresa el Profit/Loss de la columna.
- **`ConditionsTypes`:** `ThrowAway=0`, `Keep=1` — (nota: en `FilteringTask` real, los valores 2 y 3 de `conditionsType` tienen un significado especial adicional: reutilizan el motivo de rechazo que la propia estrategia ya trae desde el Build en vez de evaluar la lista de `<Conditions>` — caso avanzado, no necesario para nuestro uso estándar).

## Tarea `Filtering` en detalle (`FilteringTask.class`, decompilado)

- **`actionType`** (decompilado de `actionTypeToText()`): `0 = Deleted` (elimina las que NO pasan la condición), `1 = Copied` (copia las que SÍ pasan a la base destino, sin tocar la fuente), `2 = Moved` (mueve las que SÍ pasan a la base destino, las saca de la fuente).
- **`maxStrategies`:** tope opcional de cuántas estrategias puede aceptar el filtro, independiente de las condiciones.
- **`databankSource` / `databankTarget`:** de dónde lee y a dónde escribe — mismo databank si se usa `actionType=0` (Deleted, filtra in-place).
- Puede tener **múltiples condiciones simultáneas** (`ArrayList<Condition>`), cada una con su propio `use="true/false"` — se pueden desactivar sin borrar, mismo patrón que ya conocemos de Building Blocks.

## Qué significa esto para el pipeline de esta plantilla (y cualquier futura)

El filtro de "Profit Factor en Out-of-Sample ≥ umbral" (lo que antes íbamos a resolver con `CustomAnalysis` en Java) se arma así, **sin código**, en una tarea `Filtering`:

- Databank origen: `Results` (o el que corresponda en esa etapa del pipeline).
- Condición: columna `ProfitFactor`, tipo de muestra `Out of Sample`, comparador `≥`, valor `1.0`.
- `actionType`: `Deleted` (filtra in-place, descarta las que no cumplen) si no hace falta conservar las descartadas en otro lado; `Moved` si se quiere separar limpio/descartado en dos databanks distintos (más trazable, preferible si el volumen de datos no es un problema).

`CustomAnalysis` (Java) queda reservado genuinamente para lo que este framework de condiciones no pueda expresar — una fórmula propia que combine varias columnas con lógica no lineal, por ejemplo. Antes de escribir un `CustomAnalysis` nuevo en el futuro, **revisar primero si el catálogo de columnas de esta ficha ya lo resuelve**.

## Lo que queda sin verificar (honestidad, no se afirma sin evidencia)

Los atributos `market`, `subresult`, `confidenceLevel`, `pctRatio` que aparecen en los `<Column-Value>` reales de `Build-Task1.xml`/`TaskRetest/task.xml` **no pertenecen a la clase `ColumnConditionValue`** decompilada (que solo tiene `resultType`, `direction`, `plType`, `sampleType`, `columnType`, `pctRatio`, `column`) — es decir, `pctRatio` sí está confirmado, pero `market`, `subresult` y `confidenceLevel` deben pertenecer a una clase más específica (probablemente ligada a CrossCheck/Monte Carlo, dado que vimos `confidenceLevel="80"` en condiciones de `MonteCarloRetest`). No se investigó más a fondo por no ser necesario para el uso estándar de esta plantilla — pendiente si en el futuro se necesita una condición de confianza estadística (Monte Carlo) específica.
