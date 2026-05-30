package com.app.notespese.util

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Calcola il periodo [start, end] per un dato mese/anno tenendo conto del
 * giorno di inizio personalizzato (1-28).
 * Con giornoInizio=1 il risultato è il mese solare standard.
 * Con giornoInizio>1 il periodo inizia il giorno indicato e finisce il giorno
 * prima dello stesso giorno del mese successivo, con clamping all'ultimo
 * giorno valido del mese (es. giornoInizio=30 a febbraio → 28 feb).
 */
fun calcolaPeriodo(giornoInizio: Int, mese: Int, anno: Int): Pair<LocalDate, LocalDate> {
    if (giornoInizio <= 1) {
        return LocalDate.of(anno, mese, 1) to YearMonth.of(anno, mese).atEndOfMonth()
    }
    val ymCorrente   = YearMonth.of(anno, mese)
    val ymSuccessivo = ymCorrente.plusMonths(1)
    val startDay = minOf(giornoInizio, ymCorrente.lengthOfMonth())
    val endDay   = minOf(giornoInizio, ymSuccessivo.lengthOfMonth())
    val start = LocalDate.of(anno, mese, startDay)
    val end   = ymSuccessivo.atDay(endDay).minusDays(1)
    return start to end
}

/** Converte una LocalDate nel Timestamp Firebase corrispondente a mezzanotte locale. */
fun toTimestamp(date: LocalDate): Timestamp =
    Timestamp(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))

/** Etichetta leggibile del periodo (es. "1 gen – 31 gen" oppure "27 gen – 26 feb"). */
fun etichettaPeriodo(giornoInizio: Int, mese: Int, anno: Int): String {
    val (start, end) = calcolaPeriodo(giornoInizio, mese, anno)
    return if (giornoInizio <= 1) {
        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)
        start.format(fmt).replaceFirstChar { it.uppercase() }
    } else {
        val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
        "${start.format(fmt)} – ${end.format(fmt)}"
    }
}
