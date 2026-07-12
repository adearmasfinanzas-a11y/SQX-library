---
paths: SQX_Library/**
cargar_en: Paso 0 del protocolo (siempre, al iniciar cualquier sesión de trabajo sobre este proyecto)
naturaleza: VIVO — es el archivo que más cambia; se reescribe su cabecera cada cierre de sesión, el historial solo crece
---

# Estado actual del proyecto — SQX_Library

Este archivo es el **punto de partida obligatorio** de cualquier sesión de trabajo sobre la biblioteca. Su función es que retomar el trabajo después de días o semanas no dependa de la memoria de una conversación concreta. Se lee entero antes de hacer cualquier otra cosa (Paso 0 de `CLAUDE.md`).

---

## Informe de presentación — ENTREGADO (2026-07-09)

**Ya no está pendiente.** Se generó `_presentacion/informe_presentacion_v1.html` con toda la especificación de abajo. Si se pide una actualización futura, es una v2 sobre este archivo, no una entrega desde cero.

## Pendiente futuro (registrado 2026-07-09, no se hace hasta que el usuario lo pida explícitamente)

**Informe de presentación del proyecto**, formato HTML/CSS profesional (usar la herramienta Artifact), para público interesado en trading pero no experto en StrategyQuant (algo de conocimiento de trading, no de SQ). Debe cubrir, con honestidad completa (no solo lo positivo):

**Marco general a tener en cuenta al redactarlo (añadido 2026-07-09):** todo este proyecto es la base para construir un flujo de trabajo de trading automatizado en distintos niveles, con buenas prácticas y gestión controlada de cada detalle, buscando capitalización sostenida o rentabilidad de mediano/largo plazo — no una plantilla suelta. Incluir además ideas propias de qué más se podría hacer en conjunto que el usuario pueda no haber contemplado por su propia perspectiva (limitaciones de visión que él mismo reconoce), no limitarse a repetir literalmente lo ya discutido.
- La idea del proyecto de punta a punta: desde el origen hasta la visión más ambiciosa a futuro.
- Todo lo que se planea hacer y cómo se está haciendo.
- Lo que Claude aprendió específicamente desde el punto de vista técnico sobre StrategyQuant y cómo operarlo.
- Las plantillas y el pipeline completo que se quiere montar, y por qué (arquitectura de búsqueda, Retest/robustez, portafolios).
- Capacidades reales de Claude en este proyecto — honesto, no solo lo prolijo.
- Los problemas reales encontrados en el camino (las 10+ corridas en 0 antes de la primera convergencia, errores propios corregidos en el camino, etc.) y que hay que seguir superando.
- Dejar explícito que puede hacer falta cambiar de enfoque si las cosas se tuercen — no presentar el plan como garantizado.
- La visión de largo plazo ya registrada en `vision-proyectos-futuros.md`: bots de gestión de portafolio configurables, plataforma remota de monitoreo/gestión, optimización asistida por IA basada en análisis de datos en tiempo real — sus mecanismos reales de implementación y las complicaciones que puede traer.
- **Cubrir la responsabilidad de Claude en el tema de IA/automatización** — dejar claro los límites y el portón de validación obligatorio ya establecido (cualquier ajuste sugerido por IA sobre datos en vivo debe pasar de nuevo por el proceso de validación de StrategyQuant antes de aplicarse a una cuenta real, nunca aplicación automática directa).

## Foco actual

**Plantilla activa:** `EURUSD-REVRANGE-H1-001` (EURUSD, Reversión a la media en rango, H1, MetaTrader5)
**Estado:** `databank_lleno_1000_pendiente_ensamblar_pipeline`
**Dónde quedó exactamente (2026-07-12):** el Build de la duodécima corrida (testPrecision=1) terminó solo por `StopCondition databank-full` al llegar a **1000 estrategias** en el databank (2 días 16 hrs, log `log/global_log_20260709_085433.log` — detalle completo en `EURUSD/ReversionRango/changelog.md`, entrada 2026-07-12). Las 1000 fueron exportadas y respaldadas en `D:\Ariel De Armas\Forex\EURUSD\Plantillas\Reversion-Media\_backup_1000_bruto_2026-07-12\` (verificado: 1000 `.sqx`). Es material **en bruto, sin filtrar ni validar en OOS** — no confundir con candidatas curadas.
**Próximo paso concreto:** ensamblar el pipeline multi-tarea (Filtering → CustomAnalysis con filtro OOS Profit Factor → Retest → Portfolio → SaveToFiles → LogDatabankStats → Notification → ClearDatabanks → GoToTask) sobre el databank actual, con la tarea de Build puesta en `active="false"` (ya cumplió su función, mecanismo nativo confirmado) mientras se prueba el resto. Diseño y config. propuesta de cada bloque en `.claude/rules/pipeline-multitarea-y-diseno-is.md` secciones 3b-3e. Falta escribir `FilterByOOSProfitFactor.java` (solo diseñado, no creado como archivo) y compilar en Code Editor los 35 archivos de indicadores/patrones nuevos (17+18, dos rondas de investigación) que quedaron pendientes de compilar mientras el Build corría — ver `_indicadores/indicadores_propuestos.json`.

**Contexto histórico (M15, resuelto por pivote a H1):** el proyecto original `EURUSD-REVRANGE-M15-001` no logró convergencia real en 7 corridas (ver detalle completo abajo, sección "Historial de sesiones" y `EURUSD/ReversionRango/changelog.md`). Se pivotó a H1 el 2026-07-07 tras confirmar que toda la evidencia real disponible converge en H1/M30, nunca M15. El proyecto fue renombrado a `EURUSD-REVRANGE-H1-001` y logró su primera convergencia real en las corridas siguientes (12 candidatas organizadas en `candidatas_build/` antes de esta corrida final a 1000).

**Realidad de fondo (confirmada por el usuario el 2026-07-07):** el problema no es solo "falta pedir que se genere el `.sqx`". **Hasta ahora apenas se ha logrado crear algo compatible y funcional en el Full Builder.** La dificultad real está en la construcción/generación dentro de StrategyQuant X, no en el diseño conceptual (fichas, hipótesis, catálogo) — eso sí avanza con normalidad.

**Diagnóstico (2026-07-07, corregido):** se confirmó que `EURUSD-REVRANGE-M15-001` es el **único** proyecto real del usuario que no salió bien — el resto de proyectos en `user/projects/` (GBPJPY, GOLD, NQ, DJ CFD, etc.) son proyectos de ejemplo que vienen con la instalación, no intentos fallidos propios, y sirvieron como referencia de calibración real.

Se leyó el log real de la corrida (`log/global_log_20260705_184206.log`): 107.446 estrategias generadas, **0 aceptadas**, en 2h54min — este dato es sólido y real. **Corrección importante:** una primera comparación de configuración usó por error el `Build-Task1.xml` del proyecto genérico `Builder` (una tarea vieja no relacionada) en vez del `project.cfx` real de `EURUSD-REVRANGE-M15-001`. Verificado el archivo correcto:
- `PopulationSize=100`, `MaxGenerations=50` (no 15/10 como se dijo antes) — ya cerca de la referencia de los ejemplos, no es el problema principal.
- Money Management ya es `FixedSize`/0.1/10000 (no `FixedAmount`) — ya cumplía el estándar de la biblioteca, no había desviación real que corregir.
- 3 condiciones de aceptación simultáneas en el Build (ProfitFactor>1.3, Trades≥100, ReturnDDRatio>1.5) — esto sí es una diferencia real frente a los ejemplos (1 sola condición en Build).
- Período de ATR explorado como rango (15-30) — igual que el diseño original, diferencia real pero de impacto secundario.
- **Confirmado directamente por el usuario en la interfaz (2026-07-07):** en "What to build" figura "Simple strategy" — se descarta la hipótesis del modo "mejorar estrategia existente" (los campos del XML que lo sugerían son residuales, sin efecto real). Sí se confirmó el problema real: en la pestaña de datos, símbolo = `GBPUSD_M1_dukas` (no EURUSD), motor = MetaTrader4 (no MetaTrader5); temporalidad M15 correcta. **El Build del 2026-07-05 corrió sobre GBPUSD/MT4 — el log no es evidencia sobre la hipótesis EURUSD M15 MT5.**

**No es un problema de la hipótesis ni de la arquitectura de 5 capas** — ambas ya estaban correctamente evaluadas en el sistema de reglas existente. Documentado (y corregido) en `.claude/rules/calibracion-motor-genetico.md` y `.claude/rules/configuracion-money-management.md`.

**Ficha de diseño actualizada** (`EURUSD/ReversionRango/ficha_diseño.json`, secciones 12-20): valores reales verificados por sección, propuesta de diferir 2 de las 3 condiciones de aceptación a Retest, y sección 20 nueva documentando el hallazgo de la ruta inexistente pendiente de verificar en la interfaz del Builder. Estado de la plantilla: `recalibrada_pendiente_nueva_corrida`.

**Cambio de fondo en el protocolo:** la ficha de diseño ahora es autosuficiente — cubre toda la configuración del Builder (RulesComplexity, genética, SL/PT, criterios de aceptación por etapa, Trading Options, Money Management, plan de Retest/Optimizer), no solo el diseño conceptual. Ver `.claude/rules/formato-entrega.md` (secciones 12-19). Esto aplica a partir de ahora para toda plantilla nueva o revisada.

**Pendiente inmediato:** aplicar los valores recalibrados en el Full Builder (a mano, por el usuario) y correr de nuevo `EURUSD-REVRANGE-M15-001`. Si el usuario ajusta algo distinto a lo propuesto en la ficha durante la construcción, se documenta en el changelog de la plantilla (mecanismo ya existente).

---

## Pendientes / cabos sueltos a revisar

- ~~Referencia a "otros casos de EURUSD H1"~~ — **resuelto el 2026-07-07:** confirmado por el usuario que ese caso nunca existió. Se corrigieron las dos menciones en `EURUSD/ReversionRango/changelog.md`.
- `_indicadores/indicadores_propuestos.json` está vacío — no hay indicadores no nativos en curso todavía.
- La matriz `matriz-activo-hipotesis.md` solo tiene la fila `FX_mayor_liquido` poblada con ejemplos de partida; el resto de familias (FX_cruzado_menor, Indices_CFD, Materias_primas) están definidas pero sin ningún activo real trabajado todavía.

---

## Próximos pasos sugeridos (a confirmar con el usuario, no asumir)

1. ~~Corregir símbolo/motor~~ — **hecho y verificado.** `EURUSD-REVRANGE-M15-001` ya corre sobre `EURUSD_dukas_M1_UTCPlus02` / MetaTrader5 (netted) / M15.
2. **Criterios de aceptación del Build: se mantienen los 3 que ya tenía el usuario** (ProfitFactor>1.3, NumberOfTrades≥100, ReturnDDRatio>1.5) — decisión explícita del usuario de no bajar la vara de calidad. Quedaron justificados por criterio en la ficha (sección 15). No se tocan.
3. **SL/PT: se mantiene el período de ATR como rango (15-30), no se fija** — corrección de criterio del 2026-07-07, ver `calibracion-motor-genetico.md` punto 2.
4. Ficha completada con sección "0_resumen_ejecutivo" (explicación narrativa del diseño completo).
5. **Nueva evidencia real incorporada:** el usuario aportó 2 estrategias EURUSD (H1, MT4) propias que pasaron Retest, ubicadas en `D:\Ariel De Armas\Forex\EURUSD\ResultadosParciales\` (fuera de la instalación). Ambas usan **una sola condición de entrada** (Bollinger Bands), no una pila de capas — señal real de que forzar varias condiciones simultáneas reduce la probabilidad de que el motor encuentre algo viable. Documentado en `.claude/rules/patrones-validados-por-retest.md`. También se actualizó el plan de Retest de la ficha (sección 18) con el pipeline real de 4 pruebas que se usó en esos casos (Monte Carlo Manipulation, Monte Carlo Retest, CrossCheck de mayor precisión, perfil de sensibilidad de optimización). **No se tocó el diseño de `EURUSD-REVRANGE-M15-001` a partir de esto** — se evalúa primero el resultado de la corrida en curso; si vuelve a fallar, simplificar el número de condiciones de entrada pasa a ser la primera opción a considerar, con esta evidencia como respaldo.
6. **Hallazgo adicional mientras corría la prueba:** los Building Blocks de entrada están bien acotados (solo ADX/Bollinger/RSI, coincide con la ficha), pero los de SL/PT tienen ~29 mecanismos habilitados (Keltner, Ichimoku, ParabolicSAR, Linear Regression, medias móviles varias, etc.) cuando la ficha especifica solo ATR. Causa candidata adicional para diluir la convergencia — solución preparada (dejar solo ATR/MTATR) en `calibracion-motor-genetico.md`, **no aplicada todavía**, se evalúa después de ver el resultado de esta corrida.
7. **Segunda corrida — interrumpida sin querer a los 50 min (25.248 estrategias, 0 aceptadas).** Desglose casi idéntico al de la corrida rota en GBPUSD/MT4 (43.29% sin transacciones vs 43.2% antes) — confirma que el problema no era el instrumento, es estructural: exigir mínimo 2 condiciones simultáneas de entrada. Coincide con la evidencia de estrategias reales (`patrones-validados-por-retest.md`) que usan una sola condición.
8. **Ajuste aplicado (verificado por diff del `project.cfx`, 2026-07-07 03:07):** `minConditions` bajó de 2 a 1 en `RulesComplexity` (`maxConditions` sigue en 3).
9. **Tercera corrida completa (minConditions=1):** 276.993 estrategias, 0 aceptadas, 8h12min. Mejoró "sin transacciones" (43.29%→31.7%) pero empeoró "cierra en la misma barra" (14.79%→27.27%).
10. **Segundo ajuste aplicado (2026-07-07, con la app cerrada, editado directamente y verificado):** restringidos los Building Blocks de SL/PT a solo ATR/MTATR (desactivados ~26 mecanismos alternativos: Keltner, Ichimoku, ParabolicSAR, medias móviles, VWAP, SuperTrend, Fibo, Pivots, niveles de sesión, etc.).
11. **Error al intentar correr:** "You use Enter at Stop or Limit, you have to choose some Price Level blocks!" — desactivar TODOS los Price Levels rompió la construcción del precio de la entrada Limit (esa categoría también se usa para eso, no solo para SL/PT). **Corregido:** reactivadas las 6 referencias básicas no-indicador (Ask/Bid/Open/High/Low/Close), manteniendo apagados los mecanismos basados en indicador. Verificado por reextracción: símbolo/motor/minConditions sin cambios. Backups sucesivos en `user/projects/EURUSD-REVRANGE-M15-001/_backups/`. **Pendiente: el usuario reabre y corre de nuevo.**
11b. **Cuarta corrida completa (SL/PT restringido a ATR):** 106.593 estrategias, 0 aceptadas, 3h2min. "Cierra en la misma barra" empeoró a 30.16% (de 27.27%) — la restricción de Building Blocks no ayudó, se mantiene sin revertir por falta de evidencia de daño real.
11c. **Quinta corrida completa (testPrecision=2):** 58.013 estrategias, 0 aceptadas, 3h52min. "Cierra en la misma barra" empeoró a 35.16% (de 30.16%) — **la hipótesis de testPrecision no se confirmó**; en cambio, confirmó que el problema es real (no un artefacto de medición): "transacciones ambiguas" casi desapareció (0.9%→0.02%).
11d. **Diagnóstico pivotado:** el SL mínimo (1.5x ATR) es probablemente chico frente a la variabilidad normal de una sola vela M15 (ATR es un promedio; muchas velas superan ese promedio). Candidato: subir el multiplicador mínimo de SL a ~3x ATR. **No aplicado — pendiente de discutir con el usuario.**
11g. **Séptima corrida (rápida, pop=20/gen=15):** 7.004 estrategias, 0 aceptadas, 42min. El SL/PT ensanchado **sí funcionó** — "cierra en la misma barra" bajó de 35.16% a 1.86%. Pero apareció un problema nuevo: 57.11% rechazadas por Profit Factor≤1.30 (el objetivo de ganancia 4x-10x ATR probablemente es demasiado ambicioso para una reversión dentro de rango).
11i. **Requisito registrado, pendiente de validar** (`.claude/rules/pipeline-multitarea-y-diseno-is.md`): cuando la plantilla logre su primera convergencia real, armar el pipeline completo (Build→Retest OOS→Retest cross-market→Walk-Forward Matrix→limpieza→loop) clonado de un proyecto de ejemplo real, con IS/OOS consciente de los regímenes históricos de EURUSD. No aplicado todavía.
11h. **PIVOTE A H1 (2026-07-07):** tras 7 corridas en M15 sin ninguna aceptada, se confirmó que toda la evidencia real disponible (2 estrategias propias + 4 proyectos de ejemplo oficiales) que converge es H1/M30, nunca M15. Se pivotó: timeframe M15→H1, SL/PT revertido a valores de referencia H1 (1.5x-3x SL, 2x-5x PT), Building Blocks de SL/PT reabiertos (ya no restringidos a ATR), población restaurada a 100/50. **El proyecto real fue renombrado** de `user/projects/EURUSD-REVRANGE-M15-001` a `user/projects/EURUSD-REVRANGE-H1-001` (config.xml y carpeta), y el catálogo/ficha de SQX_Library actualizados (`EURUSD-REVRANGE-H1-001`, `id_anterior` conservado). Objetivo: conseguir la primera convergencia real para validar el pipeline completo, y reconsiderar volver a M15 con esa experiencia. **Pendiente: el usuario corre el proyecto renombrado.**
11f. **Sexto ajuste aplicado (2026-07-07, confirmado por el usuario, app cerrada, verificado):** multiplicadores de SL/PT duplicados proporcionalmente — SL 1.5x-2.5x→3x-5x ATR, PT 2x-5x→4x-10x ATR (mismo ratio riesgo/beneficio, mayor distancia absoluta). Backup en `user/projects/EURUSD-REVRANGE-M15-001/_backups/project_2026-07-07_antes_de_ampliar_SLPT.cfx`. **Pendiente: correr de nuevo.**
11e. **Corrección de alcance importante:** el usuario aclaró que las 2 estrategias reales aportadas antes no reflejaban diseño intencional (búsqueda sin hipótesis del usuario) — se retiró la conclusión "simplificar entrada" basada en esos ejemplos. El rango `minConditions=1/maxConditions=3` se mantiene, ahora respaldado por documentación oficial de StrategyQuant (recomienda 1-2) y práctica general de sobreajuste, no por esos ejemplos. Ver `patrones-validados-por-retest.md` corregido.
12. **Indicadores nuevos disponibles** (no usados todavía en esta plantilla): `ChoppinessIndex` y `EfficiencyRatio`, implementados y compilando sin errores — ver `_indicadores/indicadores_propuestos.json`.
3. Revisar juntos el resultado de esa corrida: si vuelve a dar 0 o casi 0 aceptadas, aplicar el orden de diagnóstico de `calibracion-motor-genetico.md` antes de tocar la hipótesis.
4. Si sale bien, generar los artefactos `.sqx` (Restrictiva y Flexible) y armar el plan de Retest (sección 18 de la ficha).
5. Usar los proyectos de ejemplo de la instalación (breakouts en GBPJPY/GOLD/NQ/GBPUSD) como cantera de aprendizaje para futuras plantillas nuevas — patrones de RulesComplexity, SL/PT y pipeline de Retest ya extraídos en `calibracion-motor-genetico.md` y `configuracion-money-management.md`.

---

## Historial de sesiones

### 2026-07-06 — Adopción formal del proyecto y creación del sistema de continuidad
- El usuario formalizó `SQX_Library` como el proyecto base de desarrollo a retomar en adelante.
- Se creó este archivo (`_estado/estado_proyecto.md`) y se enganchó como Paso 0 obligatorio en `CLAUDE.md`, más un Paso de cierre que lo actualiza.
- Se revisó todo el sistema existente (CLAUDE.md, las 7 reglas de `.claude/rules/`, catálogo, ficha e indicadores) sin encontrar inconsistencias nuevas más allá de lo ya documentado en `_registro-cambios.md`.
- Estado de `EURUSD-REVRANGE-M15-001` verificado: sigue en `en_construccion_full_builder`, sin cambios desde el 2026-07-04.

### 2026-07-07 — Corrección de contexto y aclaración del bloqueo real
- El usuario confirmó que el "caso EURUSD H1" citado como precedente en el changelog de `ReversionRango` **nunca existió**. Corregido en `EURUSD/ReversionRango/changelog.md`.
- El usuario aclaró un punto más importante: **hasta ahora apenas se ha logrado crear algo compatible y funcional en el Full Builder.** Esto redefine la prioridad real del proyecto — no es "generar más plantillas", sino resolver por qué la construcción en StrategyQuant X no está llegando a un resultado funcional. Pendiente pedir el detalle concreto (qué falla exactamente) en la próxima sesión.

### 2026-07-07 (continuación) — Diagnóstico real, recalibración y ampliación del protocolo
- Se investigó a fondo la instalación real (`internal/ctemplate/wizard.xml`, `talibs.xml`, `conditions.xml`, `moneyManagement.xml`, y el `Build-Task1.xml` real del proyecto) para entender de primera mano las capacidades y límites del Full Builder y AlgoWizard. Confirmado con evidencia: AlgoWizard tiene solo 10 bloques básicos y ningún mecanismo de código propio; el Full Builder expone 158 indicadores/patrones/funciones TA-Lib reales y permite código MQL propio vía `CustomAction`. Ambos hallazgos ya estaban bien capturados en `formato-entrega.md` y `arquitectura-5-capas.md` — se confirmaron, no se corrigieron.
- El usuario confirmó que `EURUSD-REVRANGE-M15-001` es el único proyecto real propio que no salió bien; el resto de proyectos en `user/projects/` son ejemplos de la instalación, útiles como referencia de calibración.
- Se leyó el log real de la corrida fallida y se comparó contra 4 proyectos de ejemplo reales que sí producen resultados. Diagnóstico: población/generaciones genéticas demasiado chicas (15/10 vs. 100/100 de referencia) + criterios de aceptación demasiado exigentes ya en el Build (4 condiciones simultáneas vs. 1) + período de ATR explorado como rango en vez de fijo.
- Se crearon dos reglas nuevas (`calibracion-motor-genetico.md`, `configuracion-money-management.md`) y se amplió `formato-entrega.md`: la ficha de diseño ahora es autosuficiente, cubre toda la configuración del Builder (RulesComplexity, genética, SL/PT, criterios de aceptación por etapa, Trading Options, Money Management, plan de Retest/Optimizer), no solo el diseño conceptual. Este cambio aplica a todas las plantillas futuras.
- `ficha_diseño.json` de `EURUSD-REVRANGE-M15-001` recalibrada con las secciones 12-19 nuevas. Estado: `recalibrada_pendiente_nueva_corrida`.
- El usuario reabrió el proyecto en el Full Builder para aplicar el cambio de símbolo/motor (GBPUSD/MT4 → EURUSD/MT5). Reportó que **el símbolo y el motor siguen siendo los mismos**. El usuario cerró el programa preguntando por un mensaje de "copia de seguridad" al cerrar — se verificó directamente: es el guardado normal de StrategyQuant al salir (log: `"Save all strategies before exiting SQX"`, sincroniza databanks y reescribe `project.cfx`), no una copia de seguridad aparte con fecha. Se confirmó además, releyendo el `project.cfx` recién guardado, que **el símbolo/motor efectivamente siguen sin cambiar** (`GBPUSD_M1_dukas`/`MetaTrader4`) — no se perdió ningún cambio, simplemente todavía no se aplicó ninguno.
- **Corregido y verificado (2026-07-07):** el usuario cambió símbolo y motor en el Full Builder y guardó. Se confirmó comparando hash/contenido del `project.cfx` antes y después: `GBPUSD_M1_dukas`/`MetaTrader4` → **`EURUSD_dukas_M1_UTCPlus02`/`MetaTrader5 (netted)`**, M15 sin cambios. El Setup del CrossCheck también quedó en MetaTrader5. El problema de símbolo/motor equivocado queda resuelto.
