---
paths: SQX_Library/**
cargar_en: Cada vez que se edite un `project.cfx` directamente (sin pasar por la interfaz de SQ) — agregar/quitar tareas, cambiar configuración de archivo
naturaleza: ESTABLE — mecanismo técnico crítico, condiciona si el asistente puede armar pipelines de forma autónoma
---

# Cómo editar un `project.cfx` de forma segura sin romper el proyecto

Un `project.cfx` es un ZIP (confirmado desde el inicio de este proyecto) que contiene `config.xml` (secuencia de tareas + lista de databanks) y un archivo `<TipoTarea>-TaskN.xml` por cada tarea. Esta regla documenta **cómo editarlo sin corromperlo** — un incidente real (2026-07-13) mostró que no cualquier método de escritura de ZIP funciona.

## Incidente real: el modo "Update" de .NET corrompe el proyecto para SQ

Se intentó agregar una tarea nueva (`Clear Databanks`) editando el `project.cfx` con el proyecto cerrado, usando `System.IO.Compression.ZipFile.Open(path, 'Update')` (abrir el ZIP existente, borrar una entrada, agregar otra, sin reconstruir el archivo completo). **Resultado: SQ mostró "Config file... is corrupted"** al abrir el proyecto — a pesar de que herramientas genéricas (`unzip -t`, listado de archivos) no detectaban ningún error en el ZIP. El lector de ZIP que usa SQ internamente (Java) es más estricto que las herramientas genéricas sobre la estructura exacta que deja el modo "Update" de .NET tras borrar+agregar entradas.

**Se restauró desde backup** (siempre tomado antes de cada edición — ver más abajo) sin pérdida de datos reales (Build, databanks, todo el trabajo de sesiones anteriores intacto).

## Método confirmado que sí funciona: reconstrucción completa desde cero

En vez de editar el ZIP existente, **extraer todos los archivos, modificar lo que haga falta en las copias extraídas, y reconstruir el `.cfx` completo desde cero** con `ZipFile.Open(path, 'Create')` (no `'Update'`), agregando cada archivo con `CreateEntryFromFile`.

**Validado dos veces (2026-07-13):**
1. Un proyecto de prueba separado (`_test_zip_method`, descartable, no relacionado con ninguna plantilla real), reconstruido con contenido idéntico al proyecto real (sin cambios) — abrió sin error de corrupción, confirmando que el método de reconstrucción en sí es compatible con SQ.
2. Aplicado al proyecto real `EURUSD-REVRANGE-H1-001`, agregando de verdad la tarea `Clear Databanks` — **abrió correctamente, con la tarea nueva en la posición correcta y su configuración (`Databank name="Results"`) bien cargada**, confirmado por el usuario en la interfaz.

## Protocolo obligatorio para cualquier edición directa futura de un `project.cfx`

1. **El proyecto tiene que estar cerrado en SQ** antes de tocar el archivo (ya establecido desde antes en este proyecto, sigue vigente).
2. **Backup del `.cfx` actual** antes de cualquier cambio, guardado en `_backups/project_<fecha>_antes_de_<descripcion>.cfx` (convención ya usada en todo el proyecto).
3. **Extraer todos los archivos** del `.cfx` actual a una carpeta temporal.
4. **Editar solo las copias extraídas** (nunca el ZIP en el lugar).
5. **Reconstruir el `.cfx` completo desde cero** con `ZipFile.Open(path, 'Create')`, agregando explícitamente cada archivo (los que cambiaron y los que no).
6. **Verificar antes de reemplazar el archivo real:** construir el nuevo `.cfx` con un nombre temporal distinto primero (ej. `project_new.cfx`), validar con `unzip -t` (integridad del ZIP) y validación de buena formación XML de cada entrada (vía PowerShell, ya que no hay Python en este entorno) — recién después de esa doble verificación, reemplazar el `project.cfx` real.
7. **El usuario abre el proyecto y confirma visualmente** que carga sin error y que el cambio aparece correcto en la interfaz — la verificación técnica (paso 6) no reemplaza la confirmación real de que SQ lo acepta.

Este protocolo es lo que permite que el asistente arme pipelines completos de forma autónoma (editando XML directamente) para plantillas futuras, en vez de depender de que el usuario configure cada tarea a mano en la interfaz — era la pregunta abierta planteada por el usuario ("si no eres capaz de agregar las tareas por sí solo no podemos automatizar esto"), resuelta con este método verificado.

## Nota técnica de por qué probablemente fallaba el modo "Update"

No se hizo un análisis forense profundo del archivo corrupto (se sobrescribió al restaurar), pero es un problema conocido en general: `ZipArchiveMode.Update` de .NET, al borrar y volver a agregar entradas en un ZIP existente, puede dejar la estructura del archivo central (central directory) o el orden físico de las entradas de una forma que ZIP readers estrictos (como el de Java) no aceptan, aunque sea técnicamente válida para lectores más permisivos. Reconstruir desde cero (`'Create'`) evita ese problema por completo porque no hereda ninguna estructura del archivo anterior.
