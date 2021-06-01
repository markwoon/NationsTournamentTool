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
  public static final String ROUND_DEAD = "dead";
  private static final Pattern sf_gameGroupPattern = Pattern.compile("- Group ([A-Za-z]+) -");
  private static final Pattern sf_roundPattern = Pattern.compile("\\w+ \\((.*)\\) - ([AB])");
  private final String m_id;
  private final String m_name;
  private final String m_group;
  private List<String> m_players;
  private String m_round;
  private LocalDateTime m_gameStarted;
  private LocalDateTime m_gameFinished;
  private LocalDateTime m_lastUpdated;
  private String m_activePlayer;
  private String m_slowestPlayer;
  private final Map<String, String> m_countries = new HashMap<>();
  private final Map<String, Integer> m_vp = new HashMap<>();
  private final Map<String, Integer> m_unusedWorkers = new HashMap<>();
  private final Map<String, Float> m_points = new HashMap<>();
  private final Map<String, Long> m_elapsedTime = new HashMap<>();


  public Game(String id, String name) {
    m_id = id;
    m_name = name;
    Matcher m = sf_gameGroupPattern.matcher(name);
    if (m.find()) {
      m_group = m.group(1);
    } else {
      throw new IllegalArgumentException("Game name doesn't have valid group");
    }
  }


  public String getId() {
    return m_id;
  }

  public String getName() {
    return m_name;
  }

  public String getGroup() {
    return m_group;
  }


  public @Nullable String getRound() {
    return m_round;
  }

  public void setRound(String round) {
    m_round = round;
  }

  public void setRoundString(String roundString) {
    if ("Setup phase".equalsIgnoreCase(roundString)) {
      m_round = "0";
      return;
    }
    Matcher m = sf_roundPattern.matcher(roundString);
    if (!m.find()) {
      throw new IllegalArgumentException("Unexpected round format: " + roundString);
    }
    switch (m.group(1)) {
      case "I" -> m_round = "1" + m.group(2);
      case "II" -> m_round = "2" + m.group(2);
      case "III" -> m_round = "3" + m.group(2);
      case "II II" -> m_round = "4" + m.group(2);
    }
  }

  public boolean isFinished() {
    return ROUND_FINISHED.equals(m_round);
  }

  public boolean isDead() {
    return ROUND_DEAD.equals(m_round);
  }


  public LocalDateTime getGameStarted() {
    return m_gameStarted;
  }

  public void setGameStarted(LocalDateTime gameStarted) {
    m_gameStarted = gameStarted;
  }

  public LocalDateTime getGameFinished() {
    return m_gameFinished;
  }

  public void setGameFinished(LocalDateTime gameFinished) {
    m_gameFinished = gameFinished;
  }


  public LocalDateTime getLastUpdated() {
    return m_lastUpdated;
  }

  public void setLastUpdated(LocalDateTime lastUpdated) {
    m_lastUpdated = lastUpdated;
  }


  /**
   * Gets players in turn order.
   */
  public List<String> getPlayers() {
    return m_players;
  }

  public void setPlayers(List<String> players) {
    m_players = players;
  }

  /**
   * Gets current player (null if game is over).
   */
  public @Nullable String getActivePlayer() {
    return m_activePlayer;
  }

  public void setActivePlayer(@Nullable String activePlayer) {
    m_activePlayer = activePlayer;
  }


  /**
   * Only set after calculating score for an unfinished game.
   */
  public @Nullable String getSlowestPlayer() {
    return m_slowestPlayer;
  }


  public String getCountry(String player) {
    return m_countries.get(player);
  }

  public void setCountry(String player, String country) {
    m_countries.put(player, country);
  }


  public int getUnusedWorkers(String player) {
    if (m_unusedWorkers.containsKey(player)) {
      return m_unusedWorkers.get(player);
    }
    return 0;
  }

  public void setUnusedWorkers(String player, int numUnusedWorkers) {
    m_unusedWorkers.put(player, numUnusedWorkers);
  }


  public int getVp(String player) {
    if (m_vp.containsKey(player)) {
      return m_vp.get(player);
    }
    return 0;
  }

  public void setVp(String player, int vp) {
    if (!m_players.contains(player)) {
      throw new IllegalArgumentException("Player '" + player + "' is not in this game " + m_players);
    }
    m_vp.put(player, vp);
  }

  public boolean hasVp() {
    return !m_vp.isEmpty();
  }


  public float getPoints(String player) {
    return m_points.get(player);
  }

  public long getElapsedTime(String player) {
    return m_elapsedTime.get(player);
  }


  /**
   * Calculates tournament points for players.
   */
  public void calculatePoints(Collection<Game> games, boolean addSlowPenalty,
      LocalDateTime currentTime) {
    calculateSlowestPlayer(games, currentTime);
    if (hasVp()) {
      List<Object[]> scoreData = new ArrayList<>();
      int total = 0;
      int order = 0;
      for (String p : m_players) {
        int score = m_vp.get(p) + getUnusedWorkers(p);
        int slowPenalty = 0;
        if (!isFinished() && addSlowPenalty) {
          if (p.equals(m_slowestPlayer)) {
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
        m_points.put((String)data[0], score);
      }
    }
  }

  private void calculateSlowestPlayer(Collection<Game> games, LocalDateTime currentTime) {
    // find games with current players
    Multimap<String, Game> playerGames = HashMultimap.create();
    for (Game game : games) {
      if (game.isDead()) {
        continue;
      }
      for (String p : m_players) {
        if (game.getPlayers().contains(p)) {
          playerGames.put(p, game);
        }
      }
    }
    SortedSet<Object[]> data = new TreeSet<>((o1, o2) -> ComparisonChain.start()
        .compare((long)o2[1], (long)o1[1])
        .compare((int)o1[2], (int)o2[2])
        .result());
    for (String p : playerGames.keySet()) {
      long elapsedTime = 0;
      for (Game g : playerGames.get(p)) {
        if (g.isFinished()) {
          if (g.getGameStarted() != null && g.getGameFinished() != null) {
            elapsedTime += Math.abs(ChronoUnit.SECONDS.between(g.getGameStarted(), g.getGameFinished()));
          }
        } else {
          elapsedTime += Math.abs(ChronoUnit.SECONDS.between(g.getGameStarted(), currentTime));
        }
      }
      data.add(new Object[]{ p, elapsedTime, m_players.indexOf(p) });
      m_elapsedTime.put(p, elapsedTime);
    }
    m_slowestPlayer = (String)data.first()[0];
  }


  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder()
        .append(m_id)
        .append(": ")
        .append(m_name)
        .append(" (")
        .append(m_round)
        .append(") - ");
    boolean started = false;
    if (m_players != null) {
      for (String player : m_players) {
        if (started) {
          builder.append(", ");
        } else {
          started = true;
        }
        if (player.equals(m_activePlayer)) {
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
            .append(getVp(player))
            .append("vp)");
      }
    }

    return builder.toString();
  }
}
