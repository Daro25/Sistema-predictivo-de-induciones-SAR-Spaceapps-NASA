package com.nasa.spilnasa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

/**
 * Índice de Humedad (humedad antecedente de los 3 días previos):
 *
 *   Indice = (0.5 * HumedadAyer + 0.3 * HumedadHace2Dias + 0.2 * HumedadHace3Dias) / 100
 *
 * El resultado va de 0 a 1 y es el coeficiente "C" de la fórmula V = P * C * A.
 */
public class IndiceHumedad {

    private static final double PESO_AYER    = 0.5;
    private static final double PESO_HACE_2  = 0.3;
    private static final double PESO_HACE_3  = 0.2;
    private static final long   MS_POR_DIA   = 24L * 60 * 60 * 1000;

    /** Fórmula directa: recibe las 3 humedades (en %, de 0 a 100). */
    public static double calcular(double ayer, double hace2dias, double hace3dias) {
        return (PESO_AYER * ayer + PESO_HACE_2 * hace2dias + PESO_HACE_3 * hace3dias) / 100.0;
    }

    /**
     * Índice de humedad para una fecha concreta, usando la lista de humedad de NASA.
     * Si falta algún día (porque se descartó un 0 = sin datos), usa el último valor
     * disponible anterior a ese día. Devuelve NaN si no hay datos suficientes.
     */
    public static double calcular(List<RegistroNASA> humedad, Date fecha) {
        TreeMap<Long, Double> porDia = new TreeMap<>();
        for (RegistroNASA r : humedad) {
            porDia.put(r.getFecha().getTime() / MS_POR_DIA, r.getValor());
        }

        long dia = fecha.getTime() / MS_POR_DIA;

        Double ayer  = ultimoValorHasta(porDia, dia - 1);
        Double hace2 = ultimoValorHasta(porDia, dia - 2);
        Double hace3 = ultimoValorHasta(porDia, dia - 3);

        if (ayer == null || hace2 == null || hace3 == null) {
            return Double.NaN;
        }
        return calcular(ayer, hace2, hace3);
    }

    /** Índice de humedad para cada día de la lista (los primeros días sin historial se omiten). */
    public static List<RegistroNASA> calcularSerie(List<RegistroNASA> humedad) {
        List<RegistroNASA> serie = new ArrayList<>();
        if (humedad.isEmpty()) return serie;

        long primerDia = Long.MAX_VALUE, ultimoDia = Long.MIN_VALUE;
        for (RegistroNASA r : humedad) {
            long d = r.getFecha().getTime() / MS_POR_DIA;
            primerDia = Math.min(primerDia, d);
            ultimoDia = Math.max(ultimoDia, d);
        }

        for (long d = primerDia + 1; d <= ultimoDia + 1; d++) {
            Date fecha = new Date(d * MS_POR_DIA);
            double indice = calcular(humedad, fecha);
            if (!Double.isNaN(indice)) {
                serie.add(new RegistroNASA(fecha, indice));
            }
        }
        Collections.sort(serie, (a, b) -> a.getFecha().compareTo(b.getFecha()));
        return serie;
    }

    /** Valor del día indicado, o el más reciente anterior a él. */
    private static Double ultimoValorHasta(TreeMap<Long, Double> porDia, long dia) {
        java.util.Map.Entry<Long, Double> e = porDia.floorEntry(dia);
        return e == null ? null : e.getValue();
    }
}
