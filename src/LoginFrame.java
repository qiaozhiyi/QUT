/*
    这里是登录界面的设计实现了用户账号的基本的登录功能，
    实现了用户的注册功能实现了用户个人信息的BCrypto的加密
 */


import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
//调用b加密的加密包文件，包文件实际上是从网上下载的
import org.mindrot.jbcrypt.BCrypt;

/*
    @writer : Dc.Yao
    @data : 2024/6/27
 */

public class LoginFrame extends JFrame {
    private JTextField usernameField;       //用户名的usernameField
    private JPasswordField passwordField;  //密码的passwordField
    private ButtonGroup roleGroup;      //单选框的roleGroup
    private JRadioButton adminRadioButton, userRadioButton;     //单选框的两个选项
    private Connection connection;      //JDBC的调用实现对于数据库的连接

    public LoginFrame() {

        //窗口配置设置
        setTitle("登录");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        //提示用户输入用户名
        JLabel usernameLabel = new JLabel("用户名:");
        usernameLabel.setBounds(20, 20, 80, 25);
        add(usernameLabel);

       //用户名输入区域填充
        usernameField = new JTextField(20);
        usernameField.setBounds(100, 20, 165, 25);
        add(usernameField);

        //提示用户输入密码
        JLabel passwordLabel = new JLabel("密码:");
        passwordLabel.setBounds(20, 50, 80, 25);
        add(passwordLabel);

        //密码输入区域填充
        passwordField = new JPasswordField(20);
        passwordField.setBounds(100, 50, 165, 25);
        add(passwordField);

        //单选框定义组
        roleGroup = new ButtonGroup();
        adminRadioButton = new JRadioButton("管理员");
        adminRadioButton.setBounds(100, 80, 80, 25);
        userRadioButton = new JRadioButton("用户");
        userRadioButton.setBounds(180, 80, 80, 25);

        //实现两个按钮的互斥
        roleGroup.add(adminRadioButton);
        roleGroup.add(userRadioButton);

        //添加两个按钮到界面之中
        add(adminRadioButton);
        add(userRadioButton);

        //添加登录按钮
        JButton loginButton = new JButton("登录");
        loginButton.setBounds(30, 120, 80, 25);
        add(loginButton);

        //添加注册按钮
        JButton registerButton = new JButton("注册");
        registerButton.setBounds(180, 120, 80, 25);
        add(registerButton);

        //登录按钮点击的对应事件
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });

        //注册按钮点击的对应事件
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openRegisterFrame();
            }
        });

        //连接数据库的t-c语法
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/librarydb", "root", "123456");
            initializeUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    //点击按钮实现的对应方法
    private void performLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        boolean isAdmin = adminRadioButton.isSelected();

        try {
            String query = "SELECT * FROM users WHERE username=?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String storedHash = resultSet.getString("password");
                boolean isAdminAccount = resultSet.getBoolean("is_admin");

                if (BCrypt.checkpw(password, storedHash)) {
                    if (isAdmin && isAdminAccount) {
                        JOptionPane.showMessageDialog(this, "管理员登录成功");
                        AdminFrame adminFrame = new AdminFrame(connection);
                        adminFrame.setLocationRelativeTo(null);
                        adminFrame.setVisible(true);
                        this.dispose(); // 关闭登录窗口

                    } else if (!isAdmin && !isAdminAccount) {
                        JOptionPane.showMessageDialog(this, "普通用户登录成功");
                        // 打开用户界面
                        UserFrame userFrame = new UserFrame(connection, username);
                        userFrame.setLocationRelativeTo(null);
                        userFrame.setVisible(true);
                        this.dispose(); // 关闭登录窗口
                    } else {
                        JOptionPane.showMessageDialog(this, "权限不符，登录失败");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "用户名或密码错误");
                }
            } else {
                JOptionPane.showMessageDialog(this, "用户名或密码错误");
            }
            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "登录失败：" + ex.getMessage());
        }
    }


    private void openRegisterFrame() {
        RegisterFrame registerFrame = new RegisterFrame(connection);
        registerFrame.setLocationRelativeTo(this);
        registerFrame.setVisible(true);
    }

    //实现定义的入口
    private void initializeUsers() {
        try {
            String[] usernames = {"admin", "user1", "user2"};
            String[] passwords = {"adminpass", "user1pass", "user2pass"};
            boolean[] isAdmin = {true, false, false};

            String checkQuery = "SELECT * FROM users WHERE username=?";     //sql查询语句的实现
            String insertQuery = "INSERT INTO users (username, password, is_admin) VALUES (?, ?, ?)";       //sql插入语句的实现

            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);     //预编译执行查询命令
            PreparedStatement insertStatement = connection.prepareStatement(insertQuery);       //预编译执行插入命令

            //进行登录验证
            for (int i = 0; i < usernames.length; i++) {
                checkStatement.setString(1, usernames[i]);
                ResultSet resultSet = checkStatement.executeQuery();


                if (!resultSet.next()) {
                    String hashedPassword = BCrypt.hashpw(passwords[i], BCrypt.gensalt());
                    insertStatement.setString(1, usernames[i]);
                    insertStatement.setString(2, hashedPassword);
                    insertStatement.setBoolean(3, isAdmin[i]);
                    insertStatement.executeUpdate();
                }

                resultSet.close();
            }

            checkStatement.close();
            insertStatement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    //实现启动的入口
    public static void main(String[] args) {
        LoginFrame frame = new LoginFrame();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
/*
设置的用户名：
            管理员：
            用户名： admin
            密码： adminpass
            普通用户
            用户名： user1
            密码： user1pass
            用户名： user2
            密码： user2pass
 */
