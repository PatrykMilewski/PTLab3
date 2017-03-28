package application;


import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class MainWindowController extends BlackWhiteFiles implements Initializable {
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<File> files;
    private ObservableList<ImageProcessingJob> tableElementsList = FXCollections.observableArrayList();
    private boolean areSelected = false, threadsAmountSelected = false;
    private int threadsAmount;
    private ProcessingTypes processingType = ProcessingTypes.UNDEFINED;
    private ForkJoinPool customPool;
    private long start;


    @FXML
    TextField threadsAmountField;
    @FXML
    TableColumn<ImageProcessingJob, String> imageNameColumn;
    @FXML
    TableColumn<ImageProcessingJob, Double> progressColumn;
    @FXML
    TableColumn<ImageProcessingJob, String> statusColumn;
    @FXML
    TableView<ImageProcessingJob> table;
    @FXML
    RadioButton sequentialProcessing;
    @FXML
    RadioButton threadsPoolProcessing;
    @FXML
    RadioButton customThreadsPoolProcessing;
    @FXML
    Label timeLabel;

    @FXML
    private void chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG images", "*.jpg"));
        files = fileChooser.showOpenMultipleDialog(null);

        createImageProcessingJobsList();
    }

    private void createImageProcessingJobsList() {
        ImageProcessingJob.filesAmount = 0;
        if (files != null) {
            areSelected = true;
            tableElementsList.clear();
            files.forEach(file -> tableElementsList.add(new ImageProcessingJob(file, this)));
        }
        refreshTable();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sequentialProcessing.setToggleGroup(poolGroup);
        threadsPoolProcessing.setToggleGroup(poolGroup);
        customThreadsPoolProcessing.setToggleGroup(poolGroup);

        refreshTable();
    }

    @FXML
    void convertFile() {
        createImageProcessingJobsList();

        if (areSelected) {
            if (processingType == ProcessingTypes.UNDEFINED)
                return;

            start = System.currentTimeMillis();

            if (processingType == ProcessingTypes.SEQUENTIAL) {
                tableElementsList.stream().forEach(task -> executor.submit(task));
            }

            if (processingType == ProcessingTypes.CUSTOMPOOL && threadsAmountSelected) {
                if (customPool == null)
                    customPool = new ForkJoinPool(threadsAmount);
                else if (customPool.getParallelism() != threadsAmount)
                    customPool = new ForkJoinPool(threadsAmount);

                tableElementsList.parallelStream().forEach(task -> customPool.submit(task));
            }

            if (processingType == ProcessingTypes.POOL)
                tableElementsList.parallelStream().forEach(task -> ForkJoinPool.commonPool().submit(task));

        }
    }

    @FXML
    void sequentialSelect() {
        processingType = ProcessingTypes.SEQUENTIAL;
    }

    @FXML
    void poolSelect() {
        processingType = ProcessingTypes.POOL;
    }

    @FXML
    void customPoolSelect() {
        processingType = ProcessingTypes.CUSTOMPOOL;
    }

    @FXML
    void chooseThreadsAmount() {
        if (isInteger(threadsAmountField.getText())) {
            threadsAmount = Integer.parseInt(threadsAmountField.getText());
            threadsAmountSelected = true;
            threadsAmountField.setStyle("-fx-text-fill: green;");
        }
        else {
            threadsAmountSelected = false;
            threadsAmountField.setStyle("-fx-text-fill: red;");
        }

    }

    private void refreshTable() {
        imageNameColumn.setCellValueFactory( //nazwa pliku
                p -> new SimpleStringProperty(p.getValue().getFile().getName()));

        statusColumn.setCellValueFactory( //status przetwarzania
                p -> p.getValue().getStatusProperty());

        progressColumn.setCellFactory( //wykorzystanie paska postępu
                ProgressBarTableCell.<ImageProcessingJob>forTableColumn());

        progressColumn.setCellValueFactory( //postęp przetwarzania
                p -> p.getValue().getProgressProperty().asObject());

        table.setItems(tableElementsList);
    }

    private void backgroundJob() {
        tableElementsList.stream().forEach(job -> job.call());
    }

    private boolean isInteger(String str) {
        int size = str.length();

        for (int i = 0; i < size; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return size > 0;
    }

    void stopTimer() {
        timeLabel.setText(Long.toString(System.currentTimeMillis() - start));
    }

}
