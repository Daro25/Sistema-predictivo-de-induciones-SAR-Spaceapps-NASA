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

        if (imageView == null || imageView.getDrawable() == null) {
            return imageView;
        }

        if (!(imageView.getDrawable() instanceof BitmapDrawable)) {
            return imageView;
        }

        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width * height];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Colores de la escala de riesgo
        final int ROJO = Color.rgb(229, 57, 53);
        final int NARANJA = Color.rgb(251, 140, 0);
        final int AMARILLO = Color.rgb(253, 216, 53);
        final int VERDE = Color.rgb(67, 160, 71);

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                int index = y * width + x;
                int pixel = pixels[index];

                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                // Obtener elevación y restar nivel del agua
                int altura = rgbToHeight(red, green, blue) - alturaAgua;

                if (alertaFase == 0) {
                    pixels[index] = Color.TRANSPARENT;
                    continue;
                }

                // Clasificación de riesgo
                if (altura <= 84) {

                    pixels[index] = ROJO;

                } else if (altura <= 110) {

                    pixels[index] = NARANJA;

                } else if (altura <= 119) {

                    pixels[index] = AMARILLO;

                } else if (altura <= 137) {

                    pixels[index] = VERDE;
                }
            }
        }

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
