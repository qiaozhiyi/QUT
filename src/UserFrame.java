import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class UserFrame extends JFrame {
    private Connection connection;
    private String username;
    private DefaultTableModel bookTableModel;

    public UserFrame(Connection connection, String username) {
        this.connection = connection;
        this.username = username;

        setTitle("用户界面");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel welcomeLabel = new JLabel("欢迎, " + username + "!");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(welcomeLabel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        // 创建各个面板
        JPanel viewBooksPanel = createViewBooksPanel();
        JPanel viewHistoryPanel = createViewHistoryPanel();

        // 添加面板到标签页
        tabbedPane.addTab("查看所有书籍", viewBooksPanel);
        tabbedPane.addTab("借阅历史", viewHistoryPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createViewBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        bookTableModel = new DefaultTableModel();
        bookTableModel.addColumn("书名");
        bookTableModel.addColumn("ISBN");
        bookTableModel.addColumn("价格");
        bookTableModel.addColumn("借阅状态");

        JTable table = new JTable(bookTableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 禁止编辑
            }
        };
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton borrowButton = new JButton("借阅");
        JButton returnButton = new JButton("归还");
        JButton refreshButton = new JButton("刷新");

        buttonPanel.add(borrowButton);
        buttonPanel.add(returnButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        borrowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    String isbn = (String) bookTableModel.getValueAt(selectedRow, 1);
                    borrowBook(isbn);
                } else {
                    JOptionPane.showMessageDialog(UserFrame.this, "请选择一本书籍进行借阅。");
                }
            }
        });

        returnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    String isbn = (String) bookTableModel.getValueAt(selectedRow, 1);
                    returnBook(isbn);
                } else {
                    JOptionPane.showMessageDialog(UserFrame.this, "请选择一本书籍进行归还。");
                }
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadBooks();
            }
        });

        loadBooks(); // 初始加载

        return panel;
    }

    private JPanel createViewHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        DefaultTableModel historyTableModel = new DefaultTableModel();
        historyTableModel.addColumn("书名");
        historyTableModel.addColumn("ISBN");
        historyTableModel.addColumn("借阅日期");
        historyTableModel.addColumn("归还日期");

        JTable table = new JTable(historyTableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 禁止编辑
            }
        };
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("刷新");
        panel.add(refreshButton, BorderLayout.SOUTH);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadHistory(historyTableModel);
            }
        });

        loadHistory(historyTableModel); // 初始加载

        return panel;
    }

    private void borrowBook(String isbn) {
        try {
            // 检查书籍是否存在且未被借阅
            String checkQuery = "SELECT * FROM books WHERE isbn=? AND id NOT IN (SELECT book_id FROM borrowed_books WHERE return_date IS NULL)";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setString(1, isbn);
            ResultSet resultSet = checkStatement.executeQuery();

            if (resultSet.next()) {
                int bookId = resultSet.getInt("id");

                // 借阅书籍
                String borrowQuery = "INSERT INTO borrowed_books (username, book_id, borrow_date) VALUES (?, ?, NOW())";
                PreparedStatement borrowStatement = connection.prepareStatement(borrowQuery);
                borrowStatement.setString(1, username);
                borrowStatement.setInt(2, bookId);
                borrowStatement.executeUpdate();

                JOptionPane.showMessageDialog(this, "书籍借阅成功！");
                borrowStatement.close();
            } else {
                JOptionPane.showMessageDialog(this, "书籍不可借阅，可能不存在或已被借阅！");
            }

            resultSet.close();
            checkStatement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "书籍借阅失败：" + ex.getMessage());
        }
        loadBooks(); // 刷新书籍列表
    }

    private void returnBook(String isbn) {
        try {
            // 检查书籍是否已被当前用户借阅
            String checkQuery = "SELECT * FROM borrowed_books bb JOIN books b ON bb.book_id = b.id WHERE b.isbn=? AND bb.username=? AND bb.return_date IS NULL";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setString(1, isbn);
            checkStatement.setString(2, username);
            ResultSet resultSet = checkStatement.executeQuery();

            if (resultSet.next()) {
                // 归还书籍
                String returnQuery = "UPDATE borrowed_books SET return_date=NOW() WHERE username=? AND book_id=(SELECT id FROM books WHERE isbn=?) AND return_date IS NULL";
                PreparedStatement returnStatement = connection.prepareStatement(returnQuery);
                returnStatement.setString(1, username);
                returnStatement.setString(2, isbn);
                returnStatement.executeUpdate();

                JOptionPane.showMessageDialog(this, "书籍归还成功！");
                returnStatement.close();
            } else {
                JOptionPane.showMessageDialog(this, "未找到当前用户借阅的该书籍！");
            }

            resultSet.close();
            checkStatement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "书籍归还失败：" + ex.getMessage());
        }
        loadBooks(); // 刷新书籍列表
    }

    private void loadBooks() {
        bookTableModel.setRowCount(0); // 清空表格

        try {
            String query = "SELECT b.name, b.isbn, b.price, bb.username FROM books b LEFT JOIN borrowed_books bb ON b.id = bb.book_id AND bb.return_date IS NULL";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String isbn = resultSet.getString("isbn");
                double price = resultSet.getDouble("price");
                String borrowedBy = resultSet.getString("username");

                String status = (borrowedBy != null) ? "已借出" : "可借阅";
                bookTableModel.addRow(new Object[]{name, isbn, price, status});
            }

            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "加载书籍信息失败：" + ex.getMessage());
        }
    }

    private void loadHistory(DefaultTableModel model) {
        model.setRowCount(0); // 清空表格

        try {
            String query = "SELECT b.name, b.isbn, bb.borrow_date, bb.return_date FROM borrowed_books bb JOIN books b ON bb.book_id = b.id WHERE bb.username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String isbn = resultSet.getString("isbn");
                Timestamp borrowDate = resultSet.getTimestamp("borrow_date");
                Timestamp returnDate = resultSet.getTimestamp("return_date");

                model.addRow(new Object[]{name, isbn, borrowDate, returnDate});
            }

            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "加载借阅历史失败：" + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        // 创建数据库连接
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/librarydb", "root", "123456");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "数据库连接失败：" + ex.getMessage());
            System.exit(1);
        }

        // 创建用户界面
        UserFrame userFrame = new UserFrame(connection, "user1"); // 替换为实际用户名
        userFrame.setLocationRelativeTo(null);
        userFrame.setVisible(true);
    }
}
