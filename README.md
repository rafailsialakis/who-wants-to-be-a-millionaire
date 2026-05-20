# Who Wants to Be a Millionaire?

Java/Swing quiz game inspired by the classic TV show. Answer 15 questions correctly to win €1,000,000.

## Features

- 15 questions loaded from JSON, randomized each game
- Prize ladder up to €1,000,000 with safe havens at €1,000 and €32,000
- Lifelines: 50:50 and Phone-an-LLM
- 3-second suspense sound before each answer is revealed
- Scoreboard with persistent results
- Cinematic dark theme with background music

## Project Structure

```
src/quizgame/       Java source files
resources/
  questions.json    Question bank
  sounds/           WAV audio files
  scoreboard.txt    Persistent scoreboard
```

## Requirements

- Java 17+

## Run

```bash
java -jar Millionaire.jar
```

## Adding questions

Edit `resources/questions.json`. Each entry follows this structure:

```json
  {
    "question": "Ποιο σύστημα αρχείων χρησιμοποιείται κυρίως από τα Windows;",
    "options": ["ext4", "HFS+", "NTFS", "UFS"],
    "correctIndex": 2
  }
```
