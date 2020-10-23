package MVC;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import Annotation.*;

public class MyHandlerAdapter {
    public MyModelAndView handle(HttpServletRequest req, HttpServletResponse resp, MyHandlerMapping handler) throws Exception{

        //解析形参
        Map<String,Integer> paramIndexMapping = new HashMap<String, Integer>();
        Annotation[][] pa = handler.getMethod().getParameterAnnotations();
        for (int i = 0; i < pa.length; i++) {
            for (Annotation a : pa[i]) {
                if (a instanceof MyRequestParam) {
                    String paramName = ((MyRequestParam) a).value();
                    if (!"".equals(paramName.trim())) {
                        paramIndexMapping.put(paramName,i);
                    }
                }
            }
        }

        //提取request和response的位置
        Class<?> [] paramterTypes = handler.getMethod().getParameterTypes();
        for (int i = 0; i < paramterTypes.length; i++) {
            Class<?> type = paramterTypes[i];
            if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                paramIndexMapping.put(type.getName(),i);
            }
        }


        //解析实参
        Map<String,String[]> paramsMap = req.getParameterMap();
        Object [] paramValues = new Object[paramterTypes.length];

        for (Map.Entry<String, String[]> param : paramsMap.entrySet()) {
            String value = Arrays.toString(paramsMap.get(param.getKey()))
                    .replaceAll("\\[|\\]","")
                    .replaceAll("\\s","");

            if(!paramIndexMapping.containsKey(param.getKey())){continue;}
            int index = paramIndexMapping.get(param.getKey());
            paramValues[index] = caseStringValue(value,paramterTypes[index]);
        }

        if(paramIndexMapping.containsKey(HttpServletRequest.class.getName())){
            int index = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[index] = req;
        }

        if(paramIndexMapping.containsKey(HttpServletResponse.class.getName())){
            int index = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[index] = resp;
        }

        //硬编码
        Object result = handler.getMethod().invoke(handler.getController(),paramValues);

        if(result == null || result instanceof Void){return null;}

        boolean isModelAndView = handler.getMethod().getReturnType() == MyModelAndView.class;
        if(isModelAndView){
            return (MyModelAndView)result;
        }

        return null;
    }

    private Object caseStringValue(String value, Class<?> paramterType) {
        if(String.class == paramterType){
            return value;
        }
        if (Integer.class == paramterType){
            return Integer.valueOf(value);
        }else if(Double.class == paramterType){
            return Double.valueOf(value);
        }else {
            if(value != null){
                return value;
            }
            return null;
        }
    }
}
