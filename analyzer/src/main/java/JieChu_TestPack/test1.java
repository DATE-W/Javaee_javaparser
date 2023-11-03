package JieChu_TestPack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.Multiset;

public class test1 {
    public static void main(String[] args){
//        String name=sayHello("JieChu");
        Map<String,Integer> a=new HashMap<>();
        a.put("1",1);
        a.put("2",2);
        a.put("3",1);

        for (Map.Entry<String,Integer> entry : a.entrySet()){
            System.out.println(entry.getKey()+"  "+entry.getValue());
        }
    }

//    public static String sayHello(String name)
//    {
//        String Name=name;
//        System.out.println(Name);
//        return Name;
//    }
}
