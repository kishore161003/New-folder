package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int MIN_PORT = 1025;
    private static final int MAX_PORT = 65535;
    private static final Random random = new Random();
    private List<ClientHandler> clients = new ArrayList<>();

    private static final Quiz quiz = new Quiz();
    private static Scanner sc = new Scanner(System.in);

    private void askQuestion() {
        System.out.println("Asking questions");
        Question[] questions = quiz.getQuestions();
        for (int i = 0; i < questions.length; i++) {
            Question question = questions[i];
            String questionString = (i + 1) + ". " + question.getQuestion() + " (" + question.getDuration() + "s)"
                    + " ::";
            String[] options = question.getOptions();
            for (int j = 0; j < options.length; j++) {
                questionString += (j + 1) + ". " + options[j] + ',';
            }
            questionString += "::" + question.getDurationInMillis();
            broadcastMessage(questionString);
            try {
                Thread.sleep(question.getDurationInMillis() + 2000);
                checkAnswer(question);
                displayScores();
                sendMessageToClients(3, "You're on the podium");
                Thread.sleep(5000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        displayScores(3);
        finalDisplayScore();
        broadcastMessage("end::The quiz has ended. Thank you for participating.");
        for (ClientHandler client : clients) {
            client.disconnect();
        }
    }

    public static void main(String[] args) {
        quiz.displayMenu();
        displayDashBoard();
    }

    private static void displayDashBoard() {
        displayServerInfo();
        System.out.println("KAHOOT SERVER");
        System.out.println("1. Start the quiz");
        System.out.println("2. Create new quiz");
        String start = sc.nextLine();
        if (start.equalsIgnoreCase("n")) {
            System.exit(0);
        } else {
            Server server = new Server();
            server.start();
        }
    }

    public static void displayServerInfo() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            System.out.println("Server running on: " + localhost.getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static int getRandomPort() {
        return random.nextInt(MAX_PORT - MIN_PORT + 1) + MIN_PORT;
    }

    public void start() {
        int port = getRandomPort();
        try (ServerSocket serverSocket = new ServerSocket(port, 0, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("Game Id (port number) " + port);
            System.out.println("1. to start the quizz");
            System.out.println("2. to exit the server");

            Thread messageSenderThread = new Thread(() -> {
                while (true) {
                    System.out.print("Press 1 to start the quiz or 2 to exit.");
                    int message = sc.nextInt();
                    if (message <= 0 || message >= 3) {
                        System.out.println("Invalid choice. Please try again.");
                        continue;
                    }
                    if (message == 1) {
                        askQuestion();
                    } else {
                        System.exit(0);
                    }
                }
            });
            messageSenderThread.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Create a new client handler for the connected client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
                clients.removeIf(client -> client.isClosed());
                if (clients.size() == 0) {
                    System.out.println("No clients connected. Exiting...");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void sendMessage(ClientHandler client, String message) {
        client.sendMessage(message);
    }

    public int calculateScore(int sec) {
        final int MAX_SCORE = 1000;
        final int MIN_SCORE = 100;
        int score = MAX_SCORE - (sec * ((MAX_SCORE - MIN_SCORE) / 60));
        score = Math.max(MIN_SCORE, score);
        return score;
    }

    private void checkAnswer(Question q) {
        for (ClientHandler client : clients) {
            try {
                System.out.println(client.getAnswer());
                if (q.verifyAnswer(Integer.parseInt(client.getAnswer().split("::")[0]))) {
                    int score = calculateScore(Integer.parseInt(client.getAnswer().split("::")[1]));
                    client.addScore(score);
                    client.addScoreArray(score);
                } else {
                    client.addScoreArray(0);
                }
            } catch (Exception e) {
                System.out.println("Error in checking answer");
            }
        }
    }

    public void sendMessageToClients(int n, String message) {
        Collections.sort(clients, Comparator.comparingInt(ClientHandler::getScore).reversed());
        int numberOfEntries = Math.min(n, clients.size());
        for (int i = 0; i < numberOfEntries; i++) {
            ClientHandler client = clients.get(i);
            // send message to client
            sendMessage(client, "message::" + message);
        }
    }

    public void displayScores(int n) {
        StringBuilder leaderboard = new StringBuilder();
        leaderboard.append(n <= 3 ? "           Podium      \n" : "           Leaderboard      \n");
        leaderboard.append("+-------+-----------------+----------+\n");
        leaderboard.append("| S.No. |    Team Name    |  Points  |\n");
        leaderboard.append("+-------+-----------------+----------+\n");
        int serialNumber = 1;
        Collections.sort(clients, Comparator.comparingInt(ClientHandler::getScore).reversed());
        int numberOfEntries = Math.min(n, clients.size());
        for (int i = 0; i < numberOfEntries; i++) {
            ClientHandler client = clients.get(i);
            String score = String.format("| %-5d | %-15s | %-8d |\n", serialNumber++, client.getTeamName(),
                    client.getScore());
            leaderboard.append(score);
        }
        leaderboard.append("+-------+-----------------+----------+\n");
        String leaderboardString = leaderboard.toString();
        System.out.println(leaderboardString);
        String clientarr[] = leaderboardString.split("\n");
        for (String scores : clientarr) {
            broadcastMessage("score::" + scores);
        }
        // broadcastMessage("score::" + leaderboardString);
    }

    public void finalDisplayScore() {
        StringBuilder leaderboard = new StringBuilder();
        leaderboard.append("           Leaderboard       \n");
        leaderboard.append("+-------+-----------------+");

        int numberofQuestions = quiz.getQuestions().length;

        for (int i = 1; i <= numberofQuestions; i++) {
            leaderboard.append("----------+");
        }
        leaderboard.append("----------+\n");
        leaderboard.append("| S.No. |    Team Name    |");
        for (int i = 1; i <= numberofQuestions; i++) {
            leaderboard.append(String.format("   Q%d     |", i));
        }
        leaderboard.append("   Total  |\n");

        leaderboard.append("+-------+-----------------+");

        for (int i = 0; i <= numberofQuestions; i++) {
            leaderboard.append("----------+");
        }
        leaderboard.append("\n");

        int serialNumber = 1;
        Collections.sort(clients, Comparator.comparingInt(ClientHandler::getScore).reversed());

        for (ClientHandler client : clients) {
            String teamName = client.getTeamName();
            int totalScore = client.getScore();
            ArrayList<Integer> scores = client.getScoreArray();

            StringBuilder scoresString = new StringBuilder();
            for (int score : scores) {
                scoresString.append(String.format(" %-8d |", score));
            }
            scoresString.append(String.format(" %-8d |", totalScore));

            String scoreLine = String.format("| %-5d | %-15s |%s", serialNumber++, teamName, scoresString);
            leaderboard.append(scoreLine);
            leaderboard.append("\n");
            leaderboard.append("+-------+-----------------+");
            for (int i = 0; i <= numberofQuestions; i++) {
                leaderboard.append("----------+");
            }
            leaderboard.append("\n");
        }
        // Adding footer line


        String leaderboardString = leaderboard.toString();
        System.out.println(leaderboardString);
    }

    public void displayScores() {
        displayScores(clients.size());
    }
}

class ClientHandler extends Thread {
    private Socket clientSocket;
    private PrintWriter out;
    private String teamName;
    private int score;
    private ArrayList<Integer> scores = new ArrayList<>();

    private String answer;

    public String getTeamName() {
        return teamName;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int score) {
        this.score += score;
    }

    public void addScoreArray(int score) {
        scores.add(score);
    }

    public ArrayList<Integer> getScoreArray() {
        return scores;
    }

    public String getAnswer() {
        return answer;
    }

    public Socket getSocket() {
        return clientSocket;
    }

    public boolean isClosed() {
        if (clientSocket.isClosed()) {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
        return clientSocket.isClosed();
    }

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            out.close();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;
            String teamName = in.readLine();
            while (teamName == null || teamName.isEmpty()) {
                teamName = in.readLine();
            }
            System.out.println();
            System.out.println(teamName + " has joined!!");
            this.teamName = teamName;
            while ((inputLine = in.readLine()) != null) {
                this.answer = inputLine;
                System.out.println("Received from " + teamName);
            }
        } catch (IOException e) {
            if (this.isClosed()) {
                System.out.println(teamName + " has disconnected");
            } else
                e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}