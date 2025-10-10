💰 Smart Expense Analyser
🧩 Overview

Smart Expense Analyser is a JavaFX-based desktop application that helps users efficiently track and manage their daily expenses.
It provides a simple, interactive interface for adding transactions, viewing spending history, and analyzing financial patterns — all in one place.

⚙️ Features

➕ Add, update, and delete daily expense records

📊 View total and category-wise spending analysis

💾 Stores all data locally using an SQLite database

🖥️ Simple and intuitive JavaFX GUI design

📈 Visual expense summary with graphs and charts

🏗️ Technologies Used

Java (JDK 17 or later)

JavaFX (for user interface)

SQLite (for data storage)

Scene Builder (for UI design — optional)

🚀 How to Run the Project

Clone the repository

git clone https://github.com/mokeshsaiuppalapati/smart-expense-analyser.git


Open the project in IntelliJ IDEA or Eclipse

Make sure JavaFX SDK and SQLite JDBC JAR are added to your project libraries

Run the Main.java file

Start adding and tracking your expenses!

📂 Project Structure
smart-expense-analyser/
│
├── src/
│   ├── com.expense.main/           # Main JavaFX application files
│   ├── com.expense.model/          # Data models (Transaction, Category)
│   ├── com.expense.repo/           # Database connection and CRUD operations
│   └── com.expense.ui/             # FXML UI and controllers
│
├── resources/
│   └── expense_analyser.db         # SQLite database file
│
└── README.md                       # Project documentation

✨ Future Improvements

🔐 Add login and user authentication

☁️ Enable cloud data sync for multi-device access

📱 Develop a mobile version using JavaFX or Flutter

👤 Author

U. Mokesh Sai
📧 Email: saiuppalapati16@gmail.com

🔗 GitHub: mokeshsaiuppalapati
