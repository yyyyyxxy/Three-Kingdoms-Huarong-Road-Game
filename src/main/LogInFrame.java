package src.main;

import javax.swing.*;
import java.awt.*;

public class LogInFrame extends JFrame{
    public LogInFrame(){
        initJPanel();
        initJFrame();
        initJButton();
    }

    private void initJFrame() {
        this.setSize(600,600);
        this.setTitle("登录界面");
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLayout(null);
        this.setVisible(true);
    }

    private void initJButton(){
        JButton logInButton=new JButton("Log In");
        logInButton.setBounds(200,400,100,50);
        this.getContentPane().add(logInButton);

        JButton registerButton=new JButton("Register");
        registerButton.setBounds(350,400,100,50);
        this.getContentPane().add(registerButton);
    }

    private void initJPanel(){
        JPanel jPanel=new JPanel(new GridLayout(3,2,10,10));
        jPanel.setBorder(BorderFactory.createEmptyBorder());
        jPanel.setBounds(100,200,300,200);
        jPanel.add(new JLabel("User Name:"));
        jPanel.add(new JTextField());
        jPanel.add(new JLabel("Password:"));
        jPanel.add(new JTextField());
        this.add(jPanel);
    }
}