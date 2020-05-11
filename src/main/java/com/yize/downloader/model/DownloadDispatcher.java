package com.yize.downloader.model;


import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.*;

public class DownloadDispatcher {
    /**
     * 线程池最大运行线程数量
     */
    private static int DEFAULT_THREAD_COUNT=128;
    private ExecutorService executorService;
    private Map<String, ConcurrentDownloader> TASK_MAP=new ConcurrentHashMap<>();
    /**
     * 双检锁单例模式
     */
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

    /**
     * 创建一个线程池
     * @param threadCount
     */
    public DownloadDispatcher(int threadCount) {
        executorService=new ThreadPoolExecutor(64
                ,threadCount
                ,10
                , TimeUnit.SECONDS
                ,new ArrayBlockingQueue<Runnable>(threadCount)
                ,new ConcurrentThreadFactory()
                ,new ExceedHandler());
    }

    /**
     * 线程工厂
     */
    private class ConcurrentThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    /**
     * 包和策略
     */
    private class ExceedHandler implements RejectedExecutionHandler {

        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.out.println("超过最大线程数");
        }
    }

    /**
     * 创建一个下载任务
     * @param downloadLink
     * @param threadNum
     * @param listener
     */
    public void dispatchNewTask(String downloadLink,int threadNum,DownloadListener listener){
        ConcurrentDownloader downloader=new ConcurrentDownloader(this.executorService);
        TASK_MAP.put(downloadLink,downloader);
        downloader.startDownload(downloadLink,threadNum,listener);
    }

    public void pauseDownload(String downloadLink){
        if(!TASK_MAP.containsKey(downloadLink)){
            return;
        }
        TASK_MAP.remove(downloadLink).pauseDownload();
    }

}
