package JieChu_TestPack;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.Multiset;

public class test1 {
    public static void main(String[] args){
//        String name=sayHello("JieChu");
        String className = test1.class.getName().replace('.', '/');
        URL classUrl = test1.class.getClassLoader().getResource(className + ".class");
        if (classUrl != null) {
            System.out.println(classUrl.getPath());
        }
    }

//    public static String sayHello(String name)
//    {
//        String Name=name;
//        System.out.println(Name);
//        return Name;
//    }
}
