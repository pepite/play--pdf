package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.Play;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.modules.pdf.PDF.Options;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;
import play.templates.Template;
import play.templates.TemplateLoader;

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

    public RenderPDFTemplate(Template template, Map<String, Object> args, PDF.Options options) throws TemplateNotFoundException {
    	this(template, template.render(args), options);
    	loadHeader(args);
    }

    public RenderPDFTemplate(Template template, String content, PDF.Options options) {
        this.template = template;
        this.content = content;
        this.options = options;
    }

    private void loadHeader(Map<String, Object> args) throws TemplateNotFoundException {
    	if(!StringUtils.isEmpty(options.HEADER_TEMPLATE)){
    		Template template = TemplateLoader.load(options.HEADER_TEMPLATE);
    		HashMap<String, Object> args2 = new HashMap<String, Object>(args);
    		args2.remove("out");
    		options.HEADER = template.render(args2);
    	}
    	if(!StringUtils.isEmpty(options.FOOTER_TEMPLATE)){
    		Template template = TemplateLoader.load(options.FOOTER_TEMPLATE);
    		HashMap<String, Object> args2 = new HashMap<String, Object>(args);
    		args2.remove("out");
    		options.FOOTER = template.render(args2);
    	}
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

            response.setHeader("Content-Disposition", "inline; filename=\"" + options.filename + "\"");
            setContentTypeIfNotSet(response, "application/pdf");
            // FIX IE bug when using SSL
            if(request.secure && isIE(request))
            	response.setHeader("Cache-Control", "");

            Map properties = Play.configuration;
            String uri = request.getBase()+request.url;
            // TODO: The page size should be configurable
            try {
                transformer.transform(new ByteArrayInputStream(content.getBytes("UTF-8")), uri, options.pageSize, headerFooterList,
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

    private boolean isIE(Request request) {
		if(!request.headers.containsKey("user-agent"))
			return false;
		
		Header userAgent = request.headers.get("user-agent");
		return userAgent.value().contains("MSIE");
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
            Map properties = Play.configuration;
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
