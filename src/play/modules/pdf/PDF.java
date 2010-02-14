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

import java.util.List;

public class PDF {

/**
     * Render a specific template
     * @param templateName The template name
     * @param args The template data
     */
    public static void renderTemplateAsPDF(String templateName, Object... args) {
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
            throw new RenderPDFTemplate(template, templateBinding.data);
        } catch (TemplateNotFoundException ex) {
            if(ex.isSourceAvailable()) {
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
     * @param args The template data
     */
    public static void renderPDF(Object... args) {
        String templateName = null;
        final Http.Request request = Http.Request.current();
        final String format =  request.format;

        if (args.length > 0 && args[0] instanceof String && LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
            templateName = args[0].toString();
        } else {
            templateName = Http.Request.current().action.replace(".", "/") + "." + (format == null ? "html" : format);
        }
        if(templateName.startsWith("@")) {
            templateName = templateName.substring(1);
            if(!templateName.contains(".")) {
                templateName = request.controller + "." + templateName;
            }
            templateName = templateName.replace(".", "/") + "." + (format == null ? "html" : format);
        }
        VirtualFile file = Play.getVirtualFile(templateName);
        if (!file.exists()) {
            templateName = templateName.substring(0,templateName.lastIndexOf("." + format)) + ".html";
        }
        renderTemplateAsPDF(templateName, args);
    }
}
