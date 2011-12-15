import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.junit.Test;
import play.Logger;
import play.Play;
import play.test.UnitTest;



public class BasicTest extends UnitTest {
  /**
   * Tests the loading of an image from the application public folder.
   * 
   * @throws IOException in case of error
   */
  @Test
  public void testLoadingOfApplicationImage() throws IOException {
    String uri = getAbsoluteUrl("/public/images/favicon.png");
    
    Logger.debug("Testing loading of image at '%s'", uri);
    
    URL url = new URL(uri);
    URLConnection connection = url.openConnection();
    
    assertTrue(connection.getContentLength() > 0);
    assertEquals("image/png", connection.getContentType());
  }
  
  /**
   * Tests the loading of an image from the Play! documentation and returned by the application.
   * 
   * @throws IOException in case of error
   */
  @Test
  public void testLoadingOfDocumentationImage() throws IOException {
    String uri = getAbsoluteUrl("/images/help");
    
    Logger.debug("Testing loading of image at '%s'", uri);
    
    URL url = new URL(uri);
    URLConnection connection = url.openConnection();
    
    assertTrue(connection.getContentLength() > 0);
    assertEquals("image/png", connection.getContentType());
  }
  
  /**
   * Tests the loading of an image available on Internet.
   * 
   * @throws IOException in case of error
   */
  @Test
  public void testLoadingOfInternetImage() throws IOException {
    String uri = "http://www.google.fr/images/srpr/logo3w.png";
    
    Logger.debug("Testing loading of image at '%s'", uri);
    
    URL url = new URL(uri);
    URLConnection connection = url.openConnection();
    
    assertTrue(connection.getContentLength() > 0);
    assertEquals("image/png", connection.getContentType());
  }
  
  /**
   * Builds the absolute url for the specified path.
   * 
   * @param path url path
   * @return the absolute url for the specified path
   */
  private String getAbsoluteUrl(String path) {
    String host = Play.configuration.getProperty("http.address", "127.0.0.1");
    String port = Play.configuration.getProperty("http.port", "9000");
    
    return "http://" + host + ":" + port + path;
  }
}