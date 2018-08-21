package name.lorenzani.andrea.reststresser;

import name.lorenzani.andrea.reststresser.invoker.Manager;

import java.util.Arrays;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "I am trying to break your application!!!" );
        String url = "http://localhost:80";
        if(args.length>0) url = args[0];
        int numThreads = 10;
        if(args.length>1) numThreads = Integer.parseInt(args[1]);
        System.out.println(String.format("Sending requests to '%s' with %d threads", url, numThreads));
        Manager manager = new Manager(url, numThreads);
        manager.stress();
    }
}
