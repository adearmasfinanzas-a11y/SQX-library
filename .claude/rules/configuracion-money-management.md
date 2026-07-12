---
paths: SQX_Library/**
cargar_en: Paso 6 del protocolo (Diseño), sección de Money Management de la ficha
naturaleza: ESTABLE — política de homogenización de riesgo entre plantillas de la biblioteca
---

# Homogenización del riesgo entre plantillas

Cada plantilla se diseña y evalúa de forma aislada, pero la biblioteca crece pensando en que varias plantillas puedan compararse o combinarse en un portafolio. Si cada una usa un método de Money Management distinto durante la investigación (Build/Retest), el Profit Factor, Drawdown y demás métricas dejan de ser comparables entre plantillas — la diferencia de resultados podría deberse al tamaño de posición, no a la calidad real de la lógica.

## Evidencia real de la instalación

Los 4 proyectos de ejemplo reales analizados (`GBPJPY BREAKOUT H1`, `GOLD BREAKOUT M30`, `NQ CFD H1`, `GBPUSD H1`) usan, de forma consistente, en **todas** las fases (Build y todas las tareas de Retest posteriores): `Method type="FixedSize"` con `Size=0.1` lotes e `InitialCapital=10000`. Ninguno usa money management basado en riesgo % durante la investigación.

**Corrección (2026-07-07):** una primera versión de esta nota decía que `EURUSD-REVRANGE-M15-001` se había desviado a `FixedAmount` — eso salió de leer por error el `Build-Task1.xml` del proyecto genérico `Builder` (una tarea vieja no relacionada). Se verificó el `project.cfx` real de `EURUSD-REVRANGE-M15-001`: **ya usa `FixedSize`, `Size=0.1`, `InitialCapital=10000`** — coincide exactamente con el estándar de los ejemplos. No hubo desviación real; esta plantilla ya cumple la regla.

## Regla para nuevas plantillas

**Fase de investigación (Build + todas las tareas de Retest/WalkForward):** usar siempre `FixedSize` con el mismo tamaño (`0.1` lotes, salvo que el activo lo haga inviable — ej. futuros con contrato mínimo distinto, a justificar por escrito) e `InitialCapital=10000`. Esto es lo que hace que Profit Factor, Drawdown y demás métricas sean comparables entre plantillas de activos distintos.

**Fase de portafolio/despliegue real:** la normalización de riesgo real (risk % de cuenta, por ejemplo) se decide en el Portfolio Composer al combinar plantillas para trading en vivo — está fuera del alcance del Build de una plantilla individual y no se mezcla con la fase de investigación.

## Excepción

Si una plantilla concreta necesita un Money Management distinto durante la investigación por una razón estructural del activo o la hipótesis (ej. una hipótesis que depende explícitamente de escalar posición con la señal), se documenta explícitamente en la ficha y en el changelog — no se asume el estándar por defecto en silencio, pero tampoco se cambia sin dejar constancia de por qué.

## Comisiones y spread: broker objetivo, definido ANTES del Build (establecido 2026-07-12)

**Regla dura:** antes de configurar el Build de cualquier plantilla nueva, hay que preguntarle al usuario **a qué broker apunta** — nunca asumir un valor genérico de comisión/spread sin esa conversación previa (ver `CLAUDE.md`, Paso 4). La estructura de costos reales varía mucho entre brokers y afecta directamente si una estrategia es viable o no.

**Patrón recomendado (propuesto por el usuario, 2026-07-12):** el Build puede usar comisión/spread **genéricos pero bien justificados** (razonables para la categoría de broker/activo, documentados con su fuente), y **subir la exigencia en el Retest** como prueba de margen de seguridad — no al revés. La idea: si la estrategia sobrevive con costos moderadamente más altos que los reales esperados, hay más confianza de que no se rompe por un centavo de diferencia con el broker real elegido más adelante.

**Cuánto subir en el Retest — moderado, no arbitrario:** un incremento real (~30-35% sobre el valor del Build) para que la prueba de estrés tenga sentido, pero sin exagerar a un valor poco realista que rompa estrategias por una razón ajena a su calidad real. Ejemplo real aplicado en `EURUSD-REVRANGE-H1-001`: Build usó `Commission SizeBased=6` y `Swap money long=-6.5/short=-3.5`; Retest subido a `Commission SizeBased=8` y `Swap long=-8/short=-5` (~33% de incremento) — a confirmar con el usuario caso por caso, no es un porcentaje fijo automático.
