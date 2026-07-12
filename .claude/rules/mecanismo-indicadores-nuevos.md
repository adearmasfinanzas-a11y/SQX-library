---
paths: SQX_Library/**
cargar_en: Bajo demanda — solo cuando surge la necesidad de un indicador no nativo
naturaleza: CONDICIONAL — no se carga en cada plantilla
---

# Mecanismo de indicadores nuevos / no nativos

Cuando se detecte (por criterio propio, o investigando por pedido del usuario) que la hipótesis se beneficiaría de un indicador o patrón que **no existe** en el catálogo nativo (los 158 indicadores/patrones/funciones de `talibs.xml`, ver `formato-entrega.md`):

1. Se propone con: qué mide, por qué la librería nativa no lo cubre (o por qué complementa a un nativo en vez de duplicarlo), en qué capa encajaría, y qué evidencia/fuente respalda la definición (si se investigó por internet, se cita la fuente).
2. Se registra en `_indicadores/indicadores_propuestos.json` con estado `propuesto` / `implementado` / `descartado`.
3. Solo se implementa (código) si el usuario lo aprueba explícitamente, indicador por indicador.

## Corrección importante sobre cómo se implementa (2026-07-07)

**No se programa en MQL4/MQL5.** Se verificó contra el manual oficial de la instalación (`Extending_SQX_es.pdf`, sección 2.1): los indicadores personalizados para el Full Builder de StrategyQuant X se programan en **Java**, vía el **Code Editor** interno, como un "Snippet" de tipo Indicador — no se puede copiar y pegar código MQL directamente, hay que reescribirlo en Java.

Mecánica real:
- Se crea en Code Editor → Nuevo → tipo "Indicator", nombre del indicador. Esto genera `Snippets/SQ/Blocks/Indicators/<Nombre>/<Nombre>.java` (carpeta propia por indicador, para agrupar señales relacionadas más adelante).
- La clase extiende `IndicatorBlock` y usa anotaciones: `@BuildingBlock(name=..., display=..., returnType=...)`, `@Parameter` (cada entrada: `Input` la serie de datos, `Period`, etc.), `@Output` (buffer de salida — puede haber más de uno, ej. banda superior/inferior de Bollinger).
- `returnType` debe ser uno de: `Price` (se dibuja sobre el precio, ej. Bollinger, medias móviles), `Number` (se dibuja aparte, ej. CCI, RSI, MACD), o `PriceRange` (diferencia entre dos precios, ej. ATR) — determina con qué otros bloques puede compararse.
- Se implementan dos métodos: `OnBarUpdate()` (calcula el valor por cada barra y lo guarda en el buffer de salida) y `OnBlockEvaluate(int relativeShift)` (lo que el motor de backtesting/genético consulta — típicamente `return Indicators.NOMBRE(parámetros).Buffer.get(relativeShift + Shift);`).
- Una vez compilado, queda disponible como bloque habilitable/deshabilitable en `BuildingBlocks` (el mismo mecanismo `use="true"/"false"` que ya usamos para editar qué indicadores puede explorar el motor genético — ver `calibracion-motor-genetico.md`).

MQL4/MQL5 (u otro lenguaje nativo de la plataforma) solo entra en juego más adelante, cuando la estrategia terminada se exporta para correr en vivo sobre esa plataforma — StrategyQuant genera ese código automáticamente a partir de la lógica interna, no es algo que se escriba a mano para que el indicador exista dentro del Builder.

4. Una vez implementado, se revisa el catálogo de plantillas existentes para identificar cuáles podrían beneficiarse de él como **actualización versionada** (nunca sobrescribiendo la plantilla original — se crea una nueva versión con changelog, la anterior queda archivada como `estado: superada`).

## Lección operativa (2026-07-07): conflicto entre edición externa y el Code Editor abierto

Si el Code Editor tiene un archivo `.java` abierto en una pestaña mientras se edita ese mismo archivo desde fuera (por Claude), al cerrar la aplicación puede **sobrescribir la corrección con la versión vieja que tenía en su buffer de memoria** — pasó exactamente esto con `ChoppinessIndex.java` (una corrección de import se perdió así). Antes de recompilar tras una edición externa, cerrar la pestaña del archivo en Code Editor (o cerrar y reabrir el Code Editor) para que cargue la versión real del disco.

## Indicadores implementados hasta ahora

Ver `_indicadores/indicadores_propuestos.json` para el detalle y estado de cada uno. Al 2026-07-07: `ChoppinessIndex` y `EfficiencyRatio` (Kaufman), ambos `implementado` y compilando sin errores, disponibles como Building Blocks para cualquier plantilla nueva o existente (no se agregaron todavía a ninguna plantilla en curso — eso se decide caso por caso).
