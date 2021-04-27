package org.markwoon.nations.ui;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;


/**
 * Field for specifying tournament sub-group.
 * Only allows numbers between 0 and 8 (inclusive).
 *
 * @author Mark Woon
 */
public class SubGroupField extends TextField {
  private int m_value = 0;

  public SubGroupField() {
    super();
    setTextFormatter(new TextFormatter<>(c -> {
      m_value = 0;
      if (c.getControlNewText().isEmpty()) {
        return c;
      }
      try {
        m_value = Integer.parseInt(c.getControlNewText());
        if (m_value < 9) {
          return c;
        }
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
