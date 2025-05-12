package src.main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import static com.sun.glass.ui.Cursor.setVisible;

public class RegisterFrame extends JFrame{
    private HashMap<String,String> match;

    public RegisterFrame(){
        match=null;
        initFrame();
        initJPanel();
        initJButton();
        setVisible(true);
    }

    private void initFrame(){
        this.setSize(600,600);
        this.setTitle("注册界面");
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLayout(null);
    }

    private void initJPanel(){
        JPanel jPanel=new JPanel(new GridLayout(3,2,10,10));
        jPanel.setBorder(BorderFactory.createEmptyBorder());
        jPanel.setBounds(100,200,300,200);
        jPanel.add(new JLabel("User Name:"));
        jPanel.add(new JTextField());
        jPanel.add(new JLabel("Password:"));
        jPanel.add(new JPasswordField());
        this.add(jPanel);
    }

    private void initJButton(){
        JButton jtb=new JButton("Register Now");
        jtb.setBounds(200,400,300,100);
        jtb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        getContentPane().add(jtb);
    }
}
