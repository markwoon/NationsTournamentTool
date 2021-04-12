package org.markwoon.nations;

import java.io.InputStream;
import java.util.Properties;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class TournamentTool extends Application {

  @Override
  public void start(Stage primaryStage) throws Exception {
    String version = "";
    try (InputStream in = getClass().getResourceAsStream("TournamentTool.properties")) {
      if (in != null) {
        Properties prop = new Properties();
        prop.load(in);
        version = " v" + prop.getProperty("version");
      }
    }

    Parent root = FXMLLoader.load(getClass().getResource("TournamentTool.fxml"));
    primaryStage.setTitle("Nations Tournament Tool" + version);
    primaryStage.setScene(new Scene(root, 600, 300));

    primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("marie_curie.png")));
    primaryStage.show();
  }


  public static void main(String[] args) {
    launch(args);
  }
}
