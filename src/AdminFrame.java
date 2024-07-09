/*
    这里是管理员系统的设置

 */
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class AdminFrame extends JFrame {
    //启用jdbc连接数据库
    private Connection connection;


    public AdminFrame(Connection connection) {
        this.connection = connection;

        setTitle("管理员界面");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // 创建选项卡面板，分别用于图书管理和用户管理
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel bookPanel = createBookPanel();       //书籍选项卡
        JPanel userPanel = createUserPanel();       //用户选项卡
        JPanel historyPanel = createHistoryPanel(); // 添加借阅历史面板到选项卡

        //添加表头
        tabbedPane.addTab("图书管理", bookPanel);
        tabbedPane.addTab("用户管理", userPanel);
        tabbedPane.addTab("借阅历史", historyPanel); // 将借阅历史面板添加到选项卡

        //添加到中部
        add(tabbedPane, BorderLayout.CENTER);
    }

    // 创建图书管理面板
    private JPanel createBookPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // 创建图书信息录入区域
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));//卡片式布局
        JLabel nameLabel = new JLabel("书名:");
        JTextField nameField = new JTextField();
        JLabel isbnLabel = new JLabel("ISBN:");
        JTextField isbnField = new JTextField();
        JLabel priceLabel = new JLabel("单价:");
        JTextField priceField = new JTextField();
        JButton addButton = new JButton("添加");

        //添加到上层容器之中
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);
        inputPanel.add(isbnLabel);
        inputPanel.add(isbnField);
        inputPanel.add(priceLabel);
        inputPanel.add(priceField);
        inputPanel.add(new JLabel());
        inputPanel.add(addButton);

        //上层容器位置
        panel.add(inputPanel, BorderLayout.NORTH);

        // 创建图书信息显示表格  调用DefaultTableModel实现数据模组的展示
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("书名");
        model.addColumn("ISBN");
        model.addColumn("单价");

        //与之对应配合的JTable显示层组件
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);


        // 添加图书按钮点击事件
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = nameField.getText();
                String isbn = isbnField.getText();
                String priceStr = priceField.getText();

                //加一个错误处理如果填写不完整的书籍信息不会被录入系统中，防止恶意数据
                if (name.isEmpty() || isbn.isEmpty() || priceStr.isEmpty()) {
                    JOptionPane.showMessageDialog(AdminFrame.this, "请填写完整的图书信息");
                    return;
                }

                try {
                    double price = Double.parseDouble(priceStr);

                    // 插入图书信息到数据库
                    String insertQuery = "INSERT INTO books (name, isbn, price) VALUES (?, ?, ?)";      //插入sql键值对
                    PreparedStatement insertStatement = connection.prepareStatement(insertQuery);       //预编译执行
                    /*
                        代替问号执行一下填充
                     */
                    insertStatement.setString(1, name);
                    insertStatement.setString(2, isbn);
                    insertStatement.setDouble(3, price);
                    insertStatement.executeUpdate();//执行数据库修改操作

                    // 更新表格显示
                    model.addRow(new Object[]{name, isbn, price});

                    JOptionPane.showMessageDialog(AdminFrame.this, "图书添加成功");
                    nameField.setText("");
                    isbnField.setText("");
                    priceField.setText("");

                    insertStatement.close();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(AdminFrame.this, "请输入有效的单价");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(AdminFrame.this, "图书添加失败：" + ex.getMessage());
                }
            }
        });

        // 显示现有图书信息
        try {
            Statement statement = connection.createStatement();
            String query = "SELECT * FROM books";
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                String bookName = resultSet.getString("name");
                String bookISBN = resultSet.getString("isbn");
                double bookPrice = resultSet.getDouble("price");
                model.addRow(new Object[]{bookName, bookISBN, bookPrice});
            }

            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(AdminFrame.this, "加载图书信息失败：" + ex.getMessage());
        }

        return panel;
    }

    // 创建用户管理面板
    private JPanel createUserPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // 创建用户信息显示表格（临时表格）
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("用户名");
        model.addColumn("密码");
        model.addColumn("是否管理员");
        model.addColumn("注册时间"); // 使用 register_time 列名

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 添加用户管理按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton viewButton = new JButton("查看");
        JButton addButton = new JButton("添加");
        JButton updateButton = new JButton("更新");
        JButton deleteButton = new JButton("删除");

        buttonPanel.add(viewButton);
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 查看用户按钮点击事件
        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setRowCount(0); // 清空表格

                try {
                    Statement statement = connection.createStatement();
                    String query = "SELECT username, password, is_admin, register_time FROM users";
                    ResultSet resultSet = statement.executeQuery(query);

                    while (resultSet.next()) {
                        String username = resultSet.getString("username");
                        String password = resultSet.getString("password"); // 注意：实际应用中不建议直接显示密码，这里仅作为演示
                        boolean isAdmin = resultSet.getBoolean("is_admin");
                        Timestamp registerTime = resultSet.getTimestamp("register_time");

                        // 添加一行数据到表格中
                        model.addRow(new Object[]{username, password, isAdmin ? "是" : "否", registerTime});
                    }

                    resultSet.close();
                    statement.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(AdminFrame.this, "加载用户信息失败：" + ex.getMessage());
                }
            }
        });


        // 添加用户按钮点击事件
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newUsername = JOptionPane.showInputDialog(AdminFrame.this, "输入新用户的用户名:");
                if (newUsername != null && !newUsername.isEmpty()) {
                    try {
                        String insertQuery = "INSERT INTO users (username, password, is_admin) VALUES (?, ?, ?)";
                        PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
                        insertStatement.setString(1, newUsername);
                        insertStatement.setString(2, BCrypt.hashpw("123456", BCrypt.gensalt())); // 默认密码为123456
                        insertStatement.setBoolean(3, false); // 默认为普通用户
                        insertStatement.executeUpdate();//更新sql数据库

                        JOptionPane.showMessageDialog(AdminFrame.this, "用户添加成功");
                        insertStatement.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(AdminFrame.this, "用户添加失败：" + ex.getMessage());
                    }
                }
            }
        });

        // 更新用户按钮点击事件（示例）
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();

                if (selectedRow != -1) {
                    String username = (String) model.getValueAt(selectedRow, 0);

                    // 显示确认对话框，获取用户的确认结果
                    int confirmResult = JOptionPane.showConfirmDialog(AdminFrame.this,
                            "是否将用户 " + username + " 设置为管理员？", "确认", JOptionPane.YES_NO_OPTION);

                    if (confirmResult == JOptionPane.YES_OPTION) {
                        try {
                            String updateQuery = "UPDATE users SET is_admin=? WHERE username=?";
                            PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
                            updateStatement.setBoolean(1, true); // 设置为管理员
                            updateStatement.setString(2, username);
                            updateStatement.executeUpdate();

                            JOptionPane.showMessageDialog(AdminFrame.this, "用户信息更新成功");
                            updateStatement.close();

                            // 刷新表格显示
                            model.setValueAt("是", selectedRow, 2); // 更新表格中的管理员信息显示
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(AdminFrame.this, "用户信息更新失败：" + ex.getMessage());
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(AdminFrame.this, "请选择要更新的用户");
                }
            }
        });


        // 删除用户按钮点击事件（示例）
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    String username = (String) model.getValueAt(selectedRow, 0);
                    int confirm = JOptionPane.showConfirmDialog(AdminFrame.this,
                            "是否删除用户 " + username + " ?", "确认", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            String deleteQuery = "DELETE FROM users WHERE username=?";
                            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                            deleteStatement.setString(1, username);
                            deleteStatement.executeUpdate();

                            model.removeRow(selectedRow);
                            JOptionPane.showMessageDialog(AdminFrame.this, "用户删除成功");
                            deleteStatement.close();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(AdminFrame.this, "用户删除失败：" + ex.getMessage());
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(AdminFrame.this, "请选择要删除的用户");
                }
            }
        });

        return panel;
    }

    // 创建借阅历史面板
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 创建借阅历史信息显示表格
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("用户名");
        model.addColumn("书名");
        model.addColumn("ISBN");
        model.addColumn("借阅日期");
        model.addColumn("归还日期");

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 添加刷新按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("刷新");

        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 刷新按钮点击事件
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadHistory(model);
            }
        });

        // 初始加载借阅历史数据
        loadHistory(model);

        return panel;
    }

    // 加载借阅历史数据的方法
    private void loadHistory(DefaultTableModel model) {
        model.setRowCount(0); // 清空表格

        try {
            //注意join不能用union以及u.username实现对于不同表中同名的列的查询 u = user b = books bb = borrow_books
            String query = "SELECT u.username, b.name AS book_name, b.isbn, bb.borrow_date, bb.return_date FROM borrowed_books bb " +
                    "JOIN users u ON bb.username = u.username " +
                    "JOIN books b ON bb.book_id = b.id";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String bookName = resultSet.getString("book_name");
                String isbn = resultSet.getString("isbn");
                Timestamp borrowDate = resultSet.getTimestamp("borrow_date");
                Timestamp returnDate = resultSet.getTimestamp("return_date");

                model.addRow(new Object[]{username, bookName, isbn, borrowDate, returnDate});
            }

            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(AdminFrame.this, "加载借阅历史失败：" + ex.getMessage());
        }
    }


}

/*
    注意添加新的用户的默认密码为123456
 */