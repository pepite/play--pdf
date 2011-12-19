package controllers;
import static play.modules.pdf.PDF.renderPDF;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jj.play.org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import jj.play.org.eclipse.mylyn.wikitext.textile.core.TextileLanguage;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.StopWatch;
import play.Logger;
import play.Play;
import play.libs.IO;
import play.modules.pdf.PDF.Options;
import play.mvc.Controller;
import play.mvc.Finally;



/**
 * Defines several actions that allow to print the Play! documentation as PDF. To enable additional log messages from 
 * the PDF module, just use the following parameter when starting the application:
 * 
 *   play run -Dxr.util-logging.loggingEnabled=true
 * 
 * The PDF module help is available in the 'Installed modules' section in the sidebar on:
 * 
 *   http://localhost:9000/@documentation/home
 * 
 * Or directly at:
 * 
 *   http://localhost:9000/@documentation/modules/pdf/home
 * 
 */
public class Application extends Controller {
  private static StopWatch watch;
  
  
  /**
   * Renders the specified image. This action will be triggered for example for the following url:
   * 
   *   http://localhost:9000/images/help
   * 
   * It will return the 'help.png' image file from the Play! documentation.
   * 
   * @param id identifier (i.e. filename) of the image
   * @throws IOException in case of error
   * @throws MalformedURLException in case of error
   */
  public static void image(String id) throws MalformedURLException, IOException {
    File file = new File("/" + Play.frameworkPath + "/documentation/images/" + id + ".png");
    
    if (file.exists()) {
      Logger.debug("Serving image at '%s'", file.getAbsolutePath());
      
      response.setContentTypeIfNotSet("image/png");
      renderBinary(file.toURI().toURL().openStream());
    } else {
      Logger.error("Unable to serve missing image at '%s'", file.getAbsolutePath());
      
      notFound();
    }
  }
  
  /**
   * Displays the welcome page.
   */
  public static void index() {
    Set<String> modules = Play.modules.keySet();
    
    render(modules);
  }
  
  /**
   * Generates a PDF document for the specified documentation section.
   * 
   * @param id identifier (i.e. filename) of the documentation section
   * @param html optionally specifies whether the result of this action should be displayed as HTML (to ease debugging) or not
   * @throws IOException in case of error
   */
  public static void generate(String id, Boolean html) throws IOException {
    notFoundIfNull(id);
    
    Logger.info("Starting generation of documentation section '%s'", id);
    
    // Builds the HTML for the requested Textile page
    String textile = getTextile(id);
    String content = toHTML(textile);
    String title = getTitle(textile);
    
    // Handles the special case of the homepage which will trigger the generation of the whole documentation
    if (id.equals("home")) {
      // Adds each page linked from any numbered list on the homepage, like for example:
      // 
      //   # "Installation guide":install
      // 
      final Pattern pattern = Pattern.compile("^#\\s*\"[^\"]+\":([^#\\s]+)", Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(textile);
      
      while (matcher.find()) {
        id = matcher.group(1);
        
        if (!id.startsWith("http://") && !id.startsWith("/")) {
          content += toHTML(getTextile(id));
        }
      }
    }
    
    if ((html != null) && html) {
      render(content, title);
    } else {
      watch = new StopWatch();
      watch.start();
      
      Options options = new Options();
      options.FOOTER = "<span style='font-size: 8pt;font-style:italic;color: #666;'>Generated with Play! Framework PDF Module</span><span style=\" color: rgb(141, 172, 38);float: right;font-size: 8pt;\">Page <pagenumber>/<pagecount></span>";
      options.filename = id + ".pdf";
      
      renderPDF(content, options, title);
    }
  }
  
  /**
   * Loads the Textile markup of the specified documentation section.
   * 
   * @param id identifier (i.e. filename) of the documentation section
   * @return the Textile markup of the specified documentation section
   * @throws IOException in case of error
   */
  private static String getTextile(String id) throws IOException {
    String textile = "";
    
    File file = new File(Play.frameworkPath + "/documentation/manual/" + id + ".textile");
    
    if (file.exists()) {
      textile = IO.readContentAsString(file);
      
      Logger.debug("Loaded documentation section '%s' in '%s' successfully", id, file.getAbsolutePath());
    } else {
      Logger.error("Unable to load documentation section '%s' in '%s'", id, file.getAbsolutePath());
    }
    
    return textile;
  }
  
  /**
   * Retrieves the main title from the specified Textile markup.
   * 
   * @param textile content formated with the Textile syntax
   * @return the main title from the specified Textile markup
   */
  private static String getTitle(String textile) {
    if (!textile.isEmpty()) {
      return textile.split("\n")[0].substring(3).trim();
    } else {
      return "";
    }
  }
  
  /**
   * Logs a message as soon as the generation of a PDF document is finished.
   */
  @Finally(only = {"generate"})
  private static void log() {
    if (watch != null) {
      watch.stop();
      
      Logger.info("Generated documentation as PDF successfully in %s", DurationFormatUtils.formatDurationWords(watch.getTime(), true, true));
      
      watch = null;
    } else {
      Logger.info("Generated documentation as HTML successfully");
    }
  }
  
  /**
   * Converts a Textile markup into its HTML counterpart.
   * 
   * @param textile content formated with the Textile syntax
   * @return the corresponding HTML markup
   */
  private static String toHTML(String textile) {
    // Converts the Textile markup into an HTML page
    String html = new MarkupParser(new TextileLanguage()).parseToHtml(textile);
    
    // Makes sure image paths are absolute, as otherwise the wrong route will be called. The following Textile markup:
    // 
    //  !images/help!
    // 
    // Will indeed generate something like this:
    // 
    //  <img border="0" src="images/help"/>
    // 
    html = html.replaceAll("src=\"images/", "src=\"/images/");
    
    // Extracts only the body of this HTML page
    return html.substring(html.indexOf("<body>") + 6, html.lastIndexOf("</body>"));
  }
}