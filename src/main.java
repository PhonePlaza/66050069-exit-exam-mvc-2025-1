import controller.JobFairController;
import model.Repository;
import view.MainWindow;

public class Main {
    public static void main(String[] args) {
        // สร้าง Repository ชี้ไปยังโฟลเดอร์ database
        Repository repo = new Repository("database");

        // สร้างหน้าต่างหลัก (มี JobsView + ApplyView)
        MainWindow window = new MainWindow();

        // สร้าง Controller เชื่อมทุกอย่าง
        new JobFairController(repo, window);

        // แสดง UI
        window.showUI();
    }
}
