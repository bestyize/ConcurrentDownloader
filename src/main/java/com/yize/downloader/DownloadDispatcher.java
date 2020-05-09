package com.yize.downloader;


import java.util.concurrent.*;

public class DownloadDispatcher {
    private static int DEFAULT_THREAD_COUNT=128;
    private ExecutorService executorService;

    private volatile static DownloadDispatcher DEFAULT_INSTANCE;
    public static DownloadDispatcher getDefault(){
        if(DEFAULT_INSTANCE==null){
            synchronized (DownloadDispatcher.class){
                if(DEFAULT_INSTANCE==null){
                    DEFAULT_INSTANCE=new DownloadDispatcher();
                }
            }
        }
        return DEFAULT_INSTANCE;
    }

    private DownloadDispatcher() {
        this(DEFAULT_THREAD_COUNT);
    }

    public DownloadDispatcher(int threadCount) {
        executorService=new ThreadPoolExecutor(threadCount
                ,threadCount
                ,0
                , TimeUnit.SECONDS
                ,new ArrayBlockingQueue<Runnable>(threadCount)
                ,new ConcurrentThreadFactory()
                ,new ExceedHandler());
    }

    private class ConcurrentThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }
    private class ExceedHandler implements RejectedExecutionHandler {

        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.out.println("超过最大线程数");
        }
    }

    public void dispatchNewTask(String downloadLink,int threadNum,DownloadListener listener){
        final ConcurrentDownloader downloader=new ConcurrentDownloader(this.executorService);
        downloader.startDownload(downloadLink,threadNum,listener);
    }

}
