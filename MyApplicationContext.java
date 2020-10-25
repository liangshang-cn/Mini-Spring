import AOP.MyAdvisedSupport;
import AOP.MyAopConfig;
import AOP.MyJdkDynamicAopProxy;
import Bean.BeanDefinition;
import Bean.BeanDefinitionReader;
import Bean.BeanWrapper;

import java.lang.reflect.Field;
import java.util.*;
import Annotation.*;

public class MyApplicationContext {
    private String [] configLocations;
    private BeanDefinitionReader reader;

    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<String,BeanDefinition>();

    private Map<String, BeanWrapper> factoryBeanInstanceCache = new HashMap<String, BeanWrapper>();

    private Map<String,Object> factoryBeanObjectCache = new HashMap<String, Object>();

    public MyApplicationContext(String ... configLocations){
        this.configLocations = configLocations;

        try {
            //1、读取配置文件,并且解析成了BeanDefintion对象
            reader = new BeanDefinitionReader(configLocations);
            List<BeanDefinition> beanDefinitions = reader.loadBeanDefinitions();

            //2、把实例对应的配置信息beanDefinition保存到一个Map，方便之后反复读取配置信息
            doRegisterBeanDefinition(beanDefinitions);

            //3、完成getBean()的初始调用，触发IoC和DI
            doCreateBean();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void doCreateBean() {
        for (Map.Entry<String,BeanDefinition> beanDefinitionEntry : this.beanDefinitionMap.entrySet()) {
            String beanName = beanDefinitionEntry.getKey();

            //真正触发IoC和DI的动作方法
            //第一件事创建出实例、第二件事依赖注入
            getBean(beanName);
        }
    }

    private void doRegisterBeanDefinition(List<BeanDefinition> beanDefinitions) throws Exception {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if(this.beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())){
                throw new Exception("The" + beanDefinition.getFactoryBeanName() + "already exist");
            }
            this.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(),beanDefinition);
            this.beanDefinitionMap.put(beanDefinition.getBeanClassName(),beanDefinition);
        }
    }

    public Object getBean(Class beanClass){
        return getBean(beanClass.getName());
    }

    public Object getBean(String beanName){

        //创建实例
        //1、获取BeanDefinition配置信息
        BeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);

        //2、用反射创建实例
        Object instance = instaniateBean(beanName,beanDefinition);

        //3、将创建出来的实例包装到BeanWrapper对象中
        BeanWrapper beanWrapper = new BeanWrapper(instance);

        //4、把BeanWrapper对象存入到IoC容器中
        factoryBeanInstanceCache.put(beanName,beanWrapper);

        //依赖注入
        //5、执行依赖注入
        populateBean(beanName,beanDefinition,beanWrapper);

        return this.factoryBeanInstanceCache.get(beanName).getWrapperInstance();
    }

    private void populateBean(String beanName, BeanDefinition beanDefinition, BeanWrapper beanWrapper) {
        Object instance = beanWrapper.getWrapperInstance();
        Class<?> clazz = beanWrapper.getWrapperClass();

        //只有加了注解的类才进行依赖注入
        if(!(clazz.isAnnotationPresent(MyController.class) || clazz.isAnnotationPresent(MyService.class))){
            return;
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(MyAutowired.class)) {
                continue;
            }

            MyAutowired autowired = field.getAnnotation(MyAutowired.class);
            String autowiredBeanName = autowired.value().trim();
            if ("".equals(autowiredBeanName)) {
                autowiredBeanName = field.getType().getName();
            }

            //强制访问私有属性
            field.setAccessible(true);

            try {

                if(this.factoryBeanInstanceCache.get(autowiredBeanName) == null){
                    continue;
                }

                field.set(instance,this.factoryBeanInstanceCache.get(autowiredBeanName).getWrapperInstance());
                //field.set(entry.getValue(), ioc.get(beanName));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                continue;
            }

        }

    }

    private Object instaniateBean(String beanName, BeanDefinition beanDefinition) {
        String className = beanDefinition.getBeanClassName();
        Object instance = null;

        try {
            Class<?> clazz = Class.forName(className);
            instance = clazz.newInstance();

            //此处加入AOP的介入
            MyAdvisedSupport config = instantionAopConfig(beanDefinition);
            config.setTargetClass(clazz);
            config.setTarget(instance);

            //判断要不要创建代理类
            if(config.ponitCutMatch()){
                instance = new MyJdkDynamicAopProxy(config).getProxy();
            }

            factoryBeanObjectCache.put(beanName,instance);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return instance;
    }

    private MyAdvisedSupport instantionAopConfig(BeanDefinition beanDefinition) {
        MyAopConfig config = new MyAopConfig();
        config.setPointCut(this.reader.getConfig().getProperty("pointCut"));
        config.setAspectClass(this.reader.getConfig().getProperty("aspectClass"));
        config.setAspectBefore(this.reader.getConfig().getProperty("aspectBefore"));
        config.setAspectAfter(this.reader.getConfig().getProperty("aspectAfter"));
        config.setAspectAfterThrow(this.reader.getConfig().getProperty("aspectAfterThrow"));
        config.setAspectAfterThrowingName(this.reader.getConfig().getProperty("aspectAfterThrowingName"));
        return new MyAdvisedSupport(config);
    }

    public int getBeanDefintionCount() {
        return this.beanDefinitionMap.size();
    }

    public String[] getBeanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[this.beanDefinitionMap.size()]);
    }

    public Properties getConfig() {
        return reader.getConfig();
    }
}
