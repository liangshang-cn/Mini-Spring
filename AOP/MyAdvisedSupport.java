package AOP;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MyAdvisedSupport {

    private MyAopConfig config;
    private Class targetClass;
    private Object target;
    private Pattern pointCutClassPattern;
    private Map<Method,Map<String, MyAdvice>> methodCache;

    //解析读取出来的配置信息
    //肯定又要用到正则
    public MyAdvisedSupport(MyAopConfig config) {
        this.config = config;
    }


    public Class getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
        parse();
    }

    private void parse() {

        String pointCut = config.getPointCut()
                .replaceAll("\\.","\\\\.")
                .replaceAll("\\\\.\\*",".*")
                .replaceAll("\\(","\\\\(")
                .replaceAll("\\)","\\\\)");

        //pointCut=public .* com.gupaoedu.vip.demo.service..*Service..*(.*)
        String pointCutForClassRegex = pointCut.substring(0,pointCut.lastIndexOf("\\(") - 4);

        //提取Class的全名 com.gupaoedu.vip.demo.service..*Service
        pointCutClassPattern = Pattern.compile(pointCutForClassRegex.substring(pointCutForClassRegex.lastIndexOf(" ") + 1));

        try {
            //思想从来没有变
            //保存方法和通知的关系
            methodCache = new HashMap<Method, Map<String, MyAdvice>>();

            Pattern pointCutPattern = Pattern.compile(pointCut);

            //com.gupaoedu.vip.demo.aspect.LogAspect
            Class aspectClass = Class.forName(this.config.getAspectClass());
            Map<String,Method> aspectMethods = new HashMap<String, Method>();
            for (Method method : aspectClass.getMethods()) {
                aspectMethods.put(method.getName(),method);
            }

            for (Method method : this.targetClass.getMethods()) {
                String methodString = method.toString();

                if(methodString.contains("throws")){
                    methodString = methodString.substring(0,methodString.lastIndexOf("throws")).trim();
                }

                Matcher matcher = pointCutPattern.matcher(methodString);
                if(matcher.matches()){
                    Map<String,MyAdvice> adivces = new HashMap<String, MyAdvice>();

                    //前置通知
                    if(!(null == config.getAspectBefore() || "".equals(config.getAspectBefore()))){
                        adivces.put("before",new MyAdvice(aspectClass.newInstance(),
                                aspectMethods.get(config.getAspectBefore())));
                    }

                    //前置通知
                    if(!(null == config.getAspectAfter() || "".equals(config.getAspectAfter()))){
                        adivces.put("after",new MyAdvice(aspectClass.newInstance(),
                                aspectMethods.get(config.getAspectAfter())));
                    }

                    //异常通知
                    if(!(null == config.getAspectAfterThrow() || "".equals(config.getAspectAfterThrow()))){
                        adivces.put("afterThrowing",new MyAdvice(aspectClass.newInstance(),
                                aspectMethods.get(config.getAspectAfterThrow())));
                    }

                    methodCache.put(method,adivces);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public boolean ponitCutMatch() {
        return pointCutClassPattern.matcher(this.targetClass.getName()).matches();
    }


    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Map<String,MyAdvice> getAdices(Method method, Class targetClass) throws Exception {
        Map<String,MyAdvice> cache = methodCache.get(method);

        //技巧：代理以后的方法
        if(null == cache){
            Method m = targetClass.getMethod(method.getName(),method.getParameterTypes());
            cache = methodCache.get(m);
            this.methodCache.put(m,cache);
        }
        return cache;
    }
}
