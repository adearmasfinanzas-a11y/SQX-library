---
paths: SQX_Library/**
cargar_en: Paso 6 del protocolo (Diseño), sección de configuración genética/RulesComplexity de la ficha
naturaleza: ESTABLE — basado en evidencia real de la instalación, se corrige solo si aparece evidencia nueva que la contradiga
---

# Calibración del motor genético (Full Builder)

Esta regla existe porque `EURUSD-REVRANGE-M15-001` corrió 2h54min, generó 107.446 estrategias y aceptó **0** (log real: `user/projects/EURUSD-REVRANGE-M15-001/log/global_log_20260705_184206.log`).

**Corrección importante (2026-07-07):** una primera versión de esta regla comparó el `Build-Task1.xml` equivocado — el del proyecto genérico `user/projects/Builder/`, que resultó ser una tarea vieja de "mejora" de una instalación StrategyQuant 4 distinta (`D:\work\StrategyQuant4\...`), no relacionada con este trabajo. Esa versión decía `PopulationSize=15`, `MaxGenerations=10` y Money Management `FixedAmount` — **esos números eran incorrectos**. Se verificó el `project.cfx` real de `EURUSD-REVRANGE-M15-001` y esta regla quedó reescrita con los valores reales.

## Configuración real de `EURUSD-REVRANGE-M15-001` (verificada, 2026-07-07)

- `RulesComplexity`: minConditions=2, maxConditions=3, minExitConditions=1, maxExitConditions=3, minPeriod=5, maxPeriod=50, minShift=1, maxShift=1 (fijo).
- `PopulationSize=100`, `MaxGenerations=50`, Islands=4 — **esto ya está razonablemente cerca de lo que usan los proyectos de ejemplo** (ver abajo), no es el problema principal.
- SL: 30-80 pips, ATR true, multiplicador 1.5-2.5, período ATR explorable 15-30.
- PT: 60-200 pips, ATR true, multiplicador 2-5, período ATR explorable 15-30.
- Money Management: `FixedSize`, Size=0.1, InitialCapital=10000 — **ya coincide con el estándar de la biblioteca** (ver `configuracion-money-management.md`), no hubo desviación real.
- Criterios de aceptación en el Build (3 simultáneos): ProfitFactor > 1.3, NumberOfTrades ≥ 100, ReturnDDRatio > 1.5.
- **Confirmado directamente por el usuario en la interfaz del Full Builder (2026-07-07):** la pestaña "What to build" muestra **"Simple strategy"** — el modo "mejorar estrategia existente" que se sospechaba por el XML (`improveType="strategy"`, `PartsToImprove` con `ExitRules use="true"`) **no está activo en la práctica**; esos campos son valores heredados del esquema XML sin efecto real cuando el tipo de estrategia es "Simple". Esta parte de la hipótesis queda descartada — no hace falta tocar nada ahí.
- **Sí confirmado como problema real:** en la pestaña de datos, el símbolo es **`GBPUSD_M1_dukas`** (no EURUSD) y el motor es **MetaTrader4** (no MetaTrader5) — la temporalidad M15 sí es correcta. El proyecto corrió sobre el par y motor equivocados frente a lo que dice la ficha de diseño. El log de 107.446 estrategias / 0 aceptadas es sobre GBPUSD M15 MT4, no sobre EURUSD M15 MT5.
- **Acción confirmada y aplicada por el usuario:** corregido el Setup principal a símbolo `EURUSD_dukas_M1_UTCPlus02`, motor MetaTrader5 (netted), M15 sin cambios. Los criterios de aceptación (sección 15 de la ficha) y los rangos de SL/PT (sección 14) se mantienen tal como estaban diseñados originalmente — no se tocan por defecto, ver puntos 1 y 2 de la regla más abajo.

## Comparación contra proyectos de ejemplo reales (sí producen resultados)

Se compararon 4 proyectos de ejemplo de la instalación (`GBPJPY BREAKOUT H1`, `GOLD BREAKOUT M30`, `NQ CFD H1`, `GBPUSD H1` — en `user/projects/`, fechados 2026-04-10/20, sin relación con el trabajo propio del usuario, pero con valor de referencia real):

| Parámetro | `EURUSD-REVRANGE-M15-001` | Ejemplos reales | Diferencia real |
|---|---|---|---|
| `PopulationSize` | 100 | 100 | Igual — no es el problema |
| `MaxGenerations` | 50 | 100 | Menor, pero no drásticamente |
| Condiciones de aceptación en el Build | 3 simultáneas | 1 sola | **Diferencia real** — los ejemplos solo exigen `ProfitFactor > umbral` en el Build; el resto de criterios se difieren a Retest |
| Período ATR para SL/PT | Rango explorable (15-30) | Fijo (=20) | Los ejemplos lo fijan, pero eso no lo convierte en mejor práctica — explorarlo como rango es una decisión de diseño legítima (ver punto 2 de la regla más abajo), no un defecto a corregir |
| `improveType`/`strategyFile` | Campo heredado sin efecto — confirmado "Simple strategy" en la interfaz | No presente en los ejemplos revisados | Descartado como causa — ver confirmación del usuario más abajo |

## Regla para nuevas plantillas

1. **Durante el Build, mantener un piso de calidad real — no reducirlo a un único filtro trivial, pero tampoco copiar números fijos de una plantilla a otra sin justificar.** Los 4 ejemplos usan solo `ProfitFactor > 1` (equivale a "no pierde plata", no a un mínimo exigible); ese extremo no sirve. Preferencia explícita del usuario (2026-07-07, corregida el mismo día): los criterios de aceptación en el Build **no son un valor rígido de biblioteca** — se analizan de forma crítica para cada plantilla y se documentan con su justificación en la ficha (sección 15), solo cuando corresponda tener cada uno. No hay un número universal correcto.

   **Qué mirar al justificar cada criterio para una plantilla concreta:**
   - `ProfitFactor`: el umbral razonable depende del tipo de hipótesis — estrategias de reversión/alta frecuencia suelen tener PF más bajo con win-rate alto; breakout/tendencia suelen necesitar PF más alto porque el win-rate es menor. No exigir el mismo número a todas las hipótesis por igual.
   - `NumberOfTrades`: el mínimo razonable depende de cuántas oportunidades genera el timeframe elegido durante el IS — M15 sobre 10 años produce muchísimas más barras que H1 sobre el mismo período, así que un mínimo que es exigente en H1 puede ser casi trivial en M15 (y viceversa). Se calcula en función de la densidad de oportunidades esperable, no se copia el número de otra plantilla.
   - `ReturnDDRatio` (u otro ratio riesgo/retorno): el umbral razonable depende de la volatilidad típica del activo y de si el SL/PT está calibrado en términos absolutos o relativos (ATR) a esa volatilidad.

   Si tras corregir cualquier problema estructural (símbolo/motor, población/generaciones) la corrida sigue devolviendo 0 o casi 0 aceptadas, se relaja **progresivamente** empezando por el criterio con la justificación más débil o más difícil de cumplir a la vez con los demás — no se elimina el control de calidad de entrada de un saque, y cualquier relajación se documenta con su motivo.
2. **Corrección de criterio (2026-07-07):** no se recomienda fijar el período de ATR como constante por defecto. Explorar rango de período **y** de multiplicador en SL/PT permite estructuras de salida dinámicas — el motor calibra la gestión de riesgo según la volatilidad real que encuentra en cada combinación, en vez de imponer una distancia fija que puede ser adecuada para unos parámetros de entrada e inadecuada para otros. Esto es una dimensión más del espacio de búsqueda con valor propio, no ruido a eliminar por default. Se fija un eje solo si hay evidencia concreta de que la población/generaciones disponibles no alcanzan para explorar razonablemente todos los ejes a la vez — no porque un proyecto de ejemplo lo haga así.
3. `PopulationSize=100` como piso razonable; `MaxGenerations` cerca de 100 si el tiempo de cómputo lo permite.
4. **Verificar explícitamente en la pestaña "What to build" del Builder que el proyecto arranca desde cero ("Build new strategies"), no en modo "mejorar" una estrategia base** — sobre todo si el proyecto se creó duplicando/copiando la configuración de otro proyecto o plantilla, porque ese campo puede arrastrar una ruta de archivo obsoleta sin que se note en la interfaz.

## Hallazgo adicional (2026-07-07, mientras corría la prueba con símbolo/motor ya corregidos): Building Blocks de SL/PT sin acotar

En `Build-Task1.xml`, sección `<BuildingBlocks>`, los bloques de tipo `Indicators.*` habilitados coinciden exactamente con la ficha (`ADX`, `BollingerBands`, `RSI` — 3 en total, nada más). **Pero los bloques `Stop/Limit Price Levels.*` y `Stop/Limit Price Ranges.*` tienen ~29 mecanismos habilitados** (Keltner Channel, Ichimoku, Parabolic SAR, Linear Regression, SMA/EMA/SMMA/TEMA/LWMA, Highest/Lowest, BBRange, BarRange, MTATR, BiggestRange, SmallestRange, además de ATR), cuando la ficha (sección 14) especifica únicamente SL/PT basado en ATR.

**Por qué importa:** el espacio de búsqueda de "cómo se construye el stop/take" queda casi 10 veces más amplio que el espacio de búsqueda de "qué dispara la entrada" (3 indicadores). Esto puede diluir la capacidad del motor de converger — parte del presupuesto combinatorio se gasta explorando qué mecanismo de precio usar para el SL/PT en vez de refinar la lógica de entrada.

**Aplicado el 2026-07-07** (con la aplicación cerrada, editado directamente el `project.cfx` y verificado por reextracción): se dejaron habilitados únicamente `Stop/Limit Price Ranges.ATR` y `Stop/Limit Price Ranges.MTATR` para la distancia de SL/PT, desactivando el resto. Motivo confirmado por evidencia real: la corrida con `minConditions=1` empeoró "cierra en la misma barra" de 14.79% a 27.27%.

**Corrección inmediata necesaria:** al desactivar también TODOS los `Stop/Limit Price Levels.*`, el proyecto dejó de poder ejecutarse — error real de StrategyQuant: *"You use Enter at Stop or Limit, you have to choose some Price Level blocks!"*. Esa categoría no es exclusiva del SL/PT: también construye el precio de la orden cuando la entrada es de tipo Limit o Stop (ej. "entrar en Low[1]"). **Regla aprendida:** si se restringe `Stop/Limit Price Levels`, hay que dejar habilitadas al menos las referencias de precio básicas no-indicador (`Ask`, `Bid`, `Open`, `High`, `Low`, `Close`) cuando el proyecto usa entradas Limit o Stop — solo se pueden apagar completamente si todas las entradas son `EnterAtMarket`. Los mecanismos basados en indicador (Keltner, Ichimoku, medias, VWAP, SuperTrend, etc.) sí se mantienen apagados. Backups sucesivos de cada paso en `user/projects/EURUSD-REVRANGE-M15-001/_backups/`.

## Hallazgo adicional (2026-07-07): `testPrecision` y el tamaño de vela — confirmado con documentación oficial

El símbolo usado (`EURUSD_dukas_M1_UTCPlus02`) son barras M1 (no ticks reales, y no hay datos de tick disponibles en esta instalación para EURUSD — M1 es el techo real). El parámetro `testPrecision` del `<Setup>` (1 en nuestro proyecto y en 3 de los 4 ejemplos; 2 en el restante, `GBPUSD H1`) controla cuántos datos intrabarra usa el motor para resolver el orden real en que se tocan SL/TP dentro de una misma vela.

**Confirmado por documentación oficial de StrategyQuant** ([Settings - Data](https://strategyquant.com/doc/strategyquant/data/)), los 4 niveles reales de precisión de testeo son:
1. **Selected Timeframe Only** (`testPrecision=1`, el más rápido) — simula con 4 "ticks" por vela (Open/High/Low/Close de la vela principal). Cita textual: *"for Stop or Limit orders the testing accuracy might not be sufficient, and you should try more precise mode"*.
2. **1 Minute Data** (`testPrecision=2`) — usa datos M1 reales para simular 4 ticks por minuto dentro de la vela (si hay datos M1 disponibles).
3. **Real Tick – Custom Spread** — Bid real de tick data, Ask calculado con spread fijo.
4. **Real Tick Data** — tick real completo, la más precisa y lenta, para verificación final.

**Práctica oficial recomendada:** correr el Build en el modo más rápido para filtrar rápido, y reservar precisión alta para el Retest — es lo que hacen 3 de los 4 proyectos de ejemplo (H1/M30), y es válido para ellos porque el SL/TP basado en ATR rara vez cae dentro del rango de una sola vela grande. **Pero la propia documentación reconoce la excepción exacta en la que estamos:** esta plantilla usa `EnterAtLimit` (orden Limit) para la entrada, con velas M15 (mucho más chicas, solo 15 barras M1 de resolución interna) y SL/PT ajustado por ATR — es precisamente el caso "Stop or Limit orders" que la documentación señala como insuficiente en el modo rápido.

**Resultado real (2026-07-07):** se aplicó `testPrecision=2` y se corrió 3h52min (58.013 estrategias). **La hipótesis no se confirmó como solución** — "cierra en la misma barra" empeoró de 30.16% a 35.16%, no mejoró. Sí bajó drásticamente "transacciones ambiguas" (0.9%→0.02%). **Conclusión correcta:** no era un problema de medición/precisión — con más precisión, el motor confirma con más certeza que el SL/TP **genuinamente** se toca dentro de la misma vela M15 la mayoría de las veces. El problema real es de **calibración de distancia** (el SL está probablemente demasiado ajustado para la volatilidad real de M15), no de resolución de datos. Ver sección siguiente.

## Hallazgo pivotado (2026-07-07): el SL probablemente está calibrado demasiado ajustado para M15, no es un problema de medición

Con `testPrecision=2` confirmado como medición correcta (no el problema), la causa mecánica más probable de que ~30-35% de las estrategias cierren en la misma vela es que el **SL mínimo (1.5x ATR) es matemáticamente chico frente a la variabilidad normal de una sola vela M15**: `ATR(n)` es un *promedio* del rango de las últimas `n` velas — por definición, muchas velas individuales tienen un rango igual o mayor a ese promedio. Un SL de solo 1.5x ese promedio puede quedar dentro del rango de una única vela más volátil que el promedio reciente, sin que haga falta que el precio "reviente" varias velas — con que una sola vela sea ~1.5-2x más volátil que el promedio (algo común, no un evento raro), el SL queda tocado dentro de esa misma vela.

**Candidato de ajuste, no aplicado todavía (pendiente confirmar con el usuario):** subir el multiplicador mínimo de ATR para el SL (actualmente `MinSLATRMultiple=1.5`) a un valor donde la distancia sea claramente mayor que el rango típico de una sola vela — ej. 3x como mínimo en vez de 1.5x — para que el SL no quede dentro del alcance de una única vela volátil. Se evalúa junto con el usuario antes de aplicar, dado que esto cambia sustancialmente el perfil riesgo/retorno de la plantilla (SL más ancho implica menos operaciones por parada prematura, pero también mayor riesgo nominal por operación).

## Evaluado y descartado (2026-07-07): patrones de vela de reversión de la plantilla oficial "Mean-Reversal"

Se revisó `internal/web/BUILDER/simpleTemplates/Mean-Reversal.xml` (plantilla nativa de StrategyQuant). Habilita `BearishEngulfing`, `DarkCloud`, `ShootingStar` (patrones de reversión bajista) y el bloque `EnterReverseAtMarket`. **Decisión: no se agregan a esta plantilla.** Motivos:
1. Son unidireccionales (solo bajistas) — nuestra hipótesis es simétrica (`MarketSides=both`); copiarlos tal cual introduce un sesgo direccional no buscado. Los equivalentes alcistas (Bullish Engulfing, Piercing Line) están en la plantilla `TrendFollowing`, no en `Mean-Reversal` — la separación entre ambas plantillas oficiales no parece un diseño riguroso de "reversión vs tendencia", más bien un preset de demostración con una partición arbitraria de bloques.
2. Dudoso valor estadístico de patrones de vela clásicos (origen en velas diarias de acciones/commodities) en un par muy líquido y arbitrado como EURUSD en M15, frente a los indicadores estadísticos ya usados (ADX/Bollinger/RSI).
3. No es el momento — agregar más bloques ahora, antes de saber si el ajuste de SL/PT resuelve la convergencia, confundiría el próximo diagnóstico.

**Si el ajuste de SL/PT tampoco resuelve la convergencia**, reconsiderar patrones de vela como variable aislada y agregarlos de forma simétrica (ambas direcciones), no copiando el subconjunto sesgado de la plantilla oficial.

## Hallazgo de hardware (2026-07-07): posible infrautilización de CPU

Esta máquina tiene un **Intel Core i9-14900K (24 núcleos / 32 hilos), 64GB RAM, SSD NVMe** — hardware muy potente, muy por encima de lo necesario en términos generales. Pero el 14900K tiene **arquitectura híbrida** (núcleos P-core + E-core), y según foros oficiales de StrategyQuant esto requiere una configuración específica en **Tools → Options → Performance** para que el motor aproveche todos los hilos disponibles — por defecto podría estar usando solo una fracción.

**Por qué importa:** si esto no está bien configurado, las corridas de horas que venimos viendo podrían tardar varias veces más de lo necesario no por la calibración genética en sí, sino por no usar el hardware disponible. Antes de seguir optimizando parámetros de búsqueda, vale la pena confirmar esta configuración — es un multiplicador de velocidad más barato que cualquier ajuste de población/generaciones.

Fuentes: [SQX Performance per core / Threadrippers / i9](https://strategyquant.com/forum/topic/sqx-performance-per-core-number-of-cores-threadrippers-i9/), [How to set the PC to run multicore/multithread](https://strategyquant.com/forum/topic/4478-how-to-set-the-pc-to-run-multicoremultithread/)

**Verificado y confirmado (2026-07-07):** el usuario ya tenía configurado 24 hilos (coincide con los núcleos físicos del i9-14900K, evitando contención de hyperthreading en cargas de cómputo intensivo) y 32GB de tope de memoria (mitad del sistema, deja margen para el SO). Configuración deliberada y razonable — no era el cuello de botella. El 0% de CPU que se había detectado se debía a que el proyecto estaba pausado, no a infrautilización de hardware.

## Criterio de tiempo prudente de espera (2026-07-07, con fuente externa)

Benchmark real de la comunidad StrategyQuant: en hardware mucho más débil (i7/8GB) suele aparecer "algo" en ~30 min; con configuración general en pares FX mayores a M15-H4, lo normal es **20-35 estrategias aceptadas por hora**. Fuentes: [Benchmark and Strategies per hour](https://strategyquant.com/forum/topic/benchmark-and-strategies-per-hour/), [PC setups, configuraciones y benchmarks](https://strategyquant.com/forum/topic/pc-setups-configurations-benchmarks-and-general-recommendations/).

Con hardware potente (i9-14900K/64GB, ver sección de hardware más abajo) pero una búsqueda más acotada que "configuración general" (3 indicadores específicos), el criterio adoptado: dar margen hasta **~2 horas totales sin ninguna aceptada** antes de considerar que algo requiere ajuste (ej. relajar el criterio de Profit Factor). No es una regla rígida — se ajusta según cuán sano se vea el patrón de rechazo (un solo filtro dominante y entendible = más margen; mezcla de causas mecánicas raras = menos margen, revisar antes).

## Nota sobre "0 estrategias aceptadas" como síntoma general

Si una corrida futura vuelve a devolver 0 (o casi 0) estrategias aceptadas, el diagnóstico se hace en este orden:
1. ¿El símbolo/motor/timeframe del Setup principal son realmente los que dice la ficha (verificar en la interfaz, no asumir por el XML)?
2. ¿`PopulationSize`/`MaxGenerations` están muy por debajo de 100/100 sin justificación?
3. ¿La población/generaciones disponibles son proporcionales a la amplitud real del espacio de búsqueda (todos los rangos combinados, incluyendo período de ATR y multiplicadores si se exploran como rango)? Explorar más ejes como rango no es un error en sí — solo se ajusta si hay evidencia de que el cómputo disponible no alcanza para explorarlos.
4. Solo si lo anterior está calibrado razonablemente y sigue sin aparecer nada, se relaja **progresivamente** el criterio de aceptación más difícil de cumplir en el Build (no se elimina todo el control de calidad de una vez).
5. Solo si aun así sigue sin aparecer nada, se revisa si la hipótesis en sí es demasiado restrictiva para el activo/timeframe elegido.
