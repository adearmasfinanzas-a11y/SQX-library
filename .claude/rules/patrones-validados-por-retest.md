---
paths: SQX_Library/**
cargar_en: Paso 6 del protocolo (Diseño), como referencia de mecánica de archivo (formato, SL/PT, Money Management) — NO como evidencia de qué diseño de hipótesis conviene
naturaleza: VIVO — se amplía cada vez que el usuario aporte nuevas estrategias reales que hayan pasado Retest
---

# Patrones reales de estrategias que pasaron Retest — corrección importante de alcance (2026-07-07)

**Corrección crítica del usuario, mismo día:** los dos ejemplos analizados (`Strategy 1.1.26.sqx`, `Strategy 4.7.22(1).sqx`, EURUSD H1, MetaTrader4) **no fueron aportados como evidencia de un diseño de hipótesis intencional que funciona** — el usuario aclaró que esas corridas se hicieron dejando un montón de building blocks abiertos, sin definir una hipótesis a propósito, "para que el Builder buscara algún diamante" sin dirección clara. Es decir: **la simplicidad de una sola condición que se observa ahí es un resultado incidental de una búsqueda no dirigida, no una lección de diseño validada.**

**Esto invalida la conclusión anterior de este archivo** ("apilar condiciones reduce la probabilidad de éxito, hay que simplificar la entrada") como argumento para rediseñar `EURUSD-REVRANGE-M15-001` u otras plantillas de esta biblioteca. El propósito explícito de este proyecto es exactamente lo contrario de esas corridas: darle al Builder **intencionalidad de hipótesis desde el principio** (Activo × Hipótesis × arquitectura de capas razonada), no dejarlo buscar a ciegas con todo habierto. No se debe imitar la simplicidad de esos ejemplos como principio de diseño — solo se aprovecha la información de **mecánica de archivo** (formato real, SL/PT híbrido, Money Management), que sigue siendo válida y no depende de si la hipótesis detrás era intencional o no.

**Lo que sigue siendo válido de este archivo:** la estructura técnica real (ver secciones de abajo) — formato del `.sqx`, mecanismo SL/PT ATR+Trailing, Money Management, pipeline de pruebas de robustez. **Lo que ya no se usa como argumento:** cualquier afirmación de que "menos condiciones de entrada es mejor diseño" basada en estos dos casos — esa conclusión se retira.

## Qué tienen en común

- **Entrada: una sola condición**, no una pila de capas simultáneas. `BBLowerRising` / `BBUpperFalling` (Bollinger Bands) — un único indicador dispara la señal de entrada.
- **Ejecución:** orden `Stop` en `Low[1]`/`High[1]` (el extremo de la barra anterior), no Market/Limit.
- **Salida:** se cierra la posición cuando la propia condición de entrada deja de cumplirse — no hay un indicador de salida independiente.
- **SL/PT híbrido:** basados en ATR (coeficiente y período explorables) **más un Trailing Stop fijo en pips** encima — no es solo SL/PT estático ni solo ATR puro, es una combinación de ambos.
- **Money Management:** `FixedSize`, 0.1 lotes — coincide con el estándar ya fijado en `configuracion-money-management.md`. Validación cruzada real de esa regla.

## Pipeline de robustez real usado (para la sección 18 "Plan de Retest" de la ficha)

En vez de un plan genérico, esto es lo que realmente se corrió y filtró candidatos válidos:
1. **Monte Carlo Manipulation** — decenas de simulaciones reordenando/perturbando el orden de las operaciones, para ver si el resultado depende de la secuencia exacta.
2. **Monte Carlo Retest** — remuestreo de la serie de resultados.
3. **CrossCheck de mayor precisión** — reprueba con datos de mayor granularidad (tick-level), genera curva de equity diaria, para descartar que el resultado sea un artefacto de la precisión de testeo usada en el Build.
4. **Perfil de sensibilidad de optimización** (`optimizationProfile.bin`) — mide qué tan sensible es el resultado a pequeños cambios en los parámetros; una estrategia robusta no debería desplomarse con variaciones menores de sus propios parámetros (esto es lo que el usuario se refería como "SysParam Permutation").

## Qué SÍ se puede seguir usando de esta evidencia

- Considerar el patrón híbrido SL/PT-ATR + Trailing fijo como una opción de diseño válida, no solo ATR puro o pips fijos — esto es mecánica de archivo, no depende de si la hipótesis detrás era intencional.
- Usar el pipeline de 4 pruebas de esta nota como punto de partida real para la sección 18 de la ficha (plan de Retest), en vez de una lista genérica — igual de independiente de la hipótesis.
- Money Management `FixedSize`/0.1 como estándar de biblioteca (ya validado por otra vía, ver `configuracion-money-management.md`).

## Qué NO se debe inferir de estos 2 ejemplos puntuales

- ~~"Menos condiciones de entrada es mejor diseño, porque estos 2 ejemplos usan 1"~~ — retirado como argumento. La simplicidad observada ahí fue incidental a una búsqueda sin hipótesis, no una lección de diseño validada.

## Pero el rango 1-3 condiciones sí tiene respaldo independiente (no de estos ejemplos)

Verificado por fuera de estos dos casos, con documentación oficial de StrategyQuant y práctica general de trading algorítmico:

- **StrategyQuant recomienda oficialmente** limitar el máximo de condiciones por señal a **1-2** para robustez (Settings → What to build → Number of conditions).
- **Regla general de sobreajuste:** cada parámetro optimizable de una estrategia requiere aproximadamente **200 operaciones out-of-sample** para validarse con confianza — más condiciones/parámetros simultáneos multiplican ese requisito.
- Consenso general: usar pocos indicadores bien elegidos en vez de combinar muchas señales reduce el riesgo de sobreajuste y mejora la robustez fuera de muestra.

**Conclusión correcta:** el rango `minConditions=1, maxConditions=3` que ya tiene `EURUSD-REVRANGE-M15-001` está dentro de lo recomendado por fuentes independientes — no hace falta bajarlo más basándose en los 2 ejemplos personales del usuario (esa parte del argumento se retira), pero el valor en sí (1-3) sigue siendo razonable por otra vía. El problema de convergencia de `EURUSD-REVRANGE-M15-001` (0 aceptadas en 4+ corridas reales) **no se explica por el número de condiciones** — sigue sin explicación satisfactoria y requiere seguir investigando otras causas (mecánica de la orden de entrada, construcción de las condiciones concretas, etc.), no simplificar la hipótesis por simplificar.
