package org.markwoon.nations;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SortedMap;
import java.util.prefs.Preferences;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.markwoon.nations.model.Game;


public class Controller  {
  private static final String PREFS_TOURNAMENT_PREFIX = "tournamentPrefix";
  private static final String PREFS_WORKING_DIR = "workingDir";
  private static final String PREFS_GAME_LIST_FILE = "file.gameList";
  private static final String PREFS_SCORE_MODE = "scoreMode";
  private static final String NORMAL_SCORE_MODE_VALUE = "Normal";
  private static final String FINAL_SCORE_MODE_VALUE = "Final";
  @FXML
  private TextField tournamentInput;
  @FXML
  private TextField dirInput;
  @FXML
  private TextField fileInput;
  @FXML
  private ChoiceBox<String> scoreMode;
  @FXML
  private Button dlGameInfoBtn;
  @FXML
  private VBox loading;


  public void initialize() {
    Preferences prefs = Preferences.userRoot().node(this.getClass().getName());

    String prefix = prefs.get(PREFS_TOURNAMENT_PREFIX, "");
    tournamentInput.setText(prefix);

    String dir = prefs.get(PREFS_WORKING_DIR, "");
    dirInput.setText(dir);

    String mode = prefs.get(PREFS_SCORE_MODE, NORMAL_SCORE_MODE_VALUE);
    scoreMode.getItems().addAll(NORMAL_SCORE_MODE_VALUE, FINAL_SCORE_MODE_VALUE);
    scoreMode.setValue(mode);

    String filename = prefs.get(PREFS_GAME_LIST_FILE, "");
    fileInput.setText(filename);
  }


  @FXML
  public void chooseDirectory(ActionEvent event) {
    DirectoryChooser dirChooser = new DirectoryChooser();

    Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    String dirName = prefs.get(PREFS_WORKING_DIR, "");
    if (dirName.length() > 0) {
      Path dir = Paths.get(dirName);
      if (Files.isDirectory(dir)) {
        dirChooser.setInitialDirectory(dir.toFile());
      }
    }

    File selectedFile = dirChooser.showDialog(((Node)event.getSource()).getScene().getWindow());
    dirInput.setText(selectedFile.getAbsolutePath());
    prefs.put(PREFS_WORKING_DIR, selectedFile.getAbsolutePath());
  }


  @FXML
  public void chooseFile(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("TSV Files", "*.tsv")
    );

    File selectedFile = fileChooser.showOpenDialog(((Node)event.getSource()).getScene().getWindow());
    fileInput.setText(selectedFile.getAbsolutePath());

    Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    prefs.put(PREFS_GAME_LIST_FILE, selectedFile.getAbsolutePath());
  }


  @FXML
  public void downloadList(@SuppressWarnings("unused") ActionEvent event) {

    String prefix = tournamentInput.getText();
    String dirName = dirInput.getText();
    if (prefix == null || prefix.length() == 0 ||
        dirName == null || dirName.length() == 0) {
      StringBuilder builder = new StringBuilder()
          .append("Please specify");
      if (prefix == null || prefix.length() == 0) {
        builder.append(" the tournament prefix");
      }
      if (dirName == null || dirName.length() == 0) {
        if (prefix == null || prefix.length() == 0) {
          builder.append(" and");
        }
        builder.append(" the directory to save to");
      }
      builder.append(".");
      alert(AlertType.ERROR, builder.toString());
      return;
    }

    Path dir = Paths.get(dirName);
    if (!Files.isDirectory(dir)) {
      alert(AlertType.ERROR, dirName + " is not a directory!");
      return;
    }

    try {
      Path file = dir.resolve(prefix.replaceAll(" ", "_")
          .replaceAll("\\.", "_") + ".tsv");

      dlGameInfoBtn.setDisable(true);
      loading.setVisible(true);

      Task<Void> task = new Task<>() {
        @Override
        protected Void call() throws Exception {
          try {
            SortedMap<String, Game> games = MabiWebUtils.getGames(prefix);
            if (games.size() == 0) {
              throw new RuntimeException("Cannot find any games with prefix '" + prefix + "'");
            }
            MabiWebUtils.writeTsv(games, file, false);
          } finally {
            loading.setVisible(false);
            dlGameInfoBtn.setDisable(false);
          }
          return null;
        }
      };
      task.setOnSucceeded(evt -> {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.put(PREFS_TOURNAMENT_PREFIX, prefix);
        prefs.put(PREFS_GAME_LIST_FILE, file.toString());
        fileInput.setText(file.toString());
        alert(AlertType.INFORMATION, "Done.\n\nSaved to:\n" + file.toString());
      });
      task.setOnFailed(evt -> {
        if (task.getException() instanceof RuntimeException &&
            task.getException().getMessage() != null &&
            task.getException().getMessage().startsWith("Cannot find any games")) {
          alert(AlertType.ERROR, task.getException().getMessage());
        } else {
          alertException(task.getException());
        }
      });
      new Thread(task).start();

    } catch (Exception ex) {
      alertException(ex);
    }
  }

  @FXML
  public void downloadGameInfo(@SuppressWarnings("unused") ActionEvent event) {

    String listFileName = fileInput.getText();
    if (listFileName == null || listFileName.length() == 0) {
      alert(AlertType.ERROR, "Please specify game list to get info on.");
      return;
    }
    Path listFile = Paths.get(listFileName);
    if (!Files.exists(listFile)) {
      alert(AlertType.ERROR, listFileName + " is not a file!");
      return;
    }

    boolean calculateFinalUnfinishedGameScore = FINAL_SCORE_MODE_VALUE.equals(scoreMode.getValue());

    Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    prefs.put(PREFS_GAME_LIST_FILE, listFileName);
    prefs.put(PREFS_SCORE_MODE, scoreMode.getValue());

    try {
      scoreMode.setDisable(true);
      dlGameInfoBtn.setDisable(true);
      loading.setVisible(true);

      //noinspection UnstableApiUsage
      String baseFilename = com.google.common.io.Files.getNameWithoutExtension(listFileName);
      if (baseFilename.matches("^(.+)_\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d$")) {
        baseFilename = baseFilename.substring(0, baseFilename.length() - 17);
      }
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
      Path file = listFile.getParent().resolve(baseFilename + "_" + timestamp + ".tsv");

      Task<Void> task = new Task<>() {
        @Override
        protected Void call() throws Exception {
          try {
            SortedMap<String, Game> games = MabiWebUtils.readTsv(listFile);
            MabiWebUtils.getGameDetails(games);
            MabiWebUtils.writeTsv(games, file, calculateFinalUnfinishedGameScore);
          } finally {
            loading.setVisible(false);
            scoreMode.setDisable(false);
            dlGameInfoBtn.setDisable(false);
          }
          return null;
        }
      };
      task.setOnSucceeded(evt ->
          alert(AlertType.INFORMATION, "Done.\n\nSaved to:\n" + file.toString()));
      task.setOnFailed(evt -> alertException(task.getException()));
      new Thread(task).start();

    } catch (Exception ex) {
      alertException(ex);
    }
  }


  private void alert(AlertType type, String msg) {
    System.out.println(msg);
    Alert alert = new Alert(type);
    alert.setHeaderText(null);
    Text text = new Text(msg);
    GridPane expContent = new GridPane();
    expContent.setMaxWidth(Double.MAX_VALUE);
    expContent.add(text, 0, 0);
    alert.getDialogPane().setContent(expContent);
    alert.showAndWait();
  }

  private void alertException(Throwable ex) {
    Alert alert = new Alert(AlertType.ERROR);
    alert.setTitle("Error");
    alert.setHeaderText(null);
    alert.setContentText("Something went wrong...");

    // create expandable text area
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ex.printStackTrace(pw);
    String exceptionText = sw.toString();

    Label label = new Label("The exception stacktrace was:");
    TextArea textArea = createExpandableTextArea(exceptionText);

    GridPane expContent = new GridPane();
    expContent.setMaxWidth(Double.MAX_VALUE);
    expContent.add(label, 0, 0);
    expContent.add(textArea, 0, 1);

    // add expandable text area to dialog pane
    alert.getDialogPane().setExpandableContent(expContent);

    alert.showAndWait();
  }

  private TextArea createExpandableTextArea(String text) {
    TextArea textArea = new TextArea(text);
    textArea.setEditable(false);
    textArea.setWrapText(true);

    textArea.setMaxWidth(Double.MAX_VALUE);
    textArea.setMaxHeight(Double.MAX_VALUE);
    GridPane.setVgrow(textArea, Priority.ALWAYS);
    GridPane.setHgrow(textArea, Priority.ALWAYS);
    return textArea;
  }
}
