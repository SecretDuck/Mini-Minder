package com.tmung.miniminder;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LinkedAccountsActivity extends AppCompatActivity {

    // This list holds the linked accounts' email addresses
    private final List<String> linkedEmails = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linked_accounts);

        // Show the linked accounts
        showLinkedAccounts();
    }

    // method to show the  linked accounts
    private void showLinkedAccounts() {

        String parentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // get reference in Firebase to the user's linked accounts
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users").child(parentUserId).child("linkedAccounts");

        // add event listener for the accounts
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> linkedAccountIDs = new ArrayList<>();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String accountID = childSnapshot.getKey();
                    linkedAccountIDs.add(accountID);
                }
                // get the emails and display them
                fetchEmailsAndUpdateListView(linkedAccountIDs);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // handle error
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(getApplicationContext(), "Failed to load linked accounts.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // method to fetch the linked accounts' emails and display them
    private void fetchEmailsAndUpdateListView(ArrayList<String> linkedAccountIds) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        for (String linkedAccountId : linkedAccountIds) {
            // use the reference to add event listener
            usersRef.child(linkedAccountId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String email = snapshot.child("email").getValue(String.class);
                        linkedEmails.add(email);

                        // Update ListView
                        updateListView(linkedEmails);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                    Log.e(TAG, "Database error: " + error.getMessage());
                    Toast.makeText(getApplicationContext(), "Error fetching linked emails.", Toast.LENGTH_SHORT).show();

                }
            });
        }
    }

    // method to update the listview with the linked ids
    private void updateListView(List<String> linkedChildIds) {
        ListView listView = findViewById(R.id.listView_linkedAccounts);
        // declare adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, linkedChildIds);
        listView.setAdapter(adapter);

        // Add click event to unlink accounts
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String childId = linkedChildIds.get(position);
                unlinkChildAccount(childId);
                // remove childID from local list
                linkedChildIds.remove(position);
                // notify adapter that data has changed
                adapter.notifyDataSetChanged();
            }
        });
    }

    // method to unlink account from parent's account
    private void unlinkChildAccount(final String childId) {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        final String parentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Delete link from parent's linkedAccounts
        ref.child(parentUserId).child("linkedAccounts").child(childId)
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {

                    // Delete link from child's linkedAccounts
                    ref.child(childId).child("linkedAccounts").child(parentUserId)
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(LinkedAccountsActivity.this, "Unlinked successfully.", Toast.LENGTH_SHORT).show();
                                // Refresh the list view
                                showLinkedAccounts();
                            } else {
                                Toast.makeText(LinkedAccountsActivity.this, "Failed to unlink.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(LinkedAccountsActivity.this, "Failed to unlink.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}