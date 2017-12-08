
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.qrcode.decoder.Decoder;

import java.io.FileInputStream;
import java.io.IOException;

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
                int g = in.read();
                int b = in.read();

                if (r > 200) {
                    matrix.unset(x, y);
                } else {
                    matrix.set(x, y);
                }
            }
        }

        return matrix;
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
            BitMatrix matrix = readQRImage(args[0], 29);

            System.out.println(matrix);

            // This isn't every unknown pixel but it seems to be enough to get a result
            Point[] unknownPixels = new Point[] {
                new Point(0, 9), new Point(1, 9), new Point(2, 9), new Point(3, 9), new Point(4, 9), new Point(5, 9),
                new Point(0, 10), new Point(1, 10), new Point(2, 10), new Point(3, 10), new Point(4, 10), new Point(5, 10),
                new Point(0, 11), new Point(1, 11), new Point(2, 11), new Point(3, 11), new Point(4, 11),
                new Point(0, 12),

                new Point(14, 2), new Point(14, 3), new Point(14, 4), new Point(14, 5),
                new Point(15, 3), new Point(15, 4), new Point(15, 5),
                new Point(16, 3), new Point(16, 4), new Point(16, 5),
                new Point(17, 4), new Point(17, 5)
            };

            System.err.println("Brute-forcing missing modules...");

            recurseGuessPixels(matrix, 0, unknownPixels, new Decoder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}