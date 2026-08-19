package com.example.assistant;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import android.widget.ImageView;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ChatHistoryViewHolder> {

    private final List<Userdatabase> chatHistoryList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Userdatabase message);
    }

    public ChatHistoryAdapter(List<Userdatabase> chatHistoryList, OnItemClickListener listener) {
        this.chatHistoryList = chatHistoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history, parent, false);
        return new ChatHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatHistoryViewHolder holder, int position) {
        Userdatabase message = chatHistoryList.get(position);
        holder.bind(message, listener);
    }

    @Override
    public int getItemCount() {
        return chatHistoryList != null ? chatHistoryList.size() : 0;
    }

    // ViewHolder 應為 static 類別
    public static class ChatHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSenderName, textViewMessageText, textViewDateTime;
        LinearLayout messagebubbleContainer, messageContainer, chatRow;
        ImageView imageViewAvatarLeft, imageViewAvatarRight;

        public ChatHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            textViewMessageText = itemView.findViewById(R.id.textViewMessageText);
            textViewDateTime = itemView.findViewById(R.id.textViewDateTime);
            messagebubbleContainer = itemView.findViewById(R.id.messageBubbleContainer);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            chatRow = itemView.findViewById(R.id.chatRow);
            imageViewAvatarLeft = itemView.findViewById(R.id.imageViewAvatarLeft);
            imageViewAvatarRight = itemView.findViewById(R.id.imageViewAvatarRight);
        }

        // 這裡才是 bind() 的正確位置（在建構子外）
        public void bind(Userdatabase message, OnItemClickListener listener) {
            // 顯示訊息內容
            textViewMessageText.setText(message.getMessage() != null ? message.getMessage() : "(無訊息)");
            textViewDateTime.setText(message.getCreated_at());

            // 根據角色決定顯示樣式
            if ("user".equalsIgnoreCase(message.getRole())) {
                textViewSenderName.setText(message.getDisplay_name());
                textViewSenderName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                messageContainer.setGravity(Gravity.END);
                chatRow.setGravity(Gravity.END);
                imageViewAvatarRight.setVisibility(View.VISIBLE);
                imageViewAvatarLeft.setVisibility(View.GONE);
                textViewMessageText.setBackgroundResource(R.drawable.bg_bubble_user);
            } else if ("gpt".equalsIgnoreCase(message.getRole())) {
                textViewSenderName.setText("KB");
                textViewSenderName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                messageContainer.setGravity(Gravity.START);
                chatRow.setGravity(Gravity.START);
                imageViewAvatarLeft.setVisibility(View.VISIBLE);
                imageViewAvatarRight.setVisibility(View.GONE);
                textViewMessageText.setBackgroundResource(R.drawable.bg_bubble_gpt);
            }

            // 點擊事件
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(message);
                }
            });
        }
    }
}
