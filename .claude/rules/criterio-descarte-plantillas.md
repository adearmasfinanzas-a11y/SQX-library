---
paths: SQX_Library/**
cargar_en: Cuando una plantilla ya generó estrategias reales y hay que decidir si el resultado (pasó/no pasó Retest-robustez, calidad del resultado) confirma o refuta la hipótesis
naturaleza: CRITERIO DE DECISIÓN — cómo distinguir "esta instancia falló" de "la hipótesis no tiene ventaja real", y qué hacer en cada caso
---

# Criterio de descarte de plantillas (registrado 2026-07-08, a pedido explícito del usuario)

Este archivo define cómo decidir, con honestidad, si el fracaso de una plantilla en generar estrategias que pasen Retest/robustez significa que la **hipótesis de mercado** no tiene ventaja real, o si es un problema de **calibración/instancia** que todavía se puede corregir.

## 1. Distinción central: instancia vs. hipótesis

Que las estrategias generadas por una plantilla no pasen Retest/robustez, o no sean tan buenas como se esperaba, **no es automáticamente evidencia de que la hipótesis esté mal**. Antes de concluir eso, hay que descartar explicaciones alternativas:

- ¿El Build encontró una instancia débil, pero con más tiempo/generaciones podría aparecer algo mejor? → problema de búsqueda, no de hipótesis.
- ¿Hay un problema de calibración todavía sin resolver (símbolo, SL/PT, filtros de aceptación, testPrecision)? → problema de configuración, no de hipótesis.
- ¿El régimen histórico elegido para el IS era representativo del comportamiento que se busca capturar? → problema de diseño de datos, no de hipótesis.
- Solo si ninguna de estas explica el fracaso, se llega a la pregunta real: ¿la ineficiencia de mercado que plantea la hipótesis existe y es explotable en este activo/timeframe con esta lógica?

## 2. Criterio real para concluir que la hipótesis debe descartarse

Se descarta la hipótesis (no solo la instancia) cuando hay un **patrón consistente en múltiples corridas independientes**, no un solo intento — en particular:
- Deterioro sistemático en OOS respecto a IS a través de varias corridas (señal de que solo capturaba ruido del IS, no ventaja real).
- Incapacidad sistemática de acercarse al piso de calidad justificado para esa plantilla, incluso después de descartar los problemas de calibración de la sección 1.

Un solo fallo, con una plantilla todavía sin calibrar del todo (como es el caso normal en las primeras corridas de cualquier plantilla nueva — ver `calibracion-motor-genetico.md`), no alcanza para esta conclusión.

## 3. Lo que nunca se hace para forzar un "pase"

**Nunca se aflojan los criterios de aceptación hasta que "algo pase" solo para tener un resultado que mostrar.** Los criterios de cada plantilla ya están pensados con justificación propia documentada (ver `formato-entrega.md` sección 15) — si de verdad nada los cumple después de descartar problemas de calibración, la conclusión correcta es que la hipótesis no sirve para ese activo/timeframe, no que el criterio "estaba mal puesto" y hay que bajarlo sin fundamento nuevo.

## 4. Documentar el fracaso con el mismo rigor que el éxito

Un resultado negativo real es información valiosa — evita proponer la misma hipótesis fallida a futuro sin saber que ya se probó y no funcionó. Cuando se concluye que una hipótesis debe descartarse:
- Se marca en `_catalogo/indice_maestro.json` con `estado: "descartada_no_supera_robustez"` (o similar), no se borra la entrada.
- Se documenta la razón concreta en el `changelog.md` de esa plantilla: qué se probó, qué patrón se observó, por qué se concluye que no hay ventaja real (no solo "no funcionó").
- Esta entrada queda como referencia para no repetir el mismo camino sin nueva evidencia que justifique reabrirlo.

## 5. Aclaración importante: "no tan buena como se esperaba" ≠ "hay que descartarla"

Si una plantilla **sí cumple el piso objetivo ya justificado** para ella (los criterios de la sección 15 de su ficha), pero el resultado es más modesto de lo que se esperaba subjetivamente, **el problema puede ser que la expectativa era poco realista, no que el modelo esté mal**. Esta distinción se hace con criterio caso por caso, no de memoria ni por decepción — cumplir el piso justificado es la vara real, no una sensación de "podría ser mejor".
