import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LogInFrame extends JFrame{

    MongoDBUtil db=new MongoDBUtil();

    public LogInFrame(){
        initJFrame();
        initJPanel();
        initJButton();
        this.setVisible(true);
    }

    private void initJFrame() {
        this.setSize(600,600);
        this.setTitle("登录界面");
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLayout(null);
        this.getContentPane().setBackground(Color.cyan);
    }

    private void initJButton(){
        JButton logInButton=new JButton("Log In");
        logInButton.setBounds(200,400,100,50);
        logInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        this.getContentPane().add(logInButton);

        JButton registerButton=new JButton("Register");
        registerButton.setBounds(350,400,100,50);
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                new RegisterFrame();
            }
        });
        getContentPane().add(registerButton);
    }

    private void initJPanel(){
        JPanel jPanel=new JPanel(new GridLayout(3,2,10,10));
        jPanel.setBorder(BorderFactory.createEmptyBorder());
        jPanel.setBounds(100,200,300,200);
        jPanel.add(new JLabel("User Name:"));
        jPanel.add(new JTextField());
        jPanel.add(new JLabel("Password:"));
        JPasswordField jPasswordField=new JPasswordField();
        char echoChar=jPasswordField.getEchoChar();
        JButton eyeButton=new JButton("\uD83D\uDC41");
        eyeButton.setBounds(400,270,50,60);
        eyeButton.addActionListener(new ActionListener() {
            boolean isVisible=false;

            @Override
            public void actionPerformed(ActionEvent e) {
                isVisible=!isVisible;
                if(isVisible){
                    jPasswordField.setEchoChar((char) 0);
                    eyeButton.setText("\uD83D\uDC41");
                }
                else{
                    jPasswordField.setEchoChar(echoChar);
                    eyeButton.setText("◯");
                }
            }
        });
        getContentPane().add(eyeButton);
        jPanel.add(jPasswordField);
        jPanel.setBackground(Color.cyan);
        getContentPane().add(jPanel);
    }
}