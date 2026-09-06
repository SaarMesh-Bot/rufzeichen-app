package de.hamlookup.rufzeichen.data.tools

import de.hamlookup.rufzeichen.data.local.QsoEntity

/** Builds an ADIF (.adi) document from logged QSOs. Pure string generation. */
object Adif {

    private fun field(name: String, value: String?): String {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return ""
        return "<$name:${v.length}>$v "
    }

    fun export(qsos: List<QsoEntity>): String {
        val sb = StringBuilder()
        sb.append("Rufzeichen – Amateurfunk · ADIF-Export\n")
        sb.append("<ADIF_VER:5>3.1.4 ")
        sb.append("<PROGRAMID:10>Rufzeichen ")
        sb.append("<EOH>\n")
        for (q in qsos) {
            sb.append(field("CALL", q.callsign.uppercase()))
            sb.append(field("QSO_DATE", q.dateYmd))
            sb.append(field("TIME_ON", q.timeHm))
            sb.append(field("BAND", q.band?.lowercase()))
            sb.append(field("MODE", q.mode?.uppercase()))
            sb.append(field("RST_SENT", q.rstSent))
            sb.append(field("RST_RCVD", q.rstRcvd))
            sb.append(field("NAME", q.name))
            sb.append(field("GRIDSQUARE", q.grid?.uppercase()))
            sb.append(field("COMMENT", q.comment))
            sb.append("<EOR>\n")
        }
        return sb.toString()
    }
}
