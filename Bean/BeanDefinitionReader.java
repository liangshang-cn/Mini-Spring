package Bean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BeanDefinitionReader {

    private Properties contextConfig = new Properties();

    private List<String> registryBeanClasses = new ArrayList<String>();

    public BeanDefinitionReader(String[] configLocations) {
        //1、读取配置文件
        doLoadConfig(configLocations[0]);

        //2、扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
    }

    public List<BeanDefinition> loadBeanDefinitions() {
        List<BeanDefinition> result = new ArrayList<BeanDefinition>();

        try {
            for (String className : registryBeanClasses) {
                Class<?> beanClass = Class.forName(className);

                if(beanClass.isInterface()){continue;}

                //默认是类名首字母小写
                result.add(doCreateBeanDefinition(toLowerFirstCase(beanClass.getSimpleName()),beanClass.getName()));

                //如果在DI时字段类型是接口，那么我们读取它实现类的配置
                for (Class<?> i : beanClass.getInterfaces()) {
                    result.add(doCreateBeanDefinition(i.getName(),beanClass.getName()));
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    private BeanDefinition doCreateBeanDefinition(String factoryBeanName,String beanClassName) {
        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setFactoryBeanName(factoryBeanName);
        beanDefinition.setBeanClassName(beanClassName);
        return beanDefinition;
    }


    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader()
                .getResource("/" + scanPackage.replaceAll("\\.","/"));
        File classPath = new File(url.getFile());

        for (File file : classPath.listFiles()) {

            //如果是文件夹，递归
            if(file.isDirectory()){
                doScanner(scanPackage + "." + file.getName());
            }else {
                //取反，减少代码嵌套，更加优雅
                if (!file.getName().endsWith(".class")) {
                    continue;
                }

                //拿到全类名， 包名.类名
                String className = (scanPackage + "." + file.getName().replace(".class", ""));
                registryBeanClasses.add(className);
            }

        }

    }

    private void doLoadConfig(String contextConfigLocation) {

        //从classPath去找到对应的配置文件，同时读取出来
        //存放到内存中
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replaceAll("classpath:",""));
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        //因为大写字符的ASCII码和小写字母的ASCII码正好相差32
        //大写字母的ASCII码要比小写字母的ASCII码要小
        chars[0] += 32;
        return String.valueOf(chars);
    }

    public Properties getConfig() {
        return this.contextConfig;
    }
}
