package org.markwoon.nations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * This class is responsible for sending tournament invites.
 *
 * @author Mark Woon
 */
public class InvitationSender {
  private static final String sf_waitingGamesUrl =
      "http://www.mabiweb.com/modules.php?name=Game_Manager&op=waiting_games";
  private static final String sf_joinGamePrefix =
      "http://www.mabiweb.com/modules.php?name=GM_Nations&op=join_game_pwd&g_id=";
  private static final Pattern sf_gameNumPattern = Pattern.compile(" - Game (\\d+)$");
  private final List<String> m_warnings = new ArrayList<>();
  private final List<Player> m_players = new ArrayList<>();
  /** Maps tournament game number to MabiWeb game ID. */
  private final Map<Integer, String> m_gameNumMap = new HashMap<>();
  private final Session m_session;
  private final String m_tournamentName;
  private final String m_tournamentPrefix;
  private final String m_tournamentSite;
  private final String m_senderEmail;
  private final String m_timeFrame;


  public InvitationSender(String smtpServer, String userId, String password, String senderEmail,
      String tournamentName, String tournamentPrefix, String tournamentSite, String timeFrame) {

    Properties prop = new Properties();
    prop.put("mail.smtp.auth", true);
    prop.put("mail.smtp.ssl.enable", "true");
    prop.put("mail.smtp.starttls.enable", "true");
    prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
    prop.put("mail.smtp.host", smtpServer);
    prop.put("mail.smtp.port", "465");
    prop.put("mail.smtp.ssl.trust", smtpServer);
    m_session = Session.getInstance(prop, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userId, password);
      }
    });

    m_senderEmail = senderEmail;
    m_tournamentName = tournamentName;
    m_tournamentPrefix = tournamentPrefix;
    m_tournamentSite = tournamentSite;
    m_timeFrame = timeFrame;
  }


  public void readInput(Path excelFile) throws IOException {
    try (InputStream fis = Files.newInputStream(excelFile)) {
      XSSFWorkbook workbook = new XSSFWorkbook(fis);
      XSSFSheet sheet = workbook.getSheetAt(0);

      int numGames = 0;
      for (Row row : sheet) {
        Cell userCell = row.getCell(0);
        if (userCell == null) {
          continue;
        }
        String userId = userCell.getStringCellValue();
        if (userId == null) {
          continue;
        }
        Player player = new Player(userId);

        Cell emailCell = row.getCell(1);
        if (emailCell == null) {
          m_warnings.add("Row " + (row.getRowNum() + 1) + " is missing an email address");
          continue;
        }
        player.email = emailCell.getStringCellValue();
        if (player.email == null) {
          m_warnings.add("Row " + (row.getRowNum() + 1) + " is missing an email address");
          continue;
        }

        for (int x = 2; x <= row.getLastCellNum(); x += 1) {
          Integer gameId = getInteger(row.getCell(x));
          if (gameId != null) {
            player.games.add(gameId);
          }
        }

        if (numGames == 0) {
          numGames = player.games.size();
        } else if (numGames != player.games.size()) {
          m_warnings.add("Row 1 had " + numGames + " games but row " + (row.getRowNum() + 1) +
              " has " + player.games.size());
        }
        m_players.add(player);
      }
    }
  }


  public void findGames() throws IOException {
    Document doc = Jsoup.connect(sf_waitingGamesUrl)
        .get();
    for (Element tr : doc.select("#gamemanager-gamelists > table > tbody > tr")) {
      Elements tds = tr.select("td");
      String name = tds.get(2).text();
      if (name.startsWith(m_tournamentPrefix)) {
        Matcher m = sf_gameNumPattern.matcher(name);
        if (m.find()) {
          Integer gameNum = Integer.parseInt(m.group(1));
          String gameId = tds.get(0).text();
          // make sure it's a number
          //noinspection ResultOfMethodCallIgnored
          Integer.parseInt(gameId);
          m_gameNumMap.put(gameNum, gameId);
        } else {
          m_warnings.add("Found game '" + name + "' but cannot determine game number");
        }
      }
    }

    Set<Integer> expectedGameNumbers = m_players.stream()
        .flatMap(p -> p.games.stream())
        .collect(Collectors.toSet());
    for (Integer gameNum : m_gameNumMap.keySet()) {
      expectedGameNumbers.remove(gameNum);
    }
    if (expectedGameNumbers.size() > 0) {
      m_warnings.add("Cannot find MabiWeb game ID for games " +
          expectedGameNumbers.stream()
              .map(Object::toString)
              .collect(Collectors.joining(", ")) + ".");
    }
  }


  public void sendEmails() throws MessagingException {
    for (Player player : m_players) {
      sendEmail(player);
    }
  }

  private void sendEmail(Player player) throws MessagingException {

    Message message = new MimeMessage(m_session);
    message.setFrom(new InternetAddress(m_senderEmail));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(player.email));
    message.setSubject(m_tournamentName +
        " - Games created, please join your games in the next " + m_timeFrame + "!");

    StringBuilder textBuilder = new StringBuilder()
        .append("Hi ")
        .append(player.userId)
        .append(",\n\nGames for ")
        .append(m_tournamentName)
        .append(" have been created!\n\n")
        .append("Please join your games via the links below in the next ")
        .append(m_timeFrame)
        .append(".\n\n")
        .append("""
            The password for each game is the tournament game number (e.g. Game 210 = password 210).
            
            Your games:
            
            """);
    StringBuilder htmlBuilder = new StringBuilder()
        .append("<p>Hi ")
        .append(player.userId)
        .append(",</p><p>Games for ")
        .append(m_tournamentName)
        .append(" have been created!</p>")
        .append("<p><b>Please join your games via the links below in the next ")
        .append(m_timeFrame)
        .append(".</b></p>")
        .append("""
            <p>
            The password for each game is the tournament game number (e.g. Game 210 = password 210).
            </p>
            <p>Your games:</p>
            """);

    htmlBuilder.append("<ul>");
    for (int gameNumber : player.games) {
      htmlBuilder.append("<li>");
      textBuilder.append("* ");
      String gameNum = m_gameNumMap.get(gameNumber);
      if (gameNum != null) {
        String url = sf_joinGamePrefix + gameNum;
        htmlBuilder.append("<a href=\"")
            .append(url)
            .append("\">Game ")
            .append(gameNumber)
            .append("</a>");
        textBuilder.append("Game ")
            .append(gameNumber)
            .append(": ")
            .append(url);
      } else {
        htmlBuilder.append("Game ")
            .append(gameNumber);
        textBuilder.append("Game ")
            .append(gameNumber);
      }
      htmlBuilder.append("</li>");
      textBuilder.append("\n");
    }
    htmlBuilder.append("</ul>");
    textBuilder.append("\n");

    if (m_tournamentSite != null) {
      htmlBuilder.append("<p>For more information, go to the <a href=\"")
          .append(m_tournamentSite)
          .append("\">tournament website</a>.</p>");
      textBuilder.append("\nFor more information, go to ")
          .append(m_tournamentSite)
          .append("\n");
    }
    htmlBuilder.append("<p>Good luck!</p>");
    textBuilder.append("Good luck!");

    MimeBodyPart textMimeBody = new MimeBodyPart();
    textMimeBody.setContent(textBuilder.toString(), "text/plain; charset=utf-8");

    MimeBodyPart htmlBodyPart = new MimeBodyPart();
    htmlBodyPart.setContent(htmlBuilder.toString(), "text/html; charset=utf-8");

    Multipart multipart = new MimeMultipart("alternative");
    // must add text first and HTML last
    multipart.addBodyPart(textMimeBody);
    multipart.addBodyPart(htmlBodyPart);

    message.setContent(multipart);

    Transport.send(message);

  }


  public List<String> getWarnings()  {
    return m_warnings;
  }


  public Integer getInteger(@Nullable Cell cell) {
    if (cell == null) {
      return null;
    }
    return switch (cell.getCellType()) {
      case NUMERIC -> Double.valueOf(cell.getNumericCellValue()).intValue();
      case STRING -> Integer.parseInt(cell.getStringCellValue());
      default -> null;
    };
  }


  private static final class Player {
    final String userId;
    String email;
    SortedSet<Integer> games = new TreeSet<>();

    Player(String userId) {
      this.userId = userId;
    }

    @Override
    public String toString() {
      return userId + " (" + email + "): " +
          games.stream().map(Object::toString).collect(Collectors.joining(", "));
    }
  }
}
