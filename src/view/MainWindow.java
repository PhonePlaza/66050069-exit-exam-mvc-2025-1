package view;

import javax.swing.*;
import java.awt.*;

/** หน้าต่างหลัก: รวม Login / Jobs / Apply / Admin */
public class MainWindow extends JFrame {

    private final CardLayout card = new CardLayout();
    private final JPanel root = new JPanel(card);

    public final LoginView loginView = new LoginView();
    public final JobsView jobsView = new JobsView();
    public final ApplyView applyView = new ApplyView();
    public final AdminView adminView = new AdminView(); // หน้าใหม่

    public MainWindow() {
        setTitle("Job Fair - MVC (Swing)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        root.add(loginView, "LOGIN");
        root.add(jobsView, "JOBS");
        root.add(applyView, "APPLY");
        root.add(adminView, "ADMIN");
        setContentPane(root);
    }

    public void showUI()   { setVisible(true); }
    public void showLogin(){ card.show(root, "LOGIN"); }
    public void showJobs() { card.show(root, "JOBS"); }
    public void showApply(){ card.show(root, "APPLY"); }
    public void showAdmin(){ card.show(root, "ADMIN"); } // หน้าผู้ดูแล

    public void setSessionTitle(String email, String role) {
        setTitle("Job Fair - MVC | " + role + " | " + email);
    }
}
