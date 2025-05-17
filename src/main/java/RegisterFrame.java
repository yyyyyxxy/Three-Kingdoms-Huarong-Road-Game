import org.bson.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.mindrot.jbcrypt.BCrypt;

import static com.mongodb.client.model.Filters.eq;

public class RegisterFrame extends JFrame{

    MongoDBUtil db=new MongoDBUtil();

    public RegisterFrame(){
        initFrame();
        initJPanel();
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
        JTextField jTextField=new JTextField();
        jPanel.add(jTextField);
        jPanel.add(new JLabel("Password:"));
        JPasswordField jPasswordField=new JPasswordField();
        jPanel.add(jPasswordField);
        this.getContentPane().add(jPanel);
        JButton jtb = getJButton(jTextField, jPasswordField);
        getContentPane().add(jtb);
    }

    private JButton getJButton(JTextField jTextField, JPasswordField jPasswordField) {
        JButton jtb=new JButton("Register Now");
        jtb.setBounds(200,400,300,100);
        jtb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userName= jTextField.getText();
                String password= jPasswordField.getUIClassID();
                if(db.getDocument("Users",eq("_id",userName))!=null){
                    new UserNameError();
                }
                else{
                    boolean[] checkPassword=checkPassword(password);
                    int T=0;
                    for(boolean b:checkPassword){
                        if(b){
                            T++;
                        }
                    }
                    if(T==5){
                        String hashed=BCrypt.hashpw(password,BCrypt.gensalt());
                        Document document=new Document("_id",userName).append("Password",hashed);
                        db.insertOne("Users",document);
                        dispose();
                        new Success();
                    }
                    else{
                        new PasswordError(password);
                    }
                }
            }

            public boolean[] checkPassword(String password){
                String specialChars="!@#$%";
                boolean length=false;
                boolean hasUpper=false;
                boolean hasLower=false;
                boolean hasDigit=false;
                boolean hasSpecial=false;
                boolean[] result={length,hasUpper,hasLower,hasDigit,hasSpecial};
                if(password.length()>=8){
                    length=true;
                }
                for(char ch:password.toCharArray()){
                    if(Character.isUpperCase(ch))
                        hasUpper=true;
                    else if(Character.isLowerCase(ch))
                        hasLower=true;
                    else if(Character.isDigit(ch))
                        hasDigit=true;
                    else if(specialChars.indexOf(ch)!=-1)
                        hasSpecial=true;
                    if(hasUpper && hasLower && hasDigit && hasSpecial)
                        break;
                }
                return result;
            }
        });
        return jtb;
    }
}

class UserNameError extends JFrame{

    public UserNameError(){
        initFrame();
        initJLabel();
        initJButton();
        this.setVisible(true);
    }

    private void initFrame(){
        this.setSize(200,400);
        this.setTitle("Error");
        this.setLocationRelativeTo(null);
        this.setLayout(null);
    }

    private void initJLabel(){
        JLabel jLabel=new JLabel("Sorry! Your user name has already taken. Please reset it.");
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

class PasswordError extends JFrame{

    private String password;

    public PasswordError(String password){
        this.password=password;
        initFrame();
        initJLabel();
        initJButton();
        this.setVisible(true);
    }

    private void initFrame(){
        this.setSize(200,400);
        this.setTitle("Error");
        this.setLocationRelativeTo(null);
        this.setLayout(null);
    }

    private void initJLabel(){
        JLabel jLabel=new JLabel();
        if(password.length()<8)
            jLabel.setText("Sorry! Your password length must be bigger than 8.");
        else{
            ArrayList<String> errors=errors(password);
            if(errors.size()==1)
                jLabel.setText(errors.get(0));
            else if(errors.size()==2)
                jLabel.setText(errors.get(0)+'\n'+errors.get(1));
            else if(errors.size()==3)
                jLabel.setText(errors.get(0)+'\n'+errors.get(1)+'\n'+errors.get(2));
            else if(errors.size()==4){
                jLabel.setText(errors.get(0)+'\n'+errors.get(1)+'\n'+errors.get(2)+'\n'+errors.get(3));
            }
        }
        this.getContentPane().add(jLabel);
    }

    private ArrayList<String> errors(String password){
        String specialChars="!@#$%";
        ArrayList<String> errors=new ArrayList<>();
        boolean hasUpper=false;
        boolean hasLower=false;
        boolean hasDigit=false;
        boolean hasSpecial=false;
        for(char ch:password.toCharArray()){
            if(Character.isUpperCase(ch))
                hasUpper=true;
            else if(Character.isLowerCase(ch))
                hasLower=true;
            else if(Character.isDigit(ch))
                hasDigit=true;
            else if(specialChars.indexOf(ch)!=-1)
                hasSpecial=true;
            if(hasUpper && hasLower && hasDigit && hasSpecial)
                break;
        }
        if(!hasUpper)
            errors.add("Sorry! Your password must have capital letters.");
        if(!hasLower)
            errors.add("Sorry! Your password must have lowercase letters.");
        if(!hasDigit)
            errors.add("Sorry! Your password must have digits.");
        if(!hasSpecial)
            errors.add("Sorry! Your password must include one or more than one special letters from !, @, #, $, %.");
        return errors;
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

class Success extends JFrame{
    public Success(){
        initJFrame();
        initJLabel();
        initJButton();
        this.setVisible(true);
    }

    private void initJFrame(){
        this.setSize(200,400);
        this.setTitle("Congratulation");
        this.setLocationRelativeTo(null);
        this.setLayout(null);
    }

    private void initJLabel(){
        JLabel jLabel=new JLabel("You register in successfully!");
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