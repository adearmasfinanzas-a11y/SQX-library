---
paths: SQX_Library/**
cargar_en: Paso 6 (Diseño, sección 18 plan de Retest) y cuando una plantilla logre su primera convergencia real (al menos 1 estrategia aceptada) — recién ahí se aplica
naturaleza: REQUISITO PENDIENTE DE VALIDAR — no aplicado todavía en ninguna plantilla, se activa la primera vez que un Build converja
---

# Pipeline multi-tarea y diseño de IS/OOS — requisito para cuando una plantilla converja

Esta regla existe porque el usuario pidió (2026-07-07) que **cada proyecto tenga todo preconfigurado con sentido común hacia un objetivo** — no solo el Build, sino el pipeline completo de validación encadenado automáticamente. Se investigó y verificó contra un proyecto de ejemplo real (`GBPJPY BREAKOUT H1 - Dukascopy`) antes de proponer nada.

## 0. Metodología fija para incorporar cada tarea nueva al pipeline (establecida 2026-07-12)

El incidente del filtro OOS (se proponía escribir `CustomAnalysis` en Java sin verificar antes si la tarea nativa `Filtering` ya lo resolvía — corregido, ver `mecanismo-condiciones-filtrado.md`) estableció el método de trabajo fijo para **cada** tarea que se incorpore al pipeline de cualquier plantilla, no solo esta:

1. **Investigar a fondo la tarea nativa antes de proponer configuración.** No asumir que hace falta código propio — SQ es una plataforma completa; verificar primero qué cubre nativamente (decompilando las clases reales del motor con `javap`, incluido en `j64/bin/` de la instalación, cruzando contra `task.xml` default del plugin en `internal/plugins/`, y contra ejemplos reales de proyectos de la instalación) antes de escribir un `CustomAnalysis`/`CallExternalScript` o cualquier otro mecanismo de código propio.
2. **Documentar lo aprendido como regla reutilizable**, no como nota de esta plantilla puntual — un archivo `.claude/rules/mecanismo-*.md` por tipo de tarea (o ampliando uno existente si aplica), enlazado desde `CLAUDE.md`. El objetivo es que cualquier plantilla futura (otra hipótesis, otro activo) pueda usar esa tarea sin tener que re-investigar desde cero.
3. **Proponer la configuración concreta según el contexto** — objetivo de la plantilla actual, evidencia real ya reunida (ej. 8/12 candidatas con PF OOS≥1 en corridas previas) — nunca un valor copiado sin justificar.
4. **La propuesta siempre pasa por aprobación del usuario antes de implementarse.** Se discute, se ajusta si hace falta, y solo entonces se aplica — mismo principio ya vigente en el resto del protocolo (`CLAUDE.md`: "Nunca se implementa un indicador nuevo sin aprobación explícita").

Pedido explícito del usuario: que este mismo proceso (investigar → documentar → proponer → aprobar → implementar) se repita para cada tarea nueva que se vaya agregando, para ir construyendo un catálogo propio de "qué se puede lograr sin programar nada" en SQ.

## 1. Diseño de IS/OOS con criterio de régimen, no partición mecánica

**No usar un 70/30 fijo sin criterio.** La práctica seria es walk-forward: ciclos sucesivos de ajuste+validación que avanzan en el tiempo, no una partición estática única. Métrica de referencia: **Walk Forward Efficiency (WFE)** = rendimiento anualizado OOS / rendimiento anualizado IS — **WFE≥0.5 es el mínimo aceptable, ≥0.7 es excelente** ([Kiploks](https://kiploks.com/research/what-is-walk-forward-analysis-complete-guide-for-algo-traders), [QuantInsti](https://blog.quantinsti.com/walk-forward-optimization-introduction/)).

**EURUSD (y por extensión otros pares FX mayores) tuvo regímenes genuinamente distintos, no una serie homogénea:**
- 2008: crisis financiera global (volatilidad extrema, no representativa de operativa normal).
- 2010-2012: crisis de deuda soberana europea.
- 2016: incertidumbre por el Brexit.
- 2020: "volatilidad ausente" durante COVID (paradójicamente, rango raro).
- 2022: el Fed subió tasas 450 pb en un año, EUR/USD tocó paridad por primera vez en 20 años — quiebre de régimen real, no ruido.

Fuentes: [BBVA Research — Equilibrium of the EUR-USD exchange rate](https://www.bbvaresearch.com/wp-content/uploads/2025/03/Equilibrium-of-the-EUR-USD-exchange-rate-A-long-term-perspective.pdf), [OANDA — EUR/USD parity historical trends](https://www.oanda.com/us-en/trade-tap-blog/analysis/fundamental/eur-usd-bearish-move-below-parity-on-hold/).

**Al diseñar el rango de IS de una plantilla, decidir explícitamente (no por default):** ¿se incluyen los picos de crisis extrema (2008, 2020) o se excluyen por no representativos? ¿se trata 2022 como parte del IS o como una ventana de validación de régimen aparte? Esta decisión se documenta con su motivo en la ficha, igual que ya se hace con otras decisiones de diseño.

## 2. Mecanismo nativo de StrategyQuant para esto: Walk-Forward Matrix / Walk-Forward Optimization

Confirmado con documentación oficial: corre múltiples ciclos de optimización con distintas combinaciones de período de reoptimización y % Out-of-Sample, devuelve un gráfico 3D de robustez (cambios graduales = robusto; cambios abruptos = sobreajustado). **Esto no se configura en el Build inicial — es una tarea de Retest/CrossCheck posterior** (`CrossCheckWalkForwardMatrix`, `CrossCheckWalkForwardOptimization`, ya vistos como plugins disponibles en la instalación).

## 3. Estructura real del pipeline multi-tarea (verificada contra `GBPJPY BREAKOUT H1 - Dukascopy`)

Un proyecto serio no es solo un Build — es una cadena de tareas en `config.xml`:

```
Build → Retest OOS (período sin solapar con el IS) → Retest en mercado correlacionado
      → Retest de sensibilidad a slippage → Retest de permutación de parámetros (MC Param)
      → ClearDatabanks → GoToTask (vuelve al Build)
```

**Cada tarea es un archivo XML propio, referenciado por nombre desde `config.xml`:**
- `Build-TaskN.xml` — igual estructura que ya editamos con confianza (RulesComplexity, BuildingBlocks, SL/PT, genética).
- `Retest-TaskN.xml` — misma estructura base (Setup de datos, RiskMoneyManagement, Rankings/criterios), pero con su propio rango de fechas/símbolo. Verificado en el ejemplo real: el Build usaba 2009-2022, el primer Retest ("OOS 1") usaba **2022.08-2024.07** — rango que **no se solapa**, walk-forward real. Los Retest titulados con otro símbolo (ej. "EURJPY") solo cambian el símbolo del Setup, para probar si la lógica se sostiene en un mercado correlacionado.
- `ClearDatabanks-TaskN.xml` — simple: `<ClearDatabanks><Databank name="Results" /></ClearDatabanks>`.
- `GoToTask-TaskN.xml` — simple: `<GoToTask task="Build strategies"><Task /><Conditions /></GoToTask>` (referencia por nombre a la tarea de Build, cierra el loop; `<Conditions />` vacío = incondicional, se podría condicionar).

## 3b. Catálogo completo de tipos de tarea (investigado 2026-07-08, verificado contra los `task.xml` reales de cada plugin)

Existen **21 tipos de tarea reales** disponibles para un proyecto personalizado (verificado en `internal/plugins/Task*`, cada uno con su `task.xml` de configuración default):

| Tarea | Función |
|---|---|
| **Build** | Genera estrategias por evolución genética (la que ya usamos a fondo). |
| **Retest** | Vuelve a testear estrategias existentes con otra configuración de datos/época. |
| **Optimize** | Optimiza parámetros de estrategias ya existentes (automático/manual/fuerza bruta), con su propia sección de Rankings y Walk-Forward. |
| **Filtering** | Filtra estrategias de un databank origen a un databank destino según condiciones, **sin volver a testear** — rápido, ideal para aplicar un umbral post-hoc. |
| **CustomAnalysis** | Corre análisis a medida (por estrategia o sobre todo el databank), puede eliminar estrategias que fallan el análisis — la vía de escape para lógica propia que las condiciones nativas no cubren (ej. nuestro cálculo de Profit Factor OOS). |
| **CreatePortfolio** | Arma un portafolio simple tomando estrategias de un databank, con tope `MaxStrategies`. |
| **AutomaticPortfolioBuilder** | Búsqueda genética/fuerza bruta de la **mejor combinación** de portafolio — condiciones de aceptación a nivel portafolio, correlación máxima configurable, money management propio del portafolio. Mucho más sofisticado que CreatePortfolio. |
| **ClearDatabanks** | Vacía un databank (ya usado en nuestro pipeline conocido). |
| **GoToTask** | Salta a otra tarea por nombre dentro del **mismo** proyecto — cierra loops. |
| **StopAndStart** | Para el proyecto bajo condiciones, y puede encadenar a **otro proyecto personalizado completo** — permite encadenar proyectos distintos, no solo tareas dentro de uno. |
| **SaveToFiles** | Guarda estrategias de un databank a archivos `.sqx`/`.str` en disco. |
| **LoadFromFiles** | Carga estrategias desde archivos en disco hacia un databank. |
| **DeleteFile** | Borra un archivo (limpieza de reportes viejos, etc.). |
| **UpdateData** | Actualiza/refresca los datos históricos antes de una corrida. |
| **LogDatabankStats** | Registra estadísticas de un databank en el log — checkpoint intermedio sin detener el pipeline. |
| **Notification** | Envía notificación (email) al llegar a esa tarea — avisa sin que el usuario tenga que estar revisando manualmente. |
| **CallExternalScript** | Pausa el proyecto, llama a un script/programa externo, puede esperar su resultado antes de continuar — la puerta de entrada para integrar lógica propia (ej. nuestro `sqcli + análisis` ya probado, o a futuro la retroalimentación con IA de `vision-proyectos-futuros.md`, siempre con el portón de validación ya establecido en esa regla). |
| **WaitFor** | Espera a que se produzca un archivo antes de continuar — pareja natural de CallExternalScript. |
| **ApplyMassConfig** | Aplica un cambio de configuración en masa a varias estrategias de un databank a la vez (ej. igualar money management antes de armar portafolio). |
| **NeuralNetworkTrainer** | Entrena una red neuronal — funcionalidad avanzada/experimental, fuera de alcance por ahora. |

Fuentes: [Call external script task](https://strategyquant.com/doc/strategyquant/call-external-script/), [Stop & Start task](https://strategyquant.com/doc/strategyquant/stop-and-start/), [Introduction to custom projects](https://strategyquant.com/doc/strategyquant/introduction-to-custom-projects/), más los `task.xml` reales de cada plugin (fuente primaria, no solo documentación).

## 3c. Secuencia propuesta para esta plantilla — dejando solo lo importante al final

Pedido explícito del usuario: que el proyecto corra tarea a tarea dejando "simplemente los resultados más importantes al final". Con el catálogo completo, la secuencia concreta propuesta:

```
Build
  → SaveToFiles (guarda TODO el resultado en bruto del Build — ver justificación abajo, 2026-07-12)
  → Filtering (filtro OOS nativo, sin código — descarta candidatas tipo 4.22.136, PF OOS < 1; ver sección 3d, corrección 2026-07-12)
  → Retest (OOS sin solapar / mercado correlacionado, ver sección 1-3)
  → Filtering (umbral post-Retest, más estricto)
  → CreatePortfolio o AutomaticPortfolioBuilder (recién con varias candidatas ya limpias)
  → SaveToFiles (persiste el resultado final a disco — esto es "lo importante al final")
  → LogDatabankStats (deja registro del cierre de ciclo)
  → Notification (avisa que terminó, sin que el usuario tenga que estar mirando)
  → ClearDatabanks (limpia los databanks intermedios, no el resultado final ya guardado)
  → GoToTask (vuelve a Build, opcional)
```

**Estado (2026-07-08):** catálogo investigado y secuencia propuesta, **no implementada todavía** — se arma recién cuando haya suficientes candidatas limpias para justificar el pipeline completo (más que las 4 actuales, una de las cuales ya se descartó a mano por PF OOS<1).

### Por qué el `SaveToFiles` inmediatamente después del Build es obligatorio, no opcional (2026-07-12)

El usuario preguntó, tras la primera corrida real de `EURUSD-REVRANGE-H1-001` (1000 estrategias, respaldadas manualmente porque era la primera plantilla en converger), si en plantillas futuras haría falta guardar el resultado completo del Build o alcanzaría con guardar solo lo que pasa el primer filtro.

**Respuesta con razonamiento, no por costumbre:** el Build **no es reproducible determinísticamente** — es búsqueda genética con aleatoriedad, así que correrlo dos veces con la misma configuración exacta da conjuntos de estrategias distintos, no el mismo resultado. Esto es una diferencia real de fondo con `Filtering` (que sí es determinístico: mismas condiciones + mismo databank de entrada = mismo resultado, siempre). Por eso:

- **El resultado bruto del Build es irrecuperable si se pierde** — no hay forma de "correrlo de nuevo y obtener lo mismo".
- **El resultado de `Filtering` es barato de reconstruir** — si hiciera falta, alcanza con volver a correr la misma tarea (condiciones ya documentadas en el `.cfx` del proyecto) contra el backup del Build ya guardado, en segundos.
- Como el pipeline **hace loop** (`GoToTask` vuelve al Build), cada vuelta genera un lote nuevo y distinto de estrategias en bruto — si no se guarda cada lote antes de que la vuelta siguiente limpie el databank para la próxima tanda, ese lote se pierde para siempre, no solo la vez que no se guardó, sino en cada vuelta del ciclo.

**Regla fija para toda plantilla futura:** `SaveToFiles` de la salida **completa y sin filtrar** del Build es un paso automático del pipeline, inmediatamente después del Build, en cada vuelta del loop — no una precaución manual de "primera plantilla". El nombre de carpeta/archivo de cada guardado debe distinguir la vuelta del loop (timestamp o contador), para no sobrescribir el lote de una vuelta anterior con el de la siguiente.

## 3d. Preparación en profundidad de cada bloque (investigado 2026-07-11, antes de tener materia prima suficiente)

Con la corrida en curso acercándose a las 1000 estrategias, se investigó a fondo el mecanismo real de cada bloque de la secuencia — no solo el catálogo, sino cómo configurarlo de verdad — para poder ensamblar el pipeline sin fricción en cuanto haya suficiente materia prima.

### Filtro de Profit Factor OOS — CORREGIDO (2026-07-12): no hace falta Java, es nativo

**Versión anterior de esta sección (2026-07-11) proponía un `CustomAnalysis` en Java (`FilterByOOSProfitFactor.java`) para esto. Era una sobre-complicación — se corrige acá.**

El usuario preguntó por qué haría falta escribir un `.java` si la tarea se arma desde la interfaz. Al investigarlo a fondo se encontró que **no hace falta**: se decompiló la clase real `com.strategyquant.tradinglib.SampleTypes` (con `javap`, incluido en la propia instalación en `j64/bin/`) y se confirmaron sus valores numéricos reales:

```
FullSample = 127
InSample = 10
OutOfSample = 20
```

Y ese mismo `sampleType="20"` ya aparece usado en archivos XML nativos reales de la instalación (ej. `internal/plugins/TaskRetest/task.xml`, condiciones de Walk-Forward Matrix/Optimization). Esto confirma que **la tarea nativa `Filtering` (misma estructura genérica `<Conditions>`/`<Column-Value>` que usa `Build` y `Retest` en sus Rankings) puede filtrar por cualquier columna con selector de tipo de muestra — incluido "Out of Sample" — directamente desde la interfaz, sin ningún código propio.**

Conclusión: `FilterByOOSProfitFactor.java` **no se escribe**. El filtro de Profit Factor OOS se arma como una tarea `Filtering` nativa: columna `ProfitFactor`, muestra `Out of Sample`, comparador `≥`, valor `1.0` (piso duro propuesto, con evidencia manual real de 8/12 candidatas ≥1.0 en corridas previas — se podría subir a 1.1-1.2 más adelante con más volumen). `CustomAnalysis` (Java) queda reservado para si en el futuro hace falta algo que el selector de columna+muestra+comparador genuinamente no pueda expresar (ej. una fórmula propia entre varias columnas) — no es el caso de este filtro.

### Filtering — propuesta concreta (actualizada)

Con el hallazgo de arriba, un solo `Filtering` puede cubrir tanto el filtro barato inicial como el filtro de Profit Factor OOS en una sola tarea con varias condiciones simultáneas (ej. `NetProfit(main) > 0` **y** `ProfitFactor(Out of Sample) ≥ 1.0`), en vez de dos tareas separadas como se había propuesto antes.

### Retest — propuesta concreta

Ventana OOS adicional que no se solape con la ya usada en el Build (2015-2024, con OOS interno 2021.12.18-2024.12.31) — evaluar si hay datos más recientes disponibles para una ventana genuinamente nueva. Retest cruzado en GBPUSD (misma familia `FX_mayor_liquido`, ver `proceso-portafolio.md`) para chequear si la lógica se sostiene fuera del activo específico donde se descubrió.

### CreatePortfolio / AutomaticPortfolioBuilder — propuesta concreta

Basado en la configuración real verificada del proyecto de ejemplo `PortfolioMaster`: `MinStrategies=2`, `MaxStrategies≈7-8`, correlación máxima `~0.3` (tipo `ProfitLoss`), `FitnessType=NetProfitFull` o `ReturnDDRatio`, Money Management `FixedSize`/0.1 — coherente con el estándar ya fijado en `configuracion-money-management.md`.

### SaveToFiles / LogDatabankStats / Notification / ClearDatabanks / GoToTask

- **SaveToFiles:** destino propuesto dentro de `SQX_Library/EURUSD/ReversionRango/candidatas_build/` (o una subcarpeta de portafolio final), `SaveInSqxFormat=true`.
- **LogDatabankStats:** sobre el databank final ya filtrado, como checkpoint de cierre de ciclo.
- **Notification:** requiere que el usuario provea una dirección de email — pendiente de pedir cuando se arme de verdad.
- **ClearDatabanks:** limpia solo los databanks intermedios (`Results` del Build), nunca lo ya persistido por `SaveToFiles`.
- **GoToTask:** vuelve a la tarea de Build, sin condición (o condicionado a "menos de N estrategias de portafolio acumuladas todavía", a definir).

**Estado:** todo lo anterior es diseño/propuesta lista para aplicar, no implementado. Se activa en cuanto la corrida actual (acercándose a 1000 en el databank) dé suficiente volumen de candidatas limpias.

### 3e. Plan operativo acordado con el usuario para el momento de ensamblar (2026-07-11)

1. Cuando termine la corrida actual, el usuario guarda las 1000 estrategias a salvo (fuera del databank volátil) — así no se pierden ni hace falta correr el Build de nuevo si algo sale mal en las tareas nuevas.
2. Se agregan las tareas nuevas (`Filtering`, `CustomAnalysis`, `Retest`, `CreatePortfolio`/`AutomaticPortfolioBuilder`, `SaveToFiles`, `LogDatabankStats`, `Notification`, `ClearDatabanks`, `GoToTask`) al `config.xml` del proyecto, cada una como un `<Task>` nuevo en la secuencia.
3. **Mecanismo confirmado para no reprocesar el Build mientras se prueban las tareas nuevas:** cada `<Task>` en `config.xml` ya trae un atributo `active="true"/"false"` (verificado en nuestro propio proyecto y en el ejemplo real de GBPJPY, que lo usa así). Se pone `active="false"` en la tarea de Build — sin borrarla — mientras se desarrolla y prueba el resto del pipeline con las 1000 ya generadas. No hace falta ningún mecanismo nuevo, ya existe.
4. Una vez que el resto del proceso funcione bien de punta a punta, se reactiva el Build (`active="true"`) para que el ciclo completo corra solo.

### 3f. Ubicación final del proyecto terminado (acordado con el usuario, 2026-07-12)

Cuando la plantilla se dé por completa (pipeline corrido de punta a punta, portafolio final definido), el proyecto **se saca de `user/projects/`** (la carpeta por defecto de StrategyQuant, donde conviven los proyectos de ejemplo que trae la instalación) y se guarda en una ubicación propia fuera de ahí — evita que los proyectos reales de la biblioteca se mezclen con los de ejemplo. El usuario confirmó que probó esto y funciona: StrategyQuant permite **abrir un proyecto desde cualquier ubicación vía un cuadro de diálogo de "buscar/cargar proyecto custom"**, no está limitado a listar solo lo que está en `user/projects/`. Falta definir la carpeta destino exacta (candidata razonable: dentro de la estructura de `SQX_Library/EURUSD/ReversionRango/` o en la carpeta personal `D:\Ariel De Armas\...`, a decidir cuando llegue el momento). **No se aplica todavía** — el proyecto sigue en `user/projects/EURUSD-REVRANGE-H1-001/` mientras se ensambla y prueba el pipeline nuevo.

## 4. Cuándo y cómo se aplica esto (requisito, no acción inmediata)

**No se aplica todavía a ninguna plantilla.** Se activa la primera vez que una plantilla logre su primera convergencia real (al menos 1 estrategia aceptada en el Build). En ese momento:
1. Se clona la estructura completa de un proyecto de ejemplo real ya validado (ej. `GBPJPY BREAKOUT H1 - Dukascopy`), no se escribe desde cero.
2. Se adaptan los parámetros específicos: símbolo/activo, rango de fechas de cada Retest (con criterio de régimen, sección 1 de esta regla), criterios de aceptación por etapa.
3. Se agrega explícitamente Walk-Forward Matrix/Optimization al plan de Retest (no estaba en el pipeline de 4 pruebas documentado en `patrones-validados-por-retest.md`, que salió de una búsqueda no dirigida del usuario — esto lo complementa con el mecanismo real de robustez de régimen).

**Estado actual (2026-07-08):** `EURUSD-REVRANGE-H1-001` ya tuvo su primera convergencia real (4 aceptadas, 10ma corrida — ver changelog.md de la plantilla), pero todavía no hay suficientes candidatas limpias (post-filtro OOS) para justificar armar el pipeline completo — este requisito queda anotado y pendiente.
