package play.modules.pdf;

import org.apache.commons.io.IOUtils;
import play.Play;
import play.PlayPlugin;
import play.libs.IO;
import play.libs.MimeTypes;
import play.modules.pdf.RenderPDFTemplate;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.mvc.results.RenderTemplate;
import play.mvc.results.Result;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import static play.modules.pdf.PDF.*;

public class PDFPlugin extends PlayPlugin { 

    @Override
    public void onActionInvocationResult(Result result) {
        // If the format requested is pdf then use RenderPDF if possible
        final Request request = Request.current();
        final String accept = request.headers.get("accept").value();
        if ("application/pdf".equals(accept) || "pdf".equals(request.format) && result instanceof RenderTemplate) {
           // We override the response, we need a better way to plug the Result into the framework
           final RenderTemplate renderTemplate = (RenderTemplate)result;
           throw new RenderPDFTemplate(renderTemplate.template, renderTemplate.getContent());
        }
   }

}