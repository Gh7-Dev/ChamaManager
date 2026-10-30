package chamamanager;

import chamamanager.presentation.LoginFrame;
import javax.swing.SwingUtilities;

public class Main {
 public static void main(String[]args){


        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });     
    
}
}
