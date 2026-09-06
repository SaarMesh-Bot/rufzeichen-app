package de.hamlookup.rufzeichen.data.tools

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure amateur-radio calculations. No Android dependencies, fully offline.
 * Formulas use the common practical constants (e.g. a ~0.95 velocity/end-effect
 * factor for wire antennas); results are guideline values, not a substitute for
 * measurement/tuning.
 */
object RadioMath {

    private const val C = 299.792458 // speed of light, Mm/s -> gives metres for MHz

    fun wavelengthM(fMHz: Double): Double = C / fMHz

    /** Half-wave dipole, practical length incl. end effect (≈143/f). */
    fun dipoleHalfWaveM(fMHz: Double): Double = 142.5 / fMHz

    /** Quarter-wave vertical/radiator. */
    fun quarterWaveM(fMHz: Double): Double = 71.25 / fMHz

    /** Full-wave loop (≈306/f). */
    fun fullWaveLoopM(fMHz: Double): Double = 306.0 / fMHz

    // ---- power / level ----
    fun wattToDbm(w: Double): Double = 30.0 + 10.0 * log10(w)
    fun dbmToWatt(dbm: Double): Double = 10.0.pow((dbm - 30.0) / 10.0)
    fun wattToDbw(w: Double): Double = 10.0 * log10(w)

    /** Result of an EIRP/ERP computation. */
    data class Eirp(val eirpW: Double, val erpW: Double, val eirpDbm: Double)

    /**
     * @param txW transmitter output power in watts
     * @param cableLossDb feedline loss in dB (use CoaxData for a value)
     * @param antennaGainDbi antenna gain in dBi
     */
    fun eirp(txW: Double, cableLossDb: Double, antennaGainDbi: Double): Eirp {
        val eirpDbm = wattToDbm(txW) - cableLossDb + antennaGainDbi
        val eirpW = dbmToWatt(eirpDbm)
        // ERP is referenced to a dipole: dBi = dBd + 2.15 -> ERP = EIRP - 2.15 dB
        val erpW = eirpW / 10.0.pow(2.15 / 10.0)
        return Eirp(eirpW = eirpW, erpW = erpW, eirpDbm = eirpDbm)
    }

    /** dBd (over dipole) -> dBi (isotropic). */
    fun dbdToDbi(dbd: Double): Double = dbd + 2.15

    /** Power remaining after a given loss. */
    fun powerAfterLossW(txW: Double, lossDb: Double): Double = txW * 10.0.pow(-lossDb / 10.0)

    /** Interpolate cable loss (dB/100 m) at f, scaling by sqrt(f) between refs. */
    fun interpLossPer100m(refs: List<Pair<Double, Double>>, fMHz: Double): Double {
        if (refs.isEmpty()) return 0.0
        val sorted = refs.sortedBy { it.first }
        if (fMHz <= sorted.first().first) return sorted.first().second
        if (fMHz >= sorted.last().first) {
            // extrapolate from the top two on a sqrt(f) basis
            val a = sorted[sorted.size - 2]; val b = sorted.last()
            return scaleSqrt(a, b, fMHz)
        }
        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]; val b = sorted[i + 1]
            if (fMHz in a.first..b.first) return scaleSqrt(a, b, fMHz)
        }
        return sorted.last().second
    }

    private fun scaleSqrt(a: Pair<Double, Double>, b: Pair<Double, Double>, f: Double): Double {
        val xa = sqrt(a.first); val xb = sqrt(b.first); val x = sqrt(f)
        if (xb == xa) return a.second
        val t = (x - xa) / (xb - xa)
        return a.second + t * (b.second - a.second)
    }
}
