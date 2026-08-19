package com.example.assistant.data;

import com.example.assistant.data.model.ChatMessage;
import com.example.assistant.data.model.ChatSummary;
import java.util.List;

public interface ChatRepository {

    interface VoidCb {
        void onSuccess();
        void onError(Throwable t);
    }

    interface MsgListCb {
        void onSuccess(List<ChatMessage> list);
        void onError(Throwable t);
    }

    interface ChatListCb {
        void onSuccess(List<ChatSummary> list);
        void onError(Throwable t);
    }

    void saveMessage(ChatMessage msg, VoidCb cb);

    void listMessages(String userId, String chatId, int limit, MsgListCb cb);

    void listChats(String userId, int limit, ChatListCb cb);
}
