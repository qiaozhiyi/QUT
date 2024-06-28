import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class RegisterFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private Connection connection;

    //注册逻辑的实现
    public RegisterFrame(Connection connection) {
        this.connection = connection;

        setTitle("注册");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(null);

        JLabel usernameLabel = new JLabel("用户名:");
        usernameLabel.setBounds(20, 20, 80, 25);
        add(usernameLabel);

        usernameField = new JTextField(20);
        usernameField.setBounds(100, 20, 165, 25);
        add(usernameField);

        JLabel passwordLabel = new JLabel("密码:");
        passwordLabel.setBounds(20, 50, 80, 25);
        add(passwordLabel);

        passwordField = new JPasswordField(20);
        passwordField.setBounds(100, 50, 165, 25);
        add(passwordField);

        JButton registerButton = new JButton("注册");
        registerButton.setBounds(100, 100, 80, 25);
        add(registerButton);

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performRegistration();
            }
        });
    }

    //注册用户的信息存储到数据库之中
    private void performRegistration() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空");
            return;
        }

        try {
            String checkQuery = "SELECT * FROM users WHERE username=?";

            //老生常谈实现预编译防止sql注入
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setString(1, username);
            ResultSet resultSet = checkStatement.executeQuery();

            //这一段实现对于是否存在的判断
            if (resultSet.next())
            {
                JOptionPane.showMessageDialog(this, "用户名已存在");
            }
            else
            {
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                //插入语句的书写
                String insertQuery = "INSERT INTO users (username, password, is_admin, register_time) VALUES (?, ?, false, CURRENT_TIMESTAMP)";
                PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
                insertStatement.setString(1, username);
                insertStatement.setString(2, hashedPassword);
                insertStatement.executeUpdate();

                JOptionPane.showMessageDialog(this, "注册成功");
                this.dispose();
                //关闭防止动态调试破解
                insertStatement.close();
            }

            resultSet.close();
            checkStatement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "注册失败：" + ex.getMessage());
        }
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    }

}
