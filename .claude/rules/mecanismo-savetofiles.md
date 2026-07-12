---
paths: SQX_Library/**
cargar_en: Al configurar cualquier tarea SaveToFiles del pipeline — de cualquier plantilla, no solo la actual
naturaleza: ESTABLE — mecanismo nativo del motor, no específico de ninguna plantilla
---

# Mecanismo nativo de la tarea `SaveToFiles`

Investigado el 2026-07-12 siguiendo la misma metodología que `mecanismo-condiciones-filtrado.md` (decompilar con `javap` antes de asumir, confirmar con un preset real exportado). Primer uso real: guardar el resultado en bruto del Build de `EURUSD-REVRANGE-H1-001` (1000 estrategias) antes de correr el filtro OOS — ver `pipeline-multitarea-y-diseno-is.md` sección 3g para la justificación de por qué este paso es obligatorio en todo pipeline.

## Capacidades reales (decompilado de `SaveToFiles.class`, `internal/plugins/TaskSaveToFiles/`)

La tarea exporta **mucho más que solo `.sqx`** — siete tipos de salida independientes, cada uno con su propio directorio de destino y su propio toggle:

| Campo | Qué exporta |
|---|---|
| `SaveInSqxFormat` → `DestDirectorySqx` | Un `.sqx` por estrategia (formato nativo, recargable en cualquier databank) |
| `SaveInStrFormat` → `DestDirectoryStr` | Formato `.str` (alternativo, no investigado en detalle — no usado todavía) |
| `ExportDatabank` → `DestDirectoryDatabank` | **Un solo archivo resumen** (XLSX o CSV) con las métricas de todas las estrategias del databank juntas — barato, alto valor, no confundir con el `.sqx` individual |
| `ExportTrades` → `DestDirectoryTrades` | Lista de operaciones (trade-by-trade) **por cada estrategia** — caro en cantidad de archivos si el databank es grande, reservar para etapas ya filtradas, no para el bruto |
| `SaveInHtmlFormat` → `DestDirectoryHtml` | Reporte HTML por estrategia |
| `SaveInPdfFormat` → `DestDirectoryPdf` | Reporte PDF por estrategia |
| `SaveSourceCode` → `DestDirectorySC` | **Código fuente ejecutable**, ver catálogo completo abajo — el más relevante para despliegue en vivo |

## Catálogo de formatos de código fuente (`SaveSourceCode`, confirmado en la interfaz real, 2026-07-12)

Desplegable real visto en pantalla:
1. `EasyLanguage for Tradestation / MultiCharts (*.el)`
2. `Expert Advisor for JForex (*.java)`
3. `Expert Advisor for MetaTrader4 (*.MQ4)`
4. **`Expert Advisor for MetaTrader5 (*.MQ5)`** ← el relevante para esta biblioteca
5. `Pseudo Code (*.TXT)`
6. `Strategy XML`

**Hallazgo importante para `proceso-exportacion-live.md`:** esta tarea puede generar directamente el `.mq5` ejecutable como parte del pipeline automatizado — no hace falta un paso manual de exportación aparte. **Pendiente de resolver antes de usarlo para despliegue real:** cómo incorporar la integración con GestorPortafolio (`PortfolioParticipant.mqh` + condición de pausa cooperativa, ver `integracion-gestorportafolio.md`) al código generado automáticamente — no investigado todavía si SQ permite inyectar código custom en el EA exportado o si hay que editarlo a mano después de generarlo.

## Magic Number nativo (`MNActive`/`MNValue`)

Confirmado con evidencia real: existe un campo de Magic Number nativo en esta tarea (`MNActive=false`, `MNValue=12345` de placeholder por defecto), que se pasa directo a `StrategySaver.save(...)` al guardar cada estrategia. **Esto corrige lo documentado antes en `proceso-portafolio.md`** (se había dicho que asignar Magic Number no era automático) — al menos a nivel de esta tarea, si es un campo nativo real. Queda pendiente confirmar si asigna el mismo valor a todas las estrategias del databank (necesitaríamos uno distinto por estrategia para que convivan en una cuenta) o si incrementa automáticamente — **no verificado todavía**, se revisa cuando se use en el guardado final de portafolio.

## Etiquetado nativo (`SetNoteActive`/`SetNoteType`/`SetNoteCustom`)

Confirmado con preset real: `SetNoteType` acepta al menos `"custom"` (texto libre, en `SetNoteCustom`) además de los presets vistos en la interfaz (símbolo, timeframe, símbolo+timeframe). Usado en la práctica para escribir `corrida01_bruto_build_2026-07-12` como nota de cada estrategia guardada — mecanismo nativo que complementa (no reemplaza) el registro externo `linaje_estrategias.json`.

## Anticolisión de nombres (`OverwriteFiles`, `generateUniqueName()`)

Decompilado antes de tener el preset real, y confirmado: si `OverwriteFiles=false` (default) y ya existe un archivo con ese nombre en el destino, usa el mismo mecanismo de sufijo `(1)`, `(2)`... que ya se había visto en Optimize/WFM — no sobrescribe, pero tampoco distingue de forma legible qué corrida generó cada copia. Para evitar depender de esto, se usa una carpeta de destino distinta por corrida (`_runs\<fecha>_corridaN\`, ver sección 3g de `pipeline-multitarea-y-diseno-is.md`) en vez de confiar en el sufijo automático.

## Bug real de la interfaz: nombres de preset con segmento numérico aislado fallan (2026-07-12)

Al intentar guardar el preset de esta tarea como `SaveToFiles_01_BrutoBuild.cfx`, la interfaz de SQ (no Windows — es un cuadro de diálogo propio de la app) rechazó el nombre con **"El nombre de archivo no es válido"**. Comparado con un nombre que sí funcionó antes (`Filtro_OOS_PF1.2_Trades30.cfx`, con guiones bajos y hasta un punto decimal), la diferencia es tener un **segmento puramente numérico aislado entre guiones bajos** (`_01_`). Quitando ese segmento (`SaveToFiles_BrutoBuild.cfx`) el guardado funcionó sin problema. **Regla práctica para nombrar presets futuros: evitar segmentos que sean solo dígitos entre separadores** (ej. usar `Trades30` o `PF1.2`, nunca `_01_` aislado) — no confirmado el motivo exacto (posible validación interna tipo nombre de clase Java, se vio `SQUtils.correctClassName()` referenciado en el bytecode de esta misma tarea para casos de exportación de código), pero el patrón de fallo es reproducible.

## Preset real completo confirmado (`SaveToFiles-Task1.xml`, guardado por el usuario, 2026-07-12)

```xml
<Task name="Save to files" type="SaveToFiles" taskXMLFile="SaveToFiles-Task1.xml" version="143.2708">
  <Settings>
    <SaveToFiles>
      <DestDirectorySqx>...\01_bruto_build</DestDirectorySqx>
      <SaveInSqxFormat>true</SaveInSqxFormat>
      <SaveInStrFormat>false</SaveInStrFormat>
      <DestDirectoryDatabank>...\01_bruto_build</DestDirectoryDatabank>
      <ExportDatabank>true</ExportDatabank>
      <ExportTrades>false</ExportTrades>
      <SaveInHtmlFormat>false</SaveInHtmlFormat>
      <SaveInPdfFormat>false</SaveInPdfFormat>
      <SaveSourceCode type="EasyLanguage for Tradestation / MultiCharts (*.el)" format="el">false</SaveSourceCode>
      <OverwriteFiles>false</OverwriteFiles>
      <MNActive>false</MNActive>
      <MNValue>12345</MNValue>
      <Data>all</Data>
      <Format>xlsx</Format>
      <UseComma>false</UseComma>
      <SetNoteActive>true</SetNoteActive>
      <SetNoteType>custom</SetNoteType>
      <SetNoteCustom>corrida01_bruto_build_2026-07-12.</SetNoteCustom>
    </SaveToFiles>
    <Databanks>
      <Databank label="Input databank" name="Input" value="Results"/>
    </Databanks>
  </Settings>
</Task>
```

Confirma el nombre de tarea real (`SaveToFiles-TaskN.xml`, mismo patrón que `Build-TaskN.xml`/`Filtering-TaskN.xml`), y que `SaveSourceCode` guarda su `type`/`format` como atributos XML incluso cuando está desactivado (el desplegable recuerda la última selección visual, que por defecto es la primera opción de la lista).
