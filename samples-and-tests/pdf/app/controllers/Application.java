package controllers;

import play.Logger;
import play.Play;
import play.libs.IO;
import play.mvc.Controller;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static play.modules.pdf.PDF.Options;
import static play.modules.pdf.PDF.renderPDF;

public class Application extends Controller {

//    public static void index() {
//        renderPDF();
//    }

    public static void index() throws Exception {

        // Load the textile markup for the home page.
        String id = "home";
        String textile = getTextile(id);

        // Add the home page to the list of pages to render.
        String html = toHTML(textile);
        String title = getTitle(textile);

          // Add each page linked from a numbered list on the home page, e.g. # "Installation guide":install
        final Pattern p = Pattern.compile("^#\\s*\"[^\"]+\":([^#\\s]+)", Pattern.MULTILINE);
        final Matcher m = p.matcher(textile);
        while (m.find()) {
            String pageId = m.group(1);
            if (!pageId.startsWith("http://") && !pageId.startsWith("/")) {
                html += toHTML(getTextile(pageId));
            }
        }

        Options options = new Options();
        options.FOOTER = "<span style='font-size: 8pt;font-style:italic;color: #666;'> Generated with playframework pdf module.</span><span style=\" color: rgb(141, 172, 38);float: right;font-size: 8pt;\">Page <pagenumber>/<pagecount></span>";
        options.filename = "Playframework manual";
        renderPDF(options, html, title);
    }

    private static String getTextile(String pageId) throws IOException {
        File page = new File(Play.frameworkPath + "/documentation/manual/" + pageId + ".textile");
        // We don't want to render the first one.
        if (!page.exists()) {
            // Ignore
            //notFound("Manual page for " + pageId + " not found");
            return "";
        }
        String textile = IO.readContentAsString(page);
        return textile;
    }

    static String getTitle(String textile) {
        if (textile.length() == 0) {
            return "";
        }
        return textile.split("\n")[0].substring(3).trim();
    }

    static String toHTML(String textile) {
        String html = new jj.play.org.eclipse.mylyn.wikitext.core.parser.MarkupParser(new jj.play.org.eclipse.mylyn.wikitext.textile.core.TextileLanguage())
                .parseToHtml(textile);
        html = html.substring(html.indexOf("<body>") + 6, html.lastIndexOf("</body>"));
        //html = html.replaceAll("images/", "http://www.playframework.org/documentation/1.0.3/images/");
        html = html.replaceAll("images/([^\"]*)", "file://" + Play.frameworkPath + "/documentation/images/$1.png");
        return html;
    }

}              
