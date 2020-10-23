package MVC;


import Annotation.MyRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;


public class MyDispatcherServlet extends HttpServlet {

    private MyApplicationContext applicationContext;

    private List<MyHandlerMapping> handlerMappings = new ArrayList<MyHandlerMapping>();

    private Map<MyHandlerMapping,MyHandlerAdapter> handlerAdapters = new HashMap<>();

    private List<MyViewResolver> viewResolvers = new ArrayList<MyViewResolver>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6.调用
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            //resp.getWriter().write("500 Exception Detail: " + Arrays.toString(e.getStackTrace()));

            Map<String,Object> model = new HashMap<String, Object>();
            model.put("detail","500 Exception Detail");
            model.put("stackTrace",Arrays.toString(e.getStackTrace()));
            try {
                processDispatchResult(req,resp,new MyModelAndView("500",model));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        //1、根据URL去拿到一个HandlerMapping
        MyHandlerMapping handler = getHandler(req);

        if (handler == null) {
            processDispatchResult(req,resp,new MyModelAndView("404"));
            return;
        }

        //2、根据HandlerMapping获得一个HandlerAdapter
        MyHandlerAdapter ha = getHandlerAdapter(handler);

        //3、根据HandlerAdpater拿到一个ModelAndView
        MyModelAndView mv = ha.handle(req, resp, handler);

        //4、根据ModelAndView决定选择哪个ViewResolver进行解析渲染
        processDispatchResult(req,resp,mv);
    }

    private MyHandlerAdapter getHandlerAdapter(MyHandlerMapping handler) {
        if(this.handlerAdapters.isEmpty()){return null;}
        return this.handlerAdapters.get(handler);
    }

    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, MyModelAndView mv) throws Exception {
        if(null == mv){return;}

        if(this.viewResolvers.isEmpty()){return;}

        for (MyViewResolver viewResolver : this.viewResolvers) {
            MyView view = viewResolver.resolveViewName(mv.getViewName());
            view.render(mv.getModel(),req,resp);
            return;
        }
    }

    private MyHandlerMapping getHandler(HttpServletRequest req) {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");

        for (MyHandlerMapping mapping : this.handlerMappings) {
            Matcher matcher = mapping.getPattern().matcher(url);
            if(!matcher.matches()){continue;}
            return mapping;
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //================== IoC、DI  ===============
        applicationContext = new MyApplicationContext(config.getInitParameter("contextConfigLocation"));

        //==================  MVC ===================
        //5、初始化HandlerMapping
//        doInitHandlerMapping();
        initStrategies(applicationContext);

        //========== 初始化阶段完成  ===========
        System.out.println("GP Spring framework is init.");

    }

    private void initStrategies(MyApplicationContext context) {
        //handlerMapping
        initHandlerMappings(context);
        //初始化参数适配器
        initHandlerAdapters(context);
        //初始化视图转换器
        initViewResolvers(context);
    }

    private void initViewResolvers(MyApplicationContext context) {
        String templateRoot = context.getConfig().getProperty("templateRoot");
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();

        File templateRootDir = new File(templateRootPath);

        for (File file : templateRootDir.listFiles()) {
            this.viewResolvers.add(new MyViewResolver(templateRoot));
        }

    }

    private void initHandlerAdapters(MyApplicationContext context) {
        for (MyHandlerMapping mapping : handlerMappings) {
            this.handlerAdapters.put(mapping,new MyHandlerAdapter());
        }
    }

    private void initHandlerMappings(MyApplicationContext context) {
        if(applicationContext.getBeanDefintionCount() == 0){
            return;
        }

        String [] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {

            Object instance = applicationContext.getBean(beanName);
            Class<?> clazz = instance.getClass();

            String baseUrl = "";
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            for (Method method : clazz.getMethods()) {
                if(!method.isAnnotationPresent(MyRequestMapping.class)){ continue; }

                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);

                //  /demo////query/
                String regex = ("/" + baseUrl + "/" + requestMapping.value())
                        .replaceAll("\\*",".*")
                        .replaceAll("/+","/");
//                handlerMapping.put(url,method);
                Pattern pattern = Pattern.compile(regex);
                handlerMappings.add(new MyHandlerMapping(pattern,instance,method));

                System.out.println("Mapped : " + regex + "," + method);
            }
        }
    }
}