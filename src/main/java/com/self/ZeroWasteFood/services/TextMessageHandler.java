package com.self.ZeroWasteFood.services;

import com.self.ZeroWasteFood.util.InMemoryUserStorage;
import com.self.ZeroWasteFood.util.Instructions;
import com.vdurmont.emoji.EmojiParser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
public class TextMessageHandler {
    private final UserService userService;
    private final MessageService messageService;
    private final InMemoryUserStorage userStorage;
    private final ProductScanService productScanService;

    public TextMessageHandler(UserService userService, MessageService messageService, InMemoryUserStorage userStorage, ProductScanService productScanService) {
        this.userService = userService;
        this.messageService = messageService;
        this.userStorage = userStorage;
        this.productScanService = productScanService;
    }

    public void handleTextMessage(String messageText, long chatId, Update update) {
        switch (messageText) {
            case "/start":
                handleStartCommand(chatId, update);
                break;
            case "/full_scan":
                handleFullScanProductCommand(chatId, update);
                break;
            default:
                handleUnknownCommand(chatId, messageText);
                break;
        }
    }


    private void handleStartCommand(long chatId, Update update) {
        if (!hasUserInDb(update.getMessage().getChat().getId())) {

            messageService.sendTextMessageWithCallbackQuery(
                    chatId,
                    Instructions.registerNewUserInstruction(update.getMessage().getChat().getFirstName()),
                    "add_me_msg",
                    EmojiParser.parseToUnicode(":sparkles: Add Me")
            );
            userStorage.saveUser(chatId, update.getMessage().getFrom());
        } else {

            messageService.sendTextMessage(chatId, Instructions.infoInstructions(update.getMessage().getChat().getFirstName()));

        }

        log.info("Handled /start command for chatId: {}", chatId);
    }

    private boolean hasUserInDb(@NonNull Long id) {
        return userService.findUserById(id).isPresent();
    }


    private void handleFullScanProductCommand(long chatId, Update update) {
        productScanService.createProductScanWithUser(
                userService
                        .findUserById
                                (update.getMessage()
                                        .getFrom()
                                        .getId()
                                )
                        .orElseThrow()
        );
        messageService.sendTextMessageWithForceReply(
                chatId,
                "Upload photo"
        );

        log.info("Handled /full_scan command for chatId: {}", chatId);
    }

    private void handleUnknownCommand(long chatId, String messageText) {
        messageService.sendTextMessage(chatId, "Unknown command: " + messageText);
        log.info("Received unknown command: {} for chatId: {}", messageText, chatId);
    }
}
