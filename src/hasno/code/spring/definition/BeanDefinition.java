package hasno.code.spring.definition;

import hasno.code.spring.annotation.Component;

public class BeanDefinition {

    private final String beanName;

    public BeanDefinition(Class<?> type) {
        Component annotation = type.getAnnotation(Component.class);
        if (annotation.name().isEmpty()) {
            this.beanName = type.getName();
        }else{
            this.beanName = annotation.name();
        }
    }

    public String getBeanName() {
        return beanName;
    }
}
