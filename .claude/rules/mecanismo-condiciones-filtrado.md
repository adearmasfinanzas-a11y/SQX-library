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
- **`ConditionsTypes`:** clase decompilada define `ThrowAway=0`, `Keep=1` — **pero el nombre del enum no coincide con el comportamiento real observado, corregido con evidencia (ver más abajo): `ConditionsType=0` es el que efectivamente selecciona "aplica a las que cumplen las condiciones" (el filtro normal e intuitivo), no `1` como el nombre `Keep` sugeriría a primera vista.** No confiar en el nombre del campo Java sin confirmar contra un XML real exportado — exactamente el tipo de error que esta ficha busca evitar. (Los valores 2 y 3, vistos en el bytecode de `FilteringTask`, tienen un significado especial adicional: reutilizan el motivo de rechazo que la propia estrategia ya trae desde el Build, sin evaluar la lista de `<Conditions>` — caso avanzado, no necesario para nuestro uso estándar. Corresponden a las opciones de radio "Éxito"/"Fallido" de la interfaz.)

## Tarea `Filtering` en detalle (`FilteringTask.class`, decompilado)

- **`actionType`** (decompilado de `actionTypeToText()`): `0 = Deleted` (elimina las que NO pasan la condición), `1 = Copied` (copia las que SÍ pasan a la base destino, sin tocar la fuente), `2 = Moved` (mueve las que SÍ pasan a la base destino, las saca de la fuente). En la interfaz simplificada (`simpleSettings`), este campo se ve como una elección entre "copiar de una base de datos a otra" o "pasar de una base de datos a otra" (Deleted no aparece como opción visible ahí).
- **`maxStrategies`:** tope **opcional** de cantidad, **totalmente independiente de las `Conditions`** — en la interfaz aparece como "¿aplica a todas o a un número específico?", ANTES de la sección de Condiciones. **Confirmado en la práctica (2026-07-12, corrección del usuario):** esto no es el filtro — es un techo duro de cantidad que se aplica ADEMÁS del filtro de condiciones (ej. "muevan como máximo las primeras N que califiquen"). **Para dejar que el filtro de Condiciones sea el único criterio, dejar esta opción en "todas"** — si se deja un número específico, aunque haya cientos de candidatas que cumplan la condición, solo pasarían las primeras N.
- **`databankSource` / `databankTarget`:** de dónde lee y a dónde escribe — mismo databank si se usa `actionType=0` (Deleted, filtra in-place). **Confirmado en la práctica (2026-07-12):** el campo Target **no admite escribir un nombre nuevo libremente** — es un desplegable que solo lista databanks ya existentes en el proyecto. Si se necesita una base nueva (ej. para separar "pasaron el filtro" de "Results"), hay que crearla antes desde la herramienta de gestión de Databanks del proyecto, y recién entonces aparece disponible para elegir como Target.
- Puede tener **múltiples condiciones simultáneas** (`ArrayList<Condition>`), cada una con su propio `use="true/false"` — se pueden desactivar sin borrar, mismo patrón que ya conocemos de Building Blocks.

## Qué significa esto para el pipeline de esta plantilla (y cualquier futura)

El filtro de "Profit Factor en Out-of-Sample ≥ umbral" (lo que antes íbamos a resolver con `CustomAnalysis` en Java) se arma así, **sin código**, en una tarea `Filtering`:

- Databank origen: `Results` (o el que corresponda en esa etapa del pipeline).
- Condición: columna `ProfitFactor`, tipo de muestra `Out of Sample`, comparador `≥`, valor `1.0`.
- `actionType`: `Deleted` (filtra in-place, descarta las que no cumplen) si no hace falta conservar las descartadas en otro lado; `Moved` si se quiere separar limpio/descartado en dos databanks distintos (más trazable, preferible si el volumen de datos no es un problema).

`CustomAnalysis` (Java) queda reservado genuinamente para lo que este framework de condiciones no pueda expresar — una fórmula propia que combine varias columnas con lógica no lineal, por ejemplo. Antes de escribir un `CustomAnalysis` nuevo en el futuro, **revisar primero si el catálogo de columnas de esta ficha ya lo resuelve**.

## Ejemplo real confirmado (condición armada y exportada, 2026-07-12)

Se armó en la interfaz (tarea `Filter Strategies`, primer filtro post-Build de `EURUSD-REVRANGE-H1-001`) la condición real "Profit Factor OOS ≥1.2 y NumberOfTrades OOS ≥30, ambas direcciones, en dinero", y se exportó a XML con el botón de guardado de configuración (`D:\Ariel De Armas\Templates SQX\filters.xml`). Resultado real:

```xml
<Conditions>
  <Condition use="true">
    <Left-Side valueType="column">
      <Column-Value column="ProfitFactor" columnType="0" format="Decimal2" resultType="main"
        direction="0" sampleType="20" plType="10" confidenceLevel="50" market="1"
        subresult="30" pctRatio="0" class="ProfitFactor"/>
    </Left-Side>
    <Comparator value="&gt;="/>
    <Right-Side valueType="numeric"><Numeric-Value value="1.2"/></Right-Side>
  </Condition>
  <Condition use="true">
    <Left-Side valueType="column">
      <Column-Value column="NumberOfTrades" columnType="0" format="Integer" resultType="main"
        direction="0" sampleType="20" plType="10" confidenceLevel="50" market="1"
        subresult="30" pctRatio="0" class="NumberOfTrades"/>
    </Left-Side>
    <Comparator value="&gt;="/>
    <Right-Side valueType="numeric"><Numeric-Value value="30"/></Right-Side>
  </Condition>
</Conditions>
```

Esto **confirma con evidencia real** (no solo decompilación) los atributos que habían quedado pendientes de verificar: `confidenceLevel="50"`, `market="1"` y `subresult="30"` aparecen como **valores fijos por defecto** en condiciones sobre `resultType="main"` (resultado principal, ni Monte Carlo ni Walk-Forward) — no varían según la columna elegida ni según el tipo de muestra (`sampleType`). Es razonable asumir que solo cambian cuando `resultType` apunta a algo específico de Monte Carlo (`confidenceLevel` ahí sí tendría sentido variable, ej. 80/95) o de Retest en mercados adicionales (`market` variable). No se investigó ese caso porque no hace falta para el uso estándar de esta plantilla — pendiente si en el futuro se arma una condición sobre resultados de Monte Carlo/mercado adicional específicamente.

## Preset completo de la tarea confirmado (guardado por el usuario, 2026-07-12) — corrige un error propio

El usuario guardó el preset **completo** de la tarea (no solo las condiciones) desde el botón "Guardar configuración" de la pestaña de ejecución, como `.cfx` (mismo formato ZIP que `project.cfx`) en `D:\Ariel De Armas\Templates SQX\Filtro_OOS_PF1.2_Trades30.cfx`. Al abrirlo (`config.xml` interno) se confirmaron con evidencia real varios valores que antes solo se habían decompilado o supuesto:

```xml
<Task name="Filter strategies" type="Filtering" taskXMLFile="Filtering-Task1.xml" version="143.2708">
  <Settings>
    <Filtering>
      <ActionType>2</ActionType>
      <MaxStrategies>0</MaxStrategies>
      <ConditionsType>0</ConditionsType>
      <Conditions>...</Conditions>
    </Filtering>
    <Databanks>
      <Databank label="Source databank" name="Source" value="Results"/>
      <Databank label="Target databank" name="Target" value="OOS_Filtrado"/>
    </Databanks>
    ...
  </Settings>
</Task>
```

Confirmado:
- **Nombre real de tarea en el archivo del proyecto:** `Filtering-TaskN.xml` — mismo patrón de nombrado que `Build-TaskN.xml`/`Retest-TaskN.xml` ya conocido.
- **`ActionType=2` = Moved** — confirma la decompilación de `actionTypeToText()`, sin discrepancia.
- **`MaxStrategies=0` = "todas" (sin tope)** — confirma que el valor 0 es el que representa "sin límite", no un campo vacío/nulo.
- **`ConditionsType=0` = "aplica a las estrategias que cumplan las condiciones"** (el filtro normal, el que usamos) — **esto corrige la ficha**: antes se había asumido, solo por el nombre del campo Java (`Keep=1` en la clase `ConditionsTypes`), que "Keep" (1) sería la opción intuitiva de "aplicar a las que cumplen". La evidencia real del preset guardado prueba lo contrario: es `0` el que hace eso. No se volvió a verificar cuál valor corresponde a la opción inversa ("ignora las que cumplan, aplica a todas las demás") — sería lo siguiente a confirmar si hace falta esa variante en el futuro.
- El archivo también trae un bloque `<Rankings>` con `MaxStrategies=1000`, `Ranking type="NetProfit"`, `StopCondition databank-full passedStrategies=1000` — esto **no es configuración propia de la tarea Filtering**, es residuo/herencia del contexto del proyecto al guardar el preset (coincide con los valores del Build). No confundir esto con ajustes reales de Filtering.

## Cómo se ejecuta una tarea individual dentro de un proyecto multi-tarea (confirmado en la práctica, 2026-07-12)

Click derecho sobre cualquier tarea en el panel de progreso del proyecto (`Proyectos a medida / <nombre> / Progreso`) abre un menú con, entre otras, estas dos opciones — **distintas y confirmadas por uso real**:

- **"Ejecute el proyecto desde aquí"** — corre la secuencia completa del pipeline **a partir de** esa tarea (útil para reanudar un pipeline largo desde donde quedó, sin repetir lo anterior).
- **"Ejecutar sólo esta tarea"** — corre **únicamente** esa tarea, sin tocar el resto de la secuencia. Esta es la que se usó para correr `Filter Strategies` sin re-disparar el Build (que ya había terminado y hubiera tardado ~2.5 días de nuevo).

Otras opciones del mismo menú, útiles para escalar a futuras plantillas sin reconfigurar a mano: **"Copiar la configuración en una o varias tareas"** y **"Aplicación masiva de la configuración de esta tarea"** (aplican la config de una tarea ya armada a otras) — más rápido que reexportar/reimportar el `.cfx` preset para reutilizar dentro del mismo proyecto o entre tareas del mismo tipo.

## Resultado real del primer filtro OOS aplicado (`EURUSD-REVRANGE-H1-001`, 2026-07-12)

Corrida sobre las 1000 estrategias en bruto del Build (condiciones: ProfitFactor OOS≥1.2 AND NumberOfTrades OOS≥30, ambas direcciones, dinero):

- **535 de 1000 (53.5%) pasaron** → movidas a `OOS_Filtrado`.
- **465 de 1000 (46.5%) no pasaron** → quedaron en `Results`.

Tasa de aprobación bastante más alta de lo que sugería la evidencia manual previa de 12 candidatas (donde 8/12, 67%, pasaban con el piso más laxo de PF OOS≥1.0) — con 1000 candidatas y umbrales más estrictos (1.2 + mínimo de trades), el resultado real fue 53.5%. Buen recordatorio de no proyectar conclusiones firmes desde una muestra de 12 al comportamiento real sobre 1000.
