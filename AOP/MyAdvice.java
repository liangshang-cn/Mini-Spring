package AOP;

import java.lang.reflect.Method;

public class MyAdvice {
    private Object aspect;
    private Method adivceMethod;
    private String throwName;
    public MyAdvice(Object aspect, Method adviceMethod) {
        this.aspect = aspect;
        this.adivceMethod = adviceMethod;
    }

    public Object getAspect() {
        return aspect;
    }


    public Method getAdivceMethod() {
        return adivceMethod;
    }

    public String getThrowName() {
        return throwName;
    }

    public void setThrowName(String throwName) {
        this.throwName = throwName;
    }
}
