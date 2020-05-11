package com.yize.downloader.model;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SerializationHelper {
    public static void writeToDisk(List<LocalFileInfo> info){
        File file=new File(info.get(0).getFilename()+".download_config.json");
        try {
            if(file.exists()){
                file.delete();
            }
            file.createNewFile();
            LocalFileInfo[] localFileInfos=new LocalFileInfo[info.size()];
            info.toArray(localFileInfos);
            ObjectOutputStream oos=new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(localFileInfos);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<LocalFileInfo> restoreDownloadInfo(String filename){
        List<LocalFileInfo> localFileInfoList=new ArrayList<LocalFileInfo>();
        filename+=".download_config.json";
        File file=new File(filename);
        try {
            ObjectInputStream ois=new ObjectInputStream(new FileInputStream(file));
            LocalFileInfo[] infos=(LocalFileInfo[])ois.readObject();
            localFileInfoList= Arrays.asList(infos);
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        localFileInfoList.sort((o1, o2) -> o2.getThreadId()-o1.getThreadId());
        return localFileInfoList;
    }
}
