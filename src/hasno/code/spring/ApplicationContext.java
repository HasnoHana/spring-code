package hasno.code.spring;

import hasno.code.spring.annotation.Component;
import hasno.code.spring.definition.BeanDefinition;
import hasno.code.spring.hh.Cat;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationContext {

    private final Map<String,BeanDefinition> beanDefinitionMap = new HashMap<>();

    private final Map<String,Object> ioc = new HashMap<>();

    //提前暴露的 ioc 容器
    private final Map<String,Object> loadingIoc = new HashMap<>();

    public ApplicationContext(String packageName) throws Exception {
        initContext(packageName);
    }

    private void initContext(String packageName) throws Exception {
        // 1. 扫描包下面的所有 带有注释的类
        List<Class<?>> classList = scanClass(packageName);
        // 2. 检查是否带注释
        classList.removeIf(type -> !canCreate(type));
        // 3. 获取所有类的信息 组装成 BeanDefinition
        for (Class<?> type: classList) {
            wrapper(type);
        }
        // 4. 执行createBean
        beanDefinitionMap.values().forEach(this::createBean);

        Cat test2 = (Cat) getBean("test2");
        System.out.println(test2.get());
    }

    private void wrapper(Class<?> type) {
        BeanDefinition beanDefinition = new BeanDefinition(type);
        if(beanDefinitionMap.containsKey(beanDefinition.getBeanName())) {
            throw new RuntimeException("Bean name repeat");
        }
        beanDefinitionMap.put(beanDefinition.getBeanName(),beanDefinition);
    }

    private boolean canCreate(Class<?> type) {
        return type.isAnnotationPresent(Component.class);
    }

    private List<Class<?>> scanClass(String packageName) throws Exception {
        List<Class<?>> ret = new ArrayList<>();

        String packagePath = packageName.replace(".", File.separator);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // 获取资源 URL（file:/Users/.../target/classes/com/example/service）
        URL resource = classLoader.getResource(packagePath);
        if(resource == null) {
            throw new RuntimeException("不存在该URL");
        }
        Path path = Path.of(resource.toURI());
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path absolutePath = file.toAbsolutePath();
                if(absolutePath.toString().endsWith(".class")) {
                    String replaceString = absolutePath.toString().replace(File.separator, ".");
                    int idx = replaceString.indexOf(packageName);
                    String className = replaceString.substring(idx, replaceString.length() - ".class".length());
                    try {
                        ret.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return ret;
    }

    public Object createBean(BeanDefinition beanDefinition) {
        String beanName = beanDefinition.getBeanName();
        // 这里保证的不重复注册
        if(ioc.containsKey(beanName)) {
            return ioc.get(beanName);
        }
        if(loadingIoc.containsKey(beanName)) {
            return loadingIoc.get(beanName);
        }
        return doCreateBean(beanDefinition);
    }

    private Object doCreateBean(BeanDefinition beanDefinition) {
        Constructor<?> constructor = beanDefinition.getConstructor();
        Object bean = null;
        try {
            bean = constructor.newInstance();
            //先不进行注入
            loadingIoc.put(beanDefinition.getBeanName(),bean);
            // 将 autowired 的目标 都赋值给 field
            List<Field> autowiredList = beanDefinition.getAutowiredList();
            for(Field fd : autowiredList) {
                fd.setAccessible(true);
                fd.set(bean,getBean(fd.getType()));
            }
            //注入完成，将半成品移除
            loadingIoc.remove(beanDefinition.getBeanName());
            ioc.put(beanDefinition.getBeanName(),bean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bean;
    }

    public Object getBean(String beanName) {
        if(beanName == null) return null;
        if(ioc.containsKey(beanName)) {
            return this.ioc.get(beanName);
        }
        // 这里很关键，当 A 需要 B 注入的时候， 保证先把 B 注册为Bean
        if(beanDefinitionMap.containsKey(beanName)) {
            return createBean(beanDefinitionMap.get(beanName));
        }
        return null;
    }

    // 根据类型获取 找到的首个
    public <T> T getBean(Class<?> beanType) {
        String beanName = beanDefinitionMap.values().stream()
                .filter(bd-> beanType.isAssignableFrom(bd.getBeanType()))
                .findFirst()
                .map(BeanDefinition::getBeanName)
                .orElse(null);
        return (T) getBean(beanName);
    }

    public <T> List<T> getBeans(Class<?> beanType) {
        return beanDefinitionMap.values().stream()
                .filter(bd-> beanType.isAssignableFrom(bd.getBeanType()))
                .map(BeanDefinition::getBeanName)
                .map(this::getBean)
                .map(bean -> (T) bean)
                .toList();
    }
}
