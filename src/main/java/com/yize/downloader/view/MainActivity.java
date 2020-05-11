package com.yize.downloader.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainActivity extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root=FXMLLoader.load(this.getClass().getClassLoader().getResource("activity_main.fxml"));
        Scene scene=new Scene(root,800,400);
        primaryStage.setTitle("ConcurrentDownloader");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
//更多请阅读：https://www.yiibai.com/javafx/javafx_gridpane.html



    public static void main(String[] args) {
        launch(args);
    }

}
