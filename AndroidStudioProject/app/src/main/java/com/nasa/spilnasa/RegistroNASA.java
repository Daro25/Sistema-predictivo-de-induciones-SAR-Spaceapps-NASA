package com.nasa.spilnasa;
import java.util.Date;

public class RegistroNASA {
    private final Date fecha;
    private final double valor;

    public RegistroNASA(Date fecha, double valor) {
        this.fecha = fecha;
        this.valor = valor;
    }

    public Date getFecha() { return fecha; }
    public double getValor() { return valor; }
}