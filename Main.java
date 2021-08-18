
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.qrcode.decoder.Decoder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

class Point {
    int x, y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

public class Main {
    private static BitMatrix readQRImage(String filename, int codeWidth) throws IOException {
        FileInputStream in = new FileInputStream(filename);

        BitMatrix matrix = new BitMatrix(codeWidth, codeWidth);

        for (int y = 0; y < codeWidth; y++) {
            for (int x = 0; x < codeWidth; x++) {
                int r = in.read();

                if (r > 200) {
                    matrix.unset(x, y);
                } else {
                    matrix.set(x, y);
                }
            }
        }

        return matrix;
    }
    
    //This could probably be intergrated better somehow, but for now this will do
    private static Point[] generateUnknownPixels(String filename, int codeWidth) throws IOException {
        FileInputStream in = new FileInputStream(filename);

        ArrayList<Point> pointList = new ArrayList<Point>();

        for (int y = 0; y < codeWidth; y++) {
            for (int x = 0; x < codeWidth; x++) {
                int r = in.read();
                int g = in.read();

                if (r > 200 && g < 20) {
                    pointList.add(new Point(x, y));
                    System.err.println("Missing point added, x:"+x+", y:"+y);
                }
            }
        }

        Point[] finished_arr = {};
        finished_arr = pointList.toArray(finished_arr);

        return finished_arr;
    }



    private static void recurseGuessPixels(BitMatrix matrix, int pixelIndex, Point[] unknownPixels, Decoder decoder) throws FormatException {
        if (pixelIndex >= unknownPixels.length) {
            try {
                // The decoder modifies the matrix, so we must clone it here so the caller's copy is not messed up
                DecoderResult result = decoder.decode(matrix.clone());

                String resultText = result.getText();
                char [] resultChars = resultText.toCharArray();

                // Make sure the decoded characters are in range for ASCII text:
                for (int i = 0; i < resultChars.length; i++) {
                    if ((int) resultChars[i] < 32 || (int) resultChars[i] > 126) {
                        // Failed.
                        return;
                    }
                }

                System.out.println(resultText);
            } catch (ChecksumException e) {
            }
        } else {
            // Run 32 threads
            if (pixelIndex < 5) {
                Thread[] threads = new Thread[]{
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BitMatrix newMatrix = matrix.clone();

                            newMatrix.unset(unknownPixels[pixelIndex].x, unknownPixels[pixelIndex].y);
                            try {
                                recurseGuessPixels(newMatrix, pixelIndex + 1, unknownPixels, new Decoder());
                            } catch (FormatException e) {
                                System.out.println(e);
                            }
                        }
                    }),
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BitMatrix newMatrix = matrix.clone();

                            newMatrix.set(unknownPixels[pixelIndex].x, unknownPixels[pixelIndex].y);
                            try {
                                recurseGuessPixels(newMatrix, pixelIndex + 1, unknownPixels, new Decoder());
                            } catch (FormatException e) {
                                System.out.println(e);
                            }
                        }
                    })
                };

                for (Thread thread : threads) {
                    thread.start();
                }

                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                matrix.unset(unknownPixels[pixelIndex].x, unknownPixels[pixelIndex].y);
                recurseGuessPixels(matrix, pixelIndex + 1, unknownPixels, decoder);

                matrix.set(unknownPixels[pixelIndex].x, unknownPixels[pixelIndex].y);
                recurseGuessPixels(matrix, pixelIndex + 1, unknownPixels, decoder);
            }
        }
    }

    public static void main(String[] args) {
        try {
            BitMatrix matrix = readQRImage(args[0], 29); //Hard coded size, change if needed

            System.out.println(matrix);

            Point[] unknownPixels = generateUnknownPixels(args[0], 29); //Hard coded size, change if needed

            System.err.println("Brute-forcing missing modules...");

            recurseGuessPixels(matrix, 0, unknownPixels, new Decoder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
