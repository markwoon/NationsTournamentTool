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
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
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
  public static final DateTimeFormatter MABIWEB_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-M-d H:m");
  public static final DateTimeFormatter US_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("M/d/yyyy H:m");
  public static final DateTimeFormatter DOT_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("d.M.yyyy H:m");


  private static void writeHeaders(PrintWriter tsvWriter, ExcelWriter xlsWriter, int numPlayers) {
    List<String> headers = new ArrayList<>();
    headers.add("Game ID");
    headers.add("Game Name");
    headers.add("Round");
    headers.add("Started");
    headers.add("Last Updated");
    headers.add("Finished");
    for (int x = 1; x <= numPlayers; x += 1) {
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
      int numPlayers = 0;
      if (games.size() > 1) {
        for (String name : games.keySet()) {
          Game game = games.get(name);
          if (numPlayers < game.getPlayers().size()) {
            numPlayers = game.getPlayers().size();
          }
        }
      }
      writeHeaders(tsvWriter, xlsWriter, numPlayers);

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
        writeCell(tsvWriter, xlsWriter, game, game.getGameStarted() == null ? "" : game.getGameStarted().format(MABIWEB_DATE_TIME_FORMATTER));
        writeCell(tsvWriter, xlsWriter, game, game.getLastUpdated() == null ? "" : game.getLastUpdated().format(MABIWEB_DATE_TIME_FORMATTER));
        writeCell(tsvWriter, xlsWriter, game, game.getGameFinished() == null ? "" : game.getGameFinished().format(MABIWEB_DATE_TIME_FORMATTER));
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
        newResultRow(tsvWriter, xlsWriter, numPlayers);
        writeCell(tsvWriter, xlsWriter, null, "Group " + group);
        boolean started = false;
        for (PlayerPoints pp : new TreeSet<>(totalPoints.get(group).values())) {
          newResultRow(tsvWriter, xlsWriter, numPlayers);
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

  private static void newResultRow(PrintWriter tsvWriter, ExcelWriter xlsWriter, int numPlayers) {
    tsvWriter.println();
    xlsWriter.newRow();
    int max = 6 + (numPlayers * 7);
    for (int x = 0; x < max; x += 1) {
      tsvWriter.print("\t");
      xlsWriter.writeCell("", null);
    }
  }


  private static LocalDateTime parseTime(String text) {
    try {
      return LocalDateTime.parse(text, MABIWEB_DATE_TIME_FORMATTER);
    } catch (DateTimeParseException ex) {
      try {
        if (text.contains(".")) {
          return LocalDateTime.parse(text, DOT_DATE_TIME_FORMATTER);
        }
        if (Locale.getDefault().getCountry().equals("US")) {
          return LocalDateTime.parse(text, US_DATE_TIME_FORMATTER);
        }
        throw new IllegalArgumentException("Cannot parse date and time from '" + text + "'");
      } catch (DateTimeParseException ex2) {
        throw new IllegalArgumentException("Cannot parse date and time from '" + text + "'");
      }
    }
  }

  public static SortedMap<String, Game> readTsv(Path file) throws IOException {

    SortedMap<String, Game> games = new TreeMap<>();
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      int numPlayers = 0;
      String line = reader.readLine();
      if (line != null && line.startsWith("Game ID")) {
        if (line.contains("\tP6\t")) {
          numPlayers = 6;
        } else if (line.contains("\tP5\t")) {
          numPlayers = 5;
        } else if (line.contains("\tP4\t")) {
          numPlayers = 4;
        } else if (line.contains("\tP3\t")) {
          numPlayers = 3;
        }
        line = reader.readLine();
      }
      while (line != null) {
        String[] cols = splitCsvLine(line);
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

        parsePlayers(cols, game, numPlayers);

        games.put(game.getName(), game);
        line = reader.readLine();
      }
    }
    return games;
  }

  private static String[] splitCsvLine(String line) {
    String[] cols = line.split("\t");
    for (int x = 0; x < cols.length; x += 1) {
      String value = StringUtils.stripToNull(cols[x]);
      if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
        value = StringUtils.stripToNull(value.substring(1, value.length() - 1));
      }
      cols[x] = value;
    }
    return cols;
  }


  private static void parsePlayers(String[] cols, Game game, int numPlayers) {
    int maxCol = Math.min(6 + (7 * numPlayers), cols.length);
    game.setPlayers(new ArrayList<>());
    for (int x = 0; x < numPlayers; x += 1) {
      int colNum = 6 + (7 * x);
      if (colNum >= maxCol) {
        break;
      }
      String p = cols[colNum];
      game.getPlayers().add(p);
      if (cols.length > colNum + 1) {
        game.setCountry(p, cols[colNum + 1]);
        if (!game.isDead() && cols.length > colNum + 2) {
          if (cols[colNum + 2] != null) {
            game.setVp(p, Integer.parseInt(cols[colNum + 2]));
          }
          if (!game.isFinished() && cols.length > colNum + 3) {
            if (cols[colNum + 3] != null) {
              game.setUnusedWorkers(p, Integer.parseInt(cols[colNum + 3]));
            }
          }
        }
      }
    }
  }
}
