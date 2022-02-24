package org.markwoon.nations;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.net.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.markwoon.nations.model.Game;


/**
 * Utilities for working with MabiWeb.
 *
 * @author Mark Woon
 */
public class MabiWebHelper {
  public enum Level {EMPEROR, KING, PRINCE, CHIEFTAIN}
  private static final Splitter sf_commaSplitter = Splitter.on(",").trimResults()
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
      boolean gotDetails = getGameDetails(game);
      boolean gotLastUpdated = lastUpdatedHelper.updateLastUpdated(game);
      // if can't get details and can't get last updated, game has expired on MabiWeb
      // mark game as dead if it isn't already finished
      if (!gotDetails && !gotLastUpdated && !game.isFinished()) {
        game.setRound(Game.ROUND_DEAD);
      }
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
  private static final Pattern sf_startedPattern = Pattern.compile("Nations Game ID=\\d+, started ([\\d.]+, \\d+:\\d+:\\d+)");
  private static final Pattern sf_finishedPattern = Pattern.compile("Game finished ([\\d.]+, \\d+:\\d+:\\d+)");
  private static final Splitter sf_spaceSplitter = Splitter.on(" ").trimResults()
      .omitEmptyStrings();
  private static final DateTimeFormatter sf_gameDetailDateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy.MM.dd, HH:mm:ss");

  /**
   * Gets the details of a specific game.
   */
  public static boolean getGameDetails(Game game) throws IOException {
    Document doc = Jsoup.connect("http://www.mabiweb.com/modules.php?name=GM_Nations&op=view_game_reset&g_id=" + game.getId())
        .get();

    if (doc.text().contains("Error: can't load position for game with ID")) {
      // game has not started
      return false;
    }

    Element header = doc.getElementById("nations-gameheader");
    String headerPlayerInfo = header.text().substring(header.text().indexOf("Players:") + 8);
    Element lastActions = doc.getElementById("last_actions");

    boolean isFinished = header.html().contains("Game finished");
    // finished?
    if (isFinished) {
      // round
      game.setRound(Game.ROUND_FINISHED);

      // started
      if (game.getGameStarted() == null) {
        Matcher startedMatcher = sf_startedPattern.matcher(lastActions.text());
        if (startedMatcher.find()) {
          game.setGameStarted(LocalDateTime.parse(startedMatcher.group(1), sf_gameDetailDateTimeFormatter));
        }
      }
      // finished
      Matcher finishedMatcher = sf_finishedPattern.matcher(lastActions.text());
      if (finishedMatcher.find()) {
        game.setGameFinished(LocalDateTime.parse(finishedMatcher.group(1), sf_gameDetailDateTimeFormatter));
      }

      // players and score
      Matcher m = sf_playerFinalScorePattern.matcher(headerPlayerInfo);
      List<String[]> playerData = new ArrayList<>();
      while (m.find()) {
        playerData.add(new String[] { m.group(1), m.group(2) });
      }
      List<String> players = new ArrayList<>();
      for (String[] p : playerData) {
        players.add(p[0]);
        game.setVp(p[0], Integer.parseInt(p[1]));
      }
      game.setPlayers(players);
      game.setActivePlayer(null);

    } else {
      // round
      Matcher roundMatcher = sf_roundPattern.matcher(header.text());
      if (roundMatcher.find()) {
        game.setRoundString(roundMatcher.group(1));
      }

      // started
      Matcher startedMatcher = sf_startedPattern.matcher(lastActions.text());
      if (startedMatcher.find()) {
        game.setGameStarted(LocalDateTime.parse(startedMatcher.group(1), sf_gameDetailDateTimeFormatter));
      }

      // players
      game.setPlayers(sf_spaceSplitter.splitToList(headerPlayerInfo));
      // active player
      String players = header.html().substring(header.html().indexOf("Players:"));
      Matcher playerMatcher = sf_playerTurnPattern.matcher(players);
      if (playerMatcher.find()) {
        game.setActivePlayer(playerMatcher.group(1));
      }

      if (!game.isSettingUp()) {
        // score
        Matcher vpTableMatcher = sf_vpTablePattern.matcher(doc.html());
        if (vpTableMatcher.find()) {
          String vpTable = vpTableMatcher.group(1);
          Matcher vpMatcher = sf_vpPattern.matcher(vpTable);
          while (vpMatcher.find()) {
            game.setVp(vpMatcher.group(1), Integer.parseInt(vpMatcher.group(2)));
          }
        }
      }
    }

    if (!game.isSettingUp()) {
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
    return true;
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

    boolean updateLastUpdated(Game game) throws IOException {
      Document doc = game.isFinished() ? m_finishedGames : m_runningGames;
      for (Element tr : doc.select("#gamemanager-gamelists > table > tbody > tr")) {
        Elements tds = tr.select("td");
        String name = tds.get(2).text();
        if (name.startsWith(game.getName())) {
          String lastUpdate = tds.get(5).text();
          game.setLastUpdated(calculateTimestamp(lastUpdate));
          return true;
        }
      }
      return false;
    }
  }


  private final HttpClient m_httpClient;

  public MabiWebHelper() {
    m_httpClient = HttpClient.newBuilder()
        .cookieHandler(new CookieManager())
        .build();
  }

  public boolean login(String userId, String pwd) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://www.mabiweb.com/modules.php?name=Your_Account"))
        .timeout(Duration.ofSeconds(30))
        .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString("op=login" +
            "&username=" + URLEncoder.encode(userId, StandardCharsets.UTF_8) +
            "&user_password=" + URLEncoder.encode(pwd, StandardCharsets.UTF_8)))
        .build();
    HttpResponse<String> response = m_httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 302) {
      return false;
    }
    return isLoggedIn();
  }

  public boolean isLoggedIn() throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://www.mabiweb.com/modules.php?name=Your_Account&stop=1"))
        .timeout(Duration.ofSeconds(30))
        .build();
    HttpResponse<String> response = m_httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return !response.body().contains("<b>Login Incorrect! Please Try Again...</b>");
  }


  public boolean createGame(String name, String password, int numPlayers, Level level)
      throws IOException, InterruptedException {

    int levelNum;
    switch (level) {
      case CHIEFTAIN -> levelNum = 4;
      case PRINCE -> levelNum = 3;
      case KING -> levelNum = 2;
      case EMPEROR -> levelNum = 1;
      default -> throw new IllegalArgumentException("Unsupported level: " + level);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://www.mabiweb.com/modules.php?name=GM_Nations"))
        .timeout(Duration.ofSeconds(30))
        .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString("op=new_game" +
            "&num_allowed[]=" + numPlayers +
            "&title=" + URLEncoder.encode(name, StandardCharsets.UTF_8) +
            "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8) +
            "&level=" + levelNum +
            // players pick boards
            "&boards=2" +
            // advanced cards
            "&cards[0]=1" +
            // expert cards
            "&cards[1]=1" +
            // dynasties expansion
            "&dynasties=1"
        ))
        .build();
    HttpResponse<String> response = m_httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    String body = response.body();
    System.out.println(body);
    return body.contains("Game created");
  }



  public static List<NewGame> buildTournamentGroup3pGameList(String prefix, String group,
      int numPlayers) {
    prefix = StringUtils.stripToNull(prefix);
    Preconditions.checkNotNull(prefix);
    group = StringUtils.stripToNull(group);
    Preconditions.checkNotNull(group);
    Preconditions.checkArgument(group.length() == 1);

    int subgroups = 1;
    int gamesPerSubgroup = 12;
    switch (numPlayers) {
      case 9 -> {
        subgroups = 1;
        gamesPerSubgroup = 12;
      }
      case 12 -> {
        subgroups = 1;
        gamesPerSubgroup = 16;
      }
      case 15 -> {
        subgroups = 1;
        gamesPerSubgroup = 20;
      }
      case 18 -> {
        subgroups = 2;
        gamesPerSubgroup = 12;
      }
      case 21 -> {
        subgroups = 1;
        gamesPerSubgroup = 28;
      }
      case 24 -> {
        subgroups = 2;
        gamesPerSubgroup = 16;
      }
      case 27 -> {
        subgroups = 3;
        gamesPerSubgroup = 12;
      }
      default -> throw new UnsupportedOperationException("Cannot have a group with " + numPlayers +
          " players");
    }

    if (subgroups == 1) {
      return buildTournamentGameList(prefix, group, 0, gamesPerSubgroup);
    }
    List<NewGame> games = new ArrayList<>();
    for (int x = 0; x < subgroups; x += 1) {
      games.addAll(buildTournamentGameList(prefix, group, x + 1, gamesPerSubgroup));
    }
    return games;
  }

  public static List<NewGame> buildTournamentGroup4pGameList(String prefix, String group,
      Level level) {
    prefix = StringUtils.stripToNull(prefix);
    Preconditions.checkNotNull(prefix);
    group = StringUtils.stripToNull(group);
    Preconditions.checkNotNull(group);
    Preconditions.checkArgument(group.length() == 1);

    String groupName = prefix + " - Group " + group.toUpperCase();
    int groupNum = group.toUpperCase().charAt(0) - 'A' + 1;

    List<NewGame> games = new ArrayList<>();
    for (int gameNum = 1; gameNum <= 20; gameNum += 1) {
      String gameId = groupNum + String.format("%02d", gameNum);
      games.add(new NewGame(groupName + " - Game " + gameId, gameId, level));
    }
    return games;
  }



  public static List<NewGame> buildTournamentGameList(String prefix, String group, int subgroup,
      int numGames) {
    prefix = StringUtils.stripToNull(prefix);
    Preconditions.checkNotNull(prefix);
    group = StringUtils.stripToNull(group);
    Preconditions.checkNotNull(group);
    Preconditions.checkArgument(group.length() == 1);
    Preconditions.checkArgument(subgroup > -1 && subgroup < 9);

    List<NewGame> games = new ArrayList<>();
    String groupName = prefix + " - Group " + group.toUpperCase();
    int groupNum = group.toUpperCase().charAt(0) - 'A' + 1;
    int subMod = 0;
    if (subgroup > 0) {
      groupName += subgroup;
      subMod = (subgroup - 1) * numGames;
    }
    int gamesPerLevel = numGames / 4;
    for (int gameNum = 1; gameNum <= numGames; gameNum += 1) {
      Level level = Level.CHIEFTAIN;
      if (gameNum <= gamesPerLevel) {
        level = Level.EMPEROR;
      } else if (gameNum <= 2*gamesPerLevel) {
        level = Level.KING;
      } else if (gameNum <= 3*gamesPerLevel) {
        level = Level.PRINCE;
      }
      String gameId = groupNum + String.format("%02d", gameNum + subMod);
      games.add(new NewGame(groupName + " - Game " + gameId, gameId, level));
    }
    return games;
  }

  static final class NewGame {
    String name;
    String password;
    Level level;

    NewGame(String name, String password, Level level) {
      this.name = name;
      this.password = password;
      this.level = level;
    }

    @Override
    public String toString() {
      return name + "    (password='" + password + "', level=" + level + ")";
    }
  }
}
