package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.hsqldb.lib.StringUtil;
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
                if (!StringUtil.isEmpty(options.HEADER))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.HEADER, IHtmlToPdfTransformer.CHeaderFooter.HEADER));
                if (!StringUtil.isEmpty(options.ALL_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ALL_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ALL_PAGES));
                if (!StringUtil.isEmpty(options.EVEN_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.EVEN_PAGES, IHtmlToPdfTransformer.CHeaderFooter.EVEN_PAGES));
                if (!StringUtil.isEmpty(options.FOOTER))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.FOOTER, IHtmlToPdfTransformer.CHeaderFooter.FOOTER));
                if (!StringUtil.isEmpty(options.ODD_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ODD_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ODD_PAGES));
                pageSize = toPageSize(options.pageSize);
            }

            response.setHeader("Content-Disposition", "inline; filename=\"" + template.name + "\"");
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
                if (!StringUtil.isEmpty(options.HEADER))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.HEADER, IHtmlToPdfTransformer.CHeaderFooter.HEADER));
                if (!StringUtil.isEmpty(options.ALL_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ALL_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ALL_PAGES));
                if (!StringUtil.isEmpty(options.EVEN_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.EVEN_PAGES, IHtmlToPdfTransformer.CHeaderFooter.EVEN_PAGES));
                if (!StringUtil.isEmpty(options.FOOTER))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.FOOTER, IHtmlToPdfTransformer.CHeaderFooter.FOOTER));
                if (!StringUtil.isEmpty(options.ODD_PAGES))
                    headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ODD_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ODD_PAGES));
                pageSize = toPageSize(options.pageSize);
            }

            String content = template.render(args);
            Map properties = new HashMap();
            String uri = Play.applicationPath.toURI().toURL().toExternalForm();
            FileOutputStream out = new FileOutputStream(file);
            // TODO: The page size should be configurable
            try {
                transformer.transform(new ByteArrayInputStream(content.getBytes("UTF-8")), uri, pageSize, headerFooterList,
                        properties, out);
                out.close();
            }
            catch (final IHtmlToPdfTransformer.CConvertException e) {
                throw e;
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private static IHtmlToPdfTransformer.PageSize toPageSize(int pageSize) {
        switch (pageSize) {
            case PDF.A0L:
                return IHtmlToPdfTransformer.A0L;
            case PDF.A0P:
                return IHtmlToPdfTransformer.A0P;
            case PDF.A10L:
                return IHtmlToPdfTransformer.A10L;
            case PDF.A10P:
                return IHtmlToPdfTransformer.A10P;
            case PDF.A11L:
                return IHtmlToPdfTransformer.A11L;
            case PDF.A11P:
                return IHtmlToPdfTransformer.A11P;
            case PDF.A12L:
                return IHtmlToPdfTransformer.A12L;
            case PDF.A12P:
                return IHtmlToPdfTransformer.A12P;
            case PDF.A13L:
                return IHtmlToPdfTransformer.A13L;
            case PDF.A13P:
                return IHtmlToPdfTransformer.A13P;
            case PDF.A14L:
                return IHtmlToPdfTransformer.A14L;
            case PDF.A14P:
                return IHtmlToPdfTransformer.A14P;
            case PDF.A1L:
                return IHtmlToPdfTransformer.A1L;
            case PDF.A1P:
                return IHtmlToPdfTransformer.A1P;
            case PDF.A2L:
                return IHtmlToPdfTransformer.A2L;
            case PDF.A2P:
                return IHtmlToPdfTransformer.A2P;
            case PDF.A3L:
                return IHtmlToPdfTransformer.A3L;
            case PDF.A3P:
                return IHtmlToPdfTransformer.A3P;
            case PDF.A4L:
                return IHtmlToPdfTransformer.A4L;
            case PDF.A4P:
                return IHtmlToPdfTransformer.A4P;
            case PDF.A5L:
                return IHtmlToPdfTransformer.A5L;
            case PDF.A5P:
                return IHtmlToPdfTransformer.A5P;
            case PDF.A6L:
                return IHtmlToPdfTransformer.A6L;
            case PDF.A6P:
                return IHtmlToPdfTransformer.A6P;
            case PDF.A7L:
                return IHtmlToPdfTransformer.A7L;
            case PDF.A7P:
                return IHtmlToPdfTransformer.A7P;
            case PDF.A8L:
                return IHtmlToPdfTransformer.A8L;
            case PDF.A8P:
                return IHtmlToPdfTransformer.A8P;
            case PDF.A9L:
                return IHtmlToPdfTransformer.A9L;
            case PDF.A9P:
                return IHtmlToPdfTransformer.A9P;
            case PDF.LETTERL:
                return IHtmlToPdfTransformer.LETTERL;
            case PDF.LETTERP:
                return IHtmlToPdfTransformer.LETTERP;
            case PDF.LEGALL:
                return IHtmlToPdfTransformer.LEGALL;
            case PDF.LEGALP:
                return IHtmlToPdfTransformer.LEGALP;
            case PDF.JUNIOR_LEGALL:
                return IHtmlToPdfTransformer.JUNIOR_LEGALL;
            case PDF.JUNIOR_LEGALP:
                return IHtmlToPdfTransformer.JUNIOR_LEGALP;
            default:
                throw new UnexpectedException("Unknown page size");

        }

    }


}