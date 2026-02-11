package hasno.code.spring;

import hasno.code.spring.annotation.Component;
import hasno.code.spring.definition.BeanDefinition;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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

    public void initContext(String packageName) throws Exception {
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
        for(Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            System.out.println(entry.getKey()+ " " + entry.getValue());
        }

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
        return null;
    }

    public Object getBean(String beanName) {
        return null;
    }

    public <T> T getBean(Class<?> beanType) {
        return null;
    }

    public <T> List<T> getBeans(Class<?> beanType) {
        return null;
    }
}
