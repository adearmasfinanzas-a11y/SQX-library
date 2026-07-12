---
paths: SQX_Library/**
cargar_en: Cuando una plantilla/portafolio ya validado esté listo para desplegarse en una cuenta real o demo
naturaleza: REFERENCIA TÉCNICA — exportación a plataformas de trading en vivo
---

# Exportación a trading en vivo (MT4/MT5/etc.)

Investigado con documentación oficial. Se usa recién cuando una estrategia (o portafolio) ya pasó todo el proceso de validación (Build → Retest/robustez → Walk-Forward) — no antes.

## El proceso de exportación

StrategyQuant genera el código nativo de la estrategia para la plataforma de destino (MT4, MT5, NinjaTrader, TradeStation, cTrader, según el motor con el que se construyó). Pasos: pestaña **Source Code** → elegir el lenguaje/plataforma destino (ej. "MetaTrader4 Expert Advisor") → esto genera el código del EA → botón **Save to file** para guardar el Expert Advisor.

Fuente: [Export strategy from StrategyQuant and test or trade it in MetaTrader](https://strategyquant.com/doc/strategyquant/export-strategy-strategyquant-test-trade-metatrader/)

**Nota de calidad:** las exportaciones a MT4/MT5 son las más probadas/confiables dentro del ecosistema StrategyQuant. Si una plantilla se construyó pensando en un motor específico (ej. nuestro estándar `MetaTrader5 (netted)`), verificar que el código exportado se comporte igual en un test manual antes de darlo por bueno — la generación automática de código no está exenta de casos borde.

## Regla operativa importante: separar generación de ejecución

**No correr StrategyQuant (Build/Retest, que consumen CPU al 100% durante horas) en la misma máquina donde corren las estrategias ya desplegadas en vivo.** El motivo es directo: si la máquina que ejecuta el trading real también está ocupada corriendo un Build, puede haber operaciones perdidas o slippage adicional por falta de recursos en el momento exacto en que la estrategia en vivo necesita ejecutar una orden.

**Recomendación:** exportar las estrategias/portafolios ya terminados a una máquina/VPS separada, dedicada solo a ejecución, y mantener esta instalación (`D:\StrategyQuantoInstalado`) exclusivamente para investigación/construcción.

## Validación cruzada MT5 antes de ir a vivo (investigado 2026-07-08)

Antes de confiar ciegamente en el backtest de StrategyQuant, se puede correr la estrategia exportada (`.ex5`) en el **Strategy Tester de MT5 de forma automatizada** (sin abrir la interfaz gráfica), para comparar si el comportamiento real en la plataforma de destino coincide con lo que StrategyQuant reportó.

**Mecánica real:** `terminal64.exe /config:"archivo.ini"` — el `.ini` define símbolo, rango de fechas, modelo de precisión de ticks (0=cada tick, 1=OHLC 1 minuto, 4=cada tick basado en ticks reales — equivalente conceptual a nuestro `testPrecision`), depósito inicial, y ruta del reporte de salida. Se puede automatizar con un `.bat` + Programador de Tareas de Windows.

Fuentes: [MetaTrader 5 Strategy Testing](https://www.metatrader5.com/en/terminal/help/algotrading/testing), [Platform Start - Advanced Users](https://www.metatrader5.com/en/terminal/help/start_advanced/start)

**Verificado en esta instalación (2026-07-08): MT5 (`terminal64.exe`) no está instalado en esta máquina** — se buscó en C:, D: y todos los perfiles de usuario, sin encontrarlo. Para poder hacer esta validación cruzada hace falta: (1) instalar MT5, (2) tener al menos una estrategia realmente aceptada para exportar (no hay ninguna todavía), (3) igualar spread/comisión/rango de fechas entre el `.ini` de MT5 y la configuración de StrategyQuant para que la comparación sea justa.

### Migrar los mismos datos (no datos de un broker cualquiera) — mejor que usar una cuenta demo genérica

MT5 necesita una cuenta (demo alcanza, gratis) para descargar el histórico la primera vez — pero la calidad/profundidad de esos datos varía según el broker, lo que puede sesgar la comparación. **Existe un proceso oficial de StrategyQuant para exportar los mismos datos que ya usamos (Dukascopy) directamente a formato nativo de MetaTrader**, en vez de depender de los datos de un broker demo cualquiera:

- Herramienta: **QuantDataManager** (complementaria a StrategyQuant X, descarga aparte — **no está instalada en esta máquina**, verificado).
- Puede exportar los datos ya descargados (incluidos los de Dukascopy que usamos en `EURUSD_dukas_M1_UTCPlus02`) a **formato HST/FXT nativo de MT4/MT5**, o a CSV genérico para importar como símbolo custom.
- Guía oficial: "How to export data from Quant Data Manager and import to Metatrader 5" (strategyquant.com/doc/quantdatamanager/how-to-import-data-to-metatrader-5/ — bloqueado para fetch directo, confirmado por título y referencias cruzadas en foros oficiales).

**Ventaja real:** usar los mismos datos de origen hace que la comparación SQX-vs-MT5 sea sobre la ejecución/simulación, no contaminada por diferencias de calidad de datos entre proveedores.

**Corrección (2026-07-08):** no hace falta instalar QuantDataManager por separado — el usuario confirmó que ya está disponible como sección/pestaña dentro de esta misma instalación de StrategyQuant X (se encontró `internal/plugins/AppQuantDataManager/AppQuantDataManager.jar` ya presente). Coincide con la licencia completa de StrategyQuant, que incluye la funcionalidad Pro de QuantDataManager sin costo aparte (aunque con su propio sistema de activación, distinto de la key de SQ).

**Pendiente real para poder hacer esto:** instalar MT5 (esto sí falta), y tener al menos una estrategia aceptada real para exportar y probar.

## Estado actual de la biblioteca (2026-07-08)

Ninguna plantilla llegó todavía a esta etapa — este archivo queda como referencia para cuando haya una plantilla validada lista para desplegarse y exportarse.
