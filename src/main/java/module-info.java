/**
 * @author Mark Woon
 */
module org.markwoon.nations {
  requires java.net.http;
  requires java.prefs;
  requires javafx.controls;
  requires javafx.fxml;

  requires org.checkerframework.checker.qual;
  requires com.google.common;
  requires org.jsoup;

  opens org.markwoon.nations to javafx.fxml, javafx.graphics;
}
