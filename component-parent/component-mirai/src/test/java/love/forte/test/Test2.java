package love.forte.test;

import love.forte.common.utils.scanner.HutoolClassesScanner;

import java.util.Set;

/**
 * @author <a href="https://github.com/ForteScarlet"> ForteScarlet </a>
 */
public class Test2 {
    public static void main(String[] args) {
        final Set<Class<?>> collection = new HutoolClassesScanner().scan("love.forte", c -> c.getSimpleName().endsWith("Kt")).getCollection();

    }

}
