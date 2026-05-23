package com.app.notespese.data.model

enum class Ruolo { ADMIN, MEMBRO }

enum class ModalitaSplit { COEFFICIENTE, CINQUANTA, PERSONALIZZATO }

enum class TipoSpesa { FISSA, VARIABILE }

enum class TipoCategoria { SPESA, ENTRATA, ENTRAMBI }

enum class StatoDebitore { IN_ATTESA, PAGATO }

enum class StatoCreditore { IN_ATTESA, CONFERMATO }

enum class TipoDebito { PRESTITO_FATTO, PRESTITO_RICEVUTO }

enum class StatoInvito { PENDING, ACCETTATO, RIFIUTATO, SCADUTO }
