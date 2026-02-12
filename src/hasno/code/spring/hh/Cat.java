package hasno.code.spring.hh;

import hasno.code.spring.annotation.Autowired;
import hasno.code.spring.annotation.Component;

@Component(name = "test2")
public class Cat {
    @Autowired
    Dog doc;

    public Dog get() {
        return doc;
    }
}
