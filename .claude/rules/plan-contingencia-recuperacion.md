---
paths: SQX_Library/**
cargar_en: Cuando exista al menos una plantilla funcional completa (portafolio-final validado) — no accionable antes, registrado para atar cabos sueltos de seguridad del trabajo
naturaleza: MARCO DE DISEÑO — plan propuesto con análisis crítico, no implementado
---

# Plan de contingencia / recuperación ante pérdida de la máquina

Pedido explícito del usuario (2026-07-13): un plan simple de reconstruir todo desde cero si la máquina se dañara, y un sistema de sincronización de las estrategias (no el código de `SQX_Library`, que ya tiene su propio backup vía git) a otro disco local o remoto — con el criterio de que el proyecto no dependa únicamente del directorio actual, salvo lo que **tiene** que vivir dentro de la instalación de SQ. Se pidió análisis crítico, no implementación — se retoma recién cuando haya una primera plantilla funcional completa.

## Análisis crítico: qué tan lejos estamos ya (más de lo que parece)

Varias decisiones tomadas en la sesión del 2026-07-12/13 ya resuelven la mayor parte de esto sin que fuera el objetivo explícito en su momento:

- **El código/documentación (`SQX_Library`) ya tiene backup real e independiente** — repo Git privado en GitHub (`github.com/adearmasfinanzas-a11y/SQX-library`), incluida una copia de los 39 archivos Java de indicadores custom (`_indicadores/codigo_fuente_java/`), aunque la copia "viva" que usa SQ para compilar vive dentro de la instalación (`user/extend/Snippets/`).
- **Las estrategias en bruto (materia prima cara de regenerar) ya viven fuera de la instalación** — `Reversion-Media\_runs\<fecha>_corridaN\`, decisión ya tomada y justificada en `pipeline-multitarea-y-diseno-is.md` sección 3g.
- **La configuración de cada tarea del pipeline ya está archivada como texto** (presets `.cfx`/`.xml` en `EURUSD/ReversionRango/pipeline_tareas/`, versionados en git) — esto es clave: no hace falta recordar de memoria cómo se configuró cada tarea, está todo documentado con evidencia real.
- **Existe un método validado para reconstruir un `project.cfx` completo por fuera de la interfaz** (`mecanismo-edicion-directa-proyectos.md`) — relevante para la reconstrucción rápida, no solo para armar tareas nuevas.

**Lo que falta no es infraestructura nueva — es (1) un runbook documentado paso a paso, y (2) un mecanismo de sincronización periódica, ninguno de los dos complejo.**

## 1. Runbook de reconstrucción desde cero (propuesto)

1. Reinstalar StrategyQuant X (proceso propio del vendor, fuera de nuestro alcance).
2. Clonar `SQX_Library` desde GitHub a la ubicación que se prefiera (ya no depende de estar dentro de la instalación, gracias al backup real).
3. Copiar los archivos Java de `SQX_Library/_indicadores/codigo_fuente_java/` a `user/extend/Snippets/SQ/Blocks/` (la ubicación que SQ **sí** necesita, es un requisito de la instalación, no una elección nuestra) — abrir Code Editor, "Compile All".
4. Para cada plantilla activa: crear un proyecto nuevo en `user/projects/` con el mismo nombre, y reconstruir `project.cfx` a partir de los presets archivados (`pipeline_tareas/*.cfx`) — dos caminos posibles, a decidir en su momento: (a) cargar cada preset manualmente desde la interfaz con "Load config" tarea por tarea, o (b) usar el método de reconstrucción directa ya validado para armar el `.cfx` completo de una — probablemente más rápido dado que ya está probado.
5. Recargar la materia prima: tarea nativa `LoadFromFiles` (ya catalogada, no investigada en profundidad todavía) apuntando al backup de `Reversion-Media`, hacia un databank `Results` limpio.
6. Re-correr el pipeline desde `Filtering` en adelante — **no hace falta re-correr el Build**, la materia prima ya está a salvo. Esto es justamente por qué el `SaveToFiles` del bruto es obligatorio (ver sección 1b de `pipeline-multitarea-y-diseno-is.md`) — sin eso, este runbook no sería posible sin volver a pagar el costo completo del Build.

## 2. Sincronización periódica de `Reversion-Media` a otro disco (propuesto)

**Recomendación: `robocopy` en modo espejo (`/MIR`) + Programador de tareas de Windows, no un vigilante de archivos en tiempo real.** Razón: los archivos de esta biblioteca se generan **en lotes**, después de cada corrida de pipeline, no de forma continua — un espejo periódico (diario, o disparado manualmente al cerrar una corrida) es más simple, más robusto y con más historial de uso comprobado que un `FileSystemWatcher` en vivo, que puede perderse eventos o toparse con archivos bloqueados durante la escritura.

- **Destino local:** otro disco físico o lógico de la misma máquina, si existe.
- **Destino remoto:** en vez de programar subida a la nube nosotros mismos, apuntar el espejo de `robocopy` a una carpeta sincronizada por un cliente de nube ya instalado (OneDrive, Google Drive Desktop, si el usuario tiene uno) — el cliente de nube se encarga de la subida real, nosotros solo garantizamos que el archivo llegue a esa carpeta local sincronizada.
- No necesita ser silencioso/invisible: dejar un log simple de la última sincronización exitosa, para que el usuario sepa si algo falló sin tener que confiar ciegamente en que "está andando".

## 3. Hallazgo crítico real: los backups de `_backups/` no sobreviven una pérdida total de la máquina

Los backups de `project.cfx` que se vienen guardando toda la sesión (`user/projects/<plantilla>/_backups/`) **viven dentro de la instalación de SQ** — no sobrevivirían si la máquina se dañara del todo. Esto no es grave por sí solo (el runbook de la sección 1 no depende de ellos, reconstruye desde los presets ya versionados en git + la materia prima en `Reversion-Media`), pero **sí conviene incluir esa carpeta en la sincronización periódica de la sección 2** — da una recuperación más rápida y cómoda (restaurar un backup reciente) sin tener que pasar por todo el runbook completo para cambios menores.

## Opinión general (pedida explícitamente por el usuario)

El pedido es prudente y no está fuera de lugar — cuanto más crece esta biblioteca, más caro sería perderla. Mi lectura crítica: **no hace falta sobre-construir esto** (no hace falta un sistema de sincronización sofisticado, ni monitoreo en tiempo real, ni infraestructura de nube propia) — con lo que ya se decidió hoy (separación real/código, backup git, materia prima fuera de la instalación, presets de tareas documentados), el 80% del trabajo duro ya está hecho. Lo que realmente falta es barato: escribir el runbook como una lista de pasos clara (no un script complejo), y programar una tarea de `robocopy` — ambos triviales de ejecutar cuando llegue el momento, y ninguno urgente hoy porque el Build ya está a salvo.

## Estado

Registrado el 2026-07-13, **no implementado**. Se retoma cuando exista al menos una plantilla con portafolio final validado — recién ahí tiene sentido invertir en el runbook y la sincronización, no antes (con solo esta plantilla en curso, el riesgo real es bajo y ya está parcialmente cubierto).
