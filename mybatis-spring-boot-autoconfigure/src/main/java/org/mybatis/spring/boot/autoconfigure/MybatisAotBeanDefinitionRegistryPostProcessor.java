package org.mybatis.spring.boot.autoconfigure;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScannerRegistrar;
import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.NativeDetector;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class MybatisAotBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private String getAotRuntimeMainClass(){
        String cmd = System.getProperty("sun.java.command");
        if (StringUtils.hasText(cmd)){
            String[] split = cmd.split(" ");
            for (int i = 0; i < split.length; i++) {
                if (split[i].equals("org.springframework.boot.SpringApplicationAotProcessor") && i + 1 < split.length){
                    return split[i + 1];
                }
            }
        }
        throw new RuntimeException("can not find AotRuntimeMainClass");
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        if (Optional.ofNullable(System.getProperty("spring.aot.processing")).orElse("false").equals("true")){
            String mainClass = getAotRuntimeMainClass();
            String[] splitArray = mainClass.split("\\.");
            Assert.state(splitArray.length > 1, "mainClass must be a full class name");
            String path = Arrays.stream(Arrays.copyOf(splitArray, splitArray.length - 1))
                    .reduce("", (a, b) -> a + "/" + b).substring(1);

            SpringBootVFS springBootVFS = new SpringBootVFS();
            try {
                for (String cls : springBootVFS.list(path)) {
                    String s = cls.replaceAll("/", ".");
                    if (s.endsWith(".class")) {
                        s = s.substring(0, s.length() - 6);
                    }
                    Class<?> claszz = Class.forName(s);
                    if (claszz.isInterface() && claszz.getAnnotation(Mapper.class) != null){
                        processMapperInterface(claszz);
                    }

                    if (!claszz.isInterface() && claszz.getAnnotation(Service.class) != null){
                        processServiceTranstional(claszz);
                    }

                }


            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void processMapperInterface(Class clazz){
        safeAdd(MyBatisAotRuntimeHintsRegistrar.REFLECTION_CLASS,clazz);
        safeAdd(MyBatisAotRuntimeHintsRegistrar.JDK_PROXY_CLASS,clazz);

        Set<Class<?>> entityClasses = new HashSet<>();

        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            Class<?> returnType = method.getReturnType();
            if (!returnType.isPrimitive() && !returnType.getName().startsWith("java.util")) {
                entityClasses.add(returnType);
            }

            Parameter[] parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                Class<?> parameterType = parameter.getType();
                if (!parameterType.isPrimitive() && !parameterType.getName().startsWith("java.util")) {
                    entityClasses.add(parameterType);
                }
            }
        }

        entityClasses.forEach(e-> safeAdd(MyBatisAotRuntimeHintsRegistrar.REFLECTION_CLASS,e));

    }

    private void processServiceTranstional(Class clazz){
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getAnnotation(org.springframework.transaction.annotation.Transactional.class) != null){
                Class<?>[] interfaces = clazz.getInterfaces();
                if (interfaces.length > 0){
                    for (Class<?> anInterface : interfaces) {
                        safeAdd(MyBatisAotRuntimeHintsRegistrar.JDK_PROXY_CLASS,anInterface);
                    }
                    safeAdd(MyBatisAotRuntimeHintsRegistrar.REFLECTION_CLASS,clazz);
                }else {
                    throw new RuntimeException("Using transactions requires the current bean to inherit the interface. -- " + clazz.getName());
                }
                break;
            }
        }
    }

    private <T> void safeAdd(List<T> list,T obj){
        if (!list.contains(obj)){
            list.add(obj);
        }
    }



    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {

    }
}
