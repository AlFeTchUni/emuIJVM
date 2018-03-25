package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application
{
    private static Controller controller;

    public static void main(String[] args)
    {
        launch(args);
    }

    public static Controller getController()
    {
        return controller;
    }

    @Override
    public void start(Stage primaryStage)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
            controller = loader.getController();
            Parent root = loader.load();
            primaryStage.setTitle("emuIJVM 2.0");
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));

            Scene scene = new Scene(root, 1366, 706);
            scene.getStylesheets().add(getClass().getResource("ijvm-keywords.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void stop(){
        System.exit(0);
    }


}
