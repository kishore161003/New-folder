package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Quiz {
  private String fileName;
  private Question[] questions;

  public String getFileName() {
    return fileName;
  }

  public Question[] getQuestions() {
    return questions;
  }

  public void setQuestions(Question[] questions) {
    this.questions = questions;
  }

  public static void main(String[] args) {
    Quiz quiz = new Quiz();
    quiz.displayMenu();
  }

  public void displayMenu() {
    Scanner sc = new Scanner(System.in);
    while (true) {
      try {
        System.out.println("Welcome to Quiz Manager");
        System.out.println("1. Create a new quiz");
        System.out.println("2. Choose an existing quiz");
        System.out.println("3. Enter a JSON file path");
        System.out.println("4. Exit");
        System.out.print("Enter your choice: ");
        int choice = sc.nextInt();
        sc.nextLine();
        if (choice <= 0 || choice >= 5) {
          System.out.println("Invalid choice. Please try again.");
          continue;
        }
        switch (choice) {
          case 1:
            createNewQuiz();
            break;
          case 2:
            chooseFile(true);
            break;
          case 3:
            System.out.print("Enter the JSON file path: ");
            String filePath = sc.nextLine();
            readFromFile(filePath);
            break;
          case 4:
            System.out.println("Exiting...");
            System.exit(0);
            break;
          default:
            break;
        }
        System.out.println("Quiz loaded successfully.");
        break;
      } catch (Exception e) {
        System.out.println("Invalid input. Please try again.");
        continue;
      }
    }
  }

  public Quiz(String fileName) {
    this.fileName = fileName;
    readFromFile();
  }

  public Quiz() {
  }

  public void setFileName(String fileName) {
    // Open the file and read the questions
    this.fileName = fileName;
    readFromFile();
  }

  public void readFromFile() {
    this.questions = JsonParser.parseJsonFile("server/questions/" + fileName);
  }

  public void readFromFile(String filePath) {
    this.questions = JsonParser.parseJsonFile(filePath);
  }

  public void showExistingQuizFiles() {
    // Show the list of existing quiz files from the directory: server/questions
    File folder = new File("server/questions");
    File[] listOfFiles = folder.listFiles();

    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        System.out.println((i + 1) + ". " + listOfFiles[i].getName().replace(".json", ""));
      } else if (listOfFiles[i].isDirectory()) {
        System.out.println("Directory " + listOfFiles[i].getName());
      }
    }
  }

  public void chooseFile(boolean showFileMenu) {
    // Ask the user to choose a file
    Scanner scanner = new Scanner(System.in);
    while (true) {
      if (showFileMenu) {
        this.showExistingQuizFiles();
      }
      System.out.print("Choose a file: ");
      int fileNumber = scanner.nextInt();
      File folder = new File("server/questions");
      File[] listOfFiles = folder.listFiles();
      if (fileNumber <= 0 || fileNumber > listOfFiles.length) {
        System.out.println("Invalid file number. Please try again.");
        continue;
      }
      this.fileName = listOfFiles[fileNumber - 1].getName();
      System.out.println("You have chosen: " + this.fileName);
      break;
    }
    this.questions = JsonParser.parseJsonFile("server/questions/" + fileName);
    // System.out.println(Arrays.toString(this.questions));
  }

  public void createNewQuiz() {
    // Create a new quiz
    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter the name of the new quiz: ");
    String quizName = scanner.nextLine();
    System.out.print("Enter the number of questions: ");
    int numberOfQuestions = scanner.nextInt();
    scanner.nextLine();
    Question[] questions = new Question[numberOfQuestions];
    for (int i = 0; i < numberOfQuestions; i++) {
      Question question = new Question();
      question.setId(i + 1);
      question.readQuestionName();
      question.readQuestionType();
      question.readOptions();
      question.readAnswerName();
      question.readDuration();
      questions[i] = question;
    }
    this.questions = questions;
    this.fileName = quizName + ".json";
    this.saveQuiz();
  }

  public void saveQuiz() {
    // Save the quiz to a file
    try {
      FileWriter fileWriter = new FileWriter("server/questions/" + this.fileName);
      fileWriter.write(this.toJson());
      fileWriter.close();
      System.out.println("Quiz saved successfully.");
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  public String toJson() {
    // Convert the quiz to JSON format
    StringBuilder json = new StringBuilder();
    json.append("{\"questions\":[");
    for (int i = 0; i < this.questions.length; i++) {
      json.append(this.questions[i].toJson());
      if (i < this.questions.length - 1) {
        json.append(",");
      }
    }
    json.append("]}");
    return json.toString();
  }
}
