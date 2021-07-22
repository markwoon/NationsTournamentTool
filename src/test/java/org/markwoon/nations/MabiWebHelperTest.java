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
  void buildGameListWith12Games() {
    int numGames = 12;
    List<MabiWebHelper.NewGame> games =
        MabiWebHelper.buildTournamentGameList("foo", "A", 0, numGames);
    assertEquals("foo - Group A - Game 101", games.get(0).name);
    long numChieftains = games.stream()
        .filter(g -> g.level == MabiWebHelper.Level.CHIEFTAIN)
        .count();
    assertEquals(3, numChieftains);

    games = MabiWebHelper.buildTournamentGameList("foo", "B", 0, numGames);
    assertEquals("foo - Group B - Game 204", games.get(3).name);

    games = MabiWebHelper.buildTournamentGameList("foo", "B", 1, numGames);
    assertEquals("foo - Group B1 - Game 201", games.get(0).name);

    games = MabiWebHelper.buildTournamentGameList("foo", "D", 4, numGames);
    assertEquals("foo - Group D4 - Game 448", games.get(11).name);

    /*
    for (MabiWebHelper.NewGame game : games) {
      System.out.println(game);
    }
    */
  }


  @Test
  void buildGameListWith20Games() {
    int numGames = 20;
    List<MabiWebHelper.NewGame> games =
        MabiWebHelper.buildTournamentGameList("foo", "A", 0, numGames);
    assertEquals("foo - Group A - Game 101", games.get(0).name);
    long numChieftains = games.stream()
        .filter(g -> g.level == MabiWebHelper.Level.CHIEFTAIN)
        .count();
    assertEquals(5, numChieftains);

    games = MabiWebHelper.buildTournamentGameList("foo", "B", 0, numGames);
    assertEquals("foo - Group B - Game 204", games.get(3).name);

    games = MabiWebHelper.buildTournamentGameList("foo", "B", 1, numGames);
    assertEquals("foo - Group B1 - Game 201", games.get(0).name);

    games = MabiWebHelper.buildTournamentGameList("foo", "D", 2, numGames);
    assertEquals("foo - Group D2 - Game 421", games.get(0).name);

    games = MabiWebHelper.buildTournamentGameList("foo", "D", 2, numGames);
    assertEquals("foo - Group D2 - Game 440", games.get(19).name);
  }


  @Test
  void buildSubgroupGameListWith21Players() {
    List<MabiWebHelper.NewGame> games =
        MabiWebHelper.buildTournamentGroupGameList("foo", "A", 9);
    assertEquals(12, games.size());
    assertEquals("foo - Group A - Game 101", games.get(0).name);

    games = MabiWebHelper.buildTournamentGroupGameList("foo", "A", 12);
    assertEquals(16, games.size());
    assertEquals("foo - Group A - Game 101", games.get(0).name);

    games = MabiWebHelper.buildTournamentGroupGameList("foo", "A", 15);
    assertEquals(20, games.size());
    assertEquals("foo - Group A - Game 101", games.get(0).name);

    games = MabiWebHelper.buildTournamentGroupGameList("foo", "A", 18);
    assertEquals(24, games.size());
    assertEquals("foo - Group A1 - Game 101", games.get(0).name);

    games = MabiWebHelper.buildTournamentGroupGameList("foo", "A", 21);
    assertEquals(28, games.size());
    assertEquals("foo - Group A - Game 101", games.get(0).name);

    games = MabiWebHelper.buildTournamentGroupGameList("foo", "A", 24);
    assertEquals(32, games.size());
    assertEquals("foo - Group A1 - Game 101", games.get(0).name);
    assertEquals("foo - Group A2 - Game 117", games.get(16).name);

    games = MabiWebHelper.buildTournamentGroupGameList("foo", "A", 27);
    assertEquals(36, games.size());
    assertEquals("foo - Group A1 - Game 101", games.get(0).name);
    assertEquals("foo - Group A3 - Game 136", games.get(35).name);

    /*
    for (MabiWebHelper.NewGame game : games) {
      System.out.println(game);
    }
    */
  }
}
