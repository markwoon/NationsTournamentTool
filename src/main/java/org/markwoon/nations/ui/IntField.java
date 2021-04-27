package org.markwoon.nations.ui;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;


/**
 * TextField that only accepts numbers.
 *
 * @author Mark Woon
 */
public class IntField extends TextField {
  private int m_value = 0;

  public IntField() {
    super();
    setTextFormatter(new TextFormatter<>(c -> {
      m_value = 0;
      if (c.getControlNewText().isEmpty()) {
        return c;
      }
      try {
        m_value = Integer.parseInt(c.getControlNewText());
        return c;
      } catch (NumberFormatException ex) {
        // ignore
      }
      return null;
    }));
  }

  public int getValue() {
    return m_value;
  }
}
