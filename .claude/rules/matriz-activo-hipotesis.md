---
paths: SQX_Library/**
cargar_en: Paso 3 del protocolo (sugerencia de hipótesis compatibles)
naturaleza: VIVO — este archivo crece con cada activo nuevo trabajado
---

# Matriz Activo ↔ Hipótesis

No todas las hipótesis son igualmente válidas en todos los activos. Ejemplos de partida (se expande según los activos que trabajemos):

| Familia (`familia_activo`) | Activo tipo | Hipótesis con sentido estructural | Hipótesis dudosas / requieren justificación extra |
|---|---|---|---|
| `FX_mayor_liquido` | FX mayor líquido (EURUSD, GBPUSD) | Reversión a la media en rango, Breakout de sesión, Momentum intradía, Liquidez de sesión (Londres/NY) | Expansión de volatilidad pura (menos frecuente sin catalizador de noticia) |
| `FX_cruzado_menor` | FX cruzado menos líquido (GBPJPY) | Breakout, Tendencia, Expansión de volatilidad | Reversión estadística fina (spread/ruido lo distorsiona) |
| `Indices_CFD` | Índices/CFD (NQ, futuros) | Tendencia, Momentum, Continuación, Gap de apertura | Liquidez de sesión clásica FX (estructura de sesión distinta) |
| `Materias_primas` | Materias primas (GOLD) | Tendencia, Expansión de volatilidad, Pullback | Microestructura fina intradía (menor profundidad de datos limpios) |

Cada vez que trabajemos un activo nuevo, esta tabla se actualiza con tu conocimiento del instrumento y el mío, y queda documentada.

## Rol de la familia en la biblioteca

**La familia es un metadato de catálogo, no una carpeta.** La estructura física de directorios permanece organizada por `<Activo>\<Hipotesis>\`, sin capa intermedia de familia. El campo `familia_activo` se guarda únicamente en `indice_maestro.json` y sirve para:

- **Paso 5 (chequeo de redundancia):** el riesgo de duplicidad conceptual es más relevante entre plantillas de la misma familia (ej. EURUSD vs GBPUSD) que entre familias distintas.
- **Checklist de cierre:** el riesgo de redundancia de portafolio (correlación entre activos hermanos) se evalúa filtrando el catálogo por `familia_activo`.

**Asignación de la familia:** cuando se trabaje un activo que todavía no tiene `familia_activo` asignada en esta tabla, se propone en el Paso 3 del protocolo — nunca se da por hecha en silencio. La propuesta incluye: el identificador (`snake_case`), a qué activos existentes se parece estructuralmente (liquidez, sesión, volatilidad típica), y por qué encaja o no en una familia ya existente. Queda sujeta a confirmación antes de escribirse en el catálogo; una vez confirmada, se añade como fila nueva a esta tabla.
