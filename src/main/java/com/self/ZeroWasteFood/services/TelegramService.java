package com.self.ZeroWasteFood.services;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class TelegramService implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final UserService userService;
    private final TelegramClient telegramClient;
    private final String botToken = "7496319396:AAGx2AE3USjrLNUJXDRB06EtZD8saLqspX0";

    @Autowired
    public TelegramService(UserService userService) {
        this.userService = userService;
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        long chat_id = 0;
        if (update.hasCallbackQuery()) {
            chat_id = update.getCallbackQuery().getMessage().getChatId();
            log.info("We get call back query {}", update.getCallbackQuery().getData());
            handleCallBackQuery(chat_id, update.getCallbackQuery().getData(), update);
        }
        if (update.hasMessage()) {
            chat_id = update.getMessage().getChatId();
            log.info("We speak with {} at {}", update.getMessage().getChat().getFirstName(), LocalTime.now());

            if (update.getMessage().hasText()) {

                log.info("Our update has message : {} ", update.getMessage());

                handleTextMessage(update.getMessage().getText(), chat_id, update);

            } else if (update.getMessage().hasPhoto()) {
                try {
                    handlePhotoMessage(update.getMessage().getPhoto(), chat_id);
                } catch (IOException | TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void handleCallBackQuery(long chatId, String callBackQuery, Update update) {
        switch (callBackQuery) {
            case "upload_photo_msg":
                log.info("We are in the case {}", callBackQuery);
                SendMessage message = SendMessage.builder()
                        .chatId(chatId)
                        .text("Reply to this message and upload your photo")
                        .replyMarkup(ForceReplyKeyboard.builder().selective(true).build())
                        .build();
                executeMessage(message);
                break;
        }
    }

    private void handleTextMessage(String messageText, long chatId, Update update) {
        switch (messageText) {
            case "/start":
                sendTextMessage(chatId, "Welcome!");
                log.info("We answered on command /start");
                break;
            case "/new":
                addNewProduct(chatId, update);
                break;
            default:
                sendTextMessage(chatId, "Unknown command: " + messageText);
                log.info("Unknown command: {}", messageText);
                break;
        }

    }

    private void addNewProduct(long chatId, Update update) {
        sendTextMessageWithReplyMarkup(chatId, getProductUploadInstructions(update.getMessage().getChat().getFirstName()));

    }

    private void sendTextMessageWithReplyMarkup(long chatId, String productUploadInstructions) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(productUploadInstructions)
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboardRow(
                                new InlineKeyboardRow(InlineKeyboardButton.builder()
                                        .text(String.format("%s Upload photo", EmojiParser.parseToUnicode(":camera:")))
                                        .callbackData("upload_photo_msg")
                                        .build())
                        )
                        .build())
                .build();
        executeMessage(message);
    }


    private String getProductUploadInstructions(String firstName) {
        return EmojiParser.parseToUnicode(
                String.format(
                        """
                                Hi, %s ! %s
                                
                                %s Please upload a photo of your product for us to scan the expiration date.
                                
                                %s Focus on the Date: The expiration date should be visible and centered in the photo.
                                """,
                        firstName,
                        EmojiParser.parseToUnicode(":wave:"),
                        EmojiParser.parseToUnicode(":calendar:"),
                        EmojiParser.parseToUnicode(":bulb:")
                )
        );
    }


    private void handlePhotoMessage(List<PhotoSize> photos, long chatId) throws IOException, TelegramApiException {
        String fileId = photos.stream()
                .max(Comparator.comparingInt(PhotoSize::getFileSize))
                .map(PhotoSize::getFileId)
                .orElse("");


        GetFile getFile = new GetFile(fileId);
        String filePath = telegramClient.execute(getFile).getFilePath();
        File img = telegramClient.downloadFile(filePath);
        buildPostRequest(img, chatId, fileId);

    }

    private void buildPostRequest(File img, long chatId, String fileId) throws IOException {
        HttpPost postRequest = new HttpPost("http://127.0.0.1:5000/process-image");

        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
                .addBinaryBody("image",
                        img,
                        ContentType.APPLICATION_OCTET_STREAM,
                        fileId
                );
        HttpEntity entity = entityBuilder.build();

        postRequest.setEntity(entity);

        makeRequest(postRequest, chatId);

    }

    private void makeRequest(HttpPost postRequest, long chatId) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(postRequest)) {

            log.info(String.valueOf(response.getStatusLine().getStatusCode()));

            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                String responseBody = EntityUtils.toString(responseEntity);
                userService.addProductToUser(chatId, responseBody);
            }


            EntityUtils.consume(responseEntity);
        }
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.info("We got a problem with message. Message info {} and {}", message.getText(), message.getChatId());
        }
    }

    private void executeMessage(SendPhoto message) {
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.info("We got a problem with Photo message. Message info {} and {}", message.getCaption(), message.getChatId());
        }
    }

    private void sendPhotoMessage(long chatId, String filePath) {
        SendPhoto message = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(new File(filePath)))
                .build();
        executeMessage(message);
    }

    private void sendPhotoMessage(long chatId, String fileId, String caption) {
        SendPhoto message = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(fileId))
                .caption(caption)
                .build();
        executeMessage(message);
    }
}
