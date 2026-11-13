package com.wikicook.support;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Records every SQL text that goes through {@code Connection.prepareStatement}, so a test can say "this request ran
 * exactly one query" (research R1). Import it with {@code @Import(StatementCounting.class)}; read {@link #statements()}
 * before and after the request. The statements of other threads count too, so use it only in tests that do nothing
 * else at the time.
 */
@TestConfiguration(proxyBeanMethods = false)
public class StatementCounting {

    private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

    public static List<String> statements() {
        return List.copyOf(STATEMENTS);
    }

    public static void reset() {
        STATEMENTS.clear();
    }

    @Bean
    static BeanPostProcessor countingDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                return bean instanceof DataSource dataSource && !Proxy.isProxyClass(bean.getClass())
                        ? countingDataSource(dataSource) : bean;
            }
        };
    }

    private static DataSource countingDataSource(DataSource target) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("getLogWriter")) {
                return target.getLogWriter();
            }
            if (method.getName().equals("setLogWriter")) {
                target.setLogWriter((PrintWriter) args[0]);
                return null;
            }
            if (method.getName().equals("getParentLogger")) {
                throw new SQLFeatureNotSupportedException();
            }
            Object result = call(target, method, args);
            if (result instanceof Connection connection && method.getName().equals("getConnection")) {
                return countingConnection(connection);
            }
            return result;
        };
        // AutoCloseable lets Spring still call close() on the pool when the context shuts down
        return (DataSource) Proxy.newProxyInstance(StatementCounting.class.getClassLoader(),
                new Class<?>[] {DataSource.class, AutoCloseable.class}, handler);
    }

    private static Connection countingConnection(Connection target) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("prepareStatement") && args != null && args.length > 0
                    && args[0] instanceof String sql) {
                STATEMENTS.add(sql);
            }
            return call(target, method, args);
        };
        return (Connection) Proxy.newProxyInstance(StatementCounting.class.getClassLoader(),
                new Class<?>[] {Connection.class}, handler);
    }

    private static Object call(Object target, Method method, Object[] args) throws Throwable {
        try {
            if (method.getDeclaringClass() == AutoCloseable.class && !(target instanceof AutoCloseable)) {
                return null;
            }
            Method actual = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            actual.setAccessible(true);
            return actual.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (NoSuchMethodException e) {
            return method.invoke(target, args);
        }
    }

}
