---
paths: SQX_Library/**
cargar_en: Paso 6 y Paso 7 del protocolo (diseño y generación del artefacto .sqx)
naturaleza: ESTABLE
---

# Formato de entrega

## Convención de nombres para estrategias individuales (`.sqx` guardados de un databank)

Formato: `<ACTIVO>_<TEMPORALIDAD>_<FAMILIAHIPOTESIS>_<ID-diferenciador>.sqx`

El `ID-diferenciador` combina el ranking dentro del lote (01, 02...) con el dato más importante para decidir entre candidatas del mismo lote — en plantillas de largo plazo, el Profit Factor OOS (ej. `01-PFOOS1.54`) — más el ID interno de SQ como referencia cruzada con el databank (`SQ2.3.109`). Ejemplo real: `EURUSD_H1_REVRANGE_01-PFOOS1.54-SQ2.3.109.sqx`. Las descartadas usan el mismo esquema con `DESCARTADA` en vez del ranking, y se archivan en una subcarpeta separada (`descartadas_<motivo>/`), nunca se borran — mismo principio que `criterio-descarte-plantillas.md` de no perder el registro de qué no funcionó.

Se distinguen dos artefactos, porque la instalación real usa un formato que no es JSON:

**A. Ficha de diseño (`ficha_diseño.json`)** — el entregable completo y **autosuficiente**: no solo el diseño conceptual de la hipótesis, sino también toda la configuración del Builder necesaria para correrla, con valores concretos a consideración (no placeholders) y su justificación. La ficha se basta a sí misma — alguien debería poder configurar el Builder solo leyéndola. Esto no quita que, al construirla a mano en el Full Builder, el usuario pueda ajustar cualquier valor sobre la marcha; si lo hace, la desviación se documenta en el `changelog.md` de la plantilla (mecanismo ya existente, ver ejemplo real en `EURUSD/ReversionRango/changelog.md`).

Secciones de la ficha:

**0. Resumen ejecutivo (obligatorio, primera sección, en prosa profesional — no JSON técnico suelto):** una explicación legible para el usuario, no para la máquina, que responda con claridad:
- **Objetivo de la plantilla** — qué se le está pidiendo a StrategyQuant X que construya.
- **Qué oportunidad de mercado ataca** — la hipótesis explicada en términos de comportamiento de mercado (no solo el nombre técnico de los indicadores), y por qué existe razón para pensar que esa ineficiencia se da en este activo/timeframe concreto.
- **Con qué herramientas, y por qué esas y no otras** — cada indicador/mecanismo entregado al motor genético se justifica: qué mide, por qué se le da esa libertad (rango) o esa restricción (valor fijo) al motor, y qué alternativa se descartó y por qué.
- **Qué tipo de resultado se busca** — se explicita que el objetivo es calidad sobre cantidad ("francotiradores": pocas estrategias con probabilidad real de sostenerse fuera de muestra, no muchas por pura combinatoria), y cómo los criterios de aceptación elegidos reflejan eso.

Esta sección es la que permite que cualquiera (incluido el propio usuario, releyendo la ficha meses después) entienda el diseño completo sin tener que interpretar los campos técnicos de las secciones siguientes.

**Diseño conceptual (las 11 secciones originales):**
1. Nombre de la hipótesis
2. Descripción de la hipótesis
3. Arquitectura completa de 5 capas (con notas de capas neutrales justificadas)
4. Espacio abierto del Builder
5. Espacio cerrado del Builder
6. Restricciones entre capas
7. Diagrama lógico del flujo
8. Espacio de exploración cerrado/abierto por elemento, con justificación
9. Evaluación crítica de fortalezas y debilidades
10. Riesgos de sobreajuste específicos de esa plantilla
11. Recomendaciones para maximizar diversidad sin romper la hipótesis

**Configuración completa del Builder (obligatoria, con valores concretos):**

12. **RulesComplexity** — min/max condiciones de entrada y salida, min/max período de indicadores, min/max shift. Valores de referencia y su justificación: `@.claude/rules/calibracion-motor-genetico.md`.
13. **Configuración genética (BuildMode)** — PopulationSize, MaxGenerations, y cualquier ajuste avanzado (islas, migración) si aplica. Referencia: `@.claude/rules/calibracion-motor-genetico.md` (100/100 salvo justificación).
14. **SL/PT** — basado en ATR o en pips fijos, multiplicadores (rango o fijo), período de ATR (fijo, salvo justificación — ver `calibracion-motor-genetico.md`).
15. **Criterios de aceptación, por etapa** — no son un valor fijo de biblioteca copiable entre plantillas. Se analizan con criterio crítico para cada plantilla concreta (tipo de hipótesis, densidad de oportunidades del timeframe/IS elegido, volatilidad del activo) y se documentan **con su justificación**, solo los que correspondan tener en esta etapa — ver `@.claude/rules/calibracion-motor-genetico.md`. Lo que se evita en cualquier caso: filtros tan laxos que dejen pasar cualquier cosa a Retest (ej. solo "no pierde plata"), y también apilar tantas condiciones simultáneas sin justificar que la probabilidad de cumplir todas a la vez sea casi nula.
16. **Trading Options** — filtros de sesión, restricción de fin de semana, máximo de operaciones por día, rango horario, y cualquier otra restricción operativa, con el valor concreto elegido (no solo "restringido por defecto" — el valor real).
17. **Money Management** — método y parámetros concretos para la fase de investigación (Build + Retest), siguiendo `@.claude/rules/configuracion-money-management.md` (estándar: FixedSize, mismo tamaño e InitialCapital que el resto de la biblioteca, salvo excepción justificada).
18. **Plan de Retest/WalkForward (recomendado)** — qué tareas de validación se plantean después del Build (out-of-sample, cross-market, slippage, Monte Carlo de parámetros, etc.), inspirado en el patrón real encontrado en los proyectos de ejemplo de la instalación (`Build → varias tareas de Retest encadenadas → limpieza de bancos → loop`). No bloquea la entrega de la ficha si todavía no se define en detalle, pero se dejan al menos las validaciones mínimas esperadas.
19. **Optimizer (si aplica)** — si la plantilla contempla una etapa de optimización de parámetros posterior al Build/Retest, qué parámetros y rango se optimizarían. Opcional, se completa solo si la plantilla lo requiere.

**B. Artefacto ejecutable (`.sqx`)** — solo se genera cuando se pida explícitamente "créala en el Builder" o "genera el archivo". Se construye replicando la estructura real verificada en la instalación (ZIP con `strategy_Portfolio.xml` + `lastSettings.xml`, bloques `Item key`/`Block` anidados por capa), y se coloca en la carpeta del activo/hipótesis correspondiente. **Se genera y edita en el Full Builder de StrategyQuant X.**

---

## Nota de diseño: por qué no se exige compatibilidad con AlgoWizard

Esta biblioteca exigió en su origen que todo `.sqx` fuera editable sin errores tanto en el Full Builder como en AlgoWizard. Esa regla se retiró el 2026-07-04 tras verificar directamente los archivos internos de la instalación (`internal\ctemplate\wizard.xml` vs `internal\ctemplate\talibs.xml`):

- AlgoWizard expone solo 7 indicadores (SMA, EMA, CCI, RSI, MACD, Stochastic, BollingerBands) de un catálogo real de ~156 indicadores/patrones/funciones disponibles en el Full Builder — menos del 5% de cobertura.
- AlgoWizard no tiene concepto de capas separadas (Contexto/Oportunidad/Evidencia/Ejecución/Gestión): colapsa todo en 4 pasos planos de condiciones (Long entry, Short entry, Long exit, Short exit) + Money management.
- El propio `wizard.xml` se autodescribe como herramienta de prototipado rápido con funcionalidad básica, remitiendo al Full Builder para "complete functionality".

Dado que la arquitectura de 5 capas y la mayoría de las hipótesis de esta biblioteca dependen de indicadores fuera del set de AlgoWizard, exigir compatibilidad con AlgoWizard habría empobrecido sistemáticamente el diseño de casi todas las plantillas a cambio de un beneficio que esta biblioteca no necesita (edición guiada para prototipos simples). **El Full Builder es el entorno de referencia único para generar y editar los `.sqx` de esta biblioteca.**

Si en el futuro una plantilla específica necesita ser editable en AlgoWizard por alguna razón puntual (ej. compartirla con alguien sin acceso al Full Builder), eso se evalúa caso por caso y se documenta en el `changelog.md` de esa plantilla — no es una regla general del sistema.
