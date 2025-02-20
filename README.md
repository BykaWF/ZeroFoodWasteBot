# ZeroFoodWasteBot

ZeroFoodWasteBot is a Telegram bot designed to help reduce food waste by processing and analyzing images of food items. This bot can interact with users through text and photo messages, and can analyze the content of images to extract useful information such as product names and expiration dates.

## Features

- **Photo Processing**:
  - Analyzes uploaded images to extract text using Optical Character Recognition (OCR).
  - Extracts product names and expiration dates from the images.

## Requirements

- Java 11 or higher
- Maven
- Python 3.x
- Flask
- OpenCV
- EasyOCR
- Requests library (Python)

## Setup

### Java Setup

1. **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/ZeroFoodWasteBot.git
    ```

2. **Navigate to the project directory**:
    ```bash
    cd ZeroFoodWasteBot
    ```

3. **Build the Java project**:
    ```bash
    mvn clean install
    ```

4. **Run the Java application**:
    ```bash
    java -cp target/your-artifact-name.jar com.example.test.ZeroFoodWasteBot YOUR_BOT_TOKEN
    ```

   Replace `YOUR_BOT_TOKEN` with your actual Telegram bot token.

### Python Flask Server Setup

1. **Navigate to the Flask server directory** (if separate):
    ```bash
    cd path/to/flask/server
    ```

2. **Install the required Python packages**:
    ```bash
    pip install flask opencv-python easyocr numpy requests
    ```

3. **Run the Flask server**:
    ```bash
    python app.py
    ```

   The server will start on port 5000 by default. Ensure that this port is open and accessible.

## Configuration

- **Bot Token**: Replace `YOUR_BOT_TOKEN` in the Java application with your actual Telegram bot token.
- **Flask Server URL**: The Java bot sends image processing requests to `http://127.0.0.1:5000/process-image`. Ensure the Flask server is running on this address.

## Usage

1. Start the Java bot and the Flask server.
2. Interact with the bot on Telegram:
   - Use commands like `/start`, `/pic`, `/markup`, and others to see different responses and functionalities.
   - Upload a photo for the bot to process. The bot will send the image to the Flask server, which will analyze it and return information about the product.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgements

- [Telegram Bots Java API](https://github.com/rubenlagus/TelegramBots)
- [EasyOCR](https://github.com/JaidedAI/EasyOCR)
- [OpenCV](https://opencv.org/)

## Contributing

Contributions are welcome! Please submit a pull request or open an issue for any suggestions or improvements.

