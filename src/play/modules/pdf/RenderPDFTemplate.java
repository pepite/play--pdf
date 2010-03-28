package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;
import play.templates.Template;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 200 OK
 */
public class RenderPDFTemplate extends Result {

    public Template template;
    private String content;
    private PDF.Options options;

    public RenderPDFTemplate(Template template, Map<String, Object> args, PDF.Options options) {
        this.template = template;
        this.content = template.render(args);
        this.options = options;
    }

    public RenderPDFTemplate(Template template, String content, PDF.Options options) {
        this.template = template;
        this.content = content;
        this.options = options;
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
            // TODO: Refactor, this is similar as writePDF
            List headerFooterList = new ArrayList();
            IHtmlToPdfTransformer.PageSize pageSize = IHtmlToPdfTransformer.A4P;
            if (options != null) {
                if (!StringUtils.isEmpty(options.HEADER))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.HEADER, IHtmlToPdfTransformer.CHeaderFooter.HEADER));
                if (!StringUtils.isEmpty(options.ALL_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ALL_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ALL_PAGES));
                if (!StringUtils.isEmpty(options.EVEN_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.EVEN_PAGES, IHtmlToPdfTransformer.CHeaderFooter.EVEN_PAGES));
                if (!StringUtils.isEmpty(options.FOOTER))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.FOOTER, IHtmlToPdfTransformer.CHeaderFooter.FOOTER));
                if (!StringUtils.isEmpty(options.ODD_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ODD_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ODD_PAGES));
                pageSize = options.pageSize;
            }

            if (options.filename != null) {
                response.setHeader("Content-Disposition", "inline; filename=\"" + options.filename + "\"");
            } else {
                String name = FilenameUtils.getBaseName(template.name) + ".pdf";
                response.setHeader("Content-Disposition", "inline; filename=\"" + name + "\"");
            }
            setContentTypeIfNotSet(response, "application/pdf");

            Map properties = new HashMap();
            String uri = request.url;
            // TODO: The page size should be configurable
            try {
                transformer.transform(new ByteArrayInputStream(content.getBytes("UTF-8")), uri, pageSize, headerFooterList,
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

    public static void writePDFAsFile(File file, Template template, PDF.Options options, Map<String, Object> args) {
        try {
            List headerFooterList = new ArrayList();
            IHtmlToPdfTransformer.PageSize pageSize = IHtmlToPdfTransformer.A4P;
            if (options != null) {
                if (!StringUtils.isEmpty(options.HEADER))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.HEADER, IHtmlToPdfTransformer.CHeaderFooter.HEADER));
                if (!StringUtils.isEmpty(options.ALL_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ALL_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ALL_PAGES));
                if (!StringUtils.isEmpty(options.EVEN_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.EVEN_PAGES, IHtmlToPdfTransformer.CHeaderFooter.EVEN_PAGES));
                if (!StringUtils.isEmpty(options.FOOTER))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.FOOTER, IHtmlToPdfTransformer.CHeaderFooter.FOOTER));
                if (!StringUtils.isEmpty(options.ODD_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ODD_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ODD_PAGES));
                pageSize = options.pageSize;
            }

            String content = template.render(args);
            Map properties = new HashMap();
            String uri = Play.applicationPath.toURI().toURL().toExternalForm();
            FileOutputStream out = new FileOutputStream(file);
            try {
                transformer.transform(new ByteArrayInputStream(content.getBytes("UTF-8")), uri, pageSize, headerFooterList,
                        properties, out);
                out.flush();
                out.close();
            }
            catch (final IHtmlToPdfTransformer.CConvertException e) {
                throw e;
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    

}