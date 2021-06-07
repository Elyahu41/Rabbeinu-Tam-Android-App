package com.elyjacobi.rabbeinutam;

import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * This class is scrapes the entire chai tables webpage and searches it for the elevation data.
 */
public class ChaiTablesScraper extends AsyncTask<Void,Void,Double> {

    /**
     * The URL to scrape, this must be set before we do any actual work.
     */
    private static String mUrl;

    /**
     * The setter method for the URL to scrape. This method should be called before you start to
     * scrape the elevation data from the webpage.
     * @param url The chai tables url to scrape
     */
    public void setUrl(String url) {
        mUrl = url;
    }

    /**
     * This is a convenience method that automatically searches the URL passed in and makes sure
     * that the page actually has the elevation data. The chai table website has 6 different options
     * for which type of sunrise/sunset table you can choose from. Astronomical, mishor (sea level,
     * and visible sunrise/sunset. However, only the webpage for the astronomical sunrise/sunset
     * tables contain the elevation data for whatever reason. Therefore, to help the user out, I
     * simply understood that I can replace whatever option they choose with the correct webpage.
     * All they need to do is choose the city, and it doesn't matter which table they choose.
     */
    private void assertUsableURL() {
        if (mUrl.contains("&cgi_types=0")) {
            mUrl = mUrl.replace("&cgi_types=0","&cgi_types=5");
        } else if (mUrl.contains("&cgi_types=1")) {
            mUrl = mUrl.replace("&cgi_types=1", "&cgi_types=5");
        } else if (mUrl.contains("&cgi_types=2")) {
            mUrl = mUrl.replace("&cgi_types=2", "&cgi_types=5");
        } else if (mUrl.contains("&cgi_types=3")) {
            mUrl = mUrl.replace("&cgi_types=3", "&cgi_types=5");
        } else if (mUrl.contains("&cgi_types=4")) {
            mUrl = mUrl.replace("&cgi_types=4", "&cgi_types=5");
        }
    }

    /**
     * This is where the real action happens. This method uses the Jsoup Api to download the whole
     * page as one very long string. (It could probably be refactored to be more efficient) After it
     * has the whole webpage, it searches it for the elevation data. Currently the data is displayed
     * in the webpage like this, " height: 30m " The method searches for the word height and a new
     * line character. This is probable not future proof, but it works for now and I do not think
     * the website will be updated for a while.
     * @return a double containing the highest elevation of the city in meters
     * @throws IOException because of the Jsoup API if an error occurs
     */
    public double saveElevationData() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        Document doc = Jsoup.connect(mUrl)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com").get();
        Elements tableElement = doc.select("table");

        Elements tableHeaderEles = tableElement.select("thead tr th");
        for (int i = 0; i < tableHeaderEles.size(); i++) {
            stringBuilder.append(tableHeaderEles.get(i).text());
            if (i != tableHeaderEles.size() - 1) {
                stringBuilder.append(',');
            }
        }
        stringBuilder.append('\n');

        Elements tableRowElements = tableElement.select(":not(thead) tr");

        for (Element row : tableRowElements) {
            Elements rowItems = row.select("td");
            for (int j = 0; j < rowItems.size(); j++) {
                stringBuilder.append(rowItems.get(j).text());
                if (j != rowItems.size() - 1) {
                    stringBuilder.append(',');
                }
            }
            stringBuilder.append('\n');
        }
        String s = stringBuilder.toString();

        return Double.parseDouble(
                s.substring(s.indexOf("height:"), s.indexOf("m\n"))//This is probably not future proof
                .replaceAll("[^\\d.]", ""));//get rid of all the letters
    }

    @Override
    protected Double doInBackground(Void... voids) {
        try {
            assertUsableURL();
            return saveElevationData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
