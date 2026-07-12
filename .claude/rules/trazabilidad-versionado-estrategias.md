---
paths: SQX_Library/**
cargar_en: Cuando una estrategia individual (no la plantilla) avanza de una etapa del pipeline a otra — Filtering, CustomAnalysis, Retest, Walk-Forward Matrix, Optimize, Portfolio, exportación a vivo
naturaleza: ESTABLE
---

# Trazabilidad y versionado de estrategias individuales

Distinto de `formato-entrega.md` (nombre de archivo al guardar candidatas del Build) — esta regla cubre el **linaje completo** de una estrategia a medida que atraviesa varias etapas del pipeline (Retest, Walk-Forward Matrix, Optimize, Portfolio, despliegue en vivo), donde el archivo `.sqx` se guarda y se vuelve a guardar múltiples veces.

## Evidencia real que motiva esta regla (investigado 2026-07-12)

Se abrió el proyecto de ejemplo real `user/projects/Optimizer/` (contiene estrategias que efectivamente pasaron por Optimize y Walk-Forward Matrix) y se inspeccionó tanto el nombre de archivo como el contenido interno del `.sqx` (es un ZIP/JAR: `settings.xml`, `strategy_Portfolio.xml`, `orders.bin`, `optimizationProfile.bin`, `version.txt`, `lastSettings.xml`). Hallazgos:

1. **El ID interno de SQ (`gen.individuo.variante`, ej. `1.1.26`) no cambia entre etapas.** Build, Retest y Optimize devuelven archivos con el mismo ID base.
2. **Solo Walk-Forward Matrix antepone un prefijo distintivo** al nombre (`WF Matrix - Strategy 1.1.26.sqx`). Retest y Optimize no agregan ningún prefijo — el archivo optimizado sale con el mismo nombre que el original.
3. **Colisión de nombre → sufijo anticolisión arbitrario `(1)`, `(2)`, `(3)`.** Confirmado caso real: `Strategy 4.7.22.sqx` y `Strategy 4.7.22(1).sqx` conviven en el mismo databank (Optimize generó una variante de parámetros distinta del mismo candidato). El sufijo no dice qué cambió, ni cuándo, ni con qué configuración — es solo anticolisión de archivo, no versionado.
4. **No hay ningún campo de linaje dentro del `.sqx`.** `version.txt` es la versión del *formato* de archivo, no de la estrategia. Se comparó `strategy_Portfolio.xml` (el árbol de reglas real) entre el original y su versión "WF Matrix": idéntico en tamaño y contenido — WFM solo agrega subcarpetas de resultados (`WF_10 runs_10% OOS`, etc.) dentro del mismo archivo, no reescribe ni marca la estrategia.

**Conclusión:** ni el archivo nativo `.sqx` ni el código fuente exportado (generado desde ese mismo árbol de reglas, sin metadata adicional que agregar) dan trazabilidad automática entre etapas. Hay que construirla externamente y anclarla al único dato confirmado como estable: el ID `gen.individuo.variante` de Build.

## Mecanismo

### 1. Tag de etapa en el nombre de archivo

Se extiende el ID-diferenciador de `formato-entrega.md` agregando un sufijo de etapa cada vez que el archivo se mueve/guarda en una carpeta de etapa distinta del pipeline:

`<nombre-base-formato-entrega>_<tag-etapa>[-vN].sqx`

Tags de etapa (en orden típico de pipeline):
- `_build` — salida cruda del Build (ya implícito en `candidatas_build/`, no hace falta repetirlo ahí)
- `_filter` — pasó la tarea Filtering
- `_oosvalid` — pasó el filtro CustomAnalysis de Profit Factor OOS
- `_retest` — pasó Retest
- `_wfm` — pasó Walk-Forward Matrix (coexiste con el prefijo nativo `WF Matrix -` que ya pone SQ; no lo reemplaza, lo complementa)
- `_opt-v1`, `_opt-v2`... — pasó Optimize. **El número de versión es obligatorio y lo asignamos nosotros** (nunca confiar en el sufijo `(N)` de SQ, que es anticolisión ciego, no semántico). Si Optimize produce varias variantes de parámetros del mismo candidato, cada una es un `-vN` distinto con su propia entrada en el registro de linaje (punto 2).
- `_portfolio-<nombre-portafolio>` — incluida en un portafolio concreto
- `_live-<cuenta-o-broker>` — desplegada en cuenta real

Ejemplo real de progresión: `EURUSD_H1_REVRANGE_01-PFOOS1.54-SQ2.3.109.sqx` (candidata de Build, ya con el esquema de `formato-entrega.md`) → tras Retest+WFM+Optimize: `EURUSD_H1_REVRANGE_01-PFOOS1.54-SQ2.3.109_retest_wfm_opt-v1.sqx`.

### 2. Registro central de linaje (`linaje_estrategias.json`, uno por carpeta de hipótesis)

Vive junto al `changelog.md` de cada hipótesis (ej. `EURUSD/ReversionRango/linaje_estrategias.json`). A diferencia del `changelog.md` (narra decisiones de la plantilla completa), este registra el historial etapa-por-etapa de estrategias individuales que avanzaron más allá de `candidatas_build/`:

```json
{
  "estrategias": [
    {
      "id_base_sq": "2.3.109",
      "plantilla_origen": "EURUSD-REVRANGE-H1-001",
      "nombre_archivo_actual": "EURUSD_H1_REVRANGE_01-PFOOS1.54-SQ2.3.109_retest_wfm_opt-v1.sqx",
      "estado_actual": "en_optimize",
      "historial": [
        {"etapa": "build", "fecha": "2026-07-12", "archivo": "EURUSD_H1_REVRANGE_01-PFOOS1.54-SQ2.3.109.sqx", "metricas_clave": {"PF_OOS": 1.54, "trades": 0}},
        {"etapa": "retest", "fecha": "", "archivo": "..._retest.sqx", "resultado": "", "metricas_clave": {}},
        {"etapa": "wfm", "fecha": "", "archivo": "..._retest_wfm.sqx", "WFE": null, "resultado": ""},
        {"etapa": "optimize", "fecha": "", "version_opt": 1, "archivo": "..._retest_wfm_opt-v1.sqx", "parametros_cambiados": ""}
      ]
    }
  ]
}
```

Cada vez que una estrategia avanza de etapa: (a) se renombra con el tag correspondiente, (b) se agrega una entrada nueva al array `historial` de su registro (crear el registro si es la primera vez que sale de `candidatas_build/`). No se sobrescribe historial previo — solo se agrega.

### 3. Regla operativa

- El renombrado con tag de etapa ocurre en el mismo momento en que el archivo se guarda en la carpeta de esa etapa (vía tarea `SaveToFiles` del pipeline o guardado manual) — no se deja para después, porque ahí es donde se sabe con certeza qué etapa acaba de pasar.
- Si Optimize genera múltiples variantes de parámetros de un mismo candidato, todas quedan registradas como entradas `optimize` distintas (`version_opt` incremental) bajo el mismo `id_base_sq` — nunca se descarta una variante sin dejar constancia de que existió, mismo principio que `criterio-descarte-plantillas.md`.
- Esta regla se activa recién cuando haya candidatas reales avanzando más allá de `candidatas_build/` (Retest/WFM/Optimize) — no aplica todavía a las 1000 estrategias en bruto del backup del 2026-07-12, que siguen sin filtrar.
