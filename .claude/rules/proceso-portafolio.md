---
paths: SQX_Library/**
cargar_en: Cuando la biblioteca tenga suficientes plantillas validadas como para combinarlas en un portafolio (no antes)
naturaleza: REFERENCIA TÉCNICA — construcción, prueba y configuración de portafolios en StrategyQuant X
---

# Portafolios en StrategyQuant X — Portfolio Composer vs. Portfolio Master

Investigado con documentación oficial. Este archivo se usa recién cuando haya **varias plantillas ya validadas** (pasaron Retest/robustez) — combinar estrategias no probadas en un portafolio no resuelve nada, solo mezcla incertidumbre.

## Dos herramientas distintas, propósitos distintos

### Portfolio Composer — combinación manual, control total
Cargás las estrategias candidatas a la izquierda y elegís **cuáles combinar y con qué peso**. El peso reescala el money management original: peso=100% usa el tamaño de posición original, 200% lo duplica, 50% lo reduce a la mitad. Sirve para simular y ajustar a mano una combinación específica que ya tenés en mente.

Fuente: [Portfolio Composer](https://strategyquant.com/doc/strategyquant/portfolio-composer/)

### Portfolio Master — construcción automática
En vez de armar la combinación a mano, **busca automáticamente** la mejor composición de portafolio entre un conjunto grande de estrategias candidatas, aplicando filtros de correlación (ver abajo) para evitar combinar estrategias que en la práctica se mueven juntas.

Fuente: [Portfolio Master - Automatic Portfolio Builder](https://strategyquant.com/blog/portfolio-master-automatic-portfolio-builder/), [Automatic Portfolio Construction](https://strategyquant.com/doc/strategyquant/automatic-portfolio-construction/)

## Correlación — el criterio central para armar un portafolio bueno

**La idea:** un portafolio de estrategias muy correlacionadas entre sí no diversifica nada — si todas ganan y pierden al mismo tiempo, el portafolio se comporta como una sola estrategia más grande, no como una cartera diversificada. El objetivo es combinar estrategias con **correlación baja**, para que cuando una atraviesa un mal período, las otras compensen.

**Cómo se mide (configurable):**
- **Base de la correlación:** Profit/Loss, apertura/cierre de operaciones o posiciones.
- **Período de agregación:** por hora, día, semana o mes — el más habitual es correlación diaria (compara el P/L día a día de cada estrategia).
- **Filtro:** se puede pedir que el Portfolio Master descarte combinaciones cuya correlación supere un umbral definido.

Fuente: [Portfolio correlation explained](https://strategyquant.com/doc/quantanalyzer/portfolio-correlation-explained/)

## Exportación de un portafolio a MT5 — mecánica real (investigado 2026-07-09)

Verificado en la instalación (proyecto real `user/projects/PortfolioMaster/`, plugin `ResultsSourceCode`) más fuentes del foro oficial — el código de generación de MQL vive compilado dentro de un `.jar`, no se puede leer como texto, así que la mecánica exacta se confirmó por evidencia externa, no por lectura directa del código fuente de StrategyQuant:

- **No exporta un único EA combinado automáticamente.** El comportamiento por defecto es exportar **cada estrategia del portafolio por separado** (misma pestaña Source Code que para una estrategia individual) y adjuntar **varias instancias de EA al mismo gráfico/símbolo** — no una fusión nativa. Existen soluciones de la comunidad para combinarlas en un solo EA, pero no es el flujo out-of-the-box.
- **El Magic Number distinto por estrategia NO es automático.** Confirmado por un hilo del foro titulado literalmente "Let's make an Automatic Unique MagicNumber for each strategy in our portfolio!" — la comunidad tuvo que construir su propia solución (extraer el nombre del archivo, convertirlo a entero, reemplazar el parámetro `MagicNumber` a mano). En la práctica, como cada estrategia se exporta como EA separado, el Magic Number es un parámetro de entrada (`input`) normal de MQL5 — se configura al adjuntar cada EA al gráfico en MetaTrader mismo, no en las opciones de exportación de StrategyQuant.
- **Sí hay acceso al código fuente `.mq5`/`.mq4` real, no solo al binario compilado** — la pestaña "Source Code" genera y muestra el código editable antes de guardarlo (confirmado, ver `proceso-exportacion-live.md`).

Fuentes: [Let's make an Automatic Unique MagicNumber for each strategy](https://strategyquant.com/forum/topic/lets-make-an-automatic-unique-magicnumber-for-each-strategy-in-our-portfolio/), [Same EA, Same Magic Number, Different Pair](https://strategyquant.com/forum/topic/4007-same-ea-same-magic-number-different-pair/), [Multi Strategies on the same symbol and the same time frame](https://strategyquant.com/forum/topic/multi-strategies-on-the-same-symbol-and-the-same-time-frame-for-futures-trading/)

## Cómo esto se conecta con el diseño de esta biblioteca

- El campo `familia_activo` del catálogo (`_catalogo/indice_maestro.json`) ya anticipa esto — plantillas de la misma familia (ej. `FX_mayor_liquido`: EURUSD, GBPUSD) son más propensas a estar correlacionadas entre sí que plantillas de familias distintas (ej. `Indices_CFD` vs `FX_mayor_liquido`).
- Al planificar un portafolio de plantillas de esta biblioteca, priorizar combinar **distintas familias de activo y distintas hipótesis** (reversión + tendencia + momentum) antes que múltiples plantillas de la misma familia/hipótesis — reduce la correlación esperada sin necesitar medirla primero.
- La verificación real de correlación (con datos, no solo con la heurística de familia) se hace en Portfolio Master/Composer una vez que hay candidatos reales para combinar.

## Estado actual de la biblioteca (2026-07-07)

Ninguna plantilla tiene todavía una versión validada (pasó Build + Retest/robustez) — este archivo queda como referencia para cuando llegue ese momento, no hay nada que aplicar todavía.
