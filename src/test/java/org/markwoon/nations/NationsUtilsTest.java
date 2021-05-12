package org.markwoon.nations;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * This is a JUnit test for {@link NationsUtils}.
 *
 * @author Mark Woon
 */
class NationsUtilsTest {

  @Test
  void parsingDates() {
    assertNotNull(LocalDateTime.parse("2021-07-05 09:15", NationsUtils.MABIWEB_DATE_TIME_FORMATTER));
    assertNotNull(LocalDateTime.parse("2021-07-05 9:15", NationsUtils.MABIWEB_DATE_TIME_FORMATTER));
    assertNotNull(LocalDateTime.parse("2021-7-5 9:15", NationsUtils.MABIWEB_DATE_TIME_FORMATTER));

    assertNotNull(LocalDateTime.parse("07/05/2021 09:15", NationsUtils.US_DATE_TIME_FORMATTER));
    assertNotNull(LocalDateTime.parse("07/05/2021 9:05", NationsUtils.US_DATE_TIME_FORMATTER));
    assertNotNull(LocalDateTime.parse("7/5/2021 9:05", NationsUtils.US_DATE_TIME_FORMATTER));

    assertNotNull(LocalDateTime.parse("14.07.2021 09:15", NationsUtils.DOT_DATE_TIME_FORMATTER));
    assertNotNull(LocalDateTime.parse("14.07.2021 9:05", NationsUtils.DOT_DATE_TIME_FORMATTER));
    assertNotNull(LocalDateTime.parse("14.7.2021 9:05", NationsUtils.DOT_DATE_TIME_FORMATTER));
  }
}
