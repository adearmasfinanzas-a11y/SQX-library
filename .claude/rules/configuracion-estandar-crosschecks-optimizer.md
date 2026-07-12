---
paths: SQX_Library/**
cargar_en: Al configurar en concreto los parámetros numéricos de Monte Carlo, SysParam Permutation u Optimizer para una plantilla (complementa proceso-robustez-crosschecks.md, que explica qué hace cada prueba pero no con qué números configurarla)
naturaleza: REFERENCIA TÉCNICA — valores de configuración comunes/recomendados, con fuente
---

# Configuración estándar de Monte Carlo, SysParam Permutation y Optimizer

Investigado en documentación oficial y foros de StrategyQuant. Esto complementa `proceso-robustez-crosschecks.md` (que explica QUÉ hace cada prueba) con los números concretos que se usan en la práctica.

## Monte Carlo (Manipulation y Retest)

- **Número de simulaciones:** 200 o más es lo recomendado; en la práctica se ve un rango de 100-500 según cuánto tiempo se le quiera dedicar (Monte Carlo Retest es más caro por simulación, ver `proceso-robustez-crosschecks.md`).
- **Qué significa el nivel de confianza del 95%:** es estadística directa — significa que solo hay un 5% de probabilidad de que el resultado real (Net Profit, Drawdown, etc.) sea peor que el valor reportado al 95%. No es un capricho de la industria, es la definición estándar de percentil de una distribución.
- **Criterio práctico de aceptación:** un punto de partida razonable es exigir que el valor al 95% de confianza siga siendo mejor que el 50% del resultado original (ej. si el backtest original dio Net Profit=10.000, el valor al 95% de las simulaciones debería seguir siendo >5.000) — algunos traders usan estándares más estrictos (98% o incluso 100%) para una evaluación más conservadora. No hay un número "correcto" universal — depende de cuánta degradación se está dispuesto a tolerar entre lo que se vio y lo que realmente podría pasar.

Fuentes: [Monte Carlo trades manipulation](https://strategyquant.com/doc/strategyquant/monte-carlo-trades-manipulation/), [What is Monte Carlo analysis and why you should use it?](https://strategyquant.com/blog/what-is-monte-carlo-analysis-and-why-you-should-use-it/), [Monte Carlo Explained in 6 Minutes](https://strategyquant.com/blog/monte-carlo-explained-in-6-minutes-the-stress-test-every-trader-needs/)

## System Parameter Permutation (SysParam) / Optimization Profile

- **Regla de oro, la más repetida en foros oficiales:** mantener el número de parámetros optimizables de la estrategia **lo más bajo posible** — 2-3 parámetros es ideal para poder testear TODAS las combinaciones posibles (fuerza bruta) en vez de solo una muestra. Menos grados de libertad = más robusto y más resistente a sobreajuste.
- **Configuración compartida por un usuario experimentado en el foro oficial** (no es un estándar universal de StrategyQuant, pero es un punto de referencia real): Max Test=500, Up=30%, Down=30%, Max Step=10, criterio de aceptación: % de optimizaciones "adecuadas" > 30%, ganancia promedio > 0, distribución uniforme < 5.
- **Qué computa realmente:** la **mediana** de cada estadística de rendimiento (Net Profit, Drawdown, %Drawdown, Sharpe, etc.) a través de todas las permutaciones — esa mediana es la estimación realista del rendimiento esperado, no el resultado del set de parámetros "ganador" original.

Fuentes: [Optimization Profile and System Parameter Permutation](https://strategyquant.com/doc/strategyquant/optimization-profile-system-parameter-permutation-strategyquant/), [Sys. Param Permutation(SSP) test recomendations](https://strategyquant.com/forum/topic/sys-param-permutationssp-test-recomendations/)

## Optimizer (genético vs. fuerza bruta)

- **Cuándo usar cada uno:** fuerza bruta cuando el espacio de combinaciones es chico (2-3 parámetros) — encuentra el óptimo real, garantizado, por búsqueda exhaustiva. Genético cuando el espacio es demasiado grande para fuerza bruta — más rápido, resultado muy bueno pero no garantizado como el óptimo absoluto.
- **Tamaño de población recomendado:**
  - Para **refinar una estrategia ya existente**: población grande (50-100) — le da al algoritmo más material para trabajar, sin demasiada diversidad.
  - Para **explorar muchas estrategias distintas**: población chica (10-30) — permite testear variedad rápido.
- **Islands (subpoblaciones):** 1-10 recomendado. Más de 10 no tiene mucho sentido — infla la población total y hace lenta cada generación.
- **Migration rate (tasa de migración entre islands):** proporcional al tamaño de población — para población=10, usar 10-20%; para población=100, usar 1-5%.

Fuentes: [Settings - Genetic options](https://strategyquant.com/doc/strategyquant/genetic-options/), [Best settings for Genetic Evolution](https://strategyquant.com/forum/topic/best-settings-for-genetic-evolution/)
