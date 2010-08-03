import play.Play;
import play.PlayPlugin;
import play.libs.IO;
import play.libs.MimeTypes;
import play.modules.pdf.PDF;
import play.modules.pdf.RenderPDFTemplate;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.results.Result;
import play.vfs.VirtualFile;

import java.io.File;

public class PDFPlugin extends PlayPlugin {


    @Override
    public void onRoutesLoaded() {
        Router.addRoute("GET", "/@documentation/pdf", "PdfDocumentation.index");
    }


}


