package com.github.ui.adapters;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.activities.MainActivity;
import com.github.R;
import com.github.db.contact.Contact;
import com.github.db.conversation.ConversationMessage;
import com.github.crypto.Cryptography;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChatFragmentRecyclerAdapter extends RecyclerView.Adapter<ChatFragmentRecyclerAdapter.ViewHolder> {
    private ArrayList<ChatData> conversations = new ArrayList<>();
    private ChatClickCallback onClick;
    private ChatClickCallback onLongClick;

    public interface ChatClickCallback {
        boolean action(CardView v, ChatData d);
    }

    public static class ChatData implements Serializable {
        public Contact contact;
        public String lastMessage;

        public ChatData(Contact contact, String lastMessage) {
            this.contact = contact;
            this.lastMessage = lastMessage;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View v) {
            super(v);
        }
    }

    public ChatFragmentRecyclerAdapter(ChatClickCallback onClick, ChatClickCallback onLongClick) {
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatview_recyclerview, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatData currentInfo = conversations.get(position);

        TextView last = holder.itemView.findViewById(R.id.last_message);
        TextView alias = holder.itemView.findViewById(R.id.chat_alias);
        TextView userName = holder.itemView.findViewById(R.id.chat_username);
        CardView card = holder.itemView.findViewById(R.id.chat_cardview);
        ImageView notification = holder.itemView.findViewById(R.id.notification_dot);

        userName.setText(currentInfo.contact.username);
        alias.setText(currentInfo.contact.alias);
        last.setText(currentInfo.lastMessage);

        //check unread
        if (currentInfo.contact.unread)
            notification.setVisibility(View.VISIBLE);
        else
            notification.setVisibility(View.INVISIBLE);

        card.setOnClickListener((v) -> this.onClick.action((CardView) v, currentInfo));
        card.setOnLongClickListener((v) -> this.onLongClick.action((CardView) v, currentInfo));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void setAll(List<ChatData> contacts) {
        this.conversations.clear();
        this.conversations.addAll(contacts);
        this.notifyDataSetChanged();
    }

    public void removeConversations(List<ChatData> conversations) {
        conversations.forEach((msg) -> {
            int i = this.conversations.indexOf(msg);
            this.conversations.remove(i);
            this.notifyItemRemoved(i);
        });
    }

    /**
     * The conversation is marked as unread, and the passed message is sown. if conversation does not exists,
     * then it is created
     *
     * @param msg - Message to update the conversation with
     */
    public void markConversationAsUnread(ConversationMessage msg) {
        int indx = 0;
        boolean found = false;

        new Thread(() -> MainActivity.databaseManager.setContactUnread(msg.conversation, true)).start();

        Iterator<ChatData> iter = conversations.iterator();
        while (iter.hasNext() && !found) {
            ChatData currentData = iter.next();
            if (msg.conversation.equals(currentData.contact.username)) {
                try {
                    currentData.lastMessage = Cryptography.decryptBytes(msg.content).plain;

                    //set unread,
                    currentData.contact.unread = true; //immediate variable
                    found = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                indx++;
            }
        }
        //If the conversation exists in the chat fragment, then is updated;
        //if not exists, then is added as a new conversation
        if (found) {
            this.notifyItemChanged(indx);
        } else {
            //update the recycler view from main thread
            final Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    ChatData data = (ChatData) msg.getData().getSerializable("data");
                    conversations.add(data);
                    ChatFragmentRecyclerAdapter.this.notifyItemInserted(conversations.size() - 1);
                }
            };

            //new conversation entry is created
            new Thread(() -> {
                try {
                    Contact contact = MainActivity.databaseManager.getContact(msg.conversation);
                    String plain = Cryptography.decryptBytes(msg.content).plain;

                    ChatData chatData = new ChatData(contact, plain);

                    Bundle b = new Bundle();
                    b.putSerializable("data", chatData);
                    Message m = new Message();
                    m.setData(b);
                    handler.sendMessage(m);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
