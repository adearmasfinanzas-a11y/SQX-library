---
paths: SQX_Library/**
cargar_en: Cada vez que el usuario dé una instrucción nueva que deba persistir más allá de la conversación actual
naturaleza: META — reglas sobre cómo evolucionan las reglas
---

# Gobernanza del sistema de instrucciones

Este archivo define **cómo se incorpora un requisito nuevo** a este sistema, para que la biblioteca de instrucciones crezca de forma modular y no vuelva a convertirse en un único documento monolítico.

## 1. Señal de activación

Se trata esto como una instrucción persistente (no como una pregunta puntual de la conversación) cuando el usuario usa frases como: "recuerda que...", "a partir de ahora...", "nunca hagas...", "siempre que pase X, haz Y", "quiero que quede documentado que...", o cuando pide explícitamente actualizar el framework/las instrucciones.

Una pregunta puntual sobre una plantilla concreta **no** dispara este proceso — solo lo hace un requisito que debe aplicar a trabajo futuro.

## 2. Decisión: ¿raíz o regla modular?

| Si el requisito... | Va en... |
|---|---|
| Aplica a **toda** plantilla, sin condición (ej. una nueva prohibición absoluta) | `CLAUDE.md` (raíz), sección "Lo que nunca se hace" o el índice del protocolo |
| Aplica solo en un paso concreto del protocolo, o solo quece bajo cierta condición (activo, motor, tipo de artefacto) | Un archivo en `.claude/rules/` |
| Es una corrección/matiz de un tema ya cubierto | Se **edita** el archivo de regla existente — nunca se duplica el tema en un archivo nuevo |
| Es un tema genuinamente nuevo sin archivo que lo cubra | Se **crea** un archivo nuevo en `.claude/rules/`, nombre en `kebab-case`, un tema por archivo |

**Regla de tamaño:** si un archivo de reglas empieza a mezclar más de un tema claramente separable, se divide. Un archivo de reglas debe poder describirse en una frase.

## 3. Todo archivo nuevo debe ser referenciado

Un archivo en `.claude/rules/` que no está enlazado desde `CLAUDE.md` (raíz) mediante `@.claude/rules/archivo.md` en el paso del protocolo que corresponde **no se carga nunca** y por tanto no existe en la práctica. Crear el archivo y enlazarlo desde la raíz son un solo paso, no dos.

## 4. Registro de cambios del sistema

Cada alta, edición o baja de una regla se anota en `.claude/rules/_registro-cambios.md` (formato: fecha, archivo afectado, qué cambió, por qué). Esto es distinto del `changelog.md` de cada plantilla individual — este registro es sobre el propio sistema de instrucciones, no sobre las plantillas que produce.

## 5. Confirmación antes de escribir

Cualquier adición a la sección "Lo que nunca se hace" de la raíz (una prohibición dura, sin excepción) se confirma explícitamente con el usuario antes de escribirse, porque aplica de forma retroactiva a todo trabajo futuro sin excepción. Las reglas modulares condicionales (`.claude/rules/`) pueden proponerse y aplicarse de inmediato si el usuario ya las formuló con claridad, sin necesitar una segunda confirmación redundante.

## 6. Poda periódica

Si al trabajar se detecta que una regla existente describe algo que ya no es cierto (un motor que ya no se usa, una restricción operativa que cambió), se señala de inmediato — no se sigue aplicando en silencio ni se elimina sin avisar. Una regla desactualizada es peor que no tener regla, porque genera confianza falsa.
