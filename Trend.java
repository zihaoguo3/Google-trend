import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class Trend {
    private static final String GOOGLE_APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbznXFsCKOuxtZZYs--vMYtpziNgdCSnkt_wisO5viNx7RzZo9iaxNTdKpzYmoVssASE/exec";
    public static void main(String[] args) {
        try {
            // Fetch Google Trends Data
            List<List<String>> trendsData = fetchGoogleTrends();
            
            // Send data to Google Sheets via Apps Script Web App
            sendToGoogleSheets(trendsData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<List<String>> fetchGoogleTrends() throws IOException {
        // Use Google Trends Daily API for US trends
        String urlString = "https://trends.google.com/trends/api/dailytrends?hl=en-US&tz=-480&geo=US&ns=15";
        
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        // Check if request was successful (HTTP 200)
        if (conn.getResponseCode() != 200) {
            throw new IOException("HTTP error code: " + conn.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = reader.lines().reduce("", String::concat).replace(")]}',", ""); // Clean JSON response
        reader.close();

        JSONObject jsonResponse = new JSONObject(response);
        JSONArray trends = jsonResponse.getJSONObject("default").getJSONArray("trendingSearchesDays").getJSONObject(0).getJSONArray("trendingSearches");

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Trending Topic", "Search Volume", "Date", "News Title", "News Source", "News Time", "News URL"));

        for (int i = 0; i < Math.min(10, trends.length()); i++) {
            JSONObject trend = trends.getJSONObject(i);
            String title = trend.getJSONObject("title").getString("query");
            String searchVolume = trend.optString("formattedTraffic", "N/A");
            JSONArray articles = trend.optJSONArray("articles");

            String newsTitle = "N/A", newsSource = "N/A", newsTime = "N/A", newsUrl = "N/A";
            if (articles != null && articles.length() > 0) {
                JSONObject firstArticle = articles.getJSONObject(0);
                newsTitle = firstArticle.optString("title", "N/A");
                newsSource = firstArticle.optString("source", "N/A");
                newsTime = firstArticle.optString("timeAgo", "N/A");
                newsUrl = firstArticle.optString("url", "N/A");
            }

            rows.add(List.of(title, searchVolume, "Today", newsTitle, newsSource, newsTime, newsUrl));
        }

        return rows;
    }

    private static void sendToGoogleSheets(List<List<String>> data) throws IOException {
        JSONArray jsonData = new JSONArray(data);
        byte[] postData = jsonData.toString().getBytes(StandardCharsets.UTF_8);

        URL url = new URL(GOOGLE_APPS_SCRIPT_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(postData);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = reader.readLine();
        reader.close();

        System.out.println("Google Sheets Response: " + response);
    }
}
