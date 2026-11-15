package com.nasa.spilnasa;


import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "NASA_WORLDVIEW";
    private static final String URL_BASE =
            "https://worldview.earthdata.nasa.gov/service/imagestat/get_stats";
    private static final long MS_DIA = 24L * 60 * 60 * 1000;
    private static final double AREA_M2 = 2578715915.0;          // D19
    private static final double VOLUMEN_SEGURO = 38680738.73;    // D22

    //inputs y views
    private ImageView mapaView;
    private TextView tvPrecipitation, tvHumidity, tvAccumulatedPrecipitation, tvHumidityIndex;
    private LinearLayout colorAlert;
    private TextView tvAlertLevel, tvAlertSubtitle, tvAlertDescription;
    public ColometriaIMG colometriaIMG;
    private double hMax = 0, indice = 0, acum = 0, volumen;
    private String alerta = "", alertaAcum = "";
    private float precipitacionOne = 0.0f;
    private int fase = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // definición de views
        mapaView = (ImageView) findViewById(R.id.mapaIMG);
        tvPrecipitation = getTextView(R.id.tvPrecipitation);
        tvHumidity = getTextView(R.id.tvHumidity);
        tvAccumulatedPrecipitation = getTextView(R.id.tvAccumulatedPrecipitation);
        tvHumidityIndex = getTextView(R.id.tvHumidityIndex);
        colorAlert = (LinearLayout) findViewById(R.id.colorAlert);
        tvAlertLevel = getTextView(R.id.tvAlertLevel);
        tvAlertSubtitle = getTextView(R.id.tvAlertSubtitle);
        tvAlertDescription = getTextView(R.id.tvAlertDescription);
        colometriaIMG = new ColometriaIMG(mapaView);
        consultarAmbasVariables();
    }
    private TextView getTextView(int id){
        return (TextView) findViewById(id);
    }

    private void consultarAmbasVariables() {

        new Thread(() -> {

            // CONSULTA 1: AIRS - HUMEDAD RELATIVA
            List<RegistroNASA> humedad = consultarNASA(
                    "AIRS_L2_Surface_Relative_Humidity_Day",
                    "AIRS_RelativeHumidity.xml",
                    "HUMEDAD AIRS"
            );

            // CONSULTA 2: IMERG - PRECIPITACIÓN
            List<RegistroNASA> precipitacion = consultarNASA(
                    "IMERG_Precipitation_Rate",
                    "GPM_Precipitation_Rate.xml",
                    "PRECIPITACIÓN IMERG"
            );

            // Validar resultados
            if (precipitacion == null || precipitacion.isEmpty()
                    || humedad == null || humedad.isEmpty()) {
                Log.e(TAG, "No hay suficientes datos NASA");
                return;
            }

            // ALERTA Y TABLA COMPLETA
            long inicio = precipitacion.get(0).getFecha().getTime();

            for (RegistroNASA p : precipitacion) {

                if (p.getFecha().getTime() < inicio + 3 * MS_DIA)
                    continue;

                Date fecha = p.getFecha();

                hMax = maxHumedad3Dias(humedad, fecha);
                indice = IndiceHumedad.calcular(humedad, fecha);
                acum = acumulada3Dias(precipitacion, fecha);

                if (Double.isNaN(hMax) || Double.isNaN(indice))
                    continue;

                alerta = evaluarAlerta(hMax, p.getValor(), acum);
                volumen = calcularVolumen(indice, acum);

                alertaAcum = volumen > VOLUMEN_SEGURO
                        ? "Alta" : "Baja";

                Log.d(TAG, fecha
                        + " | Indice: " + indice
                        + " | HumMax: " + hMax
                        + " | Acum: " + acum
                        + " | Alerta: " + alerta
                        + " | Volumen: " + volumen
                        + " | Alerta acumulada: " + alertaAcum);
            }

            // Última precipitación
            precipitacionOne = (float)
                    precipitacion.get(precipitacion.size() - 1).getValor();

            // Determinar fase
            switch (alerta) {
                case "Verde": fase = 1; break;
                case "Amarillo": fase = 2; break;
                case "Naranja": fase = 3; break;
                case "Rojo": fase = 4; break;
                default: fase = 0; break;
            }

            // Actualizar interfaz en el hilo principal
            runOnUiThread(() -> {
                tvHumidityIndex.setText(vistaPresent((float)indice*100) + "%");
                tvHumidity.setText(vistaPresent((float)hMax) + "%");
                tvAccumulatedPrecipitation.setText(vistaPresent((float)acum) + "mm/h");
                tvPrecipitation.setText(vistaPresent(precipitacionOne) + "mm/h");
                colometriaIMG.convertImage(
                        fase,
                        (int) (acum * 24 / 1000)
                );
                Log.d(TAG, "Calculado ====================");
            });
        }).start();
    }

    private List<RegistroNASA> consultarNASA(
            String layer,
            String colormap,
            String nombreVariable) {

        HttpURLConnection connection = null;
        List<RegistroNASA> registros = new ArrayList<>();


        try {

            String parametros =
                    "_type=series" +
                            "&timestamp=2025-10-01" +
                            "&steps=31" +
                            "&layer=" + URLEncoder.encode(layer, "UTF-8") +
                            "&colormap=" + URLEncoder.encode(colormap, "UTF-8") +
                            "&bbox=20.7283,-97.3204,21.1368,-96.9119" +
                            "&bins=10" +
                            "&end_timestamp=2025-10-11";

            URL url = new URL(URL_BASE + "?" + parametros);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);

            // Encabezados
            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36"
            );

            connection.setRequestProperty(
                    "Referer",
                    "https://worldview.earthdata.nasa.gov/"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json, text/plain, */*"
            );

            connection.setRequestProperty(
                    "Accept-Language",
                    "es-ES,es;q=0.9,en;q=0.8"
            );

            // Ejecutar petición
            int codigo = connection.getResponseCode();

            InputStream stream;

            if (codigo == HttpURLConnection.HTTP_OK) {
                stream = connection.getInputStream();
            } else {
                stream = connection.getErrorStream();
            }

            StringBuilder respuesta = new StringBuilder();

            if (stream != null) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, "UTF-8")
                );

                String linea;

                while ((linea = reader.readLine()) != null) {
                    respuesta.append(linea);
                }

                reader.close();
            }
            // Procesar resultados
            if (codigo == HttpURLConnection.HTTP_OK) {
                JSONObject data = new JSONObject(respuesta.toString());
                JSONObject mean = data.getJSONObject("mean");
                registros = parsearMean(mean, false);
                for (RegistroNASA r : registros) {
                    Log.d(TAG, nombreVariable + " | " + r.getFecha() + " = " + r.getValor());
                }
            } else {
                Log.e(TAG, "VARIABLE: " + nombreVariable);
                Log.e(TAG, "Error HTTP: " + codigo);
                Log.e(TAG, "Respuesta: " + respuesta);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en " + nombreVariable, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return registros;
    }
    private List<RegistroNASA> parsearMean(JSONObject mean, boolean ignorarCeros) throws Exception {
        List<RegistroNASA> lista = new ArrayList<>();

        SimpleDateFormat formato =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));

        Iterator<String> claves = mean.keys();
        while (claves.hasNext()) {
            String clave = claves.next();
            double valor = mean.optDouble(clave, Double.NaN);
            if (!Double.isNaN(valor) && !(ignorarCeros && valor == 0)) {
                lista.add(new RegistroNASA(formato.parse(clave), valor));
            }
        }

        Collections.sort(lista, (a, b) -> a.getFecha().compareTo(b.getFecha()));
        return lista;
    }


    private double maxHumedad3Dias(List<RegistroNASA> humedad, java.util.Date fecha) {
        double max = Double.NaN;
        for (int k = 1; k <= 3; k++) {
            double v = buscarValor(humedad, new java.util.Date(fecha.getTime() - k * MS_DIA));
            if (!Double.isNaN(v) && (Double.isNaN(max) || v > max)) {
                max = v;
            }
        }
        return max;
    }

    private double acumulada3Dias(List<RegistroNASA> precip, java.util.Date fecha) {
        double suma = 0;
        for (int k = 1; k <= 3; k++) {
            double v = buscarValor(precip, new java.util.Date(fecha.getTime() - k * MS_DIA));
            if (!Double.isNaN(v)) {
                suma += v;
            }
        }
        return suma;
    }

    // Traducción de tu fórmula SI.CONJUNTO
    private String evaluarAlerta(double humedadMax, double lluvia, double acumulada) {
        double h = humedadMax / 100.0;   // el Excel compara contra 0.4 y 0.6
        if (h <= 0.0f){
            return "null";
        }
        else if (h < 0.4 && h> 0 ) {
            return "Verde";
        } else if (h >= 0.4 && h < 0.6 && lluvia >= 5) {
            return "Amarillo";
        } else if (h > 0.6 && lluvia >= 2) {
            return "Naranja";
        } else if (h > 0.6 && (acumulada >= 10 || acumulada == 8)) {
            return "Rojo";
        } else {
            return "Verde";
        }
    }
    private double buscarValor(List<RegistroNASA> lista, java.util.Date fecha) {
        for (RegistroNASA r : lista) {
            if (r.getFecha().getTime() == fecha.getTime()) {
                return r.getValor();
            }
        }
        return Double.NaN;
    }
    private double calcularVolumen(double indice, double acumulada) {
        return indice * acumulada * (1.0 / 1000) * 24 * AREA_M2;
    }
    private String vistaPresent(float numero) {
        return  String.format("%.2f", numero);
    }
}
