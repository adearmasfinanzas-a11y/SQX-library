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

### 1b. Práctica estándar: reservar datos físicamente fuera del alcance del Build para el Retest (establecida 2026-07-12)

El usuario aplicó, en `EURUSD-REVRANGE-H1-001`, una barrera más fuerte contra el sobreajuste que el simple split IS/OOS dentro del mismo dataset: **actualizó los datos históricos de la instalación hasta una fecha reciente (2026), pero deliberadamente no incluyó ese tramo más nuevo en el rango de datos que el Build usó** (Build se detuvo en `2024.12.31`). Resultado: el Build **nunca tuvo acceso físico** a esos datos, ni siquiera indirectamente vía el split IS/OOS interno — es una separación real, no solo una partición estadística dentro de lo mismo que el motor ya vio.

**Se fija como práctica estándar para toda plantilla futura:** al preparar los datos históricos de un proyecto nuevo, reservar conscientemente un tramo final reciente **fuera** del rango de fechas configurado en el Build, para usarlo exclusivamente como ventana de datos del Retest. La ventana de Retest debe traer los datos **hasta la fecha más actual disponible** (no detenerse antes) salvo que haya una razón concreta para reservar una porción aparte para una validación posterior — más operaciones reales en la ventana de Retest fortalece la confiabilidad estadística del resultado, mismo criterio ya aplicado al mínimo de `NumberOfTrades` del filtro OOS.

**Detalle de coherencia importante, aportado por el usuario al armar el Setup del Retest (2026-07-12): el `dateFrom` del Retest NUNCA arranca en la ventana reservada — siempre hereda el mismo `dateFrom` que usó el Build de esa plantilla.** Solo el `dateTo` se extiende hasta el dato más reciente disponible. Dos razones, probablemente actuando juntas:
1. **Warmup de indicadores:** un indicador de período largo necesita barras históricas previas para dar su primer valor válido — si el Retest cargara datos empezando justo en la ventana nueva, las primeras señales tendrían menos contexto del que tuvieron durante el Build (que sí arrancaba con toda la profundidad histórica), introduciendo una diferencia de comportamiento ajena a la validación OOS en sí. El parámetro nativo `ReservedBars` (default 50) no alcanza para indicadores de período largo en H1.
2. **Comparación completa en un solo reporte:** correr el Retest sobre todo el rango original + la ventana nueva permite ver en una sola corrida cómo se comportó la estrategia en el período que el Build ya conocía (IS + su OOS interno) y en el tramo genuinamente nuevo, sin tener que correlacionar corridas separadas.

La definición de qué tramo del rango cargado cuenta como IS/OOS **para el reporte de estadísticas** es un paso aparte (pestaña "Test Parameters"/distribución IS-OOS del Retest, no la pestaña de datos) — ahí es donde se marca el tramo reservado como el OOS que realmente importa juzgar.

**Confirmado por fuentes externas (2026-07-12) que esto es práctica reconocida, no una idea suelta:** es una versión reforzada del walk-forward validation estándar, que ya usa un "embargo period" (período de cuarentena temporal, ej. ~30 días) entre entrenamiento y validación específicamente para evitar fuga de datos — la separación **física** aplicada acá (el dato ni siquiera estaba descargado en la instalación durante el Build) es un caso extremo del mismo principio de "purging" (técnica estándar contra data leakage: excluir del set de entrenamiento cualquier muestra que pueda filtrar información hacia el set de validación). Fuentes: [Quanthop — Parameter Optimization Without Overfitting](https://quanthop.com/learn/backtesting-optimization/parameter-optimization), [CodeSignal — Addressing Data Leakage in Time Series](https://codesignal.com/learn/courses/preparing-financial-data-for-machine-learning/lessons/addressing-data-leakage-in-time-series), [QuantInsti — Walk-Forward Optimization](https://blog.quantinsti.com/walk-forward-optimization-introduction/).

### 1c. Protocolo automático de diseño IS/OOS para toda plantilla futura (establecido 2026-07-12)

Pedido explícito del usuario: que para cada plantilla nueva, el asistente pueda — sin que el usuario tenga que pensarlo desde cero cada vez —

1. **Leer los datos disponibles del activo** (rango de fechas real del histórico ya cargado en la instalación para ese símbolo/timeframe).
2. **Contrastar con información en internet** — igual que ya se hizo para EURUSD abajo: identificar quiebres de régimen reales del activo concreto (no genéricos) dentro de ese rango.
3. **Sugerir automáticamente** el rango de tiempo a usar en el Build, cómo dividirlo en IS/OOS, y el criterio de distribución de esos períodos — en función del histórico real disponible **y** de la hipótesis de la plantilla (ej. una hipótesis de ruptura puede querer incluir deliberadamente períodos de alta volatilidad que una de reversión preferiría excluir del IS).
4. La actualización de datos en sí (traer el histórico hasta la fecha más actual) se sigue haciendo manualmente desde la interfaz por ahora — **la tarea nativa `UpdateData`** (ya catalogada en la sección 3b) es la candidata natural para automatizar esto más adelante dentro del propio pipeline, pendiente de investigar con la misma metodología del punto 0 antes de proponerla.

**Honestidad sobre el punto 1 (leer datos disponibles):** se intentó leer directamente el archivo binario de histórico (`.dat`) de esta instalación durante esta sesión y no fue posible sin las herramientas propias de SQ — **no hay todavía un método confirmado** para que el asistente determine el rango de fechas real de un histórico sin pasar por la interfaz o por `sqcli`. Queda como investigación pendiente (con la misma disciplina de verificar antes de asumir) antes de poder ofrecer el punto 1 de forma verdaderamente automática — por ahora, se le pide al usuario que confirme el rango real desde la pestaña de Datos.

**Pista real encontrada para automatizarlo más adelante (2026-07-12):** StrategyQuant corre un servidor HTTP local propio (confirmado: `localhost:8080` responde con la app abierta) con una API interna tipo REST bajo rutas como `/data/checkQualitySummary`, `/data/getIndexForDate`, `/data/updateAll` (identificadas en `internal/plugins/DataManagerData/DMDataService.js`) — la interfaz web es un cliente de esa API. En teoría permitiría consultar rango de datos disponibles o disparar una actualización sin la interfaz. **No se probó en la práctica:** las llamadas POST reales requieren el cuerpo comprimido en gzip con codificación particular (`internal/web/app/sq-tools/sqbackend/services/BackendService.js`) y posiblemente un token de sesión (`sq-auth-token`) que vive en el estado interno del navegador embebido — no es un POST simple replicable a ciegas, y no se quiso arriesgar una llamada mal armada contra un proyecto real en uso (riesgo de disparar algo no deseado, ej. `updateAll` mal dirigido). Queda como vía de investigación confirmada pero pendiente de desarrollar con cuidado, en un contexto donde equivocarse no tenga costo (app cerrada o proyecto de prueba), antes de usarla en un proyecto real.

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
| **Retest** ("Retest strategies" en interfaz) | Vuelve a testear estrategias existentes con **una única** configuración de datos/época — la que usamos para revalidar contra la ventana OOS reservada. |
| **AutomaticRetest** ("Automatic retest" en interfaz, 2026-07-12) | Distinto de `Retest`, no una variante — decompilado `XmlChartCombinator`/`AutomaticRetestTask`: genera el **producto cartesiano** de combinaciones (varios símbolos × varias ventanas de fechas × varios escenarios de spread, etc.) y las corre todas en lote/paralelo (`GridClient`, `retestBatchSize`, `jobCount`). Pensado para estudios de sensibilidad/robustez a gran escala, no para una revalidación puntual — candidato interesante para más adelante (ej. sensibilidad contra varios pares FX correlacionados a la vez), no usado en esta etapa del pipeline. |
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
Build (motor genético, días)
  → SaveToFiles #1: TODO en bruto, sin filtrar → Reversion-Media\_runs\<fecha>_corridaN\01_bruto_build\
  → Filtering OOS (nativo, sin código; ver sección 3d/mecanismo-condiciones-filtrado.md) — no se exporta, barato de reconstruir
  → Retest (OOS sin solapar / mercado correlacionado, ver sección 1-3)
  → SaveToFiles #2 (recomendado, no obligatorio: Retest es reproducible pero caro) → 03_retest_aprobadas\
  → Filtering post-Retest (umbral más estricto, barato)
  → CreatePortfolio/AutomaticPortfolioBuilder — de-duplicación de la plantilla, NO el portafolio final de despliegue (ver sección 3g)
  → SaveToFiles #3: resultado final del pipeline → 04_portafolio_final\ — obligatorio, es el entregable real
  → LogDatabankStats (checkpoint de cierre)
  → Notification (avisa que terminó)
  → ClearDatabanks (limpia databanks internos de SQ — seguro, ya está todo exportado)
  → (sin GoToTask automático por defecto — ver sección 3f. Si el usuario decide correr todo de nuevo, es una acción deliberada suya, no un loop desatendido)
```

**Estado (2026-07-08, actualizado 2026-07-12):** catálogo investigado, secuencia validada en la práctica hasta `Filtering` (ver `mecanismo-condiciones-filtrado.md` — primer filtro corrido con éxito, 535/1000). Resto de la secuencia (Retest en adelante) todavía sin armar.

### Por qué el `SaveToFiles` inmediatamente después del Build es obligatorio, no opcional (2026-07-12)

El usuario preguntó, tras la primera corrida real de `EURUSD-REVRANGE-H1-001` (1000 estrategias, respaldadas manualmente porque era la primera plantilla en converger), si en plantillas futuras haría falta guardar el resultado completo del Build o alcanzaría con guardar solo lo que pasa el primer filtro.

**Respuesta con razonamiento, no por costumbre:** el Build **no es reproducible determinísticamente** — es búsqueda genética con aleatoriedad, así que correrlo dos veces con la misma configuración exacta da conjuntos de estrategias distintos, no el mismo resultado. Esto es una diferencia real de fondo con `Filtering` (que sí es determinístico: mismas condiciones + mismo databank de entrada = mismo resultado, siempre). Por eso:

- **El resultado bruto del Build es irrecuperable si se pierde** — no hay forma de "correrlo de nuevo y obtener lo mismo".
- **El resultado de `Filtering` es barato de reconstruir** — si hiciera falta, alcanza con volver a correr la misma tarea (condiciones ya documentadas en el `.cfx` del proyecto) contra el backup del Build ya guardado, en segundos.
- **Corrección (2026-07-12):** el pipeline **no hace loop automático por defecto** (aclarado por el usuario — puede ser una corrida completa única, repetida solo si el usuario lo decide explícitamente más adelante, no un `GoToTask` desatendido). Aun así, el `SaveToFiles` del bruto sigue siendo obligatorio: cada corrida completa (sea la única o la enésima que el usuario decida disparar) genera un lote de estrategias distinto e irrepetible, y si no se guarda antes de `ClearDatabanks`, se pierde esa corrida específica para siempre.

### 3g. Dónde viven físicamente los resultados del pipeline (decidido 2026-07-12)

Separación clara entre **archivos de estrategias** (`.sqx`, binarios, no diffeables) y **metadatos/documentación** (texto, versionable):

- **`D:\Ariel De Armas\Forex\EURUSD\Plantillas\Reversion-Media\_runs\<fecha>_corridaN\`** — única fuente real de los archivos `.sqx` de esa plantilla, en todas sus etapas exportadas (bruto, retest, final). Fuera de `D:\StrategyQuantoInstalado\` por completo (mismo motivo que el backup manual de las 1000: no depender de la integridad de la instalación de SQ). **No va a git** — sería peso muerto binario sin ningún beneficio de diff/historial. Protegido por **backup manual a otro disco, con la periodicidad que decida el usuario** (no automatizado dentro del pipeline).
- **`SQX_Library` (git, `EURUSD/ReversionRango/`)** — se queda con lo que ya hace bien: reglas, changelog, catálogo, presets de tareas (`.cfx`/`.xml`, chicos y singulares por tipo de tarea), y el registro de metadatos por estrategia (`linaje_estrategias.json`, ver `trazabilidad-versionado-estrategias.md`) — apunta a la ruta real en `Reversion-Media`, no contiene el binario. `SQX_Library` en sí sigue viviendo físicamente dentro de la instalación (`D:\StrategyQuantoInstalado\user\SQX_Library\`), pero ya no importa: desde el 2026-07-12 tiene su propio respaldo real e independiente (repo Git privado en GitHub), que cumple el mismo rol de "no depender de la instalación" que backup manual afuera. Se descartó mover la carpeta.

### 3h. La tarea "Portfolio" del pipeline NO es el portafolio final de despliegue (aclarado 2026-07-12)

Todas las estrategias que sobreviven el pipeline de una plantilla comparten **la misma hipótesis, mismo activo, mismo timeframe** — es poco probable que tengan baja correlación entre sí (principio central de `proceso-portafolio.md`: la diversificación real depende de baja correlación, y variaciones del mismo patrón de reversión en EURUSD H1 tienden a reaccionar a las mismas condiciones de mercado al mismo tiempo).

Por eso, `CreatePortfolio`/`AutomaticPortfolioBuilder` dentro del pipeline de **una sola plantilla** cumple un rol distinto al que su nombre sugiere: es un paso de **des-duplicación de la plantilla** (con tope de correlación, ej. 0.3 — mismo valor real visto en `PortfolioMaster`) que filtra variantes casi idénticas entre sí y deja un subconjunto más representativo de esa hipótesis puntual — **no** produce el portafolio diversificado listo para desplegar en real.

**El portafolio real de despliegue se arma después, combinando resultados de varias plantillas distintas** (esta + una futura de ruptura + otra de otro activo, etc.) — proceso aparte, posterior, con `PortfolioMaster`/`PortfolioComposer`, ya documentado en `proceso-portafolio.md`. El pipeline de una plantilla individual entrega materia prima ya des-duplicada para ese proceso, no el resultado final.

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

### Retest — investigado y confirmado en la práctica (actualizado 2026-07-12)

Investigado comparando el `task.xml` por defecto (`internal/plugins/TaskRetest/`) contra el `Retest-Task2.xml` real de `GBPJPY BREAKOUT H1 - Dukascopy`, y luego **armado paso a paso en la interfaz real de `EURUSD-REVRANGE-H1-001`** — pestaña "What to retest" y pestaña "Data" completas, confirmadas campo por campo.

**"What to retest":**
- **Databank Input=Output** en el ejemplo real (mismo databank de entrada y salida — testea in-place). Aplicado: `OOS_Filtrado` → `OOS_Filtrado` (no se conserva el estado pre-Retest aparte — es barato de reconstruir desde el bruto ya respaldado + el filtro ya documentado).

**"Data" — Backtest data settings:**
- **Symbol/Timeframe/Engine: idénticos al Build** (`EURUSD_dukas_M1_UTCPlus02`, H1, `MetaTrader5 (netted)`) — nunca cambiar el motor entre Build y Retest, mezclaría su propio efecto con la validación OOS real.
- **Start day = el mismo `dateFrom` del Build** (`2015.01.01`), nunca el inicio de la ventana reservada — ver sección 1b para el razonamiento completo (warmup de indicadores + comparación completa en un solo reporte).
- **End day = la fecha más reciente de datos disponible** (`2026.04.16` en esta corrida) — la ventana reservada se define por el `dateTo`, no por el `dateFrom`.

**"Data" — Test parameters:**
- **Precision: "1 minute data tick simulation (slow)"** (no la opción rápida por defecto) — con solo ~500 candidatas en esta etapa (vs. millones en el Build) se puede pagar el costo de mayor precisión, mismo techo real que ya establecimos para EURUSD en esta instalación (no hay datos de tick).
- **Comisión y swap: subidos ~30-35% sobre los valores reales del Build, como prueba de estrés moderada, no una fricción real igual ni una inventada al azar.** Ejemplo real aplicado: Build `Commission SizeBased=6` / `Swap money long=-6.5 short=-3.5` → Retest `Commission SizeBased=8` / `Swap money long=-8 short=-5`. Panel real: Commission → radio "Size based" (no "None") + campo de valor; Swap → toggle "Use" activado + `Swap type=money` + campos Long/Short — Triple swap day y Rollout hour se dejan igual que el Build (son mecánica del broker, no costo a estresar). Ver `configuracion-money-management.md` para el protocolo completo (definir broker objetivo ANTES del Build, subir el costo recién en el Retest).
- **Slippage: `1` pip.** El Build usa `slippage=0` (no hay base de la cual escalar un porcentaje) — se usa un valor moderado y realista en vez de un incremento proporcional, aprovechando que la interfaz ya trae `1` por defecto (asunción conservadora común en backtesting retail de FX, no es un valor extremo).
- **Min. distance: `0` pips, igual que el Build.** A diferencia de comisión/spread/slippage, este campo **no es un costo a estresar** — es una restricción estructural del broker (distancia mínima entre precio actual y una orden pendiente), no algo que suba con la fricción de trading. Se mantiene coherente con el Build, no se toca.

**"Data" — Data range parts (distribución IS/ISV/OOS, pendiente de completar):** existen 5 presets nativos de porcentaje genérico sobre todo el rango cargado (`50/20/30`, `30/20/50`, `20/20/10/20/20/10` walk-forward de 2 vueltas, IST/ISV alternado + cola OOS, IST/OOS alternado) — **ninguno sirve tal cual**, porque nuestro límite OOS real es una fecha exacta y deliberada (`2024.12.31`→`2025.01.01`), no un porcentaje del rango total. Se decidió usar **"Add new part"** para armar manualmente los tramos con esa fecha real como frontera, en vez de un preset — pendiente de confirmar cómo quedó armado en la práctica.

**"Trading options" — confirmada campo por campo contra el Build real (2026-07-12).** Estos campos **no son costos a estresar** (a diferencia de comisión/spread/slippage) — son reglas operativas que forman parte de la lógica misma de la estrategia, así que **deben copiar exactamente los valores del Build**, nunca cambiarse sin razón: `Exit At End Of Day` (off), `Exit On Friday` (on, `20:40`), `Limit Time Range` (off, `08:00`-`16:00`), `Max distance from market` (off, `6%`), `Maximum Trades Per Day` (`0`), `Min/Max SL/PT` (todos `0`), `Realistic Gaps Handling` (off), `Session` ("No Session"), `Store Chart Data` (off).

**Hallazgo real: bug de conversión de unidades en `DontTradeOnWeekends` del Build (2026-07-12).** El XML del Build tiene `FridayCloseTime=2300` y `SundayOpenTime=2300` — estos campos se guardan en **segundos desde medianoche** (mismo formato que `EODExitTime`/`FridayExitTime`, confirmado porque esos sí convierten exacto: `83040s→23:04`, `74400s→20:40`, ambos coincidieron con la interfaz real). Pero `2300` interpretado como segundos da **`00:38`**, no `23:00` — un valor sin sentido práctico (cortaría casi toda la sesión del viernes, y no tiene efecto real el domingo porque el mercado FX no reabre hasta la noche). Hipótesis: en algún momento se quiso guardar el valor sensato `23:00` pero se ingresó como el texto crudo `2300` en vez de convertirlo a segundos (`82800`). **`DontTradeOnWeekends=true` estuvo activo durante toda la corrida real del Build (2.5 días, 1000 estrategias)** con este valor roto — en la práctica, efecto casi nulo (no protegió contra riesgo de gap de fin de semana como se pretendía, pero tampoco recortó operativa de forma dañina). No se rehace el Build ya corrido por esto. **Corregido en el Retest** (escribiendo `23:00` directamente vía la interfaz, que se encarga de la conversión correcta) y **registrado como punto a verificar explícitamente al configurar el Build de cualquier plantilla futura** — no confiar en que escribir el número de la hora "tal cual" (ej. `2300`) haga lo esperado en este campo específico.

**"ATM":** confirmado `enable="false"` en el Build real — se deja igual en el Retest, mismo criterio que Trading Options (no es un costo a estresar, es gestión de ejecución que debe coincidir con lo que el Build realmente evaluó).

**"Money management":** confirmado `FixedSize`/0.1/`InitialCapital=10000`, coincide exactamente con el estándar ya fijado — sin cambios.

**"Cross checks (robustness)" — confirmado real, con estructura de 3 niveles (2026-07-12):** la interfaz agrupa 9 pruebas en `BASIC (FAST)`, `STANDARD (SLOW)`, `EXTENSIVE (SLOWEST)`:

| Nivel | Prueba | Qué hace |
|---|---|---|
| Basic | What If simulations | 2 simulaciones |
| Basic | Monte Carlo trades manipulation | 2 tests × 30 simulaciones |
| Basic | Higher backtest precision | backtest adicional a mayor precisión, como red de seguridad |
| Standard | Backtests on additional markets | retesta en otro símbolo/mercado configurable |
| Standard | Monte Carlo retest methods | 6 tests × 10 simulaciones |
| Standard | Sequential Optimization | distribución Up/Down %, steps |
| Extensive | Opt. Profile / Sys. Param. Permutation | hasta 1000 optimizaciones |
| Extensive | Walk-Forward Optimization | Out of sample %, N runs |
| Extensive | Walk-Forward Matrix | Out of sample % (rango), N runs (rango) — da el gráfico 3D de robustez |

**Dos hallazgos reales al revisar el default heredado del proyecto (probablemente de un proyecto de ejemplo del que partió esta plantilla):**
1. **"Higher backtest precision" venía activado en "1 minute data tick simulation (slow)"** — pero la precisión principal (pestaña Data) ya se subió exactamente a ese mismo nivel (el techo real para EURUSD, sin tick data disponible). Dejarlo activado compararía el resultado contra sí mismo, sin aportar nada — **se apaga**.
2. **"Backtests on additional markets" apuntaba a `EURUSD_M1_dukas`** — no es un mercado distinto, es una variante de datos del mismo EURUSD (remanente del proyecto de ejemplo original, spread=2 en vez del real 0.4). El propósito real de este cross-check es verificar si la lógica se sostiene en un mercado correlacionado pero genuinamente distinto — **se cambia el destino a GBPUSD** (misma familia `FX_mayor_liquido`, ya identificado como candidato en `proceso-portafolio.md`).

**Plan acordado con el usuario: robustez en 3 pasadas separadas, no todo de una vez** (evita pagar el costo computacional completo sobre candidatas que ya se van a filtrar en pasos previos):
1. **Este Retest:** solo `Higher backtest precision` (apagado) + `Backtests on additional markets` (GBPUSD) — el resto de robustez queda apagado.
2. **Segunda pasada (Retest futuro):** Monte Carlo trades manipulation + Monte Carlo retest methods, sobre las sobrevivientes de este primer Retest.
3. **Tercera pasada (Retest futuro):** Walk-Forward Matrix (preferido sobre Walk-Forward Optimization — da el gráfico 3D de robustez, mide directamente la métrica WFE de referencia ya documentada en sección 1), sobre las sobrevivientes de la segunda pasada.

**Aclaración importante del usuario (2026-07-12): "Backtests on additional markets" tiene su propio Setup independiente** (símbolo, fechas, spread, comisión) — no hereda nada de la pestaña "Data" del Retest principal. Cada campo de este Setup tiene su **propio toggle individual** (apagado = usa el valor por defecto/heredado que se ve en gris; encendido = override explícito con el valor propio) — patrón distinto al resto de la interfaz, confirmado en la práctica.

**GBPUSD configurado y confirmado (2026-07-12), sirve de plantilla para cualquier "mercado adicional" futuro:**
- **Symbol:** `GBPUSD_M1_dukas`. **Timeframe:** heredado, H1.
- **Start day:** toggle activado, `2015.01.01` — mismo `dateFrom` que el Build (ver principio de coherencia ya establecido), aunque acá hay que activarlo explícitamente (a diferencia del Setup principal, donde ya viene así).
- **End day:** `2026.04.10` — **6 días antes** que el dato más reciente de EURUSD (`2026.04.16`). Diferencia real de disponibilidad de histórico entre instrumentos, no se fuerza a coincidir, se usa el dato real de cada uno.
- **Precision:** heredado, "1 minute data tick simulation".
- **Spread `0.5`, Commission `$3 per full lot`** — confirmados con fuente externa real (FTMO): GBPUSD cotiza spread 0.5 pips + comisión $3/lote **round-trip** (entrada+salida combinadas, coincide exactamente con la unidad "$ per full lot" del campo). A diferencia del Retest principal (que sube el costo ~30-35% como prueba de estrés), acá se usan los **valores reales sin estrés adicional** — el objetivo de este cross-check es validar generalización a otro mercado, no duplicar la prueba de estrés de costos.
- **Slippage: `1` pip** — mismo criterio moderado que el Retest principal (no hay una cifra "publicada" de slippage para buscar, es una asunción razonable, no una tarifa fija de broker).
- **Min. distance: `0`**, sin cambios (no es costo, es regla estructural del broker).
- **Swap:** se mantuvo el valor real que ya traía cargado por defecto (`long: -1.93 short: -1.74` puntos) — a diferencia de comisión (que venía en `$0`, un placeholder sin sentido), este campo ya tenía un valor concreto y plausible, no se tocó.

Fuentes del dato de FTMO GBPUSD: [FTMO overall costs — BabyPips Forum](https://forums.babypips.com/t/ftmo-overall-costs/1221034), [Understanding Spreads and Commissions at FTMO](https://allproptradingfirms.com/understanding-spreads-and-commissions-at-ftmo/).

**Pendiente de confirmar en la práctica: ¿las tareas nuevas heredan la configuración de tareas del mismo tipo ya armadas, o hay que redefinir todo?** No hay evidencia directa todavía. Pista fuerte a favor de que NO heredan automático: existe un mecanismo manual en el menú contextual de cada tarea ("Copiar la configuración de la tarea" / "Copiar la configuración en una o varias tareas") — si la herencia fuera automática, ese botón no tendría mucho sentido. Recomendación de trabajo: terminar de configurar y guardar este Retest, usar "Copiar la configuración de la tarea" sobre él para aplicarla a la próxima tarea Retest (pasada de Monte Carlo) en vez de armar todo desde cero, y confirmar el resultado real antes de asumirlo como mecanismo definitivo.

**"Ranking" — confirmado con evidencia real y ajustado con criterio propio (2026-07-12/13):**

- **"Data range parts":** el mecanismo real de definir IST/ISV/OOS de este Retest. Al usar "Add new part" se cometieron dos errores en el camino, corregidos con revisión conjunta: (1) el primer intento dejó un único tramo `OOS1` desde `2021.11.17` hasta `2026.04.13` — mezclaba el OOS interno que el Build ya usó (2021.12-2024.12, sobre el cual ya filtramos con la primera tarea `Filtering`) con la ventana genuinamente nueva; (2) el segundo intento agregó un `OOS2` correcto pero dejó un `OOS1` mal ubicado (`2018.10.03`-`2021.11.17`, un tramo que en realidad es In-Sample del Build, no Out-of-Sample). **Configuración final correcta: un único tramo `Out of sample - Test`, `2025.01.01` a `2026.04.13`** (el día siguiente al `dateTo` del Build hasta el dato más reciente disponible) — sin solapar nada que el Build ya haya usado.
- **"Strategy Quality ranking (fitness)":** `Return/Drawdown ratio` sobre `Main data backtest` — coincide con el mismo tipo de Ranking que usa el propio Build (`ReturnDDRatio`), no es un filtro pass/fail, solo define el orden.
- **"Strategy filtering conditions" (el filtro pass/fail real) — cada condición tiene su propio tipo de muestra oculto** (no visible en la tabla resumida, hay que entrar a cada fila para verlo/cambiarlo) — mismo mecanismo ya documentado en `mecanismo-condiciones-filtrado.md`. Config final, con criterio propio investigado y no copiado del default:
  - `Avg. Trades Per Month > 2` — muestra **Completo** (chequeo de frecuencia general, no específico de la ventana nueva).
  - `Profit Factor >= 1.3` — muestra **Out of Sample**.
  - `Ret/DD Ratio >= 3` — muestra **Out of Sample**. **Corregido de `>4` a `>=3` (2026-07-13):** el usuario observó que muchas candidatas quedaban agrupadas entre 3 y 4, lo que llevó a reconsiderar el umbral. Razonamiento: `4` era más estricto que el propio `Ret/DD>3` que el Build ya validó sobre ~10 años de datos, pero esta condición corre sobre una ventana OOS mucho más corta (~15 meses) con más ruido estadístico — exigir un umbral más duro sobre una muestra más chica y ruidosa no selecciona necesariamente mejores estrategias, puede rechazar candidatas legítimas por una racha desfavorable puntual dentro de esos 15 meses. Se iguala al criterio ya validado del Build en vez de subirlo; la selectividad adicional de esta etapa ya la aporta `Sharpe Ratio`, no hace falta que `Ret/DD` cumpla las dos funciones a la vez.
  - `Sharpe Ratio >= 0.75` — muestra **Out of Sample**, **agregada por pedido explícito del usuario tras buscar en internet en vez de asumir que el combo por defecto (Trades/PF/Ret-DD) era suficiente.** Ninguna de las tres condiciones originales mide consistencia ajustada por riesgo (PF mide magnitud, Ret/DD mide la peor caída puntual) — Sharpe Ratio cubre esa dimensión faltante. Umbral `0.75` respaldado por fuente externa ("preferably above 0.75", con la advertencia de que valores backtesteados por encima de 1.5-2.0 suelen no sostenerse en vivo). De paso, la búsqueda confirmó por una vía independiente el concepto de Walk-Forward Ratio (OOS/IS) ya documentado como WFE en la sección 1 (≥0.5 aceptable, <0.3 sobreajuste) — coincide con lo ya establecido, no lo contradice.
  - **Nota sobre `>` vs `>=`:** el usuario cambió el comparador de las condiciones 2-4 a `>=` por consistencia — sin efecto práctico real sobre un valor continuo (la probabilidad de caer exactamente en el umbral es prácticamente nula).
  - Fuentes: [Validate Your Trading Edge with Out-of-Sample Backtesting](https://arongroups.co/forex-articles/out-of-sample-backtesting/), [Understanding Drawdown, Sharpe Ratio, and Profit Factor](https://quantstrategy.io/blog/essential-backtesting-metrics-understanding-drawdown-sharpe/), [Top 7 Metrics for Backtesting Results](https://www.luxalgo.com/blog/top-7-metrics-for-backtesting-results/).
- **"Delete FAILED strategies from databank": apagado** — coherente con `criterio-descarte-plantillas.md` (nunca se borra sin dejar registro).
- **"Run crosschecks independently from Custom filters failure": apagado.** Significado confirmado: si está encendido, los cross-checks (Backtests on additional markets en GBPUSD, Higher backtest precision) correrían **igual** aunque la estrategia ya haya fallado los 4 filtros personalizados; apagado (recomendado y aplicado), esos cross-checks **no se ejecutan** sobre estrategias que ya fallaron — ahorra cómputo, coherente con la filosofía de pasadas separadas ya establecida (no pagar robustez cara sobre candidatas que ya se van a descartar). Nota: cada cross-check tiene además su propia pestaña "Filtering" interna (vista en el diálogo de GBPUSD, no explorada en detalle) para condiciones específicas de ese cross-check en particular.

**Preset completo confirmado con el `.cfx` real exportado (`Retest strategies.cfx`, archivado en `EURUSD/ReversionRango/pipeline_tareas/Retest_strategies_task1.cfx`):** verificado campo por campo contra todo lo acordado en esta sección — `dateFrom=2015.01.01`/`dateTo=2026.04.16` (Setup principal), `dateFrom=2025.01.01`/`dateTo=2026.04.13` (Data range part OOS), `testPrecision=2`, `Commission SizeBased=8`, `Swap money long=-8 short=-5`, GBPUSD `Commission=3`/`Swap points long=-1.93 short=-1.74`, las 4 condiciones de Ranking con sus `sampleType` exactos (127 para Trades/mes, 20 para PF/Ret-DD/Sharpe), `DeleteFailedStrategies=false`. **El campo real detrás de "Run crosschecks independently from Custom filters failure" es `ForceRunCrossChecks`**, confirmado en `false`. **El fix del bug de fin de semana también se confirmó matemáticamente correcto:** `FridayCloseTime`/`SundayOpenTime=82800` segundos = exactamente `23:00`.

**Pendiente de investigar:** Notes.

Retest cruzado en GBPUSD (misma familia `FX_mayor_liquido`, ver `proceso-portafolio.md`) queda como candidato para chequear si la lógica se sostiene fuera del activo específico donde se descubrió — a decidir si se incluye en esta primera pasada o se deja para más adelante.

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
