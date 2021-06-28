package NBAScrapper;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

public class MyApp {
    public static void main(String[] args) throws IOException {
        //Exit program if no args were provided.
        if(args.length == 0) {
            System.out.println("Error... No arguments found. Input search parameter eg. Luka Doncic.");
            System.exit(1);
        }

        WebClient client = new WebClient();
        client.getOptions().setThrowExceptionOnScriptError(false); //Doesn't help.
        client.getOptions().setCssEnabled(false); //No need for css
        client.getOptions().setJavaScriptEnabled(false); //Doesn't work. Error.

        String playerName = StringUtils.join(args, " "); //Join args into players name.
        System.out.println("Searching for: " + playerName);
        playerName = playerName.replace(" ", "+"); //Search parameters must be seperated by '+' in url string.
        String url = "https://www.basketball-reference.com/search/search.fcgi?search=";
        url = url + playerName;

        HtmlPage playersStatsPage = client.getPage(url); //Load the player page.
        if(playersStatsPage.getTitleText().startsWith(("Search Results"))) {
            System.out.println("No perfect match to parameters... searching for closest matching player.");
            try {
                List<DomNode> searchResults = playersStatsPage.getElementById("players").getChildNodes();
                DomNode firstPlayer = searchResults.get(2); //First element is empty, second is the title, third is the player card.
                String firstPlayerUrl = firstPlayer.getChildNodes().get(3).asNormalizedText();
                String newUrl = "https://www.basketball-reference.com/" + firstPlayerUrl;
                playersStatsPage = client.getPage(newUrl);
                System.out.println(playersStatsPage.getTitleText());
                System.out.println("-----------------------------------------------");
                printScoreTableContentsForDesiredPage(playersStatsPage);
            }
            catch (Exception e) {
                System.out.println("Error searching for player or no player found.");
                System.exit(2); //Not really needed but added non the less.
            }
        }
        else {
            System.out.println(playersStatsPage.getTitleText());
            System.out.println("-----------------------------------------------");
            printScoreTableContentsForDesiredPage(playersStatsPage);
        }
    }

    private static int findThreePtAttemptsTableCellIndex(HtmlTableRow firstRow) {
        List<HtmlTableCell> descriptionCells = firstRow.getCells();
        for (int i = 0; i < descriptionCells.size(); i++) {
            if (descriptionCells.get(i).getAttribute("data-stat").equals("fg3a_per_g")) {
                return i;
            }
        }
        return -1;
    }

    private static void printScoreTableContentsForDesiredPage(HtmlPage playerStatsPage) {
        HtmlTable scoresTable = (HtmlTable)playerStatsPage.getHtmlElementById("per_game"); //Retrieve the table that display his stats per season.
        List<HtmlTableRow> scoresTableRows = scoresTable.getRows();
        HtmlTableRow cellDescriptionsRow = scoresTableRows.get(0); //First row displays headers / descriptions of the stat displayed in the column bellow.
        int fg3papgCellIndex = findThreePtAttemptsTableCellIndex(cellDescriptionsRow); //Find cell index of desired stat. (field goal 3 point attempts per game = fg3papg)

        for (int i = 1; i < scoresTableRows.size(); i++) { //First row contains headers, last row contains total carrer averages.
            String output = "";
            String season = scoresTableRows.get(i).getCell(0).asNormalizedText(); //First column displays the season.
            if (season.equals("Career")) { //Some players have additional irrelevant information after career averages.
                break;
            }
            output += season;
            List<HtmlTableCell> currentRowCells = scoresTableRows.get(i).getCells();
            if(currentRowCells.size() == 3) { //Player didn't play during this season. (Season | age | reason for not playing
                String reason = currentRowCells.get(2).asNormalizedText();
                output += " " + reason;
            }
            else {
                String fg3papg = currentRowCells.get(fg3papgCellIndex).asNormalizedText();
                output += " " + fg3papg;
            }
            System.out.println(output);
        }
    }
}
