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

    public static final int A0L = 0;
    public static final int A0P = 1;
    public static final int A10L = 2;
    public static final int A10P = 3;
    public static final int A11L = 4;
    public static final int A11P = 5;
    public static final int A12L = 6;
    public static final int A12P = 7;
    public static final int A13L = 8;
    public static final int A13P = 9;
    public static final int A14L = 10;
    public static final int A14P = 11;
    public static final int A1L = 12;
    public static final int A1P = 13;
    public static final int A2L = 14;
    public static final int A2P = 15;
    public static final int A3L = 16;
    public static final int A3P = 17;
    public static final int A4L = 18;
    public static final int A4P = 19;
    public static final int A5L = 20;
    public static final int A5P = 21;
    public static final int A6L = 22;
    public static final int A6P = 23;
    public static final int A7L = 24;
    public static final int A7P = 25;
    public static final int A8L = 26;
    public static final int A8P = 27;
    public static final int A9L = 28;
    public static final int A9P = 29;
    public static final int LETTERL = 30;
    public static final int LETTERP = 31;
    public static final int LEGALL = 32;
    public static final int LEGALP = 33;
    public static final int JUNIOR_LEGALL = 34;
    public static final int JUNIOR_LEGALP = 35;


    public static class Options {

        public String FOOTER;
        public String HEADER;
        public String ALL_PAGES;
        public String EVEN_PAGES;
        public String ODD_PAGES;

        public int pageSize = A4P;
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
            //
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
