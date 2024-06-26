package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.time.Duration;
import java.time.LocalTime;

class SharedData {
    private String value;
    private boolean isSet = false;

    public synchronized void setValue(String newValue) {
        isSet = true;
        value = newValue;
    }

    public synchronized void clearValue() {
        isSet = false;
        value = null;
    }

    public synchronized boolean isSet() {
        return isSet;
    }

    public synchronized String getValue() {
        return value;
    }
}

public class Client {
    public static String responseRateMessage(Duration time, String qtime) {
        long seconds = time.getSeconds();
        double ratio = (double) seconds * 1000 / Double.parseDouble(qtime);
        if (ratio < 0.3) {
            return "Done in a flash";
        } else if (ratio < 0.6) {
            return "Pretty swift";
        } else if (ratio < 0.9) {
            return "Easy Peasy";
        } else if (ratio < 1.25) {
            return "A bit slow";
        } else {
            return "Too slow, you can do better next time";
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt user to enter the server's IP address
        System.out.print("Enter the server's IP address: ");
        String SERVER_ADDRESS = scanner.nextLine();
        System.out.print("Enter the game ID (port): ");
        int PORT = scanner.nextInt();
        scanner.nextLine();
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            System.out.print("Enter your team name: ");
            String teamName = scanner.nextLine();
            out.println(teamName);
            SharedData sharedData = new SharedData();
            Thread messageReceiverThread = new Thread(() -> {
                try {
                    System.out.println("Connected to server at " + SERVER_ADDRESS + ":" + PORT);
                    String message;
                    while ((message = in.readLine()) != null) {
                        // System.out.println("Server message: " + message);
                        LocalTime start = LocalTime.now();

                        String[] question = message.split("::");
                        if (question[0].equals("score")) {
                            System.out.println(question[1]);
                            continue;
                        } else if (question[0].equals("end")) {
                            System.out.println(question[1]);
                            System.out.println();
                            break;
                        } else if (question[0].equals("error")) {
                            System.out.println("Error: " + question[1]);
                            System.out.println();
                            break;
                        } else if (question[0].equals("message")) {
                            System.out.println(String.format("\t\t %s \t\t", question[1]));
                            System.out.println();
                            continue;
                        }

                        String[] answers = question[1].split(",");
                        System.out.println();
                        System.out.println(question[0]);
                        System.out.println();
                        for (String ans : answers) {
                            System.out.println(ans);
                        }
                        System.out.println();

                        try {
                            Thread receiveSenderThread = new Thread(() -> {
                                try {
                                    int options = answers.length;
                                    System.out.print("Enter your options : ");

                                    String response = scanner.nextLine();
                                    while (Integer.parseInt(response) < 1 || Integer.parseInt(response) > options) {
                                        System.out.println();
                                        System.out.println("Invalid option Enter again");
                                        System.out.println();
                                        response = scanner.nextLine();
                                    }
                                    LocalTime now = LocalTime.now();
                                    Duration duration = Duration.between(start, now);
                                    sharedData.setValue(response + "");
                                    System.out.println("                         "
                                            + responseRateMessage(duration, question[question.length - 1])
                                            + "           ");
                                    out.println(response + "::" + duration.getSeconds());
                                } catch (Exception e) {
                                    sharedData.clearValue();
                                }
                            });

                            Thread messageSenderThread = new Thread(() -> {
                                try {
                                    int time = 10000;
                                    if (question.length == 3) {
                                        time = Integer.parseInt(question[question.length - 1]);
                                    }
                                    Thread.sleep(time);
                                    if (!sharedData.isSet()) {
                                        receiveSenderThread.interrupt();
                                        LocalTime now = LocalTime.now();
                                        Duration duration = Duration.between(start, now);
                                        System.out.println("Time out");
                                        out.println(
                                                "0" + "::" + duration.getSeconds());
                                    }
                                    sharedData.clearValue();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });
                            receiveSenderThread.start();
                            messageSenderThread.start();
                        } catch (Exception e) {
                            System.out.println("Error in sending message");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exiting...");
                }
            });
            messageReceiverThread.start();
            messageReceiverThread.join();
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + SERVER_ADDRESS);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}