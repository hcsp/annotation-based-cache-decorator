package wangpeng;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        MyService service = enhanceByAnnotation();

        service.queryDatabase(1);
        service.provideHttpResponse("abc");
        service.noLog();
    }

    // 把 MyService 中所有带有 @Log 注解的方法都过滤出来
    static List<String> methodsWithLog = Stream.of(MyService.class.getMethods())
            .filter(Main::isAnnotationWithLog)
            .map(Method::getName)
            .collect(Collectors.toList());

    private static MyService enhanceByAnnotation() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return new ByteBuddy()
                // 创建用于生成子类的 builder
                .subclass(MyService.class)
                // 控制目标子类上具有哪些方法
                .method(method -> methodsWithLog.contains(method.getName()))
                // 对匹配到的方法进行拦截，传入定制化的方法实现，继续返回 builder
                .intercept(MethodDelegation.to(LoggerInterceptor.class))
                // 根据 builder 中的信息生成尚未加载的动态类型（目标子类）
                .make()
                // 尝试加载该动态类型
                .load(Main.class.getClassLoader())
                // 获取加载之后的 class 对象
                .getLoaded()
                .getConstructor()
                .newInstance();
    }

    private static boolean isAnnotationWithLog(Method method) {
        // 尝试通过反射获取方法上的注解
        return method.getAnnotation(Log.class) != null;
    }

    // 方法拦截器
    public static class LoggerInterceptor {
        public static void log(@SuperCall Callable<Void> zuper /* super */) throws Exception {
            System.out.println("Start!");
            try {
                zuper.call();
            } finally {
                System.out.println("End!");
            }
        }
    }

}
