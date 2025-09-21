package view;

import javax.swing.*;
import java.awt.*;

/** หน้าล็อกอินแบบง่าย (UI เป็นอังกฤษ, คอมเมนต์ไทย) */
public class LoginView extends JPanel {

    // ให้ Controller ใส่ callback
    public interface LoginHandler { void handle(String email, String role); }
    private LoginHandler onLogin;

    private JTextField tfEmail;
    private JComboBox<String> cbRole;

    public LoginView() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Login");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        tfEmail = new JTextField("student@example.com");
        cbRole = new JComboBox<>(new String[]{"student","admin"});

        JButton btnLogin = new JButton("Login");
        btnLogin.addActionListener(e -> {
            if (onLogin != null) {
                onLogin.handle(tfEmail.getText().trim(), (String) cbRole.getSelectedItem());
            }
        });

        int r=0;
        gc.gridx=0; gc.gridy=r++; gc.gridwidth=2; add(title, gc);
        gc.gridwidth=1;
        gc.gridy=r; add(new JLabel("Email:"), gc);
        gc.gridx=1; add(tfEmail, gc);
        gc.gridx=0; gc.gridy=++r; add(new JLabel("Role:"), gc);
        gc.gridx=1; add(cbRole, gc);
        gc.gridx=0; gc.gridy=++r; gc.gridwidth=2; add(btnLogin, gc);
    }

    /** ให้ Controller ใส่ handler สำหรับปุ่ม Login */
    public void setLoginHandler(LoginHandler h) { this.onLogin = h; }
}
