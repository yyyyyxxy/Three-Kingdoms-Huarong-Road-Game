import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.mongodb.client.model.Filters.eq;

public class LogInFrame extends JFrame{

    MongoDBUtil db=new MongoDBUtil();
    String userName;
    String password;

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
                if(db.getDocument("Users",eq("_id",userName))==null){
                    new LogInError();
                }
                else{
                    if(BCrypt.checkpw(password, (String) db.getDocument("Users",eq("_id",userName)).get("password"))){
                        dispose();
//                        new GameFrame();
                    }
                    else{
                        new NotMatch();
                    }
                }
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
        JTextField jTextField=new JTextField();
        jPanel.add(jTextField);
        userName=jTextField.getText();
        jPanel.add(new JLabel("Password:"));
        JPasswordField jPasswordField=new JPasswordField();
        password=jPasswordField.getUIClassID();
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

class LogInError extends JFrame{
    public LogInError(){
        initJFrame();
        initJLabel();
        initJButton();
        this.setVisible(true);
    }

    private void initJFrame(){
        this.setSize(200,400);
        this.setTitle("Error");
        this.setLocationRelativeTo(null);
        this.setLayout(null);
    }

    private void initJLabel(){
        JLabel jLabel=new JLabel("Please register first!");
        jLabel.setFont(new Font("微软雅黑",Font.ITALIC,30));
        jLabel.setForeground(Color.RED);
        this.getContentPane().add(jLabel);
    }

    private void initJButton(){
        JButton jButton=new JButton("Return");
        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        this.getContentPane().add(jButton);
    }
}

class NotMatch extends JFrame{
    public NotMatch(){
        initJFrame();
        initJLabel();
        initJButton();
        this.setVisible(true);
    }

    private void initJFrame(){
        this.setSize(200,400);
        this.setTitle("Error");
        this.setLocationRelativeTo(null);
        this.setLayout(null);
    }

    private void initJLabel(){
        JLabel jLabel=new JLabel("Sorry! Your password is wrong. Please try it again.");
        jLabel.setFont(new Font("微软雅黑",Font.ITALIC,30));
        jLabel.setForeground(Color.RED);
        this.getContentPane().add(jLabel);
    }

    private void initJButton(){
        JButton jButton=new JButton("Return");
        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        this.getContentPane().add(jButton);
    }
}