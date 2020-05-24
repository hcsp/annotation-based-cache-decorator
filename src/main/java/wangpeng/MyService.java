package wangpeng;

public class MyService {
    @Log
    public void queryDatabase(int param) {
        System.out.println("query db:" + param);
    }

    @Log
    public void provideHttpResponse(String param) {
        System.out.println("provide http service:" + param);
    }

    public void noLog() {
        System.out.println("no have log!");
    }
}
