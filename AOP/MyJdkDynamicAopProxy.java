package AOP;



import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public class MyJdkDynamicAopProxy implements InvocationHandler{
    private MyAdvisedSupport config;
    public MyJdkDynamicAopProxy(MyAdvisedSupport config) {
        this.config = config;
    }

    public Object getProxy() {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                this.config.getTargetClass().getInterfaces(),this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        //proxy 拿到代理的对象
        //method 用户调用的方法
        //args  实参
        Map<String,MyAdvice> advices = this.config.getAdices(method,this.config.getTargetClass());


        Object returnValue;
        invokeAdvice(advices.get("before"));

        try {
            returnValue = method.invoke(this.config.getTarget(), args);
        }catch (Exception e){
            invokeAdvice(advices.get("afterThrowing"));
            e.printStackTrace();
            throw e;
        }

        invokeAdvice(advices.get("after"));

        return returnValue;
    }

    private void invokeAdvice(MyAdvice advice) {
        try {
            advice.getAdivceMethod().invoke(advice.getAspect());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
