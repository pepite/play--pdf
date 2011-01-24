import play.PlayPlugin;
import play.mvc.Router;

public class PDFPlugin extends PlayPlugin {


    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@documentation/pdf", "PdfDocumentation.index");
    }


}


