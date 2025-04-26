package com.example.demo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;

public class HelloApplication extends Application {
    private final ArrayList<File> songs = new ArrayList<>();
    private final ArrayList<String> songNames = new ArrayList<>();
    private int id = 0;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isRepeat = false;
    private boolean isShuffle = false;

    @Override
    public void start(Stage stage) {
        VBox root = new VBox();
        root.setSpacing(15);
        root.setPadding(new Insets(20));

        Button openButton = new Button("Open Folder");
        openButton.setPrefWidth(120);

        ListView<String> playList = new ListView<>();
        playList.setPrefHeight(200);

        Label currentTrackText = new Label("Current Track");
        currentTrackText.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label trackName = new Label("Track Name");
        trackName.setStyle("-fx-font-size: 14px;");

        Slider timer = new Slider(0, 100, 0);
        timer.setPrefWidth(600);
        Label timeTrack = new Label("00:00 / 00:00");

        HBox timeBox = new HBox(timer, timeTrack);
        timeBox.setSpacing(10);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        HBox controls = new HBox();
        controls.setSpacing(10);
        controls.setAlignment(Pos.CENTER);

        Button prevButton = new Button("⏮ Previous");
        Button playButton = new Button("▶ Play");
        Button pauseButton = new Button("⏸ Pause");
        Button stopButton = new Button("⏹ Stop");
        Button nextButton = new Button("⏭ Next");

        ToggleButton repeatButton = new ToggleButton("🔁 Repeat");
        ToggleButton shuffleButton = new ToggleButton("🔀 Shuffle");

        prevButton.setPrefWidth(90);
        playButton.setPrefWidth(90);
        pauseButton.setPrefWidth(90);
        stopButton.setPrefWidth(90);
        nextButton.setPrefWidth(90);
        repeatButton.setPrefWidth(90);
        shuffleButton.setPrefWidth(90);

        controls.getChildren().addAll(prevButton, playButton, pauseButton, stopButton, nextButton, repeatButton, shuffleButton);

        root.getChildren().addAll(openButton, playList, currentTrackText, trackName, timeBox, controls);

        openButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File dir = directoryChooser.showDialog(stage);
            if (dir != null) {
                songs.clear();
                songNames.clear();
                File[] files = dir.listFiles(f ->
                        f.getName().endsWith(".mp3") || f.getName().endsWith(".wav"));
                if (files != null) {
                    for (File f : files) {
                        songs.add(f);
                        songNames.add(f.getName());
                    }
                    playList.getItems().setAll(songNames);
                }
            }
        });

        repeatButton.setOnAction(e -> isRepeat = repeatButton.isSelected());
        shuffleButton.setOnAction(e -> isShuffle = shuffleButton.isSelected());

        playList.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (newIndex != null && newIndex.intValue() >= 0) {
                id = newIndex.intValue();
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }
                isPlaying = false;
                isPaused = false;
                mediaPlayer = null;
                playButton.fire();
            }
        });

        playButton.setOnAction(e -> {
            if (mediaPlayer == null && !songs.isEmpty()) {
                File currentSong = songs.get(id);
                trackName.setText(songNames.get(id));
                Media media = new Media(currentSong.toURI().toString());
                mediaPlayer = new MediaPlayer(media);

                mediaPlayer.setOnReady(() -> {
                    Duration totalDuration = mediaPlayer.getTotalDuration();
                    timeTrack.setText("00:00 / " + formatDuration(totalDuration));
                    mediaPlayer.play();
                    isPlaying = true;
                    isPaused = false;

                    mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                        Duration total = mediaPlayer.getTotalDuration();
                        if (total != null && total.greaterThan(Duration.ZERO)) {
                            double progress = newTime.toMillis() / total.toMillis();
                            timer.setValue(progress * 100);
                            String current = formatDuration(newTime);
                            String totalStr = formatDuration(total);
                            timeTrack.setText(current + " / " + totalStr);
                        }
                    });

                    mediaPlayer.setOnEndOfMedia(() -> {
                        if (isRepeat) {
                            mediaPlayer.seek(Duration.ZERO);
                            mediaPlayer.play();
                        } else {
                            if (isShuffle) {
                                id = (int) (Math.random() * songs.size());
                            } else {
                                id = (id + 1) % songs.size();
                            }
                            if (mediaPlayer != null) {
                                mediaPlayer.stop();
                                mediaPlayer.dispose();
                            }
                            mediaPlayer = null;
                            isPlaying = false;
                            isPaused = false;
                            playButton.fire();
                        }
                    });
                });

                timer.setOnMousePressed(ev -> {
                    if (mediaPlayer != null) mediaPlayer.pause();
                });

                timer.setOnMouseReleased(ev -> {
                    if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN) {
                        Duration totalDuration = mediaPlayer.getTotalDuration();
                        if (totalDuration != null && !totalDuration.isUnknown()) {
                            double percent = timer.getValue() / 100;
                            Duration seekTo = totalDuration.multiply(percent);
                            mediaPlayer.seek(seekTo);
                            if (!isPaused) mediaPlayer.play();
                        }
                    }
                });

                playList.getSelectionModel().select(id);

            } else if (mediaPlayer != null && isPaused) {
                mediaPlayer.play();
                isPaused = false;
                isPlaying = true;
            }
        });

        pauseButton.setOnAction(e -> {
            if (mediaPlayer != null && isPlaying) {
                mediaPlayer.pause();
                isPaused = true;
                isPlaying = false;
            }
        });

        stopButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
                isPlaying = false;
                isPaused = false;
                timer.setValue(0);
                timeTrack.setText("00:00 / 00:00");
            }
        });

        prevButton.setOnAction(e -> {
            if (!songs.isEmpty()) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }
                id = (id > 0) ? id - 1 : songs.size() - 1;
                mediaPlayer = null;
                isPlaying = false;
                isPaused = false;
                playButton.fire();
            }
        });

        nextButton.setOnAction(e -> {
            if (!songs.isEmpty()) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }
                id = (id < songs.size() - 1) ? id + 1 : 0;
                mediaPlayer = null;
                isPlaying = false;
                isPaused = false;
                playButton.fire();
            }
        });

        Scene scene = new Scene(root, 900, 600);

        // ⌨ Горячие клавиши
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                if (isPlaying) {
                    pauseButton.fire();
                } else {
                    playButton.fire();
                }
            } else if (event.getCode() == KeyCode.RIGHT) {
                nextButton.fire();
            } else if (event.getCode() == KeyCode.LEFT) {
                prevButton.fire();
            }
        });

        stage.setTitle("Simple Music Player");
        stage.setScene(scene);
        stage.show();
    }

    public static String formatDuration(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) (duration.toSeconds() % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static void main(String[] args) {
        launch();
    }
}
