# Changelog — EURUSD-REVRANGE-M15-001

## 2026-07-04 — Creación de la ficha de diseño
- Activo: EURUSD (familia `FX_mayor_liquido`)
- Hipótesis: Reversión a la media en rango
- Timeframe: M15
- Motor: MetaTrader 5
- Régimen histórico: se usa todo el histórico disponible, sin exclusiones (confirmado explícitamente por el usuario)
- Trading Options restringido por defecto (preferencia operativa estándar del usuario)
- Ficha de diseño aprobada por el usuario. **Pendiente:** generación del artefacto ejecutable `.sqx` (versiones Restrictiva y Flexible) — no se genera hasta solicitud explícita ("créala en el Builder" / "genera el archivo"), y sujeta a la verificación de compatibilidad AlgoWizard (Paso 8) antes de la entrega.
- Registrado en `indice_maestro.json`.

## 2026-07-04 — Cambio de régimen histórico durante la construcción en Full Builder
- **Se detectó una desviación** respecto a lo acordado en la ficha original: se configuró un IS de 8 años en el Builder en vez de todo el histórico disponible.
- **Motivo confirmado por el usuario:** los datos más antiguos ya no representan el comportamiento actual del mercado.
- **Decisión:** se mantiene el recorte a 8 años, documentado explícitamente (no es un descuido, es una decisión deliberada).
- `ficha_diseño.json` actualizada en el campo `metadata.regimen_historico_valido`.
- Estado de la plantilla actualizado a `en_construccion_full_builder` (el usuario está construyendo el proyecto directamente en el Full Builder, guiado por el manual de construcción).

## 2026-07-07 — Corrección de referencia inexistente
- Se eliminaron dos referencias a "otros casos de EURUSD H1" como precedente (entradas del 2026-07-04). **Confirmado explícitamente por el usuario: ese caso nunca existió.** No hay ninguna plantilla EURUSD H1 en la biblioteca ni la hubo antes.
- Contexto real aportado por el usuario: hasta la fecha, apenas se ha logrado crear algo compatible y funcional en el Full Builder — no solo falta generar el `.sqx` de esta plantilla, sino que la construcción en el Builder viene resultando difícil de llevar a buen puerto en general (ver `_estado/estado_proyecto.md` para el detalle y las preguntas abiertas al respecto).

## 2026-07-07 — Diagnóstico real del "0 estrategias aceptadas" y recalibración
- Se leyó el log real de la corrida del 2026-07-05 (`log/global_log_20260705_184206.log`): 107.446 estrategias generadas, 0 aceptadas, en 2h54min. Desglose: 43.2% sin transacciones, 27.6% filtradas por Profit Factor inicial, 23.6% cerrando en la misma barra, 5.1% muy pocas transacciones.
- Se comparó la configuración real (`PopulationSize=15`, `MaxGenerations=10`, 4 condiciones de aceptación simultáneas en el Build, período ATR explorable 10-30) contra 4 proyectos de ejemplo reales de la instalación que sí producen resultados (`GBPJPY BREAKOUT H1`, `GOLD BREAKOUT M30`, `NQ CFD H1`, `GBPUSD H1` — todos usan `PopulationSize=100`, `MaxGenerations=100`, un solo filtro de aceptación en el Build, período ATR fijo).
- **Diagnóstico:** la causa no es la hipótesis ni la arquitectura de 5 capas — es una combinación de búsqueda genética demasiado chica y criterios de aceptación demasiado exigentes ya en el Build. Ver `.claude/rules/calibracion-motor-genetico.md` (nueva regla creada con esta evidencia).
- **`ficha_diseño.json` actualizada** con secciones 12-19 (config. completa del Builder): PopulationSize/MaxGenerations→100/100, período ATR fijo en 20, criterios de aceptación diferidos a Retest (solo ProfitFactor>1 en el Build), Money Management cambiado de `FixedAmount` a `FixedSize`/0.1/10000 (estándar de la biblioteca, ver `.claude/rules/configuracion-money-management.md` — la corrida real usaba `FixedAmount`, RiskedMoney=50, sin justificación documentada, lo que rompía la comparabilidad con el resto de plantillas).
- **Pendiente:** aplicar estos valores en el Full Builder y correr de nuevo. Estado de la plantilla actualizado a `recalibrada_pendiente_nueva_corrida`.

## 2026-07-07 — Segunda corrida (interrumpida) y simplificación de RulesComplexity
- Corrida con símbolo/motor ya corregidos (EURUSD/MT5): se cortó a los 50 min (25.248 estrategias, 0 aceptadas) — el usuario cerró StrategyQuant sin querer. Desglose: 43.29% sin transacciones, 36.6% Profit Factor≤1.30, 14.79% cierra en la misma barra (mejoró del 23.6% con GBPUSD), 3.97% muy pocas transacciones.
- **Hallazgo clave:** el 43.29% "sin transacciones" es casi idéntico al 43.2% de la corrida rota en GBPUSD/MT4 — señal de que el problema no es el instrumento, es estructural: exigir mínimo 2 condiciones simultáneas de entrada (`minConditions=2`) es difícil de satisfacer con ADX+Bollinger+RSI. Coincide con la evidencia de `patrones-validados-por-retest.md` (estrategias reales exitosas usan una sola condición).
- El usuario bajó `minConditions` de 2 a 1 en el Builder (verificado por diff del `project.cfx`, `maxConditions` sigue en 3). Pendiente: correr de nuevo con este ajuste.

## 2026-07-07 — Tercera corrida (minConditions=1) y restricción de Building Blocks de SL/PT
- Corrida completa con `minConditions=1`: 276.993 estrategias generadas, 0 aceptadas, 8h12min. Desglose: 31.7% sin transacciones (mejoró del 43.29%), 36.23% Profit Factor≤1.30, **27.27% cierra en la misma barra (empeoró del 14.79%)**, 3.61% muy pocas transacciones.
- **Diagnóstico:** bajar `minConditions` ayudó a que más estrategias operen, pero las entradas más sueltas (una sola condición) disparan en momentos más ruidosos, y el SL salta en la misma vela más seguido — coincide con el hallazgo ya documentado (Building Blocks de SL/PT sin acotar, ~29 mecanismos habilitados además de ATR).
- **Acción aplicada:** con la aplicación cerrada (confirmado por el usuario y por el log de salida), se editó `project.cfx` directamente — se desactivaron (`use=false`) los ~26 mecanismos de `Stop/Limit Price Levels.*` y `Stop/Limit Price Ranges.*` que no son ATR (Keltner, Ichimoku, ParabolicSAR, medias móviles, VWAP, SuperTrend, Fibo, Pivots, niveles de sesión, etc.), dejando únicamente `Stop/Limit Price Ranges.ATR` y `.MTATR` habilitados. Verificado por reextracción del zip: símbolo/motor/minConditions se conservaron sin cambios, solo se tocaron los bloques de SL/PT.
- Backup del `project.cfx` previo a este cambio guardado en `user/projects/EURUSD-REVRANGE-M15-001/_backups/project_2026-07-07_antes_de_restringir_SLPT.cfx`.
- **Error al intentar correr:** "You use Enter at Stop or Limit, you have to choose some Price Level blocks!" — al desactivar TODOS los `Stop/Limit Price Levels`, se rompió la construcción del precio de la orden Limit de entrada (esa categoría no es exclusiva del SL/PT). Se reactivaron las 6 referencias básicas no-indicador (`Ask`, `Bid`, `Open`, `High`, `Low`, `Close`), manteniendo apagados los mecanismos basados en indicador. Verificado por reextracción: símbolo/motor/minConditions sin cambios, solo esos 6 bloques reactivados.
- **Pendiente:** el usuario reabre el proyecto y corre de nuevo con esta configuración.

## 2026-07-07 — Cuarta corrida (Building Blocks SL/PT restringidos) y ajuste de testPrecision
- Corrida completa: 106.593 estrategias, 0 aceptadas, 3h2min (parada manualmente por el usuario). Desglose: 35.9% sin transacciones, **30.16% cierra en la misma barra (empeoró del 27.27%)**, 29.27% Profit Factor≤1.30.
- **La restricción de Building Blocks de SL/PT a solo ATR no ayudó** — si acaso empeoró levemente "cierra en la misma barra" (posible ruido entre corridas, no se revierte por ahora sin más evidencia).
- Se investigó y confirmó con documentación oficial de StrategyQuant que el modo de precisión rápido (`testPrecision=1`) es explícitamente insuficiente para órdenes Stop/Limit — exactamente el caso de esta plantilla. Ver `.claude/rules/calibracion-motor-genetico.md`.
- **Aplicado:** `testPrecision` subido de 1 a 2 en ambos Setups (principal y CrossCheck), con la aplicación cerrada, verificado por reextracción. Backup previo en `user/projects/EURUSD-REVRANGE-M15-001/_backups/project_2026-07-07_antes_de_testPrecision2.cfx`.
- **Pendiente:** correr de nuevo con esta configuración (será más lento, usa datos M1 para simular movimiento intrabarra).

## 2026-07-07 — Quinta corrida (testPrecision=2) y pivote del diagnóstico
- Corrida completa: 58.013 estrategias, 0 aceptadas, 3h52min (parada manualmente). Desglose: 35.79% sin transacciones, **35.16% cierra en la misma barra (empeoró del 30.16%)**, 24.9% Profit Factor≤1.30, 0.02% transacciones ambiguas (bajó drásticamente del 0.9%).
- **La hipótesis de testPrecision no se confirmó como solución.** Al contrario: con más precisión, el motor confirma con más certeza que el SL/TP genuinamente se toca dentro de la misma vela M15 — no era un artefacto de medición de baja precisión (de hecho las "ambiguas" casi desaparecieron, señal de que la medición ahora es correcta).
- **Diagnóstico pivotado:** la causa mecánica más probable es que el SL mínimo (1.5x ATR) es matemáticamente chico frente a la variabilidad normal de una sola vela M15 — ATR es un promedio, muchas velas individuales superan ese promedio. Ver `.claude/rules/calibracion-motor-genetico.md` para el razonamiento completo.
- **Corrección importante de alcance:** el usuario aclaró que las 2 estrategias reales de ejemplo aportadas antes no reflejaban diseño intencional (búsqueda sin hipótesis definida) — se retiró la conclusión de "simplificar entrada" basada en esos ejemplos. El rango minConditions=1/maxConditions=3 se mantiene, respaldado en cambio por documentación oficial de StrategyQuant y práctica general de trading algorítmico (ver `patrones-validados-por-retest.md`, sección corregida).
- **Pendiente:** discutir con el usuario si se sube el multiplicador mínimo de SL por ATR (de 1.5x a ~3x) antes de la próxima corrida.

## 2026-07-07 — Sexto ajuste: SL/PT ampliado proporcionalmente
- El usuario confirmó aplicar el ajuste. Se duplicaron proporcionalmente ambos multiplicadores (no solo el SL, para no romper el ratio riesgo/beneficio): SL de 1.5x-2.5x a **3x-5x** ATR; PT de 2x-5x a **4x-10x** ATR. Período de ATR (15-30) y el resto de la configuración sin cambios.
- Aplicado con la app cerrada (confirmado por log de salida a las 20:13:36), verificado por reextracción del zip: símbolo/motor/testPrecision/minConditions se conservaron.
- Backup previo en `user/projects/EURUSD-REVRANGE-M15-001/_backups/project_2026-07-07_antes_de_ampliar_SLPT.cfx`.
- **Pendiente:** el usuario corre de nuevo con esta configuración.

## 2026-07-07 — Corrida rápida de diagnóstico (población/generaciones reducidas)
- El proceso de probar una variable por corrida completa (1-8 horas cada una) resultó demasiado lento para iterar. Se acordó con el usuario una corrida rápida de diagnóstico: `PopulationSize` 100→20, `MaxGenerations` 50→15 (resto de la configuración sin cambios: símbolo/motor, `minConditions=1`, SL/PT ampliado 3x-5x/4x-10x, `testPrecision=2`). Objetivo: confirmar en minutos si el SL/PT ampliado alcanza para que aparezca al menos una estrategia aceptada, antes de comprometer otra corrida larga con población completa.
- Aplicado con la app cerrada, verificado por reextracción. Backup en `user/projects/EURUSD-REVRANGE-M15-001/_backups/project_2026-07-07_antes_de_corrida_rapida_pop20gen15.cfx`.
- **Importante:** esta corrida es solo para diagnóstico rápido — si da algo aceptable, se corre después con población completa (100/50) para buscar la mejor versión real, no se toma esta corrida chica como resultado final.

## 2026-07-07 — Séptima corrida (rápida, pop=20/gen=15) — el SL/PT ensanchado funcionó, pero aparece un problema nuevo
- 7.004 estrategias, 0 aceptadas, 42min. Desglose: 36.01% sin transacciones, **1.86% cierra en la misma barra (bajó de 35.16% — el ensanchamiento de SL/PT resolvió esto casi por completo)**, **57.11% Profit Factor≤1.30 (empeoró mucho, antes 25-37%)**.
- **Lección importante y aprendizaje real sobre el proceso:** reducir población/generaciones tampoco resultó "rápido" en la práctica (42 min, no minutos) — confirma lo que ya habíamos anticipado: la tasa de rechazo alta hace que el motor tenga que probar muchos candidatos igual para juntar una población chica.
- **Diagnóstico:** el SL/PT ensanchado (3x-5x SL, 4x-10x PT) resolvió el problema de "misma barra", pero probablemente el objetivo de ganancia (4x-10x ATR) es demasiado ambicioso para una hipótesis de reversión dentro de un rango — el precio revierte hacia la media, no necesariamente recorre 4-10 ATR completos. Esto explica el salto en rechazos por Profit Factor bajo.
- **Decisión:** en vez de seguir ajustando SL/PT en M15, se pivota a probar la misma hipótesis en **H1** (ver entrada siguiente) — toda la evidencia real disponible (2 estrategias propias + 4 proyectos de ejemplo oficiales) que efectivamente converge es H1/M30, nunca M15. El objetivo es conseguir la primera convergencia real para validar todo el pipeline, y después volver a acotar hacia M15 con esa experiencia.

## 2026-07-07 — Corrección: la comparación anterior usó el proyecto equivocado
- Se detectó que la entrada anterior de hoy comparó `EURUSD-REVRANGE-M15-001` contra el `Build-Task1.xml` del proyecto genérico `user/projects/Builder/` (una tarea vieja de "mejora" de una instalación StrategyQuant 4 distinta, no relacionada) en vez de contra su propio `project.cfx`. Se verificó el archivo real.
- **Datos reales corregidos:** `PopulationSize=100` (no 15), `MaxGenerations=50` (no 10) — ya cerca de la referencia, no es el problema principal. Money Management ya es `FixedSize`/0.1/10000 (no `FixedAmount`) — ya cumplía el estándar, no había desviación. Criterios de aceptación en el Build: 3 condiciones simultáneas (ProfitFactor>1.3, Trades≥100, ReturnDDRatio>1.5), no 4.
- **Hallazgo confirmado por el usuario directamente en la interfaz del Full Builder:** la pestaña "What to build" muestra "Simple strategy" — el modo "mejorar estrategia existente" que sugería el XML **no está activo**, se descarta esa hipótesis. Pero sí se confirmó el otro problema: en la pestaña de datos, el símbolo es `GBPUSD_M1_dukas` (no EURUSD) y el motor es MetaTrader4 (no MetaTrader5); la temporalidad M15 es correcta.
- **Esto invalida el resultado del log del 2026-07-05 como evidencia sobre la hipótesis EURUSD M15**: el Build corrió sobre GBPUSD/MT4. Antes de recalibrar población/criterios/ATR, hay que corregir el símbolo y el motor del Setup principal a EURUSD/MetaTrader5. `ficha_diseño.json` sección 20 actualizada con el detalle confirmado.
- `.claude/rules/calibracion-motor-genetico.md` actualizada — se descarta la hipótesis del modo "mejora" y se deja solo el hallazgo confirmado de símbolo/motor.

## 2026-07-08 — Octava corrida (H1, población 100/50) y relajación de Profit Factor
- Corrida H1 con población completa: tras ~2h04min sin ninguna estrategia aceptada, con Profit Factor≤1.30 como único filtro relevante (patrón sano, ~53% de rechazo), se cumplió el criterio de espera prudente documentado en `calibracion-motor-genetico.md` (benchmark real: 20-35 aceptadas/hora es lo normal en pares FX mayores a H1 con configuración general).
- El usuario aplicó el ajuste directamente en el Builder: `ProfitFactor` de 1.3 a **1.1** (verificado en el archivo: `NumberOfTrades≥100` y `ReturnDDRatio>1.5` sin cambios). Nueva corrida arrancada a las 00:34.
- Nota: se mantiene la política de no bajar a un filtro trivial (ver `calibracion-motor-genetico.md` punto 1) — 1.1 sigue siendo un piso por encima de breakeven, no el "PF>1" de los ejemplos oficiales.

## 2026-07-08 — Novena corrida (H1, PF Build=1.1) y ajuste del filtro de Ranking
- Corrida completa: 124.143 estrategias, 0 aceptadas, 9h23min. Desglose: 47.14% Profit Factor≤1.10 (Build), 29.12% sin transacciones, **9.65% Profit Factor<2.00 (filtro de Ranking, más estricto)**, 6.87% cierra en la misma barra (mejoró mucho vs M15), 4.11% muy pocas transacciones, 0.96% NumberOfTrades<200 (Ranking).
- **Hallazgo clave:** ~10.6% de las estrategias llegaron hasta el filtro final de Ranking (pasaron todos los filtros mecánicos y el Profit Factor del Build) pero ninguna superó simultáneamente ProfitFactor≥2.00 y NumberOfTrades≥200 — es el primer diagnóstico donde una fracción significativa de candidatos llega "casi hasta el final", no solo fallan mecánicamente al principio.
- **Aplicado:** el filtro de Ranking `ProfitFactor≥2.00` bajado a **1.5** (con la app cerrada, verificado por reextracción). `NumberOfTrades≥200` y `ReturnDDRatio>3` del Ranking sin cambios; Profit Factor del Build sigue en 1.1. Backup en `user/projects/EURUSD-REVRANGE-H1-001/_backups/project_2026-07-08_antes_de_ranking_PF15.cfx`.
- **Pendiente:** correr de nuevo con esta configuración.

## 2026-07-08 — DÉCIMA CORRIDA: primera convergencia real (4 estrategias aceptadas)
- **Hito:** primera vez que esta plantilla produce estrategias aceptadas, tras 9 corridas previas en 0 (M15 y H1 combinadas).
- Corrida H1 con Ranking ProfitFactor relajado a 1.5: 101.080 estrategias generadas, **4 aceptadas**, 7h42min. Tiempo por estrategia aceptada: 1h55min. Tasa: 0.52 aceptadas/hora (muy por debajo del benchmark de referencia de 20-35/hora para pares FX mayores en M15-H4 — ver `calibracion-motor-genetico.md`).
- Desglose: 47.61% Profit Factor≤1.10 (Build), 29.2% sin transacciones, 9.06% Profit Factor<1.50 (Ranking), 7.12% cierra en la misma barra, 4.12% muy pocas transacciones.
- El usuario observó en pantalla que la curva de equity de las candidatas "no es muy bonita" — consistente con lo esperado: pasar los filtros numéricos del Build no garantiza calidad visual/real de la curva, es exactamente lo que Retest/Monte Carlo deben exponer a continuación.
- **Pendiente:** revisar las 4 candidatas en detalle (QuantAnalyzer/Databank) antes de decidir cuál(es) avanzan a Retest.

## 2026-07-08 — Ampliación temporal del pool de indicadores (Stochastic)
- El usuario pidió revisar críticamente si estábamos limitando demasiado al Builder. Se auditó el archivo real: 79 de 509 building blocks activos, pero se confirmó que esto refleja el diseño intencional (solo ADX/Bollinger/RSI como indicadores de entrada, ~20 comparadores genéricos, 55 niveles + 6 rangos de SL/PT reabiertos para H1) — no es una restricción accidental.
- **Decisión:** ampliar temporalmente el pool con **Stochastic** (mismo tipo lógico que RSI — oscilador de sobrecompra/sobreventa, pero mide posición relativa en el rango High-Low reciente, no redundante) para dar más material a la búsqueda genética y converger más rápido en pruebas, sin abandonar la hipótesis de reversión en rango. Marcado explícitamente como ampliación de prueba, no cambio definitivo — se puede revertir si no aporta.
- Aplicado con la app cerrada (confirmado por log), verificado por reextracción: activados `Indicators.Stochastic` + 12 patrones asociados (StochFastK*/StochSlowD*). Backup en `user/projects/EURUSD-REVRANGE-H1-001/_backups/project_2026-07-08_antes_de_agregar_stochastic.cfx`.
- **Pendiente:** correr de nuevo con esta configuración y comparar tasa de convergencia contra la corrida anterior (0.52 aceptadas/hora).

## 2026-07-09 — Onceava corrida (Stochastic, 10h10min) y organización de candidatas
- Corrida con pool ampliado (ADX+Bollinger+RSI+Stochastic): 133.095 estrategias, **8 aceptadas**, 10h10min. Tasa: **0.79 aceptadas/hora** (mejoró de 0.52/hora sin Stochastic). El cross-check `RetestWithHigherPrecision` descartó 1 candidata adicional por exceso de Drawdown real — confirma que cumple su función, no es solo overhead.
- Se extrajeron y calcularon estadísticas reales (PF IS/OOS) de las 8 vía `sqcli -tools action=orderstocsv` + cálculo propio, mismo método que las 4 anteriores. Resultado: 3 de 8 pierden en OOS (4.49.114 PF OOS 0.80, 4.38.108 PF OOS 0.89, 4.50.197 PF OOS 0.90), 5 de 8 positivas en OOS, destacando **2.3.109 con PF OOS 1.54 — la mejor candidata obtenida hasta ahora**.
- **Total acumulado: 12 candidatas** (4 de la corrida sin Stochastic + 8 con Stochastic). De las 12, **8 son OOS-positivas y 4 se descartan** por PF OOS<1 (patrón de sobreajuste consistente, ~25-33% de las aceptadas del Build en ambas corridas).
- Organizadas en `EURUSD/ReversionRango/candidatas_build/`, renombradas y ordenadas por PF OOS descendente (`01_PFOOS-1.54_...` a `08_PFOOS-1.04_...`), con las 4 descartadas movidas a `candidatas_build/descartadas_pfoos_menor_1/`.
- **Aplicado:** `testPrecision` del Setup principal bajado de 2 a 1 (con la app cerrada, verificado por reextracción) — el cross-check `RetestWithHigherPrecision` (Precision=2, ya activo) queda como red de seguridad para verificar solo a los finalistas antes de la aceptación final, evitando pagar el costo de precisión completa en las ~100k+ candidatas descartadas. Backup en `_backups/project_2026-07-09_antes_de_testPrecision1.cfx`.
- **Pendiente:** correr de nuevo con testPrecision=1 y comparar velocidad/calidad contra las dos corridas anteriores.

## 2026-07-09 — Descarte de candidatas con PF OOS<1 ratificado por el usuario
- Tras discutir si la correlación en portafolio podría "salvar" a las 4 candidatas con Profit Factor OOS negativo (0.80-0.96), se concluyó con criterio crítico que la diversificación reduce riesgo/volatilidad pero no convierte una expectativa negativa en positiva — el principio de portafolio no correlacionado aplica a estrategias levemente rentables o neutras, no a estrategias con edge genuinamente negativo en datos no vistos.

## 2026-07-12 — Duodécima corrida (testPrecision=1): databank lleno a 1000 y backup en bruto
- Corrida iniciada 2026-07-09 09:54:33, finalizada 2026-07-12 02:02:08 (2 días 16 hrs). Log: `log/global_log_20260709_085433.log`.
- 4.013.428 estrategias generadas, 1080 aceptadas brutas (1000 en databank tras `Rankings type="never"` con 40 sustituidas por mejores y 40 descartadas por similitud — `StopCondition type="databank-full" passedStrategies="1000"` disparó la parada automática, sin intervención). 62.591 estrategias/hora probadas, 16.84 aceptadas/hora.
- Desglose de rechazo principal: Profit Factor<1.50 en población inicial (20.68%), sin transacciones (18.75%), demasiadas operaciones cerrando en la misma barra (4.38%), muy pocas transacciones (5.19%), Ret/DD≤3.00 en filtro global (0.0%, solo 14 casos).
- **Backup de seguridad de las 1000 en bruto** (previo a cualquier filtrado/CustomAnalysis/pipeline nuevo): exportadas por el usuario vía interfaz de SQ a `D:\Ariel De Armas\Forex\EURUSD\Plantillas\Reversion-Media\`, movidas por el asistente a la subcarpeta `_backup_1000_bruto_2026-07-12\` (verificado: 1000 archivos `.sqx`, 0 `.csv`, carpeta raíz limpia para futuras candidatas curadas). Esta carpeta queda reservada para el resultado final curado con el esquema de nombres de `formato-entrega.md` — el backup en bruto es material sin evaluar, no debe confundirse con candidatas ya validadas.
- **Pendiente:** con las 1000 a salvo, se procede a ensamblar el pipeline multi-tarea (Filtering → CustomAnalysis OOS → Retest → Portfolio → SaveToFiles → LogDatabankStats → Notification → ClearDatabanks → GoToTask) sobre el databank actual, usando `active="false"` en la tarea de Build (ya cumplió su función) mientras se prueba el resto — ver `.claude/rules/pipeline-multitarea-y-diseno-is.md` secciones 3d/3e.
- El usuario ratificó explícitamente el descarte: quedan las 8 candidatas con PF OOS≥1 como universo activo (`candidatas_build/01_...` a `08_...`), las 4 con PF OOS<1 quedan documentadas y separadas en `candidatas_build/descartadas_pfoos_menor_1/`, no se eliminan (valor como registro de qué no funcionó).
