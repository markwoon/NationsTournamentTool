package org.markwoon.nations.model;

import java.text.DecimalFormat;
import java.util.Objects;
import com.google.common.collect.ComparisonChain;


/**
 * Tracks tournament points for a player.
 *
 * @author Mark Woon
 */
public class PlayerPoints implements Comparable<PlayerPoints> {
  private final String m_name;
  private float m_points = 0;
  private int m_matches = 0;


  public PlayerPoints(String name) {
    m_name = name;
  }

  public String getName() {
    return m_name;
  }

  public float getPoints() {
    return m_points;
  }

  public void addPoints(float points) {
    m_points += points;
    m_matches += 1;
  }

  public int getMatches() {
    return m_matches;
  }


  @Override
  public int compareTo(PlayerPoints o) {
    return ComparisonChain.start()
        .compare(o.getPoints(), m_points)
        .compare(m_name, o.getName())
        .result();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (getClass() != o.getClass()) {
      return false;
    }
    PlayerPoints p = (PlayerPoints)o;
    return Objects.equals(m_name, p.getName()) &&
        Objects.equals(m_points, p.getPoints());
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_name, m_points);
  }

  @Override
  public String toString() {
    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(2);
    return m_name + ": " + df.format(m_points) + " (" + m_matches + "/4)";
  }
}
