package com.nasa.spilnasa;


import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NASA_WORLDVIEW";

    private static final String URL_BASE =
            "https://worldview.earthdata.nasa.gov/service/imagestat/get_stats";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        consultarAmbasVariables();
    }

    private void consultarAmbasVariables() {

        new Thread(() -> {

            // CONSULTA 1: AIRS - HUMEDAD RELATIVA
            consultarNASA(
                    "AIRS_L2_Surface_Relative_Humidity_Day",
                    "AIRS_RelativeHumidity.xml",
                    "HUMEDAD AIRS"
            );

            // CONSULTA 2: IMERG - PRECIPITACIÓN
            consultarNASA(
                    "IMERG_Precipitation_Rate",
                    "GPM_Precipitation_Rate.xml",
                    "PRECIPITACIÓN IMERG"
            );

        }).start();
    }

    private void consultarNASA(
            String layer,
            String colormap,
            String nombreVariable) {

        HttpURLConnection connection = null;

        try {

            String parametros =
                    "_type=series" +
                            "&timestamp=2026-10-1" +
                            "&steps=31" +
                            "&layer=" + URLEncoder.encode(layer, "UTF-8") +
                            "&colormap=" + URLEncoder.encode(colormap, "UTF-8") +
                            "&bbox=20.7283,-97.3204,21.1368,-96.9119" +
                            "&bins=10" +
                            "&end_timestamp=2026-10-11";

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

                Object mean = data.opt("mean");

                Log.d(TAG, "================================");
                Log.d(TAG, "VARIABLE: " + nombreVariable);
                Log.d(TAG, "Métricas obtenidas correctamente");
                Log.d(TAG, "Promedios por fecha (mean): " + mean);
                Log.d(TAG, "================================");


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
    }
}
