package org.markwoon.nations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.markwoon.nations.model.Game;
import org.markwoon.nations.model.PlayerPoints;

import static com.google.common.io.Files.getNameWithoutExtension;


/**
 * Utilities for reading/writing Nations data.
 *
 * @author Mark Woon
 */
public class NationsUtils {
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  public static final DateTimeFormatter DATE_TIME_FORMATTER2 =
      DateTimeFormatter.ofPattern("M/d/yyyy H:m");
  private static final int sf_numPlayers = 3;


  private static void writeHeaders(PrintWriter tsvWriter, ExcelWriter xlsWriter) {
    List<String> headers = new ArrayList<>();
    headers.add("Game ID");
    headers.add("Game Name");
    headers.add("Round");
    headers.add("Started");
    headers.add("Last Updated");
    headers.add("Finished");
    for (int x = 1; x <= sf_numPlayers; x += 1) {
      headers.add("P" + x);
      headers.add("Country");
      headers.add("VP");
      headers.add("Workers");
      headers.add("Points");
      headers.add("Score");
      headers.add("Time");
    }
    headers.add("Tournament Points");
    for (String header : headers) {
      tsvWriter.print(header);
      tsvWriter.print("\t");
      xlsWriter.writeHeader(header);
    }
    tsvWriter.println();
  }

  private static void writeCell(PrintWriter tsvWriter, ExcelWriter xlsWriter, Game game,
      String value) {
    value = StringUtils.stripToEmpty(value);
    tsvWriter.print(value);
    tsvWriter.print("\t");
    xlsWriter.writeCell(value, game);
  }

  private static void writeCell(PrintWriter tsvWriter, ExcelWriter xlsWriter, Game game,
      long value) {
    tsvWriter.print(value);
    tsvWriter.print("\t");
    xlsWriter.writeCell(value, game);
  }


  public static void writeTsv(SortedMap<String, Game> games, Path file,
      boolean addSlowPenalty) throws IOException {

    try (PrintWriter tsvWriter = new PrintWriter(Files.newBufferedWriter(file));
         ExcelWriter xlsWriter = new ExcelWriter("Tournament")) {
      writeHeaders(tsvWriter, xlsWriter);

      DecimalFormat decimalFormat = new DecimalFormat();
      decimalFormat.setMaximumFractionDigits(2);
      SortedMap<String, SortedMap<String, PlayerPoints>> totalPoints = new TreeMap<>();
      for (String name : games.keySet()) {
        Game game = games.get(name);
        if (game.hasVp() && !game.isDead()) {
          game.calculatePoints(games.values(), addSlowPenalty, LocalDateTime.now());
        }
        xlsWriter.newRow();
        writeCell(tsvWriter, xlsWriter, game, game.getId());
        writeCell(tsvWriter, xlsWriter, game, game.getName());
        writeCell(tsvWriter, xlsWriter, game, game.getRound());
        writeCell(tsvWriter, xlsWriter, game, game.getGameStarted() == null ? "" : game.getGameStarted().format(DATE_TIME_FORMATTER));
        writeCell(tsvWriter, xlsWriter, game, game.getLastUpdated() == null ? "" : game.getLastUpdated().format(DATE_TIME_FORMATTER));
        writeCell(tsvWriter, xlsWriter, game, game.getGameFinished() == null ? "" : game.getGameFinished().format(DATE_TIME_FORMATTER));
        if (game.getPlayers() != null) {
          for (String player : game.getPlayers()) {
            writeCell(tsvWriter, xlsWriter, game, player);
            writeCell(tsvWriter, xlsWriter, game, game.getCountry(player));
            if (game.isDead() || !game.hasVp()) {
              writeCell(tsvWriter, xlsWriter, game, "");
              writeCell(tsvWriter, xlsWriter, game, "");
              writeCell(tsvWriter, xlsWriter, game, "");
              writeCell(tsvWriter, xlsWriter, game, "");
              writeCell(tsvWriter, xlsWriter, game, "");
            } else {
              writeCell(tsvWriter, xlsWriter, game, game.getVp(player));
              writeCell(tsvWriter, xlsWriter, game, game.getUnusedWorkers(player));
              writeCell(tsvWriter, xlsWriter, game, game.getVp(player) + game.getUnusedWorkers(player));
              writeCell(tsvWriter, xlsWriter, game, decimalFormat.format(game.getPoints(player)));
              writeCell(tsvWriter, xlsWriter, game, game.getElapsedTime(player));
            }
          }
          // tournament score
          StringBuilder builder = new StringBuilder();
          if (game.hasVp() && !game.isDead()) {
            for (String player : game.getPlayers()) {
              if (builder.length() > 0) {
                builder.append(", ");
              }
              float points = game.getPoints(player);
              builder.append(player)
                  .append(" (")
                  .append(decimalFormat.format(points));
              if (!game.isFinished() && player.equals(game.getSlowestPlayer())) {
                builder.append(", SLOWEST");
              }
              builder.append(")");

              SortedMap<String, PlayerPoints> groupMap = totalPoints
                  .computeIfAbsent(game.getGroup(), g -> new TreeMap<>());
              PlayerPoints playerPoints = groupMap.computeIfAbsent(player, PlayerPoints::new);
              playerPoints.addPoints(points, game.isFinished());
            }
          }
          writeCell(tsvWriter, xlsWriter, game, builder.toString());
        }
        tsvWriter.println();
      }
      xlsWriter.newRow();

      for (String group : totalPoints.keySet()) {
        newResultRow(tsvWriter, xlsWriter);
        writeCell(tsvWriter, xlsWriter, null, "Group " + group);
        boolean started = false;
        for (PlayerPoints pp : new TreeSet<>(totalPoints.get(group).values())) {
          newResultRow(tsvWriter, xlsWriter);
          if (started) {
            writeCell(tsvWriter, xlsWriter, null, pp.toString());
          } else {
            writeCell(tsvWriter, xlsWriter, null, pp.toString());
            started = true;
          }
        }
        tsvWriter.println();
        xlsWriter.newRow();
      }

      //noinspection UnstableApiUsage
      Path excelFile = file.getParent()
          .resolve(getNameWithoutExtension(file.getFileName().toString()) + ".xls");
      xlsWriter.save(excelFile);
    }
  }

  private static void newResultRow(PrintWriter tsvWriter, ExcelWriter xlsWriter) {
    tsvWriter.println();
    xlsWriter.newRow();
    for (int x = 0; x < 27; x += 1) {
      tsvWriter.print("\t");
      xlsWriter.writeCell("", null);
    }
  }


  private static final Pattern sf_tsvPlayerPattern = Pattern.compile("(\\w+)(?: \\((.*?)\\))?");
  private static final Pattern sf_tsvPlayerStatPattern = Pattern.compile("(\\w+?), (\\d+) unused workers, (\\d+)vp");
  private static final Pattern sf_tsvFinishedPlayerStatPattern = Pattern.compile("(\\w+?), (\\d+)vp");

  private static LocalDateTime parseTime(String text) {
    try {
      return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
    } catch (DateTimeParseException ex) {
      return LocalDateTime.parse(text, DATE_TIME_FORMATTER2);
    }
  }

  public static SortedMap<String, Game> readTsv(Path file) throws IOException {

    SortedMap<String, Game> games = new TreeMap<>();
    int format = 2;
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      int rowNum = 0;
      String line = reader.readLine();
      if (line != null && line.startsWith("Game ID")) {
        if (line.contains("Player 1\tPlayer 2")) {
          format = 1;
        }
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
        if (StringUtils.stripToNull(cols[3]) != null) {
          game.setGameStarted(parseTime(cols[3]));
        }
        game.setLastUpdated(parseTime(cols[4]));
        if (StringUtils.stripToNull(cols[5]) != null) {
          game.setGameFinished(parseTime(cols[5]));
        }

        if (format == 1) {
          parseFmt1Players(cols, rowNum, game);
        } else {
          parseFmt2Players(cols, game);
        }

        games.put(game.getName(), game);
        line = reader.readLine();
      }
    }
    return games;
  }


  private static void parseFmt1Players(String[] cols, int rowNum, Game game) {
    int maxCol = Math.min(6 + sf_numPlayers, cols.length);
    List<String[]> players = new ArrayList<>();
    for (int x = 6; x < maxCol; x += 1) {
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
          game.setVp(player, Integer.parseInt(statMatcher.group(2)));
        } else {
          game.setUnusedWorkers(player, Integer.parseInt(statMatcher.group(2)));
          game.setVp(player, Integer.parseInt(statMatcher.group(3)));
        }
      }
    }
  }

  private static void parseFmt2Players(String[] cols, Game game) {
    int maxCol = Math.min(6 + (7 * sf_numPlayers), cols.length);
    game.setPlayers(new ArrayList<>());
    for (int x = 0; x < sf_numPlayers; x += 1) {
      int colNum = 6 + (7 * x);
      if (colNum >= maxCol) {
        break;
      }
      String p = cols[colNum];
      game.getPlayers().add(p);
      game.setCountry(p, cols[colNum + 1]);
      if (!game.isDead()) {
        game.setVp(p, Integer.parseInt(cols[colNum + 2]));
        if (!game.isFinished()) {
          game.setUnusedWorkers(p, Integer.parseInt(cols[colNum + 3]));
        }
      }
    }
  }
}
