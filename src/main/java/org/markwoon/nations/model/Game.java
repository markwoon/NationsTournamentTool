package org.markwoon.nations.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * Represents a Nations game.
 *
 * @author Mark Woon
 */
public class Game {
  public static final String ROUND_FINISHED = "finished";
  private static final Pattern sf_gameGroupPattern = Pattern.compile("- Group ([A-Za-z]+) -");
  private static final Pattern sf_roundPattern = Pattern.compile("\\w+ \\((.*)\\) - ([AB])");
  private final String id;
  private final String name;
  private final String group;
  private List<String> players;
  private String round;
  private LocalDateTime gameStarted;
  private LocalDateTime gameFinished;
  private LocalDateTime lastUpdated;
  private String activePlayer;
  private String slowestPlayer;
  private final Map<String, String> countries = new HashMap<>();
  private final Map<String, Integer> scores = new HashMap<>();
  private final Map<String, Integer> unusedWorkers = new HashMap<>();
  private final Map<String, Float> points = new HashMap<>();


  public Game(String id, String name) {
    this.id = id;
    this.name = name;
    Matcher m = sf_gameGroupPattern.matcher(name);
    if (m.find()) {
      group = m.group(1);
    } else {
      throw new IllegalArgumentException("Game name doesn't have valid group");
    }
  }


  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getGroup() {
    return group;
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


  public LocalDateTime getGameStarted() {
    return gameStarted;
  }

  public void setGameStarted(LocalDateTime gameStarted) {
    this.gameStarted = gameStarted;
  }

  public LocalDateTime getGameFinished() {
    return gameFinished;
  }

  public void setGameFinished(LocalDateTime gameFinished) {
    this.gameFinished = gameFinished;
  }


  public LocalDateTime getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(LocalDateTime lastUpdated) {
    this.lastUpdated = lastUpdated;
  }


  /**
   * Gets players in turn order.
   */
  public List<String> getPlayers() {
    return players;
  }

  public void setPlayers(List<String> players) {
    this.players = players;
  }


  /**
   * Gets current player (null if game is over).
   */
  public @Nullable String getActivePlayer() {
    return activePlayer;
  }

  public void setActivePlayer(@Nullable String activePlayer) {
    this.activePlayer = activePlayer;
  }


  /**
   * Only set after calculating score for an unfinished game.
   */
  public @Nullable String getSlowestPlayer() {
    return slowestPlayer;
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

  public boolean hasScores() {
    return !scores.isEmpty();
  }


  public float getPoints(String player) {
    return points.get(player);
  }

  /**
   * Calculates tournament points for players.
   */
  public void calculatePoints(Collection<Game> games, boolean calculateFinalUnfinishedGameScore) {
    if (!isFinished() && calculateFinalUnfinishedGameScore) {
      calculateSlowestPlayer(games);
    }
    if (hasScores()) {
      List<Object[]> scoreData = new ArrayList<>();
      int total = 0;
      int order = 0;
      for (String p : players) {
        int score = scores.get(p);
        int slowPenalty = 0;
        if (!isFinished() && calculateFinalUnfinishedGameScore) {
          score += getUnusedWorkers(p);
          if (p.equals(slowestPlayer)) {
            slowPenalty = -10;
          }
        }
        total += score;
        order += 1;
        scoreData.add(new Object[]{ p, score, order, slowPenalty });
      }
      scoreData.sort((o1, o2) -> {
        int score1 = (Integer)o1[1];
        int score2 = (Integer)o2[1];
        int order1 = (Integer)o1[2];
        int order2 = (Integer)o2[2];
        return ComparisonChain.start()
            .compare(score2, score1)
            .compare(order1, order2)
            .result();
      });
      int place = 0;
      for (Object[] data : scoreData) {
        place += 1;
        int bonus = (int)data[3];
        if (place == 1) {
          bonus += 10;
        } else if (place == 2) {
          bonus += 5;
        }
        float score = ((Integer)data[1] / (float)total * 100) + bonus;
        points.put((String)data[0], score);
      }
    }
  }

  private void calculateSlowestPlayer(Collection<Game> games) {
    // find games with current players
    Multimap<String, Game> playerGames = HashMultimap.create();
    Multimap<String, Game> ongoingPlayerGames = HashMultimap.create();
    for (Game game : games) {
      if (game == this) {
        continue;
      }
      for (String p : players) {
        if (game.getPlayers().contains(p)) {
          if (!game.isFinished()) {
            ongoingPlayerGames.put(p, game);
          }
          playerGames.put(p, game);
        }
      }
    }
    if (ongoingPlayerGames.keySet().size() == 1) {
      slowestPlayer = playerGames.keySet().iterator().next();
    } else {
      SortedSet<Object[]> data = new TreeSet<>((o1, o2) -> ComparisonChain.start()
          .compare((int)o2[1], (int)o1[1])
          .compare((long)o2[2], (long)o1[2])
          .compare((int)o2[3], (int)o1[3])
          .result());
      LocalDateTime curTime = LocalDateTime.now();
      for (String p : playerGames.keySet()) {
        long elapsedTime = 0;
        for (Game g : playerGames.get(p)) {
          if (g.isFinished()) {
            elapsedTime += Math.abs(ChronoUnit.MILLIS.between(g.gameStarted, g.gameFinished));
          } else {
            elapsedTime += Math.abs(ChronoUnit.MILLIS.between(g.gameStarted, curTime));
          }
          data.add(new Object[]{ p, ongoingPlayerGames.get(p).size(), elapsedTime, players.indexOf(p) });
        }
      }
      slowestPlayer = (String)data.first()[0];
    }
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
