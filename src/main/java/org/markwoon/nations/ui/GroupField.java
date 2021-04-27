package org.markwoon.nations.ui;

import java.util.regex.Pattern;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;


/**
 * Field for specifying tournament group.
 * Only allows letters.
 *
 * @author Mark Woon
 */
public class GroupField extends TextField {
  private static final Pattern sf_filter = Pattern.compile("^[A-Z]$");
  private String m_value = "";

  public GroupField() {
    super();
    setTextFormatter(new TextFormatter<>(c -> {
      m_value = "";
      if (c.getControlNewText().isEmpty()) {
        return c;
      }
      if (sf_filter.matcher(c.getControlNewText()).matches()) {
        m_value = c.getControlNewText();
        return c;
      }
      return null;
    }));
  }

  public String getValue() {
    return m_value;
  }
}
