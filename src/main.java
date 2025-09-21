import controller.JobFairController;
import model.Repository;
import view.MainWindow;

public class Main {
    public static void main(String[] args) {
        //Repository ชี้ไปยังโฟลเดอร์ database
        Repository repo = new Repository("database");

        //หน้าต่างหลัก 
        MainWindow window = new MainWindow();

        //Controller เชื่อมทุกอย่าง
        new JobFairController(repo, window);

        //แสดง UI
        window.showUI();
    }
}
