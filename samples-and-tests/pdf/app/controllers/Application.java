package controllers;

import play.data.validation.Validation;
import play.mvc.*;

import java.net.URL;
import static play.modules.pdf.PDF.*;

public class Application extends Controller {

    public static void index() {
        renderPDF();
    }

}              
