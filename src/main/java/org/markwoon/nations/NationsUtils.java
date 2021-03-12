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
import org.markwoon.nations.model.Game;
import org.markwoon.nations.model.PlayerPoints;


/**
 * Utilities for reading/writing Nations data.
 *
 * @author Mark Woon
 */
public class NationsUtils {
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final int sf_numPlayers = 3;

  public static void writeTsv(SortedMap<String, Game> games, Path file,
      boolean calculateFinalUnfinishedGameScore) throws IOException {

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
            game.calculatePoints(games.values(), calculateFinalUnfinishedGameScore);
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
}
