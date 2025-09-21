Job Fair MVC (Java Swing) — Overview

โปรแกรมสมัครงาน Job Fair เขียนด้วย Java + Swing ตามสถาปัตยกรรม MVC ใช้ไฟล์ CSV เป็นฐานข้อมูล (Companies, Jobs, Candidates, Admins, Applications)
Student สมัครงานให้ตนเองได้, Admin ดูรายการสมัครและใส่เกรด A–F

 ==== Features ====

- Login แบบง่าย (อีเมล + เลือกบทบาท)
- Student ต้องมีอีเมลใน candidates.csv
- Admin ต้องมีอีเมลใน admins.csv
- Jobs (Student): แสดงเฉพาะงานที่เปิดและยังไม่หมด deadline, Sort ได้ (Title/Company/Deadline), Apply ได้
- Apply (Student): ล็อกชื่อผู้สมัครเป็นคนที่ล็อกอินอยู่, บันทึกเวลาสมัครจากเครื่อง
- Applications (Admin): รายการใบสมัครทั้งหมด + แก้ไขเกรด A–F แล้วบันทึกกลับ applications.csv
- Business Rules: CO-OP รับเฉพาะ STUDYING, REGULAR รับเฉพาะ GRADUATED

 ==== Usage ====
- เปิดโปรแกรม → หน้า Login ใส่อีเมลและเลือกบทบาท
- Student → เข้าหน้า Jobs เลือกงาน → Apply → Confirm apply (ระบบบันทึกเวลาแล้วกลับหน้า Jobs)
- Admin → เข้าหน้า Applications เลือกแถว → เปลี่ยน Grade → Save selected grade
- ต้องการออกจากระบบ: กดปุ่ม Logout ที่หน้า Jobs
