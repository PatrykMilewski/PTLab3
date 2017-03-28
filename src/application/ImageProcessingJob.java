package application;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageProcessingJob extends Task {
    private File file;
    private SimpleStringProperty status;
    private DoubleProperty progress;
    private MainWindowController mainWindowController;
    public static int filesAmount = 0;

    ImageProcessingJob(File file, MainWindowController mainWindowController) {
        this.mainWindowController = mainWindowController;
        this.file = file;
        progress = new SimpleDoubleProperty();
        status = new SimpleStringProperty();
        progress.set(0.0);
        filesAmount++;
        status.set("Czekam..");
    }

    public void reset() {
        file = null;
        progress.set(0.0);
    }

    File getFile() {
        return this.file;
    }

    SimpleStringProperty getStatusProperty() {
        return this.status;
    }

    DoubleProperty getProgressProperty() {
        return this.progress;
    }

    @Override
    public Object call() {
        status.set("Konwertowanie..");
        String fileName[] = file.getName().split("\\.");
        File outputDir = new File(fileName[0] + "BW." + fileName[1]);
        try {
            //wczytanie oryginalnego pliku do pamięci
            BufferedImage original = ImageIO.read(file);

            //przygotowanie bufora na grafikę w skali szarości
            BufferedImage grayscale = new BufferedImage(
                    original.getWidth(), original.getHeight(), original.getType());
            //przetwarzanie piksel po pikselu
            for (int i = 0; i < original.getWidth(); i++) {
                for (int j = 0; j < original.getHeight(); j++) {

                    int red = new Color(original.getRGB(i, j)).getRed();
                    int green = new Color(original.getRGB(i, j)).getGreen();
                    int blue = new Color(original.getRGB(i, j)).getBlue();

                    int luminosity = (int) (0.21 * red + 0.71 * green + 0.07 * blue);
                    int newPixel =
                            new Color(luminosity, luminosity, luminosity).getRGB();

                    grayscale.setRGB(i, j, newPixel);
                }
                //obliczenie postępu przetwarzania jako liczby z przedziału [0, 1]
                double progress = (1.0 + i) / original.getWidth();
                //aktualizacja własności zbindowanej z paskiem postępu w tabeli
                Platform.runLater(() -> this.progress.set(progress));
            }
            //przygotowanie ścieżki wskazującej na plik wynikowy
            Path outputPath =
                    Paths.get(outputDir.getAbsolutePath());

            //zapisanie zawartości bufora do pliku na dysku
            ImageIO.write(grayscale, "jpg", outputPath.toFile());
            status.set("Ukonczono!");
            filesAmount--;
            if (filesAmount == 0)
                Platform.runLater(() -> mainWindowController.stopTimer());
        } catch (IOException ex) {
            status.set("Blad!");
            throw new RuntimeException(ex);
        }
        return null;
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }
}
