---
paths: SQX_Library/**
cargar_en: Paso 6 del protocolo (Diseño) y Paso 7 (Registro)
naturaleza: ESTABLE
---

# Dos versiones — vinculadas, no independientes

Toda plantilla exige versión **Restrictiva** (robustez, espacio reducido) y **Flexible** (exploración, mayor diversidad sin romper la hipótesis).

**Regla de vínculo:** ambas versiones comparten el mismo `id` base en el catálogo (ej. `EURUSD-BREAKOUT-H1-001-R` y `...-001-F`) para que no puedan divergir de hipótesis sin que quede registrado. Si en algún momento la versión Flexible empieza a explorar una hipótesis distinta a la Restrictiva, eso es una señal de ruptura de coherencia y debe reportarse, no pasarse por alto.
