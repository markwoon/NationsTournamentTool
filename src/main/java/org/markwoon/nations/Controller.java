package org.markwoon.nations;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.prefs.Preferences;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;
import org.markwoon.nations.model.Game;
import org.markwoon.nations.ui.GroupField;
import org.markwoon.nations.ui.IntField;


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
  private Button dlGameListBtn;

  @FXML
  private ChoiceBox<String> scoreMode;
  @FXML
  private Button dlGameInfoBtn;

  @FXML
  private IntField p3TournamentNumberInput;
  @FXML
  private GroupField p3TournamentGroupInput;
  @FXML
  private ChoiceBox<Integer> p3TournamentPlayersInput;
  @FXML
  private TextField p3UserIdInput;
  @FXML
  private TextField p3PasswordInput;
  @FXML
  private Button p3CreateGamesBtn;

  @FXML
  private IntField p4TournamentNumberInput;
  @FXML
  private ChoiceBox<Integer> p4TournamentDivisionInput;
  @FXML
  private ChoiceBox<String> p4TournamentGroupInput;
  @FXML
  private ChoiceBox<String> p4TournamentLevelInput;
  @FXML
  private TextField p4UserIdInput;
  @FXML
  private TextField p4PasswordInput;
  @FXML
  private Button p4CreateGamesBtn;

  @FXML
  private VBox loading;

  private final List<Control> m_controls = new ArrayList<>();


  public void initialize() {
    m_controls.add(tournamentInput);
    m_controls.add(dirInput);
    m_controls.add(fileInput);
    m_controls.add(dlGameListBtn);

    m_controls.add(scoreMode);
    m_controls.add(dlGameInfoBtn);

    m_controls.add(p3TournamentNumberInput);
    m_controls.add(p3TournamentGroupInput);
    m_controls.add(p3TournamentPlayersInput);
    m_controls.add(p3UserIdInput);
    m_controls.add(p3PasswordInput);
    m_controls.add(p3CreateGamesBtn);

    m_controls.add(p4TournamentNumberInput);
    m_controls.add(p4TournamentDivisionInput);
    m_controls.add(p4TournamentGroupInput);
    m_controls.add(p4TournamentLevelInput);
    m_controls.add(p4UserIdInput);
    m_controls.add(p4PasswordInput);
    m_controls.add(p4CreateGamesBtn);

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

    p3TournamentPlayersInput.setValue(9);

    p4TournamentDivisionInput.setValue(1);
    p4TournamentGroupInput.setValue("A");
    p4TournamentLevelInput.setValue("Emperor");
  }


  private void lockInputs() {
    for (Control c : m_controls) {
      c.setDisable(true);
    }
    loading.setVisible(true);
  }

  private void reenableInputs() {
    for (Control c : m_controls) {
      c.setDisable(false);
    }
    loading.setVisible(false);
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

    String prefix = StringUtils.stripToNull(tournamentInput.getText());
    String dirName = StringUtils.stripToNull(dirInput.getText());
    if (prefix == null || dirName == null) {
      StringBuilder builder = new StringBuilder()
          .append("Please specify");
      if (prefix == null) {
        builder.append(" the tournament prefix");
      }
      if (dirName == null) {
        if (prefix == null) {
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

      lockInputs();
      Task<Void> task = new Task<>() {
        @Override
        protected Void call() throws Exception {
          SortedMap<String, Game> games = MabiWebHelper.getGames(prefix);
          if (games.size() == 0) {
            throw new RuntimeException("Cannot find any games with prefix '" + prefix + "'");
          }
          NationsUtils.writeTsv(games, file, false);
          return null;
        }
      };
      task.setOnSucceeded(evt -> {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.put(PREFS_TOURNAMENT_PREFIX, prefix);
        prefs.put(PREFS_GAME_LIST_FILE, file.toString());
        fileInput.setText(file.toString());
        alert(AlertType.INFORMATION, "Done.\n\nSaved to:\n" + file);
        reenableInputs();
      });
      task.setOnFailed(evt -> {
        if (task.getException() instanceof RuntimeException &&
            task.getException().getMessage() != null &&
            task.getException().getMessage().startsWith("Cannot find any games")) {
          alert(AlertType.ERROR, task.getException().getMessage());
        } else {
          alertException(task.getException());
        }
        reenableInputs();
      });
      new Thread(task).start();

    } catch (Exception ex) {
      alertException(ex);
    }
  }

  @FXML
  public void downloadGameInfo(@SuppressWarnings("unused") ActionEvent event) {

    String listFileName = StringUtils.stripToNull(fileInput.getText());
    if (listFileName == null) {
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
      //noinspection UnstableApiUsage
      String baseFilename = com.google.common.io.Files.getNameWithoutExtension(listFileName);
      if (baseFilename.matches("^(.+)_\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d$")) {
        baseFilename = baseFilename.substring(0, baseFilename.length() - 17);
      }
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
      Path file = listFile.getParent().resolve(baseFilename + "_" + timestamp + ".tsv");

      lockInputs();
      Task<Void> task = new Task<>() {
        @Override
        protected Void call() throws Exception {
          SortedMap<String, Game> games = NationsUtils.readTsv(listFile);
          MabiWebHelper.getGameDetails(games);
          NationsUtils.writeTsv(games, file, calculateFinalUnfinishedGameScore);
          return null;
        }
      };
      task.setOnSucceeded(evt -> {
        alert(AlertType.INFORMATION, "Done.\n\nSaved to:\n" + file);
        reenableInputs();
      });
      task.setOnFailed(evt -> {
        alertException(task.getException());
        reenableInputs();
      });
      new Thread(task).start();

    } catch (Exception ex) {
      alertException(ex);
    }
  }


  @FXML
  public void create3PGames(@SuppressWarnings("unused") ActionEvent event) {

    String tournamentNum = StringUtils.stripToNull(p3TournamentNumberInput.getText());
    String group = StringUtils.stripToNull(p3TournamentGroupInput.getText());
    String userId = StringUtils.stripToNull(p3UserIdInput.getText());
    String password = StringUtils.stripToNull(p3PasswordInput.getText());

    if (tournamentNum == null || group == null || userId == null || password == null) {
      alert(AlertType.ERROR, "Tournament Number, Tournament Group, User ID and Password " +
          "fields are required.");
      return;
    }
    int numPlayers = p3TournamentPlayersInput.getValue();
    try {
      String prefix = tournamentNum + ".Small Tournament";
      List<MabiWebHelper.NewGame> games =
          MabiWebHelper.buildTournamentGroup3pGameList(prefix, group, numPlayers);
      StringBuilder builder = new StringBuilder();
      for (MabiWebHelper.NewGame game : games) {
        builder.append(game.toString())
            .append("\n");
      }
      builder.append("\n");
      if (!confirm("Create the following games?", builder.toString())) {
        return;
      }

      lockInputs();
      Task<Integer> task = new Task<>() {
        @Override
        protected Integer call() throws Exception {
          MabiWebHelper mabiWebHelper = new MabiWebHelper();
          if (!mabiWebHelper.login(userId, password)) {
            System.out.println("Failed to login");
            return 1;
          }
          for (MabiWebHelper.NewGame game : games) {
            mabiWebHelper.createGame(game.name, game.password, 3, game.level);
          }
          return 0;
        }
      };
      task.setOnSucceeded(evt -> {
        if ((Integer)evt.getSource().getValue() == 1) {
          alert(AlertType.WARNING, "Failed to login to MabiWeb.");
        } else {
          alert(AlertType.INFORMATION, "Games created!");
        }
        reenableInputs();
      });
      task.setOnFailed(evt -> {
        alertException(task.getException());
        reenableInputs();
      });
      new Thread(task).start();

    } catch (Exception ex) {
      alertException(ex);
    }
  }


  @FXML
  public void create4PGames(@SuppressWarnings("unused") ActionEvent event) {

    String tournamentNum = StringUtils.stripToNull(p4TournamentNumberInput.getText());
    String group = StringUtils.stripToNull(p4TournamentGroupInput.getValue());
    String levelValue = StringUtils.stripToNull(p4TournamentLevelInput.getValue());
    String userId = StringUtils.stripToNull(p4UserIdInput.getText());
    String password = StringUtils.stripToNull(p4PasswordInput.getText());

    if (tournamentNum == null || group == null || levelValue == null || userId == null ||
        password == null) {
      alert(AlertType.ERROR, "Tournament Number, Group, Level, User ID and Password fields " +
          "are required.");
      return;
    }
    MabiWebHelper.Level level;
    try {
      level = MabiWebHelper.Level.valueOf(levelValue.toUpperCase());
    } catch (IllegalArgumentException ex) {
      alert(AlertType.ERROR, "Unknown leve '" + levelValue  +"'");
      return;
    }
    try {
      // 4-Player-Tournament - Div.(variable as above) - Group (variable as above) - Game No.
      int division = p4TournamentDivisionInput.getValue();
      String prefix = "Test-4-Player-Tournament-" + tournamentNum + " - Div." + division + " - Group " +
          group.toUpperCase();
      List<MabiWebHelper.NewGame> games =
          MabiWebHelper.buildTournamentGroup4pGameList(prefix, division, group, level);
      StringBuilder builder = new StringBuilder();
      for (MabiWebHelper.NewGame game : games) {
        builder.append(game.toString())
            .append("\n");
      }
      builder.append("\n");
      if (!confirm("Create the following games?", builder.toString())) {
        return;
      }

      lockInputs();
      Task<Integer> task = new Task<>() {
        @Override
        protected Integer call() throws Exception {
          MabiWebHelper mabiWebHelper = new MabiWebHelper();
          if (!mabiWebHelper.login(userId, password)) {
            System.out.println("Failed to login");
            return 1;
          }
          for (MabiWebHelper.NewGame game : games) {
            mabiWebHelper.createGame(game.name, game.password, 4, game.level);
          }
          return 0;
        }
      };
      task.setOnSucceeded(evt -> {
        if ((Integer)evt.getSource().getValue() == 1) {
          alert(AlertType.WARNING, "Failed to login to MabiWeb.");
        } else {
          alert(AlertType.INFORMATION, "Games created!");
        }
        reenableInputs();
      });
      task.setOnFailed(evt -> {
        alertException(task.getException());
        reenableInputs();
      });
      new Thread(task).start();

    } catch (Exception ex) {
      alertException(ex);
    }
  }


  private boolean confirm(String header, String content) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle(null);
    alert.setHeaderText(header);
    alert.setContentText(content);

    Text text = new Text(content);
    GridPane expContent = new GridPane();
    expContent.setMaxWidth(Double.MAX_VALUE);
    expContent.setMaxHeight(Double.MAX_VALUE);
    expContent.add(text, 0, 0);

    alert.getDialogPane().setContent(expContent);

    Optional<ButtonType> result = alert.showAndWait();
    return result.isPresent() && result.get() == ButtonType.OK;
  }


  private void alert(AlertType type, String msg) {
    System.out.println(msg);
    Alert alert = new Alert(type);
    alert.setHeaderText(null);

    Text text = new Text(msg);
    GridPane expContent = new GridPane();
    expContent.setPadding(new Insets(24, 16, 16, 16));
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
