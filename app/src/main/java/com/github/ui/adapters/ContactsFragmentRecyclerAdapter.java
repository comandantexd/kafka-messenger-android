package com.github.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.R;
import com.github.db.contact.Contact;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ContactsFragmentRecyclerAdapter extends RecyclerView.Adapter<ContactsFragmentRecyclerAdapter.ViewHolder> {
    private ArrayList<Contact> contacts = new ArrayList<>();
    private ContactClickCallback onClick;
    private ContactClickCallback onLongClick;

    public interface ContactClickCallback {
        boolean action(CardView v, Contact c);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View v) {
            super(v);
        }
    }

    public ContactsFragmentRecyclerAdapter(ContactClickCallback onClick, ContactClickCallback onLongClick) {
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.contactview_recyclerview, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact currentContact = contacts.get(position);

        TextView userName = holder.itemView.findViewById(R.id.contact_username);
        TextView alias = holder.itemView.findViewById(R.id.contact_alias);
        CardView card = holder.itemView.findViewById(R.id.contact_cardview);

        alias.setText(currentContact.alias);
        userName.setText(currentContact.username);

        card.setOnClickListener((v) -> this.onClick.action((CardView) v, currentContact));
        card.setOnLongClickListener((v) -> this.onLongClick.action((CardView) v, currentContact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void setAll(List<Contact> contacts) {
        this.contacts.clear();
        this.contacts.addAll(contacts);
        this.notifyDataSetChanged();
    }

    public void add(Contact contact) {
        this.contacts.add(contact);
        this.notifyItemInserted(this.contacts.size() - 1);
    }

    /**
     * If exists, the contact with thus username will be removed
     * @param username - username of contact to remove
     */
    public void removeContactFromList(String username) {
        boolean done = false;
        int pos = 0;
        Iterator<Contact> iter = contacts.iterator();
        while (iter.hasNext() && !done) {
            Contact c = iter.next();
            if (c.username.equals(username)) {
                iter.remove();
                this.notifyItemRemoved(pos);
                done = true;
            } else {
                pos++;
            }
        }
    }

    public void removeContacts(List<Contact> contacts) {
        contacts.forEach((msg) -> {
            int i = this.contacts.indexOf(msg);
            this.contacts.remove(i);
            this.notifyItemRemoved(i);
        });
    }
}
