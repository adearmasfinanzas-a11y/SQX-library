---
paths: SQX_Library/**
cargar_en: Al diseñar o revisar el plan de Retest/robustez de una plantilla (sección 18 de la ficha), y al armar el pipeline multi-tarea (ver pipeline-multitarea-y-diseno-is.md)
naturaleza: REFERENCIA TÉCNICA — qué es y para qué sirve cada prueba de robustez real de StrategyQuant X
---

# Pruebas de robustez (CrossChecks) de StrategyQuant X — qué hace cada una y cuándo usarla

Investigado con documentación oficial de StrategyQuant y verificado contra la estructura real de proyectos de ejemplo de esta instalación. Todas estas pruebas viven como `CrossChecks` — se configuran dentro de una tarea de Retest (`Retest-TaskN.xml`, sección `<CrossChecks>`) o como plugins independientes (`internal/plugins/CrossCheck*`).

## 1. Monte Carlo Manipulation (`CrossCheckMonteCarloManipulation`)

**Qué hace:** reordena/perturba las operaciones **ya ejecutadas** del backtest (no corre un backtest nuevo — por eso es muy rápido). Genera cientos de curvas de equity alternativas para ver si el resultado depende de la secuencia exacta de operaciones.

**Métodos típicos:**
- *Randomize Trades Order* — baraja el orden de las operaciones. No cambia el profit neto, pero muestra qué tan distinto podría verse el drawdown según el orden en que hubieran caído las operaciones.
- *Skip trades randomly* — omite operaciones al azar con cierta probabilidad, simulando "¿y si algunas operaciones no se hubieran ejecutado?".

**Cuándo usarla:** siempre, es barata computacionalmente. Es la primera línea de defensa contra "la curva se ve bien solo por casualidad en el orden de las operaciones".

Fuente: [Monte Carlo trades manipulation](https://strategyquant.com/doc/strategyquant/monte-carlo-trades-manipulation/)

## 2. Monte Carlo Retest (`CrossCheckMonteCarloRetest`)

**Qué hace:** simula cambios aleatorios en propiedades que **sí requieren recorrer el backtest de nuevo** — spread, slippage, parámetros de la estrategia, o los datos históricos mismos. Cada simulación es un backtest completo, por eso es mucho más lento (si el backtest principal tarda 0.5s y pedís 100 simulaciones, son ~50s por estrategia).

**Cuándo usarla:** después de Monte Carlo Manipulation, sobre el subconjunto de estrategias que ya sobrevivieron el primer filtro (más caro, no correrlo sobre miles de candidatos sin filtrar antes).

Fuente: [Monte Carlo retest methods](https://strategyquant.com/doc/strategyquant/monte-carlo-retest-methods/)

## 3. Retest con mayor precisión (`CrossCheckRetestWithHigherPrecision`)

**Qué hace:** vuelve a probar la estrategia usando datos de mayor granularidad (tick, si están disponibles, o el nivel más fino que haya) para descartar que el resultado sea un artefacto de la precisión de testeo usada durante el Build (recordar `testPrecision` — ver `calibracion-motor-genetico.md`).

**Cuándo usarla:** si el Build corrió en modo rápido (`testPrecision=1`), esta prueba es la que confirma si el resultado se sostiene con más detalle intrabarra.

## 4. Optimization Profile / System Parameter Permutation (`CrossCheckOptProfileSysParamPermutation`)

**Qué hace:** en vez de evaluar la estrategia con un solo set de parámetros, prueba **todas las combinaciones posibles** de un rango alrededor de los parámetros originales, y calcula el valor **mediano** de cada métrica (Net Profit, Drawdown, etc.). La idea: si el resultado real depende de un pico aislado de parámetros exactos (sobreajuste), la mediana de la vecindad de parámetros va a ser mucho peor que el resultado original — si el resultado es robusto, la mediana se parece al original.

**Configuración:** elegís qué tipo de parámetros se permutan y el máximo número de optimizaciones a correr. El filtro de aceptación se define sobre los valores medianos (ej. "Mediana de Net Profit > X"), no sobre el resultado original.

**Cuándo usarla:** es una de las pruebas más importantes contra el sobreajuste — siempre que se pueda, después de los Monte Carlo.

Fuente: [Optimization Profile and System Parameter Permutation](https://strategyquant.com/doc/strategyquant/optimization-profile-system-parameter-permutation-strategyquant/)

## 5. Walk-Forward Optimization / Walk-Forward Matrix (`CrossCheckWalkForwardOptimization`, `CrossCheckWalkForwardMatrix`)

Ver `pipeline-multitarea-y-diseno-is.md` para el detalle completo (metodología, métrica WFE, diseño de IS/OOS con criterio de régimen). Resumen: reoptimiza la estrategia en ventanas sucesivas de tiempo y valida cada una en el período siguiente. La Matrix corre esto con varias combinaciones de período de reoptimización y %OOS a la vez, para encontrar cuál es más estable (gráfico 3D: cambios graduales = robusto, abruptos = sobreajustado).

**Cuándo usarla:** la prueba más cara computacionalmente — se corre sobre el subconjunto final de candidatos, no sobre todo el universo de estrategias.

## 6. Sequential Optimization (`CrossCheckSequentialOptimization`)

**Qué hace:** a diferencia de la optimización estándar (fuerza bruta o genética, que busca la combinación de parámetros con mejor fitness), busca acercar la estrategia a la **zona estable cercana a la mediana** de resultados — no el pico más alto, sino la meseta más consistente alrededor.

**Cuándo usarla:** cuando se sospecha que el resultado actual está en un pico aislado (posible sobreajuste) y se quiere reubicar la estrategia en una zona de parámetros más estable, sin cambiar la lógica.

Fuente: [Sequential optimization](https://strategyquant.com/doc/strategyquant/sequential-optimization/)

## 7. Retest en mercados adicionales (`CrossCheckRetestOnAdditionalMarkets`)

**Qué hace:** corre la misma estrategia, sin cambiar su lógica, sobre otros símbolos (típicamente correlacionados, ej. EURUSD→GBPUSD, o el mismo activo en otro broker/fuente de datos). Si la lógica captura una ineficiencia real del mercado (no un artefacto de los datos específicos), debería comportarse razonablemente en mercados relacionados.

**Cuándo usarla:** especialmente relevante para plantillas de esta biblioteca, dado que trabajamos por familia de activo (`familia_activo` en el catálogo) — probar una plantilla de EURUSD también contra GBPUSD (misma familia `FX_mayor_liquido`) es un chequeo natural.

Fuente: [Retest on additional markets](https://strategyquant.com/doc/strategyquant/retest-additional-markets/)

## 8. What-If (`CrossCheckWhatIf`)

**Qué hace:** simulaciones de "qué pasaría si" sobre condiciones específicas del mercado o de ejecución — se puede configurar como parte del proceso de construcción automáticamente.

## Orden recomendado (de más barato a más caro)

1. Monte Carlo Manipulation (rápido, filtra el volumen grande)
2. Optimization Profile / SysParam Permutation (mediano)
3. Monte Carlo Retest (más caro, backtest completo por simulación)
4. Retest en mercados adicionales / mayor precisión (sobre el subconjunto que ya pasó lo anterior)
5. Walk-Forward Matrix/Optimization (el más caro — reservado para los finalistas)

Este orden evita gastar cómputo caro en candidatos que ya se podían descartar con una prueba barata.
