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

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.apache.commons.io.FilenameUtils;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Scope;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.List;
import java.util.Map;

public class PDF {

    public static class Options {

        public String FOOTER = null;
        public String HEADER = null;
        public String ALL_PAGES = null;
        public String EVEN_PAGES = null;
        public String ODD_PAGES = null;
        
        public String filename = null;

        public IHtmlToPdfTransformer.PageSize pageSize = IHtmlToPdfTransformer.A4P;
    }


    /**
     * Render a specific template
     *
     * @param templateName The template name
     * @param args         The template data
     */
    public static void renderTemplateAsPDF(String templateName, Options options, Object... args) {

        // Template datas
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
            if (options.filename == null) {
                   options.filename = FilenameUtils.getBaseName(templateName) + ".pdf";
            }
            throw new RenderPDFTemplate(template, templateBinding.data, options);
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

    /**
     * Render the corresponding template
     *
     * @param args The template data
     */
    public static void renderPDF(Object... args) {
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
        if (templateName.startsWith("@")) {
            templateName = templateName.substring(1);
            if (!templateName.contains(".")) {
                templateName = request.controller + "." + templateName;
            }
            templateName = templateName.replace(".", "/") + "." + (format == null ? "html" : format);
        }
        VirtualFile file = Play.getVirtualFile(templateName);
        if (file == null || !file.exists()) {
            templateName = templateName.substring(0, templateName.lastIndexOf("." + format)) + ".html";
        }
        renderTemplateAsPDF(templateName, options, args);
    }

    public static void writePDF(File file, Object... args) {
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
        if (templateName.startsWith("@")) {
            templateName = templateName.substring(1);
            if (!templateName.contains(".")) {
                templateName = request.controller + "." + templateName;
            }
            templateName = templateName.replace(".", "/") + "." + (format == null ? "html" : format);
        }
        VirtualFile template = Play.getVirtualFile(templateName);
        if (template == null || !template.exists()) {
            templateName = templateName.substring(0, templateName.lastIndexOf("." + format)) + ".html";
        }
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
        writeTemplateAsPDF(file, templateName, options, templateBinding.data);
    }

    /**
     * Render a specific template
     *
     * @param templateName The template name
     * @param args         The template data
     */
    public static void writeTemplateAsPDF(File file, String templateName, Options options, Map<String, Object> args) {
        try {
            Template template = TemplateLoader.load(templateName);
            RenderPDFTemplate.writePDFAsFile(file, template, options, args);

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
