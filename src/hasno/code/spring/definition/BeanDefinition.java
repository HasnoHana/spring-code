package hasno.code.spring.definition;

import hasno.code.spring.annotation.Autowired;
import hasno.code.spring.annotation.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class BeanDefinition {

    private final String beanName;

    private final Constructor<?> constructor;

    private final Class<?> beanType;

    private final List<Field> autowiredList;

    public BeanDefinition(Class<?> type) {
        this.beanType = type;
        Component annotation = type.getAnnotation(Component.class);
        if (annotation.name().isEmpty()) {
            this.beanName = type.getName();
        }else{
            this.beanName = annotation.name();
        }
        try {
            this.constructor = type.getConstructor();
            // 1. 获取 autowired 的field
            Field[] declaredFields = type.getDeclaredFields();
            autowiredList = new ArrayList<>();
            for(Field fd : declaredFields) {
                if(fd.isAnnotationPresent(Autowired.class)) {
                    // 2. 获取 autowired 的目标 type
                    autowiredList.add(fd);
                }
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public String getBeanName() {
        return beanName;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public Class<?> getBeanType() {
        return beanType;
    }

    public List<Field> getAutowiredList() {
        return autowiredList;
    }
}
