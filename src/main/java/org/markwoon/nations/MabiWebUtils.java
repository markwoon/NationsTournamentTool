package org.markwoon.nations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.google.common.base.Splitter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.markwoon.nations.model.Game;
import org.markwoon.nations.model.PlayerPoints;


/**
 * Utilities for working with MabiWeb.
 *
 * @author Mark Woon
 */
public class MabiWebUtils {
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final int sf_numPlayers = 3;
  private static final Splitter sf_commaSplitter = Splitter.on(",").trimResults()
      .omitEmptyStrings();
  private static final Splitter sf_spaceSplitter = Splitter.on(" ").trimResults()
      .omitEmptyStrings();
  // maximum allowed days is 30
  private static final String sf_finishedGamesUrl =
      "http://www.mabiweb.com/modules.php?name=Game_Manager&op=completed_games&gm_type=10&days=30";
  // maximum allowed days is 30
  private static final String sf_runningGamesUrl =
      "http://www.mabiweb.com/modules.php?name=Game_Manager&op=running_games&gm_type=10&days=30";


  /**
   * Gets the basics of all games for a tournament.
   */
  public static SortedMap<String, Game> getGames(String tournamentName) throws IOException {

    SortedMap<String, Game> games = new TreeMap<>();
    getGames(tournamentName, games, false);
    getGames(tournamentName, games, true);
    return games;
  }

  private static void getGames(String tournamentName, SortedMap<String, Game> games,
      boolean finished) throws IOException {

    String url;
    if (finished) {
      url = sf_finishedGamesUrl;
    } else {
      url = sf_runningGamesUrl;
    }
    Document doc = Jsoup.connect(url)
        .get();
    for (Element tr : doc.select("#gamemanager-gamelists > table > tbody > tr")) {
      Elements tds = tr.select("td");
      String name = tds.get(2).text();
      if (name.startsWith(tournamentName)) {
        String id = tds.get(0).text();
        Game game = new Game(id, name);

        if (finished) {
          game.setRound(Game.ROUND_FINISHED);
          game.setPlayers(sf_commaSplitter.splitToList(tds.get(4).text()));
        } else {
          String data = tds.get(4).text();
          int idx = data.indexOf((" ("));
          game.setPlayers(sf_commaSplitter.splitToList(data.substring(0, idx)));
          game.setRoundString(data.substring(idx + 2, data.length() - 1));
        }

        String lastUpdate = tds.get(5).text();
        game.setLastUpdated(calculateTimestamp(lastUpdate));
        games.put(name, game);
      }
    }
  }


  private static final Pattern sf_timeAgoPattern = Pattern.compile("(\\d+)(?:\\.(\\d+))? (min|hours|days) ago");

  private static LocalDateTime calculateTimestamp(String lastUpdate) throws IOException {
    Matcher m = sf_timeAgoPattern.matcher(lastUpdate);
    if (m.matches()) {
      int main = Integer.parseInt(m.group(1));
      int sub = 0;
      if (m.group(2) != null) {
        sub = Integer.parseInt(m.group(2));
      }
      LocalDateTime dateTime = LocalDateTime.now();
      switch (m.group(3)) {
        case "min" -> {
          dateTime = dateTime.minusMinutes(main);
          if (sub > 0) {
            dateTime = dateTime.minusSeconds((long)6 * sub);
          }
        }
        case "hours" -> {
          dateTime = dateTime.minusHours(main);
          if (sub > 0) {
            dateTime = dateTime.minusMinutes((long)6 * sub);
          }
        }
        case "days" -> {
          dateTime = dateTime.minusDays(main);
          if (sub > 0) {
            dateTime = dateTime.minusMinutes((long)(2.4 * sub * 60));
          }
        }
      }
      return dateTime;

    } else {
      throw new IOException("Unexpected time ago format: " + lastUpdate);
    }
  }


  /**
   * Gets the details of all games.
   */
  public static void getGameDetails(SortedMap<String, Game> games) throws IOException {
    LastUpdatedHelper lastUpdatedHelper = new LastUpdatedHelper();
    for (String key : games.keySet()) {
      Game game = games.get(key);
      getGameDetails(game);
      lastUpdatedHelper.updateLastUpdated(game);
    }
  }


  private static final Pattern sf_roundPattern = Pattern.compile("round: (.*?), Action phase");
  private static final Pattern sf_playerTurnPattern = Pattern.compile("<b>(.*?)</b>");
  private static final Pattern sf_playerFinalScorePattern = Pattern.compile("(\\w+)\\s+(\\d+)\\s+\\(.+?\\)");
  private static final Pattern sf_vpTablePattern =
      Pattern.compile("<TABLE class=\\\\'vp_summary_table\\\\'.*?>(.*)</TABLE>");
  private static final Pattern sf_vpPattern =
      Pattern.compile("<TR><TD.*?><IMG.*?> (.+?)</TD><TD.*?>(\\d+?) <IMG.*?></TD></TR>");
  private static final Pattern sf_countryPattern =
      Pattern.compile("modules/GM_Nations/images/DPlayerBoard_(\\w+)\\.jpg");
  private static final Pattern sf_topPattern = Pattern.compile("top:(\\d+)");

  /**
   * Gets the details of a specific game.
   */
  public static void getGameDetails(Game game) throws IOException {
    Document doc = Jsoup.connect("http://www.mabiweb.com/modules.php?name=GM_Nations&op=view_game_reset&g_id=" + game.getId())
        .get();

    Element header = doc.getElementById("nations-gameheader");
    String headerPlayerInfo = header.text().substring(header.text().indexOf("Players:") + 8);

    boolean isFinished = header.html().contains("Game finished");
    // finished?
    if (isFinished) {
      // round
      game.setRound(Game.ROUND_FINISHED);

      // players and score
      Matcher m = sf_playerFinalScorePattern.matcher(headerPlayerInfo);
      List<String[]> playerData = new ArrayList<>();
      while (m.find()) {
        playerData.add(new String[] { m.group(1), m.group(2) });
      }
      List<String> players = new ArrayList<>();
      for (String[] p : playerData) {
        players.add(p[0]);
        game.setScore(p[0], Integer.parseInt(p[1]));
      }
      game.setPlayers(players);
      game.setActivePlayer(null);

    } else {
      // round
      Matcher roundMatcher = sf_roundPattern.matcher(header.text());
      if (roundMatcher.find()) {
        game.setRoundString(roundMatcher.group(1));
      }

      // players
      game.setPlayers(sf_spaceSplitter.splitToList(headerPlayerInfo));
      // active player
      String players = header.html().substring(header.html().indexOf("Players:"));
      Matcher playerMatcher = sf_playerTurnPattern.matcher(players);
      if (playerMatcher.find()) {
        game.setActivePlayer(playerMatcher.group(1));
      }

      // score
      Matcher vpTableMatcher = sf_vpTablePattern.matcher(doc.html());
      if (vpTableMatcher.find()) {
        String vpTable = vpTableMatcher.group(1);
        Matcher vpMatcher = sf_vpPattern.matcher(vpTable);
        while (vpMatcher.find()) {
          game.setScore(vpMatcher.group(1), Integer.parseInt(vpMatcher.group(2)));
        }
      }
    }

    // country
    for (String player : game.getPlayers()) {
      Element board = doc.getElementById(player);
      Matcher countryMatcher = sf_countryPattern.matcher(board.html());
      if (countryMatcher.find()) {
        game.setCountry(player, countryMatcher.group(1));
      }

      if (!isFinished) {
        Elements elems = board.select("div.outline_text");
        for (Element elem : elems) {
          Node prev = elem.previousSibling();
          if (prev.attr("src").contains("/images/Meeple_")) {
            Matcher topMatcher = sf_topPattern.matcher(elem.attr("style"));
            if (topMatcher.find()) {
              if (Integer.parseInt(topMatcher.group(1)) > 400) {
                game.setUnusedWorkers(player, Integer.parseInt(elem.text()));
              }
            }
          }
        }
      }
    }
  }


  public static void writeTsv(SortedMap<String, Game> games, Path file) throws IOException {

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      DecimalFormat decimalFormat = new DecimalFormat();
      decimalFormat.setMaximumFractionDigits(2);
      SortedMap<String, SortedMap<String, PlayerPoints>> totalPoints = new TreeMap<>();
      writer.print("Game ID\tGame Name\tRound\tLast Updated\t");
      for (int x = 0; x < sf_numPlayers; x += 1) {
        writer.print("Player " + (x + 1) + "\t");
      }
      writer.println("Tournament Points");
      for (String name : games.keySet()) {
        Game game = games.get(name);
        writer.print(game.getId());
        writer.print("\t");
        writer.print(game.getName());
        writer.print("\t");
        writer.print(game.getRound());
        writer.print("\t");
        writer.print(game.getLastUpdated() == null ? "" : game.getLastUpdated().format(DATE_TIME_FORMATTER));
        if (game.getPlayers() != null) {
          for (String player : game.getPlayers()) {
            writer.print("\t");
            writer.print(player);
            if (game.getCountry(player) != null) {
              writer.print(" (" + game.getCountry(player) + ", ");
              if (!game.isFinished()) {
                writer.print(game.getUnusedWorkers(player) + " unused workers, ");
              }
              writer.print(game.getScore(player) + "vp)");
            }
          }
          if (game.hasScores()) {
            game.calculatePoints(true, games.values());
            StringBuilder builder = new StringBuilder();
            for (String player : game.getPlayers()) {
              if (builder.length() > 0) {
                builder.append(", ");
              }
              float points = game.getPoints(player);
              builder.append(player)
                  .append(" (")
                  .append(decimalFormat.format(points));
              if (player.equals(game.getSlowestPlayer())) {
                builder.append(", SLOWEST");
              }
              builder.append(")");

              SortedMap<String, PlayerPoints> groupMap = totalPoints
                  .computeIfAbsent(game.getGroup(), g -> new TreeMap<>());
              PlayerPoints playerPoints = groupMap.computeIfAbsent(player, PlayerPoints::new);
              playerPoints.addPoints(points, game.isFinished());
            }

            writer.print("\t");
            writer.print(builder.toString());
          }
        }
        writer.println();
      }
      writer.println();
      for (String group : totalPoints.keySet()) {
        writer.print("\t\t\t\t\t\tGroup " + group);
        boolean started = false;
        for (PlayerPoints pp : new TreeSet<>(totalPoints.get(group).values())) {
          if (started) {
            writer.println("\t\t\t\t\t\t\t" + pp);
          } else {
            writer.println("\t" + pp);
            started = true;
          }
        }
      }
    }
  }


  private static final Pattern sf_tsvPlayerPattern = Pattern.compile("(\\w+)(?: \\((.*?)\\))?");
  private static final Pattern sf_tsvPlayerStatPattern = Pattern.compile("(\\w+?), (\\d+) unused workers, (\\d+)vp");
  private static final Pattern sf_tsvFinishedPlayerStatPattern = Pattern.compile("(\\w+?), (\\d+)vp");

  public static SortedMap<String, Game> readTsv(Path file) throws IOException {

    SortedMap<String, Game> games = new TreeMap<>();
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      int rowNum = 0;
      String line = reader.readLine();
      if (line != null && line.startsWith("Game ID")) {
        line = reader.readLine();
      }
      while (line != null) {
        rowNum += 1;
        String[] cols = line.split("\t");
        if (cols.length == 0 || cols[0] == null || cols[0].trim().equals("")) {
          break;
        }
        Game game = new Game(cols[0], cols[1]);
        game.setRound(cols[2]);
        game.setLastUpdated(LocalDateTime.parse(cols[3], DATE_TIME_FORMATTER));
        int maxCol = Math.min(4 + sf_numPlayers, cols.length);
        List<String[]> players = new ArrayList<>();
        for (int x = 4; x < maxCol; x += 1) {
          Matcher playerMatcher = sf_tsvPlayerPattern.matcher(cols[x]);
          if (!playerMatcher.matches()) {
            throw new IllegalArgumentException("Unexpected player data on line " + rowNum + ": '" +
                cols[x] + "'");
          }
          players.add(new String[]{ playerMatcher.group(1), playerMatcher.group(2) });
        }
        game.setPlayers(players.stream()
            .map(p -> p[0])
            .collect(Collectors.toList()));

        for (String[] data : players) {
          String player = data[0];
          String stats = data[1];
          if (stats != null) {
            Matcher statMatcher;
            if (game.isFinished()) {
              statMatcher = sf_tsvFinishedPlayerStatPattern.matcher(stats);
            } else {
              statMatcher = sf_tsvPlayerStatPattern.matcher(stats);
            }
            if (!statMatcher.find()) {
              throw new IllegalArgumentException("Unexpected player stats on line " + rowNum +
                  ": '" + stats + "'");
            }
            game.setCountry(player, statMatcher.group(1));
            if (game.isFinished()) {
              game.setScore(player, Integer.parseInt(statMatcher.group(2)));
            } else {
              game.setUnusedWorkers(player, Integer.parseInt(statMatcher.group(2)));
              game.setScore(player, Integer.parseInt(statMatcher.group(3)));
            }
          }
        }

        games.put(game.getName(), game);
        line = reader.readLine();
      }
    }
    return games;
  }


  /**
   * Helper class to get the last updated timestamp for a game.
   * Caches finished/running game lists.
   */
  private static class LastUpdatedHelper {
    private final Document m_finishedGames;
    private final Document m_runningGames;

    LastUpdatedHelper() throws IOException {
      m_finishedGames = Jsoup.connect(sf_finishedGamesUrl)
          .get();
      m_runningGames = Jsoup.connect(sf_runningGamesUrl)
          .get();
    }

    void updateLastUpdated(Game game) throws IOException {
      Document doc = game.isFinished() ? m_finishedGames : m_runningGames;
      for (Element tr : doc.select("#gamemanager-gamelists > table > tbody > tr")) {
        Elements tds = tr.select("td");
        String name = tds.get(2).text();
        if (name.startsWith(game.getName())) {
          String lastUpdate = tds.get(5).text();
          game.setLastUpdated(calculateTimestamp(lastUpdate));
          return;
        }
      }
      throw new IllegalStateException("Cannot find " + game.getName() + " in list of " +
          (game.isFinished() ? "finished" : "running") + " games");
    }
  }
}
