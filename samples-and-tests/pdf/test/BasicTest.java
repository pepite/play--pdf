import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.junit.Test;
import play.Logger;
import play.Play;
import play.test.UnitTest;



public class BasicTest extends UnitTest {
  /**
   * Tests the loading of an image.
   * 
   * @throws IOException in case of error
   */
  @Test
  public void testLoadingOfImage() throws IOException {
    String uri = getAbsoluteUrl("/images/help");
    
    Logger.debug("Testing loading of image at '%s'", uri);
    
    URL url = new URL(uri);
    URLConnection connection = url.openConnection();
    
    // Decreases timeout as this local call should be quick to resolve (so we don't have to wait one minute before the 
    // test fails)
    connection.setReadTimeout(5000);
    
    assertTrue(connection.getContentLength() > 0);
    assertSame("image/png", connection.getContentType());
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