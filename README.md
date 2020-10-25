# Mini-Spring
学习Spring框架的过程中，深感Spring框架设计之精妙，于是从0到1手写了一个微型的Spring框架，实现了IoC容器依赖注入、AOP、MVC等模块的基本功能。
文件结构如下：
* Bean
  * BeanDefinition
  * BeanDefinitionReader
  * BeanWrapper
* MVC
  * MyDispatcherServlet
  * MyHandlerAdapter
  * MyHandlerMapping
  * MyModelAndView
  * MyView
  * MyViewResolver
* Annotation
  * MyAutowired
  * MyController
  * MyRequestMapping
  * MyRequestParam
  * MyService
* AOP
  * MyAdvice
  * MyAdviceSupport
  * MyAopConfig
  * MyJDKDynamicAopProxy
* MyApplicationContext
