---
paths: SQX_Library/**
cargar_en: Al exportar cualquier bot de una plantilla de esta biblioteca a MT5, si el destino es una cuenta gestionada por GestorPortafolio
naturaleza: REQUISITO DE INTEGRACIÓN con un proyecto externo (GestorPortafolio) — no es visión, es un contrato técnico real entre dos proyectos
---

# Integración con GestorPortafolio — paquete para pasar a la IA que exporta/adapta los bots

`GestorPortafolio` es un proyecto **independiente** de SQX_Library (no se desarrolla acá — ver nota en `vision-proyectos-futuros.md`). Este archivo existe solo para que, cuando se exporte un bot de una plantilla de esta biblioteca a MT5, la IA/persona que hace el export tenga la especificación completa sin tener que pedirla de nuevo.

## Especificación exacta (tal como la definió el usuario, 2026-07-11)

**1. Include**, cerca de los demás includes del archivo exportado:
```cpp
#include <GestorPortafolio\PortfolioParticipant.mqh>
```

**2. Una condición adicional (AND)** en la lógica que decide abrir una operación nueva — combinada con las condiciones de entrada que ya tenga la estrategia, no reemplazándolas:
```cpp
!IsBotPausedByPortfolioManager(<variable_del_magic_number_de_la_estrategia>)
```
La variable de magic number es la que ya trae cada export de SQX como input — no necesita un nombre particular, la función recibe cualquier `long`.

**3. Dónde va exactamente — la parte no negociable:** la condición va únicamente en la rama que decide **enviar una orden nueva**. Nunca en una rama que gestione una posición ya abierta (trailing stop, salida parcial, cierre por condición propia de la estrategia), ni al principio de `OnTick`/`OnTimer` de forma que corte también esa gestión.

**Regla simple para aplicarla sin ambigüedad:** si el bloque se ejecutaría igual aunque la estrategia ya tenga una posición abierta y solo esté decidiendo si moverla/cerrarla, la condición NO va ahí. Si el bloque es el que decide "¿abro algo nuevo ahora?", ahí sí.

**Propósito:** GestorPortafolio necesita poder decirle a un bot "no abras más nada" sin cerrarle lo que ya tiene abierto (ej. portafolio acercándose a un límite de exposición, sin ameritar cierre total todavía). Cerrar posiciones ya lo puede hacer sin cooperación de nadie (CTrade directo); lo único que no puede hacer sin esto es prevenir una apertura nueva.

## Por qué tiene que ser así

- MQL5 no tiene API para que un programa detenga a otro que corre en un gráfico distinto del mismo terminal — no existe "pausar EA externo". La única vía es cooperativa: una variable global de terminal por magic number, que cada bot consulta voluntariamente. Si el bot no trae esta línea, GestorPortafolio no tiene ningún poder de pausa sobre él.
- Sin DLL ni WebRequest — restricción no negociable (muchas prop firms prohíben DLLs y auditan código fuente). `PortfolioParticipant.mqh` solo usa `GlobalVariableCheck`/`GlobalVariableGet`, funciones nativas de MQL5.
- El lugar importa: pausar significa "no abrir nada nuevo", no "abandonar lo que ya está abierto". Ponerlo en el lugar equivocado (ej. al principio de `OnTick`) deja a un bot pausado con una posición abierta sin trailing stop ni gestión de salida hasta que se lo reanude — riesgo real en cuenta fondeada, no un detalle cosmético.

## Archivo de referencia (10 líneas de lógica real, el resto comentarios)

```cpp
#ifndef GESTORPORTAFOLIO_PORTFOLIO_PARTICIPANT_MQH
#define GESTORPORTAFOLIO_PORTFOLIO_PARTICIPANT_MQH

string PortfolioPauseGlobalName(const long magicNumber)
{
   return "GP_PAUSE_" + IntegerToString(magicNumber);
}

bool IsBotPausedByPortfolioManager(const long magicNumber)
{
   string name = PortfolioPauseGlobalName(magicNumber);
   return GlobalVariableCheck(name) && GlobalVariableGet(name) != 0.0;
}

#endif // GESTORPORTAFOLIO_PORTFOLIO_PARTICIPANT_MQH
```

## Revisión técnica hecha desde SQX_Library antes de entregar esto (2026-07-11)

- **Verificado con documentación oficial de MQL5:** las variables globales de terminal expiran a las 4 semanas *sin acceso* — pero leerlas también cuenta como acceso, no solo escribirlas. Como cada bot llama a `IsBotPausedByPortfolioManager()` en cada tick, el propio diseño ya se auto-refresca; no hace falta nada extra para evitar la expiración. Fuente: [MQL5 Programming Basics: Global Variables](https://www.mql5.com/en/articles/2744), [GlobalVariableTime — documentación oficial](https://www.mql5.com/en/docs/globals/globalvariabletime).
- **Sugerencia no pedida, pero barata de agregar:** las variables globales se pueden perder si el terminal se cierra de forma anormal (crash) antes de un apagado normal. Si es importante que el estado de pausa sobreviva a eso, GestorPortafolio debería llamar `GlobalVariablesFlush()` justo después de escribir cada pausa, para forzar la escritura a disco inmediata.
- **Gap honesto de SQX_Library, no de la especificación:** el árbol de reglas interno de SQX (verificado en las candidatas reales de `EURUSD-REVRANGE-H1-001`) sí separa estructuralmente "Long entry"/"Short entry" (acciones `EnterAtMarket`/`EnterAtLimit`) de "Long exit"/"Short exit" (`CloseAllPositions`) — la distinción entre "abrir algo nuevo" y "gestionar lo abierto" existe en la lógica interna de SQX. Pero **todavía no se exportó ningún bot real a `.mq5` en este proyecto** (pendiente, ver `proceso-exportacion-live.md`) — no hay confirmación directa de cómo se ve el código MQL5 generado. Quien implemente esto sobre un bot real exportado tiene que ubicar el bloque de envío de orden nueva en ESE archivo concreto, no asumirlo de la estructura XML interna de SQX.

## Estado

Especificación completa y lista para aplicar en cuanto haya un bot real exportado desde una plantilla de esta biblioteca hacia una cuenta gestionada por GestorPortafolio. No se ha aplicado todavía — sigue sin existir ningún export real de esta biblioteca a MT5.
