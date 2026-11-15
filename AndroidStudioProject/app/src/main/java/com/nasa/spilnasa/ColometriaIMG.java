package com.nasa.spilnasa;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.widget.ImageView;

public class ColometriaIMG {
private ImageView imageView;

    public ColometriaIMG(ImageView imageView) {
        this.imageView = imageView;
    }
    public ImageView convertImage(int alertaFase, int alturaAgua) {
        Drawable drawable = imageView.getDrawable();
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];

        bitmap.getPixels(
                pixels, 0, width,
                0, 0, width, height
        );

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int pixel = pixels[index];

                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;
                int altura = rgbToHeight(red,green,blue) - alturaAgua;
                if(alertaFase == 0) pixels[index] = Color.TRANSPARENT;
                else if (alertaFase == 1) {
                    if (altura <= 84) {
                        float potencialInverso = (altura * 255f) / 84f;
                        float potencial = 255f - potencialInverso;
                        // Limitar el valor al rango 0-255
                        potencial = Math.max(0f, Math.min(255f, potencial));
                        int color = Color.rgb((int) potencial, 255,255);
                        pixels[index] = color;
                    } else if (altura > 84 && altura <= 110) {
                        int newAltura = altura - 84;
                        float potencialInverso = (newAltura * 255f) / 26f;
                        float potencial = 255f - potencialInverso;
                        // Limitar el valor al rango 0-255
                        potencial = Math.max(0f, Math.min(255f, potencial));
                        int color = Color.rgb(255,(int) potencial, 0);
                        pixels[index] = color;
                    } else if (altura > 110 && altura <= 119) {
                        int newAltura = altura - 110;
                        float potencialInverso = (newAltura * 255f) / 19f;
                        float potencial = 255f - potencialInverso;
                        // Limitar el valor al rango 0-255
                        potencial = Math.max(0f, Math.min(255f, potencial));
                        int color = Color.rgb((int) potencial, (int) potencial,0);
                        pixels[index] = color;
                    }else if (altura > 119 && altura <= 137) {
                        int newAltura = altura - 119;
                        float potencialInverso = (newAltura * 255f) / 18f;
                        float potencial = 255f - potencialInverso;
                        // Limitar el valor al rango 0-255
                        potencial = Math.max(0f, Math.min(255f, potencial));
                        int color = Color.rgb(255, (int) potencial,255);
                        pixels[index] = color;
                    }
                }
            }
        }
        // Convertir el arreglo de píxeles en Bitmap
        Bitmap nuevoBitmap = Bitmap.createBitmap(
                pixels,
                width,
                height,
                Bitmap.Config.ARGB_8888
        );
        imageView.setImageBitmap(nuevoBitmap);
        return imageView;
    }
    public static int rgbToHeight(int red, int green, int blue) {

        // Paleta RGB y alturas correspondientes
        double[][] palette = {
                {255, 165.04548016924582, 165.04548016924582, 137},
                {255, 110.28075022219673, 110.28075022219673, 128},
                {255, 54.86228903032509, 54.86228903032509, 119},
                {255, 0.26815998270762975, 0, 110},
                {255, 57.17929342598495, 0, 101},
                {255, 114.95323154988785, 0, 92},
                {255, 167.11232714671087, 0, 84},
                {255, 226.8072508467411, 0, 75},
                {230.10922824252566, 255, 0, 67},
                {174.9229171969556, 255, 0, 59},
                {125.59184893855206, 255, 0, 52},
                {67.8106628742363, 255, 0, 44},
                {15.781897412342914, 255, 0, 37},
                {0, 255, 36.94461309192724, 30},
                {0, 255, 84.70397162060863, 24},
                {0, 255, 134.54398928829872, 18},
                {0, 255, 187.32804366594164, 12},
                {0, 255, 234.88369921489408, 7},
                {0, 221.6859182686946, 255, 2},
                {0, 191, 255, 0}
        };

        double minDistance = Double.MAX_VALUE;
        int height = 0;

        for (double[] color : palette) {

            double dr = red - color[0];
            double dg = green - color[1];
            double db = blue - color[2];

            double distance = dr * dr + dg * dg + db * db;

            if (distance < minDistance) {
                minDistance = distance;
                height = (int) color[3];
            }
        }

        return height;
    }
    public ImageView getImageView() {
        return imageView;
    }
    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }
}
