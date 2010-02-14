package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;
import play.templates.Template;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 200 OK 
 */
public class RenderPDFTemplate extends Result {

    public Template template;
    private String content;

    public RenderPDFTemplate(Template template, Map<String, Object> args) {
        this.template = template;
        this.content = template.render(args);
    }

    public RenderPDFTemplate(Template template, String content) {
        this.template = template;
        this.content = content;
    }

    protected static IHtmlToPdfTransformer transformer;

    static {
        try {
            transformer = (IHtmlToPdfTransformer) Play.classloader.loadClass(IHtmlToPdfTransformer.DEFAULT_PDF_RENDERER)
                    .newInstance();
        } catch (Exception e) {
            Logger.error("Exception initializing pdf module", e);
        }
    }

    public void apply(Request request, Response response) {
        try {
            response.setHeader("Content-Disposition", "inline; filename=\"" + template.name + "\"");
            setContentTypeIfNotSet(response, "application/pdf");

            Map properties = new HashMap();
            String uri = request.url;

            try {
                transformer.transform(new ByteArrayInputStream(content.getBytes("UTF-8")), uri, IHtmlToPdfTransformer.A4P, new ArrayList(),
                        properties, response.out);
            }
            catch (final IHtmlToPdfTransformer.CConvertException e) {
                throw e;
            }
            catch (final Exception e) {
                throw new UnexpectedException(e);
            } // end catch
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

}