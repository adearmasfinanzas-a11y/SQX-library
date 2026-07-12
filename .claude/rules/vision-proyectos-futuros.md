---
paths: SQX_Library/**
cargar_en: Cuando se retome la conversación sobre expandir el proyecto más allá de una sola plantilla (portafolios, dashboards, automatización con IA)
naturaleza: VISIÓN — no accionable todavía, requiere al menos una plantilla validada real como base
---

# Visión de proyectos futuros (registrada 2026-07-08, no accionable todavía)

El usuario planteó una visión de largo plazo para este proyecto, más allá de una sola plantilla. Se registra acá con evaluación crítica de factibilidad y riesgo de cada pieza, para retomar cuando corresponda — **ninguna de estas piezas se empieza antes de tener al menos una plantilla validada real** (Build convergido + Retest/robustez pasado).

## 1. EA supervisor de portafolio — MOVIDO FUERA DE ESTE PROYECTO (2026-07-11)

**Ya no es parte de la visión de SQX_Library.** El usuario está desarrollando esto por otra vía, como proyecto independiente ("GestorPortafolio"). SQX_Library solo necesita saber que existe un requisito de **integración**: los bots exportados desde nuestras plantillas deben incluir un mecanismo cooperativo de pausa para que GestorPortafolio pueda pedirles "no abras nada nuevo" sin cerrarles lo que ya tienen abierto. El detalle técnico completo de esa integración vive en `integracion-gestorportafolio.md`, no acá — esta sección se mantiene solo como nota histórica de que la idea se originó en esta biblioteca antes de independizarse.

## 2. Retroalimentación con IA (exportar datos → análisis → ajuste de parámetros)

Existe como concepto ("AI-in-the-loop" / control supervisor adaptativo). **Riesgo alto si no se diseña con cuidado**: un ajuste de parámetro sugerido por análisis de IA sobre datos en vivo es una hipótesis, no algo validado — aplicarlo directo sobre una cuenta real reintroduce el mismo riesgo de sobreconfianza en razonamiento no verificado que se evitó durante todo el desarrollo de la primera plantilla (ver `_registro-cambios.md` — varias hipótesis propias resultaron equivocadas al chequearlas con datos reales). **Regla de diseño para cuando se implemente:** cualquier ajuste sugerido por análisis de IA debe volver a pasar por el proceso de validación de StrategyQuant (backtest, Monte Carlo, etc.) antes de aplicarse en una cuenta real — nunca aplicación automática directa sin ese portón de validación.

## 3. Plataforma web de gestión de cuentas

Proyecto de software convencional (servidor + frontend, datos desde las terminales MT4/5). Riesgo bajo — es solo visualización/monitoreo, no toca dinero directamente. Buen candidato a proyecto concreto cuando haya algo real que monitorear.

## 4. Estrategia externa a MT5 vía API (ej. Python)

Real y viable — MetaQuotes tiene paquete oficial de Python (`MetaTrader5`) para leer datos y gestionar órdenes sin código MQL nativo. Ventaja: más fácil de mantener/iterar en código que puedo ayudar a escribir con más fluidez que MQL. Riesgo real de ingeniería: la terminal debe permanecer abierta y conectada; requiere reconexión y manejo de errores serio porque gestiona dinero real — no es un detalle menor.

## Orden de prioridad recomendado

1. Conseguir la primera plantilla validada (Build converge + pasa Retest/robustez) — **estado actual, en curso**.
2. Pipeline multi-tarea completo para esa plantilla (ver `pipeline-multitarea-y-diseno-is.md`).
3. Despliegue simple de una sola estrategia en vivo (ver `proceso-exportacion-live.md`) — el caso más simple posible, sin portafolio ni supervisión automática todavía.
4. Recién después: portafolio de varias plantillas, supervisor/interruptor automático, dashboard web.
5. Al final, y con más cautela que cualquier otra pieza: retroalimentación con IA para ajuste de parámetros — con el portón de validación obligatorio ya mencionado.
