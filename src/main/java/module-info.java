/**
 * @author Mark Woon
 */
module org.markwoon.nations {
  requires jakarta.activation;
  requires jakarta.mail;
  requires java.net.http;
  requires java.prefs;
  requires javafx.controls;
  requires javafx.fxml;

  requires org.apache.commons.lang3;
  requires org.apache.commons.text;
  requires org.apache.poi.poi;
  requires org.apache.poi.ooxml;
  requires org.apache.poi.ooxml.schemas;
  requires org.checkerframework.checker.qual;
  requires com.google.common;
  requires org.jsoup;

  opens org.markwoon.nations to javafx.fxml, javafx.graphics;
  opens org.markwoon.nations.ui to javafx.fxml, javafx.graphics;
}
