package org.mybatis.spring.boot.autoconfigure;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyBatisAotRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    public static final List<Class> REFLECTION_CLASS = new ArrayList<>(Arrays.asList(
            // mb class
            org.apache.ibatis.javassist.util.proxy.ProxyFactory.class,
            org.apache.ibatis.scripting.xmltags.XMLLanguageDriver.class,
            org.apache.ibatis.scripting.defaults.RawLanguageDriver.class,
            org.apache.ibatis.logging.slf4j.Slf4jImpl.class,
            org.apache.ibatis.session.SqlSession.class,
            org.mybatis.spring.mapper.MapperFactoryBean.class,
            org.mybatis.spring.SqlSessionTemplate.class,
            org.apache.ibatis.mapping.BoundSql.class,
            org.apache.ibatis.plugin.Interceptor.class,
            org.apache.ibatis.executor.Executor.class,
            org.apache.ibatis.cache.impl.PerpetualCache.class,
            org.apache.ibatis.cache.decorators.FifoCache.class,
            org.mybatis.spring.SqlSessionFactoryBean.class,


            // sys class
            java.util.ArrayList.class,
            java.util.LinkedList.class,
            java.util.HashMap.class,
            java.util.LinkedHashMap.class,
            java.util.HashSet.class,
            java.io.ByteArrayOutputStream.class

    ));


    public static final List<Class> JDK_PROXY_CLASS = new ArrayList<>(Arrays.asList(
            // mb class
            org.apache.ibatis.session.SqlSession.class,
            org.apache.ibatis.executor.Executor.class
    ));


    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

        for (Class<?> cls : REFLECTION_CLASS) {
            hints.reflection().registerType(cls
                    , MemberCategory.DECLARED_CLASSES
                    ,MemberCategory.DECLARED_FIELDS
                    ,MemberCategory.INVOKE_DECLARED_METHODS
                    ,MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        }

        for (Class<?> cls : JDK_PROXY_CLASS) {
            hints.proxies().registerJdkProxy(cls);
        }

        hints.resources().registerPattern("**/*.*");
    }
}
