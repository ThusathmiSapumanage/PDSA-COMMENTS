# Project & Database Rules

Follow these steps exactly to get started. Don't skip anything!

### **1. Setup Locally**
1.  **Install MySQL Server** and **JDK 17** on your computer.
2.  **Create the Database:** Open MySQL Workbench and run:
    ```sql
    CREATE DATABASE algocore_db;
    ```
3.  **Create Tables:** Run the script in `games-backend/src/main/resources/schema.sql` to generate the tables.

### **2. Your Configuration**
1.  Go to `games-backend/src/main/resources/`.
2.  Create a new file called `application-local.properties`.
3.  Add your own MySQL username and password there like this:
    ```properties
    spring.datasource.username=root
    spring.datasource.password=your_password_here
    ```
    *(Note: This file is ignored by Git, so it won't overwrite others' settings!)*

### **3. Git Workflow**
1.  **Never push to `main`** directly.
2.  Work on your own **Feature Branch**.
3.  When done, open a **Pull Request (PR)** so others can review it.

---

### **Additional Note (Why we do this?)**
We use **local MySQL** cuz its fast, less laggy.. 
Using **Spring Profiles** (`application-local.properties`) ensures that your private database password stays on your computer and never gets pushed to GitHub. Soo this keeps our main code clean and professional for the coursework submission!
