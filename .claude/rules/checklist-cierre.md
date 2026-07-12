---
paths: SQX_Library/**
cargar_en: Al cerrar cualquier plantilla (ficha o .sqx)
naturaleza: EJECUCIÓN — lista de verificación, no convención permanente
---

# Checklist crítico de cierre

Antes de dar por terminada cualquier plantilla:

- [ ] ¿Se consultó el catálogo antes de empezar?
- [ ] ¿La hipótesis está justificada para este activo específico (no genérica)?
- [ ] ¿Cada capa responde una sola pregunta, sin relleno artificial?
- [ ] ¿Capa 2 y 3 comparten familia lógica, o la excepción está justificada por escrito?
- [ ] ¿Cada indicador tiene función y capa explícitas?
- [ ] ¿Las versiones Restrictiva y Flexible preservan la misma hipótesis?
- [ ] ¿Se verificó disponibilidad real del indicador en el motor elegido?
- [ ] ¿Se registró en `indice_maestro.json` con changelog?
- [ ] ¿Existe riesgo de redundancia de portafolio con otra plantilla de la misma `familia_activo` ya en biblioteca? ¿Se señaló?
- [ ] ¿El activo tiene `familia_activo` asignada en el catálogo y en la matriz? Si es nueva, ¿fue confirmada con el usuario antes de registrarse?
- [ ] ¿El rango histórico usado excluye regímenes de mercado ya identificados como problemáticos para ese activo?
- [ ] Si se generó `.sqx`: ¿se generó y verificó en el Full Builder de StrategyQuant X?
- [ ] ¿Se actualizó `_estado/estado_proyecto.md` (foco actual, pendientes, historial de sesiones)?
- [ ] ¿La ficha define PopulationSize/MaxGenerations concretos (100/100 salvo justificación)?
- [ ] ¿El Build exige un único filtro de aceptación liviano (no 3+ condiciones simultáneas), con el resto diferido a Retest/WalkForward?
- [ ] ¿El período de ATR para SL/PT está fijo (no explorado como rango) salvo justificación escrita?
- [ ] ¿Money Management de la fase de investigación sigue el estándar de la biblioteca (FixedSize, mismo tamaño e InitialCapital) salvo excepción documentada?
- [ ] ¿Trading Options tiene valores concretos elegidos (no solo "restringido por defecto")?
- [ ] ¿La ficha tiene un resumen ejecutivo en prosa (objetivo, oportunidad de mercado, herramientas y por qué, tipo de resultado buscado) — no solo campos técnicos sueltos?
- [ ] ¿Los criterios de aceptación y los rangos de SL/PT están justificados para esta plantilla en particular (no copiados de otra plantilla ni de un proyecto de ejemplo sin analizar si aplica)?
