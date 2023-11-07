package main;

public class DerivedClass extends ParentClass{
    void work()
    {
        ParentClass test1=new ParentClass();
        ParentClass test2=new ParentClass();
        fun1(test1,test2,3);
    }
    public void fun1(ParentClass test1,ParentClass test2,int test3)
    {
        return;
    }
}
