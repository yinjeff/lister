package com.yin.lister;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.yin.lister.obj.ListItem;

public class ListItemActivity extends AppCompatActivity {
    private DatabaseReference listRef;
    private String listName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_item);

        listName = Utilities.unescapeJSONString(getIntent().getExtras().getString("listName"));
        updateTitle(0);
        // Add the back button
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        final ListView itemsView = (ListView) findViewById(R.id.items);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        itemsView.setAdapter(adapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText newItem = (EditText) findViewById(R.id.new_item_text);
                if (newItem.getText() != null && !"".equals(newItem.getText().toString().trim())) {
                    String str = newItem.getText().toString();
                    Log.d("LISTER:", "Adding [" + str + "] due to manual add");

                    listRef.child(Utilities.escapeJSONString(str)).setValue(Utilities.getListItem(str));
                    Toast.makeText(getApplicationContext(), "Added " + str, Toast.LENGTH_SHORT).show();
                }
                newItem.setText("");
            }
        });

        itemsView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                Query myQuery = listRef.orderByKey().equalTo(Utilities.escapeJSONString((String)
                        itemsView.getItemAtPosition(position)));

                myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChildren()) {
                            DataSnapshot firstChild = dataSnapshot.getChildren().iterator().next();
                            ListItem item = firstChild.getValue(ListItem.class);
                            Log.d("LISTER:", "Removing [" + item.getItemName() + "] from firebase db due to long click");
                            firstChild.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

                return true;
                }
        });

        // attach listener for changes to list
        listRef = FirebaseDatabase.getInstance().getReference(listName + "/listItems");

        // Add listener to sync the firebase db with the listview
        addChildEventListener("addedDate", adapter);
    }

    /**
     * Add a child event listener to the array that sorts by the @childSortAttr
     * @param childSortAttr Name of the attribute to sort by
     * @param adapter Array Adapter object
     */
    private void addChildEventListener(String childSortAttr, final ArrayAdapter<String> adapter) {
        listRef.orderByChild(childSortAttr).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                String str = Utilities.unescapeJSONString(dataSnapshot.getValue(ListItem.class).getItemName());
                Log.d("LISTER:", "Adding [" + str + "] to table due to firebase add");
                adapter.add(str);
                updateTitle(adapter.getCount());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String value = Utilities.unescapeJSONString(dataSnapshot.getValue(ListItem.class).getItemName());
                Log.d("LISTER:", "Removing [" + value + "] from table due to firebase remove");
                adapter.remove(value);
                Toast.makeText(getApplicationContext(), "Removed " + value, Toast.LENGTH_SHORT).show();
                updateTitle(adapter.getCount());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.e("TAG:", "Failed to read value.", error.toException());
            }
        });
    }

    private void updateTitle(int count) {
        setTitle(listName + (count > 0?" (" + count + ")":""));
    }

}
