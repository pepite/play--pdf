/**
 *
 * Copyright 2010, Lunatech Labs.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * User: nicolas
 * Date: Feb 14, 2010
 *
 */
package play.modules.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.apache.commons.io.FilenameUtils;

import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Scope;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

public class PDF {

    public static class Options {

        public String FOOTER = null;
        public String FOOTER_TEMPLATE = null;
        public String HEADER = null;
        public String HEADER_TEMPLATE = null;
        public String ALL_PAGES = null;
        public String EVEN_PAGES = null;
        public String ODD_PAGES = null;
        
        public String filename = null;

        public IHtmlToPdfTransformer.PageSize pageSize = IHtmlToPdfTransformer.A4P;
    }

    public static class MultiDocument {
    	public String template;
    	public Options options;

    	public MultiDocument(String template, Options options) {
			this.template = template;
			this.options = options;
		}
    }
    
    /**
     * Render the corresponding template
     *
     * @param args The template data
     */
    public static void renderPDF(Object... args) {
    	// stuuuuuupid typing
    	OutputStream os = null;
    	writePDF(os, args);
    }

    /**
     * Render the corresponding template into a file
     * @param file the file to render to, or null to render to the current Response object
     * @param args the template data
     */
    public static void writePDF(File file, Object... args) {
    	try {
    		OutputStream os = new FileOutputStream(file);
    		writePDF(os);
    		os.flush();
			os.close();
		} catch (IOException e) {
			throw new UnexpectedException(e);
		}
    }
    
    /**
     * Render the corresponding template into a file
     * @param out the stream to render to, or null to render to the current Response object
     * @param args the template data
     */
    public static void writePDF(OutputStream out, Object... args) {
        String templateName = null;
        final Http.Request request = Http.Request.current();
        final String format = request.format;

        Options options = null;
        if (args.length > 0 && args[0] instanceof Options) {
            options = (Options) args[0];
        } else if (args.length > 1 && args[1] instanceof Options) {
            options = (Options) args[1];
        }

        if (args.length > 0 && args[0] instanceof String && LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
            templateName = args[0].toString();
        } else {
            templateName = request.action.replace(".", "/") + "." + (format == null ? "html" : format);
        }
        templateName = resolveTemplateName(templateName, request, format);
        
        renderTemplateAsPDF(out, templateName, options, args);
    }

    private static String resolveTemplateName(String templateName, Request request, String format) {
        if (templateName.startsWith("@")) {
            templateName = templateName.substring(1);
            if (!templateName.contains(".")) {
                templateName = request.controller + "." + templateName;
            }
            templateName = templateName.replace(".", "/") + "." + (format == null ? "html" : format);
        }
        VirtualFile template = Play.getVirtualFile(templateName);
        if (template == null || !template.exists()) {
            if (templateName.lastIndexOf("." + format) != -1) {
            	templateName = templateName.substring(0, templateName.lastIndexOf("." + format)) + ".html";
            }
        }
        return templateName;
	}

	/**
     * Render a specific template
     *
     * @param templateName The template name
     * @param args         The template data
     */
    public static void renderTemplateAsPDF(OutputStream out, String templateName, Options options, Object... args) {
        Scope.RenderArgs templateBinding = Scope.RenderArgs.current();
        for (Object o : args) {
            List<String> names = LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.getAllLocalVariableNames(o);
            for (String name : names) {
                templateBinding.put(name, o);
            }
        }
        templateBinding.put("session", Scope.Session.current());
        templateBinding.put("request", Http.Request.current());
        templateBinding.put("flash", Scope.Flash.current());
        templateBinding.put("params", Scope.Params.current());
        try {
            templateBinding.put("errors", Validation.errors());
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
        try {
            Template template = TemplateLoader.load(templateName);
        	if (options == null) {
        		options = new Options();
        	}
            if(out == null){
            	// we're rendering to the current Response object
            	if (options.filename == null) {
            		options.filename = FilenameUtils.getBaseName(templateName) + ".pdf";
            	}
            	throw new RenderPDFTemplate(template, templateBinding.data, options);
            }else{
            	RenderPDFTemplate renderer = new RenderPDFTemplate(template, templateBinding.data, options);
            	renderer.writePDF(out, Http.Request.current(), Http.Response.current());
            }
        } catch (TemplateNotFoundException ex) {
            if (ex.isSourceAvailable()) {
                throw ex;
            }
            StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex);
            if (element != null) {
                throw new TemplateNotFoundException(templateName, Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
            } else {
                throw ex;
            }
        }
    }

}
