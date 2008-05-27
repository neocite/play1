package play;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.db.jpa.Jpa;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;

public class Invoker {
    public static Executor executor =null;
       
    public static void invoke(Invocation invocation) {
        if (executor==null) {
            executor=Invoker.startExecutor();
        }
        executor.execute(invocation);      
    }

    public static void invokeInThread(Invocation invocation) {
        invocation.run();      
    }
    
    public static abstract class Invocation extends Thread {
    
        public abstract void execute() throws Exception;

        @Override
        public void run() {
            Play.detectChanges();
            setContextClassLoader(Play.classloader);
            LocalVariablesNamesTracer.enterMethod();
            if (Jpa.isEnabled()) Jpa.startTx(false);
            try {
                execute();
            } catch (Throwable e) {
                if (Jpa.isEnabled()) Jpa.closeTx(true);
                if(e instanceof PlayException) {
                    throw (PlayException)e;
                }
                throw new UnexpectedException(e);
            }
            if (Jpa.isEnabled()) Jpa.closeTx(false);
        }
    }

    private static Executor startExecutor () {
        Properties p = Play.configuration;
        BlockingQueue queue = new LinkedBlockingQueue ();
        int core = Integer.parseInt(p.getProperty("play.pool.core", "2"));
        int max = Integer.parseInt(p.getProperty("play.pool.max", "20"));
        int keepalive = Integer.parseInt(p.getProperty("play.pool.keepalive", "5"));
        return new ThreadPoolExecutor (core,max,keepalive*60,TimeUnit.SECONDS,queue,new ThreadPoolExecutor.AbortPolicy());
    }
}
