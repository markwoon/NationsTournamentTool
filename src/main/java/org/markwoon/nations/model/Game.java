package org.markwoon.nations.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * @author Mark Woon
 */
public class Game {
  public static final String ROUND_FINISHED = "finished";
  private static final Pattern sf_roundPattern = Pattern.compile("\\w+ \\((.*)\\) - ([AB])");
  private final String id;
  private final String name;
  private List<String> players;
  private String round;
  private String lastUpdated;
  private String activePlayer;
  private final Map<String, String> countries = new HashMap<>();
  private final Map<String, Integer> scores = new HashMap<>();
  private final Map<String, Integer> unusedWorkers = new HashMap<>();


  public Game(String id, String name) {
    this.id = id;
    this.name = name;
  }


  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }


  public @Nullable String getRound() {
    return round;
  }

  public void setRound(String round) {
    this.round = round;
  }

  public void setRoundString(String roundString) {
    Matcher m = sf_roundPattern.matcher(roundString);
    if (!m.find()) {
      throw new IllegalArgumentException("Unexpected round format: " + roundString);
    }
    switch (m.group(1)) {
      case "I" -> this.round = "1" + m.group(2);
      case "II" -> this.round = "2" + m.group(2);
      case "III" -> this.round = "3" + m.group(2);
      case "II II" -> this.round = "4" + m.group(2);
    }
  }

  public boolean isFinished() {
    return ROUND_FINISHED.equals(round);
  }


  public String getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(String lastUpdated) {
    this.lastUpdated = lastUpdated;
  }


  public List<String> getPlayers() {
    return players;
  }

  public void setPlayers(List<String> players) {
    this.players = players;
  }



  public String getActivePlayer() {
    return activePlayer;
  }

  public void setActivePlayer(String activePlayer) {
    this.activePlayer = activePlayer;
  }


  public String getCountry(String player) {
    return countries.get(player);
  }

  public void setCountry(String player, String country) {
    countries.put(player, country);
  }


  public int getUnusedWorkers(String player) {
    if (unusedWorkers.containsKey(player)) {
      return unusedWorkers.get(player);
    }
    return 0;
  }

  public void setUnusedWorkers(String player, int numUnusedWorkers) {
    unusedWorkers.put(player, numUnusedWorkers);
  }


  public int getScore(String player) {
    if (scores.containsKey(player)) {
      return scores.get(player);
    }
    return 0;
  }

  public void setScore(String player, int score) {
    if (!players.contains(player)) {
      throw new IllegalArgumentException("Player '" + player + "' is not in this game " + players);
    }
    scores.put(player, score);
  }


  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder()
        .append(id)
        .append(": ")
        .append(name)
        .append(" (")
        .append(round)
        .append(") - ");
    boolean started = false;
    if (players != null) {
      for (String player : players) {
        if (started) {
          builder.append(", ");
        } else {
          started = true;
        }
        if (player.equals(activePlayer)) {
          builder.append("_")
              .append(player)
              .append("_");
        } else {
          builder.append(player);
        }
        builder.append(" (")
            .append(getCountry(player))
            .append(", ")
            .append(getUnusedWorkers(player))
            .append(" unused workers, ")
            .append(getScore(player))
            .append("vp)");
      }
    }

    return builder.toString();
  }
}
