package org.markwoon.nations;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * This is a JUnit test for {@link MabiWebHelper}.
 *
 * @author Mark Woon
 */
class MabiWebHelperTest {

  @Test
  void foo() {
    MabiWebHelper mabiWebHelper = new MabiWebHelper();

    List<MabiWebHelper.NewGame> games = mabiWebHelper.buildTournamentGameList("foo", "A", 0);
    assertEquals("foo - Group A - Game 101", games.get(0).name);

    games = mabiWebHelper.buildTournamentGameList("foo", "B", 0);
    assertEquals("foo - Group B - Game 204", games.get(3).name);

    games = mabiWebHelper.buildTournamentGameList("foo", "B", 1);
    assertEquals("foo - Group B1 - Game 201", games.get(0).name);

    games = mabiWebHelper.buildTournamentGameList("foo", "D", 4);
    assertEquals("foo - Group D4 - Game 448", games.get(11).name);

    /*
    for (MabiWebHelper.NewGame game : games) {
      System.out.println(game);
    }
    */
  }
}
