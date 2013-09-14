package play.modules.pdf.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import play.exceptions.UnexpectedException;

/**
 * Class to manage the two different Binder user in Play
 * <ul>
 * <li>LVEnhancerRuntimer in Play master</li>
 * <li>LocalVariablesNamesTracer in Play 1.2.x, 1.3.x</li>
 * </ul>
 * User: Alexandre Date: Sep 14, 2013
 * 
 */
public class PdfBinderHelper {

    public enum BinderType {
        /**
         * Binder of Play V1.2.x, V1.3.x
         */
        LOCAL_VARIABLE,
        /**
         * Binder of Play master
         */
        LVENHANCER;
    }

    public BinderType binderType;
    private Method getVariablesNameMethod;

    public PdfBinderHelper() {
        this.init();
    }

    public Object getVariableNames() {
        return getVariableNames(null);
    }

    public Object getVariableNames(Object o) {
        try {
            switch (binderType) {
            case LOCAL_VARIABLE:
                return getVariablesNameMethod.invoke(null, o);
            case LVENHANCER:
                Object paramsNames = getVariablesNameMethod.invoke(null);
                Class<?> paramsNamesClazz = Class
                        .forName("play.classloading.enhancers.LVEnhancer$LVEnhancerRuntime$ParamsNames");
                if (paramsNamesClazz != null) {
                    Field field = paramsNamesClazz.getDeclaredField("varargs");
                    if (field != null) {
                        return field.get(paramsNames);
                    }
                }
                break;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Init the class to detect the binder
     * 
     */
    private void init() {
        try {
            // play <= v1.2.3
            Class<?> clazz = Class
                    .forName("play.classloading.enhancers.LocalvariablesNamesEnhancer$LocalVariablesNamesTracer");
            if (clazz != null) {
                getVariablesNameMethod = clazz.getMethod("getAllLocalVariableNames", Object.class);
                this.binderType = BinderType.LOCAL_VARIABLE;
            }
        } catch (ClassNotFoundException e) {
            // play > v1.2.3
            try {
                Class<?> clazz = Class.forName("play.classloading.enhancers.LVEnhancer$LVEnhancerRuntime");
                if (clazz != null) {
                    getVariablesNameMethod = clazz.getMethod("getParamNames");
                    this.binderType = BinderType.LVENHANCER;
                }
            } catch (Exception e1) {
                throw new UnexpectedException(e1);
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }

    }

}
