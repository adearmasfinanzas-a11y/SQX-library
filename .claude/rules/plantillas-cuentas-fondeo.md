---
paths: SQX_Library/**
cargar_en: Al diseñar una plantilla cuyo objetivo explícito es pasar el challenge/evaluación de una cuenta de fondeo (prop firm), no operativa de largo plazo
naturaleza: MARCO DE DISEÑO — reglas del juego distintas a una plantilla de largo plazo, con razonamiento propio explícito donde no hay fuente directa
---

# Plantillas orientadas a cuentas de fondeo (prop firm) — un objetivo distinto, con reglas distintas

Investigado con fuentes externas (reglas típicas de la industria de fondeo) más razonamiento propio explícito sobre cómo traducir eso a configuración de StrategyQuant — se marca claramente qué es fuente y qué es razonamiento propio, tal como pidió el usuario.

## 1. Reglas típicas de un challenge de fondeo (fuente externa, 2026)

- **Pérdida diaria máxima:** 3-5% del balance inicial (lo más común: 4-5%).
- **Drawdown máximo total:** 8-12%. Importa mucho si es **estático** (calculado sobre el balance inicial, no se mueve) o **trailing** (se mueve con el equity pico) — el estático es mucho más amigable: si la cuenta crece, el colchón real aumenta; el trailing no perdona nunca.
- **Objetivo de ganancia:** típicamente 8-10% en la fase 1, 4-5% en la fase 2 (si el challenge tiene dos fases).
- **Plazo:** 30-60 días típico, pero **no es obligatorio operar todos los días** — el límite es de tiempo calendario, no de días operados.

Fuentes: [The5ers — Prop Firm Drawdown Rules 2026](https://the5ers.com/prop-firm-drawdown-rules-explained-daily-max-and-trailing-limits-in-2026/), [Prop Firm Rules Explained 2026 — Velotrade](https://velotrade.com/blog/prop-firm-rules-explained), [TradeZella — Risk Management Framework 2026](https://www.tradezella.com/blog/pass-prop-firm-challenge)

- **Dato de contexto duro:** solo 5-14% de los traders pasan una evaluación, y solo ~7% llega a cobrar un payout — el objetivo de "pasar" no es trivial ni para humanos ni para sistemas automáticos. Fuente: [FPFX Tech — análisis de 300.000+ cuentas](https://atmosfunded.com/prop-firm-statistics/), [funderpro.com — Prop Trading Pass Rates 2025](https://funderpro.com/blog/prop-trading-pass-rates-in-2025-what-the-data-really-shows/)

## 2. Por qué el diseño tiene que ser distinto a una plantilla de largo plazo (razonamiento propio)

Una plantilla de largo plazo se evalúa sobre años de historia, buscando una ventaja estadística que se sostenga en el tiempo — el "cuándo" importa poco, lo que importa es la consistencia a lo largo de todo el período. Una plantilla de fondeo se juega en una **ventana de tiempo corta y fija** (1-2 semanas hasta 1-2 meses) con un **objetivo de ganancia concreto** y un **límite de pérdida diaria estricto** — el objetivo no es "tener ventaja en general", es "no romper las reglas y llegar al objetivo dentro de esa ventana específica".

Esto tiene consecuencias directas de diseño:
- **Frecuencia de operación:** un timeframe muy lento (H4/D1, pocas operaciones al mes) puede no generar suficientes señales dentro de una ventana de 1-2 semanas para alcanzar el objetivo — hace falta suficiente densidad de oportunidades en el timeframe elegido. Esto no significa "operar más rápido a cualquier costo" (el límite de pérdida diaria sigue mandando), pero sí descarta timeframes/hipótesis de baja frecuencia como candidatas naturales para fondeo.
- **El criterio de aceptación correcto NO es el mismo que para largo plazo.** ProfitFactor/Drawdown sobre el período completo de IS/OOS no responde la pregunta real, que es: "¿con qué frecuencia esta estrategia habría cumplido el objetivo de ganancia SIN violar el límite diario ni el drawdown máximo, dentro de una ventana del mismo largo que el challenge real?"

## 3. Metodología: tasa de éxito por ventana móvil (Rolling Window Pass Rate)

Existe como concepto en la comunidad de trading (aplicado con herramientas de terceros, no es un botón nativo de StrategyQuant): en vez de mirar un solo resultado IS/OOS, se toma el historial completo y se evalúa **cada ventana posible del mismo largo que el challenge** (ej. cada ventana de 14 días dentro de 5 años de historia) contra las reglas reales (objetivo de ganancia alcanzado, sin violar pérdida diaria ni drawdown máximo) — y se calcula qué **porcentaje de esas ventanas** habría pasado. Esto responde una pregunta mucho más útil que un solo pass/fail: "¿qué tan consistentemente esta estrategia encaja con las reglas de este challenge?", no "¿le fue bien una vez?".

Fuente conceptual: [TradesViz — Prop Firm Compliance Dashboard, Rolling Window Analysis](https://www.tradesviz.com/blog/prop-firm-compliance-tracking/)

**Honestidad sobre la implementación real:** StrategyQuant no tiene un botón nativo para esto — el Retest estándar calcula estadísticas sobre el período completo, no una tasa de éxito por ventanas móviles. Lo más cercano nativo es el **Walk-Forward Matrix** (ya documentado en `pipeline-multitarea-y-diseno-is.md`), que sí evalúa en ventanas sucesivas, pero con el objetivo de optimización walk-forward, no con las reglas específicas de un challenge (pérdida diaria, drawdown, objetivo de ganancia en X días). **Para calcular la tasa de éxito real por ventana según las reglas de un fondeo específico, hace falta un análisis a medida** (exportar la lista de operaciones/equity curve y procesarla programáticamente aplicando las reglas exactas del challenge elegido, ventana por ventana) — no es un checkbox, es trabajo de análisis adicional. Queda pendiente definir la implementación concreta cuando se aborde la primera plantilla de fondeo.

## 4. Cuántos intentos paralelos hacen falta para pasar ~1 por semana (razonamiento propio, estadística básica)

Si el paso anterior nos da una **tasa de éxito empírica `p`** (ej. "esta estrategia habría pasado el 20% de las ventanas de 14 días de la historia"), la pregunta de cuántos intentos correr en paralelo para tener aproximadamente 1 challenge aprobado por semana es un problema de probabilidad binomial estándar:

- **Valor esperado:** para esperar ~1 aprobado por semana, hacen falta aproximadamente `k ≈ 1/p` intentos paralelos independientes esa semana. Con `p=20%`, eso son ~5 intentos paralelos.
- **Con un nivel de confianza concreto** (ej. querer 95% de probabilidad de al menos 1 aprobado, no solo el promedio): `k ≥ ln(1-confianza) / ln(1-p)`. Con `p=20%` y 95% de confianza: `k ≈ ln(0.05)/ln(0.80) ≈ 13.4`, o sea unos 14 intentos paralelos para tener 95% de confianza de al menos 1 éxito esa semana (no solo el promedio de 1).

**Advertencia crítica, la parte más importante de este razonamiento:** esta matemática **solo es válida si los intentos son realmente independientes entre sí.** Correr la misma estrategia, en el mismo instrumento, al mismo tiempo, en 14 cuentas distintas **no son 14 intentos independientes** — todas van a ganar o perder juntas porque están expuestas exactamente al mismo movimiento de mercado. Para que la matemática de arriba tenga sentido real, los intentos paralelos necesitan alguna fuente real de independencia:
- Estrategias distintas (hipótesis/activo/timeframe no correlacionados entre sí — ver `proceso-portafolio.md`, mismo criterio de correlación que para portafolios).
- O el mismo tipo de estrategia pero arrancada en momentos escalonados (cada una expuesta a una ventana de mercado distinta, no simultánea).
- Probablemente una combinación de ambas cosas en la práctica.

Sin esa independencia real, correr "14 copias idénticas simultáneas" no da 14 tiros de dado distintos — da 1 solo tiro de dado, repetido 14 veces con el mismo resultado. Es el mismo principio de correlación que ya aplicamos para portafolios de largo plazo, aplicado acá a la escala de intentos de fondeo.

## 5. No sobre-exigir robustez de largo plazo, pero sí un piso objetivo real

Estos sistemas no necesitan la robustez de una plantilla de largo plazo (no van a operar años, solo semanas) — pero **sí necesitan cumplir un piso objetivo de rendimiento y riesgo real**, no ser pura suerte disfrazada. El criterio de aceptación correcto para una plantilla de fondeo, dado todo lo anterior, es la **tasa de éxito por ventana móvil** (sección 3) por encima de un umbral razonable — no ProfitFactor/Drawdown de largo plazo, y tampoco "aprobó una vez de casualidad".

## Estado (2026-07-08)

Sección de diseño registrada, no implementada todavía — no hay ninguna plantilla de fondeo en la biblioteca. Se retoma cuando se aborde la primera, momento en el cual hay que definir en concreto: reglas exactas del fondeador objetivo (varían entre firmas), implementación real del cálculo de tasa de éxito por ventana móvil, y qué combinación de plantillas/timeframes puede dar independencia real entre intentos paralelos.
